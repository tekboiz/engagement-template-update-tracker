package com.caseware.templateupdate.model;

/**
 * One line of a change summary. {@code sourcePath} is the JSON Pointer of the
 * diff operation this line came from, which is how a summary is checked to be
 * grounded. {@code action} says added, removed, or updated. {@code category}
 * is the template section, and {@code description} is the sentence a person reads.
 */
public record SummaryItem(String sourcePath, ChangeAction action, String category, String description) {}
