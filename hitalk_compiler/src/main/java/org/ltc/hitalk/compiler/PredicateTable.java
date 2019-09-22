package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.FunctorName;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.parser.HtClause;

import java.util.HashMap;

/**
 *
 */
public
class PredicateTable extends HashMap <FunctorName, HtPredicateDefinition <ISubroutine, HtPredicate, HtClause>> {

    /**
     *
     */
    public
    PredicateTable () {
    }
}
