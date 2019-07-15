package org.ltc.hitalk.compiler.bktables.db;

import org.ltc.hitalk.entities.HtRelation;

/**
 *
 */
public
abstract
class DbRecOfArraysEntity<T extends HtRelation> extends DbSchema <T> {
    /**
     *
     */
    public T[] entities1;

    /**
     * @param recordNum
     * @param entities1
     */
    protected
    DbRecOfArraysEntity ( int recordNum, T[] entities1 ) {
        super(recordNum);
        this.entities1 = entities1;
    }
}