package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.codeInsight.completion.*;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.insert.PhpVariableInsertHandler;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpVariableIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PhpTemplatingVariableCompletionContributor extends CompletionContributor {
    public PhpTemplatingVariableCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(Variable.class), new PhpTemplatingVariableCompletionProvider());
        extend(CompletionType.SMART, PlatformPatterns.psiElement().withParent(Variable.class), new PhpTemplatingVariableCompletionProvider());
    }

    private class PhpTemplatingVariableCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
            PsiFile file = parameters.getOriginalFile();
            Project project = parameters.getPosition().getProject();

            result = patchResultIfNeeded(result);

            for (Map.Entry<String, String> variableEntry: PhpTemplatingUtil.getTemplateVariablesForFile(file).entrySet()) {
                String varName = variableEntry.getKey();
                String varClass = variableEntry.getValue();

                PhpType type = new PhpType().add(varClass);
                result.addElement(new PhpLookupElement(varName, PhpVariableIndex.KEY, PhpIcons.VARIABLE, type, project, PhpVariableInsertHandler.getInstance()));
            }
        }
    }

    @NotNull
    private static CompletionResultSet patchResultIfNeeded(@NotNull CompletionResultSet result) {
        String prefix = result.getPrefixMatcher().getPrefix();
        if (prefix.startsWith("${") || prefix.startsWith("{$")) {
            result = result.withPrefixMatcher(prefix.substring(2));
        } else if (prefix.startsWith("$")) {
            result = result.withPrefixMatcher(prefix.substring(1));
        }

        return result;
    }
}
