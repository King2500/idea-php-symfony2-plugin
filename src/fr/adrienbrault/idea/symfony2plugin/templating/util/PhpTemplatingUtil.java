package fr.adrienbrault.idea.symfony2plugin.templating.util;

import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPsiUtil;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import fr.adrienbrault.idea.symfony2plugin.templating.variable.dict.PsiVariable;
import fr.adrienbrault.idea.symfony2plugin.util.PhpElementsUtil;
import fr.adrienbrault.idea.symfony2plugin.util.PhpTypeProviderUtil;
import fr.adrienbrault.idea.symfony2plugin.util.SymfonyBundleUtil;
import fr.adrienbrault.idea.symfony2plugin.util.dict.SymfonyBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final public class PhpTemplatingUtil {

    public final static String SIGNATURE_RENDERER_FRAMEWORK = "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\PhpEngine";
    public final static String SIGNATURE_RENDERER_COMPONENT = "\\Symfony\\Component\\Templating\\PhpEngine";
    private final static String SIGNATURE_GLOBAL_VARS = "\\Symfony\\Bundle\\FrameworkBundle\\Templating\\GlobalVariables";
    private final static String SIGNATURE_FULLSTACK = "\\Symfony\\Bundle\\FullStack";
    public final static String VARIABLE_VIEW = "view";
    public final static String VARIABLE_APP = "app";

    private static Map<String, String> VARIABLES = new HashMap<String, String>() {{
        put(VARIABLE_VIEW, SIGNATURE_RENDERER_COMPONENT);
    }};

    private static Map<String, String> VARIABLES_FRAMEWORK = new HashMap<String, String>() {{
        put(VARIABLE_VIEW, SIGNATURE_RENDERER_FRAMEWORK);
        put(VARIABLE_APP, SIGNATURE_GLOBAL_VARS);
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
    }

    @NotNull
    public static String getPhpEngineClass(PhpIndex phpIndex) {
        if (phpIndex.getClassesByFQN(SIGNATURE_FULLSTACK).size() > 0) {
            return SIGNATURE_RENDERER_FRAMEWORK;
        }
        // TODO: better decision which Renderer class is used (maybe user setting)
        return SIGNATURE_RENDERER_COMPONENT;
    }

    public static boolean isTypePhpEngine(@NotNull PhpType type, @NotNull PhpIndex phpIndex) {
        String phpEngineClass = getPhpEngineClass(phpIndex);
        String typeClass = type.toString();

        if (!typeClass.contains("#")) {
            return typeClass.equals(phpEngineClass);
        }

        Collection<? extends PhpNamedElement> phpNamedElementCollections = PhpTypeProviderUtil.getTypeSignature(phpIndex, type.toString());
        if (phpNamedElementCollections.size() == 0) {
            return false;
        }

        for (PhpNamedElement element : phpNamedElementCollections) {
            if (element instanceof PhpClass) {
                if (element.getFQN().equals(phpEngineClass)) {
                    return true;
                }
            }
        }

        return false;
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

    public static Map<String, String> getTemplateVariablesForFile(@NotNull PsiFile file) {
        if (!isPhpTemplate(file)) {
            return Collections.emptyMap();
        }

        Project project = file.getProject();
        Map<String, String> variables = new HashMap<>(VARIABLES);

        // when using PhpEngine from FrameworkBundle
        if (SIGNATURE_RENDERER_FRAMEWORK.equals(getPhpEngineClass(PhpIndex.getInstance(project)))) {
            // add variable "app"
            variables.putAll(VARIABLES_FRAMEWORK);
        }

        // also check for custom template variables:  render(..., array('foo'=>$foo))
        for(Map.Entry<String, PsiVariable> templateVar: collectControllerTemplateVariables((PhpFile)file).entrySet()) {
            String varName = templateVar.getKey();
            String varClass = TwigTypeResolveUtil.getTypeDisplayName(project, templateVar.getValue().getTypes());

            if (varClass.indexOf("\\") > 0) {
                // make FQN
                varClass = "\\" + varClass;
            }
            variables.put(varName, varClass);
        }

        return variables;
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

    /**
     * Find a controller method which possibly rendered the template
     *
     * Foobar/Bar.html.php" => FoobarController::barAction
     * Foobar/Bar.html.php" => FoobarController::bar
     * Foobar.html.php" => FoobarController::__invoke
     */
    @NotNull
    public static Collection<Method> findPhpFileController(@NotNull PhpFile phpFile) {

        // TODO: Copied from TwigUtil.findTwigFileController. Maybe merge together?

        SymfonyBundle symfonyBundle = new SymfonyBundleUtil(phpFile.getProject()).getContainingBundle(phpFile);
        if(symfonyBundle == null) {
            return Collections.emptyList();
        }

        String relativePath = symfonyBundle.getRelativePath(phpFile.getVirtualFile());
        if(relativePath == null || !relativePath.startsWith("Resources/views/")) {
            return Collections.emptyList();
        }

        String viewPath = relativePath.substring("Resources/views/".length());

        String className = null;
        Collection<String> methodNames = new ArrayList<>();

        Matcher methodMatcher = Pattern.compile(".*/(\\w+)\\.\\w+\\.php").matcher(viewPath);
        if(methodMatcher.find()) {
            // Foobar/Bar.html.php" => FoobarController::barAction
            // Foobar/Bar.html.php" => FoobarController::bar
            methodNames.add(methodMatcher.group(1) + "Action");
            methodNames.add(methodMatcher.group(1));

            className = String.format(
                    "%sController\\%sController",
                    symfonyBundle.getNamespaceName(),
                    viewPath.substring(0, viewPath.lastIndexOf("/")).replace("/", "\\")
            );
        } else {
            // Foobar.html.php" => FoobarController::__invoke
            Matcher invokeMatcher = Pattern.compile("^(\\w+)\\.\\w+\\.php").matcher(viewPath);
            if(invokeMatcher.find()) {
                className = String.format(
                        "%sController\\%sController",
                        symfonyBundle.getNamespaceName(),
                        invokeMatcher.group(1)
                );

                methodNames.add("__invoke");
            }
        }

        // found not valid template name pattern
        if(className == null || methodNames.size() == 0) {
            return Collections.emptyList();
        }

        // find multiple targets
        Collection<Method> methods = new HashSet<>();
        for (String methodName : methodNames) {
            Method method = PhpElementsUtil.getClassMethod(phpFile.getProject(), className, methodName);
            if(method != null) {
                methods.add(method);
            }
        }

        return methods;
    }

    public static Map<String, PsiVariable> collectControllerTemplateVariables(@NotNull PhpFile phpFile) {
        Map<String, PsiVariable> vars = new HashMap<>();

//        for (Method method : findPhpFileController(phpFile)) {
//            vars.putAll(PhpMethodVariableResolveUtil.collectMethodVariables(method));
//        }

        for(Function methodIndex : getPhpFileMethodUsageOnIndex(phpFile)) {
            vars.putAll(PhpMethodVariableResolveUtil.collectMethodVariables(methodIndex));
        }

        return vars;
    }

    @Nullable
    public static PsiElement getViewVariableTarget(@NotNull Project project) {
        Method method = PhpElementsUtil.getClassMethod(project, SIGNATURE_RENDERER_COMPONENT, "evaluate");

        if (method == null) {
            return null;
        }

        // find $view = ...
        for (PsiElement element : PhpPsiUtil.getChildren(method.getLastChild(), Statement.INSTANCEOF)) {
            PsiElement assignment = element.getFirstChild();
            if (assignment == null || !(assignment instanceof AssignmentExpression)) {
                continue;
            }
            PsiElement varDecl = assignment.getFirstChild();
            if (varDecl == null || !(varDecl instanceof Variable)) {
                continue;
            }
            if (((Variable) varDecl).getName().equals(VARIABLE_VIEW)) {
                return varDecl;
            }
        }
        return null;
    }

    public static PsiElement getAppVariableTarget(@NotNull Project project) {
        Method method = PhpElementsUtil.getClassMethod(project, SIGNATURE_RENDERER_FRAMEWORK, "__construct");

        if (method == null) {
            return null;
        }

        // find $this->addGlobal('app', ...)
        for (MethodReference methodCall : PsiTreeUtil.findChildrenOfType(method.getLastChild(), MethodReference.class)) {
            if (!"addGlobal".equals(methodCall.getName())) {
                continue;
            }

            ParameterList params = PsiTreeUtil.findChildOfType(methodCall, ParameterList.class);
            if (params == null) {
                continue;
            }

            PsiElement stringParam = params.getFirstChild();
            if (!(stringParam instanceof StringLiteralExpression)) {
                continue;
            }

            // 'app'
            StringLiteralExpression nameParameter = (StringLiteralExpression) stringParam;
            if (VARIABLE_APP.equals(nameParameter.getContents())) {
                return PsiTreeUtil.findChildOfType(params, Variable.class);
            }
        }

        return null;
    }

    /**
     * Collect function variables scopes for given PHP template file
     */
    @NotNull
    public static Set<Function> getPhpFileMethodUsageOnIndex(@NotNull PhpFile phpFile) {
        return TwigUtil.getTwigFileMethodUsageOnIndex(phpFile.getProject(), TwigUtil.getTemplateNamesForFile(phpFile.getProject(), phpFile.getVirtualFile()));
    }

    @NotNull
    public static PsiElement[] getTemplateVariableTargets(@NotNull PhpFile phpFile, Variable variable) {
        String varName = variable.getName();
        Project project = phpFile.getProject();

        if (varName.equals(PhpTemplatingUtil.VARIABLE_VIEW)) {
            PsiElement target = PhpTemplatingUtil.getViewVariableTarget(project);
            if (target == null) {
                return new PsiElement[0];
            }
            return Collections.singleton(target).toArray(new PsiElement[0]);
        }

        if (varName.equals(PhpTemplatingUtil.VARIABLE_APP)) {
            // when using PhpEngine from FrameworkBundle
            if (SIGNATURE_RENDERER_FRAMEWORK.equals(getPhpEngineClass(PhpIndex.getInstance(project)))) {
                PsiElement target = PhpTemplatingUtil.getAppVariableTarget(project);
                if (target == null) {
                    return new PsiElement[0];
                }
                return Collections.singleton(target).toArray(new PsiElement[0]);
            }
        }

        for(Map.Entry<String, PsiVariable> templateVar: PhpTemplatingUtil.collectControllerTemplateVariables(phpFile).entrySet()) {
            String templateVarName = templateVar.getKey();

            if (templateVarName.equals(varName)) {
                PsiElement target = templateVar.getValue().getElement();
                return Collections.singleton(target).toArray(new PsiElement[0]);
            }
        }

        return new PsiElement[0];
    }
}
