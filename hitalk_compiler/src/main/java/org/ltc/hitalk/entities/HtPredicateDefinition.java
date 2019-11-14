package org.ltc.hitalk.entities;

import org.ltc.hitalk.parser.HtClause;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

/**
 *
 */
public abstract
class HtPredicateDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause>
        extends PropertyOwner <HtProperty> {

    /**
     *
     */
    protected List <T> subroutines;

    /**
     * @param clauses
     * @param props
     */
    protected HtPredicateDefinition ( List <T> clauses, HtProperty... props ) {
        super(props);
        subroutines = new ArrayList <>(clauses);
    }

    /**
     * @param clause
     * @param props
     */
    public HtPredicateDefinition ( T clause, HtProperty... props ) {
        super(props);
        this.subroutines = new ArrayList <>(singletonList(clause));
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
     * @return
     */
    public T[] getBody () {
        return (T[]) subroutines.toArray(new ISubroutine[subroutines.size()]);
    }

    /**
     * @param definition
     */
    public void merge ( HtPredicateDefinition <T, P, Q> definition ) {
        this.subroutines.addAll(definition.subroutines);
    }

    /**
     *
     */
    public static
    class UserDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause>
            extends HtPredicateDefinition <T, P, Q> {
        /**
         * @param props
         */
        public UserDefinition ( List <T> clauses, HtProperty... props ) {
            super(clauses, props);
            reIndex(clauses);
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

        /**
         * @return
         */
        @Override
        public boolean isBuiltIn () {
            return false;
        }

        public void add ( T clause ) {
            subroutines.add(clause);
            reIndex(subroutines);
        }

        private void reIndex ( List <T> subroutines ) {
            //todo
        }
    }

    /**
     * @param <T>
     */
    public static
    class BuiltInDefinition<T extends ISubroutine, P extends HtPredicate, Q extends HtClause>
            extends HtPredicateDefinition <T, P, Q> {

        /**
         * @param props
         */
        public BuiltInDefinition ( T subroutine, HtProperty... props ) {
            super(subroutine, props);
        }

        /**
         * @return
         */
        public boolean isBuiltIn () {
            return true;
        }
    }
}
