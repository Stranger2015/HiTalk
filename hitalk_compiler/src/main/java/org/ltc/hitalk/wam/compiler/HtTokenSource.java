package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.isoprologparser.SimpleCharStream;
import com.thesett.common.util.Source;
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
import static org.ltc.hitalk.parser.HtPrologParserConstants.PERIOD;

/**
 *
 */
public class HtTokenSource implements Source <HtToken>, PropertyChangeListener {

    /**
     * Holds the current token.
     */
    public HtToken token;

    /**
     * Holds the tokenizer that supplies the next token on demand.
     */
    public HtPrologParserTokenManager tokenManager;

    /**
     * Builds a token source around the specified token manager.
     *
     * @param tokenManager The token manager to use to feed this source.
     */
    protected HtTokenSource ( HtPrologParserTokenManager tokenManager ) {
        this.tokenManager = tokenManager;

        // The first token is initialized to be empty, so that the first call to poll returns the first token.
        token = new HtToken();
    }

    private boolean isBofGenerated;
    private int lineOfs;
    private int colOfs;
    private boolean encodingChanged;
    private String encoding;

    public boolean isEofGenerated () {
        return isEofGenerated;
    }

    private boolean isEofGenerated;

    public boolean isBofGenerated () {
        return isBofGenerated;
    }

    public void setBofGenerated ( boolean bofGenerated ) {
        isBofGenerated = bofGenerated;
    }

    private boolean isEncodingChanged;
    private long fileBeginOffset = 0L;

    protected HiTalkStream stream;
    private InputStream input;
    private String path;

    protected void onEncodingChanged ( String encoding ) throws IOException {
        if (token.kind == PERIOD) {
            fileBeginOffset = stream.position();
            setBofGenerated(false);
            setEncodingChanged(true);
        }
    }

    /**
     * Builds a token source around the specified token manager.
     *
     * @param tokenManager The token manager to use to feed this source.
     * @param input
     */
    public HtTokenSource ( HtPrologParserTokenManager tokenManager, InputStream input ) {
        this(tokenManager);
        stream = new HiTalkStream(input, this);
        this.tokenManager = tokenManager;

    }

    public HtTokenSource ( HtPrologParserTokenManager tokenManager, InputStream input, String path ) {
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

    public static HtTokenSource getTokenSourceForIoFile ( File file ) throws IOException {
        return getTokenSourceForUri(file.toURI());
    }

    private static HtTokenSource getTokenSourceForUri ( URI uri ) throws IOException {
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
    public static HtTokenSource getTokenSourceForPath ( Path path ) throws IOException {
        FileSystemManager manager = VFS.getManager();
        File userDir = path.toAbsolutePath().toFile();
        URI url = userDir.toURI();
        FileObject file = manager.resolveFile(url);

        return getTokenSourceForVfsFileObject(file);
    }

    /**
     * Creates a token source on an input stream.
     *
     * @param in   The input stream to tokenize.
     * @param path
     * @return A token source.
     */
    public static HtTokenSource getTokenSourceForInputStream ( InputStream in, String path ) throws IOException {
        BufferedInputStream input = new BufferedInputStream(in);
        SimpleCharStream inputStream = new SimpleCharStream(input, 1, 1);
        HtPrologParserTokenManager tokenManager = new HtPrologParserTokenManager(inputStream);

        return new HtTokenSource(tokenManager, in, path);
    }

    /**
     * @param vfsFo
     * @return
     * @throws IOException
     */
    public static HtTokenSource getTokenSourceForVfsFileObject ( FileObject vfsFo ) throws IOException {
        FileContent content = vfsFo.getContent();
        InputStream inputStream = content.getInputStream();

        return getTokenSourceForInputStream(inputStream, vfsFo.getName().getPath());
    }

    /**
     * @return
     */
    public long getFileBeginOffset () {
        return fileBeginOffset;
    }

    /**
     * @param fileBeginOffset
     */
    public void setFileBeginOffset ( long fileBeginOffset ) {
        this.fileBeginOffset = fileBeginOffset;
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param event A PropertyChangeEvent object describing the event source
     *              and the property that has changed.
     */
    @Override
    public void propertyChange ( PropertyChangeEvent event ) {
        if (PrologAtoms.ENCODING.equals(event.getPropertyName())) {
            Term value = (Term) event.getNewValue();
        }
    }

    /**
     * @return
     */
    public String getPath () {
        return path;
    }

    public boolean isEncodingChanged () {
        return encodingChanged;
    }

    public void setEncodingChanged ( boolean encodingChanged ) {
        this.encodingChanged = encodingChanged;
    }

    public HtToken poll () {
        if (!isBofGenerated()) {
            setBofGenerated(true);
            token = HtToken.newToken(BOF);
            return token;
        }
        if (token.next == null) {
            token.next = tokenManager.getNextToken();
        }
        token = token.next;
//        token.next=null;

        return token;
    }

    /**
     * Retrieves, but does not remove, the head token, returning <tt>null</tt> if there are no more tokens.
     *
     * @return The head token, returning <tt>null</tt> if there are no more tokens.
     */
    public HtToken peek () {
        if (!isBofGenerated()) {
            setBofGenerated(true);
            token = newBOFToken();
            return token;
        }
        if (token.next == null) {
            token.next = tokenManager.getNextToken();
        }

        return token.next;
    }

    private HtToken newBOFToken () {
        HtToken t = HtToken.newToken(BOF);
        t.next = new HtToken();
        t.next.kind = PERIOD;
        return t;
    }
}