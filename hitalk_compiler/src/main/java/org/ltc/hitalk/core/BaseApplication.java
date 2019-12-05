package org.ltc.hitalk.core;


import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.term.io.Environment;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract
class BaseApplication<T extends HtClause, P, Q> implements IApplication {
    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    protected IConfig config;
    protected IProduct product;
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean paused = new AtomicBoolean(false);
    protected final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    protected SymbolTable <Integer, String, Object> symbolTable = new SymbolTableImpl <>();
//    protected PrologWAMCompiler <T, P, Q> compiler; = new PrologWAMCompiler <>(symbolTable, getInterner(), getParser(), LogicCompilerObserver <P, Q> observer  );
    protected HiTalkDefaultBuiltIn defaultBuiltIn;
    protected Runnable target;
    protected String fileName;
    protected State state;

//    /**
//     * @param compiler
//     */
//    public void setInstructionCompiler ( ICompiler <T, P, Q> compiler ) {
//        this.instructionCompiler = compiler;
//    }
//
//    /**
//     * @param preCompiler
//     */
//    public void setPreCompiler ( ICompiler <T, HtPredicate, T> preCompiler ) {
//        this.preCompiler = preCompiler;
//    }

    /**
     * @return
     */
    public HiTalkDefaultBuiltIn getDefaultBuiltIn () {
        return defaultBuiltIn;
    }

    /**
     * @param defaultBuiltIn
     */
    public void setDefaultBuiltIn ( HiTalkDefaultBuiltIn defaultBuiltIn ) {
        this.defaultBuiltIn = defaultBuiltIn;
    }
//
//    /**
//     * @return
//     */
//    public ICompiler <T, HtPredicate, T> getPreCompiler () {
//        return preCompiler;
//    }

    @Override
    public final Logger getLogger () {
        return logger;
    }

    /**
     * @return
     */
    @Override
    public IConfig getConfig () {
        return config;
    }

    /**
     * @param config
     */
    public void setConfig ( IConfig config ) {
        this.config = config;
    }

    /**
     *
     */
    @Override
    public void doClear () {
        initialized.set(false);
    }

    /**
     * @return
     */
    @Override
    public boolean isInited () {
        return initialized.get();
    }

    /**
     * @param b
     * @throws LinkageException
     * @throws IOException
     */
    @Override
    public void setInited ( boolean b ) throws LinkageException, IOException {
        if (b && !isInited()) {
            init();
        }
    }

    /**
     *
     */
    @Override
    public void doInit () throws LinkageException, IOException {
        getLogger().info("Initializing... ");
        initialized.set(true);
    }

    /**
     * @return
     */
    @Override
    public boolean isStarted () {
        return started.get();
    }

    /**
     * @return
     */
    @Override
    public void pause () {
        paused.set(true);
    }

    @Override
    public boolean isPaused () {
        return paused.get();
    }

    @Override
    public void resume () {
        paused.set(false);
    }

    @Override
    public boolean isRunning () {
        return false;
    }//todo

    @Override
    public void abort () {///todo

    }

//    @Override
//    public void shutdown () {//todo
//
//    }

    @Override
    public boolean isShuttingDown () {
        return shuttingDown.get();
    }

    @Override
    public void interrupt () {//todo

    }

    @Override
    public boolean isInterrupted () {
        return false;
    }

    @Override
    public State getState () {
        return state;
    }//todo

    @Override
    public Runnable getTarget () {
        return target;
    }

    /**
     * @return
     */
    @Override
    public IVafInterner getInterner () {
        return Environment.instance().getInterner();
    }

    /**
     * @return
     */
    @Override
    public SymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }

    /**
     * @param symbolTable
     */
    @Override
    public void setSymbolTable ( SymbolTable <Integer, String, Object> symbolTable ) {
        this.symbolTable = symbolTable;
    }

    /**
     * @return
     */
    @Override
    public PlPrologParser getParser () {
        return Environment.instance().getParser(getLanguage());
    }

    /**
     * @return
     */
    public IOperatorTable getOperatorTable () {
        return Environment.instance().getOptable();
    }

    /**
     * @param parser
     */
    @Override
    public void setParser ( PlPrologParser parser ) {
        Environment.instance().setParser(parser);
    }

    /**
     * @param fileName
     */
    @Override
    public void setFileName ( String fileName ) {
        this.fileName = fileName;
    }

    /**
     * @param tokenSource
     */
    @Override
    public void setTokenSource ( PlTokenSource tokenSource ) {
        getParser().setTokenSource(tokenSource);
    }

    /**
     * @return
     */
    @Override
    public PlTokenSource getTokenSource () {
        return getParser().getTokenSource();
    }

    /**
     * @param target
     */
    public void setTarget ( Runnable target ) {
        this.target = target;
    }

    /**
     * @return
     */
    @Override
    public String getFileName () {
        return fileName;
    }

    /**
     * @return
     */
    public ITermFactory getTermFactory () {
        return Environment.instance().getTermFactory();
    }

    /**
     * @param shuttingDown
     */
    public void setShuttingDown ( boolean shuttingDown ) {
        this.shuttingDown.set(shuttingDown);
    }
}
