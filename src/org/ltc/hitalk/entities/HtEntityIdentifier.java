package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public
class HtEntityIdentifier extends Functor {

    private HtEntityKind kind;

    public
    HtEntityIdentifier ( int name, Term[] arguments, HtEntityKind kind ) {
        this(name, arguments);
        if ((kind == HtEntityKind.PROTOCOL) && (arguments.length != 0)) {
            throw new IllegalStateException("Protocol name must be an atom");
        }
        this.kind = kind;
    }

    /**
     * Creates a new functor with the specified arguments.
     *
     * @param name      The name of the functor.
     * @param arguments The functors arguments.
     */
    public
    HtEntityIdentifier ( int name, Term[] arguments ) {
        super(name, arguments);
    }

    public
    HtEntityKind getKind () {
        return kind;
    }

    /**
         * @return
         */
        public final
        FunctorName getName () {
            return name;
        }

        @Override
        public
        HtProperty[] getProperties () {
            return properties;
        }

        @Override
        public
        int getPropLength () {
            return entityKind.getPropsLength();
        }


        @Override
        public final
        String toString () {
            return getClass().getSimpleName() + "{" + "name=" + name + '}';
        }
    }
}
