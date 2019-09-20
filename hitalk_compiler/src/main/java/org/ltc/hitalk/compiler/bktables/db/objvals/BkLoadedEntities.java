package org.ltc.hitalk.compiler.bktables.db.objvals;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.entities.HtEntityIdentifier;

import static org.ltc.hitalk.compiler.bktables.BkTableKind.LOADED_ENTITIES;

/**
 *
 */
public
class BkLoadedEntities extends Record {
    /**
     * @param entitiy1
     */
    public
    BkLoadedEntities ( HtEntityIdentifier entitiy1 ) {
        super(LOADED_ENTITIES, entitiy1);
    }

    /**
     * @return
     */
    @Override
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
