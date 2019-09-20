package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Functor;

/**
 *
 */
public
interface IIdentifiable extends INameable <Functor> {

    /**
     * @return
     */
    int getId ();

    <I extends IIdentifiable> I newInstance ();

//    /**
//     * @return
//     */
//    IIdentifiable newInstance ();
}
