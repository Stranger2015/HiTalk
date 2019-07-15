package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.compiler.bktables.IIdentifiable;

/**
 *
 */
public
class HtRelation implements IIdentifiable {

    protected static int idCounter = 0;
    private int id;

    public
    HtRelation ( HtEntityHierarchyKind hierarchyKind,
                 HtRelationKind relationKind,
                 HtScope scope,
                 HtEntityIdentifier entityIdentifier1,
                 HtEntityIdentifier entityIdentifier2 ) {

        this.hierarchyKind = hierarchyKind;
        this.relationKind = relationKind;
        this.scope = scope;
        this.entityIdentifier1 = entityIdentifier1;
        this.entityIdentifier2 = entityIdentifier2;
        id = ++idCounter;
    }

    private final HtEntityHierarchyKind hierarchyKind;

    private final HtScope scope;

    public
    HtScope getScope () {
        return scope;
    }

    public
    HtEntityIdentifier getEntityIdentifier1 () {
        return entityIdentifier1;
    }

    public
    HtEntityIdentifier getEntityIdentifier2 () {
        return entityIdentifier2;
    }

    private final HtEntityIdentifier entityIdentifier1;
    private final HtEntityIdentifier entityIdentifier2;

    private final HtRelationKind relationKind;

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
    int getId () {
        return id;
    }

    /**
     * @return
     */
    @Override
    public
    IIdentifiable newInstance () {
        return null;
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
