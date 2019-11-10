package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.PackedDottedPair;
import org.ltc.hitalk.term.io.Environment;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.HtPositionalTermVisitor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public abstract class MetaInterpreterVisitor extends HtBasePositionalVisitor
        implements HtPositionalTermVisitor {

    protected final Resolver <HtPredicate, HtClause> resolver;
    protected HtPositionalTermTraverser positionalTraverser;
    protected final List <HtClause> clauses = new ArrayList <>();
    protected final Set <Variable> bindings = new HashSet <>();

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    protected MetaInterpreterVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                       VariableAndFunctorInterner interner,
                                       Resolver <HtPredicate, HtClause> resolver ) {
        super(symbolTable, interner);
        this.resolver = resolver;
    }

    /**
     * @param positionalTraverser
     */
    public void setPositionalTraverser ( HtPositionalTermTraverser positionalTraverser ) {
        this.positionalTraverser = positionalTraverser;
    }

    /**
     * @param predicate The predicate being entered.
     */
    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
        final HtPredicateDefinition def = predicate.getDefinition();
        //todo op stack ==> term.accept(this);
        int bound = def.size();
        IntStream.range(0, bound).mapToObj(i -> (HtClause) def.get(i)).forEachOrdered(clause ->
                clause.accept(this));

    }

    protected void leavePredicate ( HtPredicate predicate ) {

    }

    /**
     * body may be encoded AS DOTTED PAIR (first goal) OR AS IS
     *
     * @param clause The clause being entered.
     */
    @Override
    protected void enterClause ( HtClause clause ) throws LinkageException {
        IntStream.range(0, clause.bodyLength()).mapToObj(clause::getGoal).forEachOrdered(goal ->
                goal.accept(this));
    }

    protected void leaveClause ( HtClause clause ) {

    }

    @Override
    protected void enterDottedPair ( PackedDottedPair dottedPair ) throws LinkageException {
        for (int i = 0, headsSize = dottedPair.size(); i <= headsSize; i++) {
            switch (dottedPair.getKind()) {
                case NOT:
                    dottedPair.get(i).accept(this);
                    break;
                case AND://fall down
                case OR:
                case IF:
                    dottedPair.get(i).accept(this);
                    dottedPair.get(i + 1).accept(this);
                    break;
                case GOAL:
                    IFunctor goal = (IFunctor) dottedPair.get(i);
                    if (goal.isDefined()) {
                        final Set <PiCalls> hbSet = new HashSet <>();
                        lookupGoal((PiCalls) goal, hbSet);
                    }
                    break;
                case TRUE:
                    break;
                case INLINE_GOAL:
                    break;
                case OTHER:
                    break;
                default:
                    throw new ExecutionError(PERMISSION_ERROR, null);
            }
        }
    }

    public void lookupGoal ( PiCalls goal, Set <PiCalls> hbSet ) throws LinkageException {
        ITermFactory factory = Environment.instance().getTermFactory();
        goal.setMgCalls(factory.newVariable("Calls"));
        final HtFunctor eqf = new HtFunctor(interner.internFunctorName("=", 2), EMPTY_TERM_ARRAY);// fixme
        final HtClause query = new HtClause(null, null, new HtFunctor[]{eqf});
        eqf.setArgument(0, goal);
        for (PiCalls hbEl : hbSet) {
            eqf.setArgument(1, hbEl);//fixme
            resolver.reset();
            resolver.setQuery(query);
            final Set <Variable> bindings = resolver.resolve();
            if (bindings != null) {
                this.bindings.addAll(bindings);
                return;
            }
        }

        hbSet.add(goal);//if not found just add
    }

    @Override
    protected void leaveDottedPair ( PackedDottedPair dottedPair ) {

    }

    @Override
    protected void enterFunctor ( IFunctor functor ) throws LinkageException {
        if (!functor.isBracketed() && functor.isDottedPair()) {
            enterDottedPair((PackedDottedPair) functor);
        } else {
            enterGoal(functor);//fixme redundant
        }
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
    protected void enterGoal ( IFunctor functor ) {

    }

    /**
     * @param functor
     */
    protected void leaveGoal ( IFunctor functor ) {

    }

    /**
     * @param sym
     * @param args
     */
    protected boolean filterGoal ( IFunctor sym, Term[] args ) {
//        lookupGoal(sym, args);
        return true;
    }// ->	% partially instantiated (HiLog) call

//    public Set <Variable> unify ( HtFunctor query, HtFunctor statement ) {
//        resolver.reset();
//        return resolver.resolve();
//    }
}