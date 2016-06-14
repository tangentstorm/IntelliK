package com.x1010data.intellik.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.x1010data.intellik.KFileType;
import com.x1010data.intellik.KLanguage;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Created by michal.wallace on 6/14/16.
 */
public class KFile extends PsiFileBase {

    public KFile(@NotNull FileViewProvider viewProvider) {
        super(viewProvider, KLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public FileType getFileType() {
        return KFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "K File";
    }

    @NotNull
    @Override
    public Icon getIcon(int flags) {
        return super.getIcon(flags);
    }
}
