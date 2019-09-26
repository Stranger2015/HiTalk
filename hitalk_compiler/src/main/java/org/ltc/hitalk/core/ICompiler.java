package org.ltc.hitalk.core;

import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HtTokenSource;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.thesett.aima.logic.fol.isoprologparser.TokenSource.getTokenSourceForFile;
import static com.thesett.aima.logic.fol.isoprologparser.TokenSource.getTokenSourceForString;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public
interface ICompiler<T extends HtClause, P, Q> extends LogicCompiler <T, P, Q> {

    Flag[] EMPTY_FLAG_ARRAY = new Flag[0];

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
    void compileFiles ( List <String> fnl, Flag... flags ) throws IOException, SourceCodeException {
        for (String fn : fnl) {
            compileFile(fn, flags);
        }
    }

    /**
     * @param fn
     * @throws IOException
     */
    default
    void compileFile ( String fn, Flag... flags ) throws IOException, SourceCodeException {
        compile((HtTokenSource) getTokenSourceForFile(new File(fn)), flags);
    }

    /**
     * @param fn
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    default
    void compileString ( String fn, Flag... flags ) throws IOException, SourceCodeException {
        compile((HtTokenSource) getTokenSourceForString(fn), flags);
    }

//    default
//    void compileInputStream ( InputStream input, Flag... flags ) throws IOException, SourceCodeException {
//        compile((HtTokenSource) getTokenSourceForInputStream(input), flags);
//    }

//    /**
//     * @param fn
//     * @param flags
//     * @throws IOException
//     * @throws SourceCodeException
//     */
//    default
//    void compileZipArchive ( String fn, ZipFile zipFile, Flag... flags ) throws IOException, SourceCodeException {
//        ZipEntry zipEntry = zipFile.getEntry(fn);
//        InputStream input = zipFile.getInputStream(zipEntry);
//        compileInputStream(input, flags);
//    }

    /**
     * @param tokenSource
     * @param flags
     */
    default
    void compile ( HtTokenSource tokenSource, Flag... flags ) {
        getConsole().info("Compiling" + tokenSource);
        getParser().setTokenSource(tokenSource);
        try {
            while (true) {
                // Parse the next sentence or directive.
                Sentence <T> sentence = getParser().parse();

                getConsole().info(sentence.toString());
                compile(sentence.getT(), flags);
            }
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
    HtPrologParser <T> getParser ();

    /**
     * @param clause
     * @throws SourceCodeException
     */
    void compile ( T clause, Flag... flags ) throws SourceCodeException;

    /**
     * @param rule
     */
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException;

    /**
     * @param query
     */
    void compileQuery ( Q query ) throws SourceCodeException;

    /**
     * @param clause
     */
    void compileClause ( T clause );

    /**
     * @param resolver
     */
    void setResolver ( Resolver <T, Q> resolver );
}
