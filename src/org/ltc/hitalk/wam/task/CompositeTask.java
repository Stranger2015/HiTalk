package org.ltc.hitalk.wam.task;


import com.thesett.aima.logic.fol.Clause;
import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.compiler.bktables.IComposite;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

abstract public
class CompositeTask<T extends Clause> extends CompilerTask <T> implements IComposite <T> {

    protected List <TransformTask <T>> tasks = new ArrayList <>();
    protected T target;

    public
    CompositeTask ( Function <T, List <T>> action, T target ) {
        super(action);
        this.target = target;
    }

    /**
     * @return
     */
    @Contract(pure = true)
    @Override
    public
    List <TransformTask <T>> getComponents () {
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
        tasks.get(i).transform(target);
    }//fixme
}
