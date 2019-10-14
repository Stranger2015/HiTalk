package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import com.thesett.aima.logic.fol.OpSymbol.Associativity;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.OperatorTable;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.parsing.SourceCodePositionImpl;
import com.thesett.common.util.Source;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.PrologAtoms;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.HtFunctorName;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.net.URI;
import java.nio.file.Path;

import static com.thesett.aima.logic.fol.OpSymbol.Associativity.*;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
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
    private Source <PlToken> tokenSource;
    private VariableAndFunctorInterner interner;
    private OperatorTable operatorTable;

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
    public PlTokenSource ( PlLexer lexer, InputStream input ) throws IOException {
        this(lexer);
        stream = new HiTalkStream((FileInputStream) input, this);
        this.lexer = lexer;

    }

    public PlTokenSource ( PlLexer lexer, InputStream input, String path ) throws IOException {
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
        PlLexer lexer = new PlLexer(new HiTalkStream(new FileInputStream(path)));

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

    /**
     * @return
     */
    public boolean isEncodingChanged () {
        return encodingChanged;
    }

    /**
     * @param encodingChanged
     */
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
            token.next = lexer.next(true);
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
//        t.next = new PlToken(DOT);
        return t;
    }

    public HiTalkStream getStream () {
        return stream;
    }

    /**
     * Interns an operators name as a functor of appropriate arity for the operators fixity, and sets the operator in
     * the operator table.
     *
     * @param operatorName  The name of the operator to create.
     * @param priority      The priority of the operator, zero unsets it.
     * @param associativity The operators associativity.
     */
    public void internOperator ( String operatorName, int priority, Associativity associativity ) {
        int arity;

        if ((associativity == XFY) | (associativity == YFX) | (associativity == XFX)) {
            arity = 2;
        } else {
            arity = 1;
        }

        int name = interner.internFunctorName(operatorName, arity);
        operatorTable.setOperator(name, operatorName, priority, associativity);
    }

    /**
     * Interns and inserts into the operator table all of the built in operators and functors in Prolog.
     */
    protected void initializeBuiltIns () {
        // Initializes the operator table with the standard ISO prolog built-in operators.
        internOperator(PrologAtoms.IMPLIES, 1200, XFX);
        internOperator(PrologAtoms.IMPLIES, 1200, FX);
        internOperator(PrologAtoms.DCG_IMPLIES, 1200, XFX);
        internOperator(PrologAtoms.QUERY, 1200, FX);

        internOperator(PrologAtoms.SEMICOLON, 1100, XFY);
        internOperator(PrologAtoms.IF, 1050, XFY);
        internOperator(PrologAtoms.IF_STAR, 1050, XFY);

        internOperator(PrologAtoms.COMMA, 1000, XFY);
        internOperator(PrologAtoms.NOT, 900, FY);

        internOperator(PrologAtoms.UNIFIES, 700, XFX);
        internOperator(PrologAtoms.NON_UNIFIES, 700, XFX);
        internOperator(PrologAtoms.IDENTICAL, 700, XFX);
        internOperator(PrologAtoms.NON_IDENTICAL, 700, XFX);
        internOperator(PrologAtoms.AT_LESS, 700, XFX);
        internOperator(PrologAtoms.AT_LESS_OR_EQUAL, 700, XFX);
        internOperator(PrologAtoms.AT_GREATER, 700, XFX);
        internOperator(PrologAtoms.AT_GREATER_OR_EQUAL, 700, XFX);
        internOperator(PrologAtoms.UNIV, 700, XFX);
        internOperator(PrologAtoms.IS, 700, XFX);
//        internOperator(PrologAtoms.C, 700, XFX);
        internOperator(PrologAtoms.EQ_BSLASH_EQ, 700, XFX);
        internOperator(PrologAtoms.LESS, 700, XFX);
        internOperator(PrologAtoms.LESS_OR_EQUAL, 700, XFX);
        internOperator(PrologAtoms.GREATER, 700, XFX);
        internOperator(PrologAtoms.GREATER_OR_EQUAL, 700, XFX);

//        internOperator(PrologAtoms."+", 500, YFX);
//        internOperator(PrologAtoms."-", 500, YFX);

        internOperator(PrologAtoms.BSLASH_SLASH, 500, YFX);
        internOperator(PrologAtoms.SLASH_BSLASH, 500, YFX);

        internOperator(PrologAtoms.SLASH, 400, YFX);
        internOperator(PrologAtoms.SLASH_SLASH, 400, YFX);
        internOperator(PrologAtoms.STAR, 400, YFX);
        internOperator(PrologAtoms.RSHIFT, 400, YFX);
        internOperator(PrologAtoms.LSHIFT, 400, YFX);
        internOperator(PrologAtoms.REM, 400, YFX);
        internOperator(PrologAtoms.MOD, 400, YFX);

        internOperator(PrologAtoms.MINUS, 200, FY);
        internOperator(PrologAtoms.UP, 200, YFX);
        internOperator(PrologAtoms.STAR_STAR, 200, YFX);
        internOperator(PrologAtoms.AS, 200, FY);
        //FIXME
        internOperator(PrologAtoms.VBAR, 1001, XFY);
        internOperator(PrologAtoms.VBAR, 1001, FY);

        // Intern all built in functors.
        interner.internFunctorName(PrologAtoms.ARGLIST_NIL, 0);
        interner.internFunctorName(new HtFunctorName(PrologAtoms.ARGLIST_CONS, 0, 2));

        interner.internFunctorName(PrologAtoms.NIL, 0);
        interner.internFunctorName(new HtFunctorName(PrologAtoms.CONS, 0, 2));

        interner.internFunctorName(PrologAtoms.TRUE, 0);
        interner.internFunctorName(PrologAtoms.FAIL, 0);
        interner.internFunctorName(PrologAtoms.FALSE, 0);
        interner.internFunctorName(PrologAtoms.CUT, 0);

        interner.internFunctorName(PrologAtoms.BYPASS_NIL, 0);
        interner.internFunctorName(new HtFunctorName(PrologAtoms.BYPASS_CONS, 0, 2));
    }

    /**
     * Consumes a token of the expected kind from the token sequence. If the next token in the sequence is not of the
     * expected kind an error will be raised.
     *
     * @param kind The kind of token to consume.
     * @return The consumed token of the expected kind.
     * @throws SourceCodeException If the next token in the sequence is not of the expected kind.
     */
    protected PlToken consumeToken ( TokenKind kind ) throws SourceCodeException {
        PlToken nextToken = peek();

        if (nextToken.kind != kind) {
            throw new SourceCodeException("Expected " + token.image + " but got " + nextToken.image + ".", null, null, null, new SourceCodePositionImpl(nextToken.beginLine, nextToken.beginColumn, nextToken.endLine, nextToken.endColumn));
        } else {
            nextToken = poll();

            return nextToken;
        }
    }

    /**
     * Peeks ahead for the given token entityKind, and if one is foudn with that entityKind, it is consumed.
     *
     * @param kind The token kind to look for.
     * @return <tt>true</tt> iff the token was found and consumed.
     */
    private boolean peekAndConsume ( TokenKind kind ) {
        PlToken nextToken = peek();
        if (nextToken.kind == kind) {
            try {
                consumeToken(kind);
            } catch (SourceCodeException e) {
                // If the peek ahead kind can not be consumed then something strange has gone wrong so report this
                // as a bug rather than try to recover from it.
                throw new ExecutionError(PERMISSION_ERROR, null);//illegal  state
            }
            return true;
        } else {
            return false;
        }
    }
}
