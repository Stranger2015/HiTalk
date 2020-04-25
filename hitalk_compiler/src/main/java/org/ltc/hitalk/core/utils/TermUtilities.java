package org.ltc.hitalk.core.utils;

import com.thesett.aima.logic.fol.FreeNonAnonymousVariablePredicate;
import com.thesett.aima.logic.fol.FreeVariablePredicate;
import com.thesett.aima.search.QueueBasedSearchMethod;
import com.thesett.aima.search.util.Searches;
import com.thesett.aima.search.util.uninformed.DepthFirstSearch;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.core.PrologBuiltIns;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.thesett.aima.search.util.Searches.setOf;
import static java.util.Arrays.asList;
import static org.ltc.hitalk.core.PrologBuiltIns.CALL;
import static org.ltc.hitalk.parser.PrologAtoms.*;

/**
 * TermUtils provides some convenient static utility methods for working with terms in first order logic.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Find all free variables in a term.
 *     <td> {@link DepthFirstSearch}, {@link FreeVariablePredicate}, {@link Searches}
 * <tr><td> Find all free and non-anonymous variables in a term.
 *     <td> {@link DepthFirstSearch}, {@link FreeNonAnonymousVariablePredicate}, {@link Searches}.
 * <tr><td> Flatten comma seperated lists of term.
 * <tr><td> Convert a term into a clause.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class TermUtilities {
    static protected final Logger logger = LoggerFactory.getLogger(TermUtilities.class);

    /**
     * @param original
     * @return
     */
    public static ITerm[] copyOf(ITerm[] original, int ofs) {
        int newLength = original.length + 1;
        final ITerm[] copy = new ITerm[newLength];
        System.arraycopy(original, 0, copy, ofs, original.length);
        return copy;
    }

    /**
     * @param original
     * @param elem
     * @return
     */
    public static ITerm[] prepend(ITerm elem, ITerm[] original) {
        Objects.requireNonNull(original);
        final ITerm[] copy = copyOf(original, 1);
        copy[0] = elem;

        return copy;
    }

    /**
     * @param original
     * @param elem
     * @return
     */
    public static ITerm[] append(ITerm[] original, ITerm elem) {
        Objects.requireNonNull(original);
        final ITerm[] copy = copyOf(original, 0);
        copy[copy.length - 1] = elem;

        return copy;
    }

    /**
     * Calculates the set of free variables in a term.
     *
     * @param query The term to calculate the free non-anonymous variable set from.
     * @return A set of variables that are free and non-anonymous in the term.
     */
    public static Set<HtVariable> findFreeVariables(ITerm query) {
        QueueBasedSearchMethod<ITerm, ITerm> freeVarSearch = new DepthFirstSearch<>();
        freeVarSearch.reset();
        freeVarSearch.addStartState(query);
        freeVarSearch.setGoalPredicate(new FreeVarPredicate());

        return (Set<HtVariable>) (Set) setOf(freeVarSearch);
    }

    /**
     * Calculates the set of free and non-anonymous variables in a term. This is the set of variables that a user query
     * usually wants to be made aware of.
     *
     * @param query The term to calculate the free non-anonymous variable set from.
     * @return A set of variables that are free and non-anonymous in the term.
     */
    public static Set<HtVariable> findFreeNonAnonVariables(ITerm query) {
        QueueBasedSearchMethod<ITerm, ITerm> freeVarSearch = new DepthFirstSearch<>();
        freeVarSearch.reset();
        freeVarSearch.addStartState(query);
        freeVarSearch.setGoalPredicate(new FreeNonAnonVarPredicate());

        return (Set<HtVariable>) (Set) setOf(freeVarSearch);
    }

    /**
     * Flattens a sequence of terms as a symbol separated argument list. Terms that have been parsed as a bracketed
     * expressions will not be flattened. All of the terms in the list must sub sub-classes of a specified super class.
     * This is useful, for example, when parsing a sequence of functors in a clause body, in order to check that all of
     * the body members really are functors and not just terms.
     * <p>
     * <p/>For example, 'a, b, c' is broken into the list { a, b, c } on the symbol ','. The example, 'a, (b, c), d' is
     * broken into the list { a, (b, c), d} on the symbol ',' and so on.
     *
     * @param <T>               The type of the class that all flattened terms must extend.
     * @param term              The term to flatten.
     * @param superClass        The root class that all flattened terms must extend.
     * @param symbolToFlattenOn A symbol of fixity 2 to flatten on.
     * @param interner          The functor and variable interner for the namespace the term to flatten is in.
     * @return A sequence of terms parsed as a term, then flattened back into a list seperated on commas.
     * @throws HtSourceCodeException If any of the extracted terms encountered do not extend the superclass.
     */
    public static <T extends IFunctor> T[] flattenTerm(T term, Class<T> superClass, String symbolToFlattenOn,
                                                       IVafInterner interner) throws Exception {
        List<T> terms = new ArrayList<>();

        // Used to hold the next term to examine as operators are flattened.
        ITerm nextTerm = term;

        // Used to indicate when there are no more operators to flatten.
        boolean mayBeMoreCommas = true;

        // Get the functor name of the symbol to flatten on.
        int symbolName = interner.internFunctorName(symbolToFlattenOn, 2);

        // Walk down the terms matching symbols and flattening them into a list of terms.
        while (mayBeMoreCommas) {
            if (!nextTerm.isBracketed() && (nextTerm instanceof IFunctor) &&
                    symbolName == (((T) nextTerm).getName())) {
                T op = (T) nextTerm;
                ITerm termToExtract = op.getArgument(0);

                if (superClass.isInstance(termToExtract)) {
                    terms.add(superClass.cast(termToExtract));
                    nextTerm = op.getArgument(1);
                } else {
                    throw new HtSourceCodeException("The term " + termToExtract + " is expected to extend " + superClass +
                            " but does not.", null, null, null, termToExtract.getSourceCodePosition());
                }
            } else {
                if (superClass.isInstance(nextTerm)) {
                    terms.add(superClass.cast(nextTerm));
                    mayBeMoreCommas = false;
                } else {
                    throw new HtSourceCodeException("The term " + nextTerm + " is expected to extend " + superClass +
                            " but does not.", null, null, null, nextTerm.getSourceCodePosition());
                }
            }
        }


        return (T[]) terms.toArray(new Object[terms.size()]);
    }

    /**
     * Flattens a sequence of terms as a symbol separated argument list. Terms that have been parsed as a bracketed
     * expressions will not be flattened. All of the terms in the list must sub sub-classes of a specified super class.
     * This is useful, for example, when parsing a sequence of functors in a clause body, in order to check that all of
     * the body members really are functors and not just terms.
     * <p>
     * <p/>For example, 'a, b, c' is broken into the list { a, b, c } on the symbol ','. The example, 'a, (b, c), d' is
     * broken into the list { a, (b, c), d} on the symbol ',' and so on.
     *
     * @param <T>          The type of the class that all flattened terms must extend.
     * @param term         The term to flatten.
     * @param superClass   The root class that all flattened terms must extend.
     * @param internedName The interned name of the symbol to flatten on.
     * @return A sequence of terms parsed as a term, then flattened back into a list seperated on commas.
     */
    public static <T extends IFunctor> List<T> flattenTerm(T term, Class<T> superClass, int internedName) throws Exception {
        List<T> terms = new LinkedList<>();

        // Used to hold the next term to examine as operators are flattened.
        T nextTerm = term;

        // Used to indicate when there are no more operators to flatten.
        boolean mayBeMore = true;

        // Walk down the terms matching symbols and flattening them into a list of terms.
        while (mayBeMore) {
            if (!nextTerm.isBracketed() && internedName == nextTerm.getName()) {
                T op = nextTerm;
                T termToExtract = (T) op.getArgument(0);

                if (superClass.isInstance(termToExtract)) {
                    terms.add(superClass.cast(termToExtract));
                    nextTerm = (T) op.getArgument(1);
                } else {
                    throw new IllegalStateException("The term " + termToExtract + " is expected to extend " + superClass +
                            " but does not.");
                }
            } else {
                if (superClass.isInstance(nextTerm)) {
                    terms.add(superClass.cast(nextTerm));
                    mayBeMore = false;
                } else {
                    throw new IllegalStateException("The term " + nextTerm + " is expected to extend " + superClass +
                            " but does not.");
                }
            }
        }

        return terms;
    }

    /**
     * Converts a term into a clause. The term must be a functor. If it is a functor corresponding to the ':-' symbol it
     * is a clause with a head and a body. If it is a functor corresponding to the '?-' symbol it is a query clause with
     * no head but must have a body. If it is neither but is a functor it is interpreted as a program clause ':-' with
     * no body, that is, a fact.
     *
     * @param term     The term to convert to a top-level clause.
     * @param interner The functor and variable name interner for the namespace the term to convert is in.
     * @return A clause for the term, or <tt>null</tt> if it cannot be converted.
     * @throws HtSourceCodeException If the term to convert to a clause does not form a valid clause.
     */
    public static HtClause convertToClause(ITerm term, IVafInterner interner) throws Exception {
        // Check if the top level term is a query, an implication or neither and reduce the term into a clause
        // accordingly.
        if (term instanceof OpSymbolFunctor) {
            OpSymbolFunctor symbol = (OpSymbolFunctor) term;

            if (IMPLIES.equals(symbol.getTextName())) {
                IFunctor[] flattenedArgs = flattenTerm((IFunctor) symbol.getArgument(1),
                        IFunctor.class, COMMA, interner);

                return new HtClause((IFunctor) symbol.getArgument(0), new ListTerm(asList(flattenedArgs)));
            } else if (QUERY.equals(symbol.getTextName())) {
                IFunctor[] flattenedArgs = flattenTerm((IFunctor) symbol.getArgument(0),
                        IFunctor.class,
                        COMMA,
                        interner);

                return new HtClause(null, new ListTerm(asList(flattenedArgs)));
            }
        }
        if (term != null) {
            logger.info("end_of_file");
            return new HtClause((IFunctor) term, new ListTerm(Collections.emptyList()));
        } else {
            throw new HtSourceCodeException("Only functor can be as a clause head", null, null, null, null
                    /*  requireNonNull(term).getSourceCodePosition()*/);
        }
    }

    public static boolean unifyB(ITerm term1, ITerm term2) {
        final List<ListTerm> r = unify(term1, term2);
        return r != null && !r.isEmpty();
    }

    /**
     * @param term1
     * @param term2
     * @return
     */
    public static List<ListTerm> unify(ITerm term1, ITerm term2) {
        final ListTerm lt = new ListTerm(Arrays.asList(term1, term2));
        lt.setHead(0, term1);
        lt.setHead(1, term2);

        return PrologBuiltIns.UNIFIES.getBuiltInDef().apply(lt);
    }

    /**
     * @param term1
     * @param term2
     * @return
     */
    public static List<ListTerm> termExpansion(ITerm term1, ITerm term2) {
        final ListTerm lt = new ListTerm(asList(term1, term2));
        return PrologBuiltIns.TERM_EXPANSION.getBuiltInDef().apply(lt);
    }

    public static boolean call(IFunctor functor) {
        ListTerm lt = new ListTerm(Collections.singletonList(functor));
        final List<ListTerm> l = CALL.getBuiltInDef().apply(lt);

        return l != null && !l.isEmpty();
    }

    /**
     * @param a
     * @return
     */
    public static ITerm getLast(ITerm[] a) {
        return a[a.length - 1];
    }

    /**
     * @param a
     * @return
     */
    public static ITerm getLast(List<ITerm> a) {
        return a.get(a.size() - 1);
    }


    public static HtClause convert(ITerm term) throws Exception {
        final FlattenTermVisitor ftv = new FlattenTermVisitor();
        HtClause clause = null;
        if (term instanceof IFunctor) {
            final int name = ((IFunctor) term).getName();
            final IVafInterner interner = BaseApp.appContext.getInterner();
            final String fn = interner.getFunctorName(name);
            if (IMPLIES.equals(fn)) {
                if (((IFunctor) term).getArity() == 2) {
                    final ITerm h = ((IFunctor) term).getArgument(0);
                    final ITerm b = ((IFunctor) term).getArgument(1);
                    final List<ITerm> hl = h.accept(ftv);
                    final List<ITerm> bl = b.accept(ftv);
                    bl.addAll(1, hl);
                    final IFunctor newHead = (IFunctor) hl.get(0);
                    clause = new HtClause(newHead, new ListTerm(bl));
                }
            } else {
                final List<ITerm> hl = term.accept(ftv);
                clause = new HtClause((IFunctor) hl.get(0), new ListTerm(hl.subList(1, hl.size())));
            }
        }

        return clause;
    }

    /**
     *
     */
    public static class FlattenTermVisitor implements ITermsVisitor {
        private Map<HtVariable, ITerm> map = new HashMap<>();

        public List<ITerm> visit(ITerm term) throws Exception {
            if (term instanceof IFunctor) {
                return visit((IFunctor) term);
            }
            if (term instanceof ListTerm) {
                return visit((ListTerm) term);
            }
            if (term instanceof HtVariable) {
                return visit((HtVariable) term);
            }

            return Collections.singletonList(term);
        }

        public List<ITerm> visit(IFunctor functor) throws Exception {
            final List<ITerm> l = new ArrayList<>();

            final ListTerm args = functor.getArgs();
            visit(functor.getArgs());

            return l;
        }

//        public HtClause visit(HtClause clause) throws Exception {
//            final List<ITerm> hl = visit(clause.getHead());
//            final List<ITerm> bl = visit(clause.getBody());
//            final IFunctor newHead = (IFunctor) hl.get(0);
//            hl.addAll(1, bl);
//            final ListTerm newBody = new ListTerm(hl);
//
//            return new HtClause(newHead, newBody);
//        }

        public List<ITerm> visit(ListTerm list) throws Exception {
            final List<ITerm> l = list.getHeads();
            final List<ITerm> newLits = new ArrayList<>();
            for (ITerm term : l) {
                newLits.addAll(visit(term));
            }
            final ITerm tail = list.getTail();
            final List<ITerm> tl = visit(tail);

            return l;
        }

        public List<ITerm> visit(HtVariable var) {
            if (!map.containsKey(var)) {
                map.put(var, new HtVariable());
            }
            return Collections.singletonList(var);
        }

        public List<ITerm> visit(IntTerm integer) {
            return Collections.singletonList(integer);
        }

        public List<ITerm> visit(FloatTerm real) {
            return Collections.singletonList(real);
        }
    }
}
