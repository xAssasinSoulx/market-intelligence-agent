package com.marketintel.providers;

public interface LLMProvider {

    String generate(String prompt);

    /**
     * A short reply. Adapters that can limit tokens or skip extended
     * reasoning should do so here. The default is a normal generation.
     */
    default String generateBrief(String prompt) {
        return generate(prompt);
    }
}