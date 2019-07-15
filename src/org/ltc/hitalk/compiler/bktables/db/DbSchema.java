package org.ltc.hitalk.compiler.bktables.db;

import org.ltc.hitalk.entities.HtRelation;

public abstract
class DbSchema<T extends HtRelation> {

    protected int recordNum;
    protected T[] records;

    protected
    DbSchema ( int recordNum ) {
        this.recordNum = recordNum;
    }

    public
    int getRecordNum () {
        return recordNum;
    }
}
