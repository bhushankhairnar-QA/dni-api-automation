package com.dni.apitest.testdata;

/**
 * Test constants for the Lytics Projects API test suite.
 *
 * <p>This class holds only raw expected values — string literals, numeric limits, and
 * boundary-value strings.  Request payload assembly belongs in
 * {@link LyticsProjectPayloadBuilder}.
 */
public final class LyticsProjectTestData {

    private LyticsProjectTestData() {}

    // =========================================================================
    // Connection IDs — test organisation
    // =========================================================================

    public static final String STACK_API_KEY = "bltfc558aa1c06a6869";
    public static final String LAUNCH_PROJECT_UID = "69724ef190419e263e1fcd03";
    public static final String PERSONALIZE_PROJECT_UID = "69c0e3f34dfc30183b4e6a96";

    // =========================================================================
    // Connection IDs — different organisation (negative tests)
    // =========================================================================

    public static final String STACK_API_KEY_OTHER_ORGANIZATION = "blt82a04ee13c66cfa4";
    public static final String LAUNCH_PROJECT_UID_OTHER_ORGANIZATION = "6995c9335eae15cd5a8883b4";
    public static final String PERSONALIZE_PROJECT_UID_OTHER_ORGANIZATION = "692fff71cdd1b418c16786bc";

    // =========================================================================
    // Invalid connection IDs (negative tests)
    // =========================================================================

    /** Unknown stack API key — API returns CONNECTION_NOT_FOUND on stackApiKeys. */
    public static final String INVALID_STACK_API_KEY_NOT_FOUND = "abc";
    /** Unknown launch UID — API returns CONNECTION_NOT_FOUND on launchProjectUids. */
    public static final String INVALID_LAUNCH_PROJECT_UID_NOT_FOUND = "abc";
    /** Unknown personalize UID — API returns CONNECTION_NOT_FOUND on personalizeProjectUids. */
    public static final String INVALID_PERSONALIZE_PROJECT_UID_NOT_FOUND = "abc";

    public static final String EMPTY_STACK_API_KEY = "";
    public static final String EMPTY_LAUNCH_PROJECT_UID = "";
    public static final String EMPTY_PERSONALIZE_PROJECT_UID = "";

    // =========================================================================
    // Project name — valid values
    // =========================================================================

    public static final String VALID_PROJECT_NAME = "DNI Test POST";
    public static final String PROJECT_NAME_WITH_SPECIAL_CHARS = "Proj@#123";
    public static final String PROJECT_NAME_WITH_NUMBERS = "DNI123";
    public static final String PROJECT_NAME_LOWERCASE_DNI_TEST = "dni test";
    /** Name with leading and trailing spaces — API should trim before storing. */
    public static final String PROJECT_NAME_LEADING_TRAILING_SPACES = " DNI Test ";

    // =========================================================================
    // Project name — boundary / invalid values
    // =========================================================================

    /** API maximum length for the project {@code name} field. */
    public static final int PROJECT_NAME_MAX_LENGTH = 200;

    /** Name string of exactly {@link #PROJECT_NAME_MAX_LENGTH} characters. */
    public static final String PROJECT_NAME_EXACTLY_MAX_LENGTH =
            "DNI Test POST 200char "
                    + "X".repeat(PROJECT_NAME_MAX_LENGTH - "DNI Test POST 200char ".length());

    /** Name of exactly {@link #PROJECT_NAME_MAX_LENGTH} characters containing spaces and symbols. */
    public static final String PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX =
            longSpacesSymbolsNameUpToLength(PROJECT_NAME_MAX_LENGTH);

    /** Name one character over {@link #PROJECT_NAME_MAX_LENGTH} — triggers MAX_CHAR_LIMIT. */
    public static final String PROJECT_NAME_ONE_OVER_MAX_LENGTH =
            PROJECT_NAME_EXACTLY_MAX_LENGTH + "X";

    public static final String EMPTY_PROJECT_NAME = "";
    public static final String SPACE_ONLY_PROJECT_NAME = " ";

    // =========================================================================
    // Domain — valid values
    // =========================================================================

    public static final String VALID_DOMAIN = "www.google.com";
    public static final String EXAMPLE_COM_DOMAIN = "example.com";
    public static final String EXAMPLE_COM_DOMAIN_UPPERCASE = "EXAMPLE.COM";
    public static final String SUBDOMAIN_EXAMPLE_COM = "sub.example.com";
    public static final String NUMERIC_DOMAIN_123_COM = "123.com";
    public static final String HYPHENATED_DOMAIN_MY_SITE_COM = "my-site.com";

    // =========================================================================
    // Domain — boundary / invalid values
    // =========================================================================

    /** API maximum length for the project {@code domain} field. */
    public static final int PROJECT_DOMAIN_MAX_LENGTH = 200;

    /** Domain string of exactly {@link #PROJECT_DOMAIN_MAX_LENGTH} characters. */
    public static final String DOMAIN_EXACTLY_MAX_LENGTH =
            "dni.domain.200char."
                    + "x".repeat(PROJECT_DOMAIN_MAX_LENGTH - "dni.domain.200char.".length());

    /** Domain one character over {@link #PROJECT_DOMAIN_MAX_LENGTH} — triggers MAX_CHAR_LIMIT. */
    public static final String DOMAIN_ONE_OVER_MAX_LENGTH = DOMAIN_EXACTLY_MAX_LENGTH + "x";

    public static final String EMPTY_DOMAIN = "";
    public static final String SPACE_ONLY_DOMAIN = " ";
    /** No dot / TLD (e.g. "abc") — fails INVALID_DOMAIN. */
    public static final String INVALID_DOMAIN_FORMAT_ABC = "abc";
    public static final String DOMAIN_MISSING_TLD = "example";
    public static final String DOMAIN_WITH_SPECIAL_CHARS = "exa$mple.com";
    public static final String DOMAIN_WITH_SPACES = "example .com";
    public static final String DOMAIN_STARTING_WITH_HYPHEN = "-example.com";
    public static final String DOMAIN_ENDING_WITH_HYPHEN = "example-.com";
    public static final String DOMAIN_CONSECUTIVE_DOTS = "example..com";
    public static final String DOMAIN_AS_HTTPS_URL = "https://example.com";
    /** Domain with leading/trailing spaces — API should trim to {@link #EXAMPLE_COM_DOMAIN}. */
    public static final String DOMAIN_LEADING_TRAILING_SPACES = " example.com ";

    // =========================================================================
    // Description — valid values
    // =========================================================================

    public static final String VALID_DESCRIPTION = "desc";
    public static final String SAMPLE_PROJECT_DESCRIPTION = "Sample project description";
    public static final String DESCRIPTION_SPECIAL_CHARS = "@#$$%^&*()";
    public static final String DESCRIPTION_NUMERIC_ONLY = "123456";
    public static final String DESCRIPTION_ALPHANUMERIC_SPECIAL = "ProjDesc1@#$%^&*()";

    // =========================================================================
    // Description — boundary / invalid values
    // =========================================================================

    /** API maximum length for the project {@code description} field. */
    public static final int PROJECT_DESCRIPTION_MAX_LENGTH = 255;

    /** Description string of exactly {@link #PROJECT_DESCRIPTION_MAX_LENGTH} characters. */
    public static final String DESCRIPTION_EXACTLY_MAX_LENGTH =
            "DNI desc 255char "
                    + "x".repeat(PROJECT_DESCRIPTION_MAX_LENGTH - "DNI desc 255char ".length());

    /** 255-character description consisting mostly of spaces (boundary valid). */
    public static final String DESCRIPTION_VERY_LONG_WITH_SPACES_MAX =
            veryLongDescriptionWithSpacesUpToLength(PROJECT_DESCRIPTION_MAX_LENGTH);

    /** Description one character over {@link #PROJECT_DESCRIPTION_MAX_LENGTH} — triggers MAX_CHAR_LIMIT. */
    public static final String DESCRIPTION_ONE_OVER_MAX_LENGTH =
            DESCRIPTION_EXACTLY_MAX_LENGTH + "x";

    public static final String EMPTY_DESCRIPTION = "";
    public static final String SPACE_ONLY_DESCRIPTION = " ";

    // =========================================================================
    // Private helpers — used only to initialise the boundary-value constants above
    // =========================================================================

    private static String longSpacesSymbolsNameUpToLength(int targetLen) {
        String prefix = "DNI long str spaces+symbols !@#$ %^&* () [] {} | : ; ' , . <> ? / ~ ` -= +_ ";
        String cycle = " !@#$%^&*()_+-=[]{}|;:',.<>?/~` ";
        StringBuilder b = new StringBuilder(prefix);
        for (int i = 0; b.length() < targetLen; i++) {
            b.append(cycle.charAt(i % cycle.length()));
        }
        return b.substring(0, targetLen);
    }

    private static String veryLongDescriptionWithSpacesUpToLength(int targetLen) {
        String phrase = "Very long text including spaces ";
        StringBuilder b = new StringBuilder();
        while (b.length() < targetLen) {
            b.append(phrase);
        }
        return b.substring(0, targetLen);
    }
}
