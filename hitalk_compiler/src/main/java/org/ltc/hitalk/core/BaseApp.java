package org.ltc.hitalk.core;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.compiler.VafInterner;
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

    public final static AppContext appContext = new AppContext();
    public final List<HiTalkStream> streams = new ArrayList<>();

    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicBoolean started = new AtomicBoolean(false);
    protected final AtomicBoolean paused = new AtomicBoolean(false);
    protected final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    /**
     * @param parser
     */
    protected IVafInterner interner;
    protected ITermFactory termFactory;
    protected PlPrologParser parser;
    protected IOperatorTable optable;
    protected ICompiler<HtClause, HtPredicate, HtClause> compiler;
    protected PredicateTable<HtPredicate> predicateTable;
    protected ISymbolTable<Integer, String, Object> symbolTable;
    protected IResolver<HtPredicate, HtClause> resolver;
    protected Language language;
    protected IConfig config;
    protected IProduct product;
    protected HiTalkDefaultBuiltIn defaultBuiltIn;
    protected Runnable target;
    protected String fileName;
    protected State state;

    /**
     *
     */
    public BaseApp() {
        appContext.setApp(this);
        try {
            streams.add(new HiTalkInputStream(in, 256));//  "current_input"
            streams.add(new HiTalkOutputStream(256, out));// "current_output"
            streams.add(new HiTalkOutputStream(0, err));// "current_error";

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new ExecutionError(EXISTENCE_ERROR, null);
        }
    }

    public static AppContext getAppContext() {
        return appContext;
    }

    /**
     * @param i
     * @return
     */
    public HiTalkStream getStream(int i) {
        return streams.get(i);
    }

    /**
     * @return
     */
    public HiTalkStream currentInput() {
        return getStream(0);
    }

    /**
     * @return
     */
    public HiTalkStream currentOutput() {
        return getStream(1);
    }

    /**
     * @return
     */
    public HiTalkStream currentError() {
        return getStream(2);
    }

    /**
     * @return
     */
    public HiTalkDefaultBuiltIn getDefaultBuiltIn() {
        return defaultBuiltIn;
    }

    /**
     * @param defaultBuiltIn
     */
    public void setDefaultBuiltIn(HiTalkDefaultBuiltIn defaultBuiltIn) {
        this.defaultBuiltIn = defaultBuiltIn;
    }

    /**
     * @return
     */
    public final Logger getLogger() {
        return logger;
    }

    /**
     * @return
     */
    @Override
    public IConfig getConfig() {
        return config;
    }

    /**
     * @param config
     */
    public void setConfig(IConfig config) {
        this.config = config;
    }

    /**
     *
     */
    @Override
    public void doClear() {
        initialized.set(false);
    }

    /**
     * @return
     */
    @Override
    public boolean isInited() {
        return initialized.get();
    }

    /**
     * @param inited
     * @throws LinkageException
     * @throws IOException
     */
    @Override
    public void setInited(boolean inited) throws LinkageException, IOException {
        if (inited && !isInited()) {
            init();
        }
    }

    /**
     * @param stream
     * @return
     */
    public HiTalkStream addStream(HiTalkStream stream) {
        streams.add(stream);
        return stream;
    }

    /**
     * @return
     */
    public List<HiTalkStream> getStreams() {
        return streams;
    }

    /**
     * @return
     */
    public IResolver<HtPredicate, HtClause> getResolver() {
        return resolver;
    }

    /**
     *
     */
    @Override
    public void doInit() throws Exception {
        getLogger().info("Initializing... ");
        appContext.setApp(this);
        initComponents();
        initialized.set(true);
    }

    /**
     *
     */
    protected abstract void initComponents() throws Exception;

    /**
     * @return
     */
    @Override
    public boolean isStarted() {
        return started.get();
    }

    /**
     * @return
     */
    @Override
    public void pause() {
        paused.set(true);
    }

    /**
     * @return
     */
    @Override
    public boolean isPaused() {
        return paused.get();
    }

    /**
     *
     */
    @Override
    public void resume() {
        paused.set(false);
    }

    /**
     * @return
     */
    @Override
    public boolean isRunning() {
        return false;
    }//todo

    @Override
    public void abort() {///todo

    }

//    @Override
//    public void shutdown () {//todo
//
//    }

    @Override
    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    /**
     * @param shuttingDown
     */
    public void setShuttingDown(boolean shuttingDown) {
        this.shuttingDown.set(shuttingDown);
    }

    @Override
    public void interrupt() {//todo

    }

    @Override
    public boolean isInterrupted() {
        return false;
    }

    @Override
    public State getState() {
        return state;
    }//todo

    @Override
    public Runnable getTarget() {
        return target;
    }

    /**
     * @param target
     */
    public void setTarget(Runnable target) {
        this.target = target;
    }

    /**
     * @return
     */
    @Override
    public IVafInterner getInterner() {
        return interner;
    }

    /**
     * @return
     */
    @Override
    public ISymbolTable<Integer, String, Object> getSymbolTable() {
        return symbolTable;
    }

    /**
     * @param symbolTable
     */
    @Override
    public void setSymbolTable(ISymbolTable<Integer, String, Object> symbolTable) {
        this.symbolTable = symbolTable;
        appContext.put(SYMBOL_TABLE, (IHitalkObject) symbolTable);
    }

    /**
     * @return
     */
    @Override
    public PlPrologParser getParser() {
        return parser;
    }

    /**
     * @param parser
     */
    @Override
    public void setParser(PlPrologParser parser) {
        this.parser = parser;
        appContext.putIfAbsent(PARSER, (IHitalkObject) parser);
    }

    /**
     * @return
     */
    public IOperatorTable getOperatorTable() {
        return optable;
    }

    /**
     * @return
     */
    @Override
    public PlLexer getTokenSource() {
        return getParser().getTokenSource();
    }

    /**
     * @param tokenSource
     */
//    @Override
    public void setTokenSource(PlLexer tokenSource) {
        getParser().setTokenSource(tokenSource);
    }

    /**
     * @return
     */
    @Override
    public String getFileName() {
        return fileName;
    }

    /**
     * @param fileName
     */
    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /**
     * @return
     */
    public ITermFactory getTermFactory() {
        return termFactory;
    }

    public PredicateTable<HtPredicate> getPredicateTable() {
        return predicateTable;
    }

    /**
     *
     */
    public static class AppContext extends HashMap<Components, IHitalkObject> {
        private HiTalkInputStream inputStream;
        private PlLexer tokenSource;
        private IVafInterner interner;
        private String[] nameSpace;
        private IApplication app;

        /**
         * Creates an empty enum map with the specified key type.
         *
         * @param keyType the class object of the key type for this enum map
         * @param streams
         * @throws NullPointerException if <tt>keyType</tt> is null
         */
        public AppContext() {
        }

        /**
         * @return
         */
        public String[] getNameSpace() {
            if (nameSpace == null) {
                nameSpace = new String[2];
                final String prefix = appContext.getApp().language().getName();
                nameSpace[0] = prefix + "_Variable_namespace";
                nameSpace[1] = prefix + "_Functor_namespace";
            }

            return nameSpace;
        }

        /**
         * @return
         */
        public HiTalkInputStream currentInput() {
            return (HiTalkInputStream) get(CURRENT_INPUT);
        }

        public PredicateTable getPredicateTable() {
            return (PredicateTable) get(PRED_TABLE);
        }

        public PlPrologParser getParser() throws Exception {
            PlPrologParser parser = (PlPrologParser) get(PARSER);
            if (parser == null) {
                parser = new PlPrologParser();
            }
            put(PARSER, parser);
            return parser;
        }

        /**
         * @param vns
         * @param fns
         * @return
         */
        public IVafInterner getInterner() {
            IVafInterner interner = (IVafInterner) get(INTERNER);
            if (interner == null) {
                interner = new VafInterner(getNameSpace());
            }
            putIfAbsent(INTERNER, interner);
            return interner;
        }

        public IResolver<HtPredicate, HtClause> getResolverPre() {
            return (IResolver<HtPredicate, HtClause>) get(RESOLVER_PRE);
        }

        public IResolver<HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> getResolverIC() {
            return (IResolver<HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery>) get(RESOLVER_IC);
        }

        public ISymbolTable<Integer, String, Object> getSymbolTable() {
            return (ISymbolTable<Integer, String, Object>) get(SYMBOL_TABLE);
        }

        /**
         * @return
         */
        public ITermFactory getTermFactory() {
            IHitalkObject tf = get(TERM_FACTORY);
            if (tf == null) {
                String[] ns = getNameSpace();
                tf = new TermFactory(getInterner(ns));
                putIfAbsent(TERM_FACTORY, tf);
            }
            return (ITermFactory) tf;
        }

        public void setTermFactory(IVafInterner interner) {
            putIfAbsent(TERM_FACTORY, (IHitalkObject) new TermFactory(interner));
        }

        /**
         * @return
         */
        public IOperatorTable getOpTable() {
            IOperatorTable table = (IOperatorTable) appContext.get(OP_TABLE);
            if (table == null) {
                table = new PlDynamicOperatorParser();
                setOpTable(table);
            }

            return table;
        }

        private void setOpTable(IOperatorTable table) {
            appContext.putIfAbsent(OP_TABLE, table);
        }

        public PrologDefaultBuiltIn getDefaultBuiltIn() {
            return (PrologDefaultBuiltIn) get(DEFAULT_BUILTIN);
        }

        public PrologBuiltInTransform getBuiltInTransform() {
            return (PrologBuiltInTransform) get(BUILTIN_TRANSFORM);
        }

        public <PC, QC> ICompilerObserver<PC, QC> getObserverIC() {
            final ICompilerObserver<PC, QC> observer = (ICompilerObserver<PC, QC>) get(OBSERVER_IC);
            if (observer == null) {
                setObserverIC(new ChainedCompilerObserver());
            }
            return (ICompilerObserver<PC, QC>) get(OBSERVER_IC);
        }

        public <PC, QC> void setObserverIC(ICompilerObserver observer) {
            putIfAbsent(OBSERVER_IC, (IHitalkObject) observer);
        }

        public <PC, QC> ICompilerObserver<PC, QC> getObserverPre() {
            final ICompilerObserver<PC, QC> observer = (ICompilerObserver<PC, QC>) get(OBSERVER_PRE);
            if (observer == null) {
                setObserverPre(new ICompilerObserver<PC, QC>() {//fixme
                    public void onCompilation(PC sentence) throws HtSourceCodeException {

                    }

                    public void onQueryCompilation(QC sentence) throws HtSourceCodeException {

                    }
                });
            }
            return (ICompilerObserver<PC, QC>) get(OBSERVER_PRE);
        }

        public <PC, QC> void setObserverPre(ICompilerObserver observer) {
            putIfAbsent(OBSERVER_IC, (IHitalkObject) observer);
        }

        public <T extends HtClause, P, Q, PC, QC> void setCompilerFactory(ICompilerFactory<T, P, Q, PC, QC> cf) {
            putIfAbsent(COMPILER_FACTORY, cf);
        }

        public HiTalkInputStream getInputStream() {
            return (HiTalkInputStream) getStream();
        }

        public void setInputStream(HiTalkInputStream inputStream) {
            setStream(inputStream);
        }

        private HiTalkStream getStream() {
            return (HiTalkStream) get(STREAM);
        }

        public void setStream(HiTalkStream stream) {
            putIfAbsent(STREAM, stream);
        }

        public PlLexer getTokenSource() {
            return tokenSource;
        }

        protected void setOptable(IOperatorTable optable) {
            putIfAbsent(OP_TABLE, optable);
        }

        /**
         * @param nameSpace
         * @return
         */
        public IVafInterner getInterner(String[] nameSpace) {
            if (interner == null) {
                return new VafInterner(nameSpace[0], nameSpace[1]);
            }
            return interner;
        }

        public IApplication getApp() {
            return app;
        }

        public void setApp(IApplication app) {
            this.app = app;
        }
    }
}
