package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HtMethodDef;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 *
 */
public
class PropertyOwner implements IPropertyOwner {
    protected HtProperty[] props;
    protected HtMethodDef[] methods;

    final private List<PropertyChangeListener> listeners = new ArrayList<>();

    protected Map<String, HtMethodDef> mmap = new HashMap<>();
    protected Map<String, HtProperty> map = new HashMap<>();

    public HtProperty[] getProps() {
        return props;
    }

    public HtMethodDef[] getMethods() {
        return methods;
    }

    public Map<String, HtMethodDef> getMmap() {
        return mmap;
    }

    public Map<String, HtProperty> getMap() {
        return map;
    }

    public PropertyOwner(HtProperty[] props,
                         HtMethodDef[] methods,
                         Map<String, HtProperty> map,
                         Map<String, HtMethodDef> mmap) {

        this.props = props;
        this.methods = methods;
        this.map = map;
        this.mmap = mmap;
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
    public void fireEvent(IProperty property, ITerm newValue) {
        for (PropertyChangeListener listener : listeners) {
            listener.propertyChange(
                    new PropertyChangeEvent(
                            property,
                            property.getName(),
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
    public ITerm getValue(String propertyName) {
        return map.get(propertyName).getValue();
    }

    /**
     * @param propertyName
     * @param value
     * @return
     */
    public ITerm setValue(String propertyName, ITerm value) {
        final HtProperty property = new HtProperty(propertyName, value);
        map.put(propertyName, property);
        fireEvent(property, value);
        return property.getValue();
    }

    /**
     * @param props
     */
    public PropertyOwner(HtMethodDef[] methods, HtProperty[] props) {
        this.props = props;
        this.methods = methods;
        IntStream.range(0, props.length).forEachOrdered(i -> map.put(props[i].getName(), props[i]));
        IntStream.range(0, methods.length).forEachOrdered(i -> mmap.put(methods[i].methodName, methods[i]));
    }
}
