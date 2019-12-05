package org.ltc.hitalk.wam.compiler.logtalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;

public class LogtalkTranspiler<M extends HtMethod, P, Q> extends BaseCompiler <M, P, Q> {
    /**
     * @param symbolTable
     * @param interner
     * @param parser
     * @param observer
     */
    protected LogtalkTranspiler ( SymbolTable <Integer, String, Object> symbolTable,
                                  IVafInterner interner,
                                  PlPrologParser parser,
                                  LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }

    @Override
    public void endScope () throws SourceCodeException {

    }

    /**
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    public void compile ( M clause, HtProperty... flags ) throws SourceCodeException {

    }

    /**
     * @param query
     */
    public void compileQuery ( Q query ) throws SourceCodeException {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <P, Q> resolver ) {

    }

    public void compile ( M clause ) {

    }
}
