package org.ltc.hitalk.entities;


import org.ltc.enumus.Hierarchy;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * entityKind ::=
 * <p>
 * prolog_type | logtalk_type | user_defined_type
 * <p>
 * prolog_type ::=
 * <p>
 * “term” | “nonvar” | “var” |
 * “compound” | “ground” | “callable” | “list” |
 * “atomic” | “atom” |
 * “number” | “integer” | “float”
 * <p>
 * logtalk_type ::=
 * “object” | “category” | “protocol” | “event” | "entity"
 *
 * <p>
 * user_defined_type
 * ::=
 * <p>
 * atom | compound
 * <p>
 * number_of_proofs
 * ::=
 * <p>
 * “zero” | “zero_or_one” | “zero_or_more” | “one” |
 * “one_or_more” | “one_or_error” | “error”
 */
public
enum HtType {

    PROLOG_TYPE,
    LOGTALK_TYPE,
    USER_DEFINED_TYPE,

    ENTITY_TYPE(LOGTALK_TYPE),
    EVENT(LOGTALK_TYPE),

    OBJECT(ENTITY_TYPE),
    PROTOCOL(ENTITY_TYPE),
    CATEGORY(ENTITY_TYPE),
    MODULE(ENTITY_TYPE),

    TERM(PROLOG_TYPE),
    ATOMIC(TERM),
    ATOM(ATOMIC),
    NONVAR(TERM),
    NUMBER(ATOMIC),
    INTEGER(NUMBER),
    FLOAT(NUMBER),
    VAR(TERM),
    COMPOUND(NONVAR),
    LIST(COMPOUND),
    GROUND(NONVAR, COMPOUND),
    CALLABLE(ATOM, COMPOUND, VAR),
    ;


    private static Hierarchy <HtType> hierarchy = new Hierarchy <>(HtType.class, e -> e.parent);
    /**
     *
     */
    private final EnumSet <HtType> parents = EnumSet.noneOf(HtType.class);
    private final HtType parent;


    /**
     * @param parent
     */
    HtType ( HtType parent ) {
        this.parent = parent;
        parents.add(parent);
    }

    /**
     * @param parents
     */
    HtType ( HtType... parents ) {
        this.parents.addAll(Arrays.asList(parents));
        parent = null;
    }
}
