package org.ltc.hitalk.compiler.bktables.error;

public
class ExecutionError extends RuntimeException {
    public
    enum Kind {
        TYPE_ERROR,
        PERMISSION_ERROR,
        EXISTENCE_ERROR,
        DOMAIN_ERROR,
        INSTANTIATION_ERROR,
        RESOURCE_ERROR,
        REPRESENTATION_ERROR
    }

    Kind kind;

    /**
     * Constructs a new runtime exception with {@code null} as its
     * detail message.  The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause}.
     */
    public
    ExecutionError ( Kind kind ) {
        this.kind = kind;
    }
}
