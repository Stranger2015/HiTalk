package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.wam.compiler.HtFunctorName;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.SYNTAX_ERROR;

public class ParserException extends ExecutionError {
    private int row;
    private int col;
    protected String key;
    protected String userMessage;

    public ParserException(String s) {
        this(s, 0, 0);
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ParserException(String message, int row, int col) {
        super(SYNTAX_ERROR, new HtFunctorName(message, col, row));
        this.row = row;
        this.col = col;
    }

    /**
     * @param format
     * @param token
     * @param objs
     */
    public ParserException(String format, PlToken token, Object... objs) {
        super(SYNTAX_ERROR, new HtFunctorName(String.format(format, objs), token.beginColumn, token.beginLine));
    }
//    /**
//     * Constructs a new runtime exception with the specified detail message.
//     * The cause is not initialized, and may subsequently be initialized by a
//     * call to {@link #initCause}.
//     * <p>
//     * //     * @param message the detail message. The detail message is saved for
//     * later retrieval by the {@link #getMessage()} method.
//     *
//     * @param kind
//     * @param functorName
//     */
//    public ParserException(Kind kind, HtFunctorName functorName) {
//        super(kind, functorName);
//    }
}
