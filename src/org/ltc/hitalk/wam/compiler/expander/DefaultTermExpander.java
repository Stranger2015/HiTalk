package org.ltc.hitalk.wam.compiler.expander;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;
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
class DefaultTermExpander<T extends HtClause, TC extends Term, TT extends TermRewriteTask <T, TC, TT>>
        extends TermRewriteTask <T, TC, TT> {


    //    private final Function <T, List <T>> action;
    protected final Function <TC, List <TC>> defaultAction = this::apply;

    /**
     * @param t
     */
    public
    void add ( TT t ) {
        getComponents().add(t);
    }

    /**
     * @param target
     * @param transformer
     */
    public
    DefaultTermExpander ( List <TC> target, ITransformer <T, TC> transformer ) {
        super(null, target, transformer);
//        this.action = action;
    }

    private
    List <TC> apply ( TC tc ) {
        if (tc instanceof Bypass) {
            return Collections.singletonList(tc);
        }
        return null;
    }
}

