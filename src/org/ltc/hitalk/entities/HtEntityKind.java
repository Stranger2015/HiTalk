package org.ltc.hitalk.entities;

/**
 *category/1-3
 * Description
 *
 * category(Category)
 *
 * category(Category,
 *     implements(Protocols))
 *
 * category(Category,
 *     extends(Categories))
 *
 * category(Category,
 *     complements(Objects))
 *
 * category(Category,
 *     implements(Protocols),
 *     extends(Categories))
 *
 * category(Category,
 *     implements(Protocols),
 *     complements(Objects))
 *
 * category(Category,
 *     extends(Categories),
 *     complements(Objects))
 *
 * category(Category,
 *     implements(Protocols),
 *     extends(Categories),
 *     complements(Objects))
 *
 * Starting category directive.
 * Template and modes
 *
 * category(+category_identifier)
 *
 * category(+category_identifier,
 *     implements(+implemented_protocols))
 *
 * category(+category_identifier,
 *     extends(+extended_categories))
 *
 * category(+category_identifier,
 *     complements(+complemented_objects))
 *
 * category(+category_identifier,
 *     implements(+implemented_protocols),
 *     extends(+extended_categories))
 *
 * category(+category_identifier,
 *     implements(+implemented_protocols),
 *     complements(+complemented_objects))
 *
 * category(+category_identifier,
 *     extends(+extended_categories),
 *     complements(+complemented_objects))
 *
 * category(+category_identifier,
 *     implements(+implemented_protocols),
 *     extends(+extended_categories),
 *     complements(+complemented_objects))
 *
 * Examples
 *
 * :- category(monitoring).
 *
 * :- category(monitoring,
 *     implements(monitoringp)).
 *
 * :- category(attributes,
 *     implements(protected::variables)).
 *
 * :- category(extended,
 *     extends(minimal)).
 *
 * :- category(logging,
 *     implements(monitoring),
 *     complements(employee)).
 */
public
enum HtEntityKind {
    ENTITY(true),//*alias(predicate_indicator,predicate_alias_property_list)", "built_in", "debugging", "declares(predicate_indicator,predicate_declaration_property_list)", "dynamic", "events", "file(atom)", "file(atom,atom)", "lines(integer,integer)", "private(predicate_indicator_list)", "protected(predicate_indicator_list)", "public(predicate_indicator_list)", "source_data", "static"+*/),
    OBJECT_OR_CATEGORY(ENTITY, true),//9, "calls(predicate,predicate_call_update_property_list)", "defines(predicate_indicator,predicate_definition_property_list)", "includes(predicate_indicator,object_identifier_category_identifier, predicate_definition_property_list)", "number_of_clauses(integer)", "number_of_rules(integer)", "number_of_user_clauses(integer)", "number_of_user_rules(integer)"*/, "provides(predicate_indicator,object_identifier_category_identifier, predicate_definition_property_list)", "updates(predicate,predicate_call_update_pro)"),
    OBJECT(OBJECT_OR_CATEGORY, false),// 6, "complements", "complements(allow/restrict)", "context_switching_calls", "dynamic_declarations", "module", "threaded"),
    CATEGORY(OBJECT_OR_CATEGORY, false),
    PROTOCOL(ENTITY, false),
    MODULE(ENTITY, false),
    ;

    //    private final Object[] names;
//
    private final HtEntityKind parent;

    private final boolean isAbstract;

//    private final int propsLength;

    /**
     * @param isAbstract
     */
    private
    HtEntityKind ( boolean isAbstract ) {
        this(null, isAbstract);
    }

    /**
     * @param parent
     * @param isAbstract
     */
    private
    HtEntityKind ( HtEntityKind parent, boolean isAbstract ) {
        this.parent = parent;
        this.isAbstract = isAbstract;
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
}
