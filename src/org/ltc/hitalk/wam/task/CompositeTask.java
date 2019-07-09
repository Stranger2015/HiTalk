package org.ltc.hitalk.wam.task;


import com.thesett.aima.logic.fol.Clause;
import org.jetbrains.annotations.Contract;
import org.ltc.hitalk.compiler.bktables.IComposite;
import org.ltc.hitalk.compiler.bktables.error.StopRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

abstract public
class CompositeTask<T extends Clause> extends CompilerTask implements IComposite <T, TransformTask <T>> {

    protected List <TransformTask <T>> tasks = new ArrayList <>();
    private T target;

    public
    CompositeTask ( T target ) {
        super();
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

    /**
     * Should be overridden to make something useful than simply print banner.
     */
    @Override
    public
    void invoke ( T t ) throws StopRequestException {
        super.invoke(t);
        int bound = tasks.size();
        IntStream.range(0, bound).forEach(this::accept);
    }

    private
    void accept ( int i ) {
//        T target;
        tasks.get(i).transform(target);
    }//fixme
}
