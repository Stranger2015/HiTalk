package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IntTerm;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public
class HtFunctorName implements IRangedArity {
    protected String name;
    protected int arity;

    /**
     * Creates a functor name with the specified name and arity.
     *
     * @param name  The name of the functor.
     * @param arity The arity of the functor.
     */
    public HtFunctorName(String name, int arity) {
        this.name = name;
        this.arity = arity;
    }

    /**
     * @param name
     * @param arityMin
     * @param arityMax
     */
    public
    HtFunctorName ( String name, int arityMin, int arityMax ) {//delta arity byte or short
        this(name, (arityMin & 0xfff0) | (((arityMax - arityMin) >>> 8) & 0xffff));
    }

    /**
     * Gets the functors arity.
     *
     * @return The functors arity.
     */
//    @Override
    public
    int getArity () {
        throw new ExecutionError(PERMISSION_ERROR, null);//"Arity range expected");
    }

    /**
     * @return
     */
    public
    int getArityInt () {
//        return super.getArity();
        return -1;
    }

    /**
     * @return
     */
    @Override
    public ITerm getArityTerm() {
        new HtFunctorName(getName(), getArityMin(), getArityDelta());
        int i = getArityInt();
        return hash(i);
    }

    public String getName() {
        return name;
    }

    /**
     * @param i
     * @return
     */
    private ITerm hash(int i) {
        return new IntTerm(i);
    }


    @Override
    public
    String toString () {
        return String.format("HtFunctorName{%s/%d-%d}", name, getArityMin(), getArityDelta());
    }
}
