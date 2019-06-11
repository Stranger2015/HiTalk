package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IComposite;
import org.ltc.hitalk.wam.compiler.expander.DefaultTermExpander;
import org.ltc.hitalk.wam.interpreter.HiTalkInterpreter;
import org.ltc.hitalk.wam.interpreter.Mode;
import org.ltc.hitalk.wam.task.*;
import org.ltc.hitalk.wam.transformers.DefaultTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public
class HiTalkCompilerPreprocessor extends BaseMachine implements LogicCompiler <Clause, Clause, Clause>, IComposite <Term, TransformTask <Term>> {

    protected final DefaultTransformer defaultTransformer;
    protected final DefaultTermExpander defaultExpander;

    //    protected final IApplication app;
    protected final HiTalkDefaultBuiltIn defaultBuiltIn;
    protected final HiTalkBuiltInTransform builtInTransform;
    protected final List <TransformTask <Term>> components = new ArrayList <>();
    protected final LogicCompilerObserver <Clause, Clause> observer;


    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public
    HiTalkCompilerPreprocessor ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, HiTalkDefaultBuiltIn defaultBuiltIn ) {

        super(symbolTable, interner);

        this.defaultBuiltIn = defaultBuiltIn;
        this.builtInTransform = new HiTalkBuiltInTransform(defaultBuiltIn);

        defaultTransformer = new DefaultTransformer(null);
        defaultExpander = new DefaultTermExpander();

        //
        components.add(new TermRewriteTask(defaultExpander));
        components.add(new HiLogPreprocessor(defaultTransformer, interner));
        components.add(new StandardPreprocessor(null, defaultTransformer));
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
            executeQuery(clause);//directivei
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

    /*
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */

//
//        enum MessaqeKind {
//            BANNER,
//            COMMENT(
//
//            );
//
//
//
//            MessaqeKind () {
//
//            }
//
//            enum Subkind {
//                SETTINGS,
//                HELP
//            }
//
//
//
//


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

    @Override
    public
    List <TransformTask <Term>> getComponents () {
        return components;
    }
}
