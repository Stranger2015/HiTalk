package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;

import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public class HtFunctor extends Functor implements IFunctor {

    /**
     * @param name
     * @param arityMin
     * @param arityDelta
     */
    public HtFunctor ( int name, int arityMin, int arityDelta ) {
        super(name, EMPTY_TERM_ARRAY);
        setArityRange(arityMin, arityDelta);
    }

    /**
     * @param name
     * @param args
     */
    public HtFunctor ( int name, Term[] args ) {
        this(name, args, 0);
    }

    /**
     * @param name
     * @param args
     * @param arityDelta
     */
    public HtFunctor ( int name, Term[] args, int arityDelta ) {
        super(name, args);
        setArityRange(args.length, arityDelta);
    }

    /**
     * Reports the number of arguments that this functor takes.
     *
     * @return The number of arguments that this functor takes.
     */
    @Override
    public int getArity () {
        return super.getArity();
    }

    /**
     * @return
     */
    @Override
    public String toStringArguments () {
        return super.toStringArguments();
    }

    /**
     * @return
     */
    @Override
    public int getArityInt () {
        return super.getArity();
    }

    /**
     * @return
     */
    @Override
    public Term getArityTerm () {
        return null;
    }
}
