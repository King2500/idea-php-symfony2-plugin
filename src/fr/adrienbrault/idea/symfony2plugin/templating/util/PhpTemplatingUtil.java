package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.ArrayIndex;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

final public class PhpTemplatingUtil {

    public final static String SIGNATURE_RENDERER_FRAMEWORK = "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\PhpEngine";
    public final static String SIGNATURE_RENDERER_COMPONENT = "\\Symfony\\Component\\Templating\\PhpEngine";
    private final static String SIGNATURE_GLOBAL_VARS = "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\GlobalVariables";
    private final static String SIGNATURE_FULLSTACK = "\\Symfony\\Bundle\\FullStack";

    private static Map<String, String> VARIABLES = new HashMap<String, String>() {{
        put("view", SIGNATURE_RENDERER_COMPONENT);
    }};

    private static Map<String, String> VARIABLES_FRAMEWORK = new HashMap<String, String>() {{
        put("view", SIGNATURE_RENDERER_FRAMEWORK);
        put("app", SIGNATURE_GLOBAL_VARS);
    }};

    private static Map<String, String> HELPERS = new HashMap<String, String>() {{
        put("slots", "\\Symfony\\Component\\Templating\\Helper\\SlotsHelper");
        put("request", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\RequestHelper");
        put("session", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\SessionHelper");
        put("router", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\RouterHelper");
        put("assets", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\AssetsHelper");
        put("actions", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\ActionsHelper");
        put("code", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\CodeHelper");
        put("translator", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\TranslatorHelper");
        put("form", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\FormHelper");
        put("stopwatch", "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\Helper\\StopwatchHelper");
        put("security", "\\Symfony\\Bundle\\SecurityBundle\\Templating\\Helper\\SecurityHelper");
        put("logout_url", "\\Symfony\\Bundle\\SecurityBundle\\Templating\\Helper\\LogoutUrlHelper");
    }};

    public static boolean isPhpTemplate(@NotNull PsiFile file) {
        if (!(file instanceof PhpFile)) {
            return false;
        }
        return file.getName().endsWith(".html.php");
        // TODO: Also check file is in namespaced templates path ("views") or Symfony widget
    }

    public static String getPhpEngineClass(PhpIndex phpIndex) {
        if (phpIndex.getClassesByFQN(SIGNATURE_FULLSTACK).size() > 0) {
            return SIGNATURE_RENDERER_FRAMEWORK;
        }
        // TODO: better decision which Renderer class is used (maybe user setting)
        return SIGNATURE_RENDERER_COMPONENT;
    }

    public static boolean isTypePhpEngine(@NotNull PhpType type, @NotNull PhpIndex phpIndex) {
        return type.toString().contains(getPhpEngineClass(phpIndex));
    }

    public static Map<String, String> getHelpersMap() {
        return HELPERS;
    }

    @NotNull
    public static ElementPattern<? extends PsiElement> getHelperAccessPattern() {
        return PlatformPatterns.or(
            PlatformPatterns.psiElement().withSuperParent(2, ArrayIndex.class),
            PhpElementsUtil.getParameterInsideMethodReferencePattern()
        );
    }

    public static Map<String, String> getTemplateVariablesForFile(@NotNull PhpFile file) {
        // when using PhpEngine from FrameworkBundle
        if (SIGNATURE_RENDERER_FRAMEWORK.equals(getPhpEngineClass(PhpIndex.getInstance(file.getProject())))) {
            // add variable "app"
            VARIABLES.putAll(VARIABLES_FRAMEWORK);
        }

        // TODO: also check for custom template variables:  render(..., array('foo'=>$foo))
        return VARIABLES;
    }

    public static String findSignatureForTemplateVariable(@NotNull PhpFile phpFile, @NotNull String variableName) {
        Map<String, String> variables = getTemplateVariablesForFile(phpFile);

        for (Map.Entry<String, String> variable: variables.entrySet()) {
            if (variable.getKey().equals(variableName)) {
                return variable.getValue();
            }
        }

        return null;
    }

    public static String findSignatureForHelper(String helperName) {
        if (helperName == null) {
            return null;
        }

        // TODO: better get helpers from Service tags ("templating.helper") or calls to PhpEngine::set or HelperInterface implementations (weak)

        for (Map.Entry<String, String> helper: HELPERS.entrySet()) {
            if (helper.getKey().equals(helperName)) {
                return helper.getValue();
            }
        }

        return null;
    }
}
