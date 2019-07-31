package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.wam.compiler.expander.DefaultTermExpander;
import org.ltc.hitalk.wam.task.HiLogPreprocessor;
import org.ltc.hitalk.wam.task.StandardPreprocessor;
import org.ltc.hitalk.wam.task.TransformTask;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 *
 */
public
class HiTalkPreprocessor<T extends Clause, TC extends Term, TT extends TransformTask <T, TC>>
        extends HiTalkPreCompiler <T> {

    protected final DefaultTransformer <T, TC> defaultTransformer;
    protected final HiTalkDefaultBuiltIn defaultBuiltIn;
    protected final HiTalkBuiltInTransform builtInTransform;
    protected final List <TT> components = new ArrayList <>();
    protected final Function <TC, List <TC>> defaultAction;
    protected final Resolver <T, T> resolver;
    protected LogicCompilerObserver <Clause, Clause> observer;
    protected List <T> preCompiledTarget;

    /**
     * Creates a base machine over the specified symbol table.
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param resolver
     */
    public
    HiTalkPreprocessor ( SymbolTable <Integer, String, Object> symbolTable,
                         VariableAndFunctorInterner interner,
                         HiTalkDefaultBuiltIn defaultBuiltIn, Resolver <T, T> resolver ) throws LinkageException {

        super(symbolTable, interner, defaultBuiltIn);

        this.defaultBuiltIn = defaultBuiltIn;
        this.builtInTransform = new HiTalkBuiltInTransform(defaultBuiltIn);
        this.resolver = resolver;

        defaultTransformer = new DefaultTransformer <>((T) null);

//        int i = interner.internFunctorName(BEGIN_OF_FILE, 0);
//        Term target = new Functor(i, Atom.EMPTY_TERM_ARRAY);
        defaultAction = null;
        for (T t : preCompiledTarget) {
            resolver.setQuery(t);
            resolver.resolve();
        }


        components.add((TT) new DefaultTermExpander(defaultAction, preCompiledTarget, defaultTransformer));
        components.add((TT) new HiLogPreprocessor(defaultAction, defaultTransformer, interner));
        components.add((TT) new StandardPreprocessor(defaultAction, preCompiledTarget, defaultTransformer));
//        components.add(new SuperCompiler(preCompiledTarget, defaultTransformer));

    }

    /**
     *
     * @param clauses
     */
    @Override
    protected
    void saveResult ( List <T> clauses ) {
        preCompiledTarget = clauses;
    }

    /**
     *
     * @param t
     * @return
     */
    protected
    List <T> preprocess ( T t ) {
        List <T> list = new ArrayList <>();

        for (TransformTask <T, TC> task : components) {
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
    void setCompilerObserver ( LogicCompilerObserver <Clause, Clause> observer ) {
        this.observer = observer;
    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
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
}
