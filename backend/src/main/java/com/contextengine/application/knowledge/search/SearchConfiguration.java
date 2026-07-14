package com.contextengine.application.knowledge.search;

/**
 * Settings config for adjusting matching scopes and parameters.
 */
public class SearchConfiguration {
    private boolean caseSensitive = false;
    private boolean searchPaths = true;
    private boolean searchSymbols = true;
    private boolean searchDependencies = true;
    private int maximumResults = 100;
    private boolean includeMetadata = true;

    public boolean caseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    public boolean searchPaths() {
        return searchPaths;
    }

    public void setSearchPaths(boolean searchPaths) {
        this.searchPaths = searchPaths;
    }

    public boolean searchSymbols() {
        return searchSymbols;
    }

    public void setSearchSymbols(boolean searchSymbols) {
        this.searchSymbols = searchSymbols;
    }

    public boolean searchDependencies() {
        return searchDependencies;
    }

    public void setSearchDependencies(boolean searchDependencies) {
        this.searchDependencies = searchDependencies;
    }

    public int maximumResults() {
        return maximumResults;
    }

    public void setMaximumResults(int maximumResults) {
        this.maximumResults = maximumResults;
    }

    public boolean includeMetadata() {
        return includeMetadata;
    }

    public void setIncludeMetadata(boolean includeMetadata) {
        this.includeMetadata = includeMetadata;
    }
}
