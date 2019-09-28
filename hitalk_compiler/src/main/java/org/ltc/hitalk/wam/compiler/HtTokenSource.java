
package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.isoprologparser.PrologParserTokenManager;
import com.thesett.aima.logic.fol.isoprologparser.SimpleCharStream;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.io.*;
import java.net.URI;
import java.nio.file.Path;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
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
    protected HiTalkStream stream;

    /**
     * Builds a token source around the specified token manager.
     *
     * @param tokenManager The token manager to use to feed this source.
     * @param inputStream
     */
    public
    HtTokenSource ( PrologParserTokenManager tokenManager, InputStream inputStream ) throws IOException {
        super(tokenManager);

        setOffset(0, 0);//fixme 1,1  ??????????
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
    HtTokenSource getTokenSourceForString ( String stringToTokenize, int lineOfs ) throws IOException {
        InputStream input = new ByteArrayInputStream(stringToTokenize.getBytes());
        return getTokenSourceForInputStream(input);
    }

    /**
     * Creates a token source on a file.
     *
     * @param path The file to tokenize.
     * @return A token source.
     */
    public static
    HtTokenSource getTokenSourceForPath ( Path path ) throws IOException {
//        InputStream input = Files.newInputStream(file);
//        return getTokenSourceForInputStream(input);
        FileSystemManager manager = VFS.getManager();
        File userDir = path.toAbsolutePath().toFile();
        URI url = userDir.toURI();
        FileObject file = manager.resolveFile(url);
        return getTokenSourceForVfsFileObject(file);
    }

    /**
     * Creates a token source on an input stream.
     *
     * @param in The input stream to tokenize.
     * @return A token source.
     */
    public static
    HtTokenSource getTokenSourceForInputStream ( InputStream in ) {
        try {
            BufferedInputStream input = new BufferedInputStream(in);
            SimpleCharStream inputStream = new SimpleCharStream(in, 1, 1);
            PrologParserTokenManager tokenManager = new PrologParserTokenManager(inputStream);
            return new HtTokenSource(tokenManager, input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }

    /**
     * @param vfsFo
     * @return
     * @throws IOException
     */
    public static
    HtTokenSource getTokenSourceForVfsFileObject ( FileObject vfsFo ) throws IOException {
        FileContent content = vfsFo.getContent();
        InputStream inputStream = content.getInputStream();

        return getTokenSourceForInputStream(inputStream);
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