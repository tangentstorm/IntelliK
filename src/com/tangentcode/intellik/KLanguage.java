package com.tangentcode.intellik;

import com.intellij.lang.Language;

/**
 * Created by michal.wallace on 6/14/16.
 */
public class KLanguage extends Language {
    public static final KLanguage INSTANCE = new KLanguage();

    private KLanguage() {
        super("K");
    }

}
