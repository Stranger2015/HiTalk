package org.ltc.hitalk.wam.compiler.hitalk.logtalk;

import com.thesett.aima.logic.fol.IVafInterner;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

public class LogtalkCompiler extends BaseCompiler <HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

    /**
     *
     */
    protected final LogtalkTranspiler transpiler;
    protected final PrologWAMCompiler prologWAMCompiler;

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     * @param observer
     * @param transpiler
     * @param prologWAMCompiler
     */
    protected LogtalkCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                IVafInterner interner,
                                PlPrologParser parser,
                                LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer,
                                LogtalkTranspiler transpiler, PrologWAMCompiler prologWAMCompiler ) {
        super(symbolTable, interner, parser, observer);
        this.transpiler = transpiler;
        this.prologWAMCompiler = prologWAMCompiler;
    }

    @Override
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    @Override
    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    @Override
    public void compileClause ( HtClause clause ) {

    }

    @Override
    public void endScope () throws SourceCodeException {
        transpiler.endScope();

    }
}
