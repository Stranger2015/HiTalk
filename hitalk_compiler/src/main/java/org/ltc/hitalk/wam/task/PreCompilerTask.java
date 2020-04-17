package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.lang.String.format;

/**
 *
 */
abstract public
class PreCompilerTask implements IInvokable<ITerm>, IHitalkObject {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected final PlLexer tokenSource;
    protected final IPreCompiler preCompiler;
    protected final EnumSet<DirectiveKind> kind;
//    protected PreCompilerTask nextTask;
//
//    public PreCompilerTask getNextTask() {
//        return nextTask;
//    }
//
//    public void setNextTask(PreCompilerTask nextTask) {
//        this.nextTask = nextTask;
//    }

    public void addTask(PreCompilerTask newTask) {
//        PreCompilerTask tmp = this.nextTask;
//        this.nextTask = newTask;
//        newTask = tmp;
        preCompiler.getTaskQueue().add(newTask);
    }


    @Override
    public Logger getLogger() {
        return logger;
    }

    public PlLexer getTokenSource() {
        return tokenSource;
    }

    public IPreCompiler getPreCompiler() {
        return preCompiler;
    }

    public EnumSet<DirectiveKind> getKind() {
        return kind;
    }

    /**
     * @param tokenSource
     * @param preCompiler
     * @param kind
     */
    public PreCompilerTask(IPreCompiler preCompiler, PlLexer tokenSource, EnumSet<DirectiveKind> kind) {
        this.preCompiler = preCompiler;
        this.tokenSource = tokenSource;
        this.kind = kind;
    }

    /**
     * @return
     */
    public ITerm getInput() {
        return input;
    }

    /**
     * @return
     */
    public List<ITerm> getOutput() {
        return output;
    }

    protected ITerm input;
    protected final List<ITerm> output = new ArrayList<>();

    /**
     * @param term
     * @return
     */
    @Override
    public final List<ITerm> invoke(ITerm term) throws IOException {
        List<ITerm> list = IInvokable.super.invoke(term);
        input = term;
        for (ITerm t : list) {
            final List<ITerm> listj = invoke0(t);
            output.addAll(listj);
        }

        return output;
    }


    /**
     *
     */
    public void banner() {
        logger.info(format("\nPerforming %s task ...", getClass().getSimpleName()));
    }

    /**
     * @param term
     * @return
     */
    protected List<ITerm> invoke0(ITerm term) throws IOException {
        if (!output.contains(term)) {
            output.add(term);
        }
        return output;
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }
}
