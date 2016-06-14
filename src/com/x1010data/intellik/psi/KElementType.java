package com.x1010data.intellik.psi;

import com.intellij.psi.tree.IElementType;
import com.x1010data.intellik.KLanguage;
import org.jetbrains.annotations.*;

/**
 * Created by michal.wallace on 6/14/16.
 */
public class KElementType extends IElementType {

    public KElementType(@NotNull @NonNls String debugName) {
        super(debugName, KLanguage.INSTANCE);
    }
}
