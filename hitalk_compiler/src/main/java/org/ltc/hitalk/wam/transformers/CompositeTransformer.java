package org.ltc.hitalk.wam.transformers;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.IComposite;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <T>
 */
public
class CompositeTransformer<T extends HtClause, TC extends Term, TT extends TransformTask <T, TC>>
        extends TransformTask <T, TC>
        implements ITransformer <T, TC>, IComposite <T, TC, TT> {

    protected final List <TT> transformers = new ArrayList <>();

    public
    CompositeTransformer ( List <TC> target, ITransformer <T, TC> transformer ) {
        super(target, transformer);
    }

    private
    void addAndAssign ( TransformInfo <T> result, TransformInfo <T> transform ) {

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
