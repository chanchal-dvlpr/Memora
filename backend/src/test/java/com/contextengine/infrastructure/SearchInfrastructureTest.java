package com.contextengine.infrastructure;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.domain.valueobject.SearchQuery;
import com.contextengine.infrastructure.search.SearchAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class SearchInfrastructureTest {

    private SearchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SearchAdapter();
    }

    @Test
    void testKeywordSearchAndRanking() {
        KnowledgeNode node1 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "ProjectScannerService", "kind", "CLASS")));
        KnowledgeNode node2 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "ProjectScanner", "kind", "CLASS")));
        KnowledgeNode node3 = new KnowledgeNode(NodeId.generate(), "CODE_SYMBOL", new Metadata(Map.of("name", "SearchAdapter", "kind", "CLASS")));

        List<KnowledgeNode> catalog = List.of(node1, node2, node3);

        SearchQuery query = new SearchQuery("ProjectScanner", false, new Metadata(Map.of()), 10);
        Collection<KnowledgeNode> results = adapter.executeSearch(query, catalog);

        assertThat(results).hasSize(2);
        assertThat(results.iterator().next().attributes().get("name")).isEqualTo("ProjectScanner");
    }
}
