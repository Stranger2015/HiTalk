package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;

import static org.ltc.hitalk.core.HtConstants.*;

/**
 *
 */
public
class HtScope extends PropertyOwner {

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
         *
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
    public
    HtScope ( Kind kind, HiTalkFlag... properties ) {
        super(properties);
        this.kind = kind;
    }

    /**
     * @param name
     */
    public
    HtScope ( String name ) {
        switch (name) {
            case PUBLIC:
                kind = Kind.PUBLIC;
                break;
            case PROTECTED:
                kind = Kind.PROTECTED;
                break;
            case PRIVATE:
                kind = Kind.PRIVATE;
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + name);
        }
    }

    private final Kind kind;

    /**
     * @return
     */
    public
    Kind getKind () {
        return kind;
    }
}
