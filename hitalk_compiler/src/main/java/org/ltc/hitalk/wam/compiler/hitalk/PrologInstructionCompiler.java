package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;

public class PrologInstructionCompiler<T, Q> extends BaseInstructionCompiler <T, Q> {
    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param parser
     */
    public PrologInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, PlPrologParser parser ) {
        super(symbolTable, interner, parser);
    }
}
