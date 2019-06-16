package org.ltc.hitalk.entities;

/**
 *
 */
public
class HtScope {

    /**
     *
     */
    enum Kind {
        PRIVATE("private"),
        PROTECTED("protected"),
        PUBLIC("public");

        Kind ( String s ) {

        }
    }

    /**
     * @param kind
     */
    private
    HtScope ( Kind kind ) {

        this.kind = kind;
    }

    private final Kind kind;
}
