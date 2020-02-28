package org.ltc.hitalk.parser;

import static org.ltc.hitalk.parser.ParserState.*;

/**
 *
 */
abstract public class ParserStateCompleter implements ICompleter {
    protected final StateRecord record;
    protected final ParserState state;

    /**
     * @param record
     * @param state
     */
    public ParserStateCompleter(StateRecord record, ParserState state) {
        this.record = record;
        this.state = state;
    }

    /**
     * @param state
     * @param record
     * @return
     */
    public static ICompleter create(ParserState state, StateRecord record) {
        final ICompleter completer;
        switch (state) {
            case START:
                completer = new ZeroCompleter(record, START);
                break;
            case FINISH:
                completer = new ZeroCompleter(record, FINISH);
                break;
            case EXPR_A:
                completer = new ExprACompleter(record, EXPR_A);
                break;
            case EXPR_A_EXIT:
                completer = new ZeroCompleter(record, EXPR_A_EXIT);
                break;
            case EXPR_B:
                completer = new ExprBCompleter(record, EXPR_B);
                break;
            case EXPR_B_EXIT:
                completer = new ZeroCompleter(record, EXPR_B_EXIT);
                break;
            case EXPR_C:
                completer = new ExprCCompleter(record, EXPR_C);
                break;
            case EXPR_C_EXIT:
                completer = new ZeroCompleter(record, EXPR_C_EXIT);
                break;
            case EXPR_A0:
                completer = new ExprA0Completer(record, EXPR_A0);
                break;
            case EXPR_A0_EXIT:
                completer = new ZeroCompleter(record, EXPR_A0_EXIT);
                break;
            case EXPR_A0_BRACE:
                completer = new ExprA0BraceCompleter(record, state);
                break;
            case EXPR_A0_BRACE_EXIT:
                completer = new ZeroCompleter(record, state);
                break;
            case EXPR_A0_BRACKET:
                completer = new ExprA0BracketCompleter(record, state);
                break;
            case EXPR_A0_BRACKET_EXIT:
                completer = new ZeroCompleter(record, state);
                break;
            case EXPR_A0_ARGS:
                completer = new ExprA0ArgsCompleter(record, state);
                break;
            case EXPR_A0_ARGS_EXIT:
                completer = new ZeroCompleter(record, state);
                break;
            case EXPR_A0_HEADS:
                completer = new ExprA0HeadsCompleter(record, state);
                break;
            case EXPR_A0_HEADS_EXIT:
                completer = new ZeroCompleter(record, state);
                break;
            case EXPR_A0_TAIL:
                completer = new ExprA0TailCompleter(record, state);
                break;
            case EXPR_A0_TAIL_EXIT:
                completer = new ZeroCompleter(record, state);
                break;
            case SEQUENCE:
                completer = new SequenceCompleter(record, state);
                break;
            case SEQUENCE_EXIT:
                completer = new ZeroCompleter(record, state);
                break;
            case NOP:
                completer = new ZeroCompleter(record, state);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }

        return completer;
    }

    /**
     *
     */
    protected static class ExprA0BracketCompleter extends ParserStateCompleter {
        /**
         * @param record
         * @param state
         */
        public ExprA0BracketCompleter(StateRecord record, ParserState state) {
            super(record, state);
        }

        /**
         *
         */
        public void complete() {

        }
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
    public final ParserState getState() {
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
    }

    /**
     *
     */
    public static class ExprAnCompleter extends ExprACompleter {

        public ExprAnCompleter(StateRecord record, ParserState exprAExit) {
            super(record, exprAExit);
        }

        /**
         *
         */
        public void complete() {
            super.complete();
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

    }

    /**
     *
     */
    public static class ZeroCompleter extends ParserStateCompleter {

        /**
         * @param record
         * @param start
         */
        public ZeroCompleter(StateRecord record, ParserState start) {
            super(record, start);
        }

        /**
         *
         */
        public void complete() {

        }
    }

    /**
     *
     */
    private static class ExprA0BraceCompleter extends ParserStateCompleter {
        /**
         * @param record
         * @param state
         */
        public ExprA0BraceCompleter(StateRecord record, ParserState state) {
            super(record, state);
        }

        /**
         *
         */
        public void complete() {

        }
    }

    private static class SequenceCompleter extends ParserStateCompleter {

        /**
         * @param record
         * @param state
         */
        public SequenceCompleter(StateRecord record, ParserState state) {
            super(record, state);
        }

        /**
         *
         */
        public void complete() {

        }


        /**
         *
         */
        public void tryOperators() {

        }
    }

    private static class ExprA0ArgsCompleter extends ParserStateCompleter {

        public ExprA0ArgsCompleter(StateRecord record, ParserState state) {
            super(record, state);
        }

        /**
         *
         */
        public void complete() {

        }


        /**
         *
         */
        public void tryOperators() {

        }
    }

    private static class ExprA0HeadsCompleter extends ParserStateCompleter {

        public ExprA0HeadsCompleter(StateRecord record, ParserState state) {
            super(record, state);
        }

        /**
         *
         */
        public void complete() {
        }


        /**
         *
         */
        public void tryOperators() {

        }
    }

    /**
     *
     */
    private static class ExprA0TailCompleter extends ParserStateCompleter {

        /**
         * @param record
         * @param state
         */
        public ExprA0TailCompleter(StateRecord record, ParserState state) {
            super(record, state);
        }

        /**
         *
         */
        public void complete() {

        }

        /**
         *
         */
        public void tryOperators() {

        }
    }
}