package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.io.Environment;

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
class PiCallsCollector implements Collector <PiCalls, List <PiCalls>, PiCalls> {

    private final PredicateTable predicateTable;
    private final MetaInterpreterVisitor miv;
    private final VariableAndFunctorInterner interner;
    private final SymbolTable <Integer, String, Object> symbolTable;

    private final Supplier <List <PiCalls>> supplier;
    private final BiConsumer <List <PiCalls>, PiCalls> accumulator;
    private final BinaryOperator <List <PiCalls>> combiner;
    private final Function <List <PiCalls>, PiCalls> finisher;

    private final List <PiCalls> piCallsList = new ArrayList <>();
    private final Resolver <HtPredicate, HtClause> resolver;

    /**
     *
     *
     */
    public PiCallsCollector () {
        predicateTable = Environment.instance().getPredicateTable();
        symbolTable = Environment.instance().getSymbolTable();
        interner = Environment.instance().getInterner();
        resolver = Environment.instance().getResolver();
        supplier = supplier();
        accumulator = accumulator();
        combiner = combiner();
        finisher = finisher();
        miv = new PiCallsCollectorVisitor(symbolTable, interner, resolver);
    }

    /**
     * @return
     */
    @Override
    public Supplier <List <PiCalls>> supplier () {
        return ArrayList::new;
    }

    /**
     * @return
     */
    @Override
    public BiConsumer <List <PiCalls>, PiCalls> accumulator () {
        return ( piCalls, e ) -> {
            piCalls.add(e);
        };
    }

    /**
     * @return
     */
    @Override
    public BinaryOperator <List <PiCalls>> combiner () {
        return ( acc, ps ) -> {
            acc.addAll(ps);
            return acc;
        };
    }

    /**
     * @return
     */
    @Override
    public Function <List <PiCalls>, PiCalls> finisher () {
        return ( piCalls ) -> {

            return null;
        };
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
     * @return
     */
    public BiConsumer <List <PiCalls>, PiCalls> getAccumulator () {

        return accumulator;
    }

    /**
     * @return
     */
    public PredicateTable getPredicateTable () {
        return predicateTable;
    }
}
