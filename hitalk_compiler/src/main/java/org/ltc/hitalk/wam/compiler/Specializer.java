package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.transformers.ISpecializer;
import org.ltc.hitalk.wam.transformers.TransformInfo;

import java.util.List;

/**
 *
 */
public class Specializer implements ISpecializer <HtClause, Term> {
    private final SymbolTable <Integer, String, Object> symbolTable;
    private final VariableAndFunctorInterner interner;
    private final List <PiCall> piCalls;

    /**
     *
     */
    public Specializer ( SymbolTable <Integer, String, Object> symbolTable,
                         VariableAndFunctorInterner interner,
                         List <PiCall> piCalls ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
        this.piCalls = piCalls;
    }

    /**
     * @param clause
     * @return
     */
    @Override
    public HtClause specialize ( HtClause clause ) {
        HtClause spClause = clause;
        for (PiCall piCall : piCalls) {
            spClause = specializePred(piCall);
        }

        return spClause;
    }

    private HtClause specializePred ( PiCall piCall ) {

        return null;
    }

    /**
     *
     */
    @Override
    public void reset () {

    }

    @Override
    public ExecutionContext getContext () {
        return null;
    }

    @Override
    public void setContext ( ExecutionContext context ) {

    }

    @Override
    public boolean isAcceptable ( IMetrics max ) {
        return false;
    }

    @Override
    public TransformInfo getBestSoFarResult () {
        return null;
    }

    @Override
    public Term transform ( Term t ) {
        return null;
    }

    @Override
    public void cancel () {

    }

    @Override
    public void run () {

    }
}
