package org.ltc.hitalk.wam.printer;

import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IntTerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.IFunctor;

public interface IAllTermsVisitor2 {
    void visit ( ITerm term1, ITerm term2 );

    void visit ( IntTerm term1, IntTerm term2 );

    void visit ( HtVariable term1, HtVariable term2 );

    void visit ( IFunctor term1, IFunctor term2 );

    void visit ( ListTerm term1, ListTerm term2 );


}

