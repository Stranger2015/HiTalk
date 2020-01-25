package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.term.io.HtMethodDef;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.beans.PropertyChangeListener;
import java.util.Map;

/**
 *
 */
public
interface IPropertyOwner extends PropertyChangeListener {

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
    void fireEvent(IProperty property, HtNonVar value);

    /**
     * @param propertyName
     * @return
     */
    HtNonVar getValue(IFunctor propertyName);

    /**
     * @param propertyName
     * @param value
     */
    void setValue(IFunctor propertyName, HtNonVar value);

    HtProperty[] getProps();

    HtMethodDef[] getMethods();

    Map<String, HtMethodDef> getMethodMap();

    Map<String, HtProperty> getPropMap();
}