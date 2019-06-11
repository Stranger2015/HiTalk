package org.ltc.hitalk.wam.compiler.expander;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.wam.task.TermRewriteTask;

/**
 *
 */
public
class DefaultTermExpander<T extends Term, TT extends TermRewriteTask <T, TT>> extends TermRewriteTask <T, TT> {
    public
    DefaultTermExpander () {
        super(defaultExpander);
    }
}
