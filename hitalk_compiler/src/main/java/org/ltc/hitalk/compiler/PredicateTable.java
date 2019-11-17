package org.ltc.hitalk.compiler;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.HtPredicateDefinition.UserDefinition;
import org.ltc.hitalk.entities.ISubroutine;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.PiCalls;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public
class PredicateTable<P extends HtPredicateDefinition <ISubroutine, HtPredicate, HtClause>>
        extends HashMap <Integer, P> implements Iterable <P> {

    /**
     *
     */
    public PredicateTable () {

    }

    public PredicateTable ( List <HtPredicate> predicates ) {
        predicates.stream().map(HtPredicate::getDefinition).
                filter(def -> !def.isBuiltIn()).
                forEachOrdered(definition -> accept((P) definition));
    }

    private void accept ( P definition ) {
        final int key = definition.get(0).getHead().getName();

        put(key, definition);
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
     * @param call
     * @return
     */
    public P lookup ( PiCalls call ) {
        int name = call.getName();
        P value = this.get(name);
        if (value == null) {
            value = (P) new UserDefinition <>();
            this.put(name, value);
        }

        return value;
    }
}
