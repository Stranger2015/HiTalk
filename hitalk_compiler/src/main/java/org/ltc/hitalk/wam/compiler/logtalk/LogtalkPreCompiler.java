package org.ltc.hitalk.wam.compiler.logtalk;


import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.PrologBuiltInTransform;
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
    public LogtalkPreCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                PrologDefaultBuiltIn defaultBuiltIn,
                                PrologBuiltInTransform <T, P, Q> builtInTransform,
                                IResolver <HtPredicate, HtClause> resolver,
                                PlPrologParser parser ) {
        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }
}
