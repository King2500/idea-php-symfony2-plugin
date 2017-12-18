package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.util.indexing.IndexingDataKeys;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class PhpTemplatingVariableTypeProvider implements PhpTypeProvider3 {
    private final static char TRIM_KEY = '\u0192';

    @Override
    public char getKey() {
        return '\u0166';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement element) {
        if (!Symfony2ProjectComponent.isEnabled(element)) {
            return null;
        }

        if (!(element instanceof Variable)) {
            return null;
        }

        PsiFile psiFile = element.getContainingFile();

        // PHP templates
        if (!PhpTemplatingUtil.isPhpTemplate(psiFile)) {
            return null;
        }

        Variable variable = (Variable) element;

        // only unresolved variable
        if (!variable.getSignature().startsWith("#V") /*&& !variable.getSignature().isEmpty()*/) {
            return null;
        }

        // unresolved variable should not be inside local function scopes
        if (isLocalVariable(variable)) {
            return null;
        }

        VirtualFile virtualFile = psiFile.getOriginalFile().getVirtualFile();

        if (virtualFile == null) {
            virtualFile = psiFile.getUserData(IndexingDataKeys.VIRTUAL_FILE);
        }

        if (virtualFile != null) {
            String varSignature = variable.getName() + "~" + virtualFile.getPath();
            return new PhpType().add("#" + this.getKey() + variable.getSignature() + TRIM_KEY + varSignature);
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {

        // get back our original call
        // since phpstorm 7.1.2 we need to validate this
        int endIndex = expression.lastIndexOf(TRIM_KEY);
        if(endIndex == -1) {
            return Collections.emptySet();
        }
        String parameter = expression.substring(endIndex + 1);
        PhpIndex phpIndex = PhpIndex.getInstance(project);

        int paramIndex = parameter.indexOf('~');
        if (paramIndex != -1) {
            String refFile = parameter.substring(paramIndex + 1);
            String varName = parameter.substring(0, paramIndex);

            VirtualFile relativeFile = VfsUtil.findRelativeFile(refFile, null);

            if (relativeFile != null) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(relativeFile);
                if (psiFile != null && psiFile instanceof PhpFile) {
                    String varSignature = PhpTemplatingUtil.findSignatureForTemplateVariable((PhpFile)psiFile, varName);
                    if (varSignature != null) {
                        Collection<PhpClass> result = phpIndex.getAnyByFQN(varSignature);
                        return result;
                    }
                }
            }
        }

        return Collections.emptySet();
    }

    private static boolean isLocalVariable(Variable variable) {
        return variable.getUseScope() instanceof LocalSearchScope;
    }
}
