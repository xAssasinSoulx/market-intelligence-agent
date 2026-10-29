package com.marketintel.providers;

import com.marketintel.model.FinancialStatements;

public interface FinancialStatementProvider {

    FinancialStatements getFinancialStatements(String symbol);
}
