package com.caseware.templateupdate.model;

/**
 * The choice a user records for a pending template update. Apply moves the
 * engagement's applied version forward. Decline keeps the applied version and
 * hides updates through the chosen version.
 */
public enum Decision {
    /** Accept this template version as the engagement's new applied version. */
    APPLY,
    /** Leave the applied version unchanged and dismiss updates through this version. */
    DECLINE
}
