package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.isoprologparser.PrologParserTokenManager;
import com.thesett.aima.logic.fol.isoprologparser.SimpleCharStream;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.io.*;

import static org.ltc.hitalk.parser.HtPrologParserConstants.BOF;
import static org.ltc.hitalk.parser.HtPrologParserConstants.EOF;

/**
 *
 */
public
class HtTokenSource extends TokenSource {

    private int lineOfs;
    private int colOfs;
    private long fileBeginOffset;
    private HiTalkStream stream;

    /**
     * Builds a token source around the specified token manager.
     *
     * @param tokenManager The token manager to use to feed this source.
     * @param inputStream
     */
    public
    HtTokenSource ( PrologParserTokenManager tokenManager, SimpleCharStream inputStream ) {
        super(tokenManager);

        setOffset(0, 0);
        setFileBeginOffset(0);
        stream = new HiTalkStream(inputStream);
    }

    /**
     * Creates a token source on a string.
     *
     * @param stringToTokenize The string to tokenize.
     * @return A token source.
     */
    public static
    HtTokenSource getTokenSourceForString ( String stringToTokenize, int lineOfs ) {
        SimpleCharStream inputStream = new SimpleCharStream(new StringReader(stringToTokenize), 1, 1);
        PrologParserTokenManager tokenManager = new PrologParserTokenManager(inputStream);

        return new HtTokenSource(tokenManager, inputStream);
    }

    /**
     * Creates a token source on a file.
     *
     * @param file The file to tokenize.
     * @return A token source.
     * @throws FileNotFoundException If the file cannot be found.
     */
    public static
    HtTokenSource getTokenSourceForFile ( File file ) throws FileNotFoundException {
        Reader ins = new FileReader(file);
        SimpleCharStream inputStream = new SimpleCharStream(ins, 1, 1);
        PrologParserTokenManager tokenManager = new PrologParserTokenManager(inputStream);

        return new HtTokenSource(tokenManager, inputStream);
    }

    /**
     * Creates a token source on an input stream.
     *
     * @param in The input stream to tokenize.
     * @return A token source.
     */
    public static
    HtTokenSource getTokenSourceForInputStream ( InputStream in ) {
        SimpleCharStream inputStream = new SimpleCharStream(in, 1, 1);
        PrologParserTokenManager tokenManager = new PrologParserTokenManager(inputStream);

        return new HtTokenSource(tokenManager, inputStream);
    }

    /**
     * Retrieves and removes the head token, or <tt>null</tt> if there are no more tokens.
     *
     * @return The head token, or <tt>null</tt> if there are no more tokens.
     */
    @Override
    public
    Token poll () {
        if (token.next == null) {
            token.next = new Token();
            token.next.kind = BOF;
        }
        else {
            Token t = super.poll();
            if (t == null) {
                token.next = new Token();
                token.next.kind = EOF;
            }
        }
        token = token.next;
        return addOffset(token);
    }

    /**
     * {@inheritDoc}
     */
    public
    Token peek () {
        return addOffset(super.peek());
    }


    public
    void setOffset ( int lineOfs, int colOfs ) {
        this.lineOfs = lineOfs;
        this.colOfs = colOfs;
    }


    private
    Token addOffset ( Token token ) {
        if (token == null) {
            return null;
        }
        token.beginLine += lineOfs;
        token.endLine += lineOfs;
        token.beginColumn += colOfs;
        token.endColumn += colOfs;

        return token;
    }

    public
    long getFileBeginOffset () {
        return fileBeginOffset;
    }

    public
    void setFileBeginOffset ( long fileBeginOffset ) {
        this.fileBeginOffset = fileBeginOffset;
    }
}