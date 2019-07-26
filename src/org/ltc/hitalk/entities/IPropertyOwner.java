package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;

/**
 * @param <Functor>
 */
public
interface IPropertyOwner<Functor> {

    /**
     * @return
     */
    default
    HiTalkFlag[] getFlags () {
        return new HiTalkFlag[getPropLength()];
    }

    /**
     * @return
     */
    int getPropLength ();
}