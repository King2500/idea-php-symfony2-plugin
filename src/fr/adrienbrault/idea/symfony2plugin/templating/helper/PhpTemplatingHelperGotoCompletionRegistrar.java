package fr.adrienbrault.idea.symfony2plugin.templating.helper;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.ArrayAccessExpression;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import fr.adrienbrault.idea.symfony2plugin.Symfony2Icons;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionProvider;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrar;
import fr.adrienbrault.idea.symfony2plugin.codeInsight.GotoCompletionRegistrarParameter;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PhpTemplatingHelperGotoCompletionRegistrar implements GotoCompletionRegistrar {

    @Override
    public void register(GotoCompletionRegistrarParameter registrar) {
        registrar.register(PhpTemplatingUtil.getHelperAccessPattern(), psiElement -> {
            PsiElement context = psiElement.getContext();
            if (!(context instanceof StringLiteralExpression)) {
                return null;
            }

            if (!PhpTemplatingUtil.isPhpTemplate(psiElement.getContainingFile())) {
                return null;
            }

            Project project = context.getProject();
            PhpIndex phpIndex = PhpIndex.getInstance(project);

            // $view['<caret>']
            ArrayAccessExpression array = PhpPsiUtil.getParentByCondition(context, true, ArrayAccessExpression.INSTANCEOF);
            if (array != null) {
                PsiElement element = array.getValue();

                if (element instanceof Variable) {
                    if (PhpTemplatingUtil.isTypePhpEngine(((Variable)element).getType(), phpIndex)) {
                        return new PhpTemplatingHelperCompletionProvider((StringLiteralExpression) context);
                    }
                }

                return null;
            }

            //  $view->get('<caret>')
            //  $view->has('<caret>')
            //  $view->offsetUnset('<caret>')
            //  $view->offsetExists('<caret>')
            //  $view->offsetGet('<caret>')
            MethodMatcher.MethodMatchParameter methodMatchParameter1 = new MethodMatcher.StringParameterMatcher(context, 0)
                    .withSignature(PhpTemplatingUtil.SIGNATURE_RENDERER_COMPONENT, "get")
                    .withSignature(PhpTemplatingUtil.SIGNATURE_RENDERER_COMPONENT, "has")
                    .withSignature(PhpTemplatingUtil.SIGNATURE_RENDERER_COMPONENT, "offsetUnset")
                    .withSignature(PhpTemplatingUtil.SIGNATURE_RENDERER_COMPONENT, "offsetExists")
                    .withSignature(PhpTemplatingUtil.SIGNATURE_RENDERER_COMPONENT, "offsetGet")
                    .match();

            if (methodMatchParameter1 != null) {
                return new PhpTemplatingHelperCompletionProvider((StringLiteralExpression) context);
            }

            //  $view->set(Helper, '<caret>')
            //  $view->offsetSet(Helper, '<caret>')
            MethodMatcher.MethodMatchParameter methodMatchParameter2 = new MethodMatcher.StringParameterMatcher(context, 1)
                    .withSignature(PhpTemplatingUtil.SIGNATURE_RENDERER_COMPONENT, "set")
                    .withSignature(PhpTemplatingUtil.SIGNATURE_RENDERER_COMPONENT, "offsetSet")
                    .match();

            if (methodMatchParameter2 != null) {
                return new PhpTemplatingHelperCompletionProvider((StringLiteralExpression) context);
            }

            return null;
        });

    }

    private class PhpTemplatingHelperCompletionProvider extends GotoCompletionProvider {
        public PhpTemplatingHelperCompletionProvider(StringLiteralExpression element) {
            super(element);
        }

        @NotNull
        @Override
        public Collection<LookupElement> getLookupElements() {
            Collection<LookupElement> results = new ArrayList<>();
            for (Map.Entry<String, String> helper: PhpTemplatingUtil.getHelpersMap().entrySet()) {
                String helperName = helper.getKey();
                String helperClass = StringUtil.trimLeading(helper.getValue(), '\\');
                results.add(LookupElementBuilder.create(helperName).withIcon(Symfony2Icons.SERVICE).withTypeText(helperClass, true));
            }
            return results;
        }

        @NotNull
        @Override
        public Collection<PsiElement> getPsiTargets(PsiElement psiElement) {
            PsiElement element = psiElement.getParent();
            if (!(element instanceof StringLiteralExpression)) {
                return Collections.emptyList();
            }

            String helperName = ((StringLiteralExpression) element).getContents();
            if (StringUtils.isBlank(helperName)) {
                return Collections.emptyList();
            }

            String helperSignature = PhpTemplatingUtil.findSignatureForHelper(helperName);
            if (helperSignature == null) {
                return Collections.emptyList();
            }

            return new HashSet<>(PhpElementsUtil.getClassesInterface(getProject(), helperSignature));
        }
    }
}
