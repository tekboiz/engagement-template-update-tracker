package com.caseware.templateupdate;

/**
 * A problem in the template-update rules, as opposed to a programming failure.
 * {@code code} is a stable token such as {@code not_found} or {@code conflict}
 * that a caller can branch on. The message is the sentence a person can read.
 */
public class DomainException extends RuntimeException {
    private final String code;

    /**
     * Creates an exception with a stable {@code code} and a human-readable message.
     */
    public DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns the stable error code, separate from the message text.
     */
    public String code() {
        return code;
    }
}
