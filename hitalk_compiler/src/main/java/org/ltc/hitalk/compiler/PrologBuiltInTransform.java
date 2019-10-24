package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.Function;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;

public class PrologBuiltInTransform/*<A extends IApplication, T>*/ implements Function <Functor, Functor> {

    protected final VariableAndFunctorInterner interner;
    protected final PrologPreCompiler preCompiler;
    protected final Resolver <HtClause, HtClause> resolver;
    protected final PredicateTable builtIns = new PredicateTable();

    /**
     * Holds the default built-in, for standard compilation and interners and symbol tables.
     */
    protected final PrologDefaultBuiltIn defaultBuiltIn;

    public PrologBuiltInTransform ( PrologDefaultBuiltIn defaultBuiltIn,
                                    VariableAndFunctorInterner interner,
                                    PrologPreCompiler preCompiler,
                                    Resolver <HtClause, HtClause> resolver ) {
        this.defaultBuiltIn = defaultBuiltIn;
        this.interner = interner;
        this.preCompiler = preCompiler;
        this.resolver = resolver;
    }

    @Override
    public Functor apply ( Functor functor ) {
        return null;
    }
}
