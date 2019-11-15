package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermVisitor;
import com.thesett.aima.logic.fol.compiler.DefaultTraverser;
import com.thesett.aima.logic.fol.compiler.TermWalker;
import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import com.thesett.aima.search.util.uninformed.PostFixSearch;
import com.thesett.common.util.logic.UnaryPredicate;
import org.ltc.hitalk.term.HlOpSymbol;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ITermVisitor;
import org.ltc.hitalk.wam.compiler.hitalk.HtTermWalker;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.IPositionalTermVisitor;

public
class HtTermWalkers {
    /**
     * Predicate matching conjunction and disjunction operators.
     */
    public static final UnaryPredicate <ITerm> CONJ_DISJ_OP_SYMBOL_PREDICATE =
            term -> (term instanceof HlOpSymbol) &&
                    (((HlOpSymbol) term).getTextName().equals(";") ||
                            ((HlOpSymbol) term).getTextName().equals(","));

    /**
     * Provides a simple depth first walk over a term.
     *
     * @param visitor The visitor to apply to each term.
     * @return A simple depth first walk over a term.
     */
    public static HtTermWalker simpleWalker ( ITermVisitor visitor ) {
        DepthFirstBacktrackingSearch <ITerm, ITerm> search = new DepthFirstBacktrackingSearch <Term, Term>();

        return new HtTermWalker(search, new DefaultTraverser(), visitor);
    }

    /**
     * Provides a depth first walk over a term, visiting only when a goal predicate matches.
     *
     * @param unaryPredicate The goal predicate.
     * @param visitor        The visitor to apply to each term.
     * @return A depth first walk over a term, visiting only when a goal predicate matches.
     */
    public static HtTermWalker goalWalker ( UnaryPredicate <ITerm> unaryPredicate, ITermVisitor visitor ) {
        HtTermWalker walker = simpleWalker(visitor);
        walker.setGoalPredicate(unaryPredicate);

        return walker;
    }

    /**
     * Provides a positional depth first walk over a term.
     *
     * @param visitor The visitor to apply to each term, and to notify of positional context changes.
     * @return A positional depth first walk over a term.
     */
    public static HtTermWalker positionalWalker ( IPositionalTermVisitor visitor ) {
        IPositionalTermTraverser positionalTraverser = new HtPositionalTermTraverser();
        positionalTraverser.setContextChangeVisitor(visitor);
        visitor.setPositionalTraverser(positionalTraverser);

        return new HtTermWalker(new DepthFirstBacktrackingSearch <>(), positionalTraverser, visitor);
    }

    /**
     * Provides a positional depth first walk over a term, visiting only when a goal predicate matches.
     *
     * @param unaryPredicate The goal predicate.
     * @param visitor        The visitor to apply to each term.
     * @return A positional depth first walk over a term, visiting only when a goal predicate matches.
     */
    public static HtTermWalker positionalGoalWalker ( UnaryPredicate <ITerm> unaryPredicate,
                                                      IPositionalTermVisitor visitor ) {
        HtTermWalker walker = positionalWalker(visitor);
        walker.setGoalPredicate(unaryPredicate);

        return walker;
    }

    /**
     * Provides a positional postfix walk over a term.
     *
     * @param visitor The visitor to apply to each term, and to notify of positional context changes.
     * @return A positional postfix first walk over a term.
     */
    public static TermWalker positionalPostfixWalker ( IPositionalTermVisitor visitor ) {
        HtPositionalTermTraverser positionalTraverser = new IPositionalTermTraverserImpl();
        positionalTraverser.setContextChangeVisitor(visitor);
        visitor.setPositionalTraverser(positionalTraverser);

        return new TermWalker(new PostFixSearch <>(), positionalTraverser, visitor);
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
