package org.ltc.hitalk.term;

import com.thesett.aima.search.Operator;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.stream.IntStream;

import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;
import static org.ltc.hitalk.term.ListTerm.Kind.values;

/**
 *
 */
public class ListTerm extends HtBaseTerm implements ITerm, IFunctor {

    //    public static final ITerm TRUE = new ListTerm(Kind.TRUE);
    public static final ListTerm NIL = new ListTerm();

    protected int name;
    protected Kind kind;
    protected ITerm[] arguments;
    protected Operator <ITerm> op;
    protected ListTerm args;

    /**
     * @param name
     * @param heads
     */
    public ListTerm ( int name, ITerm... heads ) {
        arguments = heads;
//        this(TermUtilities.append(heads, new IntTerm(name)));
        this.name = name;
    }

    /**
     * @param length
     */
    public ListTerm ( int length ) {
        this(length == 0 ? Kind.NIL : Kind.LIST);
        //            HtVariable htVariable = new HtVariable();
        ITerm[] heads = IntStream.range(0, length).mapToObj(i -> new HtVariable()).toArray(ITerm[]::new);
        setArguments(heads);//fixme name tail
    }

    /**
     * @param arguments
     */
    public ListTerm ( ITerm... arguments ) {
        this((arguments == null) || (arguments.length == 0) ? 0 : arguments.length);
        int bound = this.getHeads().length;
        for (int i = 0; i < bound; i++) {
            this.arguments[i] = new HtVariable();
        }
    }

    /**
     *
     */
    public ListTerm () {//fixme redundant
        this(0);
    }

    public ListTerm ( Kind kind ) {
        this.kind = kind;
    }

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

    /**
     * args = name + heads + tail
     *
     * @return
     */
    public ITerm[] getHeads () {
        ITerm[] args = getArguments();
        if (args.length <= 1) {
            return EMPTY_TERM_ARRAY;
        } else {
            int headsLen = args.length - 2;
            ITerm[] heads = new ITerm[headsLen];
            System.arraycopy(args, 1, heads, 0, headsLen);

            return heads;
        }
    }

    /**
     * @return
     */
    public boolean isNil () {
        return getName() < 0;
    }

    /**
     * @return
     */
    public int size () {
        return getHeads().length;
    }

//    public ITerm[] getHeads () {
//        return arguments;
//    }//fixme

    public ITerm getTail () {
        return arguments[arguments.length - 2];
    }

    /**
     * @param i
     * @return
     */
    public ITerm get ( int i ) {
//        if (size() == i) {
//            return TRUE;
//        }
        return getHeads()[i];
    }

    /**
     * @return
     */
    @Override
    public int getName () {
        return ((IntTerm) arguments[arguments.length - 1]).intValue();
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
        return args;
    }

    /**
     * @param i
     * @return
     */
    public ITerm getArgument ( int i ) {
        return arguments[i];
    }

    /**
     * @return
     */
    @Override
    public int getArity () {
        return 0;//fixme
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


//    public String toString () {
//        final StringBuilder sb = new StringBuilder("ListTerm{");
//        sb.append("kind=").append(kind);
//        sb.append(", arguments=").append(Arrays.toString(arguments));
//        sb.append(", bracketed=").append(bracketed);
//        sb.append('}');
//        return sb.toString();
//    }

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
