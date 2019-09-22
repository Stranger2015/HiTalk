package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.core.IConfigurable;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import java.io.FileNotFoundException;

/**
 *
 */
public
interface IApplication<T extends HtClause> extends Runnable, IConfigurable {

    /**
     * @return
     */
    IConfig getConfig ();

    void setConfig ( IConfig config );

    /**
     *
     */
    default
    void init () throws LinkageException, FileNotFoundException {
        banner();
        if (!isInited()) {
            doInit();
        }
    }

    /**
     *
     */
    default
    void clear () throws LinkageException, FileNotFoundException {
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
    void reset () throws LinkageException, FileNotFoundException {
        if (isInited()) {
            clear();
        }
        init();
    }

    /**
     * @param b
     */
    default
    void setInited ( boolean b ) throws LinkageException, FileNotFoundException {
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
    void doInit () throws LinkageException, FileNotFoundException;

    /**
     *
     */
    default
    void start () throws Exception {
        if (!isStarted()) {
            //      setStarted(true);
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
    void doStart ();

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

    void setInterner ( VariableAndFunctorInterner interner );

    SymbolTable <Integer, String, Object> getSymbolTable ();

    void setSymbolTable ( SymbolTable <Integer, String, Object> symbolTable );

    /**
     *
     */
    void banner ();

    /**
     * @param parser
     */
    void setParser ( HtPrologParser <T> parser );

    /**
     * @return
     */
    HtPrologParser <T> getParser ();

    /**
     * @param fileName
     */
    void setFileName ( String fileName );

    String getFileName ();

    /**
     * @param tokenSource
     */
    void setTokenSource ( HtTokenSource tokenSource );

    HtTokenSource getTokenSource ();
}
