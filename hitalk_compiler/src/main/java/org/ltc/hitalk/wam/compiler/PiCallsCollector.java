package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.UNORDERED;

/**
 *
 */
public
class PiCallsCollector<T extends HtClause> implements Collector <T, PiCallsCollector.Builder <T>, List <T>> {

    /**
     * @return
     */
    public Builder <T> builder () {
        return new Builder <>();
    }

    /**
     * @return
     */
    @Override
    public Supplier <Builder <T>> supplier () {
        return this::builder;
    }

    /**
     * @return
     */
    @Override
    public BiConsumer <Builder <T>, T> accumulator () {
        return Builder::add;
    }

    /**
     * @return
     */
    @Override
    public BinaryOperator <Builder <T>> combiner () {
        return ( left, right ) -> left.addAll(right.build());
    }

    /**
     * @return
     */
    @Override
    public Function <Builder <T>, List <T>> finisher () {
        return Builder::build;
    }

    /**
     * @return
     */
    @Override
    public Set <Characteristics> characteristics () {
        return Collections.unmodifiableSet(EnumSet.of(UNORDERED));
    }

    /**
     * @return
     */
    public static PiCallsCollector toPiCallsCollector () {
        return new PiCallsCollector();
    }

    /**
     *
     */
    public static class Builder<T extends HtClause> implements Supplier <Builder <T>> {
        final List <T> clauses = new ArrayList <>();
        public final Builder <T> builder = new Builder <>();

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

        public Builder <T> get () {
            return builder;
        }
    }
}
