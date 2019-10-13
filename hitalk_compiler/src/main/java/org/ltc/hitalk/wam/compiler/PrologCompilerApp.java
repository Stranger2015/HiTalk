package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.ltc.hitalk.compiler.BaseCompiler;
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.compiler.bktables.TermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.CompilationContext;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.parser.HiTalkParser;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

import java.io.IOException;
import java.nio.file.Path;

import static java.lang.System.in;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource.getTokenSourceForInputStream;

/**
 *
 */
public class PrologCompilerApp<T extends HtClause, P, Q> extends BaseApplication <T, P, Q> {

    public static final String DEFAULT_SCRATCH_DIRECTORY = "scratch";
    private static final HtProperty[] DEFAULT_PROPS = new HtProperty[]{

    };

    protected DefaultFileSystemManager fsManager;
    protected BaseCompiler <P, Q> compiler;
    protected TermFactory tf;
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
    public BaseCompiler createCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, HtPrologParser parser ) {
        return new PrologWAMCompiler(symbolTable, interner, parser);
    }

    @Override
    public void init () throws LinkageException, IOException {

    }

    @Override
    public String namespace ( String varOrFunctor ) {
        return null;
    }

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
        setParser(new HiTalkParser(getTokenSourceForInputStream(in, "stdin"), interner));

        compiler = newWAMCompiler(getSymbolTable(), getInterner(), getParser());
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

    public BaseCompiler <P, Q> newWAMCompiler ( SymbolTable <Integer, String, Object> symbolTable, VariableAndFunctorInterner interner, HtPrologParser parser ) {
        return compiler;
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
        final FileObject scratchFolder = VFS.getManager().resolveFile(getScratchDirectory(), fileSystemOptions);
        fsManager = VFS.getManager();
        // Make sure the test folder is empty
        scratchFolder.delete(Selectors.EXCLUDE_SELF);
        scratchFolder.createFolder();

        return scratchFolder;
    }

    private Path getScratchDirectory () {
    }

}
