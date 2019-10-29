package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.Function;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;

public class PrologBuiltIxnTransform<A extends IApplication, T extends HtClause>
        implements Function <HtFunctor, HtFunctor> {

    protected final VariableAndFunctorInterner interner;
    protected final PrologPreCompiler preCompiler;
    protected final Resolver <T, T> resolver;
    protected final PredicateTable predicateTable = new PredicateTable();

    /**
     * Holds the default built-in, for standard compilation and interners and symbol tables.
     */
    protected final PrologDefaultBuiltIn defaultBuiltIn;

    /**
     * @param defaultBuiltIn
     * @param interner
     * @param preCompiler
     * @param resolver
     */
    public PrologBuiltInTransform ( PrologDefaultBuiltIn defaultBuiltIn,
                                    VariableAndFunctorInterner interner,
                                    PrologPreCompiler preCompiler,
                                    Resolver <T, T> resolver ) {
        this.defaultBuiltIn = defaultBuiltIn;
        this.interner = interner;
        this.preCompiler = preCompiler;
\this.resolver = resolver;
    }

    /**
     * @param functor
     * @return
     */
    @Override
    public HtFunctor apply ( HtFunctor functor ) {
        return functor;//todo
    }
}
