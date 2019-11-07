package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.term.io.Environment;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
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
class PiCallsCollector<T extends PiCalls, A, R extends PiCalls> implements Collector <T, A, R> {

    private final PredicateTable predicateTable;
    private final PrologPositionalTransformVisitorNew pptv;
    private final VariableAndFunctorInterner interner;
    private final SymbolTable <Integer, String, Object> symbolTable;

    private final Supplier <A> supplier;
    private final BiConsumer <A, T> accumulator;
    private final BinaryOperator <A> combiner;
    private final Function <A, R> finisher;
    private final PiCallsList piCallsList = new PiCallsList();

    /**
     *
     */
    public PiCallsCollector () {
        predicateTable = Environment.instance().getPredicateTable();
        symbolTable = Environment.instance().getSymbolTable();
        interner = Environment.instance().getInterner();
        pptv = new PrologPositionalTransformVisitorNew(symbolTable, interner);
        supplier = supplier();
        accumulator = accumulator();
        combiner = combiner();
        finisher = finisher();
    }

    /**
     * @return
     */
    public PiCallsList builder () {
        return new PiCallsList();
    }

    /**
     * @return
     */
    @Override
    public Supplier <A> supplier () {
        return supplier;
    }

    /**
     * @return
     */
    @Override
    public BiConsumer <A, T> accumulator () {
        return accumulator;
    }

    /**
     * @return
     */
    @Override
    public BinaryOperator <A> combiner () {
        return combiner;
    }

    /**
     * @return
     */
    @Override
    public Function <A, R> finisher () {
        return finisher;
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
    public static PiCallsCollector <PiCalls, PiCallsList, PiCalls> toPiCallsCollector () {
        return new PiCallsCollector <>();
    }

    /**
     * @return
     */
    public BiConsumer <A, T> getAccumulator () {

        return accumulator;
    }

    /**
     * @return
     */
    public PredicateTable getPredicateTable () {
        return predicateTable;
    }
}
