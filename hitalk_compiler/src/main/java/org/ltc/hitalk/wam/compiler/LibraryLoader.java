package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ltc.hitalk.term.io.Environment.instance;

/**
 *
 */
public class LibraryLoader<T extends HtClause, P, Q> extends BaseCompiler <T, P, Q> implements ICompiler <T, P, Q> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected final PlPrologParser parser = instance().getParser();
    private final ICompiler <T, P, Q> compiler;

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     */
    public LibraryLoader ( SymbolTable <Integer, String, Object> symbolTable,
                           IVafInterner interner,
                           PlPrologParser parser,
                           LogicCompilerObserver <P, Q> observer,
                           ICompiler <T, P, Q> compiler ) {
        super(symbolTable, interner, parser, observer);
        this.compiler = compiler;
    }

    /**
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    public void compile ( T clause, HtProperty... flags ) throws SourceCodeException {
        compiler.compile(clause, flags);
    }

    /**
     * @param rule
     * @throws SourceCodeException
     */
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {
        compiler.compileDcgRule(rule);
    }

    /**
     * @param query
     * @throws SourceCodeException
     */
    @Override
    public void compileQuery ( Q query ) throws SourceCodeException {
        compiler.compileQuery(query);
    }

    /**
     * @param resolver
     */
    @Override
    public void setResolver ( IResolver <P, Q> resolver ) {
        compiler.setResolver(resolver);
    }

    /**
     * @param clause
     */
    @Override
    public void compile ( T clause ) {
        compiler.compile(clause);
    }

    /**
     * @throws SourceCodeException
     */
    @Override
    public void endScope () throws SourceCodeException {
        compiler.endScope();
    }
}
