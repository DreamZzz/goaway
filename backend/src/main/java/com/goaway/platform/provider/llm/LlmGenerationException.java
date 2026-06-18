package com.goaway.platform.provider.llm;

public class LlmGenerationException extends RuntimeException {
    public LlmGenerationException(String message) {
        super(message);
    }

    public LlmGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
