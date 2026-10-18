
package com.marketintel.providers;

public class LLMException extends RuntimeException {

    public LLMException(String message) {
        super(message);
    }

    public LLMException(
            String message,
            Throwable cause) {
        super(message, cause);
    }
}
