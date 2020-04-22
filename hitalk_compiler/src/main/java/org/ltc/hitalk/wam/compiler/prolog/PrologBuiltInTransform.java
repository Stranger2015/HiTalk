package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.common.util.Function;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.task.PreCompilerTask;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public class PrologBuiltInTransform<T extends HtClause, P, Q, PC, QC>
        implements Function<IFunctor, IFunctor> {

    protected final IVafInterner interner;
    protected final IPreCompiler<T, PreCompilerTask<T>, P, Q, PC, QC> preCompiler;
    protected final IResolver<PC, QC> resolver;
    protected final PredicateTable<HtPredicate> predicateTable = getAppContext().getPredicateTable();

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
    public PrologBuiltInTransform(PrologDefaultBuiltIn defaultBuiltIn,
                                  IVafInterner interner,
                                  IPreCompiler<T, PreCompilerTask<T>, P, Q, PC, QC> preCompiler,
                                  IResolver<PC, QC> resolver) {
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
    public IFunctor apply ( IFunctor functor ) {
        return functor;//todo
    }
}
