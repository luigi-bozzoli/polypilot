package com.polypilot.auth.siwe;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed EIP-4361 (Sign-In With Ethereum) message.
 *
 * <p>Only the fields the orchestrator validates are exposed: {@code domain},
 * {@code address}, {@code uri}, {@code version} (pinned to {@code 1} per
 * EIP-4361), {@code nonce}, {@code chainId}, {@code issuedAt} and the optional
 * {@code expirationTime}. The parser is deliberately strict
 * about the fixed preamble (first line, address line, blank line) so a malformed
 * message fails fast; {@link #parse(String)} throws
 * {@link IllegalArgumentException} on anything that is not a structurally valid
 * SIWE message.
 *
 * <p>This is <em>parsing only</em>. Domain/URI/nonce/time/chain policy is applied
 * by {@link SiweService}.
 */
@Getter
@AllArgsConstructor
public final class SiweMessage {

    private static final Pattern FIRST_LINE = Pattern.compile(
            "^(?<domain>[^\\n ]+) wants you to sign in with your Ethereum account:$");
    private static final Pattern ADDRESS_LINE = Pattern.compile("^0x[0-9a-fA-F]{40}$");
    private static final Pattern LABEL_LINE = Pattern.compile("^(?<label>[A-Za-z ]+): (?<value>.+)$");

    private final String domain;
    private final String address;
    private final String uri;
    private final String version;
    private final long chainId;
    private final String nonce;
    private final OffsetDateTime issuedAt;
    private final OffsetDateTime expirationTime; // nullable
    private final OffsetDateTime notBefore;      // nullable

    /**
     * Parse a raw EIP-4361 message.
     *
     * @throws IllegalArgumentException if the message is not structurally valid SIWE
     */
    public static SiweMessage parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Empty SIWE message");
        }

        String[] lines = raw.replace("\r\n", "\n").split("\n", -1);
        if (lines.length < 6) {
            throw new IllegalArgumentException("SIWE message is too short");
        }

        Matcher first = FIRST_LINE.matcher(lines[0]);
        if (!first.matches()) {
            throw new IllegalArgumentException("Malformed SIWE preamble line");
        }
        String domain = first.group("domain");

        String address = lines[1];
        if (!ADDRESS_LINE.matcher(address).matches()) {
            throw new IllegalArgumentException("Malformed SIWE address line");
        }

        if (!lines[2].isEmpty()) {
            throw new IllegalArgumentException("Expected blank line after SIWE address");
        }

        String uri = null;
        String version = null;
        Long chainId = null;
        String nonce = null;
        OffsetDateTime issuedAt = null;
        OffsetDateTime expirationTime = null;
        OffsetDateTime notBefore = null;

        for (int i = 3; i < lines.length; i++) {
            Matcher m = LABEL_LINE.matcher(lines[i]);
            if (!m.matches()) {
                continue; // statement text, blank lines, "Resources:" list entries
            }
            String label = m.group("label");
            String value = m.group("value");
            switch (label) {
                case "URI" -> uri = value;
                case "Version" -> version = value;
                case "Chain ID" -> chainId = parseChainId(value);
                case "Nonce" -> nonce = value;
                case "Issued At" -> issuedAt = parseTimestamp("Issued At", value);
                case "Expiration Time" -> expirationTime = parseTimestamp("Expiration Time", value);
                case "Not Before" -> notBefore = parseTimestamp("Not Before", value);
                default -> { /* Request ID, Resources, unknown labels — ignored */ }
            }
        }

        if (uri == null) {
            throw new IllegalArgumentException("SIWE message missing URI");
        }
        if (chainId == null) {
            throw new IllegalArgumentException("SIWE message missing Chain ID");
        }
        if (nonce == null || nonce.length() < 8 || !nonce.chars().allMatch(Character::isLetterOrDigit)) {
            throw new IllegalArgumentException("SIWE message has missing or invalid Nonce");
        }
        if (version == null || !version.equals("1")) {
            throw new IllegalArgumentException("SIWE message has missing or unsupported Version");
        }

        return new SiweMessage(domain, address, uri, version, chainId, nonce,
                issuedAt, expirationTime, notBefore);
    }

    private static long parseChainId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("SIWE Chain ID is not a number");
        }
    }

    private static OffsetDateTime parseTimestamp(String label, String value) {
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("SIWE " + label + " is not an ISO-8601 timestamp");
        }
    }
}
