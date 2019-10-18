package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public class HtFunctor extends Functor implements IRangedArity {

    /**
     * @param name
     * @param arityMin
     * @param arityDelta
     */
    public HtFunctor ( int name, int arityMin, int arityDelta ) {
        super(name, null);
        setArityRange(arityMin, arityDelta);
    }

    //    /**
//     * @param nameArgs
//     * @param arityDelta
//     */
//    public HtFunctor ( Term[] nameArgs, int arityDelta ) {
//        super(HILOG_COMPOUND, nameArgs);
//        setArityRange(nameArgs.length - 1, arityDelta);//minus head
//    }
//
//    public HtFunctor ( Term[] nameArgs ) {
//        this(nameArgs, 0);
//    }
//
    public HtFunctor ( int name, Term[] args ) {
        this(name, args, 0);
    }

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
