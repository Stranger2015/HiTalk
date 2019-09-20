package org.ltc.hitalk.entities;

/**
 *
 */
public
class HtRelation implements IRelation {
    private HtEntityIdentifier superEntity;
    private HtScope scope;
    private HtEntityIdentifier subEntity;
    private HtRelationKind relationKind;

    public
    HtRelation ( HtEntityIdentifier superEntity,
                 HtScope scope,
                 HtEntityIdentifier subEntity,
                 HtRelationKind relationKind ) {
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
    HtEntityIdentifier getSuperEntity () {
        return superEntity;
    }

    /**
     * @return
     */
    @Override
    public
    HtEntityIdentifier getSubEntity () {
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

    @Override
    public
    String toString () {
        return "HtRelation{" +
                "superEntity=" + superEntity +
                ", scope=" + scope +
                ", subEntity=" + subEntity +
                ", relationKind=" + relationKind +
                '}';
    }
}
