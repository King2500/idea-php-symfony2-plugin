package fr.adrienbrault.idea.symfony2plugin.lang;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.StringLiteralExpressionImpl;
import fr.adrienbrault.idea.symfony2plugin.Symfony2ProjectComponent;
import fr.adrienbrault.idea.symfony2plugin.util.MethodMatcher;
import fr.adrienbrault.idea.symfony2plugin.util.PsiElementUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ParameterLanguageInjector implements MultiHostInjector {

    private static final MethodParameterSignature[] CSS_SELECTOR_SIGNATURES = {
        new MethodParameterSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "filter", 0),
        new MethodParameterSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "children", 0),
        new MethodParameterSignature("\\Symfony\\Component\\CssSelector\\CssSelectorConverter", "toXPath", 0),
    };

    private static final MethodParameterSignature[] XPATH_SIGNATURES = {
        new MethodParameterSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "filterXPath", 0),
        new MethodParameterSignature("\\Symfony\\Component\\DomCrawler\\Crawler", "evaluate", 0),
    };

    private static final MethodParameterSignature[] JSON_SIGNATURES = {
        //new MethodParameterSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "__construct", 0),
        new MethodParameterSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "fromJsonString", 0),
        new MethodParameterSignature("\\Symfony\\Component\\HttpFoundation\\JsonResponse", "setJson", 0),
    };

    private static final MethodParameterSignature[] DQL_SIGNATURES = {
        new MethodParameterSignature("\\Doctrine\\ORM\\EntityManager", "createQuery", 0),
        new MethodParameterSignature("\\Doctrine\\ORM\\Query", "setDQL", 0),
    };

    private static final MethodParameterSignature[] PHPREGEXP_SIGNATURES = {
        new MethodParameterSignature("\\Symfony\\Bridge\\PhpUnit\\DeprecationErrorHandler\\Configuration", "fromRegex", 0),
        new MethodParameterSignature("\\Symfony\\Bundle\\FrameworkBundle\\CacheWarmer\\AnnotationsCacheWarmer", "__construct", 2),
        new MethodParameterSignature("\\Symfony\\Component\\Config\\Resource\\DirectoryResource", "__construct", 1),
        new MethodParameterSignature("\\Symfony\\Component\\Console\\Question\\ConfirmationQuestion", "__construct", 2),
        new MethodParameterSignature("\\Symfony\\Component\\CssSelector\\Parser\\Reader", "findPattern", 0),
        new MethodParameterSignature("\\Symfony\\Component\\HttpFoundation\\AcceptHeader", "filter", 0),
        new MethodParameterSignature("\\Symfony\\Component\\Security\\Http\\HttpUtils", "__construct", 2),
        new MethodParameterSignature("\\Symfony\\Component\\Security\\Http\\HttpUtils", "__construct", 3),
        new MethodParameterSignature("\\Symfony\\Component\\Validator\\Mapping\\Loader\\PropertyInfoLoader", "__construct", 2),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasRecordThatMatches", 0),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasEmergencyThatMatches", 0),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasAlertThatMatches", 0),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasCriticalThatMatches", 0),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasErrorThatMatches", 0),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasWarningThatMatches", 0),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasNoticeThatMatches", 0),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasInfoThatMatches", 0),
        new MethodParameterSignature("\\Monolog\\Handler\\TestHandler", "hasDebugThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasRecordThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasEmergencyThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasAlertThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasCriticalThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasErrorThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasWarningThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasNoticeThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasInfoThatMatches", 0),
        new MethodParameterSignature("\\Psr\\Log\\Test\\TestLogger", "hasDebugThatMatches", 0),
    };

    private final MethodLanguageInjection[] LANGUAGE_INJECTIONS = {
        new MethodLanguageInjection(LANGUAGE_ID_CSS, "@media all { ", " }", CSS_SELECTOR_SIGNATURES),
        new MethodLanguageInjection(LANGUAGE_ID_XPATH, null, null, XPATH_SIGNATURES),
        new MethodLanguageInjection(LANGUAGE_ID_JSON, null, null, JSON_SIGNATURES),
        new MethodLanguageInjection(LANGUAGE_ID_DQL, null, null, DQL_SIGNATURES),
        new MethodLanguageInjection(LANGUAGE_ID_PHPREGEXP, null, null, PHPREGEXP_SIGNATURES),
    };

    public static final String LANGUAGE_ID_CSS = "CSS";
    public static final String LANGUAGE_ID_XPATH = "XPath";
    public static final String LANGUAGE_ID_JSON = "JSON";
    public static final String LANGUAGE_ID_DQL = "DQL";
    public static final String LANGUAGE_ID_PHPREGEXP = "PhpRegExp";

    private static final String DQL_VARIABLE_NAME = "dql";

    public ParameterLanguageInjector() {
    }

    @NotNull
    @Override
    public List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
        return Collections.singletonList(StringLiteralExpressionImpl.class);
    }

    @Override
    public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement element) {
        if (!(element instanceof StringLiteralExpression) || !((PsiLanguageInjectionHost) element).isValidHost()) {
            return;
        }
        if (!Symfony2ProjectComponent.isEnabled(element.getProject())) {
            return;
        }

        final StringLiteralExpressionImpl expr = (StringLiteralExpressionImpl) element;

        PsiElement parent = expr.getParent();

        final boolean isParameter = parent instanceof ParameterList && expr.getPrevPsiSibling() == null; // 1st parameter
        final boolean isAssignment = parent instanceof AssignmentExpression;

        if (!isParameter && !isAssignment) {
            return;
        }

        if (isParameter) {
            parent = parent.getParent();
        }

        for (MethodLanguageInjection languageInjection : LANGUAGE_INJECTIONS) {
            Language language = languageInjection.getLanguage();
            if (language == null) {
                continue;
            }
            // $crawler->filter('...')
            // $em->createQuery('...')
            // JsonResponse::fromJsonString('...')
            if (parent instanceof MethodReference) {
                for (MethodParameterSignature signature : languageInjection.getSignatures()) {

                    PsiElement psiNearestParameter = PsiElementUtils.getParentOfTypeFirstChild(expr, ParameterList.class);
                    if (psiNearestParameter == null) {
                        continue;
                    }

                    MethodMatcher.CallToSignature callToSignature = new MethodMatcher.CallToSignature(signature.getClassName(), signature.getMethodName());
                    MethodMatcher.MethodMatchParameter matchParameter = MethodMatcher.getMatchedSignatureWithDepth(psiNearestParameter, new MethodMatcher.CallToSignature[]{callToSignature}, signature.getParameterIndex());
                    if (matchParameter != null) {
                        injectLanguage(registrar, expr, language, languageInjection);
                        return;
                    }
                }
            }
            // $dql = "...";
            else if (parent instanceof AssignmentExpression) {
                if (LANGUAGE_ID_DQL.equals(language.getID())) {
                    PhpPsiElement variable = ((AssignmentExpression) parent).getVariable();
                    if (variable instanceof Variable) {
                        if (DQL_VARIABLE_NAME.equals(variable.getName())) {
                            injectLanguage(registrar, expr, language, languageInjection);
                            return;
                        }
                    }
                }
            }
        }

    }

    private void injectLanguage(@NotNull MultiHostRegistrar registrar, @NotNull StringLiteralExpressionImpl element, Language language, MethodLanguageInjection languageInjection) {
        final int length = ((StringLiteralExpression) element).getContents().length();
        final TextRange range = TextRange.create(1, length + 1);

        registrar.startInjecting(language)
            .addPlace(languageInjection.getPrefix(), languageInjection.getSuffix(), element, range)
            .doneInjecting();
    }

    private class MethodLanguageInjection {
        private final Language language;
        private final String prefix;
        private final String suffix;
        private final MethodParameterSignature[] signatures;

        MethodLanguageInjection(@NotNull String languageId, String prefix, String suffix, MethodParameterSignature[] signatures) {

            this.language = Language.findLanguageByID(languageId);
            this.prefix = prefix;
            this.suffix = suffix;
            this.signatures = signatures;
        }

        public Language getLanguage() {
            return language;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getSuffix() {
            return suffix;
        }

        public MethodParameterSignature[] getSignatures() {
            return signatures;
        }
    }

    private static class MethodParameterSignature {
        private final String className;
        private final String methodName;
        private final int parameterIndex;

        private MethodParameterSignature(String className, String methodName, int parameterIndex) {
            this.className = className;
            this.methodName = methodName;
            this.parameterIndex = parameterIndex;
        }

        public String getClassName() {
            return className;
        }

        public String getMethodName() {
            return methodName;
        }

        public int getParameterIndex() {
            return parameterIndex;
        }
    }
}
