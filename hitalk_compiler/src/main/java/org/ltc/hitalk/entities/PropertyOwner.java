package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HtMethodDef;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/**
 *
 */
public
class PropertyOwner implements IPropertyOwner {
    protected HtProperty[] props;
    protected HtMethodDef[] methods;

    final private List<PropertyChangeListener> listeners = new ArrayList<>();

    protected Set<HtMethodDef> mset = new HashSet<>();
    protected Set<HtProperty> set = new HashSet<>();

    public HtProperty[] getProps() {
        return props;
    }

    public HtMethodDef[] getMethods() {
        return methods;
    }

    public Set<HtProperty> getSet() {
        return set;
    }

    public PropertyOwner(HtProperty[] props,
                         HtMethodDef[] methods,
                         Set<HtProperty> set,
                         Set<HtMethodDef> mset) {

        this.props = props;
        this.methods = methods;
        this.set = set;
        this.mset = mset;
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
        for (HtProperty prop : set) {
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
//        set.put(propertyName, property);
        fireEvent(property, value);
    }

    /**
     * @param props
     */
    public PropertyOwner(HtMethodDef[] methods, HtProperty[] props) {
        this.props = props;
        this.methods = methods;
        IntStream.range(0, props.length).forEachOrdered(i -> set.put(props[i].getName(), props[i]));
        IntStream.range(0, methods.length).forEachOrdered(i -> mset.put(methods[i].methodName, methods[i]));
    }
}
