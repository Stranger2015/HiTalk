package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.FunctorName;
import javafx.beans.property.Property;
import org.ltc.hitalk.IPropertyOwner;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public abstract
class HtEntity implements IPropertyOwner {

    /**
     *
     */
    protected final List <Property> properties = new ArrayList <>();

    /**
     *
     */
    protected FunctorName name;

    protected
    HtEntity ( FunctorName name ) {
        this.name = name;
    }

    /**
     * @return
     */
    public final
    FunctorName getName () {
        return name;
    }


    @Override
    public final
    String toString () {
        return getClass().getSimpleName() + "{" + "name=" + name + '}';
    }
}
