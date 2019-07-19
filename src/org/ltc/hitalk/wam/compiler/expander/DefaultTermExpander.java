package org.ltc.hitalk.wam.compiler.expander;


import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.wam.compiler.builtins.Bypass;
import org.ltc.hitalk.wam.task.TermRewriteTask;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 *
 */
public
class DefaultTermExpander<T extends Clause, TC extends Term, TT extends TermRewriteTask <T, TC, TT>>
        extends TermRewriteTask <T, TC, TT> {


    protected Function <TC, List <TC>> defaultAction = new Function <TC, List <TC>>() {
        @Override
        public
        List <TC> apply ( TC tc ) {

            if (tc instanceof Bypass) {
                return Collections.singletonList(tc);
            }
            else if (tc {

            }
            return null;
        }
    }
    tc ->

    apply ( tc );

    /**
     * @param target
     * @param transformer
     */
    public
    DefaultTermExpander ( List <T> target, ITransformer <T, TC> transformer ) {
        super(target, transformer);
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

