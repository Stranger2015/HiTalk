package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Term;

/**
 *
 */
public
interface IProperty { //extends INameable {
   /**
    * @return
    */
   String getName ();

   /**
    * @return
    */
   Term getValue ();

   /**
    * @param term
    */
   void setValue ( Term term );
}
