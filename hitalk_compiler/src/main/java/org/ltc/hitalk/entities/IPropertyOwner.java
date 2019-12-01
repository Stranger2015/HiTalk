package org.ltc.hitalk.entities;


import org.ltc.hitalk.term.ITerm;

import java.beans.PropertyChangeListener;

import static org.ltc.hitalk.term.io.HiTalkStream.Properties;

/**
 *
 */
public
interface IPropertyOwner {

    /**
     * @return
     */
    default
    HtProperty[] getFlags () {
        return new HtProperty[getPropLength()];
    }

    /**
     * @return
     */
    int getPropLength ();

    void addListener ( PropertyChangeListener listener );

    void removeListener ( PropertyChangeListener listener );

    void fireEvent ( IProperty property, ITerm value );

    ITerm getValue ( Properties property );

    /**
     * @param property
     * @param value
     */
    void setValue ( Properties property, ITerm value );

}