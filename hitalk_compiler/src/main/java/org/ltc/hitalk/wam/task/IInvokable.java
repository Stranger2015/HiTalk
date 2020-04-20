package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.term.ITerm;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

import static java.util.Collections.singletonList;

public
interface IInvokable<T extends ITerm> extends Runnable {
    /**
     * @return
     */
    Logger getLogger();

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
    //     */
//    @Override
//    default void run() {
//        try {
//            invoke(null);
//        } catch (StopRequestException | IOException ignored) {
//        }
//    }

    /**
     * @param t
     * @return
     */
    default List<T> invoke(T t) throws IOException {
        banner();
        return singletonList(t);
    }

    /**
     *
     */
    void banner();

//    CompositeTask<T> getCompositeTask();
//    void setCompositeTask(CompositeTask<T> task);

//    IgetNextTask();
}

