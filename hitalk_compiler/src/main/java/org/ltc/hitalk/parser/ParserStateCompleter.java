package org.ltc.hitalk.parser;

import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.IdentifiedTerm.Associativity;

import java.util.EnumSet;

import static org.ltc.hitalk.parser.ParserState.*;

/**
 *
 */
abstract public class ParserStateCompleter implements ICompleter {
    protected StateRecord record;

    public static ICompleter create(ParserState state, StateRecord record) {
        final ICompleter completer;
        switch (state) {
            case START:
                completer = new ZeroCompleter(new StateRecord(), START);
                break;
            case FINISH:
                completer = new ZeroCompleter(new StateRecord(), FINISH);
                break;
            case EXPR_A:
                completer = new ExprACompleter(new StateRecord(), EXPR_A);
                break;
            case EXPR_A_EXIT:
                completer = new ZeroCompleter(new StateRecord(), EXPR_A_EXIT);
                break;
            case EXPR_B:
                completer = new ExprBCompleter(new StateRecord(), EXPR_B);
                break;
            case EXPR_B_EXIT:
                completer = new ZeroCompleter(new StateRecord(), EXPR_B_EXIT);
                break;
            case EXPR_C:
                completer = new ExprCCompleter(new StateRecord(), EXPR_C);
                break;
            case EXPR_C_EXIT:
                completer = new ZeroCompleter(new StateRecord(), EXPR_C_EXIT);
                break;
            case EXPR_A0:
                completer = new ExprA0Completer(new StateRecord(), EXPR_A0);
                break;
            case EXPR_A0_EXIT:
                completer = new ZeroCompleter(new StateRecord(), EXPR_A0_EXIT);
                break;
            case EXPR_A0_BRACE:
                completer = new ExprA0BraceCompleter(new StateRecord(), EXPR_A0_BRACE);
                break;
            case EXPR_A0_BRACE_EXIT:
                completer = new ParserStateCompleter.
                break;
            case EXPR_A0_BRACKET:
                completer = new ParserStateCompleter.
                break;
            case EXPR_A0_BRACKET_EXIT:
                completer = new ParserStateCompleter.
                break;
            case EXPR_A0_ARGS:
                completer = new ParserStateCompleter.
                break;
            case EXPR_A0_ARGS_EXIT:
                completer = new ParserStateCompleter.
                break;
            case EXPR_A0_HEADS:
                completer = new ParserStateCompleter.
                break;
            case EXPR_A0_HEADS_EXIT:
                completer = new ParserStateCompleter.
                break;
            case EXPR_A0_TAIL:
                completer = new ParserStateCompleter.
                break;
            case EXPR_A0_TAIL_EXIT:
                completer = new ParserStateCompleter.
                break;
            case SEQUENCE:
                completer = new ParserStateCompleter.
                break;
            case SEQUENCE_EXIT:
                completer = new ParserStateCompleter.
                break;
            case NOP:
                completer = new ParserStateCompleter.
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }

        return completer;
    }

    /**
     * @param assocs
     * @param directiveKinds
     * @param rDelims
     * @param token
     * @param state
     */
    protected ParserStateCompleter(EnumSet<Associativity> assocs,
                                   EnumSet<DirectiveKind> directiveKinds,
                                   EnumSet<TokenKind> rDelims,
                                   PlToken token,
                                   ParserState state) {


        this.assocs = assocs;
        this.directiveKinds = directiveKinds;
        this.rDelims = rDelims;
        this.token = token;
        this.state = state;
    }


    /**
     * @return
     */
    public EnumSet<Associativity> getAssocs() {
        return assocs;
    }

    /**
     * @return
     */
    public EnumSet<DirectiveKind> getDirectiveKinds() {
        return directiveKinds;
    }

    /**
     * @return
     */
    public EnumSet<TokenKind> getrDelims() {
        return rDelims;
    }

    /**
     * @return
     */
    public PlToken getToken() {
        return token;
    }

    /**
     *
     */
    @Override
    abstract public void complete();

    /**
     * @return
     */
    @Override
    public ParserState getState() {
        return state;
    }

    /**
     *
     */
    @Override
    public void tryOperators() {

    }

    /**
     *
     */
    public static class ExprACompleter extends ParserStateCompleter {

        public ExprACompleter(StateRecord record, ParserState exprAExit) {
            super(record, exprAExit);
        }

        /**
         *
         */
        public void complete() {

        }

        /**
         * @param assocs
         * @param directiveKinds
         * @param rDelims
         * @param token
         * @param state
         */
        public ExprACompleter(EnumSet<Associativity> assocs,
                              EnumSet<DirectiveKind> directiveKinds,
                              EnumSet<TokenKind> rDelims,
                              PlToken token,
                              ParserState state) {
            super(assocs, directiveKinds, rDelims, token, state);
        }
    }

    /**
     *
     */
    public static class ExprBCompleter extends ParserStateCompleter {

        public ExprBCompleter(StateRecord record, ParserState exprB) {
            super(record, exprB);
        }

        /**
         *
         */
        public void complete() {

        }

        /**
         * @param assocs
         * @param directiveKinds
         * @param rDelims
         * @param token
         * @param state
         */
        public ExprBCompleter(EnumSet<Associativity> assocs,
                              EnumSet<DirectiveKind> directiveKinds,
                              EnumSet<TokenKind> rDelims,
                              PlToken token,
                              ParserState state) {
            super(assocs, directiveKinds, rDelims, token, state);
        }
    }

    /**
     *
     */
    public static class ExprCCompleter extends ParserStateCompleter {

        public ExprCCompleter(StateRecord record, ParserState exprC) {
            super(record, exprC);
        }

        /**
         *
         */
        public void complete() {

        }

        /**
         * @param assocs
         * @param directiveKinds
         * @param rDelims
         * @param token
         * @param state
         */
        public ExprCCompleter(EnumSet<Associativity> assocs,
                              EnumSet<DirectiveKind> directiveKinds,
                              EnumSet<TokenKind> rDelims,
                              PlToken token,
                              ParserState state) {
            super(assocs, directiveKinds, rDelims, token, state);
        }
    }

    /**
     *
     */
    public static class ExprAnCompleter extends ExprACompleter {

        /**
         *
         */
        public void complete() {
            super.complete();
        }

        /**
         * @param assocs
         * @param directiveKinds
         * @param rDelims
         * @param token
         * @param state
         */
        public ExprAnCompleter(EnumSet<Associativity> assocs,
                               EnumSet<DirectiveKind> directiveKinds,
                               EnumSet<TokenKind> rDelims,
                               PlToken token,
                               ParserState state) {
            super(assocs, directiveKinds, rDelims, token, state);
        }
    }

    /**
     *
     */
    public static class ExprA0Completer extends ExprACompleter {

        public ExprA0Completer(StateRecord record, ParserState exprA0) {
            super(record, exprA0);
        }

        /**
         *
         */
        public void complete() {
            super.complete();
        }

        /**
         * @param assocs
         * @param directiveKinds
         * @param rDelims
         * @param token
         * @param state
         */
        public ExprA0Completer(EnumSet<Associativity> assocs,
                               EnumSet<DirectiveKind> directiveKinds,
                               EnumSet<TokenKind> rDelims,
                               PlToken token,
                               ParserState state) {
            super(assocs, directiveKinds, rDelims, token, state);
        }
    }

    /**
     *
     */
    public static class ZeroCompleter extends ParserStateCompleter {

        /**
         * @param assocs
         * @param directiveKinds
         * @param rDelims
         * @param token
         * @param state
         */
        protected ZeroCompleter(EnumSet<Associativity> assocs,
                                EnumSet<DirectiveKind> directiveKinds,
                                EnumSet<TokenKind> rDelims,
                                PlToken token,
                                ParserState state) {
            super(assocs, directiveKinds, rDelims, token, state);
        }

        public ZeroCompleter(StateRecord record, ParserState start) {
            super(record, start);
        }

        /**
         *
         */
        public void complete() {

        }
    }

    private static class ExprA0BraceCompleter implements ICompleter {
        public ExprA0BraceCompleter(StateRecord record, ParserState exprA0Brace) {
        }

        /**
         *
         */
        public void complete() {
            
        }

        /**
         * @return
         */
        public ParserState getState() {
            return null;
        }

        /**
         *
         */
        public void tryOperators() {

        }
    }
}