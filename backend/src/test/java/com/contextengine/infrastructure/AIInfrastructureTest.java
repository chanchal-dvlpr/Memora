package com.contextengine.infrastructure;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Metadata;
import com.contextengine.domain.valueobject.NodeId;
import com.contextengine.infrastructure.ai.AIProviderAdapter;
import com.contextengine.infrastructure.ai.ContextFormatter;
import com.contextengine.infrastructure.ai.TokenCalculator;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class AIInfrastructureTest {

    @Test
    void testTokenCalculator() {
        TokenCalculator calculator = new TokenCalculator();
        assertThat(calculator.calculateTokens("")).isEqualTo(0);
        assertThat(calculator.calculateTokens("abcd")).isEqualTo(1);
        assertThat(calculator.calculateTokens("abcdefgh")).isEqualTo(2);
    }

    @Test
    void testContextFormatter() {
        ContextFormatter formatter = new ContextFormatter();
        KnowledgeNode node = new KnowledgeNode(
            NodeId.generate(),
            "CODE_SYMBOL",
            new Metadata(Map.of("name", "App", "path", "src/App.java", "kind", "CLASS"))
        );
        String md = formatter.formatAsMarkdown(List.of(node));
        assertThat(md).contains("App").contains("src/App.java").contains("CLASS");
    }

    @Test
    void testAIProviderAdapter() {
        AIProviderAdapter adapter = new AIProviderAdapter();
        String response = adapter.generateResponse("Generate hello world");
        assertThat(response).contains("Simulated LLM response");
    }
}
