package com.caseware.templateupdate.summary;

/**
 * A summary line cited a JSON path that is not in the diff it claims to
 * describe. The service refuses to store that summary.
 */
public class UngroundedSummaryException extends RuntimeException {
    /**
     * Creates the failure with a message that names the path that could not
     * be found among the diff operations.
     */
    public UngroundedSummaryException(String message) {
        super(message);
    }
}
