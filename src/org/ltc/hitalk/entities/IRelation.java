package org.ltc.hitalk.entities;

/**
 *
 */
public
interface IRelation {

    String EXTENDS = "extends";
    String IMPLEMENTS = "implements";
    String IMPORTS = "imports";
    String COMPLEMENTS = "complements";
    String INSTANTIATES = "instantiates";
    String SPECIALIZES = "specializes";

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
