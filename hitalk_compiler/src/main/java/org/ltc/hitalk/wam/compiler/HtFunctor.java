package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.search.Operator;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.wam.printer.HtFunctorTraverser;

import java.util.*;
import java.util.stream.IntStream;

import static java.lang.String.format;
import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public class HtFunctor extends BaseTerm implements IFunctor {

    protected final int name;
    protected Term[] args;

    /**
     * @param name
     * @param arityMin
     * @param arityDelta
     */
    public HtFunctor ( int name, int arityMin, int arityDelta ) {
        this.name = name;
        args = EMPTY_TERM_ARRAY;

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
        this.name = name;
        this.args = args;
//        super(name, args);
        setArityRange(args.length, arityDelta);
    }

    public int getName () {
        return name;
    }

    public Term[] getArguments () {
        return args;
    }

    public Term getArgument ( int i ) {
        return args[i];
    }

    /**
     * @return
     */
    @Override
    public boolean isDefined () {
        return false;
    }

    /**
     * @return
     */
    @Override
    public int getArityInt () {
        return getArity();
    }

    /**
     * @return
     */
    @Override
    public Term getArityTerm () {
        return null;
    }

    /**
     * @return
     */
    @Override
    public Term getValue () {
        return this;
    }

    /**
     * Frees all assigned variables in the term, leaving them unnassigned. In the case of functors and other compund
     * structures, this recurses down into the term calling free on all argument, so that the call reaches all variables
     * in leaf positions of the term.
     */
    @Override
    public void free () {
        for (Term arg : args) {
            arg.free();
        }
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
    public boolean structuralEquals ( Term term ) {
        Term comparator = term.getValue();

        if (this == comparator) {
            return true;
        }

        if ((comparator == null) || !(getClass().isAssignableFrom(comparator.getClass()))) {
            return false;
        }

        IFunctor functor = (IFunctor) comparator;

        if ((args.length != functor.getArity()) || (name != functor.getName())) {
            return false;
        }
        // Check the arguments of this functor and the comparator for structural equality.
        boolean passedArgCheck = true;
        for (int i = 0; i < args.length; i++) {
            Term leftArg = args[i];
            Term rightArg = functor.getArgument(i);
            if (!leftArg.structuralEquals(rightArg)) {
                passedArgCheck = false;
                break;
            }
        }

        return passedArgCheck;
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
    public boolean equals ( Object comparator ) {
        if (this == comparator) {
            return true;
        }

        if ((comparator == null) || (getClass() != comparator.getClass())) {
            return false;
        }

        Functor functor = (Functor) comparator;

        return (args.length == functor.getArity() && (name == functor.getName()));
    }

    /**
     * Computes a hash code for this functor that is based on the same fields as the {@link #equals} method, that is,
     * the functors name and arity.
     *
     * @return A hash code based on the name and arity.
     */
    public int hashCode () {
        int result;
        result = name;
        result = (31 * result) + args.length;

        return result;
    }

    /**
     * Provides an iterator over the child terms, if there are any. Only functors are compound, and built across a list
     * of child arguments.
     *
     * @param reverse Set, if the children should be presented in reverse order.
     * @return The sub-terms of a compound term.
     */
    public Iterator <Operator <Term>> getChildren ( boolean reverse ) {
        if ((traverser != null) && (traverser instanceof HtFunctorTraverser)) {
            return ((HtFunctorTraverser) traverser).traverse(this, reverse);
        } else {
            if (args == null) {
                return Collections.emptyIterator();
            } else if (!reverse) {
                return Arrays.asList((Operator <Term>[]) args).iterator();
            } else {
                List <Operator <Term>> argList = new LinkedList <>();

                for (int i = args.length - 1; i >= 0; i--) {
                    argList.add(args[i]);
                }

                return argList.iterator();
            }
        }
    }

    /**
     * Makes a clone of the term, converting its variables to refer directly to their storage cells.
     *
     * @return A copy of this term, with entirely independent variables to the term it was copied from.
     */
    public IFunctor queryConversion () {
        /*log.fine("public Functor queryConversion(): called)");*/

        IFunctor copy = (IFunctor) super.queryConversion();
        copy.setArguments(new Term[args.length]);
        IntStream.range(0, args.length).forEachOrdered(i ->
                copy.setArgument(i, queryConversion()));

        return copy;
    }

    /**
     * Creates a string representation of this functor, mostly used for debugging purposes.
     *
     * @return A string representation of this functor.
     */
    public String toString () {
        return format("%s: [ name = %d, arity = %d, arguments = %s ]",
                getClass().getSimpleName(), name, args.length, toStringArguments());
    }

    /**
     * {@inheritDoc}
     */
    public void accept ( TermVisitor visitor ) {
        if (visitor instanceof FunctorVisitor) {
            visitor.visit(this);
        } else {
            super.accept(visitor);
        }
    }

    /**
     * {@inheritDoc}
     */
    public IFunctor acceptTransformer ( TermTransformer transformer ) {
        IFunctor result;

        if (transformer instanceof FunctorTransformer) {
            result = (IFunctor) transformer.transform(this);
        } else {
            result = (IFunctor) super.acceptTransformer(transformer);
        }

        IntStream.range(0, args.length).forEachOrdered(i ->
                result.getArguments()[i] = args[i].acceptTransformer(transformer));

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        if (name < 0) {
            return "internal_built_in";
        }

        StringBuilder result = new StringBuilder(interner.getFunctorName(name));

        if (args.length > 0) {
            result.append("(");

            IntStream.range(0, args.length).forEachOrdered(i -> {
                Term nextArg = args[i];
                result.append(nextArg.toString(interner, printVarName, printBindings));
                result.append((i < (args.length - 1)) ? ", " : "");
            });

            result.append(")");
        }

        return result.toString();
    }

    /**
     * Creates a string representation of this functors arguments, mostly used for debugging purposes.
     *
     * @return A string reprenestation of this functors arguments.
     */
    public String toStringArguments () {
        StringBuilder result = new StringBuilder();

        if (args.length > 0) {
            result.append("[ ");

            IntStream.range(0, args.length).forEachOrdered(i -> {
                Term nextArg = args[i];
                result.append((nextArg != null) ? nextArg.toString() : "<null>");
                result.append((i < (args.length - 1)) ? ", " : " ");
            });

            result.append(" ]");
        }

        return result.toString();
    }

    /**
     * @param i
     * @param term
     */
    @Override
    public void setArgument ( int i, Term term ) {
        args[i] = term;
    }

    /**
     * @param terms
     */
    @Override
    public void setArguments ( Term[] terms ) {
        args = terms;
    }
}
