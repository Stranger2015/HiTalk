package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.interpreter.TokenBuffer;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkInputStream;

import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.EnumSet;
import java.util.Objects;

import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.parser.Quotemeta.decode;

/**
 * @author shun
 */
public class PlLexer extends StreamTokenizer implements ITokenSource {
    public static final String PUNCTUATION = ";,!|.";
    public static final String SPECIAL = "#$&*+-/:<=>?@\\^~";//quotes??
    public static final String PARENTHESES = "(){}[]";
    public static final String DELIMITERS = "\"'`";

    protected HiTalkInputStream inputStream;
    protected boolean encodingPermitted;
    protected IOperatorTable optable = getAppContext().getOpTable();

    /**
     * Creates a stream tokenizer that parses the specified input
     * stream. The stream tokenizer is initialized to the following
     * default state:
     * <ul>
     * <li>All byte values {@code 'A'} through {@code 'Z'},
     *     {@code 'a'} through {@code 'z'}, and
     *     {@code '\u005Cu00A0'} through {@code '\u005Cu00FF'} are
     *     considered to be alphabetic.
     * <li>All byte values {@code '\u005Cu0000'} through
     *     {@code '\u005Cu0020'} are considered to be white space.
     * <li>{@code '/'} is a comment character.
     * <li>Single quote {@code '\u005C''} and double quote {@code '"'}
     *     are string quote characters.
     * <li>Numbers are parsed.
     * <li>Ends of lines are treated as white space, not as separate tokens.
     * <li>C-style and C++-style comments are not recognized.
     * </ul>
     *
     * @param is an input stream.
     * @see BufferedReader
     * @see InputStreamReader
     * @see StreamTokenizer#StreamTokenizer(Reader)
     * @deprecated As of JDK version 1.1, the preferred way to tokenize an
     * input stream is to convert it into a character stream, for example:
     * <blockquote><pre>
     *   Reader r = new BufferedReader(new InputStreamReader(is));
     *   StreamTokenizer st = new StreamTokenizer(r);
     * </pre></blockquote>
     */
    public PlLexer(InputStream is) throws FileNotFoundException {
        super(is);
    }

    public TokenBuffer getPushBackBuffer() {
        return pushBackBuffer;
    }

    protected TokenBuffer pushBackBuffer = new TokenBuffer();
    /**
     * Holds the tokenizer that supplies the next token on demand.
     */
    protected IVafInterner interner;
    private boolean encodingChanged;
    private String path;

    /**
     * Create a tokenizer that parses the given character stream.
     *
     * @param r a Reader object providing the input stream.
     * @since JDK1.1
     */
    public PlLexer(Reader r) throws FileNotFoundException {
        super(r);
    }

    public PlLexer(HiTalkInputStream stream, String path) throws FileNotFoundException {
        super(stream.getReader());
        setPath(path);

    }

    public void toString0(StringBuilder sb) {

    }

    /**
     * @throws IOException
     */
    public void close() throws IOException {
        getInputStream().close();
    }
//

    /**
     * @param inputStream
     * @throws FileNotFoundException
     */
    public PlLexer(HiTalkInputStream inputStream) throws FileNotFoundException {
        super(inputStream.getInputStream());
        inputStream.setTokenSource(this);
        this.inputStream = inputStream;
        inputStream.open();
//     The first token is initialized to be empty, so that the first call to `poll` returns the first token.
        final PlToken token = new PlToken(TK_BOF);
        encodingPermitted = true;
        resetSyntax();

        // letters
        wordChars('a', 'z');
        wordChars('A', 'Z');
        wordChars('_', '_');
        wordChars('0', '9'); // need to parse numbers as special words

        ordinaryChar('!');

        // symbols
        ordinaryChar('\\');
        ordinaryChar('$');
        ordinaryChar('&');
        ordinaryChar('^');
        ordinaryChar('@');
        ordinaryChar('#');
        ordinaryChar(',');
        ordinaryChar('.');
        ordinaryChar(':');
        ordinaryChar(';');
        ordinaryChar('=');
        ordinaryChar('<');
        ordinaryChar('>');
        ordinaryChar('+');
        ordinaryChar('-');
        ordinaryChar('*');
        ordinaryChar('/');
        ordinaryChar('~');

        // quotes
        ordinaryChar('\''); // must be parsed individually to handles \\ in quotes and character code constants
        ordinaryChar('\"'); // same as above?
        ordinaryChar('`'); // same as above?

        // comments
        ordinaryChar('%');
        // it is not possible to enable StreamTokenizer#slashStarComments and % as a StreamTokenizer#commentChar
        // and it is also not possible to use StreamTokenizer#whitespaceChars for ' '
    }


    /**
     * @param encoding
     * @throws IOException
     */
    public void onEncodingChanged(String encoding) throws IOException {

    }

    /**
     * @param b
     */
    public void setEncodingPermitted(boolean b) {
        encodingPermitted = b;
    }

    /**
     * @param path
     */
    void setPath(String path) {
        this.path = path;
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param event A PropertyChangeEvent object describing the event source
     *              and the property that has changed.
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (PrologAtoms.ENCODING.equals(event.getPropertyName())) {
            ITerm value = (ITerm) event.getNewValue();
        }
    }

//    /**
//     * @return
//     */
//    @Override
//    public boolean isOpen () {
//        return getInputStream().isOpen();
//    }

    /**
     * @return
     */
    public boolean isEncodingChanged() {
        return encodingChanged;
    }

    /**
     * @param encodingChanged
     */
    public void setEncodingChanged(boolean encodingChanged) {

    }

    /**
     * @return
     */
    public boolean isEncodingPermitted() {
        return encodingPermitted;
    }

    /**
     * @return
     */
    public HiTalkInputStream getInputStream() {
        return inputStream == null ? BaseApp.getAppContext().getInputStream() : inputStream;
    }

    /**
     * @return
     */
    public boolean isOpen() {
        return getInputStream().isOpen();
    }

    /**
     * @return
     */
    @Override
    public String getPath() {
        return path;
    }

//    /**
//     * @param l
//     * @param r
//     * @return
//     */
//    public static boolean isMergeable ( String l, String r ) {
//        return isTokenBoundary(l.charAt(l.length() - 1), r.charAt(0));
//    }

//    /**
//     * Checks whether a token boundary can be between two specified characters.
//     */
//    public static boolean isTokenBoundary ( char l, char r ) {
//        return isPrologIdentifierPart(l) != isPrologIdentifierPart(r) ||
//                isTokenBoundary(l) || isTokenBoundary(r);
//    }
//
//    /**
//     * @param c
//     * @return
//     */
//    public static boolean isTokenBoundary(char c) {
//        return isWhitespace(c) ||
//                PARENTHESES.indexOf(c) != -1 ||
//                PUNCTUATION.indexOf(c) != -1 ||
//                DELIMITERS.indexOf(c) != -1;
//    }

    /**
     * @param valued
     */
    public PlToken next(boolean valued) throws Exception {
        final HiTalkInputStream stream = getInputStream();
        int lineNumber = stream.getLineNumber();
        int colNumber = stream.getColNumber();

        PlToken token = stream.isBOFNotPassed() ? PlToken.newToken(TK_BOF) : readToken();// getToken(valued);

        token.setBeginLine(lineNumber);
        token.setBeginColumn(colNumber);
        token.setEndLine(lineNumber);
        token.setEndColumn(colNumber + token.image.length());//FUNCTOR_BEGIN =  "atom("

        return token;
    }

    /**
     * @param c
     * @return
     */
    private TokenKind calcTokenKind(int c) {
        TokenKind result = null;
        for (TokenKind value : values()) {
            if (value.getChar() == c) {
                result = value;
                break;
            }
        }

        return result;
    }

    int read() throws IOException {
        return getInputStream().read();
    }

    public static boolean isLowerCase(char c) {
        return "abcdefghijklmnopqrstuvwxyz".indexOf(c) != -1;
    }

    public static boolean isUpperCase(char c) {
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZ_".indexOf(c) != -1;
    }

    public static boolean isDigit(char c) {
        return "0123456789".indexOf(c) != -1;
    }

    public static boolean isLetterOrDigit(char c) {
        return isUpperCase(c) || isLowerCase(c) || isDigit(c);
    }

    /**
     * @param c
     * @return
     */
    public static boolean isAtomStart(char c) {
        return isLowerCase(c);
    }

    /**
     * @param c
     * @return
     */
    public static boolean isVarStart(char c) {
        return isUpperCase(c);
    }

    /**
     * @param c
     * @return
     */
    public static boolean isPrologIdentifierPart(int c) {
        return isLetterOrDigit((char) c);
    }

    public void unreadToken(PlToken token) {
        pushBackBuffer.pushBack(token);
    }

    /**
     * @param valued
     * @return
     * @throws Exception
     */
    private PlToken getToken(boolean valued) throws Exception {
        PlToken token = null;
        try {
            skipWhitespaces();
            int chr = read();
            if (chr == -1) {
                token = PlToken.newToken(TK_EOF);
            } else if (valued) {
                // 括弧など
                if ("([{".indexOf(chr) != -1) {
                    token = new PlToken(Objects.requireNonNull(calcTokenKind(chr)));
                } else if (chr == '-' || chr == '+') {
                    int c = read();
                    if (isDigit((char) c)) {
                        token = getNumber(c, new String(new char[]{(char) chr}));
                    } else {
                        ungetc(c);
                    }
                } else if (isDigit((char) chr)) {
                    token = getNumber(chr, "");
                } else if ("}])".indexOf(chr) != -1) {
                    token = new PlToken(calcTokenKind(chr), String.valueOf((char) chr));
                } else {
                    token = getAtom(chr);
                    if (token == null) {//不正な文字=Illegal characters
                        throw new ParseException(":Bad char `" + (char) chr + ":0x" + Integer.toHexString(chr) + "'.");
                    }
                    if (EnumSet.of(TK_SYMBOLIC_NAME, TK_QUOTED_NAME, TK_ATOM).contains(token.kind)) {
                        if ((chr = read()) == '(') {
                            token = new PlToken(TK_FUNCTOR_BEGIN, token.image);//fixme
                        } else {
                            ungetc(chr);
                        }
                    }
                }
            }

        } catch (EOFException e) {
            token = PlToken.newToken(TK_EOF);
        }

        return token;
    }

    private PlToken getAtom(int chr) throws Exception {
        StringBuilder val = new StringBuilder();
        if (PUNCTUATION.indexOf(chr) != -1) {
            return new PlToken(TK_SYMBOLIC_NAME, String.valueOf((char) chr));
        }
        final int start = chr;
        if (isOperatorStart(start)) {
            do {
                val.append((char) chr);
            } while (isOperatorPart(chr = read()));
            ungetc(chr);
            if (isValidOperator(val.toString())) {
                return new PlToken(TK_SYMBOLIC_NAME, val.toString());
            }
        } else
            //       Atom or variable consisting only of letters
            if (isAtomStart((char) start)) {
                do {
                    val.append((char) chr);
                } while (isPrologIdentifierPart(chr = read()));
                ungetc(chr);
            } else if (isVarStart((char) start)) {
                return new PlToken(TK_VAR, val.toString());
            } else if (start == '\'') {
                while ((chr = read()) != '\'') {
                    val.append((char) chr);
                }
                return new PlToken(TK_QUOTED_NAME, decode(val.toString()));
            }
//            return new PlToken(ATOM, val.toString());
//        }        // 'アトム' = atom

        ungetc(chr);

        if (!val.toString().isEmpty()) {
            return new PlToken(TK_ATOM, val.toString());
        }
        ungetc(chr);

        return null;
    }

    private boolean isValidOperator(String s) {
        return !optable.getOperators(s).isEmpty();
    }

    private static boolean isOperatorStart(int start) {
//        return SPPECIAL.indexOf(start) != -1;
        return isOperatorPart(start);
    }

    private static boolean isOperatorPart(int chr) {
        return SPECIAL.indexOf(chr) != -1 || PUNCTUATION.indexOf(chr) != -1;
    }

    private PlToken getNumber(int chr, String prefix) throws Exception {
        String number = prefix;
        if (chr == '0') {
            chr = read();
            if (chr == '\'') {
                chr = read();
                if (SPECIAL.indexOf(chr) != -1) {
                    return new PlToken(TK_INTEGER_LITERAL, String.valueOf(chr));
                }
            }
            if (chr == 'x') {
                return new PlToken(TK_INTEGER_LITERAL, number + "0x" + repeat1("0123456789abcdefABCDEF"));
            }

            ungetc(chr);
            if (isDigit((char) chr)) {
                number += repeat("01234567");
                if (!isDigit((char) (chr = read()))) {
                    ungetc(chr);
                    return new PlToken(TK_INTEGER_LITERAL, "0" + number);
                }
                number += (char) chr;
                number += repeat("0123456789");
            } else {
                number += "0";
            }
        } else {
            number += (char) chr + repeat("0123456789");
        }
        TokenKind kind = TK_INTEGER_LITERAL;
        chr = read();
        if (chr == '.') {
            if (!isDigit((char) (chr = read()))) {
                ungetc(chr);
                ungetc('.');
                return new PlToken(TK_INTEGER_LITERAL, number);
            }
            kind = TK_FLOATING_POINT_LITERAL;
            number += "." + (char) chr + repeat("0123456789");
            chr = read();
        }
        if (chr == 'e' || chr == 'E') {
            String sign = "";
            chr = read();
            if (chr == '+' || chr == '-') {
                sign = String.valueOf((char) chr);
            } else {
                ungetc(chr);
            }
            kind = TK_FLOATING_POINT_LITERAL;
            number += "e" + sign + repeat1("0123456789");
        } else {
            ungetc(chr);
        }
        return new PlToken(kind, number);
    }

    private String repeat1(String chars) throws Exception {
        String result = repeat(chars);
        if (result.isEmpty()) {
            throw new ParseException("文字がありません。chars=\"" + chars + "\"");
        }

        return result;
    }

    private String repeat(String chars) throws Exception {
        StringBuilder result = new StringBuilder();
        for (; ; ) {
            int c = read();
            if (chars.indexOf(c) == -1) {
                ungetc(c);
                break;
            }
            result.append((char) c);
        }

        return result.toString();
    }

    /**
     * @throws Exception
     */
    private void skipWhitespaces() throws Exception {
        for (; ; ) {
            int chr = read();
            if (!isWhitespace((char) chr)) {
                if (chr == '%') {
                    getInputStream().readLine();
                    continue;
                }
                if (chr == '/') {
                    int c = read();
                    if (c == '*') {
                        while (true) {
                            if (read() == '*' && read() == '/') {
                                break;
                            }
                        }
                        continue;
                    }
                    ungetc(c);
                }
                ungetc(chr);
                break;
            }
        }
    }

    /**
     * @param c
     * @return
     */
    public static boolean isWhitespace(char c) {
        return Character.isWhitespace(c);
    }

    /**
     * @param c
     * @throws Exception
     */
    protected void ungetc(int c) throws Exception {
        if (c != -1) {
            getInputStream().unread(c);
        }
    }

    /**
     * @return
     * @throws Exception
     */
    public PlToken getNextToken() throws Exception {
        return next(true);
    }

    /**
     * Retrieves and removes the head of this queue, or <tt>null</tt> if this queue is empty.
     *
     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
     */
    public PlToken poll() {
        try {
            return getNextToken();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExecutionError(ExecutionError.Kind.PERMISSION_ERROR, null);
        }
    }

    /**
     * Retrieves, but does not remove, the head of this queue, returning <tt>null</tt> if this queue is empty.
     *
     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
     */
    public PlToken peek() {
        return poll();
    }

    /**
     * Retrieves, but does not remove, the head of this queue, returning <tt>null</tt> if this queue is empty.
     *
     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
     */
    public PlToken readToken() throws Exception {
        return !getPushBackBuffer().isEmpty() ? getPushBackBuffer().poll() : readNextToken();
    }

    public PlToken readNextToken() throws Exception {
        skipWhitespaces();
        return getToken(true);
    }
///**
// * BNF for tuProlog
// *
// * part 1: Lexer
// *      digit ::= 0 .. 9
// *      lc_letter ::= a .. z
// *      uc_letter ::= A .. Z | _
// *      symbol ::= \ | $ | & | ^ | @ | # | . | , | : | ; | = | < | > | + | - | * | / | ~
//
// *      letter ::= digit | lc_letter | uc_letter
// *      integer ::= { digit }+
// *      float ::= { digit }+ . { digit }+ [ E|e [ +|- ] { digit }+ ]
// *                                                                           // TODO Update BNF for quotes?
// *      atom ::= lc_letter { letter }* | !
// *      variable ::= uc_letter { letter }*
// *
// * from the super class, the super.nextToken() returns and updates the following relevant fields:
// * - if the next token is a collection of wordChars,
// * the type returned is TT_WORD and the value is put into the field sval.
// * - if the next token is an ordinary char,
// * the type returned is the same as the unicode int value of the ordinary character
// * - other characters should be handled as ordinary characters.
// */
//class Tokenizer extends StreamTokenizer implements Serializable {
//
//    static final int TYPEMASK = 0x00FF;
//    static final int ATTRMASK = 0xFF00;
//    static final int LPAR = 0x0001;
//    static final int RPAR = 0x0002;
//    static final int LBRA = 0x0003;
//    static final int RBRA = 0x0004;
//    static final int BAR = 0x0005;
//    static final int INTEGER = 0x0006;
//    static final int FLOAT = 0x0007;
//    static final int ATOM = 0x0008;
//    static final int VARIABLE = 0x0009;
//    static final int SQ_SEQUENCE = 0x000A;
//    static final int DQ_SEQUENCE = 0x000B;
//    static final int END = 0x000D;
//    static final int LBRA2 = 0x000E;
//    static final int RBRA2 = 0x000F;
//    static final int FUNCTOR = 0x0100;
//    static final int OPERATOR = 0x0200;
//    static final int EOF = 0x1000;
//
//    static final char[] GRAPHIC_CHARS = {'\\', '$', '&', '?', '^', '@', '#', '.', ',', ':', ';', '=', '<', '>', '+', '-', '*', '/', '~'};
//
//    static {
//        Arrays.sort(Tokenizer.GRAPHIC_CHARS);  // must be done to ensure correct behavior of Arrays.binarySearch
//    }
//
//    //used to enable pushback from the parser. Not in any way connected with pushBack2 and super.pushBack().
//    private LinkedList tokenList = new LinkedList();
//
//    //used in the double lookahead check that . following ints is a fraction marker or end marker (pushback() only works on one level)
//    private PushBack pushBack2 = null;
//
//    public Tokenizer ( String text ) {
//        this(new StringReader(text));
//    }
//
//    /**
//     * creating a tokenizer for the source stream
//     */
//    public Tokenizer ( Reader text ) {
//        super(text);
//
//        // Prepare the tokenizer for Prolog-style tokenizing rules
//
//    }
//
//
//    /**
//     * puts back token to be read again
//     */
//    void unreadToken ( Token token ) {
//        tokenList.addFirst(token);
//    }
//

//
//    /**
//     * @param typec
//     * @param svalc
//     * @return the intValue of the next character token, -1 if invalid
//     * todo needs a lookahead if typec is \
//     */
//    private static int isCharacterCodeConstantToken ( int typec, String svalc ) {
//        if (svalc != null) {
//            if (svalc.length() == 1)
//                return (int) svalc.charAt(0);
//            if (svalc.length() > 1) {
//// TODO the following charachters is not implemented:
////                * 1 meta escape sequence (* 6.4.2.1 *) todo
////                * 1 control escape sequence (* 6.4.2.1 *)
////                * 1 octal escape sequence (* 6.4.2.1 *)
////                * 1 hexadecimal escape sequence (* 6.4.2.1 *)
//                return -1;
//            }
//        }
//        if (typec == ' ' ||                       // space char (* 6.5.4 *)
//                Arrays.binarySearch(GRAPHIC_CHARS, (char) typec) >= 0)  // graphic char (* 6.5.1 *)
////        	TODO solo char (* 6.5.3 *)
//            return typec;
//
//        return -1;
//    }
//
//    private static boolean isWhite ( int type ) {
//        return type == ' ' || type == '\r' || type == '\n' || type == '\t' || type == '\f';
//    }
//
//    /**
//     * used to implement lookahead for two tokens, super.pushBack() only handles one pushBack..
//     */
//    private static class PushBack {
//        int typea;
//        String svala;
//
//        public PushBack ( int i, String s ) {
//            typea = i;
//            svala = s;
//        }
//    }
//}
// fullstop(Bool)
//    If true (default false), add a fullstop token to the output.
//    The dot is preceeded by a space if needed and followed by a space (default) or newline if the nl(true) option is
//    also given.
//ignore_ops(Bool)
//    If true, the generic term representation (<functor>(<args> ... )) will be used for all terms.
//    Otherwise (default),//    operators will be used where appropriate..
    }
