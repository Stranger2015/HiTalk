package org.ltc.hitalk.compiler.bktables.error;

import org.ltc.hitalk.wam.compiler.HtFunctorName;

/**
 *
 */
public
class ExecutionError extends RuntimeException {
    protected Throwable cause;

    public ExecutionError() {
    }

    public ExecutionError(Kind kind, String s, Throwable cause) {
        this(kind, new HtFunctorName(s, 0));

        this.cause = cause;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public
    enum Kind {//todo resources??
        TYPE_ERROR("Type error"),
        PERMISSION_ERROR("Permission error"),
        EXISTENCE_ERROR("Existence error"),
        DOMAIN_ERROR("Domain error"),
        INSTANTIATION_ERROR("Instantiation error"),
        RESOURCE_ERROR("Resource error"),
        REPRESENTATION_ERROR("Representation error"),
        SYNTAX_ERROR("Syntax error"),
        ;
        //        OBJECT_CREATION_ERROR();
        private final String kindString;

        /**
         * @param kindString
         */
        Kind(String kindString) {
            this.kindString = kindString;
        }

        /**
         * @return
         */
        public String getKindString() {
            return kindString;
        }
    }

    private String message;
    private Kind kind;

    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     * <p>
     * //     * @param message the detail message. The detail message is saved for
     * later retrieval by the {@link #getMessage()} method.
     */
    public ExecutionError(Kind kind, HtFunctorName functorName) {
        this.kind = kind;
        this.message = String.format("%s: %s", kind.getKindString(), functorName);
    }
}
