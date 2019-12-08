package org.ltc.hitalk.term.io;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

public class LogtalkWAMCompiler<T extends HtMethod, P, Q, PC, QC> extends PrologWAMCompiler <T, P, Q, PC, QC> {
    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param parser
     * @param observer
     */
    public LogtalkWAMCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                PlPrologParser parser,
                                LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }
}
