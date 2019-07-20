package org.ltc.hitalk.compiler.bktables.db;

/**
 * @param <R>
 */
public
class Recordset<R extends Record> {
    protected R[] records;

    /**
     * @param records
     */
    public
    Recordset ( R[] records ) {
        this.records = records.clone();
    }
}
