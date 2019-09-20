package org.ltc.hitalk.compiler.bktables.db;


import org.ltc.hitalk.compiler.bktables.BkTableKind;
import org.ltc.hitalk.compiler.bktables.IIdentifiable;
import org.ltc.hitalk.entities.HtEntityIdentifier;

/**
 *
 */
public abstract
class Record implements IIdentifiable {

    protected final BkTableKind kind;

    protected static int idCounter = 0;
    protected int id;

    /**
     *
     */
    protected HtEntityIdentifier entity1;

    /**
     * @return
     */
    public
    BkTableKind getKind () {
        return kind;
    }


    /**
     * @param kind
     * @param entitiy1
     */
    protected
    Record ( BkTableKind kind, HtEntityIdentifier entitiy1 ) {
        this.kind = kind;
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