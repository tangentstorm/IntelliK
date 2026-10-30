package com.x1010data.intellik.dialect;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decides which dialect applies to a file.
 *
 * <p>For now every file uses the project setting. Planned resolution order:
 * per-file override, then a {@code / dialect: ...} header comment, then the
 * file extension ({@code .q} means q), then the project setting.
 */
@Service(Service.Level.PROJECT)
public final class KDialectService {

    private final Project project;

    public KDialectService(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public static KDialectService getInstance(@NotNull Project project) {
        return project.getService(KDialectService.class);
    }

    @NotNull
    public KDialect dialectFor(@Nullable VirtualFile file) {
        // TODO: per-file override, "/ dialect:" header and .q -> Q mapping.
        return KDialectSettings.getInstance(project).getDialect();
    }

    @NotNull
    public KDialect dialectFor(@NotNull PsiFile file) {
        return dialectFor(file.getOriginalFile().getVirtualFile());
    }

    @NotNull
    public PrimitiveTable tableFor(@NotNull PsiFile file) {
        return PrimitiveTable.forDialect(dialectFor(file));
    }

    /** Table for a file when only the project may be known (lexer and highlighter factories). */
    @NotNull
    public static PrimitiveTable tableFor(@Nullable Project project, @Nullable VirtualFile file) {
        if (project == null || project.isDefault() || project.isDisposed()) {
            return PrimitiveTable.forDialect(KDialect.DEFAULT);
        }
        return PrimitiveTable.forDialect(getInstance(project).dialectFor(file));
    }
}
