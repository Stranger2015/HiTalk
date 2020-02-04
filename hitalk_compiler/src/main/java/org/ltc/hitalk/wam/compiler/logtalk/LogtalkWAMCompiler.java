package org.ltc.hitalk.wam.compiler.logtalk;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

public class LogtalkWAMCompiler<T extends HtMethod, P, Q, PC, QC> extends PrologWAMCompiler <T, P, Q, PC, QC> {

    /**
     *
     */
    protected LogtalkTranspiler <T, P, Q> transpiler;
    protected LogtalkPreCompiler <T, P, Q> preCompiler;
    protected LogtalkInstructionCompiler <T, PC, QC> instructionCompiler;

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     * @param observer
     * @param transpiler
     */
    public LogtalkWAMCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                              IVafInterner interner,
                              PlPrologParser parser,
                              ICompilerObserver<P, Q> observer,
                              LogtalkTranspiler<T, P, Q> transpiler) {
        super(symbolTable, interner, parser, observer);
        this.transpiler = transpiler;
    }

    public LogtalkWAMCompiler () {

    }

    /**
     * @param query
     */
    public void compileQuery ( HiTalkWAMCompiledQuery query ) throws HtSourceCodeException {

    }

    /**
     * @param resolver
     */
    public void setResolver ( IResolver <P, Q> resolver ) {
        this.resolver = resolver;
    }

    @Override
    public void endScope () throws HtSourceCodeException {
        transpiler.endScope();
    }

    /**
     * @param clause
     * @param flags
     * @throws HtSourceCodeException
     */
    public void compile ( T clause, HtProperty... flags ) throws HtSourceCodeException {

    }

    /**
     * @param query
     */
    public void compileQuery ( Q query ) {

    }

    @Override
    public void compile ( HtClause clause ) {

    }
}
