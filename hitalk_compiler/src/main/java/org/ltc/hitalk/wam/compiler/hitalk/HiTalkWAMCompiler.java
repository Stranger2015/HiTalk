package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

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
                               VariableAndFunctorInterner interner,
                               PlPrologParser parser,
                               LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }

//    /**
//     * @param query
//     */
//    @Override
//    public void compileQuery ( HtClause query ) throws SourceCodeException {
//        super.compileQuery(query);//todo
//    }
}
