package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.term.ITerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static java.lang.String.format;

/**
 *
 *
 */
abstract public
class PreCompilerTask<T extends HtClause> implements IInvokable<ITerm>, IHitalkObject {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected final PlLexer tokenSource;
    protected final IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?> preCompiler;
    protected final EnumSet<DirectiveKind> kind;

    /**
     * @param newTask
     */
    public void addTask(PreCompilerTask<T> newTask) {
        preCompiler.getTaskQueue().add(newTask);
    }


    @Override
    public Logger getLogger() {
        return logger;
    }

    /**
     * @return
     */
    public PlLexer getTokenSource() {
        return tokenSource;
    }

    public IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?> getPreCompiler() {
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
    public PreCompilerTask(IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?> preCompiler, PlLexer tokenSource, EnumSet<DirectiveKind> kind) {
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
    protected List<ITerm> output = new ArrayList<>();

    /**
     * @param term
     * @return
     */
    @Override
    public final List<ITerm> invoke(ITerm term) throws Exception {
        List<ITerm> list = IInvokable.super.invoke(term);
        input = term;
        for (ITerm t : list) {
            logger.info("t: " + t);
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
    protected List<ITerm> invoke0(ITerm term) throws Exception {
//        if (!output.contains(term)) {
            output.add(term);
//        }
        return output;
    }

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run() {

    }
}
