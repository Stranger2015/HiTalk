package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public
class PropertyOwner<T extends IProperty> implements IPropertyOwner {
    private T[] props;
    final private List <PropertyChangeListener> listeners = new ArrayList <>();

    /**
     * @return
     */
    @Override
    public
    int getPropLength () {
        return props.length;
    }

    @Override
    public
    void addListener ( PropertyChangeListener listener ) {
        listeners.add(listener);
    }

    @Override
    public
    void removeListener ( PropertyChangeListener listener ) {
        listeners.remove(listener);
    }

    @Override
    public
    void fireEvent ( IProperty property, Term newValue ) {
        for (PropertyChangeListener listener : listeners) {
            listener.propertyChange(
                    new PropertyChangeEvent(
                            property,
                            property.getName(),
                            property.getValue(),
                            newValue
                    ));
        }
    }

    @Override
    public
    Term getValue ( HiTalkStream.Properties property ) {
        return null;
    }

    /**
     * @param property
     * @param value
     */
    @Override
    public
    void setValue ( HiTalkStream.Properties property, Term value ) {

    }

    /**
     * @param props
     */
    @SafeVarargs
    public
    PropertyOwner ( T... props ) {
        this.props = props;
    }
}
