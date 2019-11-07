package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.FunctorName;
import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 */
public
class PredicateTable extends HashMap <FunctorName, HtPredicateDefinition <ISubroutine, HtPredicate, HtClause>>
        implements Iterable <HtPredicateDefinition <ISubroutine, HtPredicate, HtClause>> {

    /**
     *
     */
    public
    PredicateTable () {
    }

    @NotNull
    @Override
    public Iterator <HtPredicateDefinition <ISubroutine, HtPredicate, HtClause>> iterator () {
        return values().iterator();
    }

    public boolean lookup ( HtFunctor functor ) {
        this.
    }
}
