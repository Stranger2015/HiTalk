package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.FunctorName;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.ISubroutine;

import java.util.HashMap;

/**
 *
 */
public
class PredicateTable extends HashMap <FunctorName, HtPredicateDefinition<ISubroutine>> {

    /**
     *
     */
    public
    PredicateTable () {
    }
}
