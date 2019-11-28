package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public class HiTalkWAMCompiler<T extends HtClause, P, Q> extends PrologWAMCompiler <T, P, Q> {
    /**
     *
     */
    public enum ImplPolicy {
        NATIVE_LOGTALK,   //using specializer
        PROLOG_CONVERSION,//using specializer
        META_INTERPRETATION,
        PROLOG_MODELLING,//sicstus
        WAM_EXTENSION,
        ;
    }

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     */
    public HiTalkWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                               IVafInterner interner,
                               PlPrologParser parser,
                               LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }
}
