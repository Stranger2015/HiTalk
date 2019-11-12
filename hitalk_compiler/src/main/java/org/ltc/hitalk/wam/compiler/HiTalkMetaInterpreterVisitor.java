package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.IVafInterner;
import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.PackedDottedPair;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.HtPositionalTermVisitor;

public class HiTalkMetaInterpreterVisitor extends HtBasePositionalVisitor
        implements HtPositionalTermVisitor {
    protected HtPositionalTermTraverser positionalTraverser;

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public HiTalkMetaInterpreterVisitor ( SymbolTable <Integer, String, Object> symbolTable, IVafInterner interner ) {
        super(symbolTable, interner);
    }

    /**
     * @param positionalTraverser
     */
    @Override
    public void setPositionalTraverser ( HtPositionalTermTraverser positionalTraverser ) {
        this.positionalTraverser = positionalTraverser;
    }

    protected void enterPredicate ( HtPredicate predicate ) {
        super.enterPredicate(predicate);
    }

    protected void leavePredicate ( HtPredicate predicate ) {
        super.leavePredicate(predicate);
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

    protected void leaveFunctor ( IFunctor functor ) {
        super.leaveFunctor(functor);
    }

    protected void enterVariable ( Variable variable ) {
        super.enterVariable(variable);
    }

    protected void leaveVariable ( Variable variable ) {
        super.leaveVariable(variable);
    }

    protected void enterClause ( HtClause clause ) throws LinkageException {
        super.enterClause(clause);
    }

    protected void leaveClause ( HtClause clause ) {
        super.leaveClause(clause);
    }
}
