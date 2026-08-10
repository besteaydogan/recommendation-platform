package com.besteaydogan.recoflow.messaging.producer;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.besteaydogan.recoflow.messaging.model.ProductViewEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductViewFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductViewFileReader.class);

    private final ObjectMapper objectMapper;

    public ProductViewFileReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<ProductViewEvent> read(String filePath) {
        Path path = Path.of(filePath);
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("Product-view file is missing or unreadable: " + path);
        }

        List<ProductViewEvent> events = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            long lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                parseLine(line, lineNumber).ifPresent(events::add);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read product-view file: " + path, exception);
        }
        return List.copyOf(events);
    }

    private java.util.Optional<ProductViewEvent> parseLine(String line, long lineNumber) {
        try {
            ProductViewEvent event = objectMapper.readValue(line, ProductViewEvent.class);
            String invalidReason = invalidReason(event);
            if (invalidReason != null) {
                LOGGER.warn("Skipping product-view source line {}: {}", lineNumber, invalidReason);
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(event);
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Skipping malformed product-view source line {}: {}",
                    lineNumber, exception.getOriginalMessage());
            return java.util.Optional.empty();
        }
    }

    private String invalidReason(ProductViewEvent event) {
        if (event == null) {
            return "record is null";
        }
        if (!"ProductView".equals(event.event())) {
            return "event must be ProductView";
        }
        if (event.messageId() == null) {
            return "messageid is required";
        }
        if (isBlank(event.userId())) {
            return "userid is required";
        }
        if (event.properties() == null || isBlank(event.properties().productId())) {
            return "properties.productid is required";
        }
        if (event.context() == null || isBlank(event.context().source())) {
            return "context.source is required";
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
