package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public
class HtEntityIdentifier extends HtFunctor {

    private final HtEntityKind kind;

    /**
     * @param name
     * @param arguments
     * @param kind
     */
    public HtEntityIdentifier ( int name, ListTerm arguments, HtEntityKind kind ) {
        super(name, arguments);
        if ((kind == HtEntityKind.PROTOCOL) && (arguments.size() != 0)) {
            throw new ExecutionError(PERMISSION_ERROR, null/*"Protocol name must be an atom."*/);
        }
        this.kind = kind;
    }

    /**
     * @param functor
     * @param kind
     */
    public HtEntityIdentifier(IFunctor functor, HtEntityKind kind) throws Exception {
        this(functor.getName(), functor.getArgs(), kind);
    }

    /**
     * @return
     */
    public final HtEntityKind getEntityKind () {
        return kind;
    }

    /**
     * @return
     */
    @Override
    public final
    String toString () {
        return getClass().getSimpleName() + "{" + "name=" + getName() + ", kind=" + kind + "}";
    }
}
