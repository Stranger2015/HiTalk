package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.HiTalkFlag;

/**
 * @param <NT>
 */
public
interface IPropertyOwner<NT> {

    /**
     * @return
     */
    default
    HiTalkFlag[] getFlags () {
        return new HiTalkFlag[getPropLength()];
    }

    /**
     * @return
     */
    int getPropLength ();
//
//    /**
//     * @return
//     */
//    default
//    NT[] getNames () {
//        return (NT[]) new Object[getPropLength()];
//    }
//
//    /**
//     * @return
//     */
//    default
//    Term[] getValues () {
//        return new Term[getPropLength()];
//    }
//
//    /**
//     * @return
//     */
//    default
//    HtType[] getTypes () {
//        return new HtType[getPropLength()];
//    }
}
