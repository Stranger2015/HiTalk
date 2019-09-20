package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public
class HtEntityIdentifier extends Functor {

    private final HtEntityKind kind;

    /**
     * @param name
     * @param arguments
     * @param kind
     */
    public
    HtEntityIdentifier ( int name, Term[] arguments, HtEntityKind kind ) {
        super(name, arguments);
        if ((kind == HtEntityKind.PROTOCOL) && (arguments.length != 0)) {
            throw new ExecutionError(PERMISSION_ERROR, null/*"Protocol name must be an atom."*/);
        }
        this.kind = kind;
    }

    /**
     * @param functor
     * @param kind
     */
    public
    HtEntityIdentifier ( Functor functor, HtEntityKind kind ) {
        this(functor.getName(), functor.getArguments(), kind);
    }

    /**
     * @return
     */
    public final
    HtEntityKind getKind () {
        return kind;
    }

    /**
     * @return
     */
    @Override
    public final
    String toString () {
        return getClass().getSimpleName() + "{" + "name=" + name + ", kind=" + kind + "}";
    }
}
