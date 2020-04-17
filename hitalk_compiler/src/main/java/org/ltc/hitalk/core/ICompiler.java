package org.ltc.hitalk.core;

import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlLexer;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.hitalk.ILogicCompiler;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.ltc.hitalk.parser.PlLexer.getTokenSourceForIoFile;

/**
 * @param <P>
 * @param <Q>
 */
public interface ICompiler<T extends HtClause, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery>
        extends ILogicCompiler<T, P, Q> {

    HtProperty[] EMPTY_FLAG_ARRAY = new HtProperty[0];

    /**
     * @param fnl
     * @throws IOException
     * @throws HtSourceCodeException
     */
    default List<T> compileFiles(List<String> fnl) throws Exception {
        return compileFiles(fnl, EMPTY_FLAG_ARRAY);
    }

    default List<T> compileFiles(List<String> fnl, HtProperty... flags) throws Exception {
        List<T> list = new ArrayList<>();
        for (String fn : fnl) {
            list.addAll(compileFile(fn, flags));
        }

        return list;
    }

    /**
     * @param fn
     * @throws IOException
     */
    default List<T> compileFile(String fn, HtProperty... flags) throws Exception {
        return compile(getTokenSourceForIoFile(new File(fn)), flags);
    }

    /**
     * @param tokenSource
     * @param flags
     */
    List<T> compile(PlLexer tokenSource, HtProperty... flags) throws Exception;

    /**
     * @return
     */
    Logger getConsole();

    /**
     * @return
     */
    HtPrologParser getParser();

    /**
     * @param clause
     * @throws HtSourceCodeException
     */
    void compile(T clause, HtProperty... flags) throws HtSourceCodeException;

    /**
     * @param rule
     */
    void compileDcgRule(DcgRule rule) throws HtSourceCodeException;

    /**
     * @param query
     */
    void compileQuery(Q query) throws HtSourceCodeException;

    /**
     * @param resolver
     */
    void setResolver(IResolver<PC, QC> resolver);

    /**
     * @param fileName
     * @param flags
     * @throws Exception
     */
    List<T> compile(String fileName, HtProperty... flags) throws Exception;

    /**
     * @param clause
     * @throws HtSourceCodeException
     */
    void compile(T clause) throws Exception;
}
