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

public class Lgg extends BinTermVisitor implements IGeneralizer <ITerm>, IAllTermsVisitor {

    @Override
    public void visit ( HtPredicate predicate ) {

    }

    @Override
    public void visit ( HtVariable variable ) {

    }

    @Override
    public void visit ( IFunctor functor ) throws LinkageException {

    }

    @Override
    public void visit ( HtClause clause ) throws LinkageException {

    }

    @Override
    public void visit ( IntTerm term ) {

    }

    @Override
    public void visit ( ListTerm listTerm ) throws LinkageException {

    }

    @Override
    public void visit ( HtLiteralType term ) {

    }

    @Override
    public void visit ( ITerm term ) {

    }

    @Override
    public List <ITerm> generalize ( ITerm term ) {
        return null;
    }

    @Override
    public ExecutionContext getContext () {
        return null;
    }

    @Override
    public void setContext ( ExecutionContext context ) {

    }

    @Override
    public boolean isAcceptable ( IMetrics max ) {
        return false;
    }

    @Override
    public void cancel () {

    }

    @Override
    public void run () {

    }
}
