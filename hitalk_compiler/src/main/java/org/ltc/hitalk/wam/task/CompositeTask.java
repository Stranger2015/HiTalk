package org.ltc.hitalk.wam.task;


import com.thesett.aima.logic.fol.Term;
import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.compiler.bktables.IComposite;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * @param <T>
 */
abstract public
class CompositeTask<T extends HtClause, TC extends Term>
        extends StandardPreprocessor <T, TC>
        implements IComposite <T, TC, TransformTask <T, TC>> {

    protected List <TransformTask <T, TC>> tasks = new ArrayList <>();

    /**
     * @param action
     * @param target
     * @param transformer
     */
    public
    CompositeTask ( Function <T, List <T>> action, List <TC> target, ITransformer <T, TC> transformer ) {
        super(action, target, transformer);
    }

    /**
     * @return
     */
    @Contract(pure = true)
    @Override
    public
    List <TransformTask <T, TC>> getComponents () {
        return tasks;
    }

//    /**
//     * Should be overridden to make something useful than simply print banner.
//     * @return
//     */
//    public
//    List <T> invoke ( T t ) throws StopRequestException {
//        List <T> l = super.invoke(t);
//        int bound = tasks.size();
//        IntStream.range(0, bound).forEach(this::accept);
//        return l ;
//    }

    private
    void accept ( int i ) {
//        T target;
//        tasks.get(i).transform(target);
    }//fixme
}
