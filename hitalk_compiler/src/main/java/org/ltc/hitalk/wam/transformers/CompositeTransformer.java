package org.ltc.hitalk.wam.transformers;


import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.compiler.bktables.IComposite;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.ITokenSource;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;

/**
 *
 */
public
class CompositeTransformer<TT extends TransformTask>
        extends TransformTask
        implements ITransformer, IComposite <TT> {

    protected final Deque <TT> transformers = new ArrayDeque <>();
    private List <ITerm> output;

    /**
     * @param preCompiler
     * @param tokenSource
     * @param kind
     */
    protected CompositeTransformer(IPreCompiler preCompiler,
                                   ITokenSource tokenSource,
                                   EnumSet<DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);
    }

    private void addAndAssign ( TransformInfo result, TransformInfo transform ) {

    }

    private TransformInfo initial () {
        /*ExecutionContext context,
                           ExecutionInfo executionInfo,
                           IMetrics delta,
                           ITerm input,
                           List <ITerm> output*/
        return new TransformInfo(getContext(), new ExecutionInfo(), new IMetrics() {
            public IMetrics initialMetrics () {
                return null;
            }

            public IMetrics subtract ( IMetrics currentMetrics ) {
                return null;
            }

            public int compareTo ( @NotNull IMetrics o ) {
                return 0;
            }
        }, input, output);
    }

    @Override
    public final Deque <TT> getComponents () {
        return transformers;
    }
}
