package org.ltc.hitalk.wam.transformers;


import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.IComposite;
import org.ltc.hitalk.wam.task.TransformTask;

import java.util.ArrayList;
import java.util.List;

/**
 * @param <T>
 */
public
class CompositeTransformer<T extends Term, TT extends TransformTask <T>> extends TransformTask <T> implements ITransformer <T>, IComposite <T, TT> {

    protected final List <TT> transformers = new ArrayList <>();

    public
    CompositeTransformer ( T target, ITransformer <T> transformer ) {
        super(target, transformer);
    }

    private
    void addAndAssign ( TransformInfo result, TransformInfo transform ) {

    }

    private
    TransformInfo initial () {
        return null;
    }

    @Override
    public final
    List <TT> getComponents () {
        return transformers;
    }
}
