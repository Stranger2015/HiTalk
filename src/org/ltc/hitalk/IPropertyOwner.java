package org.ltc.hitalk;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.wam.compiler.HtProperty;

import java.util.List;

public
interface IPropertyOwner<NT> {

    List <HtProperty> getProperties ();

    List <NT> getNames ();

    List <Term> getValues ();

    List <HtType> getTypes ();
}
