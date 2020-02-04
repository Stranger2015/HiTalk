package org.ltc.hitalk.term;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.TermUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 *
 */
public class ListTerm extends HtBaseTerm {
    public static final ListTerm NIL = new ListTerm(emptyList());

    public Kind getKind() {
        return kind;
    }

    protected Kind kind;//fixme encode in name
    final List<ITerm> heads = new ArrayList<>();

    public ListTerm(int arity) {
        this.heads.addAll(Arrays.asList(new ITerm[arity]));
    }

    public ListTerm(Kind kind, ListTerm terms) {
        this.kind = kind;
        heads.clear();
        heads.addAll(terms.getHeads());
    }

    public ListTerm(Kind kind, List<ITerm> headTail) {
        this.kind = kind;
        heads.clear();
        heads.addAll(headTail);
    }

    /**
     * @param names
     * @return
     */
    public List<ITerm> addHeads(int... names) {
        for (final int name : names) {
            addHead(name);
        }
        return heads;
    }

    /**
     * @param i
     * @return
     */
    public ListTerm addHead(int i) {
        heads.add(new IntTerm(i));
        return this;
    }

    /**
     * @param names
     * @return
     */
    public List<ITerm> addHeads(ITerm... names) {
        for (final ITerm name : names) {
            addHead(name);
        }
        return heads;
    }

    /**
     * @param names
     * @return
     */
    public List<ITerm> addHead(ITerm... names) {
        Collections.addAll(heads, names);
        return heads;
    }

    /**
     * @param heads
     */
    public ListTerm(final List<ITerm> heads) {
//        this(heads.size());
        int bound = this.getHeads().size();
        for (int i = 0; i < bound; i++) {
            this.heads.add(heads.get(i));
        }
        this.heads.add(NIL);
    }

    /**
     * @param kind
     * @param tail
     * @param heads
     */
    public ListTerm(Kind kind, List<ITerm> heads, ITerm tail) {
        this.kind = kind;
        heads.add(tail);
        setHeads(heads);

    }


    /**
     * @param heads
     * @param tail
     */
    public ListTerm(List<ITerm> heads, ITerm tail) {
        this(Kind.LIST, heads, tail);
    }

    /**
     * args = name + heads + tail
     *
     * @return
     */
    public List<ITerm> getHeads() {
        return heads;
    }

    /**
     * @return
     */
    public boolean isNil() {
        return size() == 0;
    }

    /**
     * @return
     */
    public int size() {
        return getHeads().size();
    }

    /**
     * @return
     */
    public ITerm getTail() {
        return TermUtilities.getLast(heads);
    }

    /**
     * @param i
     * @return
     */
    public ITerm getHead(int i) {
        return getHeads().get(i);
    }

    /**
     * @return
     */
    @Override
    public boolean isList() {
        return true;
    }

    /**
     * @param i
     * @param term
     */
    public void setHead(int i, ITerm term) {
        if (isConjunction(term)) {
            getHeads().set(i, term);
        }
    }

    private boolean isConjunction(ITerm term) {
//        return (term.isList() && ((ListTerm) term).getKind() == CLAUSE_BODY);
        return true;//tidi
    }

    /**
     * @return
     */
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
     * Frees all assigned variables in the term, leaving them unassigned.
     */
    @Override
    public void free() {

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
    public String toString(IVafInterner interner, boolean printVarName, boolean printBindings) {
        final StringBuilder sb = new StringBuilder();
        switch (kind) {
            case NIL:
                sb.append("[]");
            case LIST:
                sb.append("[");
                final List<ITerm> heads = getHeads();
                final int iMax = heads.size() - 1;
                for (int i = 0; i < heads.size(); i++) {
                    sb.append(heads.get(i).toString(interner, printVarName, printBindings));

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
    public ITerm newTail(boolean isOpen) {
        return isOpen ? new HtVariable() : NIL;
    }

    public void setHeads(List<ITerm> heads) {
        this.heads.clear();
        this.heads.addAll(heads);
    }

    /**
     * @param name
     * @return
     */
    public ListTerm addHead(ITerm name) {
        heads.add(name);
        return this;
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
        Kind() {
        }

//        /**
//         * @return
//         */
//        public IFunctor getGoal () {
//            return goal;
//        }
    }
}
