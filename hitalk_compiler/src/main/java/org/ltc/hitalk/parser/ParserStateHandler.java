package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.HtPrologParser.MIN_PRIORITY;
import static org.ltc.hitalk.parser.ParserState.EXPR_A;
import static org.ltc.hitalk.parser.PlToken.TokenKind;
import static org.ltc.hitalk.parser.PlToken.newToken;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.*;

/**
 * exprA(n) ::=
 * exprB(n)
 * { op(yfx,n) exprA(n-1) |
 * op(yf,n) }*
 * ============================
 * exprB(n) ::=
 * exprC(n-1)
 * { op(xfx,n) exprA(n-1) |
 * op(xfy,n) exprA(n) |
 * op(xf,n) }*
 * // exprC is called parseLeftSide in the code
 * ============================
 * exprC(n) ::=
 * '-' integer | '-' float |
 * op( fx,n ) exprA(n-1) |
 * op( hx,n ) exprA(n-1) |
 * op( fy,n ) exprA(n) |
 * op( hy,n ) exprA(n) |
 * exprA(n)
 * ============================
 * expr_A0 ::=
 * integer |
 * float |
 * atom |
 * variable |
 * list     |
 * functor
 * ============================
 * functor ::=
 * functorName
 * args
 * ============================
 * functorName ::=
 * expr_A0
 * ============================
 * args ;;=
 * '(' sequence ')'
 * ----------------------------
 * list ::=
 * '[' sequence ']'
 * ===========================
 * block ::=
 * '('  { exprA(1200) }* ')'
 * ============================
 * bypass_blk ::=
 * '{' { exprA(1200) }* '}'
 * ============================
 * op(type,n) ::=
 * atom | { symbol }+
 * ============================
 * sequence ::=
 * [ heads tail ]
 * ============================
 */
abstract public class ParserStateHandler extends StateRecord implements IStateHandler {
    protected final HtPrologParser parser;
    protected final ParserState state;
    protected final State stateRecState;
    protected final EnumSet<Associativity> assocs;
    protected final EnumSet<DirectiveKind> dks;
    protected final int currPriority;
    protected final PlToken token;

    /**
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     */
    public ParserStateHandler(HtPrologParser parser,
                              ParserState state,
                              State stateRecState,
                              EnumSet<Associativity> assocs,
                              EnumSet<DirectiveKind> dks,
                              int currPriority,
                              PlToken token) {

        super(state, stateRecState, assocs, dks, currPriority, token);
        this.parser = parser;
    }

    public final StateRecord getStateRecord() {
        return this;
    }

    /**
     * @param h
     * @return
     */
    public static IStateHandler create(ParserStateHandler h) {
        IStateHandler handler;
        switch (h.getParserState()) {
            case START:
                handler = new NopHandler(h.getParser(), h.getParserState(), h.getStateRecordState());
                break;
            case FINISH:
                handler = new NopHandler(h.getParser(), h.getParserState(), h.getStateRecordState());
                break;
            case EXPR_A:
                handler = new ExprAHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_B:
                handler = new ExprBHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_C:
                handler = new ExprCHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0:
                handler = new ExprA0Handler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_BRACE:
                handler = new ExprA0BraceHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_BRACKET:
                handler = new ExprA0BracketHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_ARGS:
                handler = new ExprA0ArgsHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_HEADS:
                handler = new ExprA0HeadsHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_TAIL:
                handler = new ExprA0TailHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case SEQUENCE:
                handler = new SequenceHandler(
                        h.getParser(),
                        h.getParserState(),
                        h.getStateRecordState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case NOP:
                handler = new NopHandler(h.getParser(), h.getParserState(), h.getStateRecordState());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + h.getParserState());
        }

        return handler;
    }


    private HtPrologParser getParser() {
        return parser;
    }

    public StateRecord newState() {
        return new StateRecord(state, stateRecordState, assocs, dks, currPriority, token);
    }

    /**
     *
     */
    protected static class ExprA0BracketHandler extends ParserStateHandler {

        public ExprA0BracketHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }


        public void prepareState() throws Exception {
            super.prepareState();
            newState();
        }

        public void completeState() throws Exception {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }

        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    public static class ExprAHandler extends ParserStateHandler {
        ExprA0Handler handlerA0;
        ExprAnHandler handlerAn;

        public ExprAHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);

            handlerA0 = new ExprA0Handler(parser, state, stateRecordState, assocs, dks, currPriority, token);
            handlerAn = new ExprAnHandler(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public void prepareState() {
            if (getCurrPriority() == 0) {
                handlerA0.prepareState();//fixme
            } else {
                handlerAn.prepareState();//fixme
            }
        }

        public void completeState() throws Exception {

        }

        public void exitState(ParserState state) throws Exception {
        }


        public void repeat(Consumer<IStateHandler> action) {

        }

        /**
         *
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            final Set<IdentifiedTerm> result = new HashSet<>();
            for (Associativity assoc : getAssocs()) {
                result.addAll(appContext.getOpTable().getOperators(name, assoc, getCurrPriority()));
            }
            return result;
        }
    }

    /**
     *
     */
    public static class ExprBHandler extends ParserStateHandler {

        public ExprBHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public void prepareState() {

        }

        public void completeState() throws Exception {

        }

        public void exitState() {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }


        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    public static class ExprCHandler extends ParserStateHandler {

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param currPriority
         * @param token
         */
        public ExprCHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public void prepareState() {

        }

        public void completeState() throws Exception {

        }

        public void exitState() {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }


        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    public static class ExprAnHandler extends ExprAHandler {
        IdentifiedTerm leftSide;

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param currPriority
         * @param token
         */
        public ExprAnHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public void prepareState() {

        }

        public void completeState() throws Exception {

        }

        public void exitState() throws Exception {
            PlLexer lexer = appContext.getTokenSource();
            PlToken token = lexer.readToken(true);
            IOperatorTable operatorTable = appContext.getOpTable();
            for (; isOperator(token); token = lexer.readToken(true)) {
                int priorityYFX = operatorTable.getPriority(token.image, yfx);
                int priorityYF = operatorTable.getPriority(token.image, yf);
                //YF and priorityYFX has a higher priority than the left side expr and less then top limit
                // if (YF < leftSide.getPriority() && YF > PlDynamicOperatorParser.OP_HIGH) YF = -1;

                if (priorityYF < leftSide.getPriority() || priorityYF > this.currPriority) {
                    priorityYF = -1;
                }
                // if (priorityYFX < leftSide.getPriority() && priorityYFX > MAX_PRIORITY) priorityYFX = -1;
                if (priorityYFX < leftSide.getPriority() || priorityYFX > this.currPriority) {
                    priorityYFX = -1;
                }
                //priorityYFX has getPriority() over YF
                if (priorityYFX >= priorityYF && priorityYFX >= MIN_PRIORITY) {
                    newState(EXPR_A,
                            assocs,
                            dks,
                            priorityYFX - 1,
                            token);
                    if (parser.getLastTerm() != null) {
                        leftSide = new IdentifiedTerm(
                                token.image,
                                yfx,
                                priorityYFX,
                                leftSide.getResult(),
                                ((IdentifiedTerm) parser.getLastTerm()).getResult());
                    }
                } else {
                    //either YF has priority over priorityYFX or priorityYFX failed
                    if (priorityYF >= MIN_PRIORITY) {
                        leftSide = new IdentifiedTerm(
                                token.image,
                                yf,
                                priorityYF,
                                leftSide.getResult());
                    }
                }
            }
        }

        /**
         * @param token
         * @return
         */
        private boolean isOperator(PlToken token) {
            final String name = token.getImage();
            final Set<IdentifiedTerm> ops = appContext.getOpTable().getOperators(name);

            return ops.stream().anyMatch(op -> op.getTextName().equals(name));
        }

    }

    public void repeat(Consumer<IStateHandler> action) {

    }

    /**
     *
     */
    public static class ExprA0Handler extends ExprAHandler {

        public ExprA0Handler(HtPrologParser parser,
                             ParserState state,
                             State stateRecordState,
                             EnumSet<Associativity> assocs,
                             EnumSet<DirectiveKind> dks,
                             int currPriority,
                             PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public void completeState() throws Exception {

        }

        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    public static class NopHandler extends ParserStateHandler {

        /**
         * @param state
         * @param stateRecordState
         */
        public NopHandler(HtPrologParser parser, ParserState state, State stateRecordState) {
            super(parser,
                    state,
                    stateRecordState,
                    of(x),
                    of(DirectiveKind.DK_IF),
                    0,
                    newToken(TokenKind.TK_ANY_CHAR));
        }

//        private static boolean test(TokenKind tokenKind) {
//            return false;
//        }

        public void prepareState() {

        }

        public void completeState() throws Exception {

        }

        public void exitState(ParserState state) {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }


        public void repeat(Consumer<IStateHandler> action) {

        }


    }

    /**
     *
     */
    public static class ExprA0BraceHandler extends ParserStateHandler {

        public ExprA0BraceHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public StateRecord newState() {
            return null;
        }

        public void prepareState() {

        }

        public void completeState() throws Exception {

        }

        public void exitState(ParserState state) {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }


        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    public static class SequenceHandler extends ParserStateHandler {

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param currPriority
         * @param token
         */
        public SequenceHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public void prepareState() {

        }

        public void completeState() throws Exception {

        }

        public void exitState() {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }

        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    public static class ExprA0ArgsHandler extends ParserStateHandler {

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param currPriority
         * @param token
         */
        public ExprA0ArgsHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public StateRecord newState() {
            return null;
        }

        public void prepareState() {

        }

        public void completeState() throws Exception {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }

        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    public static class ExprA0HeadsHandler extends ParserStateHandler {

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param currPriority
         * @param token
         */
        public ExprA0HeadsHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public void prepareState() {

        }

        public void completeState() throws Exception {

        }

        public void exitState() {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }


        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    public static class ExprA0TailHandler extends ParserStateHandler {

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param currPriority
         * @param token
         */
        public ExprA0TailHandler(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
            super(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        private static boolean test(IStateHandler iStateHandler) {
            return true;
        }

        public void prepareState() throws Exception {
            newState(parser, state, stateRecordState, assocs, dks, currPriority, token);
        }

        public void completeState() throws Exception {

        }

        public void newState(
                HtPrologParser parser,
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
        }

        public void exitState() {

        }

        /**
         * @param name
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            return null;
        }

        public void repeat(Consumer<IStateHandler> action) {
            action.accept(this);
        }
    }

    protected StateRecord newState(ParserState state,
                                   State stateRecordState,
                                   EnumSet<Associativity> assocs,
                                   EnumSet<DirectiveKind> dks,
                                   int currPriority,
                                   PlToken token) throws Exception {
        final StateRecord sr = new StateRecord(state, stateRecordState, assocs, dks, currPriority, token);
        getStates().push(sr);

        return sr;
    }

    @Override
    public Deque<StateRecord> getStates() throws Exception {
        return appContext.getParser();
    }

    public void completeState() throws Exception {

    }

    public boolean isEndOfTerm(HtPrologParser parser) {
        return token.kind == TokenKind.TK_DOT ||
                parser.getParentheses() == 0 && parser.getBrackets() == 0 && parser.getBraces() == 0 &&
                        parser.getSquotes() % 2 == 0 && parser.getDquotes() % 2 == 0 && parser.getBquotes() % 2 == 0;

    }
}