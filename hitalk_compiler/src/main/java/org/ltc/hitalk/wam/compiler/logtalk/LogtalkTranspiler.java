package org.ltc.hitalk.wam.compiler.logtalk;

import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;

public class LogtalkTranspiler<M extends HtMethod, P, Q> extends BaseCompiler <M, P, Q> {

    public static final String CORE = "core.pl";

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     * @param observer
     */
    protected LogtalkTranspiler ( ISymbolTable <Integer, String, Object> symbolTable,
                                  IVafInterner interner,
                                  PlPrologParser parser,
                                  ICompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }

    @Override
    public void endScope () throws HtSourceCodeException {

    }

    /**
     * @param clause
     * @param flags
     * @throws HtSourceCodeException
     */
    public void compile ( M clause, HtProperty... flags ) throws HtSourceCodeException {

    }

    /**
     * @param query
     */
    public void compileQuery ( Q query ) throws HtSourceCodeException {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <P, Q> resolver ) {

    }

    public void compile ( M clause ) {

    }

    public void toString0 ( StringBuilder sb ) {

    }
}
