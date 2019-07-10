package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.entities.HtRelationKind;

import java.util.EnumSet;

/**
 *
 */
public
class DbRelation extends DbSchema {
    /**
     *
     */
    public final int entity1;
    /**
     *
     */
    public final int entity2;
    /**
     *
     */
    public final EnumSet <HtRelationKind> entityRel;

    /**
     * @param entity1
     * @param entity2
     * @param entityRel
     */
    public
    DbRelation ( int entity1, int entity2, EnumSet <HtRelationKind> entityRel ) {
        this.entity1 = entity1;
        this.entity2 = entity2;
        this.entityRel = entityRel;
    }
}
