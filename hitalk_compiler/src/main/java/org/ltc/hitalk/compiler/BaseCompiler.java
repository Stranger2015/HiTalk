package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public abstract
class BaseCompiler<T extends HtClause, P, Q>
        extends BaseMachine
        implements ICompiler <T, P, Q> {

    protected LogicCompilerObserver <P, Q> observer;
    protected HtPrologParser <T> parser;
//    protected Logger console;

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

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <P, Q> observer ) {
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
}
