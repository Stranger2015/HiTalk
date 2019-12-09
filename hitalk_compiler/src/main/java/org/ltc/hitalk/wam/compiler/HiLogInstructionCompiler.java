package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.hitalk.PrologInstructionCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;

public class HiLogInstructionCompiler<T extends HtClause, P, Q> extends PrologInstructionCompiler <T, P, Q> {
    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable    The symbol table for the machine.
     * @param interner       The interner for the machine.
     * @param defaultBuiltIn
     * @param observer
     * @param parser
     */
    public HiLogInstructionCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
                                      IVafInterner interner,
                                      PrologDefaultBuiltIn defaultBuiltIn,
                                      LogicCompilerObserver <P, Q> observer,
                                      PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, observer, parser);
    }

    public HiLogInstructionCompiler () {
    }
}
