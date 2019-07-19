package org.ltc.hitalk.compiler.bktables.db;

import org.ltc.hitalk.compiler.bktables.IIdentifiable;
import org.ltc.hitalk.entities.HtEntityIdentifier;

/**
 *
 */
public abstract
class DbSchema implements IIdentifiable {

    protected static int idCounter = 0;
    protected int id;

    /**
     *
     */
    protected HtEntityIdentifier entity1;

    /**
     * @param entitiy1
     */
    public
    DbSchema ( HtEntityIdentifier entitiy1 ) {
        this.entity1 = entitiy1;
        id = ++idCounter;
    }

    /**
     * @return
     */
    public
    HtEntityIdentifier getEntity1 () {
        return entity1;
    }

    /**
     * @return
     */
    public
    int getId () {
        return id;
    }
}