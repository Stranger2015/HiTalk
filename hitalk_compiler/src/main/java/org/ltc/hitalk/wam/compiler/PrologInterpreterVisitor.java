package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.term.io.Environment;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;

import java.util.Set;

/**
 * solve(Ctx,(G1,G2)):-
 * !, solve(Ctx,G1),
 * solve(Ctx,G2). %C2
 * <p>
 * solve(Ctx,U>>G):- !,
 * solve([U|Ctx],G). %C3
 * <p>
 * solve(Ctx,G):-
 * member(U,Ctx), %C4
 * method(U,G,Body),
 * solve(Ctx,Body).
 * <p>
 * solve(_, G):-
 * predicate_property(G, built_in),
 * call(G).
 * <p>
 * method(U, Head, Body) :-
 * %    current_entity(U),
 * clause( U::Head, Body ).
 *
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public class PrologInterpreterVisitor<T extends HtClause, P, Q> extends MetaInterpreterVisitor <T, P, Q> {

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     * @param resolver
     * @param traverser
     */
    protected PrologInterpreterVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                         IVafInterner interner,
                                         IResolver <P, Q> resolver,
                                         IPositionalTermTraverser traverser ) {

        super(symbolTable, interner, resolver, traverser);
    }

    /**
     * @param predicate The predicate being entered.
     */
    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
        PredicateTable <?> predicateTable = Environment.instance().getPredicateTable();

    }

    /**
     * @param predicate
     */
    @Override
    protected void leavePredicate ( HtPredicate predicate ) {

    }

    /**
     * @param clause The clause being entered.
     */
    @Override
    protected void enterClause ( HtClause clause ) throws LinkageException {

    }

    /**
     * @param clause The clause being left.
     */
    @Override
    protected void leaveClause ( HtClause clause ) {

    }

    /**
     * @param listTerm
     * @throws LinkageException
     */
    @Override
    protected void enterListTerm ( ListTerm listTerm ) throws LinkageException {

    }

    @Override
    public void lookupGoal ( PiCalls <?> goal, Set <PiCalls <?>> hbSet ) throws LinkageException {

    }

    @Override
    protected void leaveListTerm ( ListTerm listTerm ) {

    }

    @Override
    protected void enterFunctor ( IFunctor functor ) throws LinkageException {

    }

    /**
     * @param functor The functor being left.
     */
    @Override
    protected void leaveFunctor ( IFunctor functor ) {

    }

    /**
     * @param functor
     */
    @Override
    protected void enterGoal ( IFunctor functor ) {

    }

    /**
     * @param functor
     */
    @Override
    protected void leaveGoal ( IFunctor functor ) {

    }
}
