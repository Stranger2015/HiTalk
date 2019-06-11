package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.ltc.hitalk.compiler.IApplication;
import org.ltc.hitalk.compiler.IConfig;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public
class HiLogCompiler extends BaseMachine implements LogicCompiler <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery>, IApplication {

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public
    HiLogCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
    }


    public static
    void main ( String[] args ) {
        HiLogCompiler compiler = new HiLogCompiler(new SymbolTableImpl <>(), new VariableAndFunctorInternerImpl("HiLog_Variable_Namespace", "HiLog_Functor_Namespace"));

        try {
            compiler.compileFile(args[0]);
        } catch (IOException | SourceCodeException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    private
    void compileFile ( String fn ) throws IOException, SourceCodeException {
        compile(TokenSource.getTokenSourceForFile(new File(fn)));
    }

    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param tokenSource The sentence to compile.
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    public
    void compile ( TokenSource tokenSource ) throws SourceCodeException {

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

    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer ) {

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
    IConfig getConfig () {
        return null;
    }

    /**
     *
     */
    @Override
    public
    void start () throws IOException {

    }

    /**
     * @return
     */
    @Override
    public
    int end () {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStarted () {
        return false;
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStopped () {
        return false;
    }

    /**
     *
     */
    @Override
    public
    void banner () {

    }
}
