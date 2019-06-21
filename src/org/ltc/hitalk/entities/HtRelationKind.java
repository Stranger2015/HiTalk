package org.ltc.hitalk.entities;

/**
 *
 */
public
enum HtRelationKind {
    EXTENDS,                                      //(HtEntityKind.OBJECT,HtEntityKind.OBJECT, HtEntityHierarchyKind.PROTOTYPE),
    IMPLEMENTS,
    IMPORTS,
    COMPLEMENTS,
    INSTANTIATES,
    SPECIALIZES,
    ;

//    private final HtEntityKind entityKind2;
//    private final HtEntityHierarchyKind hierarchyKind;
//    private FINAL ?EnumSet<HtEntityHierarchyKind> hierarchyKinds;

//    private HtEntityKind entityKind;
//    private EnumSet<HtEntityKind> entityKinds;

//    HtRelationKind ( HtEntityKind entityKind,HtEntityKind entityKind2, HtEntityHierarchyKind hierarchyKind ) {
//        this.entityKind2 = entityKind2;
//        this.hierarchyKind = hierarchyKind;
//        this.entityKind = entityKind;
//    }
}
