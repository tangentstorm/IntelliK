package com.tangentcode.intellik.dialect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The K dialects IntelliK knows about. Each dialect's primitives live in
 * {@code resources/dialects/<id>.json}; only K3 has a table so far.
 */
public enum KDialect {
    K3("k3", "K3", KScoping.ONE_LEVEL),
    Q("q", "q / k4", KScoping.LOCAL_OR_GLOBAL),
    KONA("kona", "Kona", KScoping.ONE_LEVEL),
    KSHARP("ksharp", "K#", KScoping.ONE_LEVEL),
    NGN("ngn", "ngn/k", KScoping.LOCAL_OR_GLOBAL),
    OK("ok", "oK", KScoping.LEXICAL),
    K9("k9", "k9", KScoping.LOCAL_OR_GLOBAL);

    public static final KDialect DEFAULT = K3;

    private final String id;
    private final String displayName;
    private final KScoping scoping;

    KDialect(String id, String displayName, KScoping scoping) {
        this.id = id;
        this.displayName = displayName;
        this.scoping = scoping;
    }

    /** How nested lambdas see the names of the lambdas around them. */
    @NotNull
    public KScoping getScoping() {
        return scoping;
    }

    /** Stable identifier, used in settings and as the primitive table file name. */
    @NotNull
    public String getId() {
        return id;
    }

    @NotNull
    public String getDisplayName() {
        return displayName;
    }

    /** Looks up a dialect by id, falling back to {@link #DEFAULT} for unknown or missing ids. */
    @NotNull
    public static KDialect fromId(@Nullable String id) {
        for (KDialect d : values()) {
            if (d.id.equals(id)) return d;
        }
        return DEFAULT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
