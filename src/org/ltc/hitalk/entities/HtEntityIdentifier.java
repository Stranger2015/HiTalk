package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
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
     * @param <NT>
     */
    public static
    class HtEntity<NT> implements IPropertyOwner <NT> {

        /**
         *
         */
        protected final NT name;
        private final HtEntityKind entityKind;

        /**
         *
         */
        private HtProperty[] properties;

        /**
         * @param name
         * @param entityKind
         * @param htEntityKind
         */
        protected
        HtEntity ( NT name, org.ltc.hitalk.entities.HtEntityKind entityKind, org.ltc.hitalk.entities.HtEntityKind htEntityKind ) {
            this.name = name;
            this.entityKind = entityKind;
            HtEntityKind = htEntityKind;
            initProperties();
        }

        /**
         *
         */
        protected final org.ltc.hitalk.entities.HtEntityKind HtEntityKind;

        /**
         *
         */
        private
        void initProperties () {
            switch (entityKind) {
                case ENTITY:

                    break;
                case OBJECT:
                    //                getProperty();
                    break;
                case CATEGORY:
                case OBJECT_OR_CATEGORY:
                    break;
                case PROTOCOL:

                    break;
                case MODULE:

                    break;

                default:
                    throw new IllegalStateException("Unexpected value: " + entityKind);
            }
        }

        /**
         * @return
         */
        public final
        NT getName () {
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
