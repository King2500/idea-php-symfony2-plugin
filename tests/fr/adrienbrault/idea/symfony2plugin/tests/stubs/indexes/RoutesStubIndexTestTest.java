package fr.adrienbrault.idea.symfony2plugin.tests.stubs.indexes;

import com.intellij.ide.highlighter.XmlFileType;
import fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;
import org.jetbrains.yaml.YAMLFileType;

/**
 * @author Daniel Espendiller <daniel@espendiller.net>
 * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex
 */
public class RoutesStubIndexTestTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();

        myFixture.configureByText(YAMLFileType.YML, "" +
            "foo_yaml_pattern:\n" +
            "    pattern: /\n" +
            "    methods: [GET, POST]\n" +
            "    defaults: { _controller: foo_controller }" +
            "\n" +
            "foo_yaml_path:\n" +
            "    path: /\n" +
            "    defaults: { _controller: foo_controller }" +
            "\n" +
            "foo_yaml_path_only:\n" +
            "    path: /\n" +
            "foo_yaml_invalid:\n" +
            "    path_invalid: /\n"
        );

        myFixture.configureByText(XmlFileType.INSTANCE, "" +
            "<routes>\n" +
            "  <route id=\"foo_xml_pattern\" pattern=\"/blog/{slug}\"/>\n" +
            "  <route id=\"foo_xml_path\" path=\"/blog/{slug}\">\n" +
            "    <default key=\"_controller\">Foo</default>\n" +
            "  </route>\n" +
            "  <route id=\"foo_xml_id_only\"/>\n" +
            "</routes>"
        );
    }

    /**
     * @see fr.adrienbrault.idea.symfony2plugin.stubs.indexes.RoutesStubIndex#getIndexer()
     */
    public void testRouteIdIndex() {
        assertIndexContains(RoutesStubIndex.KEY,
            "foo_yaml_pattern", "foo_yaml_path", "foo_yaml_path_only",
            "foo_xml_pattern", "foo_xml_path", "foo_xml_id_only"
        );

        assertIndexNotContains(RoutesStubIndex.KEY,
            "foo_yaml_invalid"
        );
    }
}
