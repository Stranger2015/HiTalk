package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.HtVersion;
import org.ltc.hitalk.core.IConfigurable;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.Tools;
import org.slf4j.Logger;

import java.io.IOException;

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

//    /**
//     * @param symbolTable
//     * @param interner
//     * @param parser
//     * @return
//     */
//    BaseCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> createWAMCompiler (
//            SymbolTable <Integer, String, Object> symbolTable,
//            VariableAndFunctorInterner interner,
//            PlPrologParser parser );

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
                throw new ExecutionError(ExecutionError.Kind.PERMISSION_ERROR, null);
            } finally {
                undoInit();
            }
        }
    }

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

    default void run () {
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

//    /**
//     * @param interner
//     */
//    void setInterner ( VariableAndFunctorInterner interner );

    default SymbolTable <Integer, String, Object> getSymbolTable () {
        return new SymbolTableImpl <>();
    }

    void setSymbolTable ( SymbolTable <Integer, String, Object> symbolTable );

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
    Tools tool ();

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
