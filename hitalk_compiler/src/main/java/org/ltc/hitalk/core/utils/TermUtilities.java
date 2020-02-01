package org.ltc.hitalk.core.utils;

import com.thesett.aima.logic.fol.FreeNonAnonymousVariablePredicate;
import com.thesett.aima.logic.fol.FreeVariablePredicate;
import com.thesett.aima.search.QueueBasedSearchMethod;
import com.thesett.aima.search.util.Searches;
import com.thesett.aima.search.util.uninformed.DepthFirstSearch;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.PrologBuiltIns;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IdentifiedTerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.*;

import static java.util.Arrays.asList;
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
    /**
     * @param original
     * @return
     */
    public static ITerm[] copyOf ( ITerm[] original, int ofs ) {
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
    public static ITerm[] prepend ( ITerm elem, ITerm[] original ) {
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
    public static ITerm[] append ( ITerm[] original, ITerm elem ) {
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
    public static Set <HtVariable> findFreeVariables ( ITerm query ) {
        QueueBasedSearchMethod <ITerm, ITerm> freeVarSearch = new DepthFirstSearch <>();
        freeVarSearch.reset();
        freeVarSearch.addStartState(query);
        freeVarSearch.setGoalPredicate(new FreeVarPredicate());

        return (Set <HtVariable>) (Set) Searches.setOf(freeVarSearch);
    }

    /**
     * Calculates the set of free and non-anonymous variables in a term. This is the set of variables that a user query
     * usually wants to be made aware of.
     *
     * @param query The term to calculate the free non-anonymous variable set from.
     * @return A set of variables that are free and non-anonymous in the term.
     */
    public static Set <HtVariable> findFreeNonAnonVariables ( ITerm query ) {
        QueueBasedSearchMethod <ITerm, ITerm> freeVarSearch = new DepthFirstSearch <>();
        freeVarSearch.reset();
        freeVarSearch.addStartState(query);
        freeVarSearch.setGoalPredicate(new FreeNonAnonVarPredicate());

        return (Set <HtVariable>) (Set) Searches.setOf(freeVarSearch);
    }

    /**
     * Flattens a sequence of terms as a symbol separated argument list. Terms that have been parsed as a bracketed
     * expressions will not be flattened. All of the terms in the list must sub sub-classes of a specified super class.
     * This is usefull, for example, when parsing a sequence of functors in a clause body, in order to check that all of
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
    public static <T extends IFunctor> IFunctor[] flattenTerm(IFunctor term, Class<T> superClass, String symbolToFlattenOn,
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
                    symbolName == (((IFunctor) nextTerm).getName())) {
                IFunctor op = (IFunctor) nextTerm;
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

        return terms.toArray(new IFunctor[terms.size()]);
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
    public static <T extends IFunctor> List<T> flattenTerm(IFunctor term, Class<T> superClass, int internedName) throws Exception {
        List<T> terms = new LinkedList<>();

        // Used to hold the next term to examine as operators are flattened.
        IFunctor nextTerm = term;

        // Used to indicate when there are no more operators to flatten.
        boolean mayBeMore = true;

        // Walk down the terms matching symbols and flattening them into a list of terms.
        while (mayBeMore) {
            if (!nextTerm.isBracketed() && nextTerm instanceof IFunctor &&
                    internedName == nextTerm.getName()) {
                IFunctor op = nextTerm;
                IFunctor termToExtract = (IFunctor) op.getArgument(0);

                if (superClass.isInstance(termToExtract)) {
                    terms.add(superClass.cast(termToExtract));
                    nextTerm = (IFunctor) op.getArgument(1);
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
        if (term instanceof IdentifiedTerm) {
            IdentifiedTerm symbol = (IdentifiedTerm) term;

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
            return new HtClause((IFunctor) term, new ListTerm());
        } else {
            throw new HtSourceCodeException("Only functor can be as a clause head", null, null, null, null
                    /*  requireNonNull(term).getSourceCodePosition()*/);
        }
    }

    /**
     * @param term1
     * @param term2
     * @return
     */
    public static boolean unify ( ITerm term1, ITerm term2 ) {
        final ListTerm lt = new ListTerm(2);
        lt.setHead(0, term1);
        lt.setHead(1, term2);

        PrologBuiltIns.UNIFIES.getBuiltInDef().accept(lt);
        return PrologBuiltIns.getBooleanResult();
    }

    /**
     * @param term1
     * @param term2
     * @return
     */
    public static boolean term_expansion ( ITerm term1, ITerm term2 ) {
        final ListTerm lt = new ListTerm(2);
        lt.setHead(0, term1);
        lt.setHead(1, term2);

        PrologBuiltIns.TERM_EXPANSION.getBuiltInDef().accept(lt);

        return PrologBuiltIns.getBooleanResult();
    }

    /**
     * @param a
     * @return
     */
    public static ITerm getLast(ITerm[] a) {
        return a[a.length - 1];
    }
}


