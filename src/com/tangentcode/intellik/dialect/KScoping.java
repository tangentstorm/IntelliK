package com.tangentcode.intellik.dialect;

/** How a lambda nested in another lambda sees the names around it. */
public enum KScoping {
    /**
     * Names are either local or global, so a nested lambda can't see the enclosing lambda's
     * locals (k4/q, ngn/k). Values have to be passed in, usually by projection.
     */
    LOCAL_OR_GLOBAL,
    /**
     * K2/K3: a nested lambda can read the parameters and locals of the lambda it is written in,
     * one level up. The values are copied in when the nested lambda is evaluated, so they are
     * constants inside it, and assigning the name there makes a new local instead.
     */
    ONE_LEVEL,
    /** oK: lexical closures that reach any number of enclosing lambdas. */
    LEXICAL
}
