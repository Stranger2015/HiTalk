package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.handlers.*;

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
    OP(Operator.class),
    EXPR_A0(ExprA0.class),
    EXPR_AN(ExprAn.class),
    EXPR_A0_ARGS(Args.class),
    EXPR_A0_BRACE(Brace.class),
    EXPR_A0_BRACKET(Bracket.class),
    BLOCK(Block.class),
    TAIL(Tail.class),
    LIST(DottedPair.class),

    SIMPLE_SEQUENCE(SimpleSeq.class),
    LIST_SEQUENCE(ListSeq.class),

    EXPR_B(ExprB.class),

    EXPR_C(ExprC.class), START(StartRule.class);


    private Class<?> ruleClass;
    private final Object[] args;

    public Class<?> getRuleClass() {
        return ruleClass;
    }

    ParserState(Class<?> ruleClass, Object... args) {
        this.ruleClass = ruleClass;
        this.args = args;
    }
}
