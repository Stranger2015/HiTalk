package org.ltc.hitalk.entities;


import org.ltc.hitalk.compiler.bktables.Flag;

import java.beans.PropertyChangeEvent;

/**
 *
 */
public
interface IPropertyOwner<I> {

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

    /**
     *
     */
    interface IPropertyChangeListener {
        /**
         * @param event
         */
        void onPropertyChanged ( PropertyChangeEvent event );
    }
}