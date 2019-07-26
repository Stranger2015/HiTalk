package org.ltc.hitalk.entities;

import static org.ltc.hitalk.entities.HtEntityKind.ENTITY;
import static org.ltc.hitalk.entities.HtEntityKind.OBJECT_OR_CATEGORY;

public
enum HtEntityProperty {
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

    private final HtEntityKind kind;
    private final String[] args;

    HtEntityProperty () {
        this(ENTITY);
    }

    HtEntityProperty ( HtEntityKind kind, String... args ) {
        this.kind = kind;
        this.args = args;
    }

    HtEntityProperty ( String... args ) {
        this(ENTITY, args);
    }
}
