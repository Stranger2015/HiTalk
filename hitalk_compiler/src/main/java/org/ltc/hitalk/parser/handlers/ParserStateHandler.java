package org.ltc.hitalk.parser.handlers;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.IdentifiedTerm;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

import static org.ltc.hitalk.compiler.bktables.IApplication.getLogger;
import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.parser.HtPrologParser.MIN_PRIORITY;
import static org.ltc.hitalk.parser.StateRecord.State.COMPLETING;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.fx;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.fy;

///*****************************************************************
// * exprA(n) ::=
// +    n > 0
// *    exprB(n)
// *    { op(yfx,n) exprA(n-1) | op(yf,n) }*
// * ============================
// * exprB(n) ::=
// *    exprC(n-1)
// *    { op(xfx,n) exprA(n-1) |
// *        op(xfy,n) exprA(n) |
// *        op(xf,n) }*
// * // exprC is called parseLeftSide in the code
// * ============================
// * exprC(n) ::=
// *    '-' integer | '-' float |
// *    op( fx,n ) exprA(n-1) |
// *    op( hx,n ) exprA(n-1) |
// *    op( fy,n ) exprA(n) |
// *    op( hy,n ) exprA(n) |
// *    exprA(n)
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
// *    '(' sequence ')'
// * ----------------------------
// * list ::=
// *    '[' sequence ']'
// * ===========================
// * block ::=
// *    '('  { exprA(1200) }* ')'
// * ============================
// * bypass_blk ::=
// *    '{' { exprA(1200) }* '}'
// * ============================
// * op(type,n) ::=
// *    atom | { symbol }+
// * ============================
// * sequence ::=
// *    [ heads tail ]
// * ============================
// */
abstract public
class ParserStateHandler extends StateRecord implements IStateHandler {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private static ParserState state;
    private static EnumSet<Associativity> assocs;
    private static EnumSet<DirectiveKind> dks;
    private static State stateRecordState;
    private static int currPriority;
    private static PlToken token;

    protected HtPrologParser parser = appContext.getParser();
    private ITermFactory termFactory = appContext.getTermFactory();

    @SuppressWarnings("JavaReflectionInvocation")
    public static IStateHandler create(Class<?> handler, Object... params) {
        final IStateHandler result;
        try {
            result = (IStateHandler) handler.getConstructor().newInstance(params);
        } catch (ReflectiveOperationException e) {
            throw new ExecutionError();
        }

        return result;
    }

    protected IStateHandler create(StateRecord sr) {
        return create(sr.getParserState().getHandlerClass(),
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
        super(state, stateRecordState, assocs, dks, currPriority, token);
    }

    public final IStateHandler handleState(StateRecord sr) throws Exception {
        IStateHandler result = null;
        switch (sr.getStateRecordState()) {
            case PREPARING:
                prepareState(sr);
                break;
            case COMPLETING:
                result = completeState(sr);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + getStateRecordState());
        }

        return result;
    }

    public void doPrepareState(StateRecord sr) throws Exception {

    }

    /**
     * @return
     */
    public IStateHandler completeState(StateRecord sr) throws Exception {
        doCompleteState(sr);
        return IStateHandler.super.completeState(sr);
    }

    public void doCompleteState(StateRecord sr) throws Exception {

    }

    public ParserState getParserState() {
        return state;
    }

    public void setCurrPriority(int currPriority) {
        this.currPriority = currPriority;
    }

    public void setToken(PlToken token) {

    }

    public void setDks(EnumSet<DirectiveKind> dks) {

    }

    public final StateRecord getStateRecord() {
        return this;
    }

    /**
     * @param handler
     */
    public final void push(IStateHandler handler) {
        parser.states.push(handler);
    }

    /**
     * @return
     */
    public final IStateHandler pop() {
        return parser.states.pop();
    }

    /**
     * @return
     */
    public final void prepareState(StateRecord sr) throws Exception {
        getLogger().info("Preparing state " + sr.getParserState());
        IStateHandler.super.prepareState(sr);
        doPrepareState(sr);
        sr.setStateRecordState(COMPLETING);
    }

    /**
     *
     */
    @Override
    public HtPrologParser getParser() {
        return parser;
    }

    /**
     * @param token
     * @return
     */
    public boolean isOperator(PlToken token) {
        final String name = token.getImage();
        final Set<IdentifiedTerm> ops = appContext.getOpTable().getOperators(name);

        return ops.stream().anyMatch(op -> op.getTextName().equals(name));
    }

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
    public ITerm parseLeftSide(int currPriority, PlToken token) throws Exception {
//        1. prefix expression
//        token = getLexer().readToken(true);
        if (isOperator(token)) {
            int priorityFX = parser.getOptable().getPriority(token.image, fx);
            int priorityFY = parser.getOptable().getPriority(token.image, fy);
            if (priorityFY == 0) {
                priorityFY = -1;
            }
            if (token.image.equals("-") || token.image.equals("+")) {
                PlToken t = parser.getLexer().readToken(true);
                if (t.isNumber()) {
                    return termFactory.createNumber(token.image);
                } else {
                    parser.getLexer().unreadToken(t);
                }
                //check that no operator has a priority higher than permitted
                if (priorityFY > currPriority) {
                    priorityFY = -1;
                }
                if (priorityFX > currPriority) {
                    priorityFX = -1;
                }
                //priorityFX has priority over priorityFY
                boolean haveAttemptedFX = false;
                if (priorityFX >= priorityFY && priorityFX >= MIN_PRIORITY) {
//                    IdentifiedTerm found = exprA(priorityFX - 1, rDelims);    //op(fx, n) exprA(n - 1)
//                    newState(EXPR_A,
//                            assocs,
//                            directiveKinds,
//                            rDelims,
//                            priorityFX - 1,
//                            token);

                    if (parser.getLastTerm() != null) {
                        return new IdentifiedTerm(
                                token.image,
                                fx,
                                priorityFX - 1,
                                ((IdentifiedTerm) parser.getLastTerm()).getResult());
                    } else {
                        haveAttemptedFX = true;
                    }
                }
                //priorityFY has priority over priorityFX, or priorityFX has failed
                if (priorityFY >= MIN_PRIORITY) {
//                    IdentifiedTerm found = exprA(priorityFY, rDelims); //op(fy,n) exprA(1200)  or   op(fy,n) exprA(n)
//                    newState(EXPR_A,
//                            assocs,
//                            directiveKinds,
//                            rDelims,
//                            priorityFY,
//                            token);

                    if (parser.getLastTerm() != null) {
                        return new IdentifiedTerm(
                                token.image,
                                fy,
                                priorityFY,
                                ((IdentifiedTerm) parser.getLastTerm()).getResult());
                    }
                }
                //priorityFY has priority over priorityFX, but priorityFY failed
                if (!haveAttemptedFX && priorityFX >= MIN_PRIORITY) {
//                    IdentifiedTerm found = exprA(priorityFX - 1, rDelims);    //op(fx, n) exprA(n - 1)
//                    newState(EXPR_A,
//                            assocs,
//                            directiveKinds,
//                            rDelims,
//                            priorityFX - 1,
//                            token);

                    if (parser.getLastTerm() != null) {
                        return new IdentifiedTerm(
                                token.image,
                                fx,
                                priorityFX - 1,
                                ((IdentifiedTerm) parser.getLastTerm()).getResult());
                    }
                }
            }
            //2. expr0
            return parser.getLastTerm();
        }

        return parser.getLastTerm();
    }

}