package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.search.Operator;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.wam.printer.IFunctorTraverser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyIterator;
import static java.util.Collections.singletonList;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.term.ListTerm.NIL;

/**
 *
 */
public class HtFunctor extends HtBaseTerm implements IFunctor {

    /**
     * view of arguments
     */
    protected ListTerm args = NIL;

    /**
     * @param name
     * @param name
     * @param args
     */
    public HtFunctor(int hilogApply, ITerm name, ListTerm args) {
        this(args.addHead(name).addHead(hilogApply));
    }

    /**
     * @param args
     */
    public HtFunctor(ListTerm args) {
        this.args = args;//name heads tail
    }

//    /**
//     * @param args
//     * @param arityDelta
//     */
//    public HtFunctor(int arityDelta, ListTerm args) {
//        this(name, args);
//        setArityRange(args.size(), arityDelta);
//    }
//

    /**
     * @param name
     */
    public HtFunctor(String name) {
        this(new ListTerm(getAppContext().getInterner().internFunctorName(name, 0)));
    }

    /**
     * @param name
     */
    public HtFunctor(int name) {
        final IntTerm n = new IntTerm(name);
        new ListTerm(singletonList(n));
    }

    public HtFunctor(HtFunctor functor, ITerm name, ListTerm args) {
        this.args = new ListTerm((args.addHead(functor, name)));
    }

    public HtFunctor(IFunctor sym, ListTerm args, ListTerm nil, List<HtClause> selectedClauses) {

    }

    public HtFunctor(int name, ListTerm args) {
        this.args = new ListTerm(singletonList(args.addHead(name)));
    }

    public HtFunctor(int name, int arityDelta, ListTerm listTerm) {


    }

    /**
     * @return
     */
    public int getHeadsOffset() {
        return 1;
    }

    /**
     * @return
     */
    @Override
    public int getName() {
        return ((IntTerm) args.getHeads().get(getHeadsOffset())).getInt();
    }

    /**
     * @return
     */
    @Override
    public ListTerm getArguments() {
        return args;
    }

    /**
     * @param i
     * @return
     */
    @Override
    public ITerm getArgument(int i) {
        return args.getHead(i);
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
    @Override
    public int getArityInt() {
        return getArity();
    }

    /**
     * @return
     */
    @Override
    public ITerm getArityTerm() {
        return null;
    }

    /**
     * @return
     */
    public boolean isHiLog() {
        return false;
    }

    /**
     * @return
     */
    @Override
    public ITerm getValue() {
        return this;
    }

    /**
     * Frees all assigned variables in the term, leaving them unnassigned. In the case of functors and other compund
     * structures, this recurses down into the term calling free on all argument, so that the call reaches all variables
     * in leaf positions of the term.
     */
    @Override
    public void free() {
        for (ITerm arg : args.getHeads()) {
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
    public boolean structuralEquals(ITerm term) {
        ITerm comparator = term.getValue();

        if (this == comparator) {
            return true;
        }

        if ((comparator == null) || !(getClass().isAssignableFrom(comparator.getClass()))) {
            return false;
        }

        IFunctor functor = (IFunctor) comparator;

        if ((args.getHeads().size() != functor.getArity())/* || (name != functor.getName())*/) {
            return false;
        }
        // Check the arguments of this functor and the comparator for structural equality.
        boolean passedArgCheck = true;
        for (int i = 0; i < args.getHeads().size(); i++) {
            ITerm leftArg = args.getHeads().get(i);
            ITerm rightArg = functor.getArgument(i);
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
     * @param that The object to compare to.
     * @return <tt>true</tt> if the comparator has the same name and arity as this one, <tt>false</tt> otherwise.
     */
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }

        if ((that == null) || (getClass() != that.getClass())) {
            return false;
        }

        IFunctor functor = (IFunctor) that;

        return (args.getHeads().size() == functor.getArity());
    }

    /**
     * Computes a hash code for this functor that is based on the same fields as the {@link #equals} method, that is,
     * the functors name and arity.
     *
     * @return A hash code based on the name and arity.
     */
    public int hashCode() {
        int result = getName();
        result = (31 * result) + args.getHeads().size();

        return result;
    }

    /**
     * Provides an iterator over the child terms, if there are any. Only functors are compound, and built across a list
     * of child arguments.
     *
     * @param reverse Set, if the children should be presented in reverse order.
     * @return The sub-terms of a compound term.
     */
    public Iterator<Operator<ITerm>> getChildren(boolean reverse) {
        if (traverser instanceof IFunctorTraverser) {
            return ((IFunctorTraverser) traverser).traverse(this, reverse);
        } else if (args == null) {
            return emptyIterator();
        } else if (!reverse) {
            List<Operator<ITerm>> argList = new ArrayList<>();

            for (int i = args.getHeads().size() - 1; i >= 0; i--) {
                argList.add(args.getHead(i));
            }

            return argList.iterator();
        }
        return null;//fixme
    }

    /**
     * Makes a clone of the term, converting its variables to refer directly to their storage cells.
     *
     * @return A copy of this term, with entirely independent variables to the term it was copied from.
     */
    public IFunctor queryConversion() {
        /*log.fine("public Functor queryConversion(): called)");*/

        IFunctor copy = (IFunctor) super.queryConversion();
        copy.setArguments(new ITerm[args.size()]);
        IntStream.range(0, args.size()).forEachOrdered(i ->
                copy.setArgument(i, queryConversion()));

        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public void accept(ITermVisitor visitor) {
        if (visitor instanceof IFunctorVisitor) {
            visitor.visit(this);
        } else {
            super.accept(visitor);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<ITerm> acceptTransformer(ITermTransformer transformer) {
        List<ITerm> result = transformer instanceof IFunctorTransformer ? transformer.transform(this) :
                super.acceptTransformer(transformer);

        //        IntStream.range(0, args.size()).forEachOrdered(i -> FIXME
//                result.getArgument(i) = args[i].acceptTransformer(transformer));

        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString(IVafInterner interner, boolean printVarName, boolean printBindings) {
//        if (getName() < 0) {
//            return "internal_built_in";
//        }

//        StringBuilder result = new StringBuilder(interner.getFunctorName(getName()));
//
//        if (args.size() > 0) {
//            result.append("(");
//
//            IntStream.range(0, args.size()).forEachOrdered(i -> {
//                ITerm nextArg = getArgument(i);
//                result.append(nextArg.toString(interner, printVarName, printBindings));
//                result.append((i < (args.size() - 1)) ? ", " : "");
//            });
//
//            result.append(")");
        return "Scheisse";
//        }

//        return result.toString();
    }

    /**
     * Creates a string representation of this functors arguments, mostly used for debugging purposes.
     *
     * @return A string reprenestation of this functors arguments.
     */
    @Override
    public String toStringArguments() {
        StringBuilder result = new StringBuilder();

        if (args.size() > 0) {
            result.append("[ ");

            IntStream.range(0, args.size()).forEachOrdered(i -> {
                ITerm nextArg = getArgument(i);
                result.append((nextArg != null) ? nextArg.toString() : "<null>");
                result.append((i < (args.size() - 1)) ? ", " : " ");
            });
            result.append(" ]");
        }

        return result.toString();
    }

    public void setArgument(int i, ITerm term) {

    }

    public void setArguments(ITerm[] terms) {

    }
}
