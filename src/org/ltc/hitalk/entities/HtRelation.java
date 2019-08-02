package org.ltc.hitalk.entities;

/**
 *
 */
public
class HtRelation implements IRelation {
    private HtEntity superEntity;
    private HtEntity subEntity;
    private HtRelationKind relationKind;

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
}
