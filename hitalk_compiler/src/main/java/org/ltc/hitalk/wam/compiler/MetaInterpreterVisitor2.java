package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IntTerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.printer.HtLiteralType;
import org.ltc.hitalk.wam.printer.IAllTermsVisitor;

import java.util.HashMap;
import java.util.Map;

public class MetaInterpreterVisitor2 implements IAllTermsVisitor {
    private final Map <ITerm, ITerm> dictionary1 = new HashMap <>();
    private final Map <ITerm, ITerm> dictionary2 = new HashMap <>();

    IAllTermsVisitor visitor = new MetaInterpreterVisitor2();//fixme

    public void visit ( HtPredicate predicate1, HtPredicate predicate2 ) {

    }

    public void visit ( HtVariable variable1, HtVariable variable2 ) {
//
        HtVariable newVar;
        if (variable1.isVar() && variable2.isVar()) {
            newVar = variable1;
            if (variable1.getId() == variable2.getId()) {
//                newVar = variable1;
            } else {
                variable1.setSubstitution(variable2);
//                newVar = variable1;
            }
            newVar = new HtVariable(variable2.getId(), variable2, false);
            dictionary1.put(newVar, variable1);
            dictionary2.put(newVar, variable2);
        } else {//nonVar
//            HtVariable variable = newVar(variable1, variable2);
        }
    }

////    private HtVariable newVar ( HtVariable variable1, HtVariable variable2 ) {
////        return null;
////    }
//
//private boolean isSimilar(ITerm term1, ITerm term2){
//        term1.accept(this);
//
}

    public void visit ( IFunctor functor1, IFunctor functor2 ) throws LinkageException {

    }

    public void visit ( HtClause clause1, HtClause clause2 ) throws LinkageException {

    }

    public void visit ( IntTerm term1, IntTerm term2 ) {

    }

    public void visit ( ListTerm listTerm1, ListTerm listTerm2 ) throws LinkageException {

    }

    public void visit ( HtLiteralType term1, HtLiteralType term2 ) {

    }

    public void visit ( ITerm term1, ITerm term2 ) {

    }
}
