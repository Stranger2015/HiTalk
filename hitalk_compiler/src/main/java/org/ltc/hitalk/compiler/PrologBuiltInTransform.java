package org.ltc.hitalk.compiler;

import com.thesett.common.util.Function;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.io.Environment;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;

public class PrologBuiltInTransform<T extends HtClause, P, Q>
        implements Function <HtFunctor, HtFunctor> {

    protected final IVafInterner interner;
    protected final PrologPreCompiler <T, P, Q> preCompiler;
    protected final IResolver <P, Q> resolver;
    protected final PredicateTable <HtPredicate> predicateTable = Environment.instance().getPredicateTable();

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
                                    IVafInterner interner,
                                    PrologPreCompiler <T, P, Q> preCompiler,
                                    IResolver <P, Q> resolver ) {
        this.defaultBuiltIn = defaultBuiltIn;
        this.interner = interner;
        this.preCompiler = preCompiler;
        this.resolver = resolver;
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
