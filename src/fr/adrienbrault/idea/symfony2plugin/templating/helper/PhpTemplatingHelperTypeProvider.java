package fr.adrienbrault.idea.symfony2plugin.templating.helper;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PhpTemplatingHelperTypeProvider implements PhpTypeProvider3 {

    @Override
    public char getKey() {
        return '\u0167';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement element) {
        if (!(element instanceof ArrayAccessExpression)) {
            return null;
        }

        if (!(element.getFirstChild() instanceof Variable)) {
            return null;
        }

        // is $view of class PhpEngine
        if (!PhpTemplatingUtil.isTypePhpEngine(((Variable)element.getFirstChild()).getType(), PhpIndex.getInstance(element.getProject()))) {
            return null;
        }

        // $view['helper']
        String arrayKey = findArrayKey(element);
        String signature = PhpTemplatingUtil.findSignatureForHelper(arrayKey);

        if (signature == null) {
            return null;
        }

        return new PhpType().add("#" + this.getKey() + signature);
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        //return Collections.emptySet();
        return PhpIndex.getInstance(project).getAnyByFQN(expression);
    }

    private String findArrayKey(PsiElement element) {
        for (PsiElement child: element.getChildren()) {
            if (!(child instanceof ArrayIndex)) {
                continue;
            }

            PsiElement stringExpr = child.getFirstChild();
            if (!(stringExpr instanceof StringLiteralExpression)) {
                continue;
            }

            return ((StringLiteralExpression)stringExpr).getContents();
        }

        return null;
    }
}
