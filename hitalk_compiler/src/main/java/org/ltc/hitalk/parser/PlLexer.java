package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.io.EOFException;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Character.*;
import static org.ltc.hitalk.parser.PlToken.TokenKind.*;
import static org.ltc.hitalk.parser.Quotemeta.decode;

/**
 * @author shun
 */
public class PlLexer {
    private final HiTalkStream stream;
    private PlToken token;

    public static final String PUNCTUATION = "#&*+-./\\:;?@^$<=>";
    public static final String PARENTHESIS = "(){}[],!|";

    public PlLexer ( HiTalkStream stream ) {
        this.stream = stream;
    }

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
        return isWhitespace(c) || PARENTHESIS.indexOf(c) != -1;
    }

    /**
     *
     */
    public PlToken next ( boolean value ) throws Exception {
        return (token = getToken(value));
    }

    /**
     * 前に解析したトークンを返します。
     */
    public PlToken peek () {
        return token;
    }

    private TokenKind calcTokenKind ( int c ) {
        for (TokenKind value : values()) {
            if (value.getChar() == c) {
                return value;
            }
        }
        return null;
    }

    int read () throws IOException {
        return stream.readChar();
    }

    private PlToken getToken ( boolean valued ) throws Exception {
        try {
            if (stream.isBOF()) {
                return PlToken.newToken(BOF);
            }
            skipWhitespaces();
            int chr = read();
            if (chr == -1 || chr == 26) {
                return PlToken.newToken(EOF);
            }
            if (valued) {
                // 括弧など
                if ("([{".indexOf(chr) != -1) {
                    return new PlToken(Objects.requireNonNull(calcTokenKind(chr)));
                }
                // 整数値アトム
                if (chr == '-') {
                    int c = read();
                    if (isDigit(c)) {
                        return getNumber(c, "-");
                    }
                    ungetc(c);
                } else if (isDigit(chr)) {
                    return getNumber(chr, "");
                }
            }

            if ("}])".indexOf(chr) != -1) {
                return new PlToken(calcTokenKind(chr), String.valueOf((char) chr));
            }
            PlToken token = getAtom(chr);
            if (token == null) {//不正な文字=Illegal characters
                throw new ParseException(":Bad char `" + (char) chr + ":0x" + Integer.toHexString(chr) + "'.");
            }
            if (valued && token.kind == ATOM) {
                if ((chr = read()) == '(') {
                    return new PlToken(FUNCTOR_BEGIN);
                }
                ungetc(chr);
            }
        } catch (EOFException e) {
            return PlToken.newToken(EOF);
        }

        return token;
    }

    private PlToken getAtom ( int chr ) throws Exception {
        StringBuilder val = new StringBuilder();
        // 単体でアトムを構成=Atom is composed of body
        if (";,!|".indexOf(chr) != -1) {
            return new PlToken(ATOM, String.valueOf((char) chr));
        }
        // アルファベットのみで構成されるアトムか変数=Atom or variable consisting only of letters
        if (isJavaIdentifierStart(chr)) {
            do {
                val.append((char) chr);
            } while (isJavaIdentifierPart(chr = read()));
            ungetc(chr);
            return new PlToken(isUpperCase(val.charAt(0)) || val.charAt(0) == '_' ? VAR : ATOM, val.toString());
        }
        // 'アトム' = atom
        if (chr == '\'') {
            while ((chr = readFully()) != '\'') {
                val.append((char) chr);
                if (chr == '\\') {
                    val.append(readFully());
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

    private char readFully () throws IOException {
        int c = read();
        if (c == -1) {
            throw new EOFException();
        }
        return (char) c;
    }

    private void skipWhitespaces () throws Exception {
        for (; ; ) {
            int chr = read();
            if (!isWhitespace(chr)) {
                if (chr == '%') {
                    stream.readLine();
                    continue;
                }
                if (chr == '/') {
                    int c = read();
                    if (c == '*') {
                        while (true) {
                            if (readFully() == '*' && readFully() == '/') {
                                break;
                            }
                            //todo detect eol's
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

    private void ungetc ( int c ) throws Exception {
        if (c != -1) {
            stream.unreadChar(c);
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
