package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.IVafInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.compiler.PiCalls;
import org.ltc.hitalk.wam.transformers.ISpecializer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 *
 */
public class SpecializerTransformer implements ISpecializer {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final SymbolTable <Integer, String, Object> symbolTable;
    private final IVafInterner interner;
    private final PredicateTable predicateTable;
    private final List <PiCalls> piCalls;

    public SpecializerTransformer ( SymbolTable <Integer, String, Object> symbolTable,
                                    IVafInterner interner,
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

    public ITerm transform ( ITerm t ) {
        return (ITerm) specialize(t);
    }

    public void cancel () {

    }

    public void run () {

    }
}

