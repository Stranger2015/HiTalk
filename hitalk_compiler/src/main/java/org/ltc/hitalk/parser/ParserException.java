package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.wam.compiler.HtFunctorName;

public class ParserException extends ExecutionError {
    public ParserException(String s) {
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     * <p>
     * //     * @param message the detail message. The detail message is saved for
     * later retrieval by the {@link #getMessage()} method.
     *
     * @param kind
     * @param functorName
     */
    public ParserException(Kind kind, HtFunctorName functorName) {
        super(kind, functorName);
    }
}
