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
import org.jetbrains.annotations.Nullable;

public class PhpTemplatingVariableGotoDeclarationHandler implements GotoDeclarationHandler {
    @Nullable
    @Override
    public PsiElement[] getGotoDeclarationTargets(@Nullable PsiElement psiElement, int i, Editor editor) {
        if(!Symfony2ProjectComponent.isEnabled(psiElement)) {
            return new PsiElement[0];
        }

        PsiFile psiFile = psiElement.getContainingFile();
        if (!PhpTemplatingUtil.isPhpTemplate(psiFile)) {
            return new PsiElement[0];
        }

        PsiElement variable = psiElement.getContext();
        if (!(variable instanceof Variable)) {
            return new PsiElement[0];
        }

        return PhpTemplatingUtil.getTemplateVariableTargets((PhpFile)psiFile, (Variable)variable);
    }

    @Nullable
    @Override
    public String getActionText(DataContext dataContext) {
        return null;
    }
}
