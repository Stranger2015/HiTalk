package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.term.io.TermIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;

/**
 *
 */
public class LibraryLoader<T extends HtClause, P extends HtPredicate, Q extends HtClause> extends BaseCompiler <T, P, Q> implements ICompiler <T, P, Q> {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    protected final PlPrologParser parser = TermIO.instance().getParser();
    protected final ICompiler <T, P, Q> compiler = TermIO.instance().getCompiler();

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     */
    public LibraryLoader ( SymbolTable <Integer, String, Object> symbolTable,
                           VariableAndFunctorInterner interner,
                           PlPrologParser parser,
                           LogicCompilerObserver <P, Q> observer ) {
        super(symbolTable, interner, parser, observer);
    }

    /**
     * @return
     */
//    public Logger getConsole () {
//        return logger;
//    }
//
    /**
     * @return
     */
//    public PlPrologParser getParser () {
//        return parser;
//    }

    /**
     * @param clause
     * @param flags
     * @throws SourceCodeException
     */
    public void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException {

    }

    /**
     * @param rule
     * @throws SourceCodeException
     */
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException, OperationNotSupportedException {
        throw new OperationNotSupportedException("DCG LIB LDR");
    }

    /**
     * @param query
     * @throws SourceCodeException
     */
    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    @Override
    public void compileClause ( HtClause clause ) {

    }

    /**
     * @throws SourceCodeException
     */
    public void endScope () throws SourceCodeException {

    }
}
