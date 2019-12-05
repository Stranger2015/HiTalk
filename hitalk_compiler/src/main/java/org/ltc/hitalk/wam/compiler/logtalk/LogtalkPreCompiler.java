package org.ltc.hitalk.wam.compiler.logtalk;

import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PrologBuiltInTransform;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;

public class LogtalkPreCompiler<T extends HtMethod, P, Q> extends PrologPreCompiler <T, P, Q> {
    /**
     * @param symbolTable
     * @param interner
     * @param defaultBuiltIn
     * @param builtInTransform
     * @param resolver
     * @param parser
     */
    public LogtalkPreCompiler ( SymbolTable <Integer, String, Object> symbolTable, IVafInterner interner, PrologDefaultBuiltIn defaultBuiltIn, PrologBuiltInTransform <T, P, Q> builtInTransform, IResolver <P, Q> resolver, PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }
}
