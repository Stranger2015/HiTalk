package org.ltc.hitalk.entities;

import org.ltc.hitalk.term.ITerm;

/**
 *
 */
public
interface IProperty {
    /**
     * @return
     */
    String getName();

    /**
     * @return
     */
    ITerm getValue();

   /**
    * @param term
    */
   void setValue ( ITerm term );
}
