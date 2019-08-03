package org.ltc.hitalk.entities;

/**
 *
 */
public
enum HtRelationKind {
    EXTENDS,    //(HtEntityKind.OBJECT,
    // HtEntityKind.OBJECT,
    // HtEntityHierarchyKind.PROTOTYPE),
    IMPLEMENTS,
    IMPORTS,
    COMPLEMENTS,
    INSTANTIATES,
    SPECIALIZES,
    ;

    public static final int LENGTH = 6;
}
