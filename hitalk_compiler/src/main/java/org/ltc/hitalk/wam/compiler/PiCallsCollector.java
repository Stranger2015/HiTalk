package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 *
 */
public
class PiCallsCollector<T extends HtClause> implements Collector <T, PiCallsCollector.Builder <T>, List <T>> {

    /**
     * @return
     */
    public Builder <T> newBuilder () {
        return new Builder <>();
    }

    @Override
    public Supplier <Builder <T>> supplier () {
        return null;
    }

    @Override
    public BiConsumer <Builder <T>, T> accumulator () {
        return null;
    }

    @Override
    public BinaryOperator <Builder <T>> combiner () {
        return null;
    }

    @Override
    public Function <Builder <T>, List <T>> finisher () {
        return null;
    }

    @Override
    public Set <Characteristics> characteristics () {
        return EnumSet.of(Characteristics.UNORDERED);
    }

    /**
     *
     */
    static class Builder<T extends HtClause> /*implements Supplier <Builder <T>>*/ {
        final List <T> clauses = new ArrayList <>();

        /**
         *
         */
        public List <T> build () {
            //todo
            return clauses;
        }

        /**
         * @param clause
         * @return
         */
        public Builder <T> add ( T clause ) {
            clauses.add(clause);
            return this;
        }

        /**
         * @param clauses
         * @return
         */
        public Builder <T> addAll ( List <T> clauses ) {
            this.clauses.addAll(clauses);
            return this;
        }

        /**
         * @param left
         * @param right
         * @return
         */
        public Builder <T> apply ( Builder <T> left, Builder <T> right ) {
            return left.addAll(right.build());
        }
    }
}
