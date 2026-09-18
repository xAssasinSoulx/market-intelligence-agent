package com.marketintel.services;

import com.marketintel.model.ToolRequest;
import com.marketintel.model.ValidationResult;

import java.math.BigDecimal;

public class RequestValidator {

    public ValidationResult validateSymbol(
            String symbol) {

        if (symbol == null
                || symbol.isBlank()) {

            return ValidationResult.invalid(
                    "Symbol cannot be blank."
            );
        }

        String normalized =
                symbol.trim().toUpperCase();

        if (!normalized.matches(
                "[A-Z0-9][A-Z0-9.\\-]{0,14}"
        )) {

            return ValidationResult.invalid(
                    "Invalid security symbol: "
                            + symbol
            );
        }

        return ValidationResult.valid();
    }

    public ValidationResult validateToolRequest(
            ToolRequest request) {

        if (request == null) {

            return ValidationResult.invalid(
                    "Tool request cannot be null."
            );
        }

        if (request.getUserId() <= 0) {

            return ValidationResult.invalid(
                    "Invalid user ID."
            );
        }

        return ValidationResult.valid();
    }

    public ValidationResult validateQuantity(
            BigDecimal quantity) {

        if (quantity == null
                || quantity.compareTo(
                BigDecimal.ZERO
        ) <= 0) {

            return ValidationResult.invalid(
                    "Quantity must be greater than zero."
            );
        }

        return ValidationResult.valid();
    }
}