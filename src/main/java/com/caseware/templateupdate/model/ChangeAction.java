package com.caseware.templateupdate.model;

/**
 * The kind of change a summary line describes. This is the practitioner-facing
 * action, which is not always the same as the raw diff operation: a new field
 * on an existing procedure is a diff add, but the sentence calls it an update.
 */
public enum ChangeAction {
    /** A new template entity, such as a procedure or disclosure, was introduced. */
    ADDED,
    /** A template entity that used to exist was taken out. */
    REMOVED,
    /** An existing entity changed, including a field that was added onto it. */
    UPDATED
}
