package org.ltc.hitalk.compiler.bktables.db;

import org.ltc.hitalk.entities.HtRelation;
import org.ltc.hitalk.entities.HtRelationKind;

import java.util.EnumSet;

/**
 *
 */
public
class DbEntity2RecOfArrays<T extends HtRelation> extends DbRecOfArraysEntity <T> {
    /**
     *
     */
    T[] entities2;
    private final EnumSet <HtRelationKind> relationKinds;

    public
    DbEntity2RecOfArrays ( int recordNum,
                           T[] entities1,
                           T[] entities2,
                           EnumSet <HtRelationKind> relationKinds
    ) {
        super(recordNum, entities1);
        this.entities2 = entities2;
        this.relationKinds = relationKinds;
    }

    public
    EnumSet <HtRelationKind> getRelationKinds () {
        return relationKinds;
    }
}