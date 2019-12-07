package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.HtVersion;
import org.ltc.hitalk.core.IConfigurable;
import org.ltc.hitalk.core.utils.HtSymbolTable;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.Tools;
import org.slf4j.Logger;

import java.io.IOException;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public interface IApplication extends Runnable, IConfigurable {
    /**
     * @return
     */
    Logger getLogger ();

    /**
     * @return
     */
    IConfig getConfig ();

    /**
     * @param config
     */
    void setConfig ( IConfig config );

    /**
     *
     */
    default void init () throws LinkageException, IOException {
        banner();
        try {
            if (!isInited()) {
                doInit();
            }
        } catch (Throwable throwable) {
            try {
                throwable.printStackTrace();
                throw new ExecutionError(PERMISSION_ERROR, null);
            } finally {
                undoInit();
            }
        }
    }

    /**
     * @return
     */
    Language getLanguage ();

    /**
     * @param varOrFunctor
     * @return
     */
    String namespace ( String varOrFunctor );

    /**
     *
     */
    default void clear () throws LinkageException, IOException {
        setInited(false);
        doClear();
    }

    /**
     *
     */
    void doClear ();

    /**
     *
     */
    default void reset () throws LinkageException, IOException {
        if (isInited()) {
            clear();
        }
        init();
    }

    /**
     * @param b
     */
    default void setInited ( boolean b ) throws LinkageException, IOException {
        if (!isInited()) {
            init();
        } else {
            clear();
        }
    }

    /**
     * @return
     */
    boolean isInited ();

    /**
     *
     */
    void doInit () throws LinkageException, IOException;

    /**
     *
     */
    void undoInit ();
    /**
     *
     */
    default void start () throws Exception {
        if (!isStarted()) {
            doStart();
        }
    }

    /**
     * @param b
     */
    default void setStarted ( boolean b ) throws Exception {
        if (b) {
            if (!isStarted()) {
                doStart();
            }
        } else {
            if (isStarted()) {
                pause();
            }
        }
    }

    /**
     * @return
     */
    void pause (); //PAUSE

    /**
     * @return
     */
    boolean isPaused ();

    /**
     *
     */
    void resume (); //RESUME

    /**
     * @return
     */
    boolean isRunning ();

    /**
     *
     */
    void abort ();

    /**
     *
     */
    void shutdown ();

    /**
     *
     * @return
     */
    boolean isShuttingDown ();

    /**
     *
     */
    void interrupt ();

    /**
     * @return
     */
    boolean isInterrupted ();

    /**
     * @return
     */
//    boolean save();
//    boolean restore();
    State getState ();

    /**
     *
     */
    enum State {
        INITED, RUNNING, PAUSED, INTERRUPTED, TERMINATED;
    }

    /**
     * @return
     */
    boolean isStarted ();

    /**
     *
     */
    void doStart () throws Exception;

    /**
     *
     */
    default void run () {
        Runnable target = getTarget();
        if (target != null) {
            target.run();
        }
    }

    /**
     * @return
     */
    Runnable getTarget ();

    /**
     * @return
     */
    IVafInterner getInterner ();

//    /**
//     * @param interner
//     */
//    void setInterner ( IVafInterner interner );

    /**
     * @return
     */
    default ISymbolTable <Integer, String, Object> getSymbolTable () {
        return new HtSymbolTable <>();
    }

    /**
     * @param symbolTable
     */
    void setSymbolTable ( ISymbolTable <Integer, String, Object> symbolTable );

    /**
     * @return
     */
    IProduct product ();

    /**
     * @return
     */
    Language language ();

    /**
     * @return
     */
    Tools.Kind tool ();

    /**
     *
     */
    default void banner () {
        String n = product().getName();
        HtVersion v = product().getVersion();
        String c = product().getCopyright();

        System.err.printf("\n%s %s, %s\n\n", n, v, c);
    }

    /**
     * @param parser
     */
    void setParser ( PlPrologParser parser );

    /**
     * @return
     */
    PlPrologParser getParser () throws IOException;

    /**
     * @param fileName
     */
    void setFileName ( String fileName );

    /**
     * @return
     */
    String getFileName ();

    /**
     * @param tokenSource
     */
    void setTokenSource ( PlTokenSource tokenSource );

    /**
     * @return
     */
    PlTokenSource getTokenSource () throws IOException;
}
