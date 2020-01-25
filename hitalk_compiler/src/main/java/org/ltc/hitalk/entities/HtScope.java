package org.ltc.hitalk.entities;

import org.ltc.hitalk.parser.PrologAtoms;

import static org.ltc.hitalk.entities.HtScope.Kind.*;

/**
 *
 */
public
class HtScope extends PropertyOwner {
    public HtScope(HtProperty... properties) {
        super(properties);
    }

    /**
     *
     */
    public
    enum Kind {
        PRIVATE("private"),
        PROTECTED("protected"),
        PUBLIC("public");

        private final String s;

        /**
         * @param s
         */
        private
        Kind ( String s ) {
            this.s = s;
        }

        /**
         * @return
         */
        public
        String getS () {
            return s;
        }
    }

    /**
     * @param kind
     */
    public HtScope(Kind kind, HtProperty... properties) {
        super(properties);
        this.kind = kind;
        this.properties = properties;
    }

    /**
     * @param name
     * @param properties
     */
    public HtScope(String name, HtProperty... properties) {
        super(properties);
        switch (name) {
            case PrologAtoms.PUBLIC:
                kind = PUBLIC;
                break;
            case PrologAtoms.PROTECTED:
                kind = PROTECTED;
                break;
            case PrologAtoms.PRIVATE:
                kind = PRIVATE;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + name);
        }
    }

    private Kind kind;
    private HtProperty[] properties;

    /**
     * @return
     */
    public Kind getKind() {
        return kind;
    }
}
