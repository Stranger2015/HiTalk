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

        /**
         *
         */
        public List <HtClause> build () {
            //todo
            return clauses;
        }

        /**
         * @param clause
         * @return
         */
        public Builder add ( HtClause clause ) {
            clauses.add(clause);
            return this;
        }

        /**
         * @param clauses
         * @return
         */
        public Builder addAll ( List <HtClause> clauses ) {
            this.clauses.addAll(clauses);
            return this;
        }

        /**
         * @param builder
         * @param clause
         */
//        public static Builder add ( Builder builder, HtClause clause ) {
//            builder.clauses.add(clause);
//            return builder;
//        }
    }
}
