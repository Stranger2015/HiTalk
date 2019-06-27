package org.ltc.hitalk.wam.interpreter;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.entities.context.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static com.thesett.aima.logic.fol.isoprologparser.TokenSource.*;

public
interface ICompiler<S extends Clause> {

    HiTalkFlag[] EMPTY_FLAG_ARRAY = new HiTalkFlag[0];

    default
    void compileFile ( String fn ) throws IOException, SourceCodeException {
        compileFile(fn, EMPTY_FLAG_ARRAY);
    }

    default
    void compileFile ( List <String> fnl ) throws IOException, SourceCodeException {
        compileFile(fnl, EMPTY_FLAG_ARRAY);
    }

    default
    void compileFile ( List <String> fnl, HiTalkFlag... flags ) throws IOException, SourceCodeException {
        for (String fn : fnl) {
            compileFile(fn, flags);
        }
    }


    /**
     * @param fn
     * @throws IOException
     */
    default
    void compileFile ( String fn, HiTalkFlag... flags ) throws IOException, SourceCodeException {
        compile(getTokenSourceForFile(new File(fn)), flags);
    }

    /**
     * @param fn
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    default
    void compileString ( String fn, HiTalkFlag... flags ) throws IOException, SourceCodeException {
        compile(getTokenSourceForString(fn), flags);
    }

    /**
     * @param input
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    default
    void compileInputStream ( InputStream input, HiTalkFlag... flags ) throws IOException, SourceCodeException {
        compile((TokenSource) getTokenSourceForInputStream(input), flags);
    }

    /**
     * @param fn
     * @param flags
     * @throws IOException
     * @throws SourceCodeException
     */
    default
    void compileArchive ( String fn, ZipFile zipFile, HiTalkFlag... flags ) throws IOException, SourceCodeException {
        ZipEntry zipEntry = zipFile.getEntry(fn);
        InputStream input = zipFile.getInputStream(zipEntry);
        compileInputStream(input, flags);
    }

    /**
     * @param tokenSource
     * @param flags
     * @throws SourceCodeException
     */
    default
    void compile ( TokenSource tokenSource, Context context, HiTalkFlag... flags ) throws SourceCodeException {
        getParser().setTokenSource(tokenSource);
        try {
            while (true) {
                // Parse the next sentence or directive.
                Sentence <Clause> sentence = (Sentence <Clause>) getParser().parse().getT();

                getConsole().info(sentence.toString());
                compile((Sentence <S>) sentence);
            }
        } catch (Exception e) {
            getConsole().log(Level.SEVERE, e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * @return
     */
    Logger getConsole ();

    /**
     * @return
     */
    LogicCompiler <Clause, Clause, Clause> getPreCompiler ();

    /**
     * @return
     */
    Parser <S, Token> getParser ();

    /**
     * @param sentence
     * @throws SourceCodeException
     */
    void compile ( Sentence <S> sentence ) throws SourceCodeException;
}
