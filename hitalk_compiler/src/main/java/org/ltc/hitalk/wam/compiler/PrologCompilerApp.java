package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
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
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.core.HtVersion;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.CompilationContext;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.interpreter.HtProduct;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.term.io.Environment;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;
import static org.ltc.hitalk.wam.compiler.Tools.COMPILER;

/**
 *
 */
public class PrologCompilerApp<T extends HtClause, P, Q> extends BaseApplication <T, P, Q> {

    public static final String DEFAULT_SCRATCH_DIRECTORY = "scratch";
    public static final HtProperty[] DEFAULT_PROPS = new HtProperty[]{

    };

    protected DefaultFileSystemManager fsManager;
    protected BaseCompiler <T, P, Q> compiler;

    protected Path scratchDirectory = Paths.get(DEFAULT_SCRATCH_DIRECTORY).toAbsolutePath();
    protected CompilationContext compilationContext = new CompilationContext();
    protected LoadContext loadContext = new LoadContext(DEFAULT_PROPS);
    protected ExecutionContext executionContext = new ExecutionContext();
    protected IProduct product = new HtProduct("Copyright (c) Anton Danilov 2018-2019, All rights reserved",
            language().getName() + " " + tool().getName(),
            new HtVersion(0, 1, 0, 81, " ", true));
    protected LogicCompilerObserver <P, Q> observer;

    /**
     *
     */
    public PrologCompilerApp () {
    }

    public PrologCompilerApp ( String fn ) {
        fileName = fn;
    }

    //    @Override
    public BaseCompiler <T, P, Q> createWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                                      VariableAndFunctorInterner interner,
                                                      PlPrologParser parser,
                                                      LogicCompilerObserver <P, Q> observer ) {
        return new PrologWAMCompiler <>(symbolTable, interner, parser, observer);
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
     *
     */
    @Override
    public void doInit () throws LinkageException, IOException {
        super.doInit();
        setSymbolTable(new SymbolTableImpl <>());

//        setParser(createParser(getParser()));
//                PlTokenSource.getTokenSourceForInputStream(null, "stdin"), interner));

        compiler = createWAMCompiler(getSymbolTable(), getInterner(), getParser(), getObserver());
//        bkt = new BookKeepingTables();
        setConfig(new CompilerConfig());
        //scratchDirectory = "./" + DEFAULT_SCRATCH_DIRECTORY;

//            termFactory = new TermFactory(interner);

//        DEFAULT_FLAGS = new HtProperty[]{
//                tf.createFlag("access", "read_write"),//read_only
//                tf.createFlag("keep", "false"),
//                //
//                tf.createFlag("type", "false"),
//
//                };

        compilationContext = new CompilationContext();
        loadContext = new LoadContext(DEFAULT_PROPS);
        executionContext = new ExecutionContext();

        initVfs();
//=============================
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
     * @param symbolTable
     * @param interner
     * @param observer
     * @param parser
     * @return
     */
    public BaseCompiler <T, P, Q> createWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable,
                                                      VariableAndFunctorInterner interner,
                                                      LogicCompilerObserver <P, Q> observer,
                                                      PlPrologParser parser ) {
        return new PrologWAMCompiler <>(symbolTable, interner, parser, observer);
    }

    /**
     * @param stream
     * @param interner
     * @param factory
     * @param optable
     * @return
     */
    public IParser createParser ( HiTalkStream stream,
                                  VariableAndFunctorInterner interner,
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
//        initBookKeepingTables();
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
        PlTokenSource tokenSource = PlTokenSource.getTokenSourceForIoFile(path.toFile());
        // Set up a parser on the token source.
        LibParser libParser = new LibParser();//TermIO.instance().getParser();
        libParser.setTokenSource(tokenSource);

        // Load the built-ins into the domainwhile (true) {
        while (true) {
            Sentence <Term> sentence = libParser.parse();
            if (sentence == null) {
                break;
            }
//            compiler.compile(sentence);
            HtClause clause = libParser.convert(sentence.getT());
            compiler.compile((T) clause);
        }
        compiler.endScope();
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
                getCompiler().compile(fileName, loadContext.getFlags());
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(PERMISSION_ERROR, null);
            }
        });
        getLogger().info("Running target... ");
        getTarget().run();
    }

    //    @Override
    public void setInterner ( VariableAndFunctorInterner interner ) {
        Environment.instance().setInterner(interner);
    }

    private BaseCompiler <T, P, Q> getCompiler () {
        return compiler;
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
    public Tools tool () {
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
