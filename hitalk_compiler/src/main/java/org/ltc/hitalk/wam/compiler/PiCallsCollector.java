package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import static java.util.stream.Collector.Characteristics.UNORDERED;
import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public
class PiCallsCollector implements Collector <PiCalls <?>, List <PiCalls <?>>, PiCalls <?>> {

    private final PredicateTable <HtPredicate> predicateTable;
    private final PiCallsCollectorVisitor pccv;
    private final IVafInterner interner;
    private final ISymbolTable<Integer, String, Object> symbolTable;

    private final Supplier <List <PiCalls <?>>> supplier;
    private final BiConsumer <List <PiCalls <?>>, PiCalls <?>> accumulator;
    private final BinaryOperator <List <PiCalls <?>>> combiner;
    private final Function <List <PiCalls <?>>, PiCalls <?>> finisher;

    private final List <PiCalls <?>> piCallsList = new ArrayList <>();
    private final IResolver <HtPredicate, HtClause> resolver;

    /**
     *
     *
     */
    public PiCallsCollector () {
        predicateTable = getAppContext().getPredicateTable();
        symbolTable = getAppContext().getSymbolTable();
        interner = getAppContext().getInterner();
        resolver = getAppContext().getResolverPre();
        supplier = supplier();
        accumulator = accumulator();
        combiner = combiner();
        finisher = finisher();
        pccv = new PiCallsCollectorVisitor(symbolTable, interner, resolver, new HtPositionalTermTraverser());
    }

    private static List <PiCalls <?>> apply ( List <PiCalls <?>> acc, List <PiCalls <?>> ps ) {
        acc.addAll(ps);
        return acc;
    }

    /**
     * @return
     */
    @Override
    public Supplier <List <PiCalls <?>>> supplier () {
        return ArrayList::new;
    }

    /**
     * @return
     */
    @Override
    public BiConsumer <List <PiCalls <?>>, PiCalls <?>> accumulator () {
        return List::add;
    }

    /**
     * @return
     */
    @Override
    public BinaryOperator <List <PiCalls <?>>> combiner () {
        return PiCallsCollector::apply;
    }

    /**
     * @return
     */
    @Override
    public Function <List <PiCalls <?>>, PiCalls <?>> finisher () {
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
    public BiConsumer <List <PiCalls <?>>, PiCalls <?>> getAccumulator () {
        return accumulator;
    }

    /**
     * @return
     */
    public PredicateTable<HtPredicate> getPredicateTable() {
        return predicateTable;
    }

    public Supplier <List <PiCalls <?>>> getSupplier () {
        return supplier;
    }

    public BinaryOperator <List <PiCalls <?>>> getCombiner () {
        return combiner;
    }

    public Function <List <PiCalls <?>>, PiCalls <?>> getFinisher () {
        return finisher;
    }
}
