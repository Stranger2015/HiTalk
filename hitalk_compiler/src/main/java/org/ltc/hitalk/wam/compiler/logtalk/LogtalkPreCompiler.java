package org.ltc.hitalk.wam.compiler.logtalk;


import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.PrologBuiltInTransform;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;

public class LogtalkPreCompiler<T extends HtMethod, P, Q, PC, QC> extends PrologPreCompiler<T, P, Q, PC, QC> {
    /**
     * @param symbolTable
     * @param interner
     * @param defaultBuiltIn
     * @param builtInTransform
     * @param resolver
     * @param parser
     */
    public LogtalkPreCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                              IVafInterner interner,
                              PrologDefaultBuiltIn defaultBuiltIn,
                              PrologBuiltInTransform<T, P, Q, PC, QC> builtInTransform,
                              IResolver<PC, QC> resolver,
                              HtPrologParser parser) {
        super(symbolTable, interner, defaultBuiltIn, builtInTransform, resolver, parser);
    }
}
