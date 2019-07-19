package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.db.DbSchema;
import org.ltc.hitalk.entities.HtEntityIdentifier;

/**
 *
 */
public
class BkLoadedeEntities extends DbSchema {

    /**
     * @param entitiy1
     */
    public
    BkLoadedeEntities ( HtEntityIdentifier entitiy1 ) {
        super(entitiy1);
    }

    /**
     * @return
     */
    @Override
    public
    Functor getName () {
        return null;
    }
}
