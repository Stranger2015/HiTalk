package org.ltc.hitalk.compiler.bktables.db.objvals;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.db.DbSchema;
import org.ltc.hitalk.entities.HtEntityHierarchyKind;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtRelationKind;
import org.ltc.hitalk.entities.HtScope;

/**
 *
 */
public
class BkRelation extends DbSchema {

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
                 HtEntityIdentifier entityIdentifier2 ) {

        super(entityIdentifier1);

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
    @Override
    public
    Functor getName () {
        return entity1;
    }
}
