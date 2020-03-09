package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.ParserStateHandler.ExprAnHandler;

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
    EXPR_A(ExprAHandler.class),
    EXPR_A0(ExprA0Handler.class),
    EXPR_AN(ExprAnHandler.class),
    EXPR_A0_ARGS(ExprA0ArgsHandler.class),
    EXPR_A0_BRACE(ExprA0BraceHandler.class),
    EXPR_A0_BRACKET(ExprA0BracketHandler.class),
    EXPR_A0_HEADS(ExprA0HeadsHandler.class),
    EXPR_A0_TAIL(ExprA0TailHandler.class),
    SEQUENCE(SequenceHandler.class),

    EXPR_B(ExprBHandler.class),

    EXPR_C(ExprCHandler.class);

    private final Class<?> handler;

    public Class<?> getHandlerClass() {
        return handler;
    }

    ParserState(Class<?> handler) {
        this.handler = handler;
    } //    public ParserState[] getParserStates() {

    private static class ExprAHandler {
    }

    private static class ExprA0Handler {
    }

    private static class ExprA0Args {
    }

    private static class ExprA0ArgsHandler {
    }

    private static class ExprA0BraceHandler {
    }

    private static class ExprA0BracketHandler {
    }

    private static class ExprA0HeadsHandler {
    }

    private static class ExprA0TailHandler {
    }

    private static class SequenceHandler {
    }

    private static class ExprBHandler {
    }

    private static class ExprCHandler {
    }
    //        return parserStates;
    //    }
    //    public Boolean getOr() {
    //        return or;
    //    }
    //    public Substate getSubstate() {
    //        return substate;
    //    }
    //    public enum Substate {
    //        ENTERING,
    //        EXITING
    //    }
    /*boolean or, ParserState... parserStates*/ //        this.or = or;
    //        this.parserStates = parserStates;
    ,


    SEQUENCE


}
