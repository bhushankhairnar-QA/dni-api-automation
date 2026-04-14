package com.dni.apitest.testdata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.dni.apitest.testdata.LyticsProjectTestData.*;

/**
 * Factory class for building POST /projects request payloads used across the test suite.
 *
 * <p>All constant values (expected field values, max-length strings, invalid inputs, etc.) live
 * in {@link LyticsProjectTestData}.  This class is responsible only for assembling those
 * constants into {@code Map<String, Object>} request bodies.
 *
 * <p>Naming convention:
 * <ul>
 *   <li>{@code validFullProjectCreate*()} — payloads expected to yield 201 (or 400 duplicate); the same maps are used for PUT /projects/{uid} when a full resource body is required</li>
 *   <li>{@code projectCreatePayloadWith*()} — payloads with a specific field variation</li>
 *   <li>{@code projectCreatePayloadWithout*()} — payloads missing a required field</li>
 *   <li>{@code connections*()} — helpers that build only the {@code connections} sub-object</li>
 * </ul>
 */
public final class LyticsProjectPayloadBuilder {

    private LyticsProjectPayloadBuilder() {}

    // =========================================================================
    // Connection object builders
    // =========================================================================

    /** All three connection types populated with the default test IDs. */
    public static Map<String, Object> defaultConnections() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return c;
    }

    /**
     * All three connection types populated with {@link LyticsProjectTestData#STACK_API_KEY_AFTER_PUT_UPDATE},
     * {@link LyticsProjectTestData#LAUNCH_PROJECT_UID_AFTER_PUT_UPDATE}, and
     * {@link LyticsProjectTestData#PERSONALIZE_PROJECT_UID_AFTER_PUT_UPDATE}.
     */
    public static Map<String, Object> connectionsAfterPutUpdate() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY_AFTER_PUT_UPDATE));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID_AFTER_PUT_UPDATE));
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID_AFTER_PUT_UPDATE));
        return c;
    }

    /** All three connection types populated with IDs from a different organisation. */
    public static Map<String, Object> connectionsBelongingToDifferentOrganization() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY_OTHER_ORGANIZATION));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID_OTHER_ORGANIZATION));
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID_OTHER_ORGANIZATION));
        return c;
    }

    /** Only {@code stackApiKeys} populated; launch and personalize are empty arrays. */
    public static Map<String, Object> connectionsOnlyStackApiKeys() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        c.put("launchProjectUids", Collections.emptyList());
        c.put("personalizeProjectUids", Collections.emptyList());
        return c;
    }

    /** Only {@code launchProjectUids} populated; stackApiKeys and personalizeProjectUids absent. */
    public static Map<String, Object> connectionsOnlyLaunchProjectUidsOtherKeysAbsent() {
        Map<String, Object> c = new HashMap<>();
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        return c;
    }

    /** Only {@code personalizeProjectUids} populated; stackApiKeys and launchProjectUids absent. */
    public static Map<String, Object> connectionsOnlyPersonalizeProjectUidsOtherKeysAbsent() {
        Map<String, Object> c = new HashMap<>();
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return c;
    }

    /**
     * Stack and personalize are valid; {@code launchProjectUids} contains the valid UID
     * plus an empty string.
     */
    public static Map<String, Object> connectionsWithStackPersonalizeAndLaunchIncludingEmptyString() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID, EMPTY_LAUNCH_PROJECT_UID));
        return c;
    }

    /**
     * Stack and launch are valid; {@code personalizeProjectUids} contains the valid UID
     * plus an empty string.
     */
    public static Map<String, Object> connectionsWithStackLaunchAndPersonalizeIncludingEmptyString() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        c.put("personalizeProjectUids",
                Arrays.asList(PERSONALIZE_PROJECT_UID, EMPTY_PERSONALIZE_PROJECT_UID));
        return c;
    }

    /** Launch and personalize valid; {@code stackApiKeys} is an empty array. */
    public static Map<String, Object> connectionsWithLaunchPersonalizeAndEmptyStackApiKeys() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Collections.emptyList());
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return c;
    }

    /** Each connection array contains only an empty string. */
    public static Map<String, Object> connectionsWithAllConnectionArraysContainingOnlyEmptyStrings() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(EMPTY_STACK_API_KEY));
        c.put("launchProjectUids", Arrays.asList(EMPTY_LAUNCH_PROJECT_UID));
        c.put("personalizeProjectUids", Arrays.asList(EMPTY_PERSONALIZE_PROJECT_UID));
        return c;
    }

    /** Invalid stack API key; launch and personalize use valid default UIDs. */
    public static Map<String, Object> connectionsWithInvalidStackApiKeyAndValidLaunchPersonalize() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(INVALID_STACK_API_KEY_NOT_FOUND));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return c;
    }

    /** Valid stack and personalize; invalid launch UID. */
    public static Map<String, Object> connectionsWithValidStackPersonalizeAndInvalidLaunchProjectUid() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        c.put("launchProjectUids", Arrays.asList(INVALID_LAUNCH_PROJECT_UID_NOT_FOUND));
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return c;
    }

    /** Valid stack and launch; invalid personalize UID. */
    public static Map<String, Object> connectionsWithValidStackLaunchAndInvalidPersonalizeProjectUid() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        c.put("personalizeProjectUids", Arrays.asList(INVALID_PERSONALIZE_PROJECT_UID_NOT_FOUND));
        return c;
    }

    /** Each array lists the same ID twice (deduplication test). */
    public static Map<String, Object> connectionsWithDuplicateIdsPerField() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY, STACK_API_KEY));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID, LAUNCH_PROJECT_UID));
        c.put("personalizeProjectUids",
                Arrays.asList(PERSONALIZE_PROJECT_UID, PERSONALIZE_PROJECT_UID));
        return c;
    }

    /** Only {@code stackApiKeys} lists the same key twice; launch and personalize are single valid IDs. */
    public static Map<String, Object> connectionsWithDuplicateStackApiKeysOnly() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(STACK_API_KEY, STACK_API_KEY));
        c.put("launchProjectUids", Arrays.asList(LAUNCH_PROJECT_UID));
        c.put("personalizeProjectUids", Arrays.asList(PERSONALIZE_PROJECT_UID));
        return c;
    }

    /** Each connection array contains a JSON number instead of a string (type-mismatch test). */
    public static Map<String, Object> connectionsWithNonStringConnectionValues() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", Arrays.asList(12_345));
        c.put("launchProjectUids", Arrays.asList(567_878));
        c.put("personalizeProjectUids", Arrays.asList(91_456_576));
        return c;
    }

    /** Default connections plus an unknown {@code automate} key (ignored-field test). */
    public static Map<String, Object> connectionsWithExtraUnknownField() {
        Map<String, Object> c = new HashMap<>(defaultConnections());
        c.put("automate", Arrays.asList(""));
        return c;
    }

    /** Each connection field is a single scalar string instead of an array. */
    public static Map<String, Object> connectionsWithSingleScalarValues() {
        Map<String, Object> c = new HashMap<>();
        c.put("stackApiKeys", STACK_API_KEY);
        c.put("launchProjectUids", LAUNCH_PROJECT_UID);
        c.put("personalizeProjectUids", PERSONALIZE_PROJECT_UID);
        return c;
    }

    // =========================================================================
    // Full payload builders — standard (name + domain + description + connections)
    // =========================================================================

    /** Standard valid payload using all default field values. */
    public static Map<String, Object> validFullProjectCreatePayload() {
        return validFullProjectCreatePayloadWithName(VALID_PROJECT_NAME);
    }

    /** Valid payload with a custom {@code name}; domain, description, and connections are defaults. */
    public static Map<String, Object> validFullProjectCreatePayloadWithName(String name) {
        return validFullProjectCreatePayloadWithNameAndDomain(name, VALID_DOMAIN);
    }

    /** Valid payload with custom {@code name} and {@code domain}; description is default. */
    public static Map<String, Object> validFullProjectCreatePayloadWithNameAndDomain(
            String name, String domain) {
        return validFullProjectCreatePayloadWithNameDomainAndDescription(name, domain, VALID_DESCRIPTION);
    }

    /** Valid payload with custom {@code name}, {@code domain}, and {@code description}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithNameDomainAndDescription(
            String name, String domain, String description) {
        return validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
                name, domain, description, defaultConnections());
    }

    /**
     * Valid full-body payload with custom {@code name}, {@code domain}, {@code description}, and
     * {@code connections} (used for POST/PUT when connection sets differ from {@link #defaultConnections()}).
     */
    public static Map<String, Object> validFullProjectCreatePayloadWithNameDomainDescriptionAndConnections(
            String name, String domain, String description, Map<String, Object> connections) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connections);
        return body;
    }

    /** Valid payload using {@link LyticsProjectTestData#PROJECT_NAME_EXACTLY_MAX_LENGTH}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithMaxLengthName() {
        return validFullProjectCreatePayloadWithName(PROJECT_NAME_EXACTLY_MAX_LENGTH);
    }

    /** Payload with name one character over the max limit (triggers MAX_CHAR_LIMIT error). */
    public static Map<String, Object> validFullProjectCreatePayloadWithNameOverMaxLength() {
        return validFullProjectCreatePayloadWithName(PROJECT_NAME_ONE_OVER_MAX_LENGTH);
    }

    /** Valid payload with name = {@link LyticsProjectTestData#PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithLongSpacesSymbolsName() {
        return validFullProjectCreatePayloadWithName(PROJECT_NAME_LONG_SPACES_SYMBOLS_MAX);
    }

    /** Valid payload with domain = {@link LyticsProjectTestData#SUBDOMAIN_EXAMPLE_COM}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithValidSubdomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, SUBDOMAIN_EXAMPLE_COM);
    }

    /** Valid payload with domain = {@link LyticsProjectTestData#EXAMPLE_COM_DOMAIN_UPPERCASE}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithUppercaseDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, EXAMPLE_COM_DOMAIN_UPPERCASE);
    }

    /** Valid payload with domain = {@link LyticsProjectTestData#DOMAIN_LEADING_TRAILING_SPACES}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithDomainLeadingTrailingSpaces() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_LEADING_TRAILING_SPACES);
    }

    /** Valid payload with domain = {@link LyticsProjectTestData#NUMERIC_DOMAIN_123_COM}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithNumericDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, NUMERIC_DOMAIN_123_COM);
    }

    /** Valid payload with domain = {@link LyticsProjectTestData#HYPHENATED_DOMAIN_MY_SITE_COM}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithHyphenatedDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, HYPHENATED_DOMAIN_MY_SITE_COM);
    }

    /** Valid payload with domain = {@link LyticsProjectTestData#DOMAIN_EXACTLY_MAX_LENGTH}. */
    public static Map<String, Object> validFullProjectCreatePayloadWithMaxLengthDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, DOMAIN_EXACTLY_MAX_LENGTH);
    }

    /** Payload with domain one character over the max limit (triggers MAX_CHAR_LIMIT error). */
    public static Map<String, Object> validFullProjectCreatePayloadWithDomainOverMaxLength() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, DOMAIN_ONE_OVER_MAX_LENGTH);
    }

    /** Valid payload with scalar string connection values instead of arrays. */
    public static Map<String, Object> validFullProjectCreatePayloadWithScalarConnectionValues() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", VALID_PROJECT_NAME);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", connectionsWithSingleScalarValues());
        return body;
    }

    /** Payload with duplicate entries in every connection array. */
    public static Map<String, Object> validFullProjectCreatePayloadWithDuplicateConnectionEntries(
            String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", connectionsWithDuplicateIdsPerField());
        return body;
    }

    /** Payload with duplicate entries only in {@code stackApiKeys}. */
    public static Map<String, Object> projectCreatePayloadWithDuplicateStackApiKeysOnly(String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", connectionsWithDuplicateStackApiKeysOnly());
        return body;
    }

    // =========================================================================
    // Payload builders — missing required fields
    // =========================================================================

    /** Payload without the {@code name} field. */
    public static Map<String, Object> projectCreatePayloadWithoutName() {
        Map<String, Object> body = new HashMap<>();
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Payload without the {@code domain} field. */
    public static Map<String, Object> projectCreatePayloadWithoutDomain() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", VALID_PROJECT_NAME);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Payload without the {@code description} field (key absent). */
    public static Map<String, Object> projectCreatePayloadWithoutDescriptionField(
            String name, String domain) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Payload without the {@code connections} field (key absent). */
    public static Map<String, Object> projectCreatePayloadWithoutConnectionsField(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        return body;
    }

    // =========================================================================
    // Payload builders — null field values
    // =========================================================================

    /** Payload with {@code name} explicitly set to JSON {@code null}. */
    public static Map<String, Object> projectCreatePayloadWithNullName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", null);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Payload with {@code domain} explicitly set to JSON {@code null}. */
    public static Map<String, Object> projectCreatePayloadWithNullDomain() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", VALID_PROJECT_NAME);
        body.put("domain", null);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    // =========================================================================
    // Payload builders — invalid / edge-case field values
    // =========================================================================

    /** Payload with {@code name} = empty string. */
    public static Map<String, Object> projectCreatePayloadWithEmptyName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", EMPTY_PROJECT_NAME);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Payload with {@code name} = whitespace only. */
    public static Map<String, Object> projectCreatePayloadWithSpaceOnlyName() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", SPACE_ONLY_PROJECT_NAME);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", defaultConnections());
        return body;
    }

    /** Payload with {@code domain} = empty string. */
    public static Map<String, Object> projectCreatePayloadWithEmptyDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, EMPTY_DOMAIN);
    }

    /** Payload with {@code domain} = whitespace only. */
    public static Map<String, Object> projectCreatePayloadWithSpaceOnlyDomain() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, SPACE_ONLY_DOMAIN);
    }

    /** Payload with {@code domain} = {@link LyticsProjectTestData#INVALID_DOMAIN_FORMAT_ABC}. */
    public static Map<String, Object> projectCreatePayloadWithInvalidDomainFormatAbc() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, INVALID_DOMAIN_FORMAT_ABC);
    }

    /** Payload with {@code domain} = {@link LyticsProjectTestData#DOMAIN_MISSING_TLD}. */
    public static Map<String, Object> projectCreatePayloadWithDomainMissingTld() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, DOMAIN_MISSING_TLD);
    }

    /** Payload with {@code domain} containing special characters. */
    public static Map<String, Object> projectCreatePayloadWithDomainSpecialCharacters() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_WITH_SPECIAL_CHARS);
    }

    /** Payload with {@code domain} containing spaces. */
    public static Map<String, Object> projectCreatePayloadWithDomainHavingSpaces() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, DOMAIN_WITH_SPACES);
    }

    /** Payload with {@code domain} starting with a hyphen. */
    public static Map<String, Object> projectCreatePayloadWithDomainStartingWithHyphen() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_STARTING_WITH_HYPHEN);
    }

    /** Payload with {@code domain} ending with a hyphen. */
    public static Map<String, Object> projectCreatePayloadWithDomainEndingWithHyphen() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_ENDING_WITH_HYPHEN);
    }

    /** Payload with {@code domain} containing consecutive dots. */
    public static Map<String, Object> projectCreatePayloadWithDomainConsecutiveDots() {
        return validFullProjectCreatePayloadWithNameAndDomain(
                VALID_PROJECT_NAME, DOMAIN_CONSECUTIVE_DOTS);
    }

    /** Payload with {@code domain} = full HTTPS URL (invalid format). */
    public static Map<String, Object> projectCreatePayloadWithDomainAsHttpsUrl() {
        return validFullProjectCreatePayloadWithNameAndDomain(VALID_PROJECT_NAME, DOMAIN_AS_HTTPS_URL);
    }

    // =========================================================================
    // Payload builders — connections variations
    // =========================================================================

    /** Payload with connections using IDs from a different organisation. */
    public static Map<String, Object> projectCreatePayloadWithDifferentOrganizationConnections(
            String name) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", VALID_DOMAIN);
        body.put("description", VALID_DESCRIPTION);
        body.put("connections", connectionsBelongingToDifferentOrganization());
        return body;
    }

    /** Payload with only stackApiKeys populated. */
    public static Map<String, Object> projectCreatePayloadWithConnectionsOnlyStackApiKeys(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsOnlyStackApiKeys());
        return body;
    }

    /** Payload with only launchProjectUids populated (stack and personalize absent). */
    public static Map<String, Object> projectCreatePayloadWithConnectionsOnlyLaunchProjectUids(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsOnlyLaunchProjectUidsOtherKeysAbsent());
        return body;
    }

    /** Payload with only personalizeProjectUids populated (stack and launch absent). */
    public static Map<String, Object> projectCreatePayloadWithConnectionsOnlyPersonalizeProjectUids(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsOnlyPersonalizeProjectUidsOtherKeysAbsent());
        return body;
    }

    /** Payload with stack + personalize valid; launch array includes an empty string. */
    public static Map<String, Object> projectCreatePayloadWithStackPersonalizeAndLaunchIncludingEmptyString(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithStackPersonalizeAndLaunchIncludingEmptyString());
        return body;
    }

    /** Payload with stack + launch valid; personalize array includes an empty string. */
    public static Map<String, Object> projectCreatePayloadWithStackLaunchAndPersonalizeIncludingEmptyString(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithStackLaunchAndPersonalizeIncludingEmptyString());
        return body;
    }

    /** Payload with launch + personalize valid; stack array is empty. */
    public static Map<String, Object> projectCreatePayloadWithLaunchPersonalizeAndEmptyStackApiKeys(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithLaunchPersonalizeAndEmptyStackApiKeys());
        return body;
    }

    /** Payload where all connection arrays contain only empty strings. */
    public static Map<String, Object> projectCreatePayloadWithAllConnectionArraysContainingOnlyEmptyStrings(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithAllConnectionArraysContainingOnlyEmptyStrings());
        return body;
    }

    /** Payload with an invalid stack API key; launch and personalize are valid. */
    public static Map<String, Object> projectCreatePayloadWithInvalidStackApiKeyAndValidLaunchPersonalize(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithInvalidStackApiKeyAndValidLaunchPersonalize());
        return body;
    }

    /** Payload with valid stack + personalize; invalid launch UID. */
    public static Map<String, Object> projectCreatePayloadWithValidStackPersonalizeAndInvalidLaunchProjectUid(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithValidStackPersonalizeAndInvalidLaunchProjectUid());
        return body;
    }

    /** Payload with valid stack + launch; invalid personalize UID. */
    public static Map<String, Object> projectCreatePayloadWithValidStackLaunchAndInvalidPersonalizeProjectUid(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithValidStackLaunchAndInvalidPersonalizeProjectUid());
        return body;
    }

    /** Payload with JSON number (non-string) values in all connection arrays. */
    public static Map<String, Object> projectCreatePayloadWithNonStringConnectionValues(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithNonStringConnectionValues());
        return body;
    }

    /** Payload with an extra unknown field inside connections. */
    public static Map<String, Object> projectCreatePayloadWithExtraUnknownFieldInConnections(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", connectionsWithExtraUnknownField());
        return body;
    }

    /** Payload where {@code connections} is an empty JSON object {@code {}}. */
    public static Map<String, Object> projectCreatePayloadWithEmptyConnectionsObject(
            String name, String domain, String description) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("domain", domain);
        body.put("description", description);
        body.put("connections", new HashMap<String, Object>());
        return body;
    }
}
