package org.ltc.hitalk.wam.compiler.expander;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
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
class DefaultTermExpander<T extends HtClause, TC extends ITerm, TT extends TermRewriteTask <T, TC, TT>>
        extends TermRewriteTask <T, TC, TT> {

    protected final Function <TC, List <TC>> dcgExpansionAction = this::dcgExpansion;
    protected final Function <TC, List <TC>> defaultExpansionAction = this::defaultExpansion;
    protected final TermRewriteTask[] trt;

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
    @SafeVarargs
    public DefaultTermExpander ( List <TC> target, ITransformer <TC> transformer, TermRewriteTask <HtClause, TC, ?>... trt ) {
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

    public void run () {

    }
}


