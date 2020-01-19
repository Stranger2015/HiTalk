package org.ltc.hitalk.parser;

public class ParserException extends RuntimeException {

    final private int row;
    final private int col;

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
        super(message);
        this.row = row;
        this.col = col;
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
