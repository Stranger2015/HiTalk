package org.ltc.hitalk.wam.transformers;


import org.ltc.hitalk.compiler.bktables.IComposite;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <T>
 */
public
class CompositeTransformer<T extends HtClause, TC extends ITerm, TT extends TransformTask <TC>>
        extends TransformTask <TC>
        implements ITransformer <TC>, IComposite <TC, TT> {

    protected final List <TT> transformers = new ArrayList <>();

    public CompositeTransformer ( List <TC> target, ITransformer <TC> transformer ) {
        super(target, transformer);
    }

    private void addAndAssign ( TransformInfo <T> result, TransformInfo <T> transform ) {

    }

    private
    TransformInfo <T> initial () {
        return new TransformInfo <>();
    }

    @Override
    public final
    List <TT> getComponents () {
        return transformers;
    }
}
