package com.dni.apitest.testdata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Request payloads and expected field values for Lytics project API tests.
 */
public final class LyticsProjectTestData {

    private LyticsProjectTestData() {}


    public static final String STACK_API_KEY = "bltfc558aa1c06a6869";
    public static final String LAUNCH_PROJECT_UID = "69724ef190419e263e1fcd03";
    public static final String PERSONALIZE_PROJECT_UID = "69c0e3f34dfc30183b4e6a96";

    /**
     * Stack / launch / personalize IDs that belong to another organization; the configured test {@code
     * organization_uid} cannot resolve them — POST /projects returns CONNECTION_NOT_FOUND for each.
     */
    public static final String STACK_API_KEY_OTHER_ORGANIZATION = "blt82a04ee13c66cfa4";
    public static final String LAUNCH_PROJECT_UID_OTHER_ORGANIZATION = "6995c9335eae15cd5a8883b4";
    public static final String PERSONALIZE_PROJECT_UID_OTHER_ORGANIZATION = "692fff71cdd1b418c16786bc";
    public static final String VALID_PROJECT_NAME = "DNI Test POST";

    /** Project name containing special characters for valid-name POST /projects tests. */
    public static final String PROJECT_NAME_WITH_SPECIAL_CHARS = "Proj@#123";

    /** Project name containing digits for valid-name POST /projects tests. */
    public static final String PROJECT_NAME_WITH_NUMBERS = "DNI123";

    /**
     * Same words as {@code "Dni Test"} in all-lowercase — valid-name POST /projects tests for casing
     * variants.
     */
    public static final String PROJECT_NAME_LOWERCASE_DNI_TEST = "dni test";

    /**
     * Request name with leading/trailing spaces; API persists the trimmed value (e.g. DNI Test).
     */
    public static final String PROJECT_NAME_LEADING_TRAILING_SPACES = " DNI Test ";

    /** API max length for project {@code name} (see {@link #PROJECT_NAME_EXACTLY_MAX_LENGTH}). */
    public static final int PROJECT_NAME_MAX_LENGTH = 200;

    /**
     * API max length for project {@code domain} (validation errors may expose this as maxCharacters).
     */
    public static final int PROJECT_DOMAIN_MAX_LENGTH = 200;

    /**
     * Domain string with length exactly {@link #PROJECT_DOMAIN_MAX_LENGTH} characters (boundary valid
     * domain length).
     */
    public static final String DOMAIN_EXACTLY_MAX_LENGTH =
            "dni.domain.200char."
                    + "x".repeat(PROJECT_DOMAIN_MAX_LENGTH - "dni.domain.200char.".length());

    /**
     * Domain one character over {@link #PROJECT_DOMAIN_MAX_LENGTH} (invalid for POST /projects).
     */
    public static final String DOMAIN_ONE_OVER_MAX_LENGTH = DOMAIN_EXACTLY_MAX_LENGTH + "x";

    /**
     * Project name with length exactly {@link #PROJECT_NAME_MAX_LENGTH} characters (boundary valid name).
     */
    public static final String PROJECT_NAME_EXACTLY_MAX_LENGTH =
            "DNI Test POST 200char "
                    + "X".repeat(PROJECT_NAME_MAX_LENGTH - "DNI Test POST 200char ".length());

    /**
     * Exactly {@link #PROJECT_NAME_MAX_LENGTH} characters: long string with spaces and symbols (valid
     * boundary).
     */
    public static final String PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX =
            longSpacesSymbolsNameUpToLength(PROJECT_NAME_MAX_LENGTH);

    /**
     * Project name one character over {@link #PROJECT_NAME_MAX_LENGTH} (invalid for POST /projects).
     */
    public static final String PROJECT_NAME_ONE_OVER_MAX_LENGTH =
            PROJECT_NAME_EXACTLY_MAX_LENGTH + "X";
    /** Intentionally empty project name for negative API tests. */
    public static final String EMPTY_PROJECT_NAME = "";
    /** Project name that is only whitespace (single space) for negative API tests. */
    public static final String SPACE_ONLY_PROJECT_NAME = " ";
    public static final String VALID_DOMAIN = "www.google.com";

    /** Valid domain without subdomain (e.g. for POST /projects domain format tests). */
    public static final String EXAMPLE_COM_DOMAIN = "example.com";

    /** Same host as {@link #EXAMPLE_COM_DOMAIN} in uppercase (POST body casing tests). */
    public static final String EXAMPLE_COM_DOMAIN_UPPERCASE = "EXAMPLE.COM";

    /** Valid domain with subdomain for POST /projects domain format tests. */
    public static final String SUBDOMAIN_EXAMPLE_COM = "sub.example.com";

    /**
     * Domain with a numeric first label (e.g. {@code 123.com}) for POST /projects domain format tests.
     */
    public static final String NUMERIC_DOMAIN_123_COM = "123.com";

    /**
     * Hyphenated domain label (e.g. {@code my-site.com}) for POST /projects domain format tests.
     */
    public static final String HYPHENATED_DOMAIN_MY_SITE_COM = "my-site.com";

    /** Intentionally empty domain string for negative API tests. */
    public static final String EMPTY_DOMAIN = "";

    /** Domain that is only whitespace (single space) for negative API tests. */
    public static final String SPACE_ONLY_DOMAIN = " ";

    /** Invalid domain format (no dot / not a hostname shape) for negative API tests. */
    public static final String INVALID_DOMAIN_FORMAT_ABC = "abc";

    /**
     * Domain string with no TLD (single label, e.g. a hostname shape without a public suffix like
     * {@code .com}).
     */
    public static final String DOMAIN_MISSING_TLD = "example";

    /** Domain containing disallowed special characters (e.g. {@code $}) for negative API tests. */
    public static final String DOMAIN_WITH_SPECIAL_CHARS = "exa$mple.com";

    /** Domain containing spaces (invalid hostname) for negative API tests. */
    public static final String DOMAIN_WITH_SPACES = "example .com";

    /** Domain whose first label starts with a hyphen (invalid hostname) for negative API tests. */
    public static final String DOMAIN_STARTING_WITH_HYPHEN = "-example.com";

    /**
     * Domain whose label ends with a hyphen before the next dot (invalid hostname) for negative API
     * tests.
     */
    public static final String DOMAIN_ENDING_WITH_HYPHEN = "example-.com";

    /** Domain with consecutive dots (invalid hostname) for negative API tests. */
    public static final String DOMAIN_CONSECUTIVE_DOTS = "example..com";

    /** Full URL value where a bare domain is expected (invalid for negative API tests). */
    public static final String DOMAIN_AS_HTTPS_URL = "https://example.com";

    /** Request domain with leading/trailing spaces; API may trim to {@link #EXAMPLE_COM_DOMAIN}. */
    public static final String DOMAIN_LEADING_TRAILING_SPACES = " example.com ";

    /** Default description used by most POST /projects payloads in this suite. */
    public static final String VALID_DESCRIPTION = "desc";

    /** Example valid description string for positive POST /projects description tests. */
    public static final String SAMPLE_PROJECT_DESCRIPTION = "Sample project description";

    /** API max length for project {@code description} (boundary valid description length). */
    public static final int PROJECT_DESCRIPTION_MAX_LENGTH = 255;

    /**
     * Description string with length exactly {@link #PROJECT_DESCRIPTION_MAX_LENGTH} characters.
     */
    public static final String DESCRIPTION_EXACTLY_MAX_LENGTH =
            "DNI desc 255char "
                    + "x".repeat(PROJECT_DESCRIPTION_MAX_LENGTH - "DNI desc 255char ".length());

    /**
     * Exactly {@link #PROJECT_DESCRIPTION_MAX_LENGTH} characters: long prose with many spaces (boundary
     * valid description).
     */
    public static final String DESCRIPTION_VERY_LONG_WITH_SPACES_MAX =
            veryLongDescriptionWithSpacesUpToLength(PROJECT_DESCRIPTION_MAX_LENGTH);

    /** Description containing only special characters (POST /projects description format tests). */
    public static final String DESCRIPTION_SPECIAL_CHARS = "@#$$%^&*()";

    /** Description containing only digits (POST /projects description format tests). */
    public static final String DESCRIPTION_NUMERIC_ONLY = "123456";

    /** Alphanumeric plus special characters (POST /projects description format tests). */
    public static final String DESCRIPTION_ALPHANUMERIC_SPECIAL = "ProjDesc1@#$%^&*()";

    /**
     * Description one character over {@link #PROJECT_DESCRIPTION_MAX_LENGTH} (invalid for POST /projects).
     */
    public static final String DESCRIPTION_ONE_OVER_MAX_LENGTH =
            DESCRIPTION_EXACTLY_MAX_LENGTH + "x";

    /** Intentionally empty description for negative API tests. */
    public static final String EMPTY_DESCRIPTION = "";

    /** Description that is only whitespace (single space) for negative API tests. */
    public static final String SPACE_ONLY_DESCRIPTION = " ";


    public static final String EMPTY_STACK_API_KEY = "";

    /**
     * Non-existent stack API key value (wrong format / unknown to org) for negative POST /projects
     * tests — API returns {@code CONNECTION_NOT_FOUND} on {@code connections.stackApiKeys}.
     */
    public static final String INVALID_STACK_API_KEY_NOT_FOUND = "abc";
    /**
     * Unknown launch / personalize UID for negative POST /projects tests — API returns
     * CONNECTION_NOT_FOUND.
     */
    public static final String INVALID_LAUNCH_PROJECT_UID_NOT_FOUND = "abc";
    public static final String INVALID_PERSONALIZE_PROJECT_UID_NOT_FOUND = "abc";
    public static final String EMPTY_LAUNCH_PROJECT_UID = "";
    public static final String EMPTY_PERSONALIZE_PROJECT_UID = "";

    public static Map<String, Object> defaultConnections() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        connections.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return connections;
    }

    /**
     * Full {@code connections} object using {@link #STACK_API_KEY_OTHER_ORGANIZATION} and related UIDs
     * — not valid for the test org.
     */
    public static Map<String, Object> connectionsBelongingToDifferentOrganization() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY_OTHER_ORGANIZATION));
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID_OTHER_ORGANIZATION));
        connections.put(
                "personalizeProjectUids",
                Arrays.asList(PERSONALIZE_PROJECT_UID_OTHER_ORGANIZATION));
        return connections;
    }

    /**
     * POST /projects with {@link #connectionsBelongingToDifferentOrganization()} (unique {@code name}
     * per call).
     */
    public static Map<String, Object> projectCreatePayloadWithDifferentOrganizationConnections(
            String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", connectionsBelongingToDifferentOrganization());
        return body;
    }

    /**
     * Connections with only a valid stack API key; launch and personalize lists are empty (same shape
     * as a minimal POST /projects body where no Launch or Personalize projects are linked).
     */
    public static Map<String, Object> connectionsOnlyStackApiKeys() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        connections.put("launchProjectUids", Collections.emptyList());
        connections.put("personalizeProjectUids", Collections.emptyList());
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsOnlyStackApiKeys()} — only stack API keys are
     * populated.
     */
    public static Map<String, Object> projectCreatePayloadWithConnectionsOnlyStackApiKeys(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsOnlyStackApiKeys());
        return body;
    }

    /**
     * Connections with only valid launch project UIDs; {@code stackApiKeys} and {@code
     * personalizeProjectUids} are omitted from the JSON object (not sent as empty arrays).
     */
    public static Map<String, Object> connectionsOnlyLaunchProjectUidsOtherKeysAbsent() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsOnlyLaunchProjectUidsOtherKeysAbsent()} — only launch
     * UIDs populated; stack and personalize connection keys absent.
     */
    public static Map<String, Object> projectCreatePayloadWithConnectionsOnlyLaunchProjectUids(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsOnlyLaunchProjectUidsOtherKeysAbsent());
        return body;
    }

    /**
     * Connections with only valid personalize project UIDs; {@code stackApiKeys} and {@code
     * launchProjectUids} are omitted from the JSON object (not sent as empty arrays).
     */
    public static Map<String, Object> connectionsOnlyPersonalizeProjectUidsOtherKeysAbsent() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsOnlyPersonalizeProjectUidsOtherKeysAbsent()} — only
     * personalize UIDs populated; stack and launch connection keys absent.
     */
    public static Map<String, Object> projectCreatePayloadWithConnectionsOnlyPersonalizeProjectUids(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsOnlyPersonalizeProjectUidsOtherKeysAbsent());
        return body;
    }

    /**
     * Full connections like {@link #defaultConnections()} but {@code launchProjectUids} mixes the valid
     * launch UID with an empty string (stack and personalize arrays hold single valid IDs each).
     */
    public static Map<String, Object>
            connectionsWithStackPersonalizeAndLaunchIncludingEmptyString() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        connections.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        connections.put(
                "launchProjectUids",
                Arrays.asList(LAUNCH_PROJECT_UID, EMPTY_LAUNCH_PROJECT_UID));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsWithStackPersonalizeAndLaunchIncludingEmptyString()}.
     */
    public static Map<String, Object>
            projectCreatePayloadWithStackPersonalizeAndLaunchIncludingEmptyString(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithStackPersonalizeAndLaunchIncludingEmptyString());
        return body;
    }

    /**
     * Full connections like {@link #defaultConnections()} but {@code personalizeProjectUids} mixes the
     * valid personalize UID with an empty string (stack and launch arrays hold single valid IDs each).
     */
    public static Map<String, Object>
            connectionsWithStackLaunchAndPersonalizeIncludingEmptyString() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        connections.put(
                "personalizeProjectUids",
                Arrays.asList(PERSONALIZE_PROJECT_UID, EMPTY_PERSONALIZE_PROJECT_UID));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsWithStackLaunchAndPersonalizeIncludingEmptyString()}.
     */
    public static Map<String, Object>
            projectCreatePayloadWithStackLaunchAndPersonalizeIncludingEmptyString(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithStackLaunchAndPersonalizeIncludingEmptyString());
        return body;
    }

    /**
     * Valid launch and personalize UIDs with an empty {@code stackApiKeys} array (no stack keys linked;
     * same shape as manual POST /projects with {@code "stackApiKeys": []}).
     */
    public static Map<String, Object> connectionsWithLaunchPersonalizeAndEmptyStackApiKeys() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Collections.emptyList());
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        connections.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsWithLaunchPersonalizeAndEmptyStackApiKeys()}.
     */
    public static Map<String, Object> projectCreatePayloadWithLaunchPersonalizeAndEmptyStackApiKeys(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithLaunchPersonalizeAndEmptyStackApiKeys());
        return body;
    }

    /**
     * Each connection array is a single empty string (same shape as manual POST with {@code
     * "stackApiKeys": [""], "launchProjectUids": [""], "personalizeProjectUids": [""]}).
     */
    public static Map<String, Object>
            connectionsWithAllConnectionArraysContainingOnlyEmptyStrings() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(EMPTY_STACK_API_KEY));
        connections.put("launchProjectUids", Arrays.asList(EMPTY_LAUNCH_PROJECT_UID));
        connections.put("personalizeProjectUids", Arrays.asList(EMPTY_PERSONALIZE_PROJECT_UID));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsWithAllConnectionArraysContainingOnlyEmptyStrings()}.
     */
    public static Map<String, Object>
            projectCreatePayloadWithAllConnectionArraysContainingOnlyEmptyStrings(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithAllConnectionArraysContainingOnlyEmptyStrings());
        return body;
    }

    /**
     * {@code stackApiKeys} contains a value that is not a known stack connection (e.g. {@link
     * #INVALID_STACK_API_KEY_NOT_FOUND}); launch and personalize arrays use the standard valid UIDs.
     */
    public static Map<String, Object> connectionsWithInvalidStackApiKeyAndValidLaunchPersonalize() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(INVALID_STACK_API_KEY_NOT_FOUND));
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        connections.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsWithInvalidStackApiKeyAndValidLaunchPersonalize()}.
     */
    public static Map<String, Object>
            projectCreatePayloadWithInvalidStackApiKeyAndValidLaunchPersonalize(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithInvalidStackApiKeyAndValidLaunchPersonalize());
        return body;
    }

    public static Map<String, Object>
            connectionsWithValidStackPersonalizeAndInvalidLaunchProjectUid() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        connections.put("launchProjectUids", Arrays.asList(INVALID_LAUNCH_PROJECT_UID_NOT_FOUND));
        connections.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return connections;
    }

    public static Map<String, Object>
            projectCreatePayloadWithValidStackPersonalizeAndInvalidLaunchProjectUid(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithValidStackPersonalizeAndInvalidLaunchProjectUid());
        return body;
    }

    public static Map<String, Object>
            connectionsWithValidStackLaunchAndInvalidPersonalizeProjectUid() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        connections.put(
                "personalizeProjectUids",
                Arrays.asList(INVALID_PERSONALIZE_PROJECT_UID_NOT_FOUND));
        return connections;
    }

    public static Map<String, Object>
            projectCreatePayloadWithValidStackLaunchAndInvalidPersonalizeProjectUid(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithValidStackLaunchAndInvalidPersonalizeProjectUid());
        return body;
    }

    /**
     * Each connection array lists the same ID twice; the API response keeps one value per field
     * (deduplication).
     */
    public static Map<String, Object> connectionsWithDuplicateIdsPerField() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY, STACK_API_KEY));
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID, LAUNCH_PROJECT_UID));
        connections.put(
                "personalizeProjectUids",
                Arrays.asList(PERSONALIZE_PROJECT_UID, PERSONALIZE_PROJECT_UID));
        return connections;
    }

    /**
     * Only {@code stackApiKeys} lists the same key twice; launch and personalize each have a single
     * valid UID.
     */
    public static Map<String, Object> connectionsWithDuplicateStackApiKeysOnly() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(STACK_API_KEY, STACK_API_KEY));
        connections.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        connections.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsWithDuplicateStackApiKeysOnly()}.
     */
    public static Map<String, Object> projectCreatePayloadWithDuplicateStackApiKeysOnly(
            String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", connectionsWithDuplicateStackApiKeysOnly());
        return body;
    }

    /**
     * POST /projects body with duplicate entries in each connection array (see {@link
     * #connectionsWithDuplicateIdsPerField()}).
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithDuplicateConnectionEntries(
            String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", connectionsWithDuplicateIdsPerField());
        return body;
    }

    /**
     * Connection arrays contain JSON numbers instead of strings (e.g. {@code "launchProjectUids":
     * [567878]}). Invalid curl fragments like {@code [njij]} are not valid JSON; this is the
     * serializable equivalent for type-mismatch tests.
     */
    public static Map<String, Object> connectionsWithNonStringConnectionValues() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", Arrays.asList(12_345));
        connections.put("launchProjectUids", Arrays.asList(567_878));
        connections.put("personalizeProjectUids", Arrays.asList(91_456_576));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsWithNonStringConnectionValues()}.
     */
    public static Map<String, Object> projectCreatePayloadWithNonStringConnectionValues(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithNonStringConnectionValues());
        return body;
    }

    /**
     * {@link #defaultConnections()} plus an unknown field (e.g. {@code automate: [""]}) — API typically
     * ignores extra keys and returns only known connection fields.
     */
    public static Map<String, Object> connectionsWithExtraUnknownField() {
        Map<String, Object> connections = new HashMap<>(defaultConnections());
        connections.put("automate", Arrays.asList(""));
        return connections;
    }

    /**
     * POST /projects body with {@link #connectionsWithExtraUnknownField()}.
     */
    public static Map<String, Object> projectCreatePayloadWithExtraUnknownFieldInConnections(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithExtraUnknownField());
        return body;
    }

    /**
     * Same keys as {@link #defaultConnections()} but each value is a single string (not a one-element
     * array). Used to verify the API accepts scalar connection IDs in the request body.
     */
    public static Map<String, Object> connectionsWithSingleScalarValues() {
        Map<String, Object> connections = new HashMap<>();
        connections.put("stackApiKeys", STACK_API_KEY);
        connections.put("launchProjectUids", LAUNCH_PROJECT_UID);
        connections.put("personalizeProjectUids", PERSONALIZE_PROJECT_UID);
        return connections;
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code connections} uses scalar strings per
     * field instead of arrays (see {@link #connectionsWithSingleScalarValues()}).
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithScalarConnectionValues() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", VALID_PROJECT_NAME);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", connectionsWithSingleScalarValues());
        return body;
    }

    /**
     * Body for POST /projects with a given {@code name}, {@code domain}, description, and connections.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithNameAndDomain(
            String name, String domain) {
        return validFullProjectCreatePayloadWithNameDomainAndDescription(
                name, domain, VALID_DESCRIPTION);
    }

    /**
     * Body for POST /projects with explicit {@code description}; use {@link
     * #projectCreatePayloadWithoutDescriptionField} when the {@code description} key must be omitted
     * from JSON.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithNameDomainAndDescription(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", defaultConnections());
        return body;
    }

    /**
     * Same as a valid full payload but omits {@code description} (field absent from JSON). Caller supplies
     * {@code name} and {@code domain} (e.g. unique name per test).
     */
    public static Map<String, Object> projectCreatePayloadWithoutDescriptionField(
            String name, String domain) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("connections", defaultConnections());
        return body;
    }

    /**
     * POST /projects body with {@code name}, {@code domain}, and {@code description} only — the {@code
     * connections} key is omitted (API responds with empty {@code stackApiKeys}, {@code
     * launchProjectUids}, {@code personalizeProjectUids}).
     */
    public static Map<String, Object> projectCreatePayloadWithoutConnectionsField(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        return body;
    }

    /**
     * POST /projects with {@code connections} present but empty JSON object {@code {}}. The API
     * currently responds with HTTP 500 (omitting the field or sending populated connections succeeds).
     */
    public static Map<String, Object> projectCreatePayloadWithEmptyConnectionsObject(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", new HashMap<String, Object>());
        return body;
    }

    /** Body for POST /projects with a given {@code name} and default {@link #VALID_DOMAIN}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithName(String name) {
        return validFullProjectCreatePayloadWithNameAndDomain(name, VALID_DOMAIN);
    }

    /** Body for POST /projects with name, domain, description, and connections. */
    public static Map<String, Object> validFullProjectCreatePayload() {
        return validFullProjectCreatePayloadWithName(VALID_PROJECT_NAME);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #SUBDOMAIN_EXAMPLE_COM}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithValidSubdomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, SUBDOMAIN_EXAMPLE_COM);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #EXAMPLE_COM_DOMAIN_UPPERCASE}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithUppercaseDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, EXAMPLE_COM_DOMAIN_UPPERCASE);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_LEADING_TRAILING_SPACES}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithDomainLeadingTrailingSpaces() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_LEADING_TRAILING_SPACES);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #NUMERIC_DOMAIN_123_COM}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithNumericDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, NUMERIC_DOMAIN_123_COM);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #HYPHENATED_DOMAIN_MY_SITE_COM}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithHyphenatedDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, HYPHENATED_DOMAIN_MY_SITE_COM);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but omits {@code name} (field absent from
     * JSON).
     */
    public static Map<String, Object> projectCreatePayloadWithoutName() {
        Map<String, Object> body = new HashMap<>();
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but omits {@code domain} (field absent from
     * JSON).
     */
    public static Map<String, Object> projectCreatePayloadWithoutDomain() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", VALID_PROJECT_NAME);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Same as {@link #validFullProjectCreatePayload()} but {@code domain} is JSON {@code null}. */
    public static Map<String, Object> projectCreatePayloadWithNullDomain() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", VALID_PROJECT_NAME);
        body.put("domain", null);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Same as {@link #validFullProjectCreatePayload()} but {@code domain} is an empty string. */
    public static Map<String, Object> projectCreatePayloadWithEmptyDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, EMPTY_DOMAIN);
    }

    /** Same as {@link #validFullProjectCreatePayload()} but {@code domain} is only spaces. */
    public static Map<String, Object> projectCreatePayloadWithSpaceOnlyDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, SPACE_ONLY_DOMAIN);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_EXACTLY_MAX_LENGTH}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithMaxLengthDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_EXACTLY_MAX_LENGTH);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_ONE_OVER_MAX_LENGTH}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithDomainOverMaxLength() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_ONE_OVER_MAX_LENGTH);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #INVALID_DOMAIN_FORMAT_ABC}.
     */
    public static Map<String, Object> projectCreatePayloadWithInvalidDomainFormatAbc() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, INVALID_DOMAIN_FORMAT_ABC);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_MISSING_TLD}.
     */
    public static Map<String, Object> projectCreatePayloadWithDomainMissingTld() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_MISSING_TLD);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_WITH_SPECIAL_CHARS}.
     */
    public static Map<String, Object> projectCreatePayloadWithDomainSpecialCharacters() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_WITH_SPECIAL_CHARS);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_WITH_SPACES}.
     */
    public static Map<String, Object> projectCreatePayloadWithDomainHavingSpaces() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_WITH_SPACES);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_STARTING_WITH_HYPHEN}.
     */
    public static Map<String, Object> projectCreatePayloadWithDomainStartingWithHyphen() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_STARTING_WITH_HYPHEN);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_ENDING_WITH_HYPHEN}.
     */
    public static Map<String, Object> projectCreatePayloadWithDomainEndingWithHyphen() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_ENDING_WITH_HYPHEN);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_CONSECUTIVE_DOTS}.
     */
    public static Map<String, Object> projectCreatePayloadWithDomainConsecutiveDots() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_CONSECUTIVE_DOTS);
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code domain} is {@link
     * #DOMAIN_AS_HTTPS_URL}.
     */
    public static Map<String, Object> projectCreatePayloadWithDomainAsHttpsUrl() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_AS_HTTPS_URL);
    }

    /** Same as {@link #validFullProjectCreatePayload()} but {@code name} is JSON {@code null}. */
    public static Map<String, Object> projectCreatePayloadWithNullName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", null);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Same as {@link #validFullProjectCreatePayload()} but {@code name} is an empty string. */
    public static Map<String, Object> projectCreatePayloadWithEmptyName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", EMPTY_PROJECT_NAME);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Same as {@link #validFullProjectCreatePayload()} but {@code name} is only spaces. */
    public static Map<String, Object> projectCreatePayloadWithSpaceOnlyName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", SPACE_ONLY_PROJECT_NAME);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code name} is exactly {@link
     * #PROJECT_NAME_MAX_LENGTH} chars.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithMaxLengthName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", PROJECT_NAME_EXACTLY_MAX_LENGTH);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code name} is {@link
     * #PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithLongSpacesSymbolsName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /**
     * Same as {@link #validFullProjectCreatePayload()} but {@code name} is {@link
     * #PROJECT_NAME_ONE_OVER_MAX_LENGTH}.
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithNameOverMaxLength() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", PROJECT_NAME_ONE_OVER_MAX_LENGTH);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    private static String longSpacesSymbolsNameUpToLength(int targetLen) {
        String prefix =
                "DNI long str spaces+symbols !@#$ %^&* () [] {} | : ; ' , . <> ? / ~ ` "
                        + "-= +_ ";
        String cycle = " !@#$%^&*()_+-=[]{}|;:',.<>?/~` ";
        StringBuilder b = new StringBuilder(prefix);
        for (int i = 0; b.length() < targetLen; i++) {
            b.append(cycle.charAt(i % cycle.length()));
        }
        return b.substring(0, targetLen);
    }

    /**
     * Repeats a spaced phrase to fill exactly {@code targetLen} characters (for description boundary
     * tests).
     */
    private static String veryLongDescriptionWithSpacesUpToLength(int targetLen) {
        String phrase = "Very long text including spaces ";
        StringBuilder b = new StringBuilder();
        for (int i = 0; b.length() < targetLen; i++) {
            b.append(phrase);
        }
        return b.substring(0, targetLen);
    }
}
