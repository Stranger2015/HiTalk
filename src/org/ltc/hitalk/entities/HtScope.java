package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;

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
        private final String s;

        /**
         *
         * @param s
         */
        private
        Kind ( String s ) {
            this.s = s;
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

    private final Kind kind;

    /**
     * @return
     */
    public
    Kind getKind () {
        return kind;
    }
}
