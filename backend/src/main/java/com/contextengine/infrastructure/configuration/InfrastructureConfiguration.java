package com.contextengine.infrastructure.configuration;

import com.contextengine.application.port.FilesystemPort;
import com.contextengine.application.port.GitPort;
import com.contextengine.domain.repository.KnowledgeGraphRepository;
import com.contextengine.infrastructure.ai.AIClient;
import com.contextengine.infrastructure.ai.AIProviderAdapter;
import com.contextengine.infrastructure.ai.ContextFormatter;
import com.contextengine.infrastructure.ai.TokenCalculator;
import com.contextengine.infrastructure.cache.CacheManager;
import com.contextengine.infrastructure.filesystem.LocalFilesystemAdapter;
import com.contextengine.infrastructure.git.GitCLIAdapter;
import com.contextengine.infrastructure.graph.InMemoryGraphStorage;
import com.contextengine.infrastructure.graph.LocalKnowledgeGraphAdapter;
import com.contextengine.infrastructure.parser.ILanguageParserFactory;
import com.contextengine.infrastructure.parser.LanguageParserFactory;
import com.contextengine.infrastructure.search.SearchAdapter;
import com.contextengine.infrastructure.storage.LocalStorageAdapter;
import com.contextengine.infrastructure.storage.SnapshotStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class registering technical infrastructure adapters as beans.
 * <p>
 * Bounded Context: Operational Infrastructure
 * Related Subsystem: System Initialization
 * </p>
 */
@Configuration
public class InfrastructureConfiguration {

    /**
     * Registers FilesystemPort adapter.
     *
     * @return FilesystemPort instance
     */
    @Bean
    public FilesystemPort filesystemPort() {
        return new LocalFilesystemAdapter();
    }

    /**
     * Registers GitPort adapter.
     *
     * @return GitPort instance
     */
    @Bean
    public GitPort gitPort() {
        return new GitCLIAdapter();
    }

    /**
     * Registers InMemoryGraphStorage storage engine.
     *
     * @return InMemoryGraphStorage instance
     */
    @Bean
    public InMemoryGraphStorage inMemoryGraphStorage() {
        return new InMemoryGraphStorage();
    }

    /**
     * Registers KnowledgeGraphRepository persistence adapter.
     *
     * @param storage memory graph storage engine
     * @return KnowledgeGraphRepository instance
     */
    @Bean
    public KnowledgeGraphRepository knowledgeGraphRepository(InMemoryGraphStorage storage) {
        return new LocalKnowledgeGraphAdapter(storage);
    }

    /**
     * Registers ILanguageParserFactory.
     *
     * @return ILanguageParserFactory instance
     */
    @Bean
    public ILanguageParserFactory languageParserFactory() {
        return new LanguageParserFactory();
    }

    /**
     * Registers SearchAdapter.
     *
     * @return SearchAdapter instance
     */
    @Bean
    public SearchAdapter searchAdapter() {
        return new SearchAdapter();
    }

    /**
     * Registers LocalStorageAdapter.
     *
     * @return LocalStorageAdapter instance
     */
    @Bean
    public LocalStorageAdapter localStorageAdapter() {
        return new LocalStorageAdapter();
    }

    /**
     * Registers SnapshotStorage.
     *
     * @param localAdapter disk storage adapter
     * @return SnapshotStorage instance
     */
    @Bean
    public SnapshotStorage snapshotStorage(LocalStorageAdapter localAdapter) {
        return new SnapshotStorage(localAdapter);
    }

    /**
     * Registers TokenCalculator.
     *
     * @return TokenCalculator instance
     */
    @Bean
    public TokenCalculator tokenCalculator() {
        return new TokenCalculator();
    }

    /**
     * Registers ContextFormatter.
     *
     * @return ContextFormatter instance
     */
    @Bean
    public ContextFormatter contextFormatter() {
        return new ContextFormatter();
    }

    /**
     * Registers AIClient adapter.
     *
     * @return AIClient instance
     */
    @Bean
    public AIClient aiClient() {
        return new AIProviderAdapter();
    }

    /**
     * Registers CacheManager.
     *
     * @return CacheManager instance
     */
    @Bean
    public CacheManager cacheManager() {
        return new CacheManager();
    }
}
