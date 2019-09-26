package org.ltc.hitalk.wam.compiler.expander;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.builtins.Bypass;
import org.ltc.hitalk.wam.task.TermRewriteTask;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 *
 */
public
class DefaultTermExpander<T extends HtClause, TC extends Term, TT extends TermRewriteTask <T, TC, TT>>
        extends TermRewriteTask <T, TC, TT> {

    protected final Function <TC, List <TC>> dcgExpansionAction = this::dcgExpansion;
    protected final Function <TC, List <TC>> defaultExpansionAction = this::defaultExpansion;
    private final TermRewriteTask[] trt;

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
    DefaultTermExpander ( List <TC> target, ITransformer <T, TC> transformer, TermRewriteTask... trt ) {
        super(null, target, transformer);
        this.trt = trt;
//        add(ruleExpander);
        if (target != null) {
            target.forEach(this::defaultExpansion);
        }
    }

    /**
     * @param tc
     * @return
     */
    protected
    List <TC> defaultExpansion ( TC tc ) {
        List <TC> result = new ArrayList <>();
        if (tc instanceof Bypass) {
            result = Collections.singletonList(tc);
        }

        return result;
    }

    /**
     * @param tc
     * @return
     */
    protected
    List <TC> dcgExpansion ( TC tc ) {
        List <TC> result = new ArrayList <>();

        return result;
    }
}


