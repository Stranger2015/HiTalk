package org.ltc.hitalk.parser;


import org.ltc.hitalk.gnu.prolog.term.Term;
import org.ltc.hitalk.gnu.prolog.term.TermCloneContext;

/**
 *
 */
public class HtClause extends Term {

    boolean isDirective() {
        return this instanceof Directive;

    }

//    /**
//     * Creates a program sentence in L2.
//     *
//     * @param head The head of the program.
//     * @param body The functors that make up the query body of the program, if any. May be <tt>null</tt>
//     */
//    public HtClause (IFunctor head, ListTerm body ) {
//        this.head = head;
//        this.body = body;
//    }
//
//    /**
//     * @param head
//     * @param body
//     */
//    public HtClause ( IFunctor<T> head, IFunctor<T> body ) {
//        this.head = head;
//        this.body = new ListTerm<>(1);
//        this.body.setHead(0, (T) body);
//    }

//    /**
//     * @param head
//     */
//    public HtClause ( IFunctor head ) {
//        this(head, (ListTerm) null);
//    }

    /**
     * Gets the wrapped sentence in the logical language over T.
     *
     * @return The wrapped sentence in the logical language.
     */
//    @Override
    public HtClause getT () {
        return this;
    }

    /**
     * @return
     */
    public boolean isDcgRule () {
        return false;
    }

    /**
     * clone the object using clone context
     *
     * @param context clone context
     * @return cloned term
     */
    public Term clone(TermCloneContext context) {
        return null;
    }

//    @Override
//    public IFunctor getHead () {
//        return head;
//    }
//
//    /**
//     * @return
//     */
//    @Override
//    public ListTerm getBody () {
//        return body;
//    }

//    /**
//     * @return
//     */
//    public int bodyLength () {
//        return getBody().size();
//    }
//
//    @Override
//    public boolean isHiLog () {
//        return false;
//    }
//
//    /**
//     * @return
//     */
//    public boolean isJavaObject () {
//        return false;
//    }
//
//    public void free () {
//
//    }
//
//    public String toString (IVafInterner interner, boolean printVarName, boolean printBindings ) {
//        StringBuilder result = new StringBuilder();
//        if (head != null) {
//            result.append(head.toString(interner, printVarName, printBindings));
//        }
//
//        if (body != null) {
//            result.append(isQuery() ? "?- " : " :- ");
//
//            IntStream.range(0, body.size()).forEachOrdered(i -> result.append(body.getHead(i).
//                    toString(interner, printVarName, printBindings))
//                    .append((i < (body.size() - 1)) ? ", " : ""));
//        }
//
//        return result.toString();
//    }
//
//    public ListTerm getBodyAsListTerm () {
//        return body;
//    }
//
//    public void toString0 ( StringBuilder sb ) {
//    }
//
//    /**
//     * @param i
//     * @return
//     */
//    @Override
//    public IFunctor getGoal ( int i ) {
//        return (IFunctor) getBody().getHead(i);
//    }
}