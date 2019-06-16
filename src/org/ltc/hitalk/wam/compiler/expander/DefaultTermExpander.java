package org.ltc.hitalk.wam.compiler.expander;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.wam.task.TermRewriteTask;
import org.ltc.hitalk.wam.transformers.ITransformer;

/**
 *
 */
public
class DefaultTermExpander<T extends Term, TT extends TermRewriteTask <T, TT>> extends TermRewriteTask <T, TT> {

    /**
     * @param target
     * @param transformer
     */
    public
    DefaultTermExpander ( T target, ITransformer <T> transformer ) {
        super(target, transformer);
    }
}
