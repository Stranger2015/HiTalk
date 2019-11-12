package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.IntegerType;
import com.thesett.aima.logic.fol.LiteralType;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.MetaInterpreterVisitor;

import static java.lang.String.format;

/**
 *
 */
public class HiLogEncoder extends MetaInterpreterVisitor {

    /**
     * Creates a positional visitor.
     *
     * @param symbolTable The compiler symbol table.
     * @param interner    The name interner.
     */
    public HiLogEncoder ( SymbolTable <Integer, String, Object> symbolTable,
                          IVafInterner interner ) {
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
    protected void enterFunctor ( IFunctor functor ) {

    }

    @Override
    protected void leaveFunctor ( IFunctor functor ) {
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
