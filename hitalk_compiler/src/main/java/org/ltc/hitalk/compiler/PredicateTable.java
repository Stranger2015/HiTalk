package org.ltc.hitalk.compiler;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.HtPredicateDefinition.UserDefinition;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.parser.HtClause;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 */
public
class PredicateTable<P extends HtPredicateDefinition <ISubroutine, HtPredicate, HtClause>>
        extends HashMap <Integer, P> implements Iterable <P> {

    /**
     *
     */
    public
    PredicateTable () {
    }

    /**
     * @return
     */
    @NotNull
    @Override
    public Iterator <P> iterator () {
        return values().iterator();
    }

    /**
     * @param clause
     * @return
     */
    public P lookup ( HtClause clause ) {
        int name = clause.getHead().getName();
        P value = this.get(name);
        if (value == null) {
            value = (P) new UserDefinition(clause);
            this.put(name, value);
        }

        return value;
    }
}
