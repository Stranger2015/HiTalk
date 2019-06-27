package org.ltc.hitalk.entities;

/**
 *
 */
public
class HtScope extends PropertyOwner {

    /**
     *
     */
    enum Kind {
        PRIVATE("private"),
        PROTECTED("protected"),
        PUBLIC("public");

        /**
         * @param s
         */
        private
        Kind ( String s ) {

        }
    }

    /**
     * @param kind
     */
    public
    HtScope ( Kind kind, HtProperty... properties ) {
        super(properties);
        this.kind = kind;
    }

    private final Kind kind;
}
