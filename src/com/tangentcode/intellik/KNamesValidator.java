package com.tangentcode.intellik;

import com.intellij.lang.refactoring.NamesValidator;
import com.intellij.openapi.project.Project;
import com.tangentcode.intellik.dialect.KDialect;
import com.tangentcode.intellik.dialect.KDialectService;
import com.tangentcode.intellik.dialect.PrimitiveTable;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/** Valid rename targets: K identifiers that aren't keywords or builtins. */
public class KNamesValidator implements NamesValidator {

    /** Same shape as IDENT in K.flex. */
    private static final Pattern IDENT = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");

    @Override
    public boolean isKeyword(@NotNull String name, Project project) {
        PrimitiveTable table = KDialectService.tableFor(project, null);
        if (table.getKeywords().isEmpty()) table = PrimitiveTable.forDialect(KDialect.DEFAULT);
        return table.getKeywords().stream().anyMatch(k -> k.getName().equals(name))
                || table.isBuiltin(name);
    }

    @Override
    public boolean isIdentifier(@NotNull String name, Project project) {
        return IDENT.matcher(name).matches() && !isKeyword(name, project);
    }
}
