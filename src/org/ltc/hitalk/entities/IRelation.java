package org.ltc.hitalk.entities;

/**
 *
 */
public
interface IRelation {
    /**
     * @return
     */
    HtEntity getSuperEntity ();

    /**
     * @return
     */
    HtEntity getSubEntity ();

    /**
     * @return
     */
    HtRelationKind getRelationKind ();
}
