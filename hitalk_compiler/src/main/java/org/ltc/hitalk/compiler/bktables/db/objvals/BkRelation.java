package org.ltc.hitalk.compiler.bktables.db.objvals;

import org.ltc.hitalk.compiler.bktables.BkTableKind;
import org.ltc.hitalk.compiler.bktables.IIdentifiable;
import org.ltc.hitalk.compiler.bktables.db.Record;
import org.ltc.hitalk.entities.HtEntityHierarchyKind;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtRelationKind;
import org.ltc.hitalk.entities.HtScope;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public
class BkRelation extends Record {

    /**
     * @param hierarchyKind
     * @param relationKind
     * @param scope
     * @param entityIdentifier1
     * @param entityIdentifier2
     */
    public
    BkRelation ( HtEntityHierarchyKind hierarchyKind,
                 HtRelationKind relationKind,
                 HtScope scope,
                 HtEntityIdentifier entityIdentifier1,
                 HtEntityIdentifier entityIdentifier2,
                 BkTableKind kind
    ) {

        super(kind, entityIdentifier1);

        this.hierarchyKind = hierarchyKind;
        this.relationKind = relationKind;
        this.scope = scope;
        this.entityIdentifier2 = entityIdentifier2;
    }

    private final HtEntityHierarchyKind hierarchyKind;

    private final HtScope scope;

    /**
     * @return
     */
    public
    HtScope getScope () {
        return scope;
    }

    /**
     * @return
     */
    public
    HtEntityIdentifier getEntityIdentifier2 () {
        return entityIdentifier2;
    }

    private final HtEntityIdentifier entityIdentifier2;

    private final HtRelationKind relationKind;

    /**
     * @return
     */
    public final
    HtEntityHierarchyKind getHierarchyKind () {
        return hierarchyKind;
    }

    /**
     * @return
     */
    public final
    HtRelationKind getRelationKind () {
        return relationKind;
    }

    /**
     * @return
     */
    public IFunctor getName () {
        return entity1;
    }

    /**
     * @param <R>
     * @return
     */
    @Override
    public
    <R extends IIdentifiable> R newInstance () {
        return null;
    }
}
