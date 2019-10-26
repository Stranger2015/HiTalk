package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PiCall extends HtFunctor {

    protected final List <HtFunctor> clauses = new ArrayList <>();
    protected boolean representable;

    /**
     * @param name
     * @param args
     */
    public PiCall ( int name, Term[] args ) {
        super(name, args);
    }

    /**
     * @return
     */
    public static Builder newBuilder () {
        return new Builder();
    }

    /**
     *
     */
    public static class Builder {
        final List <HtClause> clauses = new ArrayList <>();
        HtClause clause;

        /**
         *
         */
        public List <HtClause> build () {
            return clauses;
        }

        /**
         * @param clause
         * @return
         */
        public boolean add ( HtClause clause ) {
            return clauses.add(clause);
        }

        /**
         * @param clauses
         * @return
         */
        public boolean addAll ( List <HtClause> clauses ) {
            return this.clauses.addAll(clauses);
        }

        /**
         * @param builder
         * @param clause
         */
        public static void add ( Builder builder, HtClause clause ) {
            builder.clauses.add(clause);
        }

        public static void add ( Builder builder, HtClause clause ) {
        }
    }
}
