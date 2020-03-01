package org.ltc.hitalk.parser;

/**
 * // * BNF part 2: Parser
 * //================================================================================================================
 * //   term ::=
 * //      exprA(1200)
 * //============================
 * //   exprA(n) ::=
 * //      exprB(n) { op(yfx,n) exprA(n-1) | op(yf,n) }*
 * //============================
 * //  exprB(n) ::=
 * //      exprC(n-1) { op(xfx,n) exprA(n-1) | op(xfy,n) exprA(n) | op(xf,n) }*
 * //   // exprC is called parseLeftSide in the code
 * //============================
 * //  exprC(n) ::=
 * //      '-/+' integer | '-/+' float |
 * //      op( fx,n ) exprA(n-1) |
 * //      op( hx,n ) exprA(n-1) |
 * //      op( fy,n ) exprA(n) |
 * //      op( hy,n ) exprA(n) |
 * //                 exprA(n)
 * //=================================================================================================================
 * //  expr_A0 ::=
 * //      integer |
 * //      float |
 * //      atom |
 * //      variable |
 * //      list     |
 * //      functor
 * //============================
 * // functor ::= functorName args
 * //============================
 * // functorName ::= expr_A0
 * // ============================
 * // args ;;= '(' sequence ')'
 * //----------------------------
 * // list ::= '[' sequence ']'
 * //===========================
 * //     block ::= '('  { exprA(1200) }* ')'  //block
 * //============================
 * //     bypass_blk ::= '{' { exprA(1200) }* '}'
 * //============================
 * //     op(type,n) ::= atom | { symbol }+
 * //============================
 * //     sequence ::= [ heads tail ]
 * //============================
 * //     heads ::= [ exprA(1200) { ',' exprA(1200) }* ]
 * //============================
 * //     tail ::=  [ '|' (variable | list) ]
 */
public enum ParserState {
    EXPR_C,
//    EXPR_C_EXIT,

    EXPR_A0_BRACE,
//    EXPR_A0_BRACE_EXIT,

    EXPR_A0_BRACKET,
//    EXPR_A0_BRACKET_EXIT,

    EXPR_A0_ARGS,
//    EXPR_A0_ARGS_EXIT,

    EXPR_A0_HEADS,
//    EXPR_A0_HEADS_EXIT,

    EXPR_A0_TAIL,
//    EXPR_A0_TAIL_EXIT,

    SEQUENCE,
//    SEQUENCE_EXIT,

    EXPR_A0(true,
            EXPR_A0_ARGS,
            EXPR_A0_BRACE,
            EXPR_A0_BRACKET,
            EXPR_A0_HEADS,
            EXPR_A0_TAIL
    ),
//    EXPR_A0_EXIT,

    EXPR_B(EXPR_C),
//    EXPR_B_EXIT,

    EXPR_A(EXPR_B),
//    EXPR_A_EXIT,


    START(true, EXPR_A, EXPR_A0),
    FINISH,


    NOP;

    private final ParserState[] parserStates;

    ParserState(ParserState... parserStates) {
        this.parserStates = parserStates;
    }

    private Boolean or;

    ParserState(boolean or, ParserState... parserStates) {
        this.or = or;
        this.parserStates = parserStates;
    }

    public ParserState[] getParserStates() {
        return parserStates;
    }

    public Boolean getOr() {
        return or;
    }
}
