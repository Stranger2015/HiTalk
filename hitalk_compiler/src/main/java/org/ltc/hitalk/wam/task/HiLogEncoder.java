package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.IntegerType;
import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.LiteralType;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IntTerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.MetaInterpreterVisitor;
import org.ltc.hitalk.wam.printer.HtLiteralType;
import org.ltc.hitalk.wam.printer.IListTermVisitor;
import org.ltc.hitalk.wam.printer.IPositionalTermTraverser;

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
                          IVafInterner interner,
                          IResolver <HtPredicate, HtClause> resolver,
                          IPositionalTermTraverser traverser ) {

        super(symbolTable, interner, resolver, traverser);
}

    /**
     * @param term
     * @return
     */
    public ITerm encode ( ITerm term ) {
        return term;
    }

    //    @Override
    protected void enterTerm ( ITerm term ) {

    }

    //    @Override
    protected void leaveTerm ( ITerm term ) {

    }

    @Override
    protected void enterFunctor ( IFunctor functor ) {

    }

    @Override
    protected void leaveFunctor ( IFunctor functor ) {
    }

    //    @Override
    protected void enterVariable ( Variable variable ) {
    }

    //    @Override
    protected void leaveVariable ( Variable variable ) {
    }

    @Override
    protected void enterPredicate ( HtPredicate predicate ) {
    }

    @Override
    protected void leavePredicate ( HtPredicate predicate ) {
    }

    @Override
    protected void enterClause ( HtClause clause ) throws LinkageException {
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

    public void visit ( IntTerm term1, IntTerm term2 ) {

    }

    public void visit ( HtLiteralType term1, HtLiteralType term2 ) {

    }

    public void visit ( ITerm term1, ITerm term2 ) {

    }

    public void visit ( HtPredicate predicate1, HtPredicate predicate2 ) {

    }

    public void visit ( HtVariable variable1, HtVariable variable2 ) {

    }

    public void visit ( IFunctor functor1, IFunctor functor2 ) throws LinkageException {

    }

    public void visit ( HtClause clause1, HtClause clause2 ) throws LinkageException {

    }

    public void visit ( ListTerm listTerm1, ListTerm listTerm2 ) throws LinkageException {

    }

    public IListTermVisitor getIListTermVisitor () {
        return null;
    }
}
