package org.ltc.hitalk.entities;

/**
 *
 */
public
class HtRelation {

    /**
     * @param hierarchyKind
     * @param relationKind
     * @param entityKind
     */
    public
    HtRelation ( HtEntityHierarchyKind hierarchyKind, HtRelationKind relationKind, HtEntityKind entityKind ) {
        this.hierarchyKind = hierarchyKind;
        this.relationKind = relationKind;
        this.entityKind = entityKind;
    }

    private final HtEntityHierarchyKind hierarchyKind;

    private final HtEntityKind entityKind;

    private final HtRelationKind relationKind;

    /**
     * @return
     */
    public final
    HtEntityKind getEntityKind () {
        return entityKind;
    }

    /**
     * @return
     */
    public final
    HtEntityHierarchyKind getHierarchyKind () {
        return hierarchyKind;
    }

    /**
     * @return
     */
    public final
    HtRelationKind getRelationKind () {
        return relationKind;
    }
}
