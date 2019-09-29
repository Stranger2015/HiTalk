package org.ltc.hitalk.entities;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.beans.PropertyChangeListener;

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

    void addListener ( PropertyChangeListener listener );

    void removeListener ( PropertyChangeListener listener );

    void fireEvent ( IProperty property, Term value );


    Term getValue ( HiTalkStream.Properties property );

    /**
     * @param property
     * @param value
     */
    void setValue ( HiTalkStream.Properties property, Term value );

//    /**
//     *
//     */
//    interface IPropertyChangeListener {
//        /**
//         * @param event
//         */
//        void onPropertyChanged ( PropertyChangeEvent event );
//    }
}