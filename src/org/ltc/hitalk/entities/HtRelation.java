package org.ltc.hitalk.entities;

/**
 *
 */
public
class HtRelation implements IRelation {
    private HtEntity superEntity;
    private HtScope scope;
    private HtEntity subEntity;
    private HtRelationKind relationKind;

    public
    HtRelation ( HtEntity superEntity, HtScope scope, HtEntity subEntity, HtRelationKind relationKind ) {
        this.superEntity = superEntity;
        this.scope = scope;
        this.subEntity = subEntity;
        this.relationKind = relationKind;
    }

    /**
     * @return
     */
    @Override
    public
    HtEntity getSuperEntity () {
        return superEntity;
    }

    /**
     * @return
     */
    @Override
    public
    HtEntity getSubEntity () {
        return subEntity;
    }

    /**
     * @return
     */
    @Override
    public
    HtRelationKind getRelationKind () {
        return relationKind;
    }

    public
    HtScope getScope () {
        return scope;
    }
}
