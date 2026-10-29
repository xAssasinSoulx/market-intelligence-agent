package com.marketintel.providers;

/**
 * Reads the headquarters country published for one company.
 * A null result means the profile had no country. It is not a guess.
 */
public interface HeadquartersLookup {

    String headquartersCountry(String symbol);
}
