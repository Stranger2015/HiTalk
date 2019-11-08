package org.ltc.hitalk.entities;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.PrologBuiltIns;
import org.ltc.hitalk.parser.HtClause;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public abstract
class HtPredicateDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause> extends PropertyOwner <HtProperty> {
    protected List <T> subroutines;

    /**
     * @param clauses
     * @param builtIn
     * @param props
     */
    protected HtPredicateDefinition ( List <T> clauses, PrologBuiltIns builtIn, HtProperty... props ) {
        super(props);
        subroutines = Collections.unmodifiableList(clauses);
    }

    /**
     * @param clause
     * @param props
     */
    public HtPredicateDefinition ( T clause, HtProperty... props ) {
        super(props);
        this.subroutines = Collections.singletonList(clause);
    }

    /**
     * @return
     */
    abstract public boolean isBuiltIn ();

    /**
     * @return
     */
    public int size () {
        return subroutines.size();
    }

    /**
     * @param i
     * @return
     */
    public T get ( int i ) {
        return subroutines.get(i);
    }

    /**
     * @param definition
     */
    public void merge ( HtPredicateDefinition <T, P, Q> definition ) {
        this.subroutines.addAll(definition.subroutines);
    }

    public List <HtClause> getClauses () {
        if (isBuiltIn()) {
            throw new ExecutionError(ExecutionError.Kind.EXISTENCE_ERROR, null);
        }
        return null;/*Stream.collect();*/
    }

    /**
     *
     */
    public static
    class UserDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause> extends HtPredicateDefinition <T, P, Q> {
        /**
         * @param props
         */
        public UserDefinition ( List <T> clauses, HtProperty... props ) {
            super(clauses, null, props);
        }

        public UserDefinition ( T clause ) {
            super(clause);
        }

        /**
         * @return
         */
        @Override
        public HtProperty[] getFlags () {
            return new HtProperty[getPropLength()];
        }

        public boolean isBuiltIn () {
            return false;
        }
    }

    /**
     * @param <T>
     */
    public static
    class BuiltInDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause>
            extends HtPredicateDefinition <T, P, Q> {

        private final PrologBuiltIns builtIn;

        /**
         * @param props
         */
        public BuiltInDefinition ( T subroutine, PrologBuiltIns builtIn, HtProperty... props ) {
            super(subroutine, props);
            this.builtIn = builtIn;
        }

        /**
         * @return
         */
        public boolean isBuiltIn () {
            return true;
        }
    }
}
