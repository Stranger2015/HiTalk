package org.ltc.hitalk.entities;

import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import java.util.function.Consumer;

/**
 *
 */
public abstract
class PredefinedProc implements ISubroutine {
    /**
     * @return
     */
    public boolean isBuiltIn () {
        return builtIn;
    }

    boolean builtIn;

    /**
     *
     */
    enum Lang {
        JAVA(Lang::acceptJava),
        PROLOG(Lang::acceptProlog),
        WAM(Lang::acceptWAM);

        /**
         * @param functor
         */
        protected static void acceptJava ( HtFunctor functor, Consumer <HtFunctor> proc ) {
            proc.accept(functor);
        }

        /**
         * @param functor
         */
        protected static void acceptProlog ( HtFunctor functor, HtPredicate proc ) {
            final ISubroutine clause = proc.getDefinition().get(0);
        }

        /**
         * @param functor
         */
        private static void acceptWAM ( HtFunctor functor, HiTalkWAMCompiledPredicate proc ) {
            proc.
        }

        /**
         *
         */
        protected Object proc;

        /**
         * @param proc
         */
        Lang ( Consumer <HtFunctor> proc ) {
            this.proc = proc;
        }

        /**
         * @param proc
         */
        Lang ( HiTalkWAMCompiledPredicate proc ) {
            this.proc = proc;
        }

        /**
         * @param proc
         */
        Lang ( HtPredicate proc ) {
            this.proc = proc;
        }
    }

    /**
     *
     */
    protected HtFunctor head;

    /**
     *
     */
    public PredefinedProc () {
    }

    /**
     * @return
     */
    @Override
    public HtFunctor getHead () {
        return head;
    }

    /**
     * @return
     */
    @Override
    public abstract HtFunctor[] getBody ();
}
