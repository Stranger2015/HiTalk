package org.ltc.hitalk.wam.compiler;


import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
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
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.compiler.bktables.PlOperatorTable;
import org.ltc.hitalk.compiler.bktables.TermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.CompilationContext;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public class PrologCompilerApp<T extends HtClause, P, Q> extends BaseApplication <T, P, Q> {

    public static final String DEFAULT_SCRATCH_DIRECTORY = "scratch";
    private static final HtProperty[] DEFAULT_PROPS = new HtProperty[]{

    };

    protected DefaultFileSystemManager fsManager;
    protected BaseCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler;
    protected ITermFactory tf;
    protected String scratchDirectory;
    protected CompilationContext compilationContext;
    protected LoadContext loadContext;
    protected ExecutionContext executionContext;

    /**
     *
     */
    public PrologCompilerApp () {
    }

    @Override
    public BaseCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> createWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, PlPrologParser parser ) {
        return new PrologWAMCompiler(symbolTable, interner, parser);
    }

    @Override
    public void init () throws LinkageException, IOException {

    }

    @Override
    public String namespace ( String varOrFunctor ) {
        return null;
    }//fixme

    @Override
    public void clear () throws LinkageException, IOException {

    }

    @Override
    public void reset () throws LinkageException, IOException {

    }

    /**
     *
     */
    @Override
    public void doInit () throws LinkageException, IOException {
        getLogger().info("Initializing ");

        setSymbolTable(new SymbolTableImpl <>());
        interner = new VariableAndFunctorInternerImpl(namespace("Variable"), namespace("Functor"));
        setParser(createParser(PlTokenSource.getTokenSourceForInputStream(null, "stdin"), interner));

        compiler = createWAMCompiler(getSymbolTable(), getInterner(), getParser());
//        bkt = new BookKeepingTables();
        setConfig(new CompilerConfig());
        scratchDirectory = "./" + DEFAULT_SCRATCH_DIRECTORY;

        tf = new TermFactory(interner);

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
        super.doInit();
    }

    public PlPrologParser createParser ( HiTalkStream stream,
                                         VariableAndFunctorInterner interner,
                                         ITermFactory factory,
                                         PlOperatorTable optable ) {
        return new PlPrologParser(stream, interner, factory, optable);
    }

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

    @Override
    public void start () throws Exception {

    }

    @Override
    public void setStarted ( boolean b ) throws Exception {

    }

    /**
     * @throws Exception
     */
    @Override
    public void doStart () throws Exception {
        getLogger().info("Starting ");
        setTarget(() -> {
            try {
                initialize();
                getCompiler().compile(fileName, loadContext.getFlags());
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(PERMISSION_ERROR, null);
            }

        });
        getLogger().info("Running target ");
        getTarget().run();
    }

    private BaseCompiler <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> getCompiler () {
        return compiler;
    }

    @Override
    public void run () {

    }

    @Override
    public IProduct product () {
        return null;
    }

    @Override
    public String language () {
        return null;
    }

    @Override
    public String tool () {
        return null;
    }

    @Override
    public void banner () {

    }

    public void initialize () throws Exception {

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

    private Path getScratchDirectory () {
        return Paths.get(DEFAULT_SCRATCH_DIRECTORY).toAbsolutePath();
    }

}
