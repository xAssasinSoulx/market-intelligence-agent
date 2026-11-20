package com.marketintel.bootstrap;

import com.marketintel.auth.AuthService;
import com.marketintel.auth.SessionManager;
import com.marketintel.model.User;

import java.util.function.Function;

/**
 * Signs a batch run in without reading a prompt.
 *
 * <p>When the task does not name an account, the local demo user
 * {@code guest} / {@code guest-demo} is created in the SQLite file
 * if needed. That password is a local demo password, not an API key.
 * {@code --user} and {@code MI_USERNAME} override it.
 */
public final class BatchSession {

    public static final String GUEST_USERNAME = "guest";

    public static final String GUEST_PASSWORD = "guest-demo";

    private BatchSession() {
    }

    public record Login(
            String username,
            boolean created,
            boolean demoAccount) {
    }

    record Resolved(
            boolean guest,
            String username,
            String password,
            boolean register) {
    }

    public static Login signIn(
            ApplicationContext context,
            CliOptions options) {

        return signIn(context, options, System::getenv);
    }

    static Login signIn(
            ApplicationContext context,
            CliOptions options,
            Function<String, String> environment) {

        Resolved resolved = resolve(options, environment);
        AuthService auth = context.getAuthService();
        SessionManager sessions = context.getSessionManager();

        if (resolved.guest()) {
            boolean created = openGuest(auth, sessions);
            return new Login(GUEST_USERNAME, created, true);
        }

        User user;
        boolean created = false;
        if (resolved.register()) {
            try {
                user = auth.register(
                        resolved.username(),
                        resolved.password()
                );
                created = true;
            } catch (IllegalArgumentException exception) {
                if (exception.getMessage() == null
                        || !exception.getMessage()
                        .contains("already registered")) {
                    throw exception;
                }
                user = auth.authenticate(
                        resolved.username(),
                        resolved.password()
                );
            }
        } else {
            user = auth.authenticate(
                    resolved.username(),
                    resolved.password()
            );
        }
        sessions.startSession(user);
        return new Login(user.getUsername(), created, false);
    }

    static Resolved resolve(
            CliOptions options,
            Function<String, String> environment) {

        if (options.register() && options.username() == null) {
            throw new IllegalArgumentException(
                    "--register requires --user."
            );
        }
        if (options.guest() && options.username() != null) {
            throw new IllegalArgumentException(
                    "Use either --guest or --user, not both."
            );
        }
        if (options.username() != null) {
            String password = firstNonBlank(
                    options.password(),
                    environment.apply("MI_PASSWORD")
            );
            if (password == null) {
                throw new IllegalArgumentException(
                        "Account " + options.username()
                                + " needs a password. Pass --password or set MI_PASSWORD."
                );
            }
            return new Resolved(
                    false,
                    options.username(),
                    password,
                    options.register()
            );
        }
        if (options.guest()
                || "1".equals(environment.apply("MI_GUEST"))) {
            return new Resolved(
                    true,
                    GUEST_USERNAME,
                    GUEST_PASSWORD,
                    false
            );
        }
        String envUser = firstNonBlank(
                environment.apply("MI_USERNAME"),
                null
        );
        if (envUser != null) {
            String password = firstNonBlank(
                    options.password(),
                    environment.apply("MI_PASSWORD")
            );
            if (password == null) {
                throw new IllegalArgumentException(
                        "MI_USERNAME is set but MI_PASSWORD is empty."
                );
            }
            return new Resolved(false, envUser, password, options.register());
        }
        return new Resolved(
                true,
                GUEST_USERNAME,
                GUEST_PASSWORD,
                false
        );
    }

    private static boolean openGuest(
            AuthService auth,
            SessionManager sessions) {

        try {
            sessions.startSession(
                    auth.authenticate(GUEST_USERNAME, GUEST_PASSWORD)
            );
            return false;
        } catch (SecurityException ignored) {
            // The demo account is missing, or a different password is stored.
        }
        try {
            sessions.startSession(
                    auth.register(GUEST_USERNAME, GUEST_PASSWORD)
            );
            return true;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "A local account named guest already exists, "
                            + "and its password is not the demo password. "
                            + "Pass --user and --password for that account."
            );
        }
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }
}
