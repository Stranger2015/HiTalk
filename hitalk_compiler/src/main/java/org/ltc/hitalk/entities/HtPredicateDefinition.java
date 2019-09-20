package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.Flag;
import org.ltc.hitalk.parser.HtClause;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public abstract
class HtPredicateDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause> extends PropertyOwner <HtProperty> {
    protected List <T> subroutines;
    protected boolean builtIn;

    /**
     * @param clauses
     * @param builtIn
     * @param props
     */
    public
    HtPredicateDefinition ( List <T> clauses, boolean builtIn, HtProperty... props ) {
        super(props);

        subroutines = Collections.unmodifiableList(clauses);
        this.builtIn = builtIn;
    }

    /**
     * @param builtIn
     * @param clause
     * @param props
     */
    public
    HtPredicateDefinition ( T clause, boolean builtIn, HtProperty... props ) {
        super(props);

        this.subroutines = Collections.singletonList(clause);
        this.builtIn = builtIn;
    }

    /**
     * @return
     */
    public
    boolean isBuiltIn () {
        return builtIn;
    }

    /**
     * @return
     */
    public
    int size () {
        return subroutines.size();
    }

    /**
     * @param i
     * @return
     */
    public
    T get ( int i ) {
        return subroutines.get(i);
    }

    /**
     * @param definition
     */
    public
    void merge ( HtPredicateDefinition <T, P, Q> definition ) {
        this.subroutines.addAll(definition.subroutines);
    }

    /**
     *
     */
    public static
    class UserDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause> extends HtPredicateDefinition <T, P, Q> {
        /**
         * @param props
         */
        public
        UserDefinition ( List <T> clauses, HtProperty... props ) {
            super(clauses, false, props);
        }

        public
        UserDefinition ( T cclause ) {
            super(cclause, false);
        }

        /**
         * @return
         */
        @Override
        public
        Flag[] getFlags () {
            return new Flag[getPropLength()];
        }
    }

    /**
     * @param <T>
     */
    public static
    class BuiltInDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause> extends HtPredicateDefinition <T, P, Q> {

        /**
         * @param props
         */
        public
        BuiltInDefinition ( T subroutine, HtProperty... props ) {
            super(subroutine, true, props);
        }
    }
}
