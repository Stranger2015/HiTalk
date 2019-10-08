package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import com.thesett.aima.logic.fol.Term;
import com.thesett.common.util.Source;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.PrologAtoms;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;

import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind.BOF;
import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind.DOT;

public class PlTokenSource implements Source <PlToken>, PropertyChangeListener {

    /**
     * Holds the current token.
     */
    public PlToken token;

    /**
     * Holds the tokenizer that supplies the next token on demand.
     */
    public PlLexer lexer;

    /**
     * Builds a token source around the specified token manager.
     *
     * @param lexer The token manager to use to feed this source.
     */
    protected PlTokenSource ( PlLexer lexer ) {
        this.lexer = lexer;

        // The first token is initialized to be empty, so that the first call to poll returns the first token.
        token = new PlToken(BOF);
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
        if (token.kind == DOT) {
            fileBeginOffset = stream.position();
            setBofGenerated(false);
            setEncodingChanged(true);
        }
    }

    /**
     * Builds a token source around the specified token manager.
     *
     * @param lexer The token manager to use to feed this source.
     * @param input
     */
    public PlTokenSource ( PlLexer lexer, InputStream input ) {
        this(lexer);
        stream = new HiTalkStream(input, this);
        this.lexer = lexer;

    }

    public PlTokenSource ( PlLexer lexer, InputStream input, String path ) {
        this(lexer, input);
        this.path = path;
    }

    /**
     * Creates a token source on a string.
     *
     * @param stringToTokenize The string to tokenize.
     * @return A token source.
     */
    public static PlTokenSource getPlTokenSourceForString ( String stringToTokenize, int lineOfs ) throws IOException {
        InputStream input = new ByteArrayInputStream(stringToTokenize.getBytes());
        return getTokenSourceForInputStream(input, "");//fixme
    }

    public static PlTokenSource getTokenSourceForIoFile ( File file ) throws IOException {
        return getTokenSourceForUri(file.toURI());
    }

    private static PlTokenSource getTokenSourceForUri ( URI uri ) throws IOException {
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
    public static PlTokenSource getTokenSourceForPath ( Path path ) throws IOException {
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
    public static PlTokenSource getTokenSourceForInputStream ( InputStream in, String path ) throws IOException {
        InputStreamReader input = new InputStreamReader(in);
//        SimpleCharStream inputStream = new SimpleCharStream(input, 1, 1);
        PlLexer lexer = new PlLexer(input);

        return new PlTokenSource(lexer, in, path);
    }

    /**
     * @param vfsFo
     * @return
     * @throws IOException
     */
    public static PlTokenSource getTokenSourceForVfsFileObject ( FileObject vfsFo ) throws IOException {
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

    public PlToken poll () {
        if (!isBofGenerated()) {
            setBofGenerated(true);
            token = PlToken.newToken(BOF);
            return token;
        }
        if (token.next == null) {
            try {
                token.next = lexer.next(true);
            } catch (ParseException | IOException e) {
                e.printStackTrace();
                throw new ExecutionError(ExecutionError.Kind.PERMISSION_ERROR, null);
            }
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
    public PlToken peek () {
        if (!isBofGenerated()) {
            setBofGenerated(true);
            token = newBOFToken();
            return token;
        }
        if (token.next == null) {
            token.next = lexer.next(true);
        }

        return token.next;
    }

    private PlToken newBOFToken () {
        PlToken t = PlToken.newToken(BOF);
        t.next = new PlToken(DOT);
        return t;
    }
}
