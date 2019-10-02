package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.core.IConfigurable;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HtTokenSource;
import org.slf4j.Logger;

import java.io.IOException;

/**
 *
 */
public
interface IApplication extends Runnable, IConfigurable {
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
    default
    void init () throws LinkageException, IOException {
        banner();
        if (!isInited()) {
            doInit();
        }
    }

    /**
     *
     */
    default
    void clear () throws LinkageException, IOException {
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
    default
    void reset () throws LinkageException, IOException {
        if (isInited()) {
            clear();
        }
        init();
    }

    /**
     * @param b
     */
    default
    void setInited ( boolean b ) throws LinkageException, IOException {
        if (!isInited()) {
            init();
        }
        else {
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
    default
    void start () throws Exception {
        if (!isStarted()) {
            doStart();
        }
    }

    /**
     * @param b
     */
    default
    void setStarted ( boolean b ) throws Exception {
        if (b) {
            if (!isStarted()) {
                doStart();
            }
        }
        else {
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
     */
    void isShuttingDown ();

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
        INITED,
        RUNNING,
        PAUSED,
        INTERRUPTED,
        TERMINATED;
    }


    /**
     * @return
     */
    boolean isStarted ();

    /**
     *
     */
    void doStart () throws Exception;

    default
    void run () {
        Runnable target = getTarget();
        if (target != null) {
            target.run();
        }
    }

    Runnable getTarget ();

    /**
     * @return
     */
    VariableAndFunctorInterner getInterner ();

    /**
     * @param interner
     */
    void setInterner ( VariableAndFunctorInterner interner );

    default
    SymbolTable <Integer, String, Object> getSymbolTable () {
        return null;
    }

    void setSymbolTable ( SymbolTable <Integer, String, Object> symbolTable );

    /**
     *
     */
    void banner ();

    /**
     * @param parser
     */
    void setParser ( HtPrologParser parser );

    /**
     * @return
     */
    HtPrologParser getParser () throws IOException;

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
    void setTokenSource ( HtTokenSource tokenSource );

    /**
     * @return
     */
    HtTokenSource getTokenSource () throws IOException;
}
