package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.psi.PsiFile;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.Variable;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

final public class PhpTemplatingUtil {

    private final static String SIGNATURE_RENDERER_FRAMEWORK = "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\PhpEngine";
    private final static String SIGNATURE_RENDERER_COMPONENT = "\\Symfony\\Component\\Templating\\PhpEngine";
    private final static String SIGNATURE_GLOBAL_VARS = "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\GlobalVariables";
    private final static String SIGNATURE_FULLSTACK = "\\Symfony\\Bundle\\FullStack";

    private static Map<String, String> VARIABLES = new HashMap<String, String>() {{
        put("view", SIGNATURE_RENDERER_COMPONENT);
    }};

    private static Map<String, String> VARIABLES_FRAMEWORK = new HashMap<String, String>() {{
        put("app", SIGNATURE_GLOBAL_VARS);
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
}
