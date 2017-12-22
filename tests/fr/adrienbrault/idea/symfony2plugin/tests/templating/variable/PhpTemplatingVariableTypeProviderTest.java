package fr.adrienbrault.idea.symfony2plugin.tests.templating.variable;

import com.intellij.patterns.PlatformPatterns;
import com.jetbrains.php.lang.psi.elements.Method;
import fr.adrienbrault.idea.symfony2plugin.tests.SymfonyLightCodeInsightFixtureTestCase;

import java.io.File;

/**
 * @see fr.adrienbrault.idea.symfony2plugin.templating.variable.PhpTemplatingVariableTypeProvider
 */
public class PhpTemplatingVariableTypeProviderTest extends SymfonyLightCodeInsightFixtureTestCase {

    public void setUp() throws Exception {
        super.setUp();
        myFixture.copyFileToProject("ide-twig.json");
        myFixture.copyFileToProject("classes.php");
        myFixture.copyFileToProject("dummy.html.php");
    }

    public String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testTemplateVariablePhpType() {

        assertPhpReferenceResolveTo("dummy.html.php",
            "<?php\n" +
                    "$view->es<caret>cape('bla')",
            PlatformPatterns.psiElement(Method.class).withName("escape")
        );
    }
}