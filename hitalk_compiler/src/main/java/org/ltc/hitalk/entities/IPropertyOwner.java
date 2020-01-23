package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HtMethodDef;

import java.beans.PropertyChangeListener;
import java.util.Set;

/**
 *
 */
public
interface IPropertyOwner {

    /**
     * @return
     */
    int getPropLength();

    /**
     * @param listener
     */
    void addListener(PropertyChangeListener listener);

    /**
     * @param listener
     */
    void removeListener(PropertyChangeListener listener);

    /**
     * @param property
     * @param value
     */
    void fireEvent(IProperty property, ITerm value);

    /**
     * @param propertyName
     * @return
     */
    ITerm getValue(String propertyName);

    /**
     * @param propertyName
     * @param value
     */
    void setValue(String propertyName, ITerm value);

    HtProperty[] getProps();

    HtMethodDef[] getMethods();

    Set<HtMethodDef> getMethodSet();

    Set<HtProperty> getSet();

}