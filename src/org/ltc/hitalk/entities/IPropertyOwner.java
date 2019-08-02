package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;

public
interface IPropertyOwner {

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