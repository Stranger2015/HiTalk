package org.ltc.hitalk.core;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public abstract
class BaseApplication<T extends HtClause, P, Q> implements IApplication <T> {//
//    Сидоров Игорь Анатольевич (ИНН 500804331824)


    protected IConfig config;
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean paused = new AtomicBoolean(false);

    protected SymbolTable <Integer, String, Object> symbolTable;
    protected VariableAndFunctorInterner interner;
    protected HtPrologParser <T> parser;
    protected ICompiler <T, P, Q> instructionCompiler;

    protected ICompiler <T, HtPredicate, T> preCompiler;
    protected HiTalkDefaultBuiltIn defaultBuiltIn;
    protected Runnable target;
    protected String fileName;

    public
    ICompiler <T, P, Q> getInstructionCompiler () {
        return instructionCompiler;
    }

    public
    void setInstructionCompiler ( ICompiler <T, P, Q> compiler ) {
        this.instructionCompiler = compiler;
    }

    public
    void setPreCompiler ( ICompiler <T, HtPredicate, T> preCompiler ) {
        this.preCompiler = preCompiler;
    }

    public
    HiTalkDefaultBuiltIn getDefaultBuiltIn () {
        return defaultBuiltIn;
    }

    public
    void setDefaultBuiltIn ( HiTalkDefaultBuiltIn defaultBuiltIn ) {
        this.defaultBuiltIn = defaultBuiltIn;
    }

    /**
     * @return
     */
    public
    ICompiler <T, HtPredicate, T> getPreCompiler () {
        return preCompiler;
    }

    /**
     * @return
     */
    @Override
    public
    IConfig getConfig () {
        return config;
    }

    /**
     * @param config
     */
    public
    void setConfig ( IConfig config ) {
        this.config = config;
    }

    /**
     *
     */
    @Override
    public
    void doClear () {
        initialized.set(false);
    }

    /**
     * @return
     */
    @Override
    public
    boolean isInited () {
        return initialized.get();
    }

    @Override
    public
    void setInited ( boolean b ) throws LinkageException, FileNotFoundException {
        if (b && !isInited()) {
            init();
        }
    }

    /**
     *
     */
    @Override
    public
    void doInit () throws LinkageException, FileNotFoundException {
        initialized.set(true);
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStarted () {
        return started.get();
    }

    /**
     * @return
     */
    @Override
    public
    void pause () {
        paused.set(true);
    }

    @Override
    public
    boolean isPaused () {
        return paused.get();
    }

    @Override
    public
    void resume () {
        paused.set(false);
    }

    @Override
    public
    boolean isRunning () {
        return false;
    }//todo

    @Override
    public
    void abort () {///todo

    }

    @Override
    public
    void shutdown () {//todo

    }

    @Override
    public
    void isShuttingDown () {///todo

    }

    @Override
    public
    void interrupt () {//todo

    }

    @Override
    public
    boolean isInterrupted () {
        return false;
    }

    @Override
    public
    State getState () {
        return null;
    }//todo
//
//    /**
//     *
//     */
//    @Override
//    public
//    void doStart () {
//
//    }

    @Override
    public
    Runnable getTarget () {
        return target;
    }

    /**
     * @return
     */
    @Override
    public
    VariableAndFunctorInterner getInterner () {
        return interner;
    }

    @Override
    public
    void setInterner ( VariableAndFunctorInterner interner ) {
        this.interner = interner;
    }

    @Override
    public
    SymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }

    @Override
    public
    void setSymbolTable ( SymbolTable <Integer, String, Object> symbolTable ) {
        this.symbolTable = symbolTable;
    }

    /**
     *
     */
    @Override
    public
    void banner () {
    }

    /**
     * @return
     */
    @Override
    public
    HtPrologParser <T> getParser () {
        return parser;
    }

    /**
     * @param parser
     */
    @Override
    public
    void setParser ( HtPrologParser <T> parser ) {
        this.parser = parser;
    }

    /**
     * @param fileName
     */
    @Override
    public
    void setFileName ( String fileName ) {
        this.fileName = fileName;
    }

    /**
     * @param tokenSource
     */
    @Override
    public
    void setTokenSource ( HtTokenSource tokenSource ) {
        parser.setTokenSource(tokenSource);
    }

    @Override
    public
    HtTokenSource getTokenSource () {
        return parser.getTokenSource();
    }

    /**
     * @param target
     */
    public
    void setTarget ( Runnable target ) {
        this.target = target;
    }

    /**
     * @return
     */
    public
    String getFileName () {
        return fileName;
    }
}
