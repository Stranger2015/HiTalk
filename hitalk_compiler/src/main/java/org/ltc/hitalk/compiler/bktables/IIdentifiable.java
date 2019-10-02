package org.ltc.hitalk.compiler.bktables;

/**
 *
 */
public
interface IIdentifiable /*extends INameable*/ {

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
