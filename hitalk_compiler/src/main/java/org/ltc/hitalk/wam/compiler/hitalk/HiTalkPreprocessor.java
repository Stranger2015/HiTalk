package org.ltc.hitalk.wam.compiler.hitalk;


import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.expander.DefaultTermExpander;
import org.ltc.hitalk.wam.task.HiLogPreprocessor;
import org.ltc.hitalk.wam.task.StandardPreprocessor;
import org.ltc.hitalk.wam.task.TransformTask;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 */
public
class HiTalkPreprocessor<TC extends Term, TT extends TransformTask <HtClause, TC>>
        extends HiTalkPreCompiler {

    protected final DefaultTransformer <HtClause, TC> defaultTransformer;
    protected final HiTalkDefaultBuiltIn defaultBuiltIn;
    //    protected final HiTalkBuiltInTransform builtInTransform;
    protected final List <TT> components = new ArrayList <>();
    //    protected final Function <TC, List <TC>> defaultAction;
    protected Resolver <HtClause, HtClause> resolver;

    @Override
    public
    LogicCompilerObserver <HtPredicate, HtClause> getObserver () {
        return observer;
    }

    protected LogicCompilerObserver <HtPredicate, HtClause> observer;
    protected List <HtClause> preCompiledTarget;
    protected HtPrologParser parser;

    /**
     * {@inheritDoc}
     *
     * @param sentence
     */
    @Override
    public
    void compile ( Sentence <HtClause> sentence ) throws SourceCodeException {
    }

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param resolver
     */
    public
    HiTalkPreprocessor ( SymbolTable <Integer, String, Object> symbolTable,
                         VariableAndFunctorInterner interner,
                         HiTalkDefaultBuiltIn defaultBuiltIn,
                         Resolver <HtClause, HtClause> resolver,
                         HiTalkWAMCompiler compiler )
            throws LinkageException {

        super(symbolTable, interner, defaultBuiltIn, resolver, compiler);

        this.defaultBuiltIn = defaultBuiltIn;
//        this.builtInTransform = new HiTalkBuiltInTransform(defaultBuiltIn);
        this.resolver = resolver;

        defaultTransformer = new DefaultTransformer <>((HtClause) null);

//        int i = interner.internFunctorName(BEGIN_OF_FILE, 0);
//        Term target = new Functor(i, Atom.EMPTY_TERM_ARRAY);
//        defaultAction = new Function <TC, List <TC>>() {
//            DcgRuleExpander ruleExpander
//            @Override
//            public
//            List <TC> apply ( TC tc ) {
//                return null;
//            }
//        };
        if (preCompiledTarget != null) {
            for (HtClause t : preCompiledTarget) {
                resolver.setQuery(t);
                resolver.resolve();
            }
        }

        components.add((TT) new DefaultTermExpander(preCompiledTarget, defaultTransformer));
        components.add((TT) new HiLogPreprocessor <>(null, defaultTransformer, interner));
        components.add((TT) new StandardPreprocessor(null, preCompiledTarget, defaultTransformer));
    }

    /**
     * @param clauses
     */
    @Override
    protected
    void saveResult ( List <HtClause> clauses ) {
        preCompiledTarget = clauses;
    }

    /**
     * @param t
     * @return
     */
    protected
    List <HtClause> preprocess ( HtClause t ) {
        List <HtClause> list = new ArrayList <>();

        for (TransformTask <HtClause, TC> task : components) {
            list.addAll(task.invoke(t));
        }
        return list;
    }

    private
    void initialize () {

    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <HtClause, HtClause> observer ) {
//        this.observer = observer;
    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     */
    @Override
    public
    void endScope () {
    }

    /**
     * @param t
     */
//    @Override
    public
    void add ( TT t ) {
        components.add(t);
    }

    //    @Override
    public
    List <TT> getComponents () {
        return components;
    }

    private
    List <TC> apply ( TC tc ) {
        return Collections.singletonList(tc);
    }

    /**
     * @return
     */
    @Override
    public
    Logger getConsole () {
        return null;
    }

    /**
     * @return
     */
    @Override
    public
    HtPrologParser getParser () {
        return parser;
    }

    /**
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    /**
     * @param rule
     */
    @Override
    public
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    /**
     * @param query
     */
    @Override
    public
    void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    /**
     * @param clause
     */
    @Override
    public
    void compileClause ( HtClause clause ) {

    }

    /**
     * @param resolver
     */
    @Override
    public
    void setResolver ( Resolver <HtClause, HtClause> resolver ) {
        this.resolver = resolver;
    }
}
