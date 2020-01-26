package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.term.io.HtMethodDef;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.IntStream.range;

/**
 *
 */
public
class PropertyOwner implements IPropertyOwner {

    protected HtProperty[] props;
    protected HtMethodDef[] methods;

    final private List<PropertyChangeListener> listeners = new ArrayList<>();

    protected Map<String, HtMethodDef> mMap = new HashMap<>();
    protected Map<String, HtProperty> map = new HashMap<>();

    public PropertyOwner(HtProperty[] properties) {
        this(properties, new HtMethodDef[0], new HashMap<>(), new HashMap<>());
    }

    /**
     * @param s
     * @param options
     * @return
     */
    public static HtProperty createProperty(String s, String v, String... options) {
        return new HtProperty(s, v, options);
    }

    public HtProperty[] getProps() {
        return props;
    }

    public HtMethodDef[] getMethods() {
        return methods;
    }

    public Map<String, HtMethodDef> getMethodMap() {
        return mMap;
    }

    public Map<String, HtProperty> getPropMap() {
        return map;
    }

    public PropertyOwner(HtProperty[] props,
                         HtMethodDef[] methods,
                         Map<String, HtProperty> map,
                         Map<String, HtMethodDef> mMap) {

        this.props = props;
        this.methods = methods;
        this.map = map;
        this.mMap = mMap;
    }

    /**
     * @return
     */
    @Override
    public int getPropLength() {
        return props.length;
    }

    @Override
    public void addListener(PropertyChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void fireEvent(IProperty property, HtNonVar newValue) {
        for (PropertyChangeListener listener : listeners) {
            listener.propertyChange(
                    new PropertyChangeEvent(
                            property,
                            property.toString(),
                            newValue,
                            property.getValue()
                    )
            );
        }
    }

    /**
     * @param propertyName
     * @return
     */
    public HtNonVar getValue(IFunctor propertyName) {
        for (HtProperty prop : map.values()) {
            if (prop.getName().equals(propertyName)) {
                return prop.getValue();
            }
        }
        return null;
    }

    /**
     * @param propertyName
     * @param value
     */
    public void setValue(IFunctor propertyName, HtNonVar value) {
        final HtProperty property = new HtProperty(propertyName, value);
        map.put(propertyName.toString(), property);
        fireEvent(property, value);
    }

    /**
     * @param props
     */
    public PropertyOwner(HtMethodDef[] methods, HtProperty[] props) {
        this.props = props;
        this.methods = methods;
        range(0, props.length).forEachOrdered(i1 -> map.put(props[i1].getNameAsString(), props[i1]));
        range(0, methods.length).forEachOrdered(i -> mMap.put(methods[i].methodName, methods[i]));
    }

    /**
     * This method gets called when a bound property is changed.
     *
     * @param evt A PropertyChangeEvent object describing the event source
     *            and the property that has changed.
     */
    public void propertyChange(PropertyChangeEvent evt) {

    }
}
