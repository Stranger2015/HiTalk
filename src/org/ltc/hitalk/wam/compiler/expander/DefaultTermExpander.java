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


    protected Function <TC, List <TC>> defaultAction = tc -> {

        if (tc instanceof Bypass) {
            return Collections.singletonList(tc);
        }
        return null;
    };

    /**
     * @param t
     */
    public
    void add ( TT t ) {
        getComponents().add(t);
    }

    /**
     * @param action
     * @param target
     * @param transformer
     */
    public
    DefaultTermExpander ( Function <T, List <T>> action, List <TC> target, ITransformer <T, TC> transformer ) {
        super(action, target, transformer);
    }
}

