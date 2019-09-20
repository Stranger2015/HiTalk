package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.OpSymbol;
import com.thesett.aima.logic.fol.PositionalTermVisitor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermVisitor;
import com.thesett.aima.logic.fol.compiler.DefaultTraverser;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverserImpl;
import com.thesett.aima.logic.fol.compiler.TermWalker;
import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import com.thesett.aima.search.util.uninformed.PostFixSearch;
import com.thesett.common.util.logic.UnaryPredicate;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.HtPositionalTermVisitor;

public
class HtTermWalkers {
    /**
     * Predicate matching conjunction and disjunction operators.
     */
    public static final UnaryPredicate <Term> CONJ_DISJ_OP_SYMBOL_PREDICATE =
            term -> (term instanceof OpSymbol) &&
                    (((OpSymbol) term).getTextName().equals(";") ||
                            ((OpSymbol) term).getTextName().equals(","));

    /**
     * Provides a simple depth first walk over a term.
     *
     * @param visitor The visitor to apply to each term.
     * @return A simple depth first walk over a term.
     */
    public static
    TermWalker simpleWalker ( TermVisitor visitor ) {
        DepthFirstBacktrackingSearch <Term, Term> search = new DepthFirstBacktrackingSearch <Term, Term>();

        return new TermWalker(search, new DefaultTraverser(), visitor);
    }

    /**
     * Provides a depth first walk over a term, visiting only when a goal predicate matches.
     *
     * @param unaryPredicate The goal predicate.
     * @param visitor        The visitor to apply to each term.
     * @return A depth first walk over a term, visiting only when a goal predicate matches.
     */
    public static
    TermWalker goalWalker ( UnaryPredicate <Term> unaryPredicate, TermVisitor visitor ) {
        TermWalker walker = simpleWalker(visitor);
        walker.setGoalPredicate(unaryPredicate);

        return walker;
    }

    /**
     * Provides a positional depth first walk over a term.
     *
     * @param visitor The visitor to apply to each term, and to notify of positional context changes.
     * @return A positional depth first walk over a term.
     */
    public static
    TermWalker positionalWalker ( HtPositionalTermVisitor visitor ) {
        HtPositionalTermTraverser positionalTraverser = new HtPositionalTermTraverserImpl();
        positionalTraverser.setContextChangeVisitor(visitor);
        visitor.setPositionalTraverser(positionalTraverser);

        return new TermWalker(new DepthFirstBacktrackingSearch <>(), positionalTraverser, visitor);
    }

    /**
     * Provides a positional depth first walk over a term, visiting only when a goal predicate matches.
     *
     * @param unaryPredicate The goal predicate.
     * @param visitor        The visitor to apply to each term.
     * @return A positional depth first walk over a term, visiting only when a goal predicate matches.
     */
    public static
    TermWalker positionalGoalWalker ( UnaryPredicate <Term> unaryPredicate, HtPositionalTermVisitor visitor ) {
        TermWalker walker = positionalWalker(visitor);
        walker.setGoalPredicate(unaryPredicate);

        return walker;
    }

    /**
     * Provides a positional postfix walk over a term.
     *
     * @param visitor The visitor to apply to each term, and to notify of positional context changes.
     * @return A positional postfix first walk over a term.
     */
    public static
    TermWalker positionalPostfixWalker ( PositionalTermVisitor visitor ) {
        PositionalTermTraverser positionalTraverser = new PositionalTermTraverserImpl();
        positionalTraverser.setContextChangeVisitor(visitor);
        visitor.setPositionalTraverser(positionalTraverser);

        return new TermWalker(new PostFixSearch <Term, Term>(), positionalTraverser, visitor);
    }

    /**
     * Provides a walk over a term, that finds all conjunction and disjunction operators.
     *
     * @param visitor The visitor to apply to each term.
     * @return A walk over a term, that finds all conjunction and disjunction operators.
     */
    public static
    TermWalker conjunctionAndDisjunctionOpSymbolWalker ( TermVisitor visitor ) {
        //return positionalGoalWalker(CONJ_DISJ_OP_SYMBOL_PREDICATE, visitor);
        return goalWalker(CONJ_DISJ_OP_SYMBOL_PREDICATE, visitor);
    }
}
