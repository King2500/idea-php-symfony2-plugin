package fr.adrienbrault.idea.symfony2plugin.tests.templating.variable;

import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.templating.variable.PhpTemplatingVariableCompletionContributor
 */
public class PhpTemplatingVariableCompletionContributorTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ide-twig.json");
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("dummy.html.php");
    }

    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testVariableCompletion() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionContains("dummy.html.php", "" +
                "<?php\n" +
                "$<caret>",
            "view"//, "foo", "dt"
        );

        assertCompletionNotContains("dummy.html.php", "" +
                "<?php\n" +
                "$<caret>",
            "app"
        );
    }

    public void testVariableCompletionInFullStack() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        myFixture.copyFileToProject("classesFullStack.php");

        assertCompletionContains("dummy.html.php", "" +
                "<?php\n" +
                "$<caret>",
            "view", "app"//, "foo", "dt"
        );
    }

    public void testVariableCompletionInsideString() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionContains("dummy.html.php", "" +
                "<?php\n" +
                "$a = \"bla {$<caret>\";",
            "view"//, "foo", "dt"
        );

        assertCompletionContains("dummy.html.php", "" +
                "<?php\n" +
                "$a = \"bla ${<caret>\";",
            "view"//, "foo", "dt"
        );
    }

    public void testVariableCompletionNotInsideFunction() {
        if(System.getenv("PHPSTORM_ENV") != null) return;

        assertCompletionNotContains("dummy.html.php", "" +
                "<?php\n" +
                "function bar($foo) {\n" +
                "  $<caret> \n" +
                "}",
            "view"//, "foo", "dt"
        );
    }
}