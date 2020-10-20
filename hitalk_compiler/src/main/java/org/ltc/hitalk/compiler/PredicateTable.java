package org.ltc.hitalk.compiler;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public
class PredicateTable<T extends ITerm<T>> extends HashMap<Integer, T> implements Iterable<T> {

    /**
     *
     */
    public PredicateTable() {

    }

    /**
     * @param predicates
     * @throws Exception
     */
    public PredicateTable(List<T> predicates) throws Exception {
        for (T predicate : predicates) {
            List<T> def = predicate.getClauses();
            if (!predicate.isBuiltIn()) {
                accept(def);
            }
        }
    }

    /**
     * @param definition
     * @throws Exception
     */
    private void accept(List<T> definition) throws Exception {
        final int key = ((IFunctor<T>)definition.get(0)).getName();

        put(key, (T) definition);
    }

    /**
     * @return
     */
    @NotNull
    @Override
    public Iterator<T> iterator() {
        return values().iterator();
    }

    public HtClause lookup(HtFunctor<T> goal, HtClause clause) {
        return null;
    }
}
