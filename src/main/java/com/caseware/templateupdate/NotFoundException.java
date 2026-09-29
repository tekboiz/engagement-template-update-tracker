package com.caseware.templateupdate;

/**
 * The decision named an engagement the index has never seen. Raised instead of
 * creating an index row from a decision alone.
 */
public class NotFoundException extends DomainException {
    /**
     * Creates a not-found error whose code is {@code not_found}. {@code what}
     * is the kind of thing that was missing, and {@code id} is the value that
     * was looked up. The message reads "{what} not found: {id}".
     */
    public NotFoundException(String what, String id) {
        super("not_found", what + " not found: " + id);
    }
}
