package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Unifier;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
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

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public abstract class MetaInterpreterVisitor extends HtBasePositionalVisitor
        implements HtPositionalTermVisitor {

    private final Resolver <HtPredicate, HtClause> resolver;
    protected HtPositionalTermTraverser positionalTraverser;
    protected List <HtClause> clauses = new ArrayList <>();

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

    public void setPositionalTraverser ( HtPositionalTermTraverser positionalTraverser ) {
        this.positionalTraverser = positionalTraverser;
    }

    protected void enterPredicate ( HtPredicate predicate ) {
        final HtPredicateDefinition def = predicate.getDefinition();
        for (int i = 0; i < def.size(); i++) {
            final HtClause cl = (HtClause) def.get(i);
            visit(cl);//todo stack
        }

    }

    protected void leavePredicate ( HtPredicate predicate ) {

    }

    /**
     * body may be encoded AS DOTTED PAIR (first goal) OR AS IS
     *
     * @param clause The clause being entered.
     */
    @Override
    protected void enterClause ( HtClause clause ) {
        for (int i = 0; i < clause.bodyLength(); i++) {
            IFunctor goal = clause.getGoal(i);
            visit(goal);
        }
    }

    protected void leaveClause ( HtClause clause ) {

    }

    @Override
    protected void enterDottedPair ( PackedDottedPair dottedPair ) {
        for (int i = 0, headsSize = dottedPair.size(); i <= headsSize; i++) {
            switch (dottedPair.getKind()) {
                case NOT:
                    visit(dottedPair.get(i));
                    break;
                case AND://fall down
                case OR:
                case IF:
                    visit(dottedPair.get(i));
                    visit(dottedPair.get(i + 1));
                    break;
                case GOAL:
                    IFunctor goal = (IFunctor) dottedPair.get(i);
                    if (goal.isDefined()) {
                        final Set <PiCalls> hbSet = new HashSet <>();
                        lookupGoal(goal, hbSet);
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

    Set <Variable> lookupGoal ( IFunctor goal, Set <PiCalls> hbSet ) {
//        Unifier<HtFunctor> unifier = this::unify;
//        final PiCalls mgGoal = new PiCalls();
        ITermFactory factory = Environment.instance().getTermFactory();
        final IFunctor ggoal = factory.createMostGeneral(goal);
        Unifier <HtFunctor> unifier = ( query, statement ) -> unify(query, statement);

        return null;
    }

    public HtFunctor mgt ( HtFunctor f, PiCalls pc ) {

    }

    @Override
    protected void leaveDottedPair ( PackedDottedPair dottedPair ) {

    }

    @Override
    protected void enterFunctor ( IFunctor functor ) {
        if (!functor.isBracketed() && functor.isDottedPair()) {
            enterDottedPair((PackedDottedPair) functor);
        } else {
            enterGoal(functor);
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

    protected void leaveGoal ( IFunctor functor ) {

    }

    protected void filterGoal ( IFunctor functor ) {

    }// ->	% partially instantiated (HiLog) call

    public Set <Variable> unify ( HtFunctor query, HtFunctor statement ) {
        resolver.reset();
        return resolver.resolve();
    }
}