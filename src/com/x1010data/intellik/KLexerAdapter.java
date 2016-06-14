package com.x1010data.intellik;

import com.intellij.lexer.FlexAdapter;

import java.io.Reader;

/**
 * Created by michal.wallace on 6/14/16.
 */
public class KLexerAdapter extends FlexAdapter {
    public KLexerAdapter() {
        super(new KLexer((Reader) null));
    }
}
