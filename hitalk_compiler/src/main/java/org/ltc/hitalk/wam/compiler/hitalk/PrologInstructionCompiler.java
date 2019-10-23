package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.BaseInstructionCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;

public class PrologInstructionCompiler<T, Q> extends BaseInstructionCompiler <T, Q> {
    private PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param parser
     */
    public PrologInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                       VariableAndFunctorInterner interner,
                                       PrologDefaultBuiltIn defaultBuiltIn,
                                       PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, parser);
//        this.defaultBuiltIn = defaultBuiltIn;
    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }
}
