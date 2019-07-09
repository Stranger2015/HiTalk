package org.ltc.hitalk.entities;

/**
 *
 */
public
class HtRelation {

    /**
     * @param hierarchyKind
     * @param relationKind
     * @param entityKind1
     * @param entityKind2
     */
    public
    HtRelation ( HtEntityHierarchyKind hierarchyKind,
                 HtRelationKind relationKind,
                 HtEntityKind entityKind1,
                 HtEntityKind entityKind2 ) {
        this.hierarchyKind = hierarchyKind;
        this.relationKind = relationKind;
        this.entityKind1 = entityKind1;
        this.entityKind2 = entityKind2;
    }

    private final HtEntityHierarchyKind hierarchyKind;

    private final HtEntityKind entityKind1;
    private final HtEntityKind entityKind2;

    private final HtRelationKind relationKind;

    /**
     * @return
     */
    public final
    HtEntityKind getEntityKind1 () {
        return entityKind1;
    }

    /**
     * @return
     */
    public final
    HtEntityKind getEntityKind2 () {
        return entityKind2;
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
