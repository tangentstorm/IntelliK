package com.tangentcode.intellik.psi;

import com.intellij.psi.PsiNameIdentifierOwner;

/** A K declaration with a renamable name: an assignment ({@link KDefine}) or a lambda parameter ({@link KParam}). */
public interface KNamedElement extends PsiNameIdentifierOwner {
}
