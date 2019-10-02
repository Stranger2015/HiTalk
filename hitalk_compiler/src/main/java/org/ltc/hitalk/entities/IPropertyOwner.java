package org.ltc.hitalk.entities;


import com.thesett.aima.logic.fol.Term;

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

    void fireEvent ( IProperty property, Term value );

    Term getValue ( Properties property );

    /**
     * @param property
     * @param value
     */
    void setValue ( Properties property, Term value );

}