package org.ltc.hitalk.parser;

import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.function.Consumer;

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
        IF(functor -> {
        }),
        ELSE(functor -> {
        }),
        ELIF(functor -> {
        }),
        ENDIF(functor -> {
        }),

        ENCODING(functor -> {
        }),
        INCLUDE(functor -> {
        }),
        OP(functor -> {
        }),
        ;

        DirectiveKind ( Consumer <IFunctor> handler ) {
        }
    }

    public IFunctor getDef () {
        return getGoal(0);
    }

//    protected IFunctor def;

    public DirectiveKind getKind () {
        return kind;
    }

    /**
     *
     */
    protected DirectiveKind kind;
}
