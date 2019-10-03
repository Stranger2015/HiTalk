
package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.isoprologparser.PrologParserTokenManager;
import com.thesett.aima.logic.fol.isoprologparser.SimpleCharStream;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.ltc.hitalk.parser.PrologAtoms;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;

import static org.ltc.hitalk.parser.HtPrologParserConstants.BOF;
import static org.ltc.hitalk.parser.HtPrologParserConstants.EOF;

/**
 *
 */
public
class HtTokenSource extends TokenSource implements PropertyChangeListener {
    //    private BufferedInputStream input;//implements PropertyChangeListener {
    private String path;

    private int lineOfs;
    private int colOfs;
    private long fileBeginOffset;

    public
    HiTalkStream getStream () {
        return stream;
    }

    public
    void setStream ( HiTalkStream stream ) {
        this.stream = stream;
    }

    protected HiTalkStream stream;

    protected
    void onEncodingChanged ( String encoding ) {
        fileBeginOffset = -1;
        lineOfs = -1;
        colOfs = -1;
    }
    /**
     * Builds a token source around the specified token manager.
     *
     * @param tokenManager The token manager to use to feed this source.
     * @param inputStream
     */
    public
    HtTokenSource ( PrologParserTokenManager tokenManager, InputStream inputStream ) {
        super(tokenManager);
        stream = new HiTalkStream(inputStream, this);
    }

    public
    HtTokenSource ( PrologParserTokenManager tokenManager, BufferedInputStream input, String path ) throws IOException {
        this(tokenManager, input);
        this.path = path;
    }

    /**
     * Creates a token source on a string.
     *
     * @param stringToTokenize The string to tokenize.
     * @return A token source.
     */
    public static HtTokenSource getHtTokenSourceForString ( String stringToTokenize, int lineOfs ) throws IOException {
        InputStream input = new ByteArrayInputStream(stringToTokenize.getBytes());
        return getTokenSourceForInputStream(input, "");//fixme
    }

    public static
    HtTokenSource getTokenSourceForIoFile ( File file ) throws IOException {
        return getTokenSourceForUri(file.toURI());
    }

    private static
    HtTokenSource getTokenSourceForUri ( URI uri ) throws IOException {
        FileSystemManager manager = VFS.getManager();
        FileObject file = manager.resolveFile(uri);

        return getTokenSourceForVfsFileObject(file);
    }

    /**
     * Creates a token source on a file.
     *
     * @param path The file to tokenize.
     * @return A token source.
     */
    public static
    HtTokenSource getTokenSourceForPath ( Path path ) throws IOException {
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
     * @param path
     * @return A token source.
     */
    public static
    HtTokenSource getTokenSourceForInputStream ( InputStream in, String path ) throws IOException {
        BufferedInputStream input = new BufferedInputStream(in);
        SimpleCharStream inputStream = new SimpleCharStream(in, 1, 1);
        PrologParserTokenManager tokenManager = new PrologParserTokenManager(inputStream);

        return new HtTokenSource(tokenManager, input, path);
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

        return getTokenSourceForInputStream(inputStream, vfsFo.getName().getPath());
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


    /**
     * @param lineOfs
     * @param colOfs
     */
    public
    void setOffset ( int lineOfs, int colOfs ) {
        this.lineOfs = lineOfs;
        this.colOfs = colOfs;
    }


    /**
     * @param token
     * @return
     */
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

    /**
     * @return
     */
    public
    long getFileBeginOffset () {
        return fileBeginOffset;
    }

    /**
     * @param fileBeginOffset
     */
    public
    void setFileBeginOffset ( long fileBeginOffset ) {
        this.fileBeginOffset = fileBeginOffset;
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param event A PropertyChangeEvent object describing the event source
     *              and the property that has changed.
     */
    @Override
    public
    void propertyChange ( PropertyChangeEvent event ) {
        if (PrologAtoms.ENCODING.equals(event.getPropertyName())) {
            Term value = (Term) event.getNewValue();
        }
    }

    /**
     * @return
     */
    public
    String getPath () {
        return path;
    }
}