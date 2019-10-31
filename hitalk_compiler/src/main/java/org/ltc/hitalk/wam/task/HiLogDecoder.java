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
public class HiLogDecoder extends PrologPositionalTransformVisitor {
    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public HiLogDecoder ( SymbolTable <Integer, String, Object> symbolTable,
                          VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
    }

    /**
     * @param term
     * @return
     */
    public Term decode ( Term term ) {
//        enterTerm(term);
        return term;
    }

    /**
     * @param term The term being entered.
     */
    @Override
    protected void enterTerm ( Term term ) {
    }

    /**
     * @param term The term being left.
     */
    @Override
    protected void leaveTerm ( Term term ) {
    }

    /**
     * @param functor The functor being entered.
     */
    @Override
    protected void enterFunctor ( HtFunctor functor ) {
        super.enterFunctor(functor);
    }

    /**
     * @param functor The functor being left.
     */
    @Override
    protected void leaveFunctor ( HtFunctor functor ) {
        super.leaveFunctor(functor);
    }

    /**
     * @param variable The variable being entered.
     */
    @Override
    protected void enterVariable ( Variable variable ) {
        super.enterVariable(variable);
    }

    /**
     * @param variable The variable being left.
     */
    @Override
    protected void leaveVariable ( Variable variable ) {

    }

    /**
     * @param predicate The predicate being entered.
     */
    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
        super.enterPredicate(predicate);
    }

    /**
     * @param predicate The predicate being left.
     */
    @Override
    protected void leavePredicate ( HtPredicate predicate ) {
        super.leavePredicate(predicate);
    }

    /**
     * @param clause The clause being entered.
     */
    @Override
    protected void enterClause ( HtClause clause ) {
        super.enterClause(clause);
    }

    /**
     * @param clause The clause being left.
     */
    @Override
    protected void leaveClause ( HtClause clause ) {
        super.leaveClause(clause);
    }

    /**
     * @param literal The integer literal being entered.
     */
    @Override
    protected void enterIntLiteral ( IntegerType literal ) {
        super.enterIntLiteral(literal);
    }

    /**
     * @param literal The integer literal being left.
     */
    @Override
    protected void leaveIntLiteral ( IntegerType literal ) {
        super.leaveIntLiteral(literal);
    }

    /**
     * @param literal The literal being entered.
     */
    @Override
    protected void enterLiteral ( LiteralType literal ) {
        super.enterLiteral(literal);
    }

    /**
     * @param literal The literal being left.
     */
    @Override
    protected void leaveLiteral ( LiteralType literal ) {
        super.leaveLiteral(literal);
    }

    /**
     * @return
     */
    @Override
    public String toString () {
        return format("%s{traverser=%s", getClass().getSimpleName(), traverser);
    }

}
