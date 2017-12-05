package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3;
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
        if (!(element instanceof Variable)) {
            return null;
        }

        // PHP templates
        if (!PhpTemplatingUtil.isPhpTemplate(element.getContainingFile())) {
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

        // class for $view, $app, ...
        String varSignature = PhpTemplatingUtil.findSignatureForTemplateVariable(
            (PhpFile)variable.getContainingFile(),
            variable.getName()
        );

        if (varSignature != null) {
            //return new PhpType().add("#C" + varSignature);
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

        return PhpIndex.getInstance(project).getAnyByFQN(expression.substring(endIndex + 1));
    }

    private static boolean isLocalVariable(Variable variable) {
        return variable.getUseScope() instanceof LocalSearchScope;
    }


}
