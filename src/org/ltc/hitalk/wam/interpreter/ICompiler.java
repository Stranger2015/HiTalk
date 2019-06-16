package org.ltc.hitalk.wam.interpreter;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.thesett.aima.logic.fol.isoprologparser.TokenSource.*;

public
interface ICompiler<S extends Clause> {
    /**
     * @param fn
     * @throws IOException
     */
    default
    void compileFile ( String fn ) throws IOException, SourceCodeException {
        compile(getTokenSourceForFile(new File(fn)));
    }


    default
    void compileString ( String s ) throws SourceCodeException {
        compile(getTokenSourceForString(s));
    }

    /**
     * @param input
     */
    default
    void compileInputStream ( InputStream input ) throws SourceCodeException {
        compile((TokenSource) getTokenSourceForInputStream(input));
    }

    /**
     * @param tokenSource
     */
    default
    void compile ( TokenSource tokenSource ) throws SourceCodeException {
        getParser().setTokenSource(tokenSource);
        try {
            while (true) {
                // Parse the next sentence or directive.
                Sentence <Clause> sentence = (Sentence <Clause>) getParser().parse().getT();

                getConsole().info(sentence.toString());
                getPreCompiler().compile(sentence);
            }
        } catch (Exception e) {
            getConsole().log(Level.SEVERE, e.getMessage(), e);
            System.exit(1);
        }
    }

    Logger getConsole ();

    LogicCompiler <Clause, Clause, Clause> getPreCompiler ();

    Parser <S, Token> getParser ();

//    void compile ( Sentence <S> sentence );
}
