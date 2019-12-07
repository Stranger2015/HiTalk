package org.ltc.hitalk.wam.compiler.hilog;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

/**
 * @param <T>
 * @param <P>
 *
 * @param <Q>
 * @param <PC>
 * @param <QC>
 */
public class HiLogWAMCompiler<T extends HtClause, P, Q, PC, QC> extends PrologWAMCompiler <T, P, Q, PC, QC> {
    /**
     * Creates a base machine over the specified symbol table.
     *
     * @param symbolTable The symbol table for the machine.
     * @param interner    The interner for the machine.
     * @param parser
     * @param observer
     */
    public HiLogWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                              IVafInterner interner,
                              PlPrologParser parser,
                              LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }

    public HiLogWAMCompiler () {

    }
}
