package org.ltc.hitalk.compiler.bktables.db;

/**
 * @param <R>
 */
public
class Recordset<R extends DbSchema> {
    protected R[] records;

    /**
     * @param records
     */
    public
    Recordset ( R[] records ) {
        this.records = records.clone();
    }
}
