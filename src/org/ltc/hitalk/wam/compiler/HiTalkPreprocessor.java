package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.wam.compiler.expander.DefaultTermExpander;
import org.ltc.hitalk.wam.interpreter.HiTalkInterpreter;
import org.ltc.hitalk.wam.interpreter.Mode;
import org.ltc.hitalk.wam.task.HiLogPreprocessor;
import org.ltc.hitalk.wam.task.StandardPreprocessor;
import org.ltc.hitalk.wam.task.SuperCompiler;
import org.ltc.hitalk.wam.task.TransformTask;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;

import java.util.ArrayList;
import java.util.List;

import static org.ltc.hitalk.parser.HtPrologParser.BEGIN_OF_FILE;

/**
 *
 */
public
class HiTalkPreprocessor extends HiTalkPreCompiler {

    protected final DefaultTransformer defaultTransformer;

    //    protected final IApplication app;
    protected final HiTalkDefaultBuiltIn defaultBuiltIn;
    protected final HiTalkBuiltInTransform builtInTransform;
    protected final List <TransformTask <Term>> components = new ArrayList <>();
    protected LogicCompilerObserver <Clause, Clause> observer;


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

        defaultTransformer = new DefaultTransformer <>(null);

        int i = interner.internFunctorName(BEGIN_OF_FILE, 0);
        Term target = new Functor(i, new Term[0]);
        components.add(new DefaultTermExpander(target, defaultTransformer));
        components.add(new HiLogPreprocessor(defaultTransformer, interner));
        components.add(new StandardPreprocessor <Term>(null, defaultTransformer));
        components.add(new SuperCompiler(null, defaultTransformer));
    }

    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param sentence The sentence to compile.
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    @Override
    public
    void compile ( Sentence <Clause> sentence ) throws SourceCodeException {
        process(sentence.getT());
    }

    /**
     * @param clause
     * @throws SourceCodeException
     */
    public
    void process ( Clause clause ) throws SourceCodeException {
        if (clause.isQuery()) {
            endScope();
            executeQuery(clause);//directivei compileQuery
            //preprocess
        }
        else {
            doProcess(clause);
        }
    }

    private
    void doProcess ( Clause clause ) {

    }

    protected
    void executeQuery ( Clause clause ) {
        HiTalkInterpreter interpreter = new HiTalkInterpreter(Mode.ProgramMultiLine);
        interpreter.getMode();
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

//    @Override
//    public
//    LogicCompiler <Clause, Clause, Clause> getPreCompiler () {
//        return this;
//    }
//
//    @Override
//    public
//   HtPrologParser getParser () {
//        return null;
//    }

    /**
     * @param t
     */
//    @Override
    public
    void add ( TransformTask <Term> t ) {

    }

    //    @Override
    public
    List <TransformTask <Term>> getComponents () {
        return null;
    }
}
