package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.compiler.error.StopRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 *
 */
abstract public
class CompilerTask implements Runnable {

    //    protected final IApplication app;
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());


    protected
    CompilerTask () {

    }

    /**
     * When an object implementing interfacebject's
     * <code>run</code> method to be called in that separately executing
     * thread. <code>Runnable</code> is used
     * to create a thread, starting the thread causes the o
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public final
    void run () {
//       if ( app.isStarted() && !app.isStopped())
//        {
        try {
            invoke();
        } catch (StopRequestException ignored) {
        }
    }

    /**
     * Should be overridden to make something useful than simply print banner.
     */
    public
    void invoke () throws StopRequestException {
        banner(logger);
    }

    /**
     * @param logger
     */
    private
    void banner ( Logger logger ) {
        logger.info(format("\nPerforming %s task ...", getClass().getSimpleName()));
    }

}
