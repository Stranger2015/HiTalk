package org.ltc.hitalk.wam.task;


import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.compiler.bktables.IComposite;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @param <T>
 */
@Deprecated
abstract public
class CompositeTask<T extends HtClause, TC extends ITerm>
        extends PrologStandardPreprocessor <TC>
        implements IComposite <TC, TransformTask <TC>> {

    protected List <TransformTask <TC>> tasks = new ArrayList <>();

    /**
     * @param action
     * @param target
     * @param transformer
     */
    public CompositeTask ( Function <TC, List <TC>> action, List <TC> target, ITransformer <TC> transformer ) {
        super(action, target, transformer);
    }

    /**
     * @return
     */
    @Contract(pure = true)
    @Override
    public List <TransformTask <TC>> getComponents () {
        return tasks;
    }

    private
    void accept ( int i ) {
    }//fixme
}
