package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Term;

/**
 * @param <NT>
 */
public
interface IPropertyOwner<NT> {

//    enum Properties {
//BUILT_IN,
//    }

    /**
     * @return
     */
    default
    HtProperty[] getProperties () {
        return new HtProperty[getPropLength()];
    }


    int getPropLength ();

    /**
     * @return
     */
    default
    NT[] getNames () {
        return (NT[]) new Object[getPropLength()];
    }

    /**
     * @return
     */
    default
    Term[] getValues () {
        return new Term[getPropLength()];
    }

    /**
     * @return
     */
    default
    HtType[] getTypes () {
        return new HtType[getPropLength()];
    }
}
