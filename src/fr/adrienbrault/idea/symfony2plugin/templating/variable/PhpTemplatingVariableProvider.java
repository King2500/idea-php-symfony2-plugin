package fr.adrienbrault.idea.symfony2plugin.templating.variable;

import com.intellij.util.containers.ArrayListSet;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.PhpPredefinedVariableProvider;
import fr.adrienbrault.idea.symfony2plugin.templating.util.PhpTemplatingUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PhpTemplatingVariableProvider implements PhpPredefinedVariableProvider {

    @NotNull
    @Override
    public Set<CharSequence> getPredefinedVariables(@NotNull PhpFile file) {

        if (!PhpTemplatingUtil.isPhpTemplate(file)) {
            return Collections.emptySet();
        }

        return new HashSet<>(PhpTemplatingUtil.getTemplateVariablesForFile(file).keySet());
    }
}
