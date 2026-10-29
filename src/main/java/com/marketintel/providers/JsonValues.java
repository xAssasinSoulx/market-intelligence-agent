package com.marketintel.providers;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.List;

final class JsonValues {

    private JsonValues() {
    }

    static BigDecimal decimal(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = field == null ? node : node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        String text = value.asText("").trim();
        if (text.isBlank()
                || text.equalsIgnoreCase("none")
                || text.equals("-")) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    static String text(JsonNode node, String field) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || !value.isValueNode()) {
            return "";
        }
        return value.asText("").trim();
    }

    static String errorMessage(JsonNode node) {
        if (node == null || !node.isObject()) {
            return "";
        }
        for (String field : List.of("Error Message", "error", "message", "Information", "Note")) {
            String text = text(node, field);
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }
}
