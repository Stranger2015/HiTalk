package org.ltc.hitalk.core.utils;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.search.QueueBasedSearchMethod;
import com.thesett.aima.search.util.Searches;
import com.thesett.aima.search.util.uninformed.DepthFirstSearch;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
     * Calculates the set of free variables in a term.
     *
     * @param query The term to calculate the free non-anonymous variable set from.
     * @return A set of variables that are free and non-anonymous in the term.
     */
    public static Set <Variable> findFreeVariables ( Term query ) {
        QueueBasedSearchMethod <Term, Term> freeVarSearch = new DepthFirstSearch <>();
        freeVarSearch.reset();
        freeVarSearch.addStartState(query);
        freeVarSearch.setGoalPredicate(new FreeVariablePredicate());

        return (Set <Variable>) (Set) Searches.setOf(freeVarSearch);
    }

    /**
     * Calculates the set of free and non-anonymous variables in a term. This is the set of variables that a user query
     * usually wants to be made aware of.
     *
     * @param query The term to calculate the free non-anonymous variable set from.
     * @return A set of variables that are free and non-anonymous in the term.
     */
    public static Set <Variable> findFreeNonAnonymousVariables ( Term query ) {
        QueueBasedSearchMethod <Term, Term> freeVarSearch = new DepthFirstSearch <Term, Term>();
        freeVarSearch.reset();
        freeVarSearch.addStartState(query);
        freeVarSearch.setGoalPredicate(new FreeNonAnonymousVariablePredicate());

        return (Set <Variable>) (Set) Searches.setOf(freeVarSearch);
    }

    /**
     * Flattens a sequence of terms as a symbol seperated argument list. Terms that have been parsed as a bracketed
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
     * @throws SourceCodeException If any of the extracted terms encountered do not extend the superclass.
     */
    public static <T extends Term> List <T> flattenTerm ( Term term, Class <T> superClass, String symbolToFlattenOn,
                                                          IVafInterner interner ) throws SourceCodeException {
        List <T> terms = new LinkedList <T>();

        // Used to hold the next term to examine as operators are flattened.
        Term nextTerm = term;

        // Used to indicate when there are no more operators to flatten.
        boolean mayBeMoreCommas = true;

        // Get the functor name of the symbol to flatten on.
        int symbolName = interner.internFunctorName(symbolToFlattenOn, 2);

        // Walk down the terms matching symbols and flattening them into a list of terms.
        while (mayBeMoreCommas) {
            if (!nextTerm.isBracketed() && (nextTerm instanceof IFunctor) &&
                    (symbolName == (((IFunctor) nextTerm).getName()))) {
                IFunctor op = (IFunctor) nextTerm;
                Term termToExtract = op.getArgument(0);

                if (superClass.isInstance(termToExtract)) {
                    terms.add(superClass.cast(termToExtract));
                    nextTerm = op.getArgument(1);
                } else {
                    throw new SourceCodeException("The term " + termToExtract + " is expected to extend " + superClass +
                            " but does not.", null, null, null, termToExtract.getSourceCodePosition());
                }
            } else {
                if (superClass.isInstance(nextTerm)) {
                    terms.add(superClass.cast(nextTerm));
                    mayBeMoreCommas = false;
                } else {
                    throw new SourceCodeException("The term " + nextTerm + " is expected to extend " + superClass +
                            " but does not.", null, null, null, nextTerm.getSourceCodePosition());
                }
            }
        }

        return terms;
    }

    /**
     * Flattens a sequence of terms as a symbol seperated argument list. Terms that have been parsed as a bracketed
     * expressions will not be flattened. All of the terms in the list must sub sub-classes of a specified super class.
     * This is usefull, for example, when parsing a sequence of functors in a clause body, in order to check that all of
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
    public static <T extends Term> List <T> flattenTerm ( Term term, Class <T> superClass, int internedName ) {
        List <T> terms = new LinkedList <T>();

        // Used to hold the next term to examine as operators are flattened.
        Term nextTerm = term;

        // Used to indicate when there are no more operators to flatten.
        boolean mayBeMore = true;

        // Walk down the terms matching symbols and flattening them into a list of terms.
        while (mayBeMore) {
            if (!nextTerm.isBracketed() && (nextTerm instanceof IFunctor) &&
                    (internedName == (((IFunctor) nextTerm).getName()))) {
                IFunctor op = (IFunctor) nextTerm;
                Term termToExtract = op.getArgument(0);

                if (superClass.isInstance(termToExtract)) {
                    terms.add(superClass.cast(termToExtract));
                    nextTerm = op.getArgument(1);
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
     * @throws SourceCodeException If the term to convert to a clause does not form a valid clause.
     */
    public static HtClause convertToClause ( Term term, IVafInterner interner ) throws SourceCodeException {
        // Check if the top level term is a query, an implication or neither and reduce the term into a clause
        // accordingly.
        if (term instanceof OpSymbol) {
            OpSymbol symbol = (OpSymbol) term;

            if (":-".equals(symbol.getTextName())) {
                List <IFunctor> flattenedArgs = flattenTerm(symbol.getArgument(1), IFunctor.class, ",", interner);

                return new HtClause((IFunctor) symbol.getArgument(0),
                        flattenedArgs.toArray(new IFunctor[flattenedArgs.size()]));
            } else if ("?-".equals(symbol.getTextName())) {
                List <IFunctor> flattenedArgs = flattenTerm(symbol.getArgument(0), IFunctor.class, ",", interner);

                return new HtClause(null, flattenedArgs.toArray(new IFunctor[flattenedArgs.size()]));
            }
        }

        if (term instanceof IFunctor) {
            return new HtClause(null, (IFunctor) term, null);
        } else {
            throw new SourceCodeException("Only functors can for a clause body, not " + term + ".", null, null, null,
                    term.getSourceCodePosition());
        }
    }
}


