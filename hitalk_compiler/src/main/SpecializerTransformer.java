import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.PiCalls;
import org.ltc.hitalk.wam.compiler.PiCallsCollector;
import org.ltc.hitalk.wam.compiler.PiCallsList;
import org.ltc.hitalk.wam.compiler.PrologPositionalTransformVisitorNew;
import org.ltc.hitalk.wam.transformers.ISpecializer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.wam.compiler.PiCallsCollector.Characteristics;

/**
 *
 */
public class SpecializerTransformer implements ISpecializer {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final SymbolTable <Integer, String, Object> symbolTable;
    private final VariableAndFunctorInterner interner;
    private final PredicateTable predicateTable;
    private final List <PiCalls> piCalls;

    public SpecializerTransformer ( SymbolTable <Integer, String, Object> symbolTable,
                                    VariableAndFunctorInterner interner,
                                    PredicateTable predicateTable,
                                    List <PiCalls> piCalls ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
        this.predicateTable = predicateTable;
        this.piCalls = piCalls;
    }

    public void reset () {

    }

    public ExecutionContext getContext () {
        return null;
    }

    public void setContext ( ExecutionContext context ) {

    }

    public boolean isAcceptable ( IMetrics max ) {
        return false;
    }

    public TransformInfo getBestSoFarResult () {
        return null;
    }

    public Term transform ( Term t ) {
        return (Term) specialize((HtClause) t);
    }

    public void cancel () {

    }

    public void run () {

    }

    /**
     * @param clause
     * @return
     */
    public List <HtClause> specialize ( HtClause clause ) {
        return null;
    }

    List <HtClause> clauseList ( HtPredicateDefinition <? extends HtClause, ? extends org.ltc.hitalk.entities.HtPredicate, ? extends HtClause> definition ) {
        if (definition.isBuiltIn()) {
            throw new ExecutionError(PERMISSION_ERROR, null);
        }

        return IntStream.range(0, definition.size()).mapToObj(i -> definition.get(i)).
                collect(Collectors.toList());
    }

    private PiCallsCollector piCallsCollector = (PiCallsCollector) Collector.of(
            new Supplier <PiCallsList>() {
                public PiCallsList get () {
                    return null;
                }
            },
            new BiConsumer <PiCallsList, PiCalls>() {
                public void accept ( PiCallsList piCallsList, PiCalls piCalls ) {

                }
            },
            new BinaryOperator <PiCallsList>() {
                public PiCallsList apply ( PiCallsList piCallsList, PiCallsList piCallsList2 ) {
                    return null;
                }
            },
            new Function <PiCallsList, PiCalls>() {
                public PiCalls apply ( PiCallsList piCallsList ) {
                    return null;
                }
            },
            Characteristics.UNORDERED
    );

    //        while (predicateTable.iterator().hasNext()) {
//            HtPredicateDefinition predicateDefinition = predicateTable.iterator().next();
//            PiCallsCollector<HtClause> piCallsCollector = Collector.of(new Supplier(),new );
//
//
//        }
    List <HtClause> spClauses = new ArrayList <>();
    //        List <PiCall> piCalls = new ArrayList <>();
    PrologPositionalTransformVisitorNew pptv = new PrologPositionalTransformVisitorNew(symbolTable, interner);
        pptv.setPositionalTraverser(new

    HtPositionalTermTraverserImpl ());
//        for (HtPredicateDefinition <ISubroutine, HtPredicate, HtClause> pd : predicateTable) {
//            for (int i = 0; i < pd.size(); i++) {
//                piCalls.addAll(collectPiCalls(pd));
//
//
//                spClauses.add((HtClause)sub);
//            }
//        }
}

