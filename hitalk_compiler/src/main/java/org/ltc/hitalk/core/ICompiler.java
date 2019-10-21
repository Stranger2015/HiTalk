package org.ltc.hitalk.core;


import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @param <P>
 * @param <Q>
 */
public interface ICompiler<P, Q> extends LogicCompiler <HtClause, P, Q> {

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
        compile(PlTokenSource.getTokenSourceForIoFile(new File(fn)), flags);
    }

    /**
     * @param fn
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    default void compileString ( String fn, HtProperty... flags ) throws Exception {
//        compile(PlTokenSource.getPlTokenSourceForString(fn, 1), flags);
    }

    /**
     *
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
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException;

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

    void compile ( String fileName, HtProperty[] flags ) throws IOException, SourceCodeException;
}
