package com.caseware.templateupdate;

/**
 * The requested decision fights the index as it stands now. Raised when a user
 * tries to apply a template version older than the version already applied.
 */
public class ConflictException extends DomainException {
    /**
     * Creates a conflict whose code is {@code conflict} and whose message
     * explains which version was rejected and which version is already applied.
     */
    public ConflictException(String message) {
        super("conflict", message);
    }
}
