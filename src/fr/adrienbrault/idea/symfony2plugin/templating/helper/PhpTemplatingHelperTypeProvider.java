package fr.adrienbrault.idea.symfony2plugin.templating.helper;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider3;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class PhpTemplatingHelperTypeProvider implements PhpTypeProvider3 {
    private final static char TRIM_KEY = '\u0193';

    @Override
    public char getKey() {
        return '\u0167';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement element) {
        if (!Symfony2ProjectComponent.isEnabled(element)) {
            return null;
        }

        String helperName = null;
        String refSignature = "";

        // $view['helper']
        if (element instanceof ArrayAccessExpression) {
            PhpPsiElement arrayAccess = ((ArrayAccessExpression)element).getValue();
            if (arrayAccess instanceof PhpReference) {
                String subject = ((PhpReference)arrayAccess).getType().toString();
                helperName = findArrayKey(element);

                PhpType result = new PhpType();
                String finalHelperName = helperName;
                StringUtil.split(subject, "|").stream().filter((s) -> {
                    return !s.contains("#" + this.getKey());
                }).map((s) -> {
                    return "#" + this.getKey() + TRIM_KEY + finalHelperName + '~' + subject;
                }).forEach(result::add);
                return result;
            }
        }

        // $view->get('helper')
        if (element instanceof MethodReference) {
            if (!PhpElementsUtil.isMethodWithFirstString(element, "get")) {
                return null;
            }
            MethodReference methodReference = (MethodReference) element;

            PsiElement[] parameters = methodReference.getParameters();
            if (parameters.length < 1) {
                return null;
            }
            if (!(parameters[0] instanceof StringLiteralExpression)) {
                return null;
            }

            helperName = ((StringLiteralExpression)parameters[0]).getContents();
            if (helperName.isEmpty()) {
                return null;
            }
            refSignature = methodReference.getSignature();

            return new PhpType().add("#" + this.getKey() + refSignature + TRIM_KEY + helperName);
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String expression, Set<String> visited, int depth, Project project) {
        // get back our original call
        int endIndex = expression.lastIndexOf(TRIM_KEY);
        if(endIndex == -1) {
            return Collections.emptySet();
        }

        String originalSignature = expression.substring(0, endIndex);
        String parameter = expression.substring(endIndex + 1);

        PhpIndex phpIndex = PhpIndex.getInstance(project);

        // Method call
        if (!originalSignature.isEmpty()) {
            Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpTypeProviderUtil.getTypeSignature(phpIndex, originalSignature);
            if (phpNamedElementCollections.size() == 0) {
                return Collections.emptySet();
            }

            // get first matched item
            PhpNamedElement element = phpNamedElementCollections.iterator().next();
            if (!(element instanceof Method)) {
                return Collections.emptySet();
            }

            // method "get" of PhpEngine
            if (!PhpElementsUtil.isMethodInstanceOf((Method)element, PhpTemplatingUtil.SIGNATURE_RENDERER_COMPONENT, "get")) {
                return Collections.emptySet();
            }
        }

        // Array access
        int paramIndex = parameter.indexOf('~');
        if (paramIndex >= 0) {
            // referenced class
            String refParam = parameter.substring(paramIndex + 1);

            // helper name
            parameter = parameter.substring(0, paramIndex);

            Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpTypeProviderUtil.getTypeSignature(phpIndex, refParam);
            if (phpNamedElementCollections.size() == 0) {
                return Collections.emptySet();
            }

            // check for PhpEngine class
            PhpNamedElement element = phpNamedElementCollections.iterator().next();
            if (!PhpTemplatingUtil.isTypePhpEngine(element.getType(), phpIndex)) {
                return Collections.emptySet();
            }
        }

        String signature = PhpTemplatingUtil.findSignatureForHelper(parameter);
        if (signature == null) {
            return Collections.emptySet();
        }

        return phpIndex.getAnyByFQN(signature);
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
