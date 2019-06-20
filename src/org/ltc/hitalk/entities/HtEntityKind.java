package org.ltc.hitalk.entities;

/**
 *
 */
enum HtEntityKind {
    ENTITY(true, 14, "alias(predicate_indicator,predicate_alias_property_list)", "built_in", "debugging", "declares(predicate_indicator,predicate_declaration_property_list)", "dynamic", "events", "file(atom)", "file(atom,atom)", "lines(integer,integer)", "private(predicate_indicator_list)", "protected(predicate_indicator_list)", "public(predicate_indicator_list)", "source_data", "static"),
    OBJECT_OR_CATEGORY(ENTITY, true, 9, "calls(predicate,predicate_call_update_property_list)", "defines(predicate_indicator,predicate_definition_property_list)", "includes(predicate_indicator,object_identifier_category_identifier, predicate_definition_property_list)", "number_of_clauses(integer)", "number_of_rules(integer)", "number_of_user_clauses(integer)", "number_of_user_rules(integer)", "provides(predicate_indicator,object_identifier_category_identifier, predicate_definition_property_list)", "updates(predicate,predicate_call_update_pro)"),
    OBJECT(OBJECT_OR_CATEGORY, false, 6, "complements", "complements(allow/restrict)", "context_switching_calls", "dynamic_declarations", "module", "threaded"),
    CATEGORY(OBJECT_OR_CATEGORY, false, 0),
    PROTOCOL(ENTITY, false, 0),
    MODULE(ENTITY, false, 0),
    ;

    private final Object[] names;

    private final HtEntityKind parent;

    private final boolean isAbstract;

    private final int propsLength;

    /**
     * @param isAbstract
     * @param propsLength
     * @param names
     */
    private
    HtEntityKind ( boolean isAbstract, int propsLength, Object... names ) {
        this.isAbstract = isAbstract;
        this.propsLength = propsLength;
        this.names = names;
        parent = null;
    }

    /**
     * @param parent
     * @param isAbstract
     * @param propsLength
     * @param names
     */
    private
    HtEntityKind ( HtEntityKind parent, boolean isAbstract, int propsLength, Object... names ) {
        this.parent = parent;
        this.isAbstract = isAbstract;
        this.propsLength = propsLength;
        this.names = names;
    }

    /**
     * @return
     */
    public
    Object[] getNames () {
        return names;
    }

    /**
     * @return
     */
    public
    HtEntityKind getParent () {
        return parent;
    }

    /**
     * @return
     */
    public
    boolean isAbstract () {
        return isAbstract;
    }

    public
    int getPropsLength () {
        return propsLength;
    }
}
