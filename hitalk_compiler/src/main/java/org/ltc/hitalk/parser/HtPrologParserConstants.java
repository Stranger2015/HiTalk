package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken;

public interface HtPrologParserConstants {
    int EOF = 0;
    PlToken.TokenKind PERIOD = 9;
    PlToken.TokenKind LPAREN = 10;
    PlToken.TokenKind RPAREN = 11;
    PlToken.TokenKind LBRACKET = 12;
    PlToken.TokenKind RBRACKET = 13;
    int DQUOTE = 14;
    int QUOTE = 15;
    PlToken.TokenKind CONS = 16;
    PlToken.TokenKind INTEGER_LITERAL = 17;
    int DECIMAL_LITERAL = 18;
    int HEX_LITERAL = 19;
    PlToken.TokenKind FLOATING_POINT_LITERAL = 20;
    int DECIMAL_FLOATING_POINT_LITERAL = 21;
    int DECIMAL_EXPONENT = 22;
    int CHARACTER_LITERAL = 23;
    PlToken.TokenKind STRING_LITERAL = 24;
    PlToken.TokenKind VAR = 25;
    PlToken.TokenKind FUNCTOR = 26;
    PlToken.TokenKind ATOM = 27;
    int NAME = 28;
    int SYMBOLIC_NAME = 29;
    int DIGIT = 30;
    int ANYCHAR = 31;
    int LOCASE = 32;
    int HICASE = 33;
    int SYMBOL = 34;
    PlToken.TokenKind INFO = 35;
    PlToken.TokenKind TRACE = 36;
    PlToken.TokenKind USER = 37;
    PlToken.TokenKind LBRACE = 38;
    PlToken.TokenKind RBRACE = 39;
    int BOF = 40;

    int DEFAULT = 0;
    int WITHIN_COMMENT = 1;

    String[] tokenImage = {"<EOF>", "\" \"", "\"\\t\"", "\"\\n\"", "\"\\r\"", "\"\\f\"", "\"/*\"", "\"*/\"", "<token of kind 8>", "\".\"", "\"(\"", "\")\"", "\"[\"", "\"]\"", "\"\\\"\"", "\"\\\'\"", "\"|\"", "<INTEGER_LITERAL>", "<DECIMAL_LITERAL>", "<HEX_LITERAL>", "<FLOATING_POINT_LITERAL>", "<DECIMAL_FLOATING_POINT_LITERAL>", "<DECIMAL_EXPONENT>", "<CHARACTER_LITERAL>", "<STRING_LITERAL>", "<VAR>", "<FUNCTOR>", "<ATOM>", "<NAME>", "<SYMBOLIC_NAME>", "<DIGIT>", "<ANYCHAR>", "<LOCASE>", "<HICASE>", "<SYMBOL>", "<INFO>", "<TRACE>", "<USER>", "\"{\"", "\"}\"", "<BOF>"};
}
