package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.PackedDottedPair;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.HtPositionalTermVisitor;

import java.util.List;

/**
 *
 */
public abstract class MetaInterpreterVisitor extends HtBasePositionalVisitor
        implements HtPositionalTermVisitor {

    protected HtPositionalTermTraverser positionalTraverser;
    protected List <HtClause> clauses;

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    protected MetaInterpreterVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                       VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
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
            HtFunctor goal = clause.getGoal(i);
            visit(goal);
        }
    }

    protected void leaveClause ( HtClause clause ) {

    }

    @Override
    protected void enterDottedPair ( PackedDottedPair dottedPair ) {

    }

    protected void leaveDottedPair ( PackedDottedPair dottedPair ) {

    }

    protected void enterFunctor ( HtFunctor functor ) {
        if (!functor.isBracketed() && functor.isDottedPair())
    }

    protected void leaveFunctor ( HtFunctor functor ) {

    }
}