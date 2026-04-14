package com.dni.apitest.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

/**
 * Loads {@code config.properties} from the test classpath; system properties override file values.
 */
public final class TestConfig {

    private static final String BASE_URI_KEY = "base.uri";
    private static final String LYTICS_BASE_URI_KEY = "lytics.base.uri";
    private static final String LYTICS_ORG_UID_KEY = "lytics.organization.uid";
    /** Well-formed organization UID that must not exist (negative tests). */
    private static final String LYTICS_ORG_UID_NONEXISTING_KEY = "lytics.organization.uid.nonexisting";
    private static final String LYTICS_AUTH_TOKEN_KEY = "lytics.auth.token";
    /**
     * Optional user UID sent as {@code x-user-uid} on requests that need it (e.g. POST before a negative PUT that
     * omits the header).
     */
    private static final String LYTICS_USER_UID_KEY = "lytics.user.uid";
    /** Optional; used by tests that need a token the API treats as expired. */
    private static final String LYTICS_EXPIRED_AUTH_TOKEN_KEY = "lytics.auth.token.expired";

    private static final String LYTICS_API_VERSION_KEY = "lytics.api.version";
    /**
     * Optional SLA (milliseconds) for GET /projects response-time assertions. System property overrides file;
     * when unset, {@link #lyticsGetMaxResponseTimeMs()} returns 30_000 ms.
     */
    private static final String LYTICS_GET_MAX_RESPONSE_TIME_MS_KEY = "lytics.api.get.max.response.time.ms";

    private static final String CONFIG_FILE = "config.properties";

    private static final long DEFAULT_GET_MAX_RESPONSE_TIME_MS = 30_000L;

    private static volatile Properties cachedProps;

    private TestConfig() {}

    public static String baseUri() {
        return requireProperty(BASE_URI_KEY);
    }

    public static String lyticsBaseUri() {
        return requireProperty(LYTICS_BASE_URI_KEY);
    }

    public static String lyticsOrganizationUid() {
        return requireProperty(LYTICS_ORG_UID_KEY);
    }

    /**
     * Organization UID in valid format but not provisioned in the target environment (e.g. TC_071). Override with
     * {@code -Dlytics.organization.uid.nonexisting=...} if needed.
     */
    public static String lyticsNonExistingOrganizationUid() {
        return requireProperty(LYTICS_ORG_UID_NONEXISTING_KEY);
    }

    public static String lyticsAuthToken() {
        return requireProperty(LYTICS_AUTH_TOKEN_KEY);
    }

    /**
     * Optional Lytics user UID for the {@code x-user-uid} header. Set {@code lytics.user.uid} in {@code config.properties}
     * or {@code -Dlytics.user.uid=} when an environment requires it on create but must omit it on a negative PUT test.
     */
    public static Optional<String> lyticsUserUid() {
        String fromSys = System.getProperty(LYTICS_USER_UID_KEY);
        if (fromSys != null && !fromSys.isBlank()) {
            return Optional.of(fromSys.trim());
        }
        String fromFile = properties().getProperty(LYTICS_USER_UID_KEY);
        if (fromFile != null && !fromFile.isBlank()) {
            return Optional.of(fromFile.trim());
        }
        return Optional.empty();
    }

    /**
     * Expired Lytics authtoken for negative tests. Set system property {@code lytics.auth.token.expired} or the same
     * key in {@code config.properties}; blank or absent yields empty.
     */
    public static Optional<String> lyticsExpiredAuthToken() {
        String fromSys = System.getProperty(LYTICS_EXPIRED_AUTH_TOKEN_KEY);
        if (fromSys != null && !fromSys.isBlank()) {
            return Optional.of(fromSys.trim());
        }
        String fromFile = properties().getProperty(LYTICS_EXPIRED_AUTH_TOKEN_KEY);
        if (fromFile != null && !fromFile.isBlank()) {
            return Optional.of(fromFile.trim());
        }
        return Optional.empty();
    }

    /** CS API version header value (e.g. {@code 1}). */
    public static String lyticsApiVersion() {
        return requireProperty(LYTICS_API_VERSION_KEY);
    }

    /**
     * Maximum acceptable round-trip time in milliseconds for GET /projects in response-time tests.
     * Configure with {@code lytics.api.get.max.response.time.ms} in {@code config.properties} or {@code -D} override.
     */
    public static long lyticsGetMaxResponseTimeMs() {
        String fromSys = System.getProperty(LYTICS_GET_MAX_RESPONSE_TIME_MS_KEY);
        String raw =
                (fromSys != null && !fromSys.isBlank())
                        ? fromSys.trim()
                        : Optional.ofNullable(properties().getProperty(LYTICS_GET_MAX_RESPONSE_TIME_MS_KEY))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .orElse(null);
        if (raw == null) {
            return DEFAULT_GET_MAX_RESPONSE_TIME_MS;
        }
        try {
            long v = Long.parseLong(raw);
            if (v <= 0) {
                throw new IllegalStateException(
                        LYTICS_GET_MAX_RESPONSE_TIME_MS_KEY + " must be positive, got: " + raw);
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid " + LYTICS_GET_MAX_RESPONSE_TIME_MS_KEY + ": " + raw, e);
        }
    }

    private static String requireProperty(String key) {
        String fromSys = System.getProperty(key);
        if (fromSys != null && !fromSys.isBlank()) {
            return fromSys.trim();
        }
        String fromFile = properties().getProperty(key);
        if (fromFile == null || fromFile.isBlank()) {
            throw new IllegalStateException("Missing " + key + " in " + CONFIG_FILE + " or -D" + key);
        }
        return fromFile.trim();
    }

    private static Properties properties() {
        if (cachedProps != null) {
            return cachedProps;
        }
        synchronized (TestConfig.class) {
            if (cachedProps != null) {
                return cachedProps;
            }
            Properties props = new Properties();
            try (InputStream in = TestConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (in != null) {
                    props.load(in);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load " + CONFIG_FILE, e);
            }
            cachedProps = props;
            return cachedProps;
        }
    }
}
