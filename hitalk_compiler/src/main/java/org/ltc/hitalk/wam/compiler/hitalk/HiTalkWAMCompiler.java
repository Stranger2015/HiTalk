package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

public class HiTalkWAMCompiler extends PrologWAMCompiler {
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
                               PlPrologParser parser ) {
        super(symbolTable, interner, parser);
    }

    /**
     * @param query
     */
    @Override
    public void compileQuery ( HtClause query ) throws SourceCodeException {
        super.compileQuery(query);//todo
    }
}
