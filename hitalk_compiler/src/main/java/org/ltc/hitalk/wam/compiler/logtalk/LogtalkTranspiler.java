package org.ltc.hitalk.wam.compiler.logtalk;

import org.ltc.hitalk.compiler.BaseCompiler;
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

import java.util.List;

/**
 * @param <M>
 * @param <P>
 * @param <Q>
 */
public class LogtalkTranspiler<M extends HtMethod, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery> extends BaseCompiler<M, P, Q, PC, QC> {

    public static final String CORE = "core.pl";

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     * @param observer
     */
    protected LogtalkTranspiler(ISymbolTable<Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                HtPrologParser parser,
                                ICompilerObserver<P, Q> observer) {
        super(symbolTable, interner, parser, observer);
    }

    @Override
    public void endScope() throws HtSourceCodeException {

    }

    /**
     * @param fnl
     * @throws Exception
     * @throws HtSourceCodeException
     */
    public List<M> compileFiles(List<String> fnl) throws Exception {
        return null;
    }

    public List<M> compileFiles(List<String> fnl, HtProperty... flags) {
        return null;
    }

    /**
     * @param fn
     * @param flags
     * @throws Exception
     */
    public List<M> compileFile(String fn, HtProperty... flags) throws Exception {
        return null;
    }

    /**
     * @param clause
     * @param flags
     * @throws HtSourceCodeException
     */
    public void compile(M clause, HtProperty... flags) throws HtSourceCodeException {

    }

    /**
     * @param query
     */
    public void compileQuery(Q query) throws HtSourceCodeException {

    }

    /**
     * @param resolver
     */
    public void setResolver(IResolver<PC, QC> resolver) {

    }

    @Override
    public void compile(M clause) {

    }

    @Override
    public void toString0(StringBuilder sb) {

    }

    /**
     * @param filename
     */
    public void consult(String filename) {

    }
}
