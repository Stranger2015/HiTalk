package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.printer.HtBasePositionalVisitor;
import org.ltc.hitalk.wam.printer.HtPositionalTermTraverser;
import org.ltc.hitalk.wam.printer.HtPositionalTermVisitor;

public class PrologPositionalTransformVisitor extends HtBasePositionalVisitor
        implements HtPositionalTermVisitor {

    protected HtPositionalTermTraverser positionalTraverser;

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public PrologPositionalTransformVisitor ( SymbolTable <Integer, String, Object> symbolTable,
                                              VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
    }

    /**
     * @param positionalTraverser
     */
    @Override
    public void setPositionalTraverser ( HtPositionalTermTraverser positionalTraverser ) {
        this.positionalTraverser = positionalTraverser;
    }

    /**
     * @param functor
     */
    @Override
    public void visit ( HtFunctor functor ) {

    }

    public void visit ( Term term ) {
        super.visit(term);
    }

    public void visit ( Variable variable ) {
        super.visit(variable);
    }

    public void visit ( HtPredicate predicate ) {
        super.visit(predicate);
    }

    public void visit ( HtClause clause ) {
        super.visit(clause);
    }

    public void visit ( IntegerType literal ) {
        super.visit(literal);
    }

    public void visit ( LiteralType literal ) {
        super.visit(literal);
    }

    protected void enterTerm ( Term term ) {
        super.enterTerm(term);
    }

    protected void leaveTerm ( Term term ) {
        super.leaveTerm(term);
    }

    protected void enterFunctor ( HtFunctor functor ) {
        super.enterFunctor(functor);
    }

    protected void leaveFunctor ( HtFunctor functor ) {
        super.leaveFunctor(functor);
    }

    protected void enterVariable ( Variable variable ) {
        super.enterVariable(variable);
    }

    protected void leaveVariable ( Variable variable ) {
        super.leaveVariable(variable);
    }

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

    protected void enterIntLiteral ( IntegerType literal ) {
        super.enterIntLiteral(literal);
    }

    protected void leaveIntLiteral ( IntegerType literal ) {
        super.leaveIntLiteral(literal);
    }

    protected void enterLiteral ( LiteralType literal ) {
        super.enterLiteral(literal);
    }

    protected void leaveLiteral ( LiteralType literal ) {
        super.leaveLiteral(literal);
    }
}
