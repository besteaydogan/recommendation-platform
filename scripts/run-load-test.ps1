[CmdletBinding()]
param(
    [ValidateRange(1, 10000000)]
    [int] $ExpectedEvents = 100000,

    [ValidateRange(1, 120)]
    [int] $TimeoutMinutes = 30,

    [switch] $KeepRunning
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$composeProject = 'recoflow-load-test'
$envFile = Join-Path $projectRoot 'load-test.env'
$sourceFile = Join-Path $projectRoot 'product-views.json'
$resultFile = Join-Path $projectRoot 'target/load-test-result.json'
$appPort = 8081
$topic = 'product-views-load-test'
$consumerGroup = 'recoflow-product-views-load-test'

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]] $Arguments)

    & docker compose --project-name $composeProject --env-file $envFile @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed with exit code $LASTEXITCODE"
    }
}

function Invoke-ContainerCommand {
    param(
        [string] $ContainerId,
        [Parameter(ValueFromRemainingArguments = $true)][string[]] $Arguments
    )

    $output = & docker exec $ContainerId @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker exec failed with exit code $LASTEXITCODE"
    }
    return $output
}

function Get-DatabaseCount {
    param([string] $PostgresContainer)

    $commandArguments = @(
        'psql', '-U', 'recoflow', '-d', 'recoflow_load_test', '-tA',
        '-c', 'select count(*) from product_views;'
    )
    $value = Invoke-ContainerCommand -ContainerId $PostgresContainer -Arguments $commandArguments
    return [long]($value | Select-Object -Last 1)
}

function Get-ConsumerLag {
    param([string] $KafkaContainer)

    $commandArguments = @(
        '/opt/kafka/bin/kafka-consumer-groups.sh',
        '--bootstrap-server', 'localhost:9092',
        '--group', $consumerGroup,
        '--describe'
    )
    $lines = Invoke-ContainerCommand -ContainerId $KafkaContainer -Arguments $commandArguments
    $lag = 0L
    $matched = $false
    foreach ($line in $lines) {
        $columns = $line.Trim() -split '\s+'
        if ($columns.Count -ge 6 -and $columns[1] -eq $topic -and $columns[5] -match '^\d+$') {
            $lag += [long]$columns[5]
            $matched = $true
        }
    }
    if (-not $matched) {
        return $null
    }
    return $lag
}

function Get-TopicRecordCount {
    param(
        [string] $KafkaContainer,
        [string] $TopicName
    )

    $commandArguments = @(
        '/opt/kafka/bin/kafka-get-offsets.sh',
        '--bootstrap-server', 'localhost:9092',
        '--topic', $TopicName
    )
    $lines = Invoke-ContainerCommand -ContainerId $KafkaContainer -Arguments $commandArguments
    $count = 0L
    foreach ($line in $lines) {
        if ($line -match ':(\d+)$') {
            $count += [long]$Matches[1]
        }
    }
    return $count
}

function Get-PrometheusCounter {
    param(
        [string] $Metrics,
        [string] $Name
    )

    $match = [regex]::Match($Metrics, "(?m)^$([regex]::Escape($Name))\s+([0-9.eE+-]+)$")
    if (-not $match.Success) {
        return 0L
    }
    return [long][double]$match.Groups[1].Value
}

if (-not (Test-Path -LiteralPath $sourceFile -PathType Leaf)) {
    throw "Load-test source file not found: $sourceFile"
}

$actualLines = [System.Linq.Enumerable]::Count([System.IO.File]::ReadLines($sourceFile))
if ($actualLines -ne $ExpectedEvents) {
    throw "Expected $ExpectedEvents JSONL records, but $sourceFile contains $actualLines lines."
}

$completed = $false
try {
    Write-Host "Starting isolated load-test stack ($ExpectedEvents events)..."
    Invoke-Compose down --volumes --remove-orphans
    Invoke-Compose up --detach --build

    $appContainer = (Invoke-Compose ps --quiet recommendation-platform | Select-Object -Last 1).Trim()
    $postgresContainer = (Invoke-Compose ps --quiet postgres | Select-Object -Last 1).Trim()
    $kafkaContainer = (Invoke-Compose ps --quiet kafka | Select-Object -Last 1).Trim()
    if (-not $appContainer -or -not $postgresContainer -or -not $kafkaContainer) {
        throw 'Could not resolve one or more load-test container IDs.'
    }

    $deadline = [DateTimeOffset]::UtcNow.AddMinutes($TimeoutMinutes)
    $startedAt = $null
    while ([DateTimeOffset]::UtcNow -lt $deadline -and $null -eq $startedAt) {
        $appRunning = (& docker inspect --format '{{.State.Running}}' $appContainer 2>$null) -eq 'true'
        if (-not $appRunning) {
            $recentLogs = & docker logs --tail 50 $appContainer 2>&1
            throw "Application container stopped before the load test started:`n$recentLogs"
        }
        $startLine = & docker logs --timestamps $appContainer 2>&1 |
            Select-String 'Started streaming product-view source records' |
            Select-Object -First 1
        if ($startLine -and $startLine.Line -match '^(\S+)') {
            $startedAt = [DateTimeOffset]::Parse($Matches[1])
            break
        }
        Start-Sleep -Milliseconds 250
    }
    if ($null -eq $startedAt) {
        throw 'Timed out waiting for the producer to start streaming.'
    }

    $databaseRows = 0L
    $consumerLag = $null
    do {
        if ([DateTimeOffset]::UtcNow -ge $deadline) {
            throw "Load test did not complete within $TimeoutMinutes minutes (rows=$databaseRows, lag=$consumerLag)."
        }
        $databaseRows = Get-DatabaseCount $postgresContainer
        $consumerLag = Get-ConsumerLag $kafkaContainer
        Write-Progress -Activity 'Consuming product-view events' `
            -Status "$databaseRows / $ExpectedEvents rows; lag=$consumerLag" `
            -PercentComplete ([Math]::Min(100, 100 * $databaseRows / $ExpectedEvents))
        if ($databaseRows -lt $ExpectedEvents -or $consumerLag -ne 0) {
            Start-Sleep -Milliseconds 500
        }
    } while ($databaseRows -lt $ExpectedEvents -or $consumerLag -ne 0)
    Write-Progress -Activity 'Consuming product-view events' -Completed

    $completedAt = [DateTimeOffset]::UtcNow
    $durationSeconds = [Math]::Round(($completedAt - $startedAt).TotalSeconds, 3)
    $throughput = [Math]::Round($ExpectedEvents / $durationSeconds, 1)
    $metrics = (Invoke-WebRequest -UseBasicParsing "http://localhost:$appPort/actuator/prometheus").Content
    $failureCount = Get-PrometheusCounter $metrics 'recoflow_kafka_consumer_failures_total'
    $dltMetricCount = Get-PrometheusCounter $metrics 'recoflow_kafka_dlt_messages_total'
    $dltTopicCount = Get-TopicRecordCount $kafkaContainer "$topic.DLT"

    $result = [ordered]@{
        measuredAt = $completedAt.ToString('o')
        source = 'product-views.json'
        events = $ExpectedEvents
        persistedRows = $databaseRows
        processingSeconds = $durationSeconds
        eventsPerSecond = $throughput
        finalConsumerLag = $consumerLag
        consumerFailures = $failureCount
        dltMetricCount = $dltMetricCount
        dltTopicRecords = $dltTopicCount
        kafkaPartitions = 1
        producerInterval = '1ms'
        measurementBoundary = 'producer streaming start -> database row count reached target and consumer lag reached zero'
    }

    New-Item -ItemType Directory -Path (Split-Path -Parent $resultFile) -Force | Out-Null
    $result | ConvertTo-Json | Set-Content -LiteralPath $resultFile -Encoding utf8
    $result | Format-Table -AutoSize
    Write-Host "Result saved to $resultFile"
    $completed = $true
}
finally {
    if (-not $KeepRunning) {
        Invoke-Compose down --volumes --remove-orphans
    } elseif ($completed) {
        Write-Host "Load-test stack is still running under Compose project '$composeProject'."
    }
}
