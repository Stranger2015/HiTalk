package org.ltc.hitalk.compiler.bktables.db;


import org.ltc.hitalk.entities.HtRelationKind;

import java.util.EnumSet;

/**
 *
 */
@Deprecated
public
class DbArrayOfRecsHtEntity<T extends DbRecOfArraysEntity> extends DbRecOfArraysEntity {
    /**
     *
     */
    public int entity2;
    /**
     *
     */
    public EnumSet <HtRelationKind> entityRel;

    /**
     * @param entity1
     * @param entity2
     * @param entityRel
     */
    public
    DbArrayOfRecsHtEntity ( int entity1, int entity2, EnumSet <HtRelationKind> entityRel ) {
        super(entity1);
        this.entity2 = entity2;
        this.entityRel = entityRel;
    }
}
