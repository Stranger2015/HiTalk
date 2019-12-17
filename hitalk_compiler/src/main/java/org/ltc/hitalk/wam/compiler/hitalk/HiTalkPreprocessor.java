package org.ltc.hitalk.wam.compiler.hitalk;


import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.HtMethod;
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
public class HiTalkPreprocessor<T extends HtMethod, P, Q, TT extends TransformTask>
        extends HiTalkPreCompiler <T, P, Q> {

    protected final DefaultTransformer defaultTransformer;
    protected final List <TT> components = new ArrayList <>();

    private static Object apply ( Object o ) {
        return o;
    }

    @Override
    public LogicCompilerObserver <P, Q> getObserver () {
        return observer;
    }

    protected LogicCompilerObserver <P, Q> observer;
    protected List <T> preCompiledTarget;
    protected PlPrologParser parser;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param resolver
     */
    public HiTalkPreprocessor ( ISymbolTable <Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                PrologBuiltInTransform <T, P, Q> builtInTransform,
                                PrologDefaultBuiltIn defaultBuiltIn,
                                IResolver <HtPredicate, HtClause> resolver,
                                PlPrologParser parser )
            throws LinkageException {

        super(symbolTable, interner, builtInTransform, defaultBuiltIn, resolver, parser);

        defaultTransformer = new DefaultTransformer(null);

        if (preCompiledTarget != null) {
            for (final T t : preCompiledTarget) {
                resolver.setQuery(t);
                resolver.resolve();
            }
        }
//        components.add((TT)
    }

    /**
     * {@inheritDoc}
     *
     * @param sentence
     */
    @Override
    public void compile ( Sentence <T> sentence ) throws SourceCodeException {
//
    }

    /**
     * @param clauses
     */
    @Override
    protected void saveResult ( List <T> clauses ) {
        preCompiledTarget = clauses;
    }

    /**
     * @param t
     * @return
     */
    protected List <T> preprocess ( T t ) {
        List <T> list = new ArrayList <>();

        components.stream().map(task -> task.invoke(t)).forEach((Consumer <? super List <ITerm>>) list);//fixme

        return list;
    }

    private void initialize () {

    }

    /**
     * @param t
     */
//    @Override
    public void add ( TT t ) {
        components.add(t);
    }

    //    @Override
    public List <TT> getComponents () {
        return components;
    }
}
