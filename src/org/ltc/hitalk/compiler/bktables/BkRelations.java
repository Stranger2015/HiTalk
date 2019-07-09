package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.entities.HtRelationKind;

import java.util.EnumSet;

public
class BkRelations extends DbSchema {
    private short[] entity2Indexes;
    private EnumSet <HtRelationKind>[] relations;
}
