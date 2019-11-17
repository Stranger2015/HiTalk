package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IntTerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.printer.HtLiteralType;
import org.ltc.hitalk.wam.printer.IAllTermsVisitor;
import org.ltc.hitalk.wam.transformers.IGeneralizer;

import java.util.List;

public class MostSpecificGeneralizer implements IGeneralizer <ITerm>, IAllTermsVisitor {

    /**
     * Visits a predicate.
     *
     * @param predicate The predicate to visit.
     */
    @Override
    public void visit ( HtPredicate predicate ) {

    }

    public void visit ( HtPredicate predicate1, HtPredicate predicate2 ) {

    }

    /**
     * @param variable
     */
    @Override
    public void visit ( HtVariable variable ) {

    }

    public void visit ( HtVariable variable1, HtVariable variable2 ) {

    }

    /**
     * Visits a functor.
     *
     * @param functor The functor to visit.
     */
    @Override
    public void visit ( IFunctor functor ) throws LinkageException {

    }

    public void visit ( IFunctor functor1, IFunctor functor2 ) throws LinkageException {

    }

    /**
     * Visits a clause.
     *
     * @param clause The clause to visit.
     */
    @Override
    public void visit ( HtClause clause ) throws LinkageException {

    }

    public void visit ( HtClause clause1, HtClause clause2 ) throws LinkageException {

    }

    /**
     * @param term
     */
    @Override
    public void visit ( IntTerm term ) {

    }

    public void visit ( IntTerm term1, IntTerm term2 ) {

    }

    /**
     * @param listTerm
     * @throws LinkageException
     */
    @Override
    public void visit ( ListTerm listTerm ) throws LinkageException {

    }

    public void visit ( ListTerm listTerm1, ListTerm listTerm2 ) throws LinkageException {

    }

    /**
     * @param term
     */
    @Override
    public void visit ( HtLiteralType term ) {

    }

    public void visit ( HtLiteralType term1, HtLiteralType term2 ) {

    }

    /**
     * Visits a term.
     *
     * @param term The term to visit.
     */
    @Override
    public void visit ( ITerm term ) {

    }

    public void visit ( ITerm term1, ITerm term2 ) {

    }

    /**
     * @param term
     * @return
     */
    @Override
    public List <ITerm> generalize ( ITerm term ) {
        return null;
    }

    /**
     * @return
     */
    @Override
    public ExecutionContext getContext () {
        return null;
    }

    /**
     * @param context
     */
    @Override
    public void setContext ( ExecutionContext context ) {

    }

    /**
     * @param max
     * @return
     */
    @Override
    public boolean isAcceptable ( IMetrics max ) {
        return false;
    }

    /**
     *
     */
    @Override
    public void cancel () {

    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run () {

    }
}
