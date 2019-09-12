package org.ltc.hitalk.entities;


import org.ltc.hitalk.compiler.bktables.Flag;

/**
 *
 */
public
interface IPropertyOwner {

    /**
     * @return
     */
    default
    Flag[] getFlags () {
        return new Flag[getPropLength()];
    }

    /**
     * @return
     */
    int getPropLength ();
}