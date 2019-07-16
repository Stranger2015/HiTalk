package org.ltc.hitalk.wam.compiler.expander;


import com.thesett.aima.logic.fol.Clause;
import org.ltc.hitalk.wam.task.TermRewriteTask;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.List;

/**
 *
 */
public
class DefaultTermExpander<T extends Clause, TT extends TermRewriteTask <T, TT>>
        extends TermRewriteTask <T, TT> {

    /**
     * @param target
     * @param transformer
     */
    public
    DefaultTermExpander ( List <T> target, ITransformer <T> transformer ) {
        super(target, transformer);
    }

    public
    List <T> invoke0 ( T c ) {
        List <T> list = super.invoke0(c);
        for (int i = 0; i < list.size(); i++) {
            T t = list.get(i);
            list.addAll(invoke(t));

        }

        return list;
    }

    /**
     * @param t
     */
    @Override
    public
    void add ( TT t ) {
        getComponents().add(t);
    }
}

