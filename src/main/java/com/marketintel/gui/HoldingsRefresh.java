package com.marketintel.gui;

/**
 * Whether Home is already showing a book.
 * A later price load keeps that book until the new quotes return.
 * Sign-off and an empty portfolio drop it.
 */
public final class HoldingsRefresh {

    private boolean bookOnScreen;

    public void clear() {
        bookOnScreen = false;
    }

    public boolean retainWhileLoading() {
        return bookOnScreen;
    }

    public void showedBook() {
        bookOnScreen = true;
    }
}
