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
        DK_IF(DirectiveKind::_if),
        DK_ELSE(DirectiveKind::_else),
        DK_ELIF(DirectiveKind::elif),
        DK_ENDIF(DirectiveKind::endif),

        DK_ENCODING(DirectiveKind::encoding),
        DK_INCLUDE(DirectiveKind::include),
        DK_OP(DirectiveKind::op),
        ;

        DirectiveKind(Consumer<IFunctor> handler) {
        }

        private static void op(IFunctor functor) {
        }

        private static void include(IFunctor functor) {
        }

        private static void encoding(IFunctor functor) {
        }

        private static void endif(IFunctor functor) {
        }

        private static void elif(IFunctor functor) {
        }

        private static void _else(IFunctor functor) {
        }

        private static void _if(IFunctor functor) {
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
