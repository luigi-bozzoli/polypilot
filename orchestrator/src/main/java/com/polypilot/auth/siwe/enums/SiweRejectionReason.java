package com.polypilot.auth.siwe.enums;

public enum SiweRejectionReason {

    DOMAIN_MISMATCH("domain mismatch"),
    URI_MISMATCH("uri mismatch"),
    UNKNOWN_OR_EXPIRED_NONCE("unknown or expired nonce"),
    MISSING_ISSUED_AT("missing issuedAt"),
    ISSUED_AT_IN_FUTURE("issuedAt is in the future"),
    ISSUED_AT_TOO_OLD("issuedAt is too old"),
    NOT_YET_VALID("message not yet valid (notBefore)"),
    EXPIRATION_PRECEDES_ISSUED_AT("expirationTime precedes issuedAt"),
    MESSAGE_EXPIRED("message expired"),
    CHAIN_ID_NOT_ALLOWED("chain id %s not allowed"),
    NO_ADDRESS_RECOVERED("no address recovered"),
    ADDRESS_MISMATCH("recovered address does not match message address (or bad EIP-55 checksum)"),
    NONCE_ALREADY_CONSUMED("nonce already consumed");

    private final String template;

    SiweRejectionReason(String template) {
        this.template = template;
    }

    public String format(Object... args) {
        return String.format(template, args);
    }

    @Override
    public String toString() {
        return format();
    }
}
