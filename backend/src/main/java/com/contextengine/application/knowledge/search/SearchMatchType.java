package com.contextengine.application.knowledge.search;

/**
 * Classifies the match type strength of a query result element.
 */
public enum SearchMatchType {
    EXACT,
    PREFIX,
    SUBSTRING,
    PATH,
    SYMBOL,
    DEPENDENCY
}
