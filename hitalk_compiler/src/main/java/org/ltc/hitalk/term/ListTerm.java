package org.ltc.hitalk.term;

import org.ltc.hitalk.compiler.IVafInterner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;
import static org.ltc.hitalk.term.ListTerm.Kind.CLAUSE_BODY;

/**
 *
 */
public class ListTerm extends HtBaseTerm {
    //    public static final ITerm TRUE = new ListTerm(Kind.TRUE);
    public static final ListTerm NIL = new ListTerm();
    protected Kind kind;//fixme encode in name
    ITerm[] heads = ITerm.EMPTY_TERM_ARRAY;
    ITerm tail = NIL;

    /**
     * @param length
     */
    public ListTerm ( int length ) {
        this(length == 0 ? Kind.NIL : Kind.LIST);
        heads = IntStream.range(0, length).mapToObj(i -> new HtVariable()).toArray(ITerm[]::new);
    }

    /**
     * @param heads
     */
    public ListTerm ( ITerm... heads ) {
        this((heads == null) || (heads.length == 0) ? 0 : heads.length);
        int bound = this.getHeads().length;
        IntStream.range(0, bound).forEach(i -> this.heads[i] = requireNonNull(heads)[i]);
    }

    /**
     *
     */
    public ListTerm () {//fixme redundant
        this(0);
    }

    /**
     * @param kind
     */
    public ListTerm ( Kind kind ) {
        this.kind = kind;
    }

    /**
     * @param kind
     * @param tail
     * @param heads
     */
    public ListTerm(Kind kind, ITerm tail, ITerm... heads) {
        this(kind);
        setHeads((heads.length > 0) ? Arrays.copyOf(heads, heads.length - 1) : EMPTY_TERM_ARRAY);
        this.tail = tail;
    }

    public ListTerm(ITerm tail, ITerm[] toArray) {
        this(Kind.LIST, tail, toArray);
    }

    public ListTerm(ListTerm.Kind kind, List<ITerm> heads, ITerm tail) {

    }

    /**
     * @return
     */
    public Kind getKind() {
        return kind;
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

    /**
     * args = name + heads + tail
     *
     * @return
     */
    public ITerm[] getHeads () {
//        ITerm[] args = getArguments();
//        if (args.length <= 1) {
//            return EMPTY_TERM_ARRAY;
//        } else {
//            int headsLen = arguments.length - 2;
//            ITerm[] heads = new ITerm[headsLen];
//            System.arraycopy(arguments, 1, heads, 0, headsLen);
//
//            return heads;
//        }
        return heads;
    }

    /**
     * @return
     */
    public boolean isNil () {
        return size() == 0;
    }

    /**
     * @return
     */
    public int size () {
        return getHeads().length;
    }

    public ITerm getTail () {
        return tail;
    }

    /**
     * @param i
     * @return
     */
    public ITerm getHead ( int i ) {
        return getHeads()[i];
    }

    /**
     * @return
     */
    @Override
    public boolean isList () {
        return true;
    }

    /**
     * @param i
     * @param term
     */
    public void setHead ( int i, ITerm term ) {
        if (isConjunction(term)) {
            getHeads()[i] = term;
        }
    }

    private boolean isConjunction ( ITerm term ) {
        return (term.isList() && ((ListTerm) term).getKind() == CLAUSE_BODY);
    }

    /**
     * @return
     */
    @Override
    public boolean isHiLog () {
        return false;
    }

    /**
     * @return
     */
    public boolean isJavaObject () {
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
        final StringBuilder sb = new StringBuilder();
        switch (kind) {
            case NIL:
                sb.append("[]");
            case LIST:
                sb.append("[");
                final ITerm[] heads = getHeads();
                final int iMax = heads.length - 1;
                for (int i = 0; i < heads.length; i++) {
                    sb.append(heads[i].toString(interner, printVarName, printBindings));

                    if (i < iMax) {
                        sb.append(", ");
                    } else if (i == iMax) {
                        sb.append('|');
                    }

                    sb.append(getTail().toString(interner, printVarName, printBindings));
                    sb.append("]");
                }
            case BYPASS:
                break;
            case AND:
                break;
            case OR:
                break;
            case NOT:
                break;
            case IF:
                break;
            case TRUE:
                break;
            case GOAL:
                break;
            case HILOG_APPLY:
                break;
            case INLINE_GOAL:
                break;
            case OTHER:
                break;
        }
        return sb.toString();
    }

    /**
     * @param isOpen
     * @return
     */
    public ITerm newTail ( boolean isOpen ) {
        return isOpen ? new HtVariable() : NIL;
    }

    public void setHeads ( ITerm[] heads ) {
        this.heads = heads;
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
        OTHER(),
        CLAUSE_BODY(),
        ARGS,
        BLOCK;

//        private IFunctor goal;

//        /**
//         * @param goal
//         */
//        Kind ( IFunctor goal ) {
//            this.goal = goal;
//        }

        /**
         *
         */
        Kind () {
        }

//        /**
//         * @return
//         */
//        public IFunctor getGoal () {
//            return goal;
//        }
    }
}
