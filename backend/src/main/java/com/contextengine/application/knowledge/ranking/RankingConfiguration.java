package com.contextengine.application.knowledge.ranking;

/**
 * Settings configuration controlling relevance weight toggles and maximum candidates bounds.
 */
public class RankingConfiguration {
    private final boolean enableRecencyWeight;
    private final boolean enableStructuralWeight;
    private final boolean enableDependencyWeight;
    private final boolean enableSymbolWeight;
    private final int maximumCandidates;

    public RankingConfiguration() {
        this(true, true, true, true, 1000);
    }

    public RankingConfiguration(
        boolean enableRecencyWeight,
        boolean enableStructuralWeight,
        boolean enableDependencyWeight,
        boolean enableSymbolWeight,
        int maximumCandidates
    ) {
        this.enableRecencyWeight = enableRecencyWeight;
        this.enableStructuralWeight = enableStructuralWeight;
        this.enableDependencyWeight = enableDependencyWeight;
        this.enableSymbolWeight = enableSymbolWeight;
        this.maximumCandidates = maximumCandidates;
    }

    public boolean enableRecencyWeight() {
        return enableRecencyWeight;
    }

    public boolean enableStructuralWeight() {
        return enableStructuralWeight;
    }

    public boolean enableDependencyWeight() {
        return enableDependencyWeight;
    }

    public boolean enableSymbolWeight() {
        return enableSymbolWeight;
    }

    public int maximumCandidates() {
        return maximumCandidates;
    }
}
