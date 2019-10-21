package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
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
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.term.io.TermIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 */
public class LibraryLoader extends BaseCompiler <HtPredicate, HtClause> implements ICompiler <HtPredicate, HtClause> {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());
    protected final PlPrologParser parser = TermIO.instance().getParser();

    /**
     * @param symbolTable
     * @param interner
     * @param parser
     */
    public LibraryLoader ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, PlPrologParser parser ) {
        super(symbolTable, interner, parser);
    }

    /**
     * @return
     */
    public Logger getConsole () {
        return logger;
    }

    /**
     * @return
     */
    public PlPrologParser getParser () {
        return parser;
    }

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
    public void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    /**
     * @param query
     * @throws SourceCodeException
     */
    public void compileQuery ( HtClause query ) throws SourceCodeException {

    }

    /**
     * @param clause
     */
    public void compileClause ( HtClause clause ) {

    }


    /**
     * @param fileName
     * @param flags
     * @throws IOException
     */
    public void compile ( String fileName, HtProperty[] flags ) throws IOException {

    }

    public void compile ( Sentence sentence ) throws SourceCodeException {

    }

    public void setCompilerObserver ( LogicCompilerObserver observer ) {

    }

    public void endScope () throws SourceCodeException {

    }

    /**
     * @param tokenSource
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    public void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException {
        getConsole().info("Compiling " + tokenSource.getPath() + "... ");
        parser.setTokenSource(tokenSource);
        while (true) {
            Term t = parser.next();
            if (t == null) {
                break;
            }
            HtClause c = parser.convert(t);
            compile(c, flags);
        }
    }
}
