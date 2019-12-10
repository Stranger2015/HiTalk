package org.ltc.hitalk.term;

import com.thesett.aima.search.Operator;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.TermUtilities;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.stream.IntStream;

import static org.ltc.hitalk.term.ListTerm.Kind.values;

/**
 *
 */
public class ListTerm extends HtBaseTerm implements ITerm, IFunctor {

    public static final ITerm TRUE = new ListTerm(Kind.TRUE);
    public static final ListTerm NIL = new ListTerm(Kind.NIL);

    protected Kind kind;
    protected ITerm[] arguments;
    protected Operator <ITerm> op;

    /**
     * @param name
     * @param heads
     */
    public ListTerm ( Kind kind, int name, ITerm[] heads ) {
        this(kind, TermUtilities.prepend(heads, new IntTerm(name)));
    }

    public ListTerm ( int length ) {
        this(length == 0 ? Kind.NIL : Kind.LIST);
        ITerm[] heads = IntStream.range(0, length).mapToObj(i -> new HtVariable())
                .toArray(ITerm[]::new);
        setArguments(heads);//fixme name tail
    }

    /**
     * @param kind
     * @param arguments
     */
    public ListTerm ( Kind kind, ITerm... arguments ) {
//        super(-kind.ordinal(), arguments);
        this.kind = kind;
        this.arguments = arguments;
    }

    /**
     *
     */
    public ListTerm () {
        this(0);
    }

//    //todo name
//    public ListTerm ( Kind kind, ITerm[] heads ) {
////        this(kind, heads);
//        this.kind = kind;
//        this.heads = heads;
//    }

    /**
     * @return
     */
    public Kind getKind () {
        return values()[-getName()];
    }

//    /**
//     * args = name + heads + tail
//     *
//     * @return
//     */
//    public ITerm newTail () {
//        ITerm[] args = getArguments();
//        return args[args.length - 1];
//    }

//    /**
//     * args = name + heads + tail
//     *
//     * @return
//     */
//    public ITerm[] getHeads () {
//        ITerm[] args = getArguments();
//        if (args.length <= 1) {
//            return EMPTY_TERM_ARRAY;
//        } else {
//            int headsLen = args.length - 2;
//            ITerm[] heads = new ITerm[headsLen];
//            System.arraycopy(args, 1, heads, 0, headsLen);
//
//            return heads;
//        }
//    }

    public boolean isNil () {
        return getName() < 0;
    }

    public int size () {
        return getHeads().length;
    }

    public ITerm[] getHeads () {
        return arguments;
    }//fixme

    /**
     * @param i
     * @return
     */
    public ITerm get ( int i ) {
        if (size() == i) {
            return TRUE;
        }
        return getHeads()[i];
    }

    /**
     * @return
     */
    @Override
    public int getName () {
        if (arguments[0].isFunctor()) {// also isHilog()
            return ((IFunctor) arguments[0]).getName();
        } else {
            return -2;
        }
    }

    /**
     * @return
     */
    @Override
    public ITerm[] getArguments () {
        return arguments;
    }

    /**
     * @return
     */
    @Override
    public ListTerm getArgsAsListTerm () {
        return null;
    }

    /**
     * @param i
     * @return
     */
    public ITerm getArgument ( int i ) {
        return null;
    }

    /**
     * @return
     */
    @Override
    public int getArity () {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public boolean isList () {
        return true;
    }

    /**
     * @return
     */
    @Override
    public boolean isDefined () {
        return false;
    }

    @Override
    public String toStringArguments () {
        return null;
    }

    /**
     * @param i
     * @param term
     */
    @Override
    public void setArgument ( int i, ITerm term ) {
        getHeads()[i] = term;
    }

    /**
     * @param terms
     */
    @Override
    public void setArguments ( ITerm[] terms ) {
        arguments = terms;
    }

    /**
     * @return
     */
    @Override
    public int getArityInt () {
        return 0;
    }

    /**
     * @return
     */
    public ITerm getArityTerm () {
        return null;
    }

    /**
     * @return
     */
    @Override
    public boolean isHiLog () {
        return false;
    }

    /**
     * Frees all assigned variables in the term, leaving them unassigned.
     */
    @Override
    public void free () {

    }

    /**
     * @param interner      The interner use to provide symbol names.
     * @param printVarName  <tt>true</tt> if the names of bound variables should be printed, <tt>false</tt> if just the
     *                      binding without the variable name should be printed.
     * @param printBindings <tt>true</tt> if variable binding values should be printed, <tt>false</tt> if just the
     *                      variables name without any binding should be printed.
     * @return
     */
    @Override
    public String toString ( IVafInterner interner, boolean printVarName, boolean printBindings ) {
        return null;
    }


    /**
     * @param op
     * @return
     */
    @Override
    public ITerm getChildStateForOperator ( Operator <ITerm> op ) {
        this.op = op;
        return null;
    }//todo

    /**
     * @param isOpen
     * @return
     */
    public ITerm newTail ( boolean isOpen ) {
        return isOpen ? new HtVariable() : NIL;
    }

    /**
     *
     */
    public enum Kind {
        NIL, //"[]" "{}" "()" BY  INTERNED NAME

        LIST, //-1 [.......]
        BYPASS,//-2
        AND,//-3 blocked  term
        OR,
        NOT,
        IF,
        TRUE,
        GOAL(),
        HILOG_APPLY,
        INLINE_GOAL,
        OTHER();

        private IFunctor goal;

        /**
         * @param goal
         */
        Kind ( IFunctor goal ) {
            this.goal = goal;
        }

        /**
         *
         */
        Kind () {
        }
    }
}
