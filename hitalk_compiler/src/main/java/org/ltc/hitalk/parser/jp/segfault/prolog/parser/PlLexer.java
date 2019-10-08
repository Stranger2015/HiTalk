package org.ltc.hitalk.parser.jp.segfault.prolog.parser;


import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind;

import java.io.*;
import java.util.Optional;

import static java.lang.Character.*;
import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind.*;

/**
 * 入力ストリームをPrologテキストとみなし、トークン列に分解します。
 *
 * @author shun
 */
public class PlLexer {

    private PushbackReader reader;
    private PlToken token;

    public static final String PUNCTUATION = "#&*+-./\\:;?@^$<=>";
    public static final String PARENTHESIS = "(){}[],!|";

    public PlLexer ( Reader reader ) {
        this.reader = new PushbackReader(reader, 3);
    }

    public static boolean isMergeable ( String l, String r ) {
        return isTokenBoundary(l.charAt(l.length() - 1), r.charAt(0));
    }

    /**
     * 指定した二文字の間がトークンの境界になりうるかどうかを調べます。
     */
    public static boolean isTokenBoundary ( char l, char r ) {
        return isJavaIdentifierPart(l) != isJavaIdentifierPart(r) || isTokenBoundary(l) || isTokenBoundary(r);
    }

    public static boolean isTokenBoundary ( char c ) {
        return isWhitespace(c) || PARENTHESIS.indexOf(c) != -1;
    }

    /**
     * 次のトークンを返します。
     */
    public PlToken next ( boolean value ) {
        try {
            return (token = getToken(value));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            throw new ExecutionError(ExecutionError.Kind.PERMISSION_ERROR, null);
        }
    }

    /**
     * 前に解析したトークンを返します。
     */
    public PlToken peek () {
        return token;
    }

    private TokenKind calcTokenKind ( int c ) {
        for (int i = 0; i < values().length; i++) {
            TokenKind value = values()[i];
            if (value.getChar() == c) {
                return value;
            }
        }
        return null;
    }

    private PlToken getToken ( boolean valued ) throws IOException, ParseException {
        int chr = reader.read();
        if (chr == -1) {
            return null;
        }
        if (valued) {
            // 括弧など
            if ("([{".indexOf(chr) != -1) {
                return new PlToken(calcTokenKind(chr));
            }
            // 整数値アトム
            if (chr == '-') {
                int c = reader.read();
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
        if (token == null) {//不正な文字
            throw new ParseException(":Bad char `" + (char) chr + ":0x" + Integer.toHexString(chr) + "'.");
        }
        if (valued && token.kind == ATOM) {

            if ((chr = reader.read()) == '(') {
                return new PlToken(FUNCTOR);
            }
            ungetc(chr);
        }

        return token;
    }

    private PlToken getAtom ( int chr ) throws IOException, ParseException {
        StringBuilder val = new StringBuilder();
        // 単体でアトムを構成
        if (";,!|".indexOf(chr) != -1) {
            return new PlToken(ATOM, String.valueOf((char) chr));
        }
        // アルファベットのみで構成されるアトムか変数
        if (isJavaIdentifierStart(chr)) {
            do {
                val.append((char) chr);
            } while (isJavaIdentifierPart(chr = reader.read()));
            ungetc(chr);
            return new PlToken(isUpperCase(val.charAt(0)) || val.charAt(0) == '_' ? VAR : ATOM, val.toString());
        }
        // 'アトム'
        if (chr == '\'') {
            while ((chr = readFully()) != '\'') {
                val.append((char) chr);
                if (chr == '\\') {
                    val.append(readFully());
                }
            }
            return new PlToken(ATOM, Quotemeta.decode(val.toString()))/*, true)*/;
        }
        // アトム
        ungetc(chr);
        val = Optional.of(repeat(PUNCTUATION)).map(StringBuilder::new).orElse(null);
        if (!val.toString().isEmpty()) {
            return new PlToken(ATOM, val.toString());
        }
        return null;
    }

    private PlToken getNumber ( int chr, String prefix ) throws IOException, ParseException {
        String number = prefix;
        if (chr == '0') {
            chr = reader.read();
            if (chr == 'x') {
                return new PlToken(INTEGER_LITERAL, number + "0x" + repeat1("0123456789abcdefABCDEF"));
            }
            ungetc(chr);
            if (isDigit(chr)) {
                number += repeat("01234567");
                if (!isDigit(chr = reader.read())) {
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
        PlToken.TokenKind kind = INTEGER_LITERAL;
        chr = reader.read();
        if (chr == '.') {
            if (!isDigit(chr = reader.read())) {
                ungetc(chr);
                ungetc('.');
                return new PlToken(INTEGER_LITERAL, number);
            }
            kind = FLOATING_POINT_LITERAL;
            number += "." + (char) chr + repeat("0123456789");
            chr = reader.read();
        }
        if (chr == 'e' || chr == 'E') {
            String sign = "";
            chr = reader.read();
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

    private String repeat1 ( String chars ) throws IOException, ParseException {
        String result = repeat(chars);
        if (result.isEmpty()) {
            throw new ParseException("文字がありません。chars=\"" + chars + "\"");
        }
        return result;
    }

    private String repeat ( String chars ) throws IOException {
        StringBuilder result = new StringBuilder();
        for (; ; ) {
            int c = reader.read();
            if (chars.indexOf(c) == -1) {
                ungetc(c);
                break;
            }
            result.append((char) c);
        }
        return result.toString();
    }

    private char readFully () throws IOException {
        int c = reader.read();
        if (c == -1) {
            throw new EOFException();
        }
        return (char) c;
    }

    private void skipWhitespaces () throws IOException {
        for (; ; ) {
            int chr = reader.read();
            if (!isWhitespace(chr)) {
                if (chr == '%') {
                    new BufferedReader(reader, 1).readLine();
                    continue;
                }
                if (chr == '/') {
                    int c = reader.read();
                    if (c == '*') {
                        while (readFully() != '*' || readFully() != '/') ;
                        continue;
                    }
                    ungetc(c);
                }
                ungetc(chr);
                break;
            }
        }
    }
//
//    public void takeBack () {
//
//    }

    private void ungetc ( int c ) throws IOException {
        if (c != -1) {
            reader.unread(c);
        }
    }

    public PlToken getNextToken () throws IOException, ParseException {
        return next(true);
    }
}
