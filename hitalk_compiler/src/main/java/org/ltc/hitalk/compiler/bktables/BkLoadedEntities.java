package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.entities.HtEntityIdentifier;

/**
 *
 */
public
class BkLoadedEntities extends Record {

    /**
     * @param entity1
     */
    public
    BkLoadedEntities ( HtEntityIdentifier entity1 ) {
        super(BkTableKind.LOADED_ENTITIES, entity1);
    }

    /**
     * @return
     */
    public
    Functor getName () {
        return null;
    }

    @Override
    public
    BkLoadedEntities newInstance () {
        return null;
    }
}
