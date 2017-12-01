package fr.adrienbrault.idea.symfony2plugin.templating.helper;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class PhpTemplatingHelperCompletionContributor extends CompletionContributor {
    public PhpTemplatingHelperCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.or(
                PlatformPatterns.psiElement().withSuperParent(2, ArrayIndex.class),
                PhpElementsUtil.getParameterInsideMethodReferencePattern()
            ),
            new CompletionProvider<CompletionParameters>() {

            @Override
            protected void addCompletions(@NotNull CompletionParameters parameters, ProcessingContext context, @NotNull CompletionResultSet result) {
                if (!PhpTemplatingUtil.isPhpTemplate(parameters.getOriginalFile())) {
                    return;
                }

                PsiElement position = parameters.getOriginalPosition();
                if (position == null) {
                    position = parameters.getPosition().getOriginalElement();
                }

                StringLiteralExpression literal = PhpPsiUtil.getParentByCondition(position, true, StringLiteralExpression.INSTANCEOF);
                if (literal == null) {
                    return;
                }

                Project project = position.getProject();
                PhpIndex phpIndex = PhpIndex.getInstance(project);

                // $view['<caret>']
                ArrayAccessExpression array = PhpPsiUtil.getParentByCondition(position, true, ArrayAccessExpression.INSTANCEOF);
                if (array != null) {
                    PsiElement element = array.getValue();

                    if (element instanceof Variable) {
                        if (PhpTemplatingUtil.isTypePhpEngine(((Variable)element).getType(), phpIndex)) {
                            addTemplatingHelpersVariants(result);
                        }
                    }

                    return;
                }

                //  $view->get('<caret>')
                ParameterList parameterList = PhpPsiUtil.getParentByCondition(position, true, ParameterList.INSTANCEOF);
                if (parameterList != null) {
                    MethodReference method = PhpPsiUtil.getParentByCondition(parameterList, MemberReference.INSTANCEOF);
                    if (method != null && method.getName() != null && method.getClassReference() != null) {
                        if(method.getName().equals("get") && PhpTemplatingUtil.isTypePhpEngine(method.getClassReference().getType(), phpIndex)) {
                            addTemplatingHelpersVariants(result);
                        }
                    }
                }

                //  $view->has('<caret>')
                //  $view->offsetUnset('<caret>')
                //  $view->offsetExists('<caret>')
                //  $view->offsetGet('<caret>')

                //  $view->set(Helper, '<caret>')
                //  $view->offsetSet(Helper, '<caret>')
            }

            private void addTemplatingHelpersVariants(CompletionResultSet result) {
                for (Map.Entry<String, String> helper: PhpTemplatingUtil.getHelpersMap().entrySet()) {
                    String helperName = helper.getKey();
                    String helperClass = StringUtil.trimLeading(helper.getValue(), '\\');
                    result.addElement(LookupElementBuilder.create(helperName).withIcon(Symfony2Icons.SERVICE).withTypeText(helperClass, true));
                }
            }
        });
    }

}
