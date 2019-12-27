package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.aima.logic.fol.LinkageException;
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
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.utils.HtSymbolTable;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.CompilationContext;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.interpreter.HtProduct;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.PlTokenSource;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.CompilerFactory;
import org.ltc.hitalk.wam.compiler.ICompilerFactory;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.LibParser;
import org.ltc.hitalk.wam.compiler.Tools.Kind;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.core.Components.INTERNER;
import static org.ltc.hitalk.core.Components.WAM_COMPILER;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;
import static org.ltc.hitalk.wam.compiler.Tools.Kind.COMPILER;

/**
 *
 */
public class PrologCompilerApp<T extends HtClause, P, Q, PC, QC> extends BaseApp <T, P, Q, PC, QC> {

    public static final String DEFAULT_SCRATCH_DIRECTORY = "scratch";
    public static final HtProperty[] DEFAULT_PROPS = new HtProperty[]{
    };

    protected DefaultFileSystemManager fsManager;
    protected PrologWAMCompiler <T, P, Q, PC, QC> wamCompiler;

    protected Path scratchDirectory = Paths.get(DEFAULT_SCRATCH_DIRECTORY).toAbsolutePath();
    protected CompilationContext compilationContext = new CompilationContext();
    protected LoadContext loadContext = new LoadContext(DEFAULT_PROPS);
    protected ExecutionContext executionContext = new ExecutionContext();
    protected IProduct product = new HtProduct("Copyright (c) Anton Danilov 2018-2019, All rights reserved.",
            language().getName() + " " + tool().getName(),
            new HtVersion(0, 1, 0, 119, "", false));
    protected ICompilerObserver <P, Q> observer;
    protected IVafInterner interner;
    protected IPreCompiler preCompiler;

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

    /**
     * @return
     */
    public Language getLanguage () {
        return language;
    }

    /**
     * @param varOrFunctor
     * @return
     */
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

//        final ListTerm lt = new ListTerm(new ITerm[1]);
//        final String s = lt.toString();

        parser.initializeBuiltIns();
    }
    @Override
    protected void initComponents () throws FileNotFoundException {
        final AppContext appCtx = getAppContext();
        final ICompilerFactory <T, P, Q, PC, QC> cf = new CompilerFactory <>();
        setSymbolTable(new HtSymbolTable <>());
        setInterner(new VafInterner(
                language().getName() + "_Variable_Namespace",
                language().getName() + "_Functor_Namespace"));
        appCtx.setInputStream(createInputHiTalkStream(fileName));
        appCtx.setTermFactory(appCtx.getInterner());
//        appCtx.setOptable()
        setParser(new PlPrologParser());
//        getParser().
        setWAMCompiler(cf.createWAMCompiler(language()));
    }

    /**
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    public HiTalkInputStream createInputHiTalkStream ( String fileName ) throws FileNotFoundException {
        return new HiTalkInputStream(new File(fileName));
    }

    /**
     *
     */
    @Override
    public void undoInit () {
        getLogger().info("cancelled!");
        initialized.set(false);
    }

    /**
     * @param inputStream
     * @param interner
     * @param factory
     * @param optable
     * @return
     */
    public IParser createParser ( HiTalkInputStream inputStream,
                                  IVafInterner interner,
                                  ITermFactory factory,
                                  IOperatorTable optable ) {
        return new PlPrologParser(inputStream, interner, factory, optable);
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
        Path scratchDirectory = loadWAMBuiltIns(Paths.get("c:\\Users\\Anthony_2\\IdeaProjects\\WAM\\hitalk_compiler\\src\\main\\resources\\wam_builtins.pl"));
        startRuntimeThreading();
//        Path path = Paths.get("c:\\Users\\Anthony_2\\IdeaProjects\\WAM\\hitalk_compiler\\src\\main\\resources\\empty.pl");
//        Object result = loadSettingsFile(path);
//        reportSettingsFile(result);
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

        return path;
    }

    private void startRuntimeThreading () {

    }

    /**
     * @param classPath wambuiltins.pl
     * @return
     * @throws Exception
     */
    private Path loadWAMBuiltIns ( Path classPath ) throws Exception {
//        final String classPath = Paths.get(System.getProperties().getProperty("CLASSPATH",);
        PlTokenSource tokenSource = PlTokenSource.getTokenSourceForIoFile(classPath.toFile());
        // Set up a parser on the token source.
        LibParser libParser = new LibParser();
        libParser.setTokenSource(tokenSource);
        logger.info("Parsing " + classPath);
        // Load the built-ins into the domain
        while (true) {
            final ITerm term = libParser.parse();
            if (term == PlPrologParser.BEGIN_OF_FILE_ATOM) {//ignore
                logger.info("begin_of_file");
                continue;
            }
            if (term == PlPrologParser.END_OF_FILE_ATOM) {//ignore
                logger.info("end_of_file");
                parser.popTokenSource();
                break;
            }
            HtClause clause = libParser.convert(term);
            wamCompiler.compile((T) clause);
        }
        wamCompiler.endScope();
//         There should not be any errors in the built in library, if there are then the prolog engine just
//         isn't going to work, so report this as a bug.
//        throw new IllegalStateException("Got an exception whilst loading the built-in library.", e);

        return classPath;
    }

    protected Object clone () throws CloneNotSupportedException {
        return super.clone();
    }

    void cacheCompilerFlags () {
    }

    protected void initDirectives () {
//        cond
//        encodunf
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

    /**
     * @param interner
     */
    public void setInterner ( IVafInterner interner ) {
        this.interner = interner;
        appContext.putIfAbsent(INTERNER, interner);
    }

    /**
     * @return
     */
    protected PrologWAMCompiler <T, P, Q, PC, QC> getWAMCompiler () {
        return wamCompiler;
    }

    /**
     * @param compiler
     */
    protected void setWAMCompiler ( PrologWAMCompiler <T, P, Q, PC, QC> compiler ) {
        this.wamCompiler = compiler;
        getAppContext().putIfAbsent(WAM_COMPILER, compiler);
    }

    /**
     * @return
     */
    @Override
    public IProduct product () {
        return product;
    }

    /**
     * @return
     */
    @Override
    public Language language () {
        return PROLOG;
    }

    /**
     * @return
     */
    @Override
    public Kind tool () {
        return COMPILER;
    }

    /**
     * @return
     * @throws Exception
     */
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

    public ICompilerObserver <P, Q> getObserver () {
        return observer;
    }

    public void setObserver ( ICompilerObserver <P, Q> observer ) {
        this.observer = observer;
    }
}
