package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.core.BaseApp.appContext;
import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.parser.HtPrologParser.MIN_PRIORITY;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.*;

/**
 * exprA(n) ::=
 * ____exprB(n)
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
 * args ::=
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

    /**
     * @param assocs
     * @param dks
     * @param currPriority
     * @param token
     */
    public ParserStateHandler(ParserState state,
                              EnumSet<Associativity> assocs,
                              EnumSet<DirectiveKind> dks,
                              int currPriority,
                              PlToken token) throws Exception {

        super(state, assocs, dks, currPriority, token);
        this.parser = appContext.getParser();
    }

    public static IStateHandler create(StateRecord stateRecord) throws Exception {
        return create(
                stateRecord.getParserState(),
                stateRecord.getAssocs(),
                stateRecord.getDks(),
                stateRecord.getCurrPriority(),
                stateRecord.getToken());
    }

    public final StateRecord getStateRecord() {
        return this;
    }

    /**
     * @return
     */
    public static IStateHandler create(ParserState state,
                                       EnumSet<Associativity> assocs,
                                       EnumSet<DirectiveKind> dks,
                                       int currPriority,
                                       PlToken token) throws Exception {
        IStateHandler handler;
        switch (state) {
            case EXPR_A:
                handler = new ExprAHandler(
                        state,
                        of(yfx, yf),
                        dks,
                        currPriority,
                        token);
                break;
            case EXPR_B:
                handler = new ExprBHandler(
                        state,
                        of(xfx, xfy, xf),
                        dks,
                        currPriority,
                        token);
                break;
            case EXPR_C:
                handler = new ExprCHandler(
                        state,
                        of(x),
                        dks,
                        currPriority,
                        token);
                break;
            case EXPR_A0:
                handler = new ExprA0Handler(
                        state,
                        of(x),
                        dks,
                        currPriority,
                        token);
                break;
            case EXPR_A0_BRACE:
                handler = new ExprA0BraceHandler(
                        state,
                        of(x),
                        dks,
                        currPriority,
                        token);
                break;
            case EXPR_A0_BRACKET:
                handler = new ExprA0BracketHandler(
                        state,
                        of(x),
                        dks,
                        currPriority,
                        token);
                break;
            case EXPR_A0_ARGS:
                handler = new ExprA0ArgsHandler(
                        state,
                        of(x),
                        dks,
                        currPriority,
                        token);
                break;
            case EXPR_A0_HEADS:
                handler = new ExprA0HeadsHandler(
                        state,
                        of(x),
                        dks,
                        currPriority,
                        token);
                break;
            case EXPR_A0_TAIL:
                handler = new ExprA0TailHandler(
                        state,
                        of(x),
                        dks,
                        currPriority,
                        token);
                break;
            case SEQUENCE:
                handler = new SequenceHandler(
                        state,
                        of(x),
                        dks,
                        currPriority,
                        token);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }

        return handler;
    }

    /**
     * @param handler
     */
    public void push(IStateHandler handler) {
        parser.states.push(handler);
    }

    /**
     * @return
     */
    public IStateHandler pop() {
        return null;
    }

    public final IStateHandler prepareState() throws Exception {
        IStateHandler handler = IStateHandler.super.prepareState();
        doPrepareState();
        return handler;
    }

    /**
     *
     */
    protected void doPrepareState() throws Exception {

    }

    /**
     *
     */
    protected void doCompleteState() throws Exception {

    }


    public final IStateHandler handleState() throws Exception {
        IStateHandler result = null;
        final StateRecord sr = getStateRecord();
        switch (sr.stateRecordState) {
            case PREPARING:
                result = this.prepareState();
                break;
            case COMPLETING:
                result = this.completeState();
                break;
        }

        return result;
    }


    public final IStateHandler completeState() throws Exception {
        IStateHandler s = IStateHandler.super.completeState();
        doCompleteState();
        return s;
    }

    private HtPrologParser getParser() {
        return parser;
    }

    public StateRecord newState() {
        return new StateRecord(state, assocs, dks, currPriority, token);
    }

    /**
     *
     */
    protected static class ExprA0BracketHandler extends ParserStateHandler {

        public ExprA0BracketHandler(
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
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
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);

            handlerA0 = new ExprA0Handler(
                    state,
                    of(x),
                    dks,
                    currPriority,
                    token);
            handlerAn = new ExprAnHandler(
                    state,
                    of(x),
                    dks,
                    currPriority,
                    token);
        }

        public void doPrepareState() throws Exception {
//            IStateHandler result;
            if (currPriority == MIN_PRIORITY) {
                /* result =*/
                handlerA0.prepareState();//fixme
            } else {
                /*result =*/
                handlerAn.prepareState();//fixme
            }
            /*return result;*/
        }


        public void repeat(Consumer<IStateHandler> action) {

        }

        /**
         *
         */
        public Set<IdentifiedTerm> tryOperators(String name) {
            final Set<IdentifiedTerm> result = new HashSet<>();
            for (Associativity assoc : getAssocs()) {
                result.addAll(appContext.getOpTable().getOperators(name, assoc, currPriority));
            }
            return result;
        }
    }

    /**
     *
     */
    public static class ExprBHandler extends ParserStateHandler {

        public ExprBHandler(
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
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
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
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
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
        }

        public void doCompleteState() throws Exception {
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
                    handlerAn.prepareState();
//                            of(x),
//                            dks,
//                            priorityYFX - 1,
//                            token);
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

//    public void repeat(Consumer<IStateHandler> action) {
//
//    }
//

    /**
     *
     */
    public static class ExprA0Handler extends ExprAHandler {

        public ExprA0Handler(
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
        }

        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    public static class ExprA0BraceHandler extends ParserStateHandler {

        public ExprA0BraceHandler(
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
        }

        public StateRecord newState() {
            return null;
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
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
        }

        public final void push(StateRecord sr) {

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
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
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
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
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
                ParserState state,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) throws Exception {
            super(state, assocs, dks, currPriority, token);
        }

        @Deprecated
        public void newState(
                ParserState state,
                State stateRecordState,
                EnumSet<Associativity> assocs,
                EnumSet<DirectiveKind> dks,
                int currPriority,
                PlToken token) {
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

//        protected StateRecord newState(ParserState state,
//                                       EnumSet<Associativity> assocs,
//                                       EnumSet<DirectiveKind> dks,
//                                       int currPriority,
//                                       PlToken token) throws Exception {
//            final StateRecord sr = new StateRecord(state, assocs, dks, currPriority, token);
//            getStates().push(sr);
//
//            return sr;
//        }

//        @Override
//        public Deque<StateRecord> getStates() throws Exception {
//            return appContext.getParser().states;
//        }

    }
}