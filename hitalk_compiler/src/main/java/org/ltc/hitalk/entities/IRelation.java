package org.ltc.hitalk.entities;


import org.ltc.hitalk.parser.PrologAtoms;

/**
 *
 */
public
interface IRelation extends PrologAtoms {

    /**
     * @return
     */
    HtEntityIdentifier getSuperEntity ();

    /**
     * @return
     */
    HtEntityIdentifier getSubEntity ();

    /**
     * @return
     */
    HtRelationKind getRelationKind ();

    /**
     * @return
     */
    HtScope getScope ();
}
