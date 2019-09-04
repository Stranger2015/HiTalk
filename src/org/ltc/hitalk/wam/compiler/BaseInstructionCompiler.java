package org.ltc.hitalk.wam.compiler;


import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.task.CompilerTask;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @param <T1>
 * @param <T2>
 */
public abstract
class BaseInstructionCompiler<T1, T2> extends BaseCompiler <HtClause, T1, T2> {

    public final Deque <CompilerTask <HtClause, Term>> tasks = new ArrayDeque <>();

    /**
     * Creates a base machine over the specified symbol table.
     *!
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     */
    public
    BaseInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                              VariableAndFunctorInterner interner ) {
        super(symbolTable, interner);
    }

    /**
     * @return
     */
    public
    Deque <CompilerTask <HtClause, Term>> getTasks () {
        return tasks;
    }
}
