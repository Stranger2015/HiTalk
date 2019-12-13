package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.apache.commons.vfs2.provider.jar.JarFileProvider;
import org.apache.commons.vfs2.provider.local.DefaultLocalFileProvider;
import org.apache.commons.vfs2.provider.ram.RamFileProvider;
import org.apache.commons.vfs2.provider.res.ResourceFileProvider;
import org.apache.commons.vfs2.provider.temp.TemporaryFileProvider;
import org.apache.commons.vfs2.provider.url.UrlFileProvider;
import org.apache.commons.vfs2.provider.zip.ZipFileProvider;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.VafInterner;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.core.HtVersion;
import org.ltc.hitalk.core.utils.HtSymbolTable;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.CompilationContext;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.interpreter.HtProduct;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.CompilerFactory;
import org.ltc.hitalk.wam.compiler.ICompilerFactory;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.LibParser;
import org.ltc.hitalk.wam.compiler.Tools.Kind;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledClause;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.core.Components.INTERNER;
import static org.ltc.hitalk.core.Components.WAM_COMPILER;
import static org.ltc.hitalk.term.io.HiTalkStream.createHiTalkStream;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;
import static org.ltc.hitalk.wam.compiler.Tools.Kind.COMPILER;

/**
 *
 */
public class PrologCompilerApp<T extends HtClause, P, Q> extends BaseApp <T, P, Q> {

    public static final String DEFAULT_SCRATCH_DIRECTORY = "scratch";
    public static final HtProperty[] DEFAULT_PROPS = new HtProperty[]{

    };

    protected DefaultFileSystemManager fsManager;
    protected PrologWAMCompiler <HtClause, HtPredicate, HtClause,
            HiTalkWAMCompiledPredicate, HiTalkWAMCompiledClause> wamCompiler;

    protected Path scratchDirectory = Paths.get(DEFAULT_SCRATCH_DIRECTORY).toAbsolutePath();
    protected CompilationContext compilationContext = new CompilationContext();
    protected LoadContext loadContext = new LoadContext(DEFAULT_PROPS);
    protected ExecutionContext executionContext = new ExecutionContext();
    protected IProduct product = new HtProduct("Copyright (c) Anton Danilov 2018-2019, All rights reserved",
            language().getName() + " " + tool().getName(),
            new HtVersion(0, 1, 0, 99, " ", true));
    protected LogicCompilerObserver <P, Q> observer;
    protected IVafInterner interner;
    protected PrologPreCompiler <HtClause, Object, Object> preCompiler;

    /**
     * @param fn
     */
    public PrologCompilerApp ( String fn ) {
        fileName = fn;
    }

    /**
     * @param args
     */
    public static void main ( String[] args ) {
        try {
            IApplication application = new PrologCompilerApp <>(args[0]);
            application.init();
            application.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }

//    /**
//     * @param symbolTable
//     * @param interner
//     * @param parser
//     * @param observer
//     * @return
//     */
//    public PrologWAMCompiler <T, P, Q, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery>
//
//    createWAMCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
//                        IVafInterner interner,
//                        PlPrologParser parser,
//                        LogicCompilerObserver <P, Q> observer ) {
//        return new PrologWAMCompiler(symbolTable, interner, parser, observer);
//    }

    /**
     * @return
     */
    public Language getLanguage () {
        return language;
    }

    @Override
    public String namespace ( String varOrFunctor ) {
        return "namespace " + varOrFunctor;
    }

    /**
     * @return
     */
    public LoadContext getLoadContext () {
        if (loadContext == null) {
            loadContext = new LoadContext(DEFAULT_PROPS);
        }
        return loadContext;
    }

    /**
     *
     */
    @Override
    public void doInit () throws LinkageException, IOException {
        super.doInit();

        compilationContext = new CompilationContext();
        loadContext = new LoadContext(DEFAULT_PROPS);
        executionContext = new ExecutionContext();

        initVfs();
    }

    @Override
    protected void initComponents () {
        final AppContext appCtx = BaseApp.getAppContext();
        final ICompilerFactory <HtClause, Object, Object, Object, Object> cf = new CompilerFactory <>();
        setSymbolTable(new HtSymbolTable <>());
        setInterner(new VafInterner(
                language().getName() + "_Variable_Namespace",//todo language
                language().getName() + "_Functor_Namespace"));
        appCtx.setStream(createHiTalkStream(fileName, true));
        appCtx.setTermFactory(appCtx.getInterner());
        setParser((PlPrologParser) cf.createParser(language()));
        setWAMCompiler(cf.createWAMCompiler(language()));

    }

    /**
     *
     */
    @Override
    public void undoInit () {
        getLogger().info("cancelled!");
        initialized.set(false);
    }

//    /**
//     * @param symbolTable
//     * @param interner
//     * @param observer
//     * @param parser
//     * @return
//     */
//    public BaseCompiler <T, P, Q>
//    createWAMCompiler ( ISymbolTable <Integer, String, Object> symbolTable,
//                        IVafInterner interner,
//                        LogicCompilerObserver <P, Q> observer,
//                        PlPrologParser parser ) {
//        return new PrologWAMCompiler(symbolTable, interner, parser, observer);
//    }

    /**
     * @param stream
     * @param interner
     * @param factory
     * @param optable
     * @return
     */
    public IParser createParser ( HiTalkStream stream,
                                  IVafInterner interner,
                                  ITermFactory factory,
                                  IOperatorTable optable ) {
        return new PlPrologParser(stream, interner, factory, optable);
    }

    /**
     *
     */
    protected void initVfs () {
        try {
            fsManager = new DefaultFileSystemManager();
//            fsManager.setLogger((Log) logger);
            fsManager.addProvider("file", new DefaultLocalFileProvider());
            fsManager.addProvider("zip", new ZipFileProvider());
            fsManager.addProvider("ram", new RamFileProvider());
            fsManager.addProvider("jar", new JarFileProvider());
            fsManager.addProvider("temp", new TemporaryFileProvider());//ram zip
            fsManager.addProvider("res", new ResourceFileProvider());
            fsManager.addProvider("url", new UrlFileProvider());
//            fsManager.addProvider("vfs2nio", new org.ltc.hitalk.com.sshtools.vfs2nio.Vfs2NioFileSystemProvider());
//
//            /* Creation of root directory for VFS */
//            is = new InputStreamReader(System.in);
//            br = new BufferedReader(is);
//            File f = new File("C:\\");
//            URI uri = f.toURI();
//            fsManager.setBaseFile(new File(System.getProperty("user.dir")));

            fsManager.setCacheStrategy(CacheStrategy.ON_RESOLVE);

            fsManager.init();

        } catch (IOException e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }

    /**
     *
     */
    public void initialize () throws Exception {
        initDirectives();
        cacheCompilerFlags();
        Path scratchDirectory = loadBuiltIns();
        startRuntimeThreading();
        Path path = Paths.get("c:\\Users\\Anthony_2\\IdeaProjects\\WAM\\hitalk_compiler\\src\\main\\resources\\empty.pl");
        Object result = loadSettingsFile(path);
        reportSettingsFile(result);
    }

    private void reportSettingsFile ( Object result ) {

    }

    /**
     * @param path
     * @return
     * @throws IOException
     */
    protected Object loadSettingsFile ( Path path ) throws Exception {
        // Create a token source to load the model rules from.
//        InputStream input = getClass().getClassLoader().getResourceAsStream(BUILT_IN_LIB);
        logger.debug("Loading settings...");
        PlTokenSource tokenSource = PlTokenSource.getTokenSourceForIoFile(path.toFile());
        // Set up a parser on the token source.
        LibParser libParser = new LibParser();
        libParser.setTokenSource(tokenSource);

        // Load the built-ins into the domain
        while (true) {
            ISentence <ITerm> sentence = libParser.parse();
            final ITerm term = sentence.getT();
            //TODO  GLOBAL WITHOUT SPECIAL CASES
            if (term == PlPrologParser.BEGIN_OF_FILE_ATOM) {//ignore
                //    final List <ITerm> l = preCompiler.expandTerm(term);
                continue;
            }
            if (term == PlPrologParser.END_OF_FILE_ATOM) {
                if (!libParser.getTokenSource().isEofGenerated()) {
                    parser.popTokenSource();
                    break;
                }
            }
            //            compiler.compile(sentence);
            HtClause clause = libParser.convert(sentence.getT());
            wamCompiler.compile(clause);
        }
        wamCompiler.endScope();
//         There should not be any errors in the built in library, if there are then the prolog engine just
//         isn't going to work, so report this as a bug.
//        throw new IllegalStateException("Got an exception whilst loading the built-in library.", e);

        return path;
    }

    private void startRuntimeThreading () {

    }

    private Path loadBuiltIns () {
        return null;
    }

    private void cacheCompilerFlags () {
    }

    private void initDirectives () {
    }

    /**
     * @throws Exception
     */
    @Override
    public void doStart () throws Exception {
        getLogger().info("Starting... ");
        setTarget(() -> {
            try {
                this.initialize();
                getWAMCompiler().compile(fileName, loadContext.getFlags());
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(PERMISSION_ERROR, null);
            }
        });
        getLogger().info("Running target... ");
        getTarget().run();
    }

    public void setInterner ( IVafInterner interner ) {
        this.interner = interner;
        appContext.putIfAbsent(INTERNER, interner);
    }

    protected PrologWAMCompiler <HtClause, HtPredicate, HtClause,
            HiTalkWAMCompiledPredicate, HiTalkWAMCompiledClause> getWAMCompiler () {
        return wamCompiler;
    }

    protected void setWAMCompiler ( PrologWAMCompiler <HtClause, HtPredicate, HtClause,
            HiTalkWAMCompiledPredicate, HiTalkWAMCompiledClause> compiler ) {
        this.wamCompiler = compiler;
        BaseApp.getAppContext().putIfAbsent(WAM_COMPILER, compiler);
//        setPreCompiler();
    }

    @Override
    public IProduct product () {
        return product;
    }

    @Override
    public Language language () {
        return PROLOG;
    }

    @Override
    public Kind tool () {
        return COMPILER;
    }

    protected FileObject createScratchDirectory () throws Exception {
        FileSystemOptions fileSystemOptions = new FileSystemOptions();
        final FileObject scratchFolder = VFS.getManager().resolveFile(String.valueOf(getScratchDirectory().toFile()));
//        fsManager = VFS.getManager();
        // Make sure the test folder is empty
        scratchFolder.delete(Selectors.EXCLUDE_SELF);
        scratchFolder.createFolder();

        return scratchFolder;
    }

    public Path getScratchDirectory () {
        System.getProperty("user.home");
        return Paths.get(DEFAULT_SCRATCH_DIRECTORY).toAbsolutePath();
    }

    @Override
    public void shutdown () {
        fsManager.close();
    }

    public LogicCompilerObserver <P, Q> getObserver () {
        return observer;
    }

    public void setObserver ( LogicCompilerObserver <P, Q> observer ) {
        this.observer = observer;
    }
}
