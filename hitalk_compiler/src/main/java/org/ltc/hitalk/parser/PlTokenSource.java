package org.ltc.hitalk.parser;


import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.parsing.SourceCodePositionImpl;
import com.thesett.common.util.Source;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.HlOpSymbol.Associativity;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.HtFunctorName;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.EXISTENCE_ERROR;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.term.HlOpSymbol.Associativity.*;

public class PlTokenSource implements Source <PlToken>, PropertyChangeListener {
    private boolean encodingPermitted;

    /**
     * Holds the current token.
     */
    public PlToken token;

    /**
     * Holds the tokenizer that supplies the next token on demand.
     */
    public PlLexer lexer;
    //    protected Source <PlToken> tokenSource;
    protected IVafInterner interner;
    protected IOperatorTable operatorTable;

    /**
     * Builds a token source around the specified token manager.
     *
     * @param lexer The token manager to use to feed this source.
     */
    public PlTokenSource ( PlLexer lexer ) {
        this.lexer = lexer;

        // The first token is initialized to be empty, so that the first call to poll returns the first token.
        token = new PlToken(BOF);
        encodingPermitted = true;
    }

    private boolean isBofGenerated;
    private boolean encodingChanged;
    private String encoding = "UTF8";

    /**
     * @param string
     * @return
     * @throws IOException
     * @throws CloneNotSupportedException
     */
    public static PlTokenSource getPlTokenSourceForString ( String string ) throws IOException, CloneNotSupportedException {
        return getPlTokenSourceForStdin(new ByteArrayInputStream(string.getBytes()));
    }

    private static PlTokenSource getPlTokenSourceForStdin ( InputStream inputStream ) throws IOException, CloneNotSupportedException {
        HiTalkInputStream stream = getAppContext().currentInput();
        stream.setInputStream(new FileInputStream(FileDescriptor.in));
        PlLexer lexer = new PlLexer(stream);
        return new PlTokenSource(lexer);
    }

    /**
     * @return
     */
    public boolean isEofGenerated () {
        return isEofGenerated;
    }

    private boolean isEofGenerated;

    /**
     * @return
     */
    public boolean isBofGenerated () {
        return isBofGenerated;
    }

    /**
     * @param bofGenerated
     */
    public void setBofGenerated ( boolean bofGenerated ) {
        isBofGenerated = bofGenerated;
    }

    protected HiTalkInputStream stream;
    private String path;

    /**
     * @param encoding
     * @throws IOException
     */
    protected void onEncodingChanged ( String encoding ) throws IOException {
        if (token.kind == DOT) {
            setEncodingChanged(true);
            setEncodingPermitted(false);
        }
    }

    /**
     * @param lexer
     * @param path
     */
    public PlTokenSource ( PlLexer lexer, String path ) {
        this.lexer = lexer;
        this.path = path;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public static PlTokenSource getTokenSourceForIoFile ( File file ) throws IOException {
        PlLexer lexer = new PlLexer(new HiTalkInputStream(file.getAbsolutePath()));
        return new PlTokenSource(lexer);
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
            ITerm value = (ITerm) event.getNewValue();
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

    /**
     * @return
     */
    public PlToken poll () {
        if (!isBofGenerated()) {
            setBofGenerated(true);
            token = newBOFToken();
            return token;
        }
        if (token.next == null) {
            try {
                token.next = lexer.next(true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(EXISTENCE_ERROR, null);
            }

        }

        token = token.next;
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
            try {
                token.next = lexer.next(true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(EXISTENCE_ERROR, null);
            }
        }

        return token.next;
    }

    private PlToken newBOFToken () {
        return PlToken.newToken(BOF);
    }

    private PlToken newEOFToken () {
        return PlToken.newToken(EOF);
    }

    public HiTalkInputStream getStream () {
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

        if ((associativity == xfy) | (associativity == yfx) | (associativity == xfx)) {
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
        internOperator(PrologAtoms.IMPLIES, 1200, xfx);
        internOperator(PrologAtoms.IMPLIES, 1200, fx);
        internOperator(PrologAtoms.DCG_IMPLIES, 1200, xfx);
        internOperator(PrologAtoms.QUERY, 1200, fx);

        internOperator(PrologAtoms.SEMICOLON, 1100, xfy);
        internOperator(PrologAtoms.IF, 1050, xfy);
        internOperator(PrologAtoms.IF_STAR, 1050, xfy);

        internOperator(PrologAtoms.COMMA, 1000, xfy);
        internOperator(PrologAtoms.NOT, 900, fy);

        internOperator(PrologAtoms.UNIFIES, 700, xfx);
        internOperator(PrologAtoms.NON_UNIFIES, 700, xfx);
        internOperator(PrologAtoms.IDENTICAL, 700, xfx);
        internOperator(PrologAtoms.NON_IDENTICAL, 700, xfx);
        internOperator(PrologAtoms.AT_LESS, 700, xfx);
        internOperator(PrologAtoms.AT_LESS_OR_EQUAL, 700, xfx);
        internOperator(PrologAtoms.AT_GREATER, 700, xfx);
        internOperator(PrologAtoms.AT_GREATER_OR_EQUAL, 700, xfx);
        internOperator(PrologAtoms.UNIV, 700, xfx);
        internOperator(PrologAtoms.IS, 700, xfx);
//        internOperator(PrologAtoms.C, 700, xfx);
        internOperator(PrologAtoms.EQ_BSLASH_EQ, 700, xfx);
        internOperator(PrologAtoms.LESS, 700, xfx);
        internOperator(PrologAtoms.LESS_OR_EQUAL, 700, xfx);
        internOperator(PrologAtoms.GREATER, 700, xfx);
        internOperator(PrologAtoms.GREATER_OR_EQUAL, 700, xfx);

//        internOperator(PrologAtoms."+", 500, yfx);
//        internOperator(PrologAtoms."-", 500, yfx);

        internOperator(PrologAtoms.BSLASH_SLASH, 500, yfx);
        internOperator(PrologAtoms.SLASH_BSLASH, 500, yfx);

        internOperator(PrologAtoms.SLASH, 400, yfx);
        internOperator(PrologAtoms.SLASH_SLASH, 400, yfx);
        internOperator(PrologAtoms.STAR, 400, yfx);
        internOperator(PrologAtoms.RSHIFT, 400, yfx);
        internOperator(PrologAtoms.LSHIFT, 400, yfx);
        internOperator(PrologAtoms.REM, 400, yfx);
        internOperator(PrologAtoms.MOD, 400, yfx);

        internOperator(PrologAtoms.MINUS, 200, fy);
        internOperator(PrologAtoms.UP, 200, yfx);
        internOperator(PrologAtoms.STAR_STAR, 200, yfx);
        internOperator(PrologAtoms.AS, 200, fy);

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
        PlToken result;
        PlToken nextToken = peek();

        if (nextToken.kind != kind) {
            throw new SourceCodeException("Expected " + token.image + " but got " + nextToken.image + ".", null, null, null, new SourceCodePositionImpl(nextToken.beginLine, nextToken.beginColumn, nextToken.endLine, nextToken.endColumn));
        } else {
            nextToken = poll();
            result = nextToken;
        }
        return result;
    }

    /**
     * Peeks ahead for the given token entityKind,
     * and if one is found with that entityKind, it is consumed.
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

    /**
     * @param isEofGenerated
     */
    public void setEofGenerated ( boolean isEofGenerated ) {
        this.isEofGenerated = isEofGenerated;
    }

    public boolean isEncodingPermitted () {
        return encodingPermitted;
    }

    public void setEncodingPermitted ( boolean encodingPermitted ) {
        this.encodingPermitted = encodingPermitted;
    }
}
