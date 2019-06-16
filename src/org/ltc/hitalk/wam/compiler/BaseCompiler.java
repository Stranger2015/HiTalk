package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.wam.interpreter.ICompiler;
import org.ltc.wam.hitalk.parser.HtPrologParser;

import java.util.logging.Logger;

public abstract
class BaseCompiler<T extends Clause, T1, T2> extends BaseMachine implements LogicCompiler <T, T1, T2>, ICompiler <T> {
    protected LogicCompilerObserver <T1, T2> observer;
    protected HtPrologParser parser;
    protected Logger console;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public
    BaseCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
    }


//    /**
//     * @param tokenSource
//     */
//    public
//    void compile ( TokenSource tokenSource ) throws SourceCodeException {
//        ICompiler.super.compile( tokenSource );
//
//    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <T1, T2> observer ) {
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
    Logger getConsole () {
        return console;
    }
//
//    @Override
//    public
//    HiTalkCompilerPreprocessor getPreCompiler () {
//        return co
//    }
}
