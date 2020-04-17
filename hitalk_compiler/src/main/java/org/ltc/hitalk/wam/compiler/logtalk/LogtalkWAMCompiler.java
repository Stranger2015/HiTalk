package org.ltc.hitalk.wam.compiler.logtalk;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 * @param <PC>
 * @param <QC>
 */
public class LogtalkWAMCompiler<T extends HtMethod, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery>
        extends PrologWAMCompiler<T, P, Q, PC, QC> {

    /**
     *
     */
    protected LogtalkTranspiler<T, P, Q, PC, QC> transpiler;
    protected LogtalkPreCompiler<T, P, Q, PC, QC> preCompiler;
    protected LogtalkInstructionCompiler<T, P, Q, PC, QC> instructionCompiler;

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     * @param observer
     * @param transpiler
     */
    public LogtalkWAMCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                              IVafInterner interner,
                              HtPrologParser parser,
                              ICompilerObserver<P, Q> observer,
                              LogtalkTranspiler<T, P, Q, PC, QC> transpiler) {
        super(symbolTable, interner, parser, observer);
        this.transpiler = transpiler;
    }

//    public LogtalkWAMCompiler() {
//
//    }

    /**
     * @param query
     */
//    public void compileQuery(HiTalkWAMCompiledQuery query) throws HtSourceCodeException {
//
//    }

    /**
     * @param resolver
     */
    @Override
    public void setResolver(IResolver<PC, QC> resolver) {
        this.resolver = resolver;
    }

    @Override
    public void endScope() throws HtSourceCodeException {
        transpiler.endScope();
    }

    /**
     * @param clause
     * @param flags
     * @throws HtSourceCodeException
     */
    public void compile(T clause, HtProperty... flags) throws HtSourceCodeException {

    }

    /**
     * @param query
     */
    public void compileQuery(Q query) {

    }

    @Override
    public void compile(T clause) {

    }
}
