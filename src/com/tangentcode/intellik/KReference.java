package com.tangentcode.intellik;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.IncorrectOperationException;
import com.tangentcode.intellik.psi.KNameRef;
import com.tangentcode.intellik.psi.impl.KNameRefMixin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Reference from a name to its declaration, resolved with K3 scoping by {@link KResolver}. */
public class KReference extends PsiReferenceBase<KNameRef> {

    public KReference(@NotNull KNameRef element) {
        super(element, TextRange.from(0, element.getTextLength()));
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        return KResolver.resolve(getElement());
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newName) throws IncorrectOperationException {
        ((KNameRefMixin) getElement()).rename(newName);
        return getElement();
    }

    @Override
    public Object @NotNull [] getVariants() {
        return KResolver.visibleNames(getElement()).stream()
                .map(n -> LookupElementBuilder.create(n).withTypeText("variable"))
                .toArray();
    }
}
