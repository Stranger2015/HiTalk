package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.PackedDottedPair;

/**
 *
 */
public class PiCallsCollectorVisitor extends MetaInterpreterVisitor {
    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    protected PiCallsCollectorVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                        VariableAndFunctorInterner interner,
                                        Resolver <HtPredicate, HtClause> resolver ) {
        super(symbolTable, interner, resolver);
    }

    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
        super.enterPredicate(predicate);
    }

    protected void leavePredicate ( HtPredicate predicate ) {
        super.leavePredicate(predicate);
    }

    protected void enterClause ( HtClause clause ) {
        super.enterClause(clause);
    }

    protected void leaveClause ( HtClause clause ) {
        super.leaveClause(clause);
    }

    protected void enterDottedPair ( PackedDottedPair dottedPair ) throws LinkageException {
        super.enterDottedPair(dottedPair);
    }

    protected void leaveDottedPair ( PackedDottedPair dottedPair ) {
        super.leaveDottedPair(dottedPair);
    }

    protected void enterFunctor ( IFunctor functor ) throws LinkageException {
        super.enterFunctor(functor);
    }

    @Override
    protected void leaveFunctor ( IFunctor functor ) {
        super.leaveFunctor(functor);
    }


    protected void enterGoal ( IFunctor functor ) {
        super.enterGoal(functor);
    }

    protected boolean filterGoal ( IFunctor symbol, Term[] args ) {
        return super.filterGoal(symbol, args);
    }
}
