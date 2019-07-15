package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.entities.context.Context;

/**
 *
 */
public
class HtScope extends PropertyOwner {

    @Override
    public
    String get ( Context.Kind.Loading basename ) {
        return null;
    }

    /**
     *
     */
    enum Kind {
        PRIVATE("private"),
        PROTECTED("protected"),
        PUBLIC("public");

        /**
         *
         *
         *
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
    HtScope ( Kind kind, HiTalkFlag... properties ) {
        super(properties);
        this.kind = kind;
    }

    private final Kind kind;
}
