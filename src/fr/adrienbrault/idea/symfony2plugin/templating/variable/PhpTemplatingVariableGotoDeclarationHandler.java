package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Variable;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

public class PhpTemplatingVariableGotoDeclarationHandler implements GotoDeclarationHandler {
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return null;
        }

        PsiFile psiFile = psiElement.getContainingFile();
        if (!PhpTemplatingUtil.isPhpTemplate(psiFile)) {
            return null;
        }

        PsiElement variable = psiElement.getContext();
        if (!(variable instanceof Variable)) {
            return new PsiElement[0];
        }

//        if (((Variable)variable).getName().equals("view")) {
//            PsiElement target = PhpTemplatingUtil.getViewVariableTarget();
//            return Collections.singleton(target).toArray(new PsiElement[0]);
//        }

        for(Map.Entry<String, PsiVariable> templateVar: PhpTemplatingUtil.collectControllerTemplateVariables((PhpFile)psiFile).entrySet()) {
            String varName = templateVar.getKey();

            if (((Variable)variable).getName().equals(varName)) {
                PsiElement target = templateVar.getValue().getElement();
                return Collections.singleton(target).toArray(new PsiElement[0]);
            }
        }

        return new PsiElement[0];
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
