package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;

import java.util.function.Consumer;

/**
 *
 */
public abstract
class PredefinedProc implements ISubroutine {
    /**
     * @return
     */
    public
    boolean isBuiltIn () {
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

        private static
        void acceptJava ( Functor functor ) {

        }

        private static
        void acceptProlog ( Functor functor ) {

        }

        private static
        void acceptWAM ( Functor functor ) {

        }

        Object proc;

        /**
         * @param proc
         */
        Lang ( Object proc ) {
            this.proc = proc;

        }

        Lang ( Consumer <Functor> proc ) {
            this.proc = proc;
        }

        Lang ( HiTalkWAMCompiledPredicate proc ) {
            this.proc = proc;
        }

    }

    /**
     *
     */
    protected Functor head;

    /**
     *
     */
    public
    PredefinedProc () {
    }

    /**
     * @return
     */
    @Override
    public
    Functor getHead () {
        return head;
    }

    /**
     * @return
     */
    @Override
    public abstract
    Functor[] getBody ();
}
