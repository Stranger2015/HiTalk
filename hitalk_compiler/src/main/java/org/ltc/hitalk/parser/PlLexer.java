package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.io.HiTalkInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Character.*;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.EXISTENCE_ERROR;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.parser.Quotemeta.decode;

/**
 * @author shun
 */
public class PlLexer extends PlTokenSource {
    //    rivate final HiTalkInputStream stream;
    private PlToken token;

    public static final String PUNCTUATION = "#&*+-./\\:;?@^$<=>,!|";
    public static final String PARENTHESES = "(){}[]";

    public PlLexer ( HiTalkInputStream inputStream ) {
        super(inputStream);
    }

//    public HiTalkInputStream getInputStream () {
//        return stream;
//    }

    public static boolean isMergeable ( String l, String r ) {
        return isTokenBoundary(l.charAt(l.length() - 1), r.charAt(0));
    }

    /**
     * Checks whether a token boundary can be between two specified characters.
     */
    public static boolean isTokenBoundary ( char l, char r ) {
        return isJavaIdentifierPart(l) != isJavaIdentifierPart(r) || isTokenBoundary(l) || isTokenBoundary(r);
    }

    /**
     * @param c
     * @return
     */
    public static boolean isTokenBoundary ( char c ) {
        return isWhitespace(c) || PARENTHESES.indexOf(c) != -1;
    }

    /**
     *
     */
    public PlToken next ( boolean value ) throws Exception {
        final HiTalkInputStream stream = getInputStream();
        int lineNumber = stream.getLineNumber();
        int colNumber = stream.getColNumber();

        PlToken token = getToken(value);

        token.setBeginLine(lineNumber);
        token.setBeginColumn(colNumber);
        token.setEndLine(lineNumber);
        token.setEndColumn(colNumber + token.image.length());

        return token;
    }

    /**
     * @return
     */
    public PlToken poll () {
        if (token.next == null) {
            try {
                token.next = next(true);
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
        if (token.next == null) {
            try {
                token.next = next(true);
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(EXISTENCE_ERROR, null);
            }
        }

        return token.next;
    }

    /**
     * @param c
     * @return
     */
    private TokenKind calcTokenKind ( int c ) {
        TokenKind result = null;
        for (TokenKind value : values()) {
            if (value.getChar() == c) {
                result = value;
                break;
            }
        }

        return result;
    }

    int read () throws IOException {
        return getInputStream().read();
    }

    private PlToken getToken ( boolean valued ) throws Exception {
        PlToken token = null;
        try {
            skipWhitespaces();
            int chr = read();
            if (chr == -1) {
                token = PlToken.newToken(EOF);
            } else {
                if (valued) {
                    // 括弧など
                    if ("([{".indexOf(chr) != -1) {
                        token = new PlToken(Objects.requireNonNull(calcTokenKind(chr)));
                    } else if (chr == '-') {
                        int c = read();
                        if (isDigit(c)) {
                            token = getNumber(c, "-");
                        } else {
                            ungetc(c);
                        }
                    } else if (isDigit(chr)) {
                        token = getNumber(chr, "");
                    }
                }
                if (token == null) {
                    if ("}])".indexOf(chr) != -1) {
                        token = new PlToken(calcTokenKind(chr), String.valueOf((char) chr));
                    } else {
                        token = getAtom(chr);
                        if (token == null) {//不正な文字=Illegal characters
                            throw new ParseException(":Bad char `" + (char) chr + ":0x" + Integer.toHexString(chr) + "'.");
                        }
                        if (valued && token.kind == ATOM) {
                            if ((chr = read()) == '(') {
                                token = new PlToken(FUNCTOR_BEGIN);
                            } else {
                                ungetc(chr);
                            }
                        }
                    }
                }
            }
        } catch (EOFException e) {
            token = PlToken.newToken(EOF);
        }

        return token;
    }

    private PlToken getAtom ( int chr ) throws Exception {
        StringBuilder val = new StringBuilder();
        if (";,!|".indexOf(chr) != -1) {
            return new PlToken(ATOM, String.valueOf((char) chr));
        }
//       Atom or variable consisting only of letters
        if (isJavaIdentifierStart(chr)) {
            do {
                val.append((char) chr);
            } while (isJavaIdentifierPart(chr = read()));
            ungetc(chr);
            return new PlToken(isUpperCase(val.charAt(0)) || val.charAt(0) == '_' ? VAR : ATOM, val.toString());
        }
        // 'アトム' = atom
        if (chr == '\'') {
            while ((chr = read()) != '\'') {
                val.append((char) chr);
                if (chr == '\\') {
                    val.append(read());
                }
            }
            return new PlToken(ATOM, decode(val.toString()))/*, true)*/;//todo encoding
        }
        ungetc(chr);
        val = Optional.of(repeat(PUNCTUATION)).map(StringBuilder::new).orElse(null);
        if (!val.toString().isEmpty()) {
            return new PlToken(ATOM, val.toString());
        }

        return null;
    }

    private PlToken getNumber ( int chr, String prefix ) throws Exception {
        String number = prefix;
        if (chr == '0') {
            chr = read();
            if (chr == 'x') {
                return new PlToken(INTEGER_LITERAL, number + "0x" + repeat1("0123456789abcdefABCDEF"));
            }
            ungetc(chr);
            if (isDigit(chr)) {
                number += repeat("01234567");
                if (!isDigit(chr = read())) {
                    ungetc(chr);
                    return new PlToken(INTEGER_LITERAL, "0" + number);
                }
                number += (char) chr;
                number += repeat("0123456789");
            } else {
                number += "0";
            }
        } else {
            number += (char) chr + repeat("0123456789");
        }
        TokenKind kind = INTEGER_LITERAL;
        chr = read();
        if (chr == '.') {
            if (!isDigit(chr = read())) {
                ungetc(chr);
                ungetc('.');
                return new PlToken(INTEGER_LITERAL, number);
            }
            kind = FLOATING_POINT_LITERAL;
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
            kind = FLOATING_POINT_LITERAL;
            number += "e" + sign + repeat1("0123456789");
        } else {
            ungetc(chr);
        }
        return new PlToken(kind, number);
    }

    private String repeat1 ( String chars ) throws Exception {
        String result = repeat(chars);
        if (result.isEmpty()) {
            throw new ParseException("文字がありません。chars=\"" + chars + "\"");
        }

        return result;
    }

    private String repeat ( String chars ) throws Exception {
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

    private void skipWhitespaces () throws Exception {
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
                        while (read() != '*' || read() != '/') ;
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
    public static boolean isWhitespace ( char c ) {
        return " \r\n\t".indexOf(c) != -1;
    }

    private void ungetc ( int c ) throws Exception {
        if (c != -1) {
            getInputStream().unread(c);
        }
    }

    /**
     * @return
     * @throws Exception
     */
    public PlToken getNextToken () throws Exception {
        return next(true);
    }
}
