package com.marketintel.services;

import com.marketintel.persistence.DatabaseManager;
import com.marketintel.persistence.SQLiteConversationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConversationMemoryTest {

    @TempDir
    Path tempDirectory;

    @Test
    void keepsEachUsersLastSymbolSeparateAcrossARestart() throws Exception {
        String url = "jdbc:sqlite:" + tempDirectory.resolve("memory.db");
        ConversationMemory first = memory(url);
        insertUsers(url);
        first.remember(1, "user", "Why did NVDA move?", "NVDA", "MARKET_MOVEMENT");
        first.remember(2, "user", "Quote AAPL", "AAPL", "MARKET_QUOTE");

        ConversationMemory reopened = memory(url);

        assertEquals("NVDA", reopened.lastSymbol(1));
        assertEquals("AAPL", reopened.lastSymbol(2));
        assertEquals(1, reopened.recent(1).size());
        assertNull(reopened.lastSymbol(3));
    }

    private static ConversationMemory memory(String url) {
        DatabaseManager database = new DatabaseManager(url);
        database.initialize();
        return new ConversationMemory(new SQLiteConversationRepository(database));
    }

    private static void insertUsers(String url) throws Exception {
        DatabaseManager database = new DatabaseManager(url);
        database.initialize();
        try (Connection connection = database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO users (id, username, password_hash, created_at) VALUES
                    (1, 'one', 'hash', '2026-10-29T00:00:00Z'),
                    (2, 'two', 'hash', '2026-10-29T00:00:00Z')
                    """);
        }
    }
}
