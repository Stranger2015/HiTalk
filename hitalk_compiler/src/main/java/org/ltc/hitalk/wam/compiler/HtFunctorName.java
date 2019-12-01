package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.FunctorName;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IntTerm;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public
class HtFunctorName extends FunctorName implements IRangedArity {
    /**
     * Creates a functor name with the specified name and arity.
     *
     * @param name  The name of the functor.
     * @param arity The arity of the functor.
     */
    public
    HtFunctorName ( String name, int arity ) {
        super(name, arity);
    }

    /**
     * @param name
     * @param arityMin
     * @param arityMax
     */
    public
    HtFunctorName ( String name, int arityMin, int arityMax ) {//delta arity byte or short
        super(name, (arityMin & 0xfff0) | (((arityMax - arityMin) >>> 8) & 0xffff));//
    }

//    /**
//     * Gets the functors arity.
//     *
//     * @return The functors arity.
//     */
//    public
//    int getArityMin () {
//        return arity & 0xffff0000;
//    }
//
//    public
//    int getArityMax () {
//        return (arity << 16) & 0xffff;
//    }

    /**
     * Gets the functors arity.
     *
     * @return The functors arity.
     */
    @Override
    public
    int getArity () {
        throw new ExecutionError(PERMISSION_ERROR, null);//"Arity range expected");
    }

    /**
     * @return
     */
    public
    int getArityInt () {
        return super.getArity();
    }

    /**
     * @return
     */
    @Override
    public ITerm getArityTerm () {
        new HtFunctorName(getName(), getArityMin(), getArityDelta());
        int i = getArityInt();
        return hash(i);
    }

    /**
     * @param i
     * @return
     */
    private ITerm hash ( int i ) {
        return new IntTerm(i);
    }


    @Override
    public
    String toString () {
        return String.format("HtFunctorName{%s/%d-%d}", name, getArityMin(), getArityDelta());
    }
}
