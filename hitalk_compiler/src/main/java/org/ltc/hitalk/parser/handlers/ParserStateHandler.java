package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.IdentifiedTerm;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind.DK_IF;
import static org.ltc.hitalk.parser.ParserState.EXPR_AN;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_BOF;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.x;

///*****************************************************************
// * query ::=
// *    exprAn(1200)
// * ---------------------------------------------------------------
// * exprAn(n) ::=
// +    n > 0
// *    exprB(n)
// *    {
//          op(yfx,n) exprA(n-1) | op(yf,n)
//      }*
// * ============================
// * exprB(n) ::=
// *    exprC(n-1)
// *    {
//        op(xfx,n) -> exprAn(n-1) |
// *      op(xfy,n) -> exprAn(n) |
// *      op(xf,n)  -> true
//      }*
// * // exprC is called parseLeftSide in the code
// * ============================
// * exprC(n) ::=
// *    '-' integer | '-' float |
// *    op( fx, n ) -> exprAn(n-1) |
// *    op( hx, n ) -> exprAn(n-1) |
// *    op( fy, n ) -> exprAn(n) |
// *    op( hy, n ) -> exprAn(n) |
// *    true        -> exprAn(n)
// * ============================
// * expr_A(0) ::=
// *    integer |
// *    float |
// *    atom |
// *    variable |
// *    list     |
// *    functor
// * ============================
// * functor ::=
// *    functorName
// *    args
// * ============================
// * functorName ::=
// *    expr_A0
// * ============================
// * args ::=
// *    '(' list_seq ')'
// * ============================
// * list ::=
// *    '[' list_seq ']'
// * ===========================
// * block ::=
// *    '('  simple_seq ')'
// * ============================
// * bypass_blk ::=
// *    '{' simple_seq '}'
// *    //'{' { exprAn(1200) "," }* '}'
// *    //'{' [ exprAn(1200) [","] ] bypass_blk '}'
// * ============================
// * op(type,n) ::=
// *    atom | { symbol }+
// * ============================
// * list_seq ::=
// *    [ heads tail ]
// * ============================
//   simple_seq ::=
//      [ exprAn(1200) [ "," ]] simple_seq
//===============================
//   heads ::=
//      simple_seq
// ===========================================
//   tail ::=
//       "|" list | variable
// ===========================================
abstract public
class ParserStateHandler extends StateRecord implements IStateHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private static ParserState state;
    private static EnumSet<Associativity> assocs;
    private static EnumSet<DirectiveKind> dks;
    private static int currPriority;
    private static PlToken token;

    protected HtPrologParser parser = appContext.getParser();
    protected ITermFactory termFactory = appContext.getTermFactory();
    protected boolean isPushed;

    @SuppressWarnings("JavaReflectionInvocation")
    public static IStateHandler create(Class<?> handler, Object... params) {
        final IStateHandler result;
        try {
            result = (IStateHandler) handler.getConstructor().newInstance(params);
        } catch (ReflectiveOperationException e) {
            throw new ExecutionError();
        }
        result.push(result);
        return result;
    }

    public static IStateHandler create(StateRecord sr) {
        return create(sr.getParserState().getRuleClass(),
                sr.getParserState(),
                sr.getAssocs(),
                sr.getDks(),
                sr.getCurrPriority(),
                sr.getToken());
    }

    public ParserStateHandler(ParserState state,
                              EnumSet<Associativity> assocs,
                              EnumSet<DirectiveKind> dks,
                              int currPriority,
                              PlToken token) throws Exception {
        super(state, assocs, dks, currPriority, token);
    }

    /**
     * @return
     */
//    @Override
//    public IStateHandler completeState(StateRecord sr) throws Exception {
//        doCompleteState(sr);
//        return IStateHandler.super.completeState(sr);
//    }

//    @Override
//    public void doCompleteState(StateRecord sr) throws Exception {
//
//    }

    @Override
    public ParserState getParserState() {
        return state;
    }

    /**
     * @param name
     * @param handler
     */
    @Override
    public Set<IdentifiedTerm> tryOperators(String name, IStateHandler handler) {
        return IStateHandler.super.tryOperators(name, handler);

    }

    @Override
    public void setCurrPriority(int currPriority) {
        this.currPriority = currPriority;
    }

    @Override
    public void setToken(PlToken token) {
    }

    @Override
    public void setDks(EnumSet<DirectiveKind> dks) {

    }

    /**
     * @param handler
     */
    public final void push(IStateHandler handler) {
        parser.states.push(handler);
        isPushed = true;
    }

    /**
     * @return
     */
    public final IStateHandler pop() {
        return parser.states.pop();
    }

    @Override
    public boolean isPushed() {
        return isPushed;
    }

    /**
     *
     */
    @Override
    public HtPrologParser getParser() {
        return parser;
    }

//    /**
//     * @param token
//     * @return
//     */
//    public boolean isOperator(PlToken token) {
//        final String name = token.getImage();
//        final Set<IdentifiedTerm> ops = appContext.getOpTable().getOperators(name);
//
//        return ops.stream().anyMatch(op -> op.getTextName().equals(name));
//    }

    /**
     * Parses and returns a valid 'leftside' of an expression.
     * If the left side starts with a prefix, it consumes other expressions with a lower priority than itself.
     * If the left side does not have a prefix it must be an expr0.
     *
     * @param currPriority operators with a higher priority than this will effectively end the expression
     * @param token
     * @return a wrapper of: 1. term correctly structured and 2. the priority of its root operator
     * @throws InvalidTermException
     */
//    public ITerm parseLeftSide(int currPriority, PlToken token) throws Exception {
////        1. prefix expression
////        token = getLexer().readToken(true);
//        if (isOperator(token)) {
//            int priorityFX = parser.getOptable().getPriority(token.image, fx);
//            int priorityFY = parser.getOptable().getPriority(token.image, fy);
//            if (priorityFY == 0) {
//                priorityFY = -1;
//            }
//            if (token.image.equals("-") || token.image.equals("+")) {
//                PlToken t = parser.getLexer().readToken(true);
//                if (t.isNumber()) {
//                    return termFactory.createNumber(token.image);
//                } else {
//                    parser.getLexer().unreadToken(t);
//                }
//                //check that no operator has a priority higher than permitted
//                if (priorityFY > currPriority) {
//                    priorityFY = -1;
//                }
//                if (priorityFX > currPriority) {
//                    priorityFX = -1;
//                }
//                //priorityFX has priority over priorityFY
//                boolean haveAttemptedFX = false;
//                if (priorityFX >= priorityFY && priorityFX >= MIN_PRIORITY) {
//                    if (parser.getLastTerm() != null) {
//                        return new IdentifiedTerm(
//                                token.image,
//                                fx,
//                                priorityFX - 1,
//                                ((IdentifiedTerm) parser.getLastTerm()).getResult());
//                    } else {
//                        haveAttemptedFX = true;
//                    }
//                }
//                //priorityFY has priority over priorityFX, or priorityFX has failed
//                if (priorityFY >= MIN_PRIORITY) {
//                    if (parser.getLastTerm() != null) {
//                        return new IdentifiedTerm(
//                                token.image,
//                                fy,
//                                priorityFY,
//                                ((IdentifiedTerm) parser.getLastTerm()).getResult());
//                    }
//                }
//                //priorityFY has priority over priorityFX, but priorityFY failed
//                if (!haveAttemptedFX && priorityFX >= MIN_PRIORITY) {
//                    if (parser.getLastTerm() != null) {
//                        return new IdentifiedTerm(
//                                token.image,
//                                fx,
//                                priorityFX - 1,
//                                ((IdentifiedTerm) parser.getLastTerm()).getResult());
//                    }
//                }
//            }
//        }
//        return parser.getLastTerm();
//    }

    public <T extends ParserStateHandler> void accept(IStateVisitor<T> visitor) throws Exception {
        visitor.visit(new ExprAn(
                EXPR_AN,
                of(x),
                of(DK_IF),
                1200,
                PlToken.newToken(TK_BOF)));
    }

}