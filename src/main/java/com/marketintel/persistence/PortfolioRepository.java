package com.marketintel.persistence;

import com.marketintel.model.Portfolio;

public interface PortfolioRepository {

    Portfolio loadForUser(long userId);

    void saveForUser(
            long userId,
            Portfolio portfolio
    );
}