package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import jline.ConsoleReader;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.PrologInterpreter;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;

import java.io.IOException;
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
public class PrologInterpreterVisitor<T extends HtMethod, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery> extends MetaInterpreterVisitor<T, P, Q, PC, QC> {

    protected final PrologInterpreter<T, P, Q, PC, QC> engine;
    protected final ConsoleReader consoleReader = new ConsoleReader();

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     * @param resolver
     * @param traverser
     * @param engine
     */
    protected PrologInterpreterVisitor ( ISymbolTable <Integer, String, Object> symbolTable,
                                         IVafInterner interner,
                                         IResolver <P, Q> resolver,
                                         IPositionalTermTraverser traverser,
                                         PrologInterpreter <T, P, Q, PC, QC> engine ) throws IOException {

        super(symbolTable, interner, resolver, traverser);
        this.engine = engine;
    }

    /**
     * @param predicate The predicate being entered.
     */
    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
//        PredicateTable <?> predicateTable = BaseApp.instance().getPredicateTable();

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
     * @param dottedPair
     * @throws LinkageException
     */
    @Override
    protected void enterListTerm ( ListTerm dottedPair ) throws LinkageException {

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
