package org.ltc.hitalk.parser;

import org.ltc.hitalk.wam.compiler.IFunctor;

public class Directive extends HtClause {
    /**
     * Creates a program sentence in L2.
     *
     * @param def The functor that makes up the directive body.
     */
    public Directive ( IFunctor def ) {
        super(null, def);
    }

    public enum DirectiveKind {
        //        CONDITIONAL_COMPILATION,
        IF,
        ELSE,
        ELIF,
        ENDIF,

        ENCODING,
        INCLUDE,
        OP,
    }

    protected IFunctor def;

    public DirectiveKind getKind () {
        return kind;
    }

    /**
     *
     */
    protected DirectiveKind kind;
}
