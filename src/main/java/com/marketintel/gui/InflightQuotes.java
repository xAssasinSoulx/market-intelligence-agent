package com.marketintel.gui;

import com.marketintel.model.ToolResult;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Shares one quote request while it is still running.
 * The tape and the home book ask for the same symbols at the same time.
 * A second caller waits for that result instead of starting another vendor
 * call. The shared request is always completed, so a waiter cannot hang
 * after the owner stops.
 */
public final class InflightQuotes {

    private final ConcurrentHashMap<String, Flight> inflight = new ConcurrentHashMap<>();

    public static String quoteKey(long userId, String symbol) {
        String normalized = symbol == null ? "" : symbol.trim().toUpperCase(Locale.ROOT);
        return userId + ":" + normalized;
    }

    public ToolResult load(String key, Supplier<ToolResult> fetch) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Quote key cannot be blank.");
        }
        if (fetch == null) {
            throw new IllegalArgumentException("Quote fetch cannot be null.");
        }

        Flight created = new Flight();
        Flight flight = inflight.putIfAbsent(key, created);
        if (flight == null) {
            return own(key, created, fetch);
        }
        flight.waiters.incrementAndGet();
        try {
            return await(flight.future);
        } finally {
            flight.waiters.decrementAndGet();
        }
    }

    int waiters(String key) {
        Flight flight = inflight.get(key);
        return flight == null ? 0 : flight.waiters.get();
    }

    private ToolResult own(String key, Flight flight, Supplier<ToolResult> fetch) {
        try {
            ToolResult result = fetch.get();
            if (result == null) {
                result = ToolResult.failure("No quote was returned.");
            }
            flight.future.complete(result);
            return result;
        } catch (Throwable thrown) {
            flight.future.completeExceptionally(thrown);
            throw asRuntime(thrown);
        } finally {
            inflight.remove(key, flight);
            if (!flight.future.isDone()) {
                flight.future.completeExceptionally(
                        new IllegalStateException("Quote request ended early.")
                );
            }
        }
    }

    private static ToolResult await(CompletableFuture<ToolResult> future) {
        try {
            return future.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Quote request was interrupted.", interrupted);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            throw asRuntime(cause);
        }
    }

    private static RuntimeException asRuntime(Throwable thrown) {
        if (thrown instanceof RuntimeException runtime) {
            return runtime;
        }
        if (thrown instanceof Error error) {
            throw error;
        }
        String message = thrown.getMessage();
        return new IllegalStateException(
                message == null || message.isBlank() ? "Quote request failed." : message,
                thrown
        );
    }

    private static final class Flight {
        private final CompletableFuture<ToolResult> future = new CompletableFuture<>();
        private final AtomicInteger waiters = new AtomicInteger();
    }
}
