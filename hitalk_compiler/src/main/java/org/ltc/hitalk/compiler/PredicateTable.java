package org.ltc.hitalk.compiler;

import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.BodyCall;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.PiCalls;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public
class PredicateTable<P extends HtPredicate> extends HashMap <Integer, P> implements Iterable <P> {

    /**
     *
     */
    public PredicateTable () {

    }

    public PredicateTable ( List <HtPredicate> predicates ) {
        for (HtPredicate predicate : predicates) {
            List <HtClause> def = predicate.getClauses();
            if (!predicate.isBuiltIn()) {
                accept((P) def);
            }
        }
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

    public HtClause lookup ( HtFunctor goal, HtClause clause ) {
        return null;
    }

    public <C extends BodyCall.BodyCalls <C>> P lookup ( PiCalls <C> piCall ) {
        return null;
    }
}

//    /**
//     * @param call
//     * @return
//     */
//    public UserDefinition <?, ?, ?> lookup ( PiCalls <?> call ) {
//        int name = call.getName();
////        UserDefinition <?, ?, ?> value = this.get(name);
//        if (value == null) {
//            value = new UserDefinition();
//            this.put(name, value);
//        }
//
//        return value;
//    }
//
//    public HtClause lookup ( HtFunctor goal, HtClause clause ) {
//        return null;
//    }
//
//    public HtClause lookup ( HtFunctor goal, boolean redo ) {
//        return null;
//    }
//}
