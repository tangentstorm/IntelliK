package com.x1010data.intellik.psi.impl;

import com.intellij.lang.ASTNode;
import com.x1010data.intellik.psi.KTypes;
import com.x1010data.intellik.psi.KAssign;

/**
 * Created by michal.wallace on 6/16/16.
 */
public class KPsiImpUtil {
    public static String getKey(KAssign el) {
        ASTNode idNode = el.getNode().findChildByType(KTypes.IDENT);
        return idNode == null ? null : idNode.getText();
    }
}
