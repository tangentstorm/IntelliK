package com.tangentcode.intellik.dialect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The K dialects IntelliK knows about. Each dialect's primitives live in
 * {@code resources/dialects/<id>.json}; only K3 has a table so far.
 */
public enum KDialect {
    K3("k3", "K3"),
    Q("q", "q / k4"),
    KONA("kona", "Kona"),
    KSHARP("ksharp", "K#"),
    NGN("ngn", "ngn/k"),
    OK("ok", "oK"),
    K9("k9", "k9");

    public static final KDialect DEFAULT = K3;

    private final String id;
    private final String displayName;

    KDialect(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
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
