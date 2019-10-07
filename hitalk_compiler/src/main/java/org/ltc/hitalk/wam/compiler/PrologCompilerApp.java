package org.ltc.hitalk.wam.compiler;

import com.sun.tools.javac.resources.compiler;
import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemOptions;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.ltc.hitalk.compiler.bktables.BookKeepingTables;
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.compiler.bktables.TermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.entities.context.CompilationContext;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.parser.HiTalkParser;
import org.ltc.hitalk.parser.HtClause;

import java.io.IOException;

import static java.lang.System.in;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.wam.compiler.HiTalkCompilerApp.DEFAULT_SCRATCH_DIRECTORY;

/**
 *
 */
public class PrologCompilerApp extends BaseApplication <HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {


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
        setParser(new HiTalkParser(HtTokenSource.getTokenSourceForInputStream(in, "stdin"), interner));

        compiler = newWAMCompiler(getSymbolTable(), getInterner(), getParser());
        bkt = new BookKeepingTables();
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

    public void initialize () {

    }

    protected FileObject createScratchDirectory () throws Exception {
        FileSystemOptions fileSystemOptions = new FileSystemOptions();
        final FileObject scratchFolder = VFS.getManager().resolveFile(getScratchDirectory(), fileSystemOptions);
        // Make sure the test folder is empty
        scratchFolder.delete(Selectors.EXCLUDE_SELF);
        scratchFolder.createFolder();

        return scratchFolder;
    }

}
