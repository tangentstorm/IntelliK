package com.x1010data.intellik;

import com.intellij.openapi.fileTypes.LanguageFileType;
import org.jetbrains.annotations.*;

import javax.swing.*;

/**
 * Created by michal.wallace on 6/14/16.
 */
public class KFileType extends LanguageFileType {
    public static final KFileType INSTANCE = new KFileType();

    private KFileType(){
        super(KLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public String getName() {
        return "K Language Source File";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "K Language Source File";
    }

    @NotNull
    @Override
    public String getDefaultExtension() {
        return "k";
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return KIcons.FILE;
    }

}
