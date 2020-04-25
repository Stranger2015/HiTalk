package org.ltc.hitalk.wam.compiler.hitalk;


import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Sentence;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.PrologBuiltInTransform;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.task.TransformTask;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 *
 */
public class HiTalkPreprocessor<T extends HtMethod, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery>
        extends HiTalkPreCompiler<T, P, Q, PC, QC> {

    protected final DefaultTransformer defaultTransformer;
    protected final List<TransformTask> components = new ArrayList<>();

    private static Object apply(Object o) {
        return o;
    }

    @Override
    public ICompilerObserver<P, Q> getObserver() {
        return observer;
    }

    protected ICompilerObserver<P, Q> observer;
    protected List<QC> preCompiledTarget;
    protected HtPrologParser parser;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param resolver
     */
    public HiTalkPreprocessor(ISymbolTable<Integer, String, Object> symbolTable,
                              IVafInterner interner,
                              PrologBuiltInTransform<T, P, Q, PC, QC> builtInTransform,
                              PrologDefaultBuiltIn defaultBuiltIn,
                              IResolver<PC, QC> resolver,
                              HtPrologParser parser)
            throws LinkageException {

        super(symbolTable, interner, builtInTransform, defaultBuiltIn, resolver, parser);

        defaultTransformer = new DefaultTransformer(null);

        if (preCompiledTarget != null) {
            for (final QC t : preCompiledTarget) {
                resolver.setQuery((QC) t);
                resolver.resolve();
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param sentence
     */
    @Override
    public void compile(Sentence<T> sentence) throws HtSourceCodeException {
//
    }

    /**
     * @param clauses
     */
    @Override
    protected void saveResult(List<QC> clauses) {
        preCompiledTarget = clauses;
    }

    /**
     * @param t
     * @return
     */
    ;
    protected List<T> preprocess(T t) {
        List<T> list = new ArrayList<>();

        components.stream().map(
                task -> {
                    try {
                        return task.invoke(t);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return null;
                }).forEach((Consumer<? super List<ITerm>>) list);//fixme

        return list;
    }

    private void initialize() {

    }

    /**
     * @param t
     */
//    @Override
    public void add(TransformTask t) {
        components.add(t);
    }

    //    @Override
    public List<TransformTask> getComponents() {
        return components;
    }
}
