package org.ltc.hitalk.entities;

/**
 *
 */
public
class HtRelation {

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
    public
    HtEntityKind getEntityKind () {
        return entityKind;
    }

    /**
     * @return
     */
    public
    HtEntityHierarchyKind getHierarchyKind () {
        return hierarchyKind;
    }

    /**
     * @return
     */
    public
    HtRelationKind getRelationKind () {
        return relationKind;
    }
}
