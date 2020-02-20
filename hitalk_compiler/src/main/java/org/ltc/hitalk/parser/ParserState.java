package org.ltc.hitalk.parser;

public enum ParserState {
    START,
    FINISH,
    EXPR_A,
    EXPR_B,
    EXPR_C,
    EXPR_A0, EXPR_A0_BRACE, EXPR_A0_BRACKET, EXPR_A0_ARGS, EXPR_A0_HEADS, EXPR_A0_TAIL,
}
