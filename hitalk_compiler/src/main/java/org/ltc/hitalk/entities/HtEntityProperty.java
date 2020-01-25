package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.HtNonVar;
import org.ltc.hitalk.term.io.HtMethodDef;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import static org.ltc.hitalk.entities.HtEntityKind.ENTITY;
import static org.ltc.hitalk.entities.HtEntityKind.OBJECT_OR_CATEGORY;

public
enum HtEntityProperty implements IPropertyOwner {

    //    ENTITY
    ALIAS("predicate_indicator", "predicate_alias_property_list"),
    BUILT_IN,
    DEBUGGING,
    DECLARES("predicate_indicator", "predicate_declaration_property_list"),
    DYNAMIC,
    EVENTS,
    FILE("atom", "..."),//atom/atom
    LINES("integer", "integer"),
    PRIVATE("predicate_indicator_list"),
    PROTECTED("predicate_indicator_list"),
    PUBLIC("predicate_indicator_list"),
    SOURCE_DATA,
    STATIC,
    //    ==========================================================
//OBJECT_OR_CATEGORY(ENTITY, ),
    CALLS(OBJECT_OR_CATEGORY, "predicate", "predicate_call_update_property_list"),
    DEFINES(OBJECT_OR_CATEGORY, "predicate_indicator", "predicate_definition_property_list"),
    INCLUDES(OBJECT_OR_CATEGORY,
            "predicate_indicator",
            "object_identifier_or_category_identifier",
            "predicate_definition_property_list"),
    NUMBER_OF_CLAUSES(OBJECT_OR_CATEGORY, "integer"),
    NUMBER_OF_RULES(OBJECT_OR_CATEGORY, "integer"),
    NUMBER_OF_USER_CLAUSES(OBJECT_OR_CATEGORY, "integer"),
    NUMBER_OF_USER_RULES(OBJECT_OR_CATEGORY, "integer"),
    PROVIDES(OBJECT_OR_CATEGORY,
            "predicate_indicator",
            "object_identifier_or_category_identifier",
            "predicate_definition_property_list"),
    UPDATES(OBJECT_OR_CATEGORY, "predicate", "predicate_call_update_pro"),
    COMPLEMENTS("... allow/restrict"),
    CONTEXT_SWITCHING_CALLS,
    DYNAMIC_DECLARATIONS,
    MODULE,
    THREADED,
    ;

    protected PropertyOwner owner = new PropertyOwner(getMethods(), getProps());

    private final HtEntityKind kind;
    private final String[] args;

    HtEntityProperty() {
        this(ENTITY);
    }

    HtEntityProperty(HtEntityKind kind, String... args) {
        this.kind = kind;
        this.args = args;
    }

    HtEntityProperty(String... args) {
        this(ENTITY, args);
    }

    /**
     * @return
     */
    public int getPropLength() {
        return 0;
    }

    /**
     * @param listener
     */
    public void addListener(PropertyChangeListener listener) {

    }

    /**
     * @param listener
     */
    public void removeListener(PropertyChangeListener listener) {

    }

    /**
     * @param property
     * @param value
     */
    public void fireEvent(IProperty property, HtNonVar value) {

    }

    /**
     * @param propertyName
     * @return
     */
    public HtNonVar getValue(IFunctor propertyName) {
        return owner.getValue(propertyName);
    }

    /**
     * @param propertyName
     * @param value
     */
    public void setValue(IFunctor propertyName, HtNonVar value) {
        owner.setValue(propertyName, value);
    }

    public HtProperty[] getProps() {
        return owner.getProps();
    }

    public HtMethodDef[] getMethods() {
        return owner.getMethods();
    }

    public Map<String, HtMethodDef> getMethodMap() {
        return owner.getMethodMap();
    }

    public Map<String, HtProperty> getPropMap() {
        return null;
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
