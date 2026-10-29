package com.marketintel.providers;

import com.marketintel.model.SentimentReading;

public interface SentimentProvider {

    SentimentReading getSentiment(String symbol);
}
