package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.PrologPositionalTransformVisitor;

import static java.lang.String.format;

/**
 *
 */
public class HiLogEncoder extends PrologPositionalTransformVisitor {

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public HiLogEncoder ( SymbolTable <Integer, String, Object> symbolTable,
                          VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
    }

    /**
     * @param term
     * @return
     */
    public Term encode ( Term term ) {
        return term;
    }

    //    @Override
    protected void enterTerm ( Term term ) {

    }

    //    @Override
    protected void leaveTerm ( Term term ) {

    }

    @Override
    protected void enterFunctor ( HtFunctor functor ) {

    }

    @Override
    protected void leaveFunctor ( HtFunctor functor ) {
    }

    @Override
    protected void enterVariable ( Variable variable ) {
    }

    @Override
    protected void leaveVariable ( Variable variable ) {
    }

    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
    }

    @Override
    protected void leavePredicate ( HtPredicate predicate ) {
    }

    @Override
    protected void enterClause ( HtClause clause ) {
        super.enterClause(clause);
    }

    @Override
    protected void leaveClause ( HtClause clause ) {
        super.leaveClause(clause);
    }

    @Override
    protected void enterIntLiteral ( IntegerType literal ) {

    }

    @Override
    protected void leaveIntLiteral ( IntegerType literal ) {

    }

    @Override
    protected void enterLiteral ( LiteralType literal ) {

    }

    @Override
    protected void leaveLiteral ( LiteralType literal ) {
    }

    @Override
    public String toString () {
        return format("%s{traverser=%s", getClass().getSimpleName(), traverser);
    }

}
