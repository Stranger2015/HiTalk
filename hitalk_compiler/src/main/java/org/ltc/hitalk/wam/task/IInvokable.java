package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.compiler.bktables.error.StopRequestException;
import org.ltc.hitalk.parser.HtClause;

import java.util.Collections;
import java.util.List;

public
interface IInvokable<T extends HtClause> extends Runnable {

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    default
    void run () {
        try {
            invoke(null);
        } catch (StopRequestException ignored) {
        }
    }

    /**
     * @param t
     * @return
     */
    default
    List <T> invoke ( T t ) {
        banner();

        return Collections.singletonList(t);
    }

    /**
     *
     */
    void banner ();
}
