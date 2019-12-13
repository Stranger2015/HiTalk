package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.term.ITerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

import static java.lang.String.format;

/**
 *
 */
abstract public
class CompilerTask implements IInvokable <ITerm> {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected final IPreCompiler preCompiler;
    protected final Function <ITerm, List <ITerm>> action;
    protected final DirectiveKind kind;
    protected ITerm input;

    public CompilerTask ( IPreCompiler preCompiler,
                          Function <ITerm, List <ITerm>> action,
                          DirectiveKind kind ) {
        this.preCompiler = preCompiler;
        this.action = action;
        this.kind = kind;
    }

    /**
     * @param t
     * @return
     */
    @Override
    public final List <ITerm> invoke ( ITerm t ) {
        List <ITerm> list = IInvokable.super.invoke(t);
        list.forEach(t1 -> list.addAll(action.apply(t1)));

        return list;
    }

    /**
     *
     */
    public
    void banner () {
        logger.debug(format("\nPerforming %s task ...", getClass().getSimpleName()));
    }
}
