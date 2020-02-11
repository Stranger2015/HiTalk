package org.ltc.hitalk.parser;


import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.interpreter.TokenBuffer;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkInputStream;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.Objects;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.RESOURCE_ERROR;
import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.entities.PropertyOwner.createProperty;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;

/**
 * @author shun
 */
public class PlLexer implements PropertyChangeListener/*, Stream<PlToken>, Iterator<PlToken> */ {
    public static final String PUNCTUATION = ";,!|.";
    public static final String SPECIAL = "#$&*+-/:<=>?@\\^~";
    public static final String PARENTHESES = "(){}[]";
    public static final String DELIMITERS = "\"'`";

    protected HiTalkInputStream inputStream;
    protected boolean encodingPermitted;
    protected IOperatorTable optable = getAppContext().getOpTable();

    private PlToken lastToken;
    private HiTalkInputStream stream;

    public static PlLexer getTokenSourceForInputStream(InputStream input, String s) {
        return null;
    }

//    /**
//     * Creates a stream tokenizer that parses the specified input
//     * stream. The stream tokenizer is initialized to the following
//     * default state:
//     * <ul>
//     * <li>All byte values {@code 'A'} through {@code 'Z'},
//     *     {@code 'a'} through {@code 'z'}, and
//     *     {@code '\u005Cu00A0'} through {@code '\u005Cu00FF'} are
//     *     considered to be alphabetic.
//     * <li>All byte values {@code '\u005Cu0000'} through
//     *     {@code '\u005Cu0020'} are considered to be white space.
//     * <li>{@code '/'} is a comment character.
//     * <li>Single quote {@code '\u005C''} and double quote {@code '"'}
//     *     are string quote characters.
//     * <li>Numbers are parsed.
//     * <li>Ends of lines are treated as white space, not as separate tokens.
//     * <li>C-style and C++-style comments are not recognized.
//     * </ul>
//     *
//     * @param is an input stream.
//     * @see BufferedReader
//     * @see InputStreamReader
//     * @deprecated As of JDK version 1.1, the preferred way to tokenize an
//     * input stream is to convert it into a character stream, for example:
//     * <blockquote><pre>
//     *   Reader r = new BufferedReader(new InputStreamReader(is));
//     *   StreamTokenizer st = new StreamTokenizer(r);
//     * </pre></blockquote>
//     */
//    public PlLexer(InputStream is) {
//        super(is);
//        pushBackBuffer = new TokenBuffer(inputStream);
//    }

    public TokenBuffer getPushBackBuffer() {
        return pushBackBuffer;
    }

    protected TokenBuffer pushBackBuffer;
    /**
     * Holds the tokenizer that supplies the next token on demand.
     */
    protected IVafInterner interner;

    private boolean encodingChanged;
    private Path path;

    public void toString0(StringBuilder sb) {
//sb.append();
    }

    /**
     *
     */
    public void close() {
        try {
            getInputStream().close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExecutionError(RESOURCE_ERROR, "I/O Error.", e);
        }
    }

    /**
     * @param inputStream
     * @param path
     * @throws FileNotFoundException
     */
    public PlLexer(HiTalkInputStream inputStream, Path path) throws IOException {
//        super(new LineNumberReader(new FileReader()));
//        inputStream.removeListener(inputStream.getTokenSource());
        setPath(path);
        inputStream.setTokenSource(this);
        inputStream.addListener(this);
        this.inputStream = inputStream;
        pushBackBuffer = new TokenBuffer(inputStream);
        inputStream.open();

//     The first token is initialized to be empty, so that the first call to `poll` returns the first token.
        lastToken = new PlToken(TK_BOF);
        encodingPermitted = true;
//        resetSyntax();
    }

    /**
     * @param path
     */
    void setPath(Path path) {
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
        } else if (event.getPropertyName().equals("file_name")) {
            setPath(Paths.get((String) event.getNewValue()));
        }
        setEncodingChanged(isEncodingPermitted());
        encodingPermitted = false;
    }

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
        this.encodingChanged = encodingChanged;
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

        setPath(Paths.get(createProperty("file_name",
                "c:\\Users\\Anthony_2\\IdeaProjects\\WAM\\hitalk_compiler\\src\\main\\resources\\test.pl",
                "").getV()));
        return inputStream == null ? getAppContext().getInputStream() : inputStream;
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
//    @Override
    public Path getPath() {
        return path;
    }

    /**
     * @param valued
     */
    public PlToken next(boolean valued) throws Exception {
        final HiTalkInputStream stream = getInputStream();
        int lineNumber = stream.getLineNumber();
        int colNumber = stream.getColNumber();

        lastToken = stream.isBOFNotPassed() ? PlToken.newToken(TK_BOF) : readToken(valued);

        lastToken.setBeginLine(lineNumber);
        lastToken.setBeginColumn(colNumber);
        lastToken.setEndLine(lineNumber);
        lastToken.setEndColumn(colNumber + lastToken.image.length());//FUNCTOR_BEGIN =  "atom("

        return lastToken;
    }

    /**
     * @param c
     * @return
     */
    private TokenKind calcTokenKind(int c) {
        TokenKind result = null;
        for (TokenKind value : TokenKind.values()) {
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

    /**
     * @param c
     * @return
     */
    public static boolean isLowerCase(char c) {
        return "abcdefghijklmnopqrstuvwxyz".indexOf(c) != -1;
    }

    /**
     * @param c
     * @return
     */
    public static boolean isUpperCase(char c) {
        return "ABCDEFGHIJKLMNOPQRSTUVWXYZ_".indexOf(c) != -1;
    }

    /**
     * @param c
     * @return
     */
    public static boolean isDigit(char c) {
        return "0123456789".indexOf(c) != -1;
    }

    /**
     * @param c
     * @return
     */
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

    /**
     * @param token
     */
    public void unreadToken(PlToken token) {
        pushBackBuffer.pushBack(token);
    }

    /**
     * @param valued
     * @return
     * @throws Exception
     */
    public PlToken getToken(boolean valued) throws Exception {
//        PlToken token = null;
        try {
            skipWhitespaces();
            int chr = read();
            if (chr == -1) {
                lastToken = PlToken.newToken(TK_EOF);
            } else if (valued) {
                // 括弧など
                if ("([{".indexOf(chr) != -1) {
                    lastToken = new PlToken(Objects.requireNonNull(calcTokenKind(chr)));
                } else if (chr == '-' || chr == '+') {
                    int c = read();
                    if (isDigit((char) c)) {
                        lastToken = getNumber(c, new String(new char[]{(char) chr}));
                    } else {
                        ungetc(c);
                    }
                } else if (isDigit((char) chr)) {
                    lastToken = getNumber(chr, "");
                } else if ("}])".indexOf(chr) != -1) {
                    lastToken = new PlToken(calcTokenKind(chr), String.valueOf((char) chr));
                } else {
                    lastToken = getAtom(chr);
                    if (lastToken == null) {//不正な文字=Illegal characters
                        throw new ParserException(":Bad char `" + (char) chr + ":0x" + Integer.toHexString(chr) + "'.");
                    }
                    if (EnumSet.of(TK_SYMBOLIC_NAME, TK_QUOTED_NAME, TK_ATOM).contains(lastToken.kind)) {
                        if ((chr = read()) == '(') {
                            lastToken = new PlToken(TK_FUNCTOR_BEGIN, lastToken.image);//fixme
                        } else {
                            ungetc(chr);
                        }
                    }
                }
            }

        } catch (EOFException e) {
            lastToken = PlToken.newToken(TK_EOF);
        }

        return lastToken;
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
                return new PlToken(TK_QUOTED_NAME, Quotemeta.decode(val.toString()));
            }
//            return new PlToken(ATOM, val.toString());
//        }        // 'アトム' = atom
        ungetc(chr);
        if (!val.toString().isEmpty()) {
            return new PlToken(TK_ATOM, val.toString());
        }
        ungetc(chr);

        return lastToken;
    }

    /**
     * @param s
     * @return
     */
    private boolean isValidOperator(String s) {
        return !optable.getOperators(s).isEmpty();
    }

    /**
     * @param start
     * @return
     */
    private static boolean isOperatorStart(int start) {
        return isOperatorPart(start);
    }

    /**
     * @param chr
     * @return
     */
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
            throw new ParserException("文字がありません。chars=\"" + chars + "\"");
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

//    /**
//     * Retrieves and removes the head of this queue, or <tt>null</tt> if this queue is empty.
//     *
//     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
//     */
//    public PlToken poll() {
//        try {
//            return getNextToken();
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new ExecutionError(ExecutionError.Kind.PERMISSION_ERROR, null);
//        }
//    }

    /**
     * Retrieves, but does not remove, the head of this queue, returning <tt>null</tt> if this queue is empty.
     *
     * @return The head of this queue, or <tt>null</tt> if this queue is empty.
     */
    public PlToken readToken(boolean valued) throws Exception {
        return lastToken = !getPushBackBuffer().isEmpty() ? getPushBackBuffer().poll() : getToken(valued);
    }

    public static PlLexer getPlLexerForString(String string) throws Exception {
        Path path = Paths.get(string);
        HiTalkInputStream inputStream = appContext.createHiTalkInputStream(path);

        final PlLexer lexer = new PlLexer(inputStream, path);
        inputStream.addListener(lexer);

        return lexer;
    }

    /**
     * @param file
     * @return
     * @throws IOException
     */
    public static PlLexer getTokenSourceForIoFile(File file) throws Exception {
        Path path = file.toPath();
        HiTalkInputStream stream = appContext.createHiTalkInputStream(path);
        return new PlLexer(stream, path);
    }

    public String
    toString() {
        return "PlLexer{ path='" + path + "'}";
    }

    /**
     * @param fileName
     * @return
     * @throws IOException
     */
    public static PlLexer getTokenSourceForIoFileName(String fileName) throws Exception {
        HiTalkInputStream stream = appContext.createHiTalkInputStream(Paths.get(fileName));
        return new PlLexer(stream, Paths.get(fileName));
    }

    /**
     * @return
     */
    public PlToken getLastToken() {
        return lastToken;
    }

    /**
     * @param lastToken
     */
    public void setLastToken(PlToken lastToken) {
        this.lastToken = lastToken;
    }
}
