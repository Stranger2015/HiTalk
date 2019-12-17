package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.IPendingTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

/**
 *
 */
abstract public
class PreCompilerTask implements IPendingTasks, IInvokable <ITerm>, IHitalkObject {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected final PlTokenSource tokenSource;
    protected final IPreCompiler preCompiler;
    protected final EnumSet <DirectiveKind> kind;

    public PreCompilerTask ( IPreCompiler preCompiler ) {
        this.preCompiler = preCompiler;
        kind = EnumSet.noneOf(DirectiveKind.class);
        tokenSource = null;
    }

    public ITerm getInput () {
        return input;
    }

    public Deque <ITerm> getOutput () {
        return output;
    }

    protected ITerm input;
    protected final Deque <ITerm> output = new ArrayDeque <>();

    public PreCompilerTask ( IPreCompiler preCompiler,
                             PlTokenSource tokenSource,
                             EnumSet <DirectiveKind> kind ) {
        this.preCompiler = preCompiler;
        this.tokenSource = tokenSource;
        this.kind = kind;
    }

    /**
     * @param term
     * @return
     */
    @Override
    public final List <ITerm> invoke ( ITerm term ) {
        List <ITerm> list = IInvokable.super.invoke(term);
        for (ITerm t : list) {
            list.addAll(invoke0(t));
        }

        return list;
    }

    /**
     * @param term
     * @return
     */
    protected List <ITerm> invoke0 ( ITerm term ) {
        return Collections.singletonList(term);
    }

    /**
     *
     */
    public void banner () {
        logger.info(format("\nPerforming %s task ...", getClass().getSimpleName()));
    }
}
