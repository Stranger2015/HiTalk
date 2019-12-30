package org.ltc.hitalk.core;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.compiler.bktables.*;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.term.io.HiTalkOutputStream;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.ICompilerFactory;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.ChainedCompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.PrologBuiltInTransform;
import org.ltc.hitalk.wam.compiler.prolog.PrologDefaultBuiltIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.io.FileDescriptor.*;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.EXISTENCE_ERROR;
import static org.ltc.hitalk.core.Components.*;
import static org.ltc.hitalk.core.PrologBuiltIns.CURRENT_INPUT;

/**
 *
 */
@SuppressWarnings("ALL")
public abstract
class BaseApp<T extends HtClause, P, Q, PC, QC> implements IApplication {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    public final List <HiTalkStream> streams = new ArrayList <>();

    public final static AppContext appContext = new AppContext();

    public static AppContext getAppContext () {
        return appContext;
    }

    /**
     * @param i
     * @return
     */
    public HiTalkStream getStream ( int i ) {
        return streams.get(i);
    }

    /**
     * @return
     */
    public HiTalkStream currentInput () {
        return getStream(0);
    }

    /**
     * @return
     */
    public HiTalkStream currentOutput () {
        return getStream(1);
    }

    /**
     * @return
     */
    public HiTalkStream currentError () {
        return getStream(2);
    }


    /**
     * @param parser
     */
    protected IVafInterner interner;

    protected ITermFactory termFactory;
    protected PlPrologParser parser;
    protected IOperatorTable optable;
    protected ICompiler <HtClause, HtPredicate, HtClause> compiler;
    protected PredicateTable <HtPredicate> predicateTable;
    protected ISymbolTable <Integer, String, Object> symbolTable;
    protected IResolver <HtPredicate, HtClause> resolver;
    protected Language language;

    protected IConfig config;
    protected IProduct product;
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean paused = new AtomicBoolean(false);
    protected final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    protected HiTalkDefaultBuiltIn defaultBuiltIn;
    protected Runnable target;
    protected String fileName;
    protected State state;

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

    /**
     * @return
     */
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
     * @param inited
     * @throws LinkageException
     * @throws IOException
     */
    @Override
    public void setInited ( boolean inited ) throws LinkageException, IOException {
        if (inited && !isInited()) {
            init();
        }
    }

    /**
     * @param stream
     * @return
     */
    public HiTalkStream addStream ( HiTalkStream stream ) {
        streams.add(stream);
        return stream;
    }

    /**
     * @return
     */
    public List <HiTalkStream> getStreams () {
        return streams;
    }

    public IResolver <HtPredicate, HtClause> getResolver () {
        return resolver;// == null ? new HtResolutionEngine <>(getParser((), getInterner(), getCompiler()) : resolver;//todo
    }

    /**
     *
     */
    public BaseApp () {
        try {
            streams.add(new HiTalkInputStream(in, 256));//  "current_input"
            streams.add(new HiTalkOutputStream(256, out));// "current_output"
            streams.add(new HiTalkOutputStream(0, err));// "current_error";

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ExecutionError(EXISTENCE_ERROR, null);
        }
    }

    /**
     *
     */
    @Override
    public void doInit () throws LinkageException, IOException {
        getLogger().info("Initializing... ");
        initComponents();
        initialized.set(true);
    }

    /**
     *
     */
    protected abstract void initComponents () throws FileNotFoundException;

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

    /**
     * @return
     */
    @Override
    public boolean isPaused () {
        return paused.get();
    }

    /**
     *
     */
    @Override
    public void resume () {
        paused.set(false);
    }

    /**
     * @return
     */
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
        return interner;
    }

    /**
     * @return
     */
    @Override
    public ISymbolTable <Integer, String, Object> getSymbolTable () {
        return symbolTable;
    }

    /**
     * @param symbolTable
     */
    @Override
    public void setSymbolTable ( ISymbolTable <Integer, String, Object> symbolTable ) {
        this.symbolTable = symbolTable;
        appContext.put(SYMBOL_TABLE, (IHitalkObject) symbolTable);
    }

    /**
     * @return
     */
    @Override
    public PlPrologParser getParser () {
        return parser;
    }

    /**
     * @return
     */
    public IOperatorTable getOperatorTable () {
        return optable;
    }

    /**
     * @param parser
     */
    @Override
    public void setParser ( PlPrologParser parser ) {
        this.parser = parser;
        appContext.putIfAbsent(PARSER, (IHitalkObject) parser);
    }

    /**
     * @param fileName
     */
    @Override
    public void setFileName ( String fileName ) {
        this.fileName = fileName;
//        BaseApp.getAppContext().putIfAbsent(FILE_NAME, fileName);
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
        return termFactory;
    }

    /**
     * @param shuttingDown
     */
    public void setShuttingDown ( boolean shuttingDown ) {
        this.shuttingDown.set(shuttingDown);
    }

    public PredicateTable <HtPredicate> getPredicateTable () {
        return predicateTable;
    }

    /**
     *
     */
    public static class AppContext extends HashMap <Components, IHitalkObject> {

        private HiTalkInputStream inputStream;
        private PlTokenSource tokenSource;

        /**
         * Creates an empty enum map with the specified key type.
         *
         * @param keyType the class object of the key type for this enum map
         * @param streams
         * @throws NullPointerException if <tt>keyType</tt> is null
         */
        public AppContext () {
        }

        public HiTalkInputStream currentInput () {
            return (HiTalkInputStream) get(CURRENT_INPUT);
        }

        public PredicateTable getPredicateTable () {
            return (PredicateTable) get(PRED_TABLE);
        }

        public PlPrologParser getParser () {
            PlPrologParser parser = (PlPrologParser) get(PARSER);
            if (parser == null) {
                parser = new PlPrologParser();
            }
            put(PARSER, parser);
            return parser;
        }

        public IVafInterner getInterner () {
            return (IVafInterner) get(INTERNER);
        }

        public IResolver <HtPredicate, HtClause> getResolverPre () {
            return (IResolver <HtPredicate, HtClause>) get(RESOLVER_PRE);
        }

        public IResolver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> getResolverIC () {
            return (IResolver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery>) get(RESOLVER_IC);
        }

        public ISymbolTable <Integer, String, Object> getSymbolTable () {
            return (ISymbolTable <Integer, String, Object>) get(SYMBOL_TABLE);
        }

        public ITermFactory getTermFactory () {
            return (ITermFactory) get(TERM_FACTORY);
        }

        /**
         * @return
         */
        public IOperatorTable getOpTable () {
            IOperatorTable table = (IOperatorTable) appContext.get(OP_TABLE);
            if (table == null) {
                table = new PlDynamicOperatorParser();
                setOpTable(table);
            }
            return table;
        }

        private void setOpTable ( IOperatorTable table ) {
            appContext.putIfAbsent(OP_TABLE, table);
        }

        public PrologDefaultBuiltIn getDefaultBuiltIn () {
            return (PrologDefaultBuiltIn) get(DEFAULT_BUILTIN);
        }

        public PrologBuiltInTransform getBuiltInTransform () {
            return (PrologBuiltInTransform) get(BUILTIN_TRANSFORM);
        }

        public <PC, QC> void setObserverIC ( ICompilerObserver observer ) {
            putIfAbsent(OBSERVER_IC, (IHitalkObject) observer);
        }

        public <PC, QC> ICompilerObserver <PC, QC> getObserverIC () {
            final ICompilerObserver <PC, QC> observer = (ICompilerObserver <PC, QC>) get(OBSERVER_IC);
            if (observer == null) {
                setObserverIC(new ChainedCompilerObserver());
            }
            return (ICompilerObserver <PC, QC>) get(OBSERVER_IC);
        }

        public <PC, QC> void setObserverPre ( ICompilerObserver observer ) {
            putIfAbsent(OBSERVER_IC, (IHitalkObject) observer);
        }

        public <PC, QC> ICompilerObserver <PC, QC> getObserverPre () {
            final ICompilerObserver <PC, QC> observer = (ICompilerObserver <PC, QC>) get(OBSERVER_PRE);
            if (observer == null) {
                setObserverPre(new ICompilerObserver <PC, QC>() {//fixme
                    public void onCompilation ( PC sentence ) throws HtSourceCodeException {

                    }

                    public void onQueryCompilation ( QC sentence ) throws HtSourceCodeException {

                    }
                });
            }
            return (ICompilerObserver <PC, QC>) get(OBSERVER_PRE);
        }


        public <T extends HtClause, P, Q, PC, QC> void setCompilerFactory ( ICompilerFactory <T, P, Q, PC, QC> cf ) {
            putIfAbsent(COMPILER_FACTORY, cf);
        }

        public void setStream ( HiTalkStream stream ) {
            putIfAbsent(STREAM, stream);
        }

        public void setTermFactory ( IVafInterner interner ) {
            putIfAbsent(TERM_FACTORY, (IHitalkObject) new TermFactory(interner));
        }

        public void setInputStream ( HiTalkInputStream inputStream ) {
            setStream(inputStream);
        }

        public HiTalkInputStream getInputStream () {
            return (HiTalkInputStream) getStream();
        }

        private HiTalkStream getStream () {
            return (HiTalkStream) get(STREAM);
        }

        public PlTokenSource getTokenSource () {
            return tokenSource;
        }

        protected void setOptable ( IOperatorTable optable ) {
            putIfAbsent(OP_TABLE, optable);
        }
    }
}
