package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
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
    protected LogicCompilerObserver <Clause, Clause> observer;
    protected List <T> preCompiledTarget;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public
    HiTalkPreprocessor ( SymbolTable <Integer, String, Object> symbolTable,
                         VariableAndFunctorInterner interner,
                         HiTalkDefaultBuiltIn defaultBuiltIn ) {

        super(symbolTable, interner, defaultBuiltIn);

        this.defaultBuiltIn = defaultBuiltIn;
        this.builtInTransform = new HiTalkBuiltInTransform(defaultBuiltIn);

        defaultTransformer = new DefaultTransformer <>((T) null);

//        int i = interner.internFunctorName(BEGIN_OF_FILE, 0);
//        Term target = new Functor(i, Atom.EMPTY_TERM_ARRAY);
        defaultAction = null;
        components.add((TT) new DefaultTermExpander(preCompiledTarget, defaultTransformer));
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
