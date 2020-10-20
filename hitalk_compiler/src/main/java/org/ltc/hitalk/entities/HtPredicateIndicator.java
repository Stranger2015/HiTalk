package org.ltc.hitalk.entities;

import org.ltc.enumus.Hierarchy;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public
class HtPredicateIndicator extends HtBaseTerm implements IFunctor {

    protected HtFunctor delegate;

    /**
     * @param name
     * @param arg1
     * @param arg2
     */
    public HtPredicateIndicator(IFunctor name, ITerm arg1, ITerm arg2) {
//        delegate = new HtFunctor(name.getName(), new ListTerm(name, arg1, arg2));fixme
    }

    public HtPredicateIndicator(IFunctor functor) {
        this(functor, functor.getArgument(0), functor.getArgument(1));
    }

    public HtPredicateIndicator(IFunctor name, int arityMin, int arityMax) {

    }

    /**
     * Reports whether or not this term is a functor.
     *
     * @return Always <tt>true</tt>.
     */
    @Override
    public boolean isFunctor() {
        return delegate.isFunctor();
    }

    /**
     * Reports whether or not this term is a constant (a number or a functor of arity zero).
     *
     * @return <tt>true</tt> if this functor has arity zero, <tt>false</tt> otherwise.
     */
    @Override
    public boolean isConstant() {
        return delegate.isConstant();
    }

    /**
     * Reports whether or not this term is compound (a functor of arity one or more).
     *
     * @return <tt>true</tt> if this funtor has arity more than zero, <tt>fals</tt> otherwise.
     */
    @Override
    public boolean isCompound() {
        return delegate.isCompound();
    }

    /**
     * Reports whether or not this term is an atom (a functor of arity zero).
     *
     * @return <tt>true</tt> if this functor has arity zero, <tt>false</tt> otherwise.
     */
    @Override
    public boolean isAtom() {
        return delegate.isAtom();
    }

    /**
     * Reports whether or not this term is a ground term. Constants (functors of arity zero) and numbers are ground
     * terms, as are functors all of the arguments of which are ground term.
     *
     * @return <tt>true</tt> if this functor is a ground term, <tt>false</tt> othewise.
     */
    @Override
    public boolean isGround() {
        return delegate.isGround();
    }

    @Override
    public boolean isHiLog() {
        return false;
    }

    /**
     * @return
     */
    public boolean isJavaObject() {
        return false;
    }

    /**
     * Frees all assigned variables in the term, leaving them unnassigned. In the case of functors and other compund
     * structures, this recurses down into the term calling free on all argument, so that the call reaches all variables
     * in leaf positions of the term.
     */
    @Override
    public void free() {
        delegate.free();
    }

    /*
     * Gets the actual value of a term, which is either the term itself, or in the case of variables, the value that is
     * currently assigned to the variable. For functors, the value is the functor itself.
     *
     * @return The functor itself.
     */
    @Override
    public ITerm getValue() {
        return delegate.getValue();
    }

    /**
     * Gets the argument within the functor with the specified index.
     *
     * @param index The index to get the argument for.
     * @return The argument.
     */
    public ITerm getArgument(int index) {
        return delegate.getArgument(index);
    }

    /**
     * Provides all of this functors arguments.
     *
     * @return All of this functors arguments, possibly <tt>null</tt>.
     */
    public List<ITerm> getArguments() {
        return delegate.getArguments();
    }

    /**
     * Sets the argument within the functor to the specified value.
     *
     * @param index The index to set the argument for.
     * @param value The argument.
     */
    public void setArgument(int index, ITerm value) {
        delegate.setArgument(index, value);
    }

    public void setArguments(List<ITerm> terms) {
        delegate.setArguments(terms);
    }

    public ListTerm getArgs() {
        return delegate.getArgs();
    }

    /**
     * Reports the number of arguments that this functor takes.
     *
     * @return The number of arguments that this functor takes.
     */
    public int getArity() {
        return delegate.getArity();
    }

    /**
     * @return
     */
    @Override
    public boolean isDefined() {
        return false;
    }

    /**
     * @return
     */
    public int getHeadsOffset() {
        return 1;
    }

    /**
     * Reports the name of this functor, or of the set that it denotes.
     *
     * @return This functors name.
     */
    @Override
    public int getName() throws Exception {
        IFunctor t = (IFunctor) getAppContext().getParser().parseSingleTerm("P/N");
        return t.getName();

    }

    /**
     * Compares this term for structural equality with another. Two terms are structurally equal if they are the same
     * functor with the same arguments, or are the same unbound variable, or the bound values of the left or right
     * variable operands are structurally equal. Structural equality is a stronger equality than unification and unlike
     * unification it does not produce any variable bindings. Two unified terms will always be structurally equal.
     *
     * @param term The term to compare with this one for structural equality.
     * @return <tt>true</tt> if the two terms are structurally eqaul, <tt>false</tt> otherwise.
     */
    @Override
    public boolean structuralEquals(ITerm<?> term) {
        return delegate.structuralEquals(term);
    }

    /**
     * Compares this functor with another, to check if they are equal by name and arity. Two functors deemed equal by
     * this method, may not be fully equal terms in first order logic. This equality method checks the natural key, that
     * is, the name and arity, of the functor only. It is intended to be used to skim check potentially unifiable
     * functors, and in data structures that need to efficiently compare functors that may be unifiable.
     * <p>
     * <p/>Another way of looking at it is to say that, if this method returns false, the two functors are definitely
     * not equal, and cannot be unified. The false answer is the definite one. If this method returns true, the two
     * functors may be unifiable.
     *
     * @param comparator The object to compare to.
     * @return <tt>true</tt> if the comparator has the same name and arity as this one, <tt>false</tt> otherwise.
     */
    @Override
    public boolean equals(Object comparator) {
        return delegate.equals(comparator);
    }

    /**
     * Computes a hash code for this functor that is based on the same fields as the {@link #equals} method, that is,
     * the functors name and arity.
     *
     * @return A hash code based on the name and arity.
     */
    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    /**
     * Provides an iterator over the child terms, if there are any. Only functors are compound, and built across a list
     * of child arguments.
     *
     * @param reverse Set, if the children should be presented in reverse order.
     * @return The sub-terms of a compound term.
     */
    @Override
    public Iterator<HtVariable> getChildren(boolean reverse) {
        return delegate.getChildren(reverse);
    }

    /**
     * Makes a clone of the term, converting its variables to refer directly to their storage cells.
     *
     * @return A copy of this term, with entirely independent variables to the term it was copied from.
     */
    @Override
    public IFunctor queryConversion() {
        return delegate.queryConversion();
    }

    /**
     * Creates a string representation of this functor, mostly used for debugging purposes.
     *
     * @return A string representation of this functor.
     */
    @Override
    public String toString() {
        return delegate.toString();
    }

    /**
     * {@inheritDoc}
     *
     * @param visitor
     */
    @Override
    public void accept(ITermVisitor visitor) {
        delegate.accept(visitor);
    }

    /**
     * {@inheritDoc}
     *
     * @param transformer
     */
//    @Override
    public List<ITerm> acceptTransformer(ITermTransformer transformer) {
        return delegate.acceptTransformer(transformer);
    }

    /**
     * {@inheritDoc}
     *
     * @param interner
     * @param printVarName
     * @param printBindings
     */
//    @Override
    public String toString(IVafInterner interner, boolean printVarName, boolean printBindings) {
        return delegate.toString(interner, printVarName, printBindings);
    }

    /**
     * Creates a string representation of this functors arguments, mostly used for debugging purposes.
     *
     * @return A string reprenestation of this functors arguments.
     */
    public String toStringArguments() {
        return delegate.toStringArguments();
    }

    /**
     * @return
     */
    public int getArityInt() {
        return 0;
    }

    /**
     * @return
     */
    public ITerm getArityTerm() {
        return null;
    }
//

    /**
     *
     */
    public static
    class HtProperties extends Hierarchy {
        /**
         * @param type
         * @param parentAccessor
         */
        public HtProperties(Class type, Function parentAccessor) {
            super(type, parentAccessor);
        }
    }
}
