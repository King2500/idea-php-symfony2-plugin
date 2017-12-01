package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIcons;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.completion.PhpLookupElement;
import com.jetbrains.php.completion.insert.PhpVariableInsertHandler;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.stubs.indexes.PhpVariableIndex;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import org.jetbrains.annotations.NotNull;

public class PhpTemplatingVariableCompletionContributor extends CompletionContributor {
    public PhpTemplatingVariableCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement()
                .withParent(Variable.class), new PhpTemplatingVariableCompletionProvider());
    }

    private class PhpTemplatingVariableCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
            if (!PhpTemplatingUtil.isPhpTemplate(parameters.getOriginalFile())) {
                return;
            }

            Project project = parameters.getPosition().getProject();
            PhpIndex phpIndex = PhpIndex.getInstance(project);
            PhpType type = new PhpType().add("#C" + PhpTemplatingUtil.getPhpEngineClass(phpIndex));
//            InsertHandler insertHandler = (insertionContext, lookupElement) -> PhpVariableInsertHandler.getInstance().handleInsert(insertionContext, lookupElement);
//            PhpLookupElement element = new PhpLookupElement("view", StubIndexKey.createIndexKey("TEMPLATING_VIEW"), PhpIcons.VARIABLE, type, project, insertHandler);
//            result.addElement(element);

            //LookupElementBuilder lookupElement = LookupElementBuilder.create("view");
            //LookupElementBuilder lookupElement = LookupElementBuilder.create(new VariableImpl(new PhpVariableStubImpl(null, new PhpVariableElementType("dummy"), StringRef.fromString("dummyRef"), PhpType.INT)));
            PhpLookupElement lookupElement = new PhpLookupElement("view", PhpVariableIndex.KEY, PhpIcons.VARIABLE, type, project, PhpVariableInsertHandler.getInstance());
            result.addElement(lookupElement);
        }
    }
}
