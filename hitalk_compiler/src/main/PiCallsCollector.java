import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.PiCall;

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
public class PiCallsCollector implements Collector <HtClause, PiCall.Builder, List <HtClause>> {
    @Override
    public Supplier <PiCall.Builder> supplier () {
        return PiCall::newBuilder;
    }

    @Override
    public BiConsumer <PiCall.Builder, HtClause> accumulator () {
        return PiCall.Builder::add;
    }

    @Override
    public BinaryOperator <PiCall.Builder> combiner () {
        return ( left, right ) -> left.addAll(right.build());

    }

    @Override
    public Function <PiCall.Builder, List <HtClause>> finisher () {
        return null;
    }

    @Override
    public Set <Characteristics> characteristics () {
        return null;
    }
}
