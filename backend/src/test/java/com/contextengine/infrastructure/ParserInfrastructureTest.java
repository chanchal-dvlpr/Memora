package com.contextengine.infrastructure;

import com.contextengine.domain.entity.KnowledgeNode;
import com.contextengine.domain.valueobject.Path;
import com.contextengine.infrastructure.parser.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collection;
import static org.assertj.core.api.Assertions.assertThat;

class ParserInfrastructureTest {

    private LanguageParserFactory factory;

    @BeforeEach
    void setUp() {
        factory = new LanguageParserFactory();
    }

    @Test
    void testParserFactoryResolution() {
        ILanguageSymbolParser javaParser = factory.getParser(new Path("src/App.java"));
        assertThat(javaParser).isInstanceOf(TSParserBridge.class);

        ILanguageSymbolParser txtParser = factory.getParser(new Path("README.md"));
        assertThat(txtParser).isInstanceOf(GenericTextParser.class);
    }

    @Test
    void testGenericTextParser() {
        GenericTextParser parser = new GenericTextParser();
        Collection<KnowledgeNode> symbols = parser.parse(new Path("README.md"), "# Title\nSome content");
        assertThat(symbols).hasSize(1);
        
        KnowledgeNode node = symbols.iterator().next();
        assertThat(node.type()).isEqualTo("CODE_SYMBOL");
        assertThat(node.attributes().get("kind")).isEqualTo("TEXT_FILE");
        assertThat(node.attributes().get("name")).isEqualTo("README.md");
    }

    @Test
    void testTSParserBridgeSymbols() {
        TSParserBridge parser = new TSParserBridge();
        String code = "public class App {\n" +
                      "    public void run() {\n" +
                      "        System.out.println(\"hello\");\n" +
                      "    }\n" +
                      "}";
        Collection<KnowledgeNode> symbols = parser.parse(new Path("src/App.java"), code);
        assertThat(symbols).hasSize(2);

        boolean hasClass = symbols.stream().anyMatch(n -> n.attributes().get("kind").equals("CLASS") && n.attributes().get("name").equals("App"));
        boolean hasMethod = symbols.stream().anyMatch(n -> n.attributes().get("kind").equals("METHOD") && n.attributes().get("name").equals("run"));
        
        assertThat(hasClass).isTrue();
        assertThat(hasMethod).isTrue();
    }
}
