package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.wam.compiler.expander.DefaultTermExpander;
import org.ltc.hitalk.wam.task.HiLogPreprocessor;
import org.ltc.hitalk.wam.task.StandardPreprocessor;
import org.ltc.hitalk.wam.task.SuperCompiler;
import org.ltc.hitalk.wam.task.TransformTask;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;

import java.util.ArrayList;
import java.util.List;

import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 *
 */
public
class HiTalkPreprocessor<T extends Clause> extends HiTalkPreCompiler <T> {

    protected final DefaultTransformer <T> defaultTransformer;
    protected final HiTalkDefaultBuiltIn defaultBuiltIn;
    protected final HiTalkBuiltInTransform builtInTransform;
    protected final List <TransformTask <T>> components = new ArrayList <>();
    protected LogicCompilerObserver <Clause, Clause> observer;
    protected List <T> preCompiledTarget;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public
    HiTalkPreprocessor ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, HiTalkDefaultBuiltIn defaultBuiltIn ) {

        super(symbolTable, interner, defaultBuiltIn);

        this.defaultBuiltIn = defaultBuiltIn;
        this.builtInTransform = new HiTalkBuiltInTransform(defaultBuiltIn);

        defaultTransformer = new DefaultTransformer <>((T) null);

//        int i = interner.internFunctorName(BEGIN_OF_FILE, 0);
//        Term target = new Functor(i, Atom.EMPTY_TERM_ARRAY);
//
        Term expand_term = new Functor(interner.internFunctorName("expand_term", 0), EMPTY_TERM_ARRAY);
        Term expand_goal = new Functor(interner.internFunctorName("expand_goal", 0), EMPTY_TERM_ARRAY);
        Term term_expansion = new Functor(interner.internFunctorName("term_expansion", 0), EMPTY_TERM_ARRAY);
        Term goal_expansion = new Functor(interner.internFunctorName("goal_expansion", 0), EMPTY_TERM_ARRAY);

        components.add(new DefaultTermExpander(preCompiledTarget, defaultTransformer));
        components.add(new HiLogPreprocessor(defaultTransformer, interner));
        components.add(new StandardPreprocessor(preCompiledTarget, defaultTransformer));
        components.add(new SuperCompiler(preCompiledTarget, defaultTransformer));
    }

    /**
     * @param clauses
     */
    @Override
    protected
    void saveResult ( List <Clause> clauses ) {

    }

    /**
     * @param term
     * @return
     */
    @Override
    protected
    List <Term> preprocess ( Term term ) {
        return null;
    }

    /**
     * @param term
     * @return
     */
    protected
    List <T> preprocess ( T term ) {
        List <T> list = new ArrayList <>();
        for (TransformTask <T> task : components) {
            list.addAll(task.invoke(term));
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
    void setCompilerObserver ( LogicCompilerObserver <Clause, Clause> observer ) {
        this.observer = observer;
    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    @Override
    public
    void endScope () throws SourceCodeException {
    }

    /**
     * @param t
     */
//    @Override
    public
    void add ( TransformTask <T> t ) {
        components.add(t);
    }

    //    @Override
    public
    List <TransformTask <T>> getComponents () {
        return components;
    }
}
