package org.ltc.hitalk.wam.compiler.logtalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.hitalk.PrologInstructionCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;

public class LogtalkInstructionCompiler<T extends HtMethod, P, Q> extends PrologInstructionCompiler <T, P, Q> {
    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable    The symbol table for the machine.
     * @param interner       The interner for the machine.
     * @param defaultBuiltIn
     * @param observer
     * @param parser
     */
    public LogtalkInstructionCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                        IVafInterner interner,
                                        PrologDefaultBuiltIn defaultBuiltIn,
                                        LogicCompilerObserver <P, Q> observer,
                                        PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, observer, parser);
    }
}
