package org.ltc.hitalk.core;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.wam.compiler.hitalk.ILogicCompiler;
import org.slf4j.Logger;

import javax.naming.OperationNotSupportedException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource.getPlTokenSourceForString;
import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource.getTokenSourceForIoFile;

/**
 * @param <P>
 * @param <Q>
 */
public interface ICompiler<T, P, Q> extends ILogicCompiler <T, P, Q> {

    HtProperty[] EMPTY_FLAG_ARRAY = new HtProperty[0];

    /**
     * @param fnl
     * @throws IOException
     * @throws SourceCodeException
     */
    default void compileFiles ( List <String> fnl ) throws IOException, SourceCodeException {
        compileFiles(fnl, EMPTY_FLAG_ARRAY);
    }

    default void compileFiles ( List <String> fnl, HtProperty... flags ) throws IOException, SourceCodeException {
        for (String fn : fnl) {
            compileFile(fn, flags);
        }
    }

    /**
     * @param fn
     * @throws IOException
     */
    default void compileFile ( String fn, HtProperty... flags ) throws IOException, SourceCodeException {
        compile(getTokenSourceForIoFile(new File(fn)), flags);
    }

    /**
     * @param string
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    default void compileString ( String string, HtProperty... flags ) throws Exception {
        compile(getPlTokenSourceForString(string), flags);
    }

    /**
     * @param tokenSource
     * @param flags
     */
    void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException;

    /**
     * @return
     */
    Logger getConsole ();

    /**
     * @return
     */
    PlPrologParser getParser ();

    /**
     * @param clause
     * @throws SourceCodeException
     */
    void compile ( HtClause clause, HtProperty... flags ) throws SourceCodeException;

    /**
     * @param rule
     */
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException, OperationNotSupportedException;

    /**
     * @param query
     */
    void compileQuery ( HtClause query ) throws SourceCodeException;

    /**
     * @param clause
     */
    void compileClause ( HtClause clause );

    /**
     * @param resolver
     */
    void setResolver ( Resolver <HtClause, Q> resolver );

    void compile ( String fileName, HtProperty... flags ) throws IOException, SourceCodeException;
}
