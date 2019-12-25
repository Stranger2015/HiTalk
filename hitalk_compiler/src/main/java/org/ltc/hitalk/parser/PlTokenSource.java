package org.ltc.hitalk.parser;

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

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.PlToken.TokenKind.BOF;
import static org.ltc.hitalk.parser.PlToken.TokenKind.DOT;
import static org.ltc.hitalk.term.HlOpSymbol.Associativity.*;

abstract public class PlTokenSource implements Source <PlToken>, PropertyChangeListener {

    protected HiTalkInputStream inputStream;
    protected boolean encodingPermitted;

    /**
     * Holds the current token.
     */
    public PlToken token;

    public String toString () {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName() + "{");
        sb.append("inputStream=").append(inputStream);
        sb.append(", path='").append(path).append('\'');
        sb.append('}');
        return sb.toString();
    }

    /**
     * Holds the tokenizer that supplies the next token on demand.
     */
    protected IVafInterner interner;
    protected IOperatorTable operatorTable;

    /**
     * Builds a token source around the specified token manager.
     *
     * @param inputStream The token manager to use to feed this source.
     */
    public PlTokenSource ( HiTalkInputStream inputStream ) {
        this.inputStream = inputStream;

        // The first token is initialized to be empty, so that the first call to poll returns the first token.
        token = new PlToken(BOF);
        isBofGenerated = true;
        encodingPermitted = true;
    }

    private boolean isBofGenerated;
    private boolean encodingChanged;
    private String encoding = "UTF-8";

    /**
     * //     * @param string
     *
     * @return
     */
    public static PlTokenSource getPlTokenSourceForString ( String string ) throws FileNotFoundException {
        HiTalkInputStream inputStream = new HiTalkInputStream(string);
        return new PlLexer(inputStream);
    }

    public static PlTokenSource getPlTokenSourceForStdin () {
        HiTalkInputStream stream = getAppContext().currentInput();
        stream.setInputStream(new FileInputStream(FileDescriptor.in));
        return new PlLexer(stream);
    }

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

    //    protected HiTalkInputStream stream;
    protected String path;

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

    public void setEncodingPermitted ( boolean b ) {
        encodingPermitted = b;
    }

    /**
     * @param inputStream
     * @param path
     */
    public PlTokenSource ( HiTalkInputStream inputStream, String path ) {
//        super(inputStream);
        this.inputStream = inputStream;
        this.path = path;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public static PlTokenSource getTokenSourceForIoFile ( File file ) throws IOException {
        HiTalkInputStream stream = new HiTalkInputStream(file);
        return new PlLexer(stream);
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
//

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
     */
    protected PlToken consumeToken ( TokenKind kind ) throws HtSourceCodeException {
        PlToken result;
        PlToken nextToken = peek();

        if (nextToken.kind != kind) {
            throw new HtSourceCodeException("Expected " + token.image + " but got " + nextToken.image + ".",
                    null,
                    null,
                    null,
                    nextToken);
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
            } catch (HtSourceCodeException e) {
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
     * @return
     */
    public boolean isEncodingPermitted () {
        return encodingPermitted;
    }

    /**
     * @return
     */
    public HiTalkInputStream getInputStream () {
        return inputStream;
    }
}
