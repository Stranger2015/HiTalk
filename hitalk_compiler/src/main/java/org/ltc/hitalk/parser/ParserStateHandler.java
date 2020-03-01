package org.ltc.hitalk.parser;

import org.ltc.hitalk.term.IdentifiedTerm;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.EnumSet.of;
import static org.ltc.hitalk.parser.Directive.DirectiveKind;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity;
import static org.ltc.hitalk.term.IdentifiedTerm.Associativity.x;

/**
 * exprA(n) ::=
 * exprB(n) { op(yfx,n) exprA(n-1) | op(yf,n) }*
 * //============================
 * //  exprB(n) ::=
 * //      exprC(n-1) { op(xfx,n) exprA(n-1) | op(xfy,n) exprA(n) | op(xf,n) }*
 * //   // exprC is called parseLeftSide in the code
 * //============================
 * //  exprC(n) ::=
 * //      '-' integer | '-' float |
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
 * // block ::= '('  { exprA(1200) }* ')'  //block
 * //============================
 * // bypass_blk ::= '{' { exprA(1200) }* '}'
 * //============================
 * // op(type,n) ::= atom | { symbol }+
 * //============================
 * // sequence ::= [ heads tail ]
 * //============================
 */
abstract public class ParserStateHandler extends StateRecord implements IStateHandler {
    private final ParserState parserState;
    private final EnumSet<Associativity> assocs;
    private final EnumSet<DirectiveKind> dks;
    private final Predicate<IStateHandler> rDelimCondition;
    private final int currPriority;
    private final PlToken token;

    /**
     * @param assocs
     * @param dks
     * @param rDelimCondition
     * @param currPriority
     * @param token
     */
    public ParserStateHandler(ParserState parserState,
                              EnumSet<Associativity> assocs,
                              EnumSet<DirectiveKind> dks,
                              Predicate<IStateHandler> rDelimCondition,
                              int currPriority,
                              PlToken token) {
        this.parserState = parserState;
        this.assocs = assocs;
        this.dks = dks;
        this.rDelimCondition = rDelimCondition;
        this.currPriority = currPriority;
        this.token = token;
    }

    /**
     * @param h
     * @return
     */
    public static IStateHandler create(ParserStateHandler h) {
        IStateHandler handler;
        switch (h.getParserState()) {
            case START:
                handler = new NopHandler(h.getParserState());
                break;
            case FINISH:
                handler = new NopHandler(h.getParserState());
                break;
            case EXPR_A:
                handler = new ExprAHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_B:
                handler = new ExprBHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_C:
                handler = new ExprCHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0:
                handler = new ExprA0Handler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_BRACE:
                handler = new ExprA0BraceHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_BRACKET:
                handler = new ExprA0BracketHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_ARGS:
                handler = new ExprA0ArgsHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_HEADS:
                handler = new ExprA0HeadsHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case EXPR_A0_TAIL:
                handler = new ExprA0TailHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case SEQUENCE:
                handler = new SequenceHandler(
                        h.getParserState(),
                        h.getAssocs(),
                        h.getDks(),
                        h.getrDelimCondition(),
                        h.getCurrPriority(),
                        h.getToken());
                break;
            case NOP:
                handler = new NopHandler(h.getParserState());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + h.getParserState());
        }

        return handler;
    }

    /**
     *
     */
    protected static class ExprA0BracketHandler extends ParserStateHandler {

        public ExprA0BracketHandler(ParserState state,
                                    EnumSet<Associativity> assocs,
                                    EnumSet<DirectiveKind> dks,
                                    Predicate<IStateHandler> rDelimCondition,
                                    int currPriority,
                                    PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {

        }


        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    @Override
    public void tryOperators() {

    }

    public void repeat(Consumer<IStateHandler> action) {

    }

    /**
     *
     */
    public static class ExprAHandler extends ParserStateHandler {
        public ExprA0Handler getHandlerA0() {
            return handlerA0;
        }

        public ExprAnHandler getHandlerAn() {
            return handlerAn;
        }

        ExprA0Handler handlerA0;
        ExprAnHandler handlerAn;

        public ExprAHandler(ParserState state,
                            EnumSet<Associativity> assocs,
                            EnumSet<DirectiveKind> dks,
                            Predicate<IStateHandler> rDelimCondition,
                            int currPriority,
                            PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
            handlerA0 = new ExprA0Handler(state, assocs, dks, rDelimCondition, currPriority, token);
            handlerAn = new ExprAnHandler(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {
            if (getCurrPriority() == 0) {
                getHandlerA0().enterState(state);//fixme
            } else {
                getHandlerAn().enterState(state);//fixme
            }
        }

        public void exitState(ParserState state) {

        }


        public void repeat(Consumer<IStateHandler> action) {

        }

        /**
         *
         */
        public void tryOperators() {
            final Set<IdentifiedTerm> result = new HashSet<>();
            for (Associativity assoc : getAssocs() {
                result.addAll(getOptable().getOperators(name, getAssocs(), getCurrPriority()));
            }
            return result;

        }
    }

    /**
     *
     */
    public static class ExprBHandler extends ParserStateHandler {

        public ExprBHandler(ParserState state,
                            EnumSet<Associativity> assocs,
                            EnumSet<DirectiveKind> dks,
                            Predicate<IStateHandler> rDelimCondition,
                            int currPriority,
                            PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {

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
         * @param rDelimCondition
         * @param currPriority
         * @param token
         */
        public ExprCHandler(ParserState state,
                            EnumSet<Associativity> assocs,
                            EnumSet<DirectiveKind> dks,
                            Predicate<IStateHandler> rDelimCondition,
                            int currPriority,
                            PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {

        }


        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    public static class ExprAnHandler extends ExprAHandler {

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param rDelimCondition
         * @param currPriority
         * @param token
         */
        public ExprAnHandler(ParserState state,
                             EnumSet<Associativity> assocs,
                             EnumSet<DirectiveKind> dks,
                             Predicate<IStateHandler> rDelimCondition,
                             int currPriority,
                             PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        /**
         *
         */
        public void tryOperators() {
            super.tryOperators();
        }

        public void enterState(ParserState state) {
            super.enterState(state);
        }

        public void exitState(ParserState state) {
            super.exitState(state);
        }

        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    /**
     *
     */
    public static class ExprA0Handler extends ExprAHandler {

        public ExprA0Handler(ParserState state,
                             EnumSet<Associativity> assocs,
                             EnumSet<DirectiveKind> dks,
                             Predicate<IStateHandler> rDelimCondition,
                             int currPriority,
                             PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
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
         */
        public NopHandler(ParserState state) {
            super(state,
                    of(x),
                    of(DirectiveKind.DK_IF),
                    NopHandler::testRDelims,
                    0,
                    PlToken.newToken(PlToken.TokenKind.TK_ANY_CHAR));
        }

        private static boolean testRDelims(IStateHandler iStateHandler) {
            return false;
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {

        }


        public void repeat(Consumer<IStateHandler> action) {

        }


    }

    /**
     *
     */
    public static class ExprA0BraceHandler extends ParserStateHandler {

        public ExprA0BraceHandler(ParserState state,
                                  EnumSet<Associativity> assocs,
                                  EnumSet<DirectiveKind> dks,
                                  Predicate<IStateHandler> rDelimCondition,
                                  int currPriority,
                                  PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {

        }


        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    public static class SequenceHandler extends ParserStateHandler {

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param rDelimCondition
         * @param currPriority
         * @param token
         */
        public SequenceHandler(ParserState state,
                               EnumSet<Associativity> assocs,
                               EnumSet<DirectiveKind> dks,
                               Predicate<IStateHandler> rDelimCondition,
                               int currPriority,
                               PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {

        }


        /**
         *
         */
        public void tryOperators() {

        }

        public void repeat(Consumer<IStateHandler> action) {

        }


    }

    public static class ExprA0ArgsHandler extends ParserStateHandler {

        /**
         * @param state
         * @param assocs
         * @param dks
         * @param rDelimCondition
         * @param currPriority
         * @param token
         */
        public ExprA0ArgsHandler(ParserState state,
                                 EnumSet<Associativity> assocs,
                                 EnumSet<DirectiveKind> dks,
                                 Predicate<IStateHandler> rDelimCondition,
                                 int currPriority,
                                 PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {

        }


        /**
         *
         */
        public void tryOperators() {

        }

        public void repeat(Consumer<IStateHandler> action) {

        }
    }

    public static class ExprA0HeadsHandler extends ParserStateHandler {
        /**
         * @param state
         * @param assocs
         * @param dks
         * @param rDelimCondition
         * @param currPriority
         * @param token
         */
        public ExprA0HeadsHandler(ParserState state,
                                  EnumSet<Associativity> assocs,
                                  EnumSet<DirectiveKind> dks,
                                  Predicate<IStateHandler> rDelimCondition,
                                  int currPriority,
                                  PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {

        }


        /**
         *
         */
        public void tryOperators() {

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
         * @param rDelimCondition
         * @param currPriority
         * @param token
         */
        public ExprA0TailHandler(ParserState state,
                                 EnumSet<Associativity> assocs,
                                 EnumSet<DirectiveKind> dks,
                                 Predicate<IStateHandler> rDelimCondition,
                                 int currPriority,
                                 PlToken token) {
            super(state, assocs, dks, rDelimCondition, currPriority, token);
        }

        public void enterState(ParserState state) {

        }

        public void exitState(ParserState state) {
        }

        /**
         *
         */
        public void tryOperators() {

        }

        public void repeat(Consumer<IStateHandler> action) {
            action.accept(this);
        }
    }
}