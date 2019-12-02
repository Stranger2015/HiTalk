package org.ltc.hitalk.wam.compiler.hitalk;


import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.expander.DefaultTermExpander;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.task.StandardPreprocessor;
import org.ltc.hitalk.wam.task.TransformTask;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class HiTalkPreprocessor<T extends HtMethod, P, Q, TT extends TransformTask <T>>
        extends HiTalkPreCompiler <T, P, Q> {

    protected final DefaultTransformer <T> defaultTransformer;
    //    protected final HiTalkDefaultBuiltIn defaultBuiltIn;
    //    protected final HiTalkBuiltInTransform builtInTransform;
    protected final List <TT> components = new ArrayList <>();
    //    protected final Function <TC, List <TC>> defaultAction;

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
    public HiTalkPreprocessor ( SymbolTable <Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                PrologBuiltInTransform <T, P, Q> builtInTransform,
                                PrologDefaultBuiltIn defaultBuiltIn,
                                IResolver <P, Q> resolver,
                                PlPrologParser parser )
            throws LinkageException {

        super(symbolTable, interner, builtInTransform, defaultBuiltIn, resolver, parser);

        defaultTransformer = new DefaultTransformer <>(null);

        if (preCompiledTarget != null) {
            for (final T t : preCompiledTarget) {
                resolver.setQuery((Q) t);
                resolver.resolve();
            }
        }

        components.add((TT) new DefaultTermExpander(preCompiledTarget, defaultTransformer));
//        components.add((TT) new HiLogPreprocessor <>(null, defaultTransformer, interner));
        components.add((TT) new StandardPreprocessor(null, preCompiledTarget, defaultTransformer));
    }

    /**
     * {@inheritDoc}
     *
     * @param sentence
     */
    @Override
    public void compile ( Sentence <T> sentence ) throws SourceCodeException {

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

        components.stream().map(task -> task.invoke(t)).forEach(list::addAll);

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
