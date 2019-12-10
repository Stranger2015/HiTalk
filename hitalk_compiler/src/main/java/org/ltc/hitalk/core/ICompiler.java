package org.ltc.hitalk.core;

import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.ParseException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.wam.compiler.hitalk.ILogicCompiler;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * @param <P>
 * @param <Q>
 */
public interface ICompiler<T extends HtClause, P, Q> extends ILogicCompiler <T, P, Q> {

    HtProperty[] EMPTY_FLAG_ARRAY = new HtProperty[0];

    /**
     * @param fnl
     * @throws IOException
     * @throws SourceCodeException
     */
    default void compileFiles ( List <String> fnl ) throws IOException, SourceCodeException, ParseException {
        compileFiles(fnl, EMPTY_FLAG_ARRAY);
    }

    default void compileFiles ( List <String> fnl, HtProperty... flags ) throws IOException, SourceCodeException, ParseException {
        for (String fn : fnl) {
            compileFile(fn, flags);
        }
    }

    /**
     * @param fn
     * @throws IOException
     */
    default void compileFile ( String fn, HtProperty... flags ) throws IOException, SourceCodeException, ParseException {
        compile(PlTokenSource.getTokenSourceForIoFile(new File(fn)), flags);
    }

    /**
     * @param string
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    default void compileString ( String string, HtProperty... flags ) throws Exception {
        compile(PlTokenSource.getPlTokenSourceForString(string), flags);
    }

    /**
     * @param tokenSource
     * @param flags
     */
    void compile ( PlTokenSource tokenSource, HtProperty... flags ) throws IOException, SourceCodeException, ParseException;

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
    void compile ( T clause, HtProperty... flags ) throws SourceCodeException;

    /**
     * @param rule
     */
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException;

    /**
     * @param query
     */
    void compileQuery ( Q query ) throws SourceCodeException;

    /**
     * @param resolver
     */
    void setResolver ( IResolver <P, Q> resolver );

    void compile ( String fileName, HtProperty... flags ) throws IOException, SourceCodeException, ParseException;

    void compile ( T clause ) throws SourceCodeException;
}
