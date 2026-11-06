package com.marketintel.gui;

import com.marketintel.model.ToolResult;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class InflightQuotesTest {

    @Test
    void quoteKeyIgnoresCaseAndKeepsUsersApart() {
        assertEquals(InflightQuotes.quoteKey(7, " nvda "), InflightQuotes.quoteKey(7, "NVDA"));
        assertFalse(InflightQuotes.quoteKey(7, "NVDA").equals(InflightQuotes.quoteKey(8, "NVDA")));
        assertEquals("4:", InflightQuotes.quoteKey(4, null));
    }

    @Test
    void sharesOneFetchWhileTheFirstCallIsStillRunning() throws Exception {
        InflightQuotes quotes = new InflightQuotes();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Supplier<ToolResult> fetch = () -> {
            calls.incrementAndGet();
            inside.countDown();
            await(release);
            return ToolResult.success("shared");
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ToolResult> first = pool.submit(() -> quotes.load("7:NVDA", fetch));
            assertTrue(inside.await(2, TimeUnit.SECONDS));
            Future<ToolResult> second = pool.submit(() -> quotes.load("7:NVDA", fetch));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (quotes.waiters("7:NVDA") < 1) {
                if (System.nanoTime() > deadline) {
                    fail("second request did not join the in-flight quote");
                }
                Thread.onSpinWait();
            }
            release.countDown();

            assertEquals("shared", first.get(2, TimeUnit.SECONDS).getData());
            assertEquals("shared", second.get(2, TimeUnit.SECONDS).getData());
            assertEquals(1, calls.get());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void oneFailureIsSharedAndTheNextLoadTriesAgain() throws Exception {
        InflightQuotes quotes = new InflightQuotes();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch inside = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Supplier<ToolResult> fetch = () -> {
            int call = calls.incrementAndGet();
            if (call == 1) {
                inside.countDown();
                await(release);
                throw new IllegalStateException("vendor down");
            }
            return ToolResult.success("later");
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ToolResult> first = pool.submit(() -> quotes.load("7:SPY", fetch));
            assertTrue(inside.await(2, TimeUnit.SECONDS));
            Future<ToolResult> second = pool.submit(() -> quotes.load("7:SPY", fetch));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (quotes.waiters("7:SPY") < 1) {
                if (System.nanoTime() > deadline) {
                    fail("second request did not join the failed quote");
                }
                Thread.onSpinWait();
            }
            release.countDown();

            ExecutionException firstFailure = assertThrows(ExecutionException.class, () ->
                    first.get(2, TimeUnit.SECONDS));
            ExecutionException secondFailure = assertThrows(ExecutionException.class, () ->
                    second.get(2, TimeUnit.SECONDS));
            assertEquals("vendor down", firstFailure.getCause().getMessage());
            assertEquals("vendor down", secondFailure.getCause().getMessage());
            assertEquals(1, calls.get());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }

        ToolResult again = quotes.load("7:SPY", fetch);
        assertEquals("later", again.getData());
        assertEquals(2, calls.get());
    }

    @Test
    void aFinishedQuoteIsNotReused() {
        InflightQuotes quotes = new InflightQuotes();
        AtomicInteger calls = new AtomicInteger();
        Supplier<ToolResult> fetch = () -> ToolResult.success(calls.incrementAndGet());

        assertEquals(1, quotes.load("7:QQQ", fetch).getData());
        assertEquals(2, quotes.load("7:QQQ", fetch).getData());
        assertEquals(3, quotes.load("8:QQQ", fetch).getData());
    }

    @Test
    void differentSymbolsFetchSeparately() throws Exception {
        InflightQuotes quotes = new InflightQuotes();
        AtomicInteger calls = new AtomicInteger();
        CountDownLatch bothInside = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        Supplier<ToolResult> fetch = () -> {
            calls.incrementAndGet();
            bothInside.countDown();
            await(release);
            return ToolResult.success("ok");
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<ToolResult> spy = pool.submit(() -> quotes.load("7:SPY", fetch));
            Future<ToolResult> nvda = pool.submit(() -> quotes.load("7:NVDA", fetch));
            assertTrue(bothInside.await(2, TimeUnit.SECONDS));
            release.countDown();
            assertEquals("ok", spy.get(2, TimeUnit.SECONDS).getData());
            assertEquals("ok", nvda.get(2, TimeUnit.SECONDS).getData());
            assertEquals(2, calls.get());
        } finally {
            release.countDown();
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for the test latch");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted", interrupted);
        }
    }
}
