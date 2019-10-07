package org.ltc.hitalk.core;

import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HtTokenSource;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

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
    default
    void compileFiles ( List <String> fnl ) throws IOException, SourceCodeException {
        compileFiles(fnl, EMPTY_FLAG_ARRAY);
    }

    default
    void compileFiles ( List <String> fnl, HtProperty... flags ) throws IOException, SourceCodeException {
        for (String fn : fnl) {
            compileFile(fn, flags);
        }
    }

    /**
     * @param fn
     * @throws IOException
     */
    default
    void compileFile ( String fn, HtProperty... flags ) throws IOException, SourceCodeException {
        compile(HtTokenSource.getTokenSourceForIoFile(new File(fn)), flags);
    }

    /**
     * @param fn
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    default
    void compileString ( String fn, HtProperty... flags ) throws Exception {
        compile(HtTokenSource.getHtTokenSourceForString(fn, -1), flags);
    }

    /**
     * @param tokenSource
     * @param flags
     */
    default
    void compile ( HtTokenSource tokenSource, HtProperty... flags ) {
        getConsole().info("Compiling " + tokenSource);
        getParser().setTokenSource(tokenSource);
        try {
//            while (true) {
//                // Parse the next sentence or directive.
//                Sentence <HtClause> sentence = getParser().parse();
//
//                getConsole().info(sentence.toString());
//                compile(sentence.getT(), flags);
//            }
            List <HtClause> sentences = getParser().sentences();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }

    /**
     * @return
     */
    Logger getConsole ();

    /**
     * @return
     */
    HtPrologParser getParser ();

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
}
