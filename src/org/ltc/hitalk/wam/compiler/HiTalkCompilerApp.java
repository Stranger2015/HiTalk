package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.*;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.CompilationContext;
import org.ltc.hitalk.entities.context.Context;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.interpreter.DcgRule;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.interpreter.ICompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.machine.HiTalkWAMEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.ltc.hitalk.compiler.bktables.BkTableKind.LOADED_ENTITIES;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 * Reloading files, active code and threads
 * <p>
 * Traditionally, Prolog environments allow for reloading files holding currently active code.
 * In particular, the following sequence is a valid use of the development environment:
 * <p>
 * Trace a goal
 * Find unexpected behaviour of a predicate
 * Enter a break using the b command
 * Fix the sources and reload them using make/0
 * Exit the break, retry executing the now fixed predicate using the r command
 * <p>
 * Reloading a previously loaded file is safe, both in the debug scenario above and when the code is being executed
 * by another thread. Executing threads switch atomically to the new definition of modified predicates,
 * while clauses that belong to the old definition are (eventually) reclaimed by garbageCollectClauses/0
 * Below we describe the steps taken for reloading a file to help understanding the limitations of the process.
 *
 * <p>
 * If a file is being reloaded, a reload context is associated to the file administration.
 * This context includes a table keeping track of predicates and a table keeping track of the module(s)
 * associated with this source.
 * <p>
 * If a new predicate is found, an entry is added to the context predicate table. Three options are considered:
 * The predicate is new. It is handled the same as if the file was loaded for the first time.
 * The predicate is foreign or thread local. These too are treated as if the file was loaded for the first time.
 * Normal predicates. Here we initialise a pointer to the current clause.
 * <p>
 * New clauses for `normal predicates' are considered as follows:
 * If the clause's byte-code is the same as the predicates current clause, discard the clause and advance
 * the current clause pointer.
 * If the clause's byte-code is the same as some clause further into the clause list of tce he predicate,
 * discard the new clause, mark all intermediate clauses for future deletion, and advance
 * the current clause pointer to the first clause after the matched one.
 * If the clause's byte-code matches no clause, insert it for future activation before the current clause
 * and keep the current clause.
 * Properties such as dynamic or meta_predicate are in part applied immediately and in part during
 * the fixup process after the file completes loading. Currently, dynamic and threadLocal are applied immediately.
 * New modules are recorded in the reload context. Export declarations (the module's public list and export/1 calls)
 * are both applied and recorded.
 * When the end-of-file is reached, the following fixup steps are taken:
 * For each predicate
 * The current clause and subsequent clauses are marked for future deletion.
 * All clauses marked for future deletion or creation are (in)activated by changing
 * their `erased' or `created' generation. Erased clauses are (eventually) reclaimed by the clause
 * garbage collector, see garbageCollectClauses/0.
 * Pending predicate property changes are applied.
 * For each module
 * Exported predicates that are not encountered in the reload context are removed from the export list.
 * <p>
 * The above generally ensures that changes to the content of source files can typically be activated safely using
 * make/0.
 * Global changes such as operator changes, changes of module names, changes to multi-file predicates, etc.
 * sometimes require a restart. In almost all cases, the need for restart is indicated by permission or syntax errors
 * during the reload or existence errors while running the program.
 * <p>
 * In some cases the content of a source file refers `to itself'. This is notably the case if local rules for
 * goal_expansion/2 or term_expansion/2 are defined or goals are executed using directives.
 * <p>
 * Up to version 7.5.12 it was typically needed to reload the file twice, once for updating the code that was used for
 * compiling the remainder of the file and once to effectuate this. As of version 7.5.13, conventional transaction
 * semantics apply. This implies that for the thread performing the reload the file's content is first wiped and
 * gradually rebuilt, while other threads see an atomic update from the old file content to the new one.
 * <p>
 * Compilation of mutually dependent code
 * <p>
 * Large programs are generally split into multiple files. If file A accesses predicates from file B which accesses
 * predicates from file A, we consider this a mutual or circular dependency. If traditional load predicates
 * (e.g., consult/1) are used to include file B from A and A from B, loading either file results in a loop.
 * This is because consult/1 is mapped to loadFiles/2 using the option if(true)(if(true)).
 * Such programs are typically loaded using a load file that consults all required (non-module) files.
 * If modules are used, the dependencies are made explicit using use_module/1 statements. The use_module/1 predicate,
 * however, maps to loadFiles/2 with the option if(notLoaded)(if(notLoaded)) A use_module/1 on an already loaded file
 * merely makes the public predicates of the used module available.
 * <p>
 * Summarizing, mutual dependency of source files is fully supported with no precautions when using modules.
 * Modules can use each other in an arbitrary dependency graph. When using consult/1, predicate dependencies between
 * loaded files can still be arbitrary, but the consult relations between files must be a proper tree.
 * <p>
 * Compilation with multiple threads
 * <p>
 * This section discusses compiling files for the first time. For reloading, see section 4.3.2.
 * In older versions, compilation was thread-safe due to a global lock in loadFiles/2 and the code dealing with
 * auto-loading (see section 2.13).
 * Besides unnecessary stalling when multiple threads trap unrelated undefined predicates, this easily leads to deadlocks,
 * notably if threads are started from an initialization/1 directive.
 * <p>
 * Starting with version 5.11.27, the autoloader is no longer locked and multiple threads can compile files concurrently.
 * This requires special precautions only if multiple threads wish to load the same file at the same time.
 * Therefore, loadFiles/2 checks automatically whether some other thread is already loading the file. If not,
 * it starts loading the file. If another thread is already loading the file, the thread blocks until the other thread
 * finishes loading the file. After waiting, and if the file is a module file, it will make the public predicates
 * available.
 * <p>
 * Note that this schema does not prevent deadlocks under all situations. Consider two mutually dependent (see section
 * 4.3.2.1) module files A and B, where thread 1 starts loading A and thread 2 starts loading B at the same time.
 * Both threads will deadlock when trying to load the used module.
 * <p>
 * The current implementation does not detect such cases and the involved threads will freeze.
 * This problem can be avoided if a mutually dependent collection of files is always loaded from the same start file.
 */
public
class HiTalkCompilerApp<T extends HtClause, P, Q> extends HiTalkWAMEngine <T, P, Q> implements IApplication {

    public static final String DEFAULT_SCRATCH_DIRECTORY = "scratch";

    /**
     * <code>access(Access)</code>,  where <code>Access</code> can be either <code>read_write (the default) or
     * <code>read_only;
     * keep(Keep),      where <code>Keep can be either <code>false (the default) or <code>true, for deciding
     * if an existing definition of the flag should be kept or replaced by the new one;
     * type(Type)       for specifying the type of the flag, which can be
     * <code>boolean, <code>atom, <code>integer, <code>float, <code>term.
     * (which only restricts the flag value to ground terms).
     * When the <code>type/1 option is not specified, the type of the flag is inferred
     * from its initial value.
     */
//    public final Flag[] DEFAULT_FLAGS;
    public final HtProperty[] DEFAULT_PROPS = new HtProperty[0];
    //library entity names
    public final HtEntityIdentifier EXPANDING;
    public final HtEntityIdentifier MONITORING;
    public final HtEntityIdentifier FORWARDING;
    public final HtEntityIdentifier USER;
    public final HtEntityIdentifier LOGTALK;
    public final HtEntityIdentifier CORE_MESSAGES;
    //
    public final Functor OBJECT;
    public final Functor PROTOCOL;
    public final Functor CATEGORY;

    public final Functor END_OBJECT;
    public final Functor END_CATEGORY;
    public final Functor END_PROTOCOL;
// public final Functor END_ENUM;


    /**
     * Used for logging to the console.
     */
    private static final Logger console = LoggerFactory.getLogger(/* "CONSOLE." + */ HiTalkCompilerApp.class);

    protected boolean compiling;

    protected boolean compilingEntity;
    /**
     * True if the system is compiling source files with the -c option or qcompile/1
     * into an intermediate code file.
     * Can be used to perform conditional code optimizations in term_expansion/2 (see also the -O option)
     * or to omit execution of directives during compilation.
     */
    protected ITermFactory tf;
    /**
     * Holds the instruction generating compiler.
     */
    protected ICompiler <T, P, Q> instructionCompiler;
    protected String scratchDirectory;
    //
//    private final BookKeepingTables bkt = new BookKeepingTables();
    protected final IRegistry <BkLoadedEntities> registry = new BookKeepingTables <>();
    protected final CompilationContext compilationContext;
    protected final LoadContext loadContext;
    protected final ExecutionContext executionContext;
    protected LogicCompilerObserver <P, Q> observer;
    protected LogicCompilerObserver <P, Q> observer2;

    /**
     * Holds the pre-compiler, for analyzing and transforming terms prior to compilation proper.
     */
    protected ICompiler <T, P, Q> preCompiler;
    protected String fileName;
    protected IConfig config;
    //    protected HiTalkCompilerApp app;
    protected boolean started;
    protected HtPrologParser parser;
    protected Resolver <P, Q> resolver;

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     *
     * @param parser   The parser.
     * @param interner The interner.
     */
    public
    HiTalkCompilerApp ( SymbolTable <Integer, String, Object> symbolTable,
                        VariableAndFunctorInterner interner,
                        HtPrologParser parser,
                        ICompiler <T, P, Q> compiler,
                        HiTalkDefaultBuiltIn defaultBuiltIn ) throws LinkageException {
        super(parser, interner, compiler);

        Resolver <P, Q> resolver =
                new HtResolutionEngine <>(parser, interner, compiler);

        preCompiler = new HiTalkPreprocessor(symbolTable, interner, defaultBuiltIn, resolver, this);
        instructionCompiler = compiler;
        observer2 = new ClauseChainObserver(instructionCompiler);
        preCompiler.setCompilerObserver(observer2);
        observer = new ChainedCompilerObserver();
        instructionCompiler.setCompilerObserver(observer);
        scratchDirectory = ".\\" + DEFAULT_SCRATCH_DIRECTORY;

        tf = new TermFactory(interner);

        EXPANDING = tf.createIdentifier(HtEntityKind.PROTOCOL, "expanding");
        MONITORING = tf.createIdentifier(HtEntityKind.PROTOCOL, "monitoring");
        FORWARDING = tf.createIdentifier(HtEntityKind.PROTOCOL, "forwarding");
        USER = tf.createIdentifier(HtEntityKind.OBJECT, "user");
        LOGTALK = tf.createIdentifier(HtEntityKind.OBJECT, "logtalk");
        CORE_MESSAGES = tf.createIdentifier(HtEntityKind.CATEGORY, "core_messages");
        OBJECT = tf.createAtom("object");
        PROTOCOL = tf.createAtom("protocol");
        CATEGORY = tf.createAtom("category");
        END_OBJECT = tf.createAtom("end_object");
//        END_ENUM = tf.createAtom("end_enum");
        END_PROTOCOL = tf.createAtom("end_protocol");
        END_CATEGORY = tf.createAtom("end_category");

//        ENUM = tf.createIdentifier(HtEntityKind.OBJECT, "enum");
//
//        DEFAULT_FLAGS = new Flag[]{
//                tf.createFlag("access", "read_write"),//read_only
//                tf.createFlag("keep", "false"),
//                //
//                tf.createFlag("type", "false"),
//
//                };

        compilationContext = new CompilationContext();
        loadContext = new LoadContext(DEFAULT_PROPS);
        executionContext = new ExecutionContext();
    }

    public
    HiTalkCompilerApp () {
        super();

        tf = new TermFactory(interner);

        EXPANDING = tf.createIdentifier(HtEntityKind.PROTOCOL, "expanding");
        MONITORING = tf.createIdentifier(HtEntityKind.PROTOCOL, "monitoring");
        FORWARDING = tf.createIdentifier(HtEntityKind.PROTOCOL, "forwarding");
        USER = tf.createIdentifier(HtEntityKind.OBJECT, "user");
        LOGTALK = tf.createIdentifier(HtEntityKind.OBJECT, "logtalk");
        CORE_MESSAGES = tf.createIdentifier(HtEntityKind.CATEGORY, "core_messages");
        OBJECT = tf.createAtom("object");
        PROTOCOL = tf.createAtom("protocol");
        CATEGORY = tf.createAtom("category");
        END_OBJECT = tf.createAtom("end_object");
//        END_ENUM = tf.createAtom("end_enum");
        END_PROTOCOL = tf.createAtom("end_protocol");
        END_CATEGORY = tf.createAtom("end_category");

//        ENUM = tf.createIdentifier(HtEntityKind.OBJECT, "enum");
//
//        DEFAULT_FLAGS = new Flag[]{
//                tf.createFlag("access", "read_write"),//read_only
//                tf.createFlag("keep", "false"),
//                //
//                tf.createFlag("type", "false"),
//
//                };

        compilationContext = new CompilationContext();
        loadContext = new LoadContext(DEFAULT_PROPS);
        executionContext = new ExecutionContext();
    }

    public static
    void main ( String[] args ) {
        IApplication application = new HiTalkCompilerApp <>();
        application.setFileName(args[0]);
        try {
            application.start();
        } catch (Exception e) {
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }

    /**
     * String basename;
     * Path directory;
     * HtEntityIdentifier entityIdentifier;
     * //    entity_prefix,
     * //    entity_type,IN eI
     * String file;
     * // Flag[] flags;
     * Path source;
     * InputStream stream;
     * Path target;
     * Term term;
     * int[] termPosition = new int[]{0, 0};
     * Map <String, Variable> variableNames = new HashMap <>();
     *
     * @param loadContext
     * @param scratchDirectory
     * @return
     */
    protected
    Flag[] createFlags ( LoadContext loadContext, String scratchDirectory ) {
        Flag[] flags = new Flag[]{///todo flags
                                  tf.createFlag("basename", fileName),//FIXME PARSE
                                  tf.createFlag("directory", fileName),//FIXME PARSE
                                  tf.createFlag("entity_identifier", new Functor(-1, null)),//FIXME PARSE
                                  tf.createFlag("file", fileName),//FIXME PARSE
                                  tf.createFlag("basename", fileName),//FIXME PARSE
        };

        return flags;
    }

    /**
     *
     */
    void initialize () throws Exception {
        initBookKeepingTables();
        initDirectives();
        cacheCompilerFlags();
        String scratchDirectory = loadBuiltInEntities();
        startRuntimeThreading();
        Object result = loadSettingsFile(scratchDirectory);
//        printMessage(BANNER, CORE, BANNER);
//        printMessage(comment(settings), CORE, DEFAULT_FLAGS);
//        expandGoals()
//        ;
//        compileHooks();        reportSettingsFile(result);
    }

    /**
     * @param result
     */
    protected
    void reportSettingsFile ( Object result ) {

    }

    protected
    Object loadSettingsFile ( String scratchDir ) {
        logtalkCompile("startup.pl");
        return null;
    }

    /**
     * @param identifier
     * @param fileName
     * @param scratchDir
     * @throws IOException
     * @throws SourceCodeException
     */
    protected
    void loadBuiltInEntity ( HtEntityIdentifier identifier, String fileName, String scratchDir )
            throws Exception {
        List <BkLoadedEntities> rs = registry.select(LOADED_ENTITIES, new BkLoadedEntities(identifier));
        if (rs.isEmpty()) {
//            loadContext.setFlags(createFlags(scratchDir));
            loadContext.setProps(createProps(scratchDir));
            compileLoad(fileName, loadContext);
            registry.add(new BkLoadedEntities(identifier));
        }
    }

    private
    HtProperty[] createProps ( String scratchDir ) {
        return new HtProperty[0];
    }

    protected
    Flag[] createFlags ( String scratchDir ) {
        return new Flag[]{
                //we need a fixed code prefix as some of the entity predicates may need
//                    to be called directly by the compiler/runtime
                tf.createFlag("code_prefix", "$"),
                //delete the generated intermediate files as they may be non-portable
                //between backend Prolog compilers
                tf.createFlag("clean", "on"),
                //use a scratch directory where we expect to have writing permission
                tf.createFlag("scratch_directory", scratchDir),
                //optimize entity code, allowing static binding to this entity resource
                tf.createFlag("optimize", "on"),
                //don't print any messages on the compilation and loading of these entities
                tf.createFlag("report", "off"),
                //prevent any attempts of logtalk_make(all) to reload this file
                tf.createFlag("reload", "skip")
        };
    }

    /**
     * @param fileNames
     * @param context
     * @throws IOException
     * @throws SourceCodeException
     */
    public
    void compileLoad ( List <String> fileNames, LoadContext context ) throws Exception {
        for (String fileName : fileNames) {
            context.reset();
            compileLoad(fileName, context);
        }
    }

    /**
     * @param fileName
     * @param context
     * @throws IOException
     * @throws SourceCodeException
     */
    public
    void compileLoad ( String fileName, LoadContext context ) throws Exception {
        compileFile(fileName, context);
        loadFile(fileName, context);//bytecode
    }

    public //todo read_files
    void loadFiles ( List <String> fileNames, LoadContext context ) throws Exception {
        for (String s : fileNames) {
            context.reset();
            loadFile(s, context);
        }
    }

    /**
     * load_files(:Files, +Options)
     * The predicate load_files/2 is the parent of all the other loading predicates except for include/1. It currently supports a subset of the options of Quintus load_files/2. Files is either a single source file or a list of source files. The specification for a source file is handed to absolute_file_name/2. See this predicate for the supported expansions. Options is a list of options using the format OptionName(OptionValue).
     * <p>
     * The following options are currently supported:
     * <p>
     * <p>
     * autoload(Bool)If true (default false), indicate that this load is a demand load. This implies that, depending on the setting of the Prolog flag verbose_autoload, the load action is printed at level informational or silent. See also print_message/2 and current_prolog_flag/2.
     * check_script(Bool)
     * If false (default true), do not check the first character to be # and skip the first line when found.
     * derived_from(File)
     * Indicate that the loaded file is derived from File. Used by make/0 to time-check and load the original file rather than the derived file.
     * dialect(+Dialect)
     * Load Files with enhanced compatibility with the target Prolog system identified by Dialect. See expects_dialect/1 and section C for details.
     * encoding(Encoding)
     * Specify the way characters are encoded in the file. Default is taken from the Prolog flag encoding. See section 2.19.1 for details.
     * expand(Bool)
     * If true, run the filenames through expand_file_name/2 and load the returned files. Default is false, except for consult/1 which is intended for interactive use. Flexible location of files is defined by file_search_path/2.
     * format(+Format)
     * Used to specify the file format if data is loaded from a stream using the stream(Stream) option. Default is source, loading Prolog source text. If qlf, load QLF data (see qcompile/1).
     * if(Condition)
     * Load the file only if the specified condition is satisfied. The value true loads the file unconditionally, changed loads the file if it was not loaded before or has been modified since it was loaded the last time, and not_loaded loads the file if it was not loaded before.
     * imports(Import)
     * Specify what to import from the loaded module. The default for use_module/1 is all. Import is passed from the second argument of use_module/2. Traditionally it is a list of predicate indicators to import. As part of the SWI-Prolog/YAP integration, we also support Pred as Name to import a predicate under another name. Finally, Import can be the term except(Exceptions), where Exceptions is a list of predicate indicators that specify predicates that are not imported or Pred as Name terms to denote renamed predicates. See also reexport/2 and use_module/2.bug
     * <p>
     * If Import equals all, all operators are imported as well. Otherwise, operators are not imported. Operators can be imported selectively by adding terms op(Pri,Assoc,Name) to the Import list. If such a term is encountered, all exported operators that unify with this term are imported. Typically, this construct will be used with all arguments unbound to import all operators or with only Name bound to import a particular operator.
     * modified(TimeStamp)
     * Claim that the source was loaded at TimeStamp without checking the source. This option is intended to be used together with the stream(Input) option, for example after extracting the time from an HTTP server or database.
     * module(+Module)
     * Load the indicated file into the given module, overruling the module name specified in the :- module(Name, ...) directive. This currently serves two purposes: (1) allow loading two module files that specify the same module into the same process and force and (2): force loading source code in a specific module, even if the code provides its own module name. Experimental.
     * must_be_module(Bool)
     * If true, raise an error if the file is not a module file. Used by use_module/[1,2].
     * qcompile(Atom)
     * How to deal with quick-load-file compilation by qcompile/1. Values are:
     * <p>
     * never
     * Default. Do not use qcompile unless called explicitly.
     * auto
     * Use qcompile for all writeable files. See comment below.
     * large
     * Use qcompile if the file is `large'. Currently, files larger than 100 Kbytes are considered large.
     * part
     * If load_files/2 appears in a directive of a file that is compiled into Quick Load Format using qcompile/1, the contents of the argument files are included in the .qlf file instead of the loading directive.
     * <p>
     * If this option is not present, it uses the value of the Prolog flag qcompile as default.
     * optimise(+Boolean)
     * Explicitly set the optimization for compiling this module. See optimise.
     * redefine_module(+Action)
     * Defines what to do if a file is loaded that provides a module that is already loaded from another file. Action is one of false (default), which prints an error and refuses to load the file, or true, which uses unload_file/1 on the old file and then proceeds loading the new file. Finally, there is ask, which starts interaction with the user. ask is only provided if the stream user_input is associated with a terminal.
     * reexport(Bool)
     * If true re-export the imported predicate. Used by reexport/1 and reexport/2.
     * register(Bool)
     * If false, do not register the load location and options. This option is used by make/0 and load_hotfixes/1 to avoid polluting the load-context database. See source_file_property/2.
     * sandboxed(Bool)
     * Load the file in sandboxed mode. This option controls the flag sandboxed_load. The only meaningful value for Bool is true. Using false while the Prolog flag is set to true raises a permission error.
     * scope_settings(Bool)
     * Scope style_check/1 and expects_dialect/1 to the file and files loaded from the file after the directive. Default is true. The system and user initialization files (see -f and -F) are loading with scope_settings(false).
     * silent(Bool)
     * If true, load the file without printing a message. The specified value is the default for all files loaded as a result of loading the specified files. This option writes the Prolog flag verbose_load with the negation of Bool.
     * stream(Input)
     * This SWI-Prolog extension compiles the data from the stream Input. If this option is used, Files must be a single atom which is used to identify the source location of the loaded clauses as well as to remove all clauses if the data is reconsulted.
     * e
     * This option is added to allow compiling from non-file locations such as databases, the web, the user (see consult/1) or other servers. It can be combined with format(qlf) to load QLF data from a stream.
     * <p>
     * The load_files/2 predicate can be hooked to load other data or data from objects other than files. See prolog_load_file/2 for a description and library(htt
     *
     * @param fileName
     * @param context
     */
    public//todo readFIles??
    void loadFile ( String fileName, LoadContext context ) throws Exception {
        compileFile(fileName, context);
//        String baseName = context.get(LoadContext.Kind.Loading.BASENAME);
        loadTargetFile(fileName, context);
    }

    protected
    void loadTargetFile ( String targetFile, LoadContext context ) {

    }

    protected
    void compileFile ( String fileName, LoadContext context ) throws Exception {
//        // Set up a parser on the token source.
//        parser = new HiTalkParser(tokenSource, interner);
//
//        compiler.endScope();
    }


    /**
     * //Compile_hooks(+callable)
     * %
     * //compiles the user-defined default compiler hooks
     * //(replacing any existing defined hooks)
     * <p>
     * Compile_hooks(HookEntity) :-
     * CompCtx(Ctx, _, _, user, user, user, HookEntity, _, [], [], ExCtx, runtime, [], _),
     * executionContext(ExCtx, user, user, user, HookEntity, [], []),
     * CurrentFlag_(events, Events),
     * Compile_message_to_object(term_expansion(Term, ExpandedTerm), HookEntity, TermExpansionGoal, Events, Ctx),
     * Compile_message_to_object(goal_expansion(Term, ExpandedTerm), HookEntity, GoalExpansionGoal, Events, Ctx),
     * retractall(hook_term_expansion_(_, _)),
     * assertz((
     * hook_term_expansion_(Term, ExpandedTerm) :-
     * catch(TermExpansionGoal, Error, term_expansion_error(HookEntity, Term, Error))
     * ),
     * retractall(hook_goal_expansion_(_, _)),
     * assertz((
     * hook_goal_expansion_(Term, ExpandedTerm) :-
     * catch(GoalExpansionGoal, Error, goal_expansion_error(HookEntity, Term, Error))
     * )).
     */
    protected
    void initBookKeepingTables () {

    }

    /**
     * compiles the user-defined default compiler hooks
     * (replacing any existing defined hooks)
     */
    protected
    void compileHooks ( HtEntityIdentifier hookEntity ) {
//= bkt.getTable(TERM_EXPANSION_DEFAULT_HOOKS);

//        Compile_hooks(HookEntity) :-
//                CompCtx(Ctx, _, _, user, user, user, HookEntity, _, [], [], ExCtx, runtime, [], _),
//        executionContext(ExCtx, user, user, user, HookEntity, [], []),
//        CurrentFlag_(events, Events),
//                Compile_message_to_object(term_expansion(Term, ExpandedTerm), HookEntity, TermExpansionGoal, Events, Ctx),
//                Compile_message_to_object(goal_expansion(Term, ExpandedTerm), HookEntity, GoalExpansionGoal, Events, Ctx),
//                retractall(hook_term_expansion_(_, _)),
//                assertz((
//                        hook_term_expansion_(Term, ExpandedTerm) :-
//        catch(TermExpansionGoal, Error, term_expansion_error(HookEntity, Term, Error))
//        )),
//        retractall(hook_goal_expansion_(_, _)),
//                assertz((
//                        hook_goal_expansion_(Term, ExpandedTerm) :-
//        catch(GoalExpansionGoal, Error, goal_expansion_error(HookEntity, Term, Error))
//        )).

    }


    //        LogtalkFlag.Hook
//        table=bkt.getTable(RUNTIME_FLAGS);
//        table.forEach(new BiConsumer<String, HtEntity>(){
//
//            /**
//             * Performs this operation on the given arguments.
//             *
//             * @param s        the first input argument
//             * @param htEntity the second input argument
//             */
//            @Override
//            public
//            void accept ( String s, HtEntity htEntity ) {
//
//            }
//        });
//        CurrentFlag_(events, Events),
//                Compile_message_to_object(term_expansion(Term, Terms), HookEntity, TermExpansionGoal, Events, Ctx),
//                Compile_message_to_object(goal_expansion(Goal, ExpandedGoal), HookEntity, GoalExpansionGoal, Events, Ctx)


    protected
    void initDirectives () {
        initRuntimeDirectives();
        initCompilerDirectives();
    }

    protected
    void initCompilerDirectives () {

    }

    protected
    void initRuntimeDirectives () {

    }

    //Compile_message_to_object(@term, @objectIdentifier, -callable, +atom, +compilationContext)

    //compiles a message sending call

    //messages to the pseudo-object "user"

    protected
    void compileMessageToObject ( Term pred, HtEntityIdentifier obj, ICallable call, Functor atom, Context ctx ) {
        if (obj.equals(USER) && pred.isVar() || pred.isFunctor()) {

        }
    }

//        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//        %
//        % runtime directives (bookkeeping tables)
//        %
//        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//       //tables of defined events and monitors
//
//        //before_event_(Obj, Msg, Sender, Monitor, Call)
//        :- dynamic(before_event_'/5).
//        //After_event_(Obj, Msg, Sender, Monitor, Call)
//        :- dynamic(After_event_'/5).
//
//
//        //tables of loaded entities, entity and predicate properties, plus entity relations
//
//        //Current_protocol_(Ptc, Prefix, Dcl, Rnm, Flags)
//        :- multifile(Current_protocol_'/5).
//        :- dynamic(Current_protocol_'/5).
//        //CurrentCategory_(Ctg, Prefix, Dcl, Def, Rnm, Flags)
//        :- multifile(CurrentCategory_'/6).
//        :- dynamic(CurrentCategory_'/6).
//        //Current_object_(Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, Flags)
//        :- multifile(Current_object_'/11).
//        :- dynamic(Current_object_'/11).
//
//        //entity_property_(Entity, Property)
//        :- multifile(entity_property_'/2).
//        :- dynamic(entity_property_'/2).
//        //predicate_property_(Entity, Functor/Arity, Property)
//        :- multifile(predicate_property_'/3).
//        :- dynamic(predicate_property_'/3).
//
//        //Implements_protocol_(ObjOrCtg, Ptc, Scope)
//        :- multifile(Implements_protocol_'/3).
//        :- dynamic(Implements_protocol_'/3).
//        //ImportsCategory_(Obj, Ctg, Scope)
//        :- multifile(ImportsCategory_'/3).
//        :- dynamic(ImportsCategory_'/3).
//        //InstantiatesClass_(Instance, Class, Scope)
//        :- multifile(InstantiatesClass_'/3).
//        :- dynamic(InstantiatesClass_'/3).
//        //SpecializesClass_(Class, Superclass, Scope)
//        :- multifile(SpecializesClass_'/3).
//        :- dynamic(SpecializesClass_'/3).
//        //extendsCategory_(Ctg, ExtCtg, Scope)
//        :- multifile(extendsCategory_'/3).
//        :- dynamic(extendsCategory_'/3).
//        //extends_object_(Prototype, Parent, Scope)
//        :- multifile(extends_object_'/3).
//        :- dynamic(extends_object_'/3).
//        //extends_protocol_(Ptc, ExtPtc, Scope)
//        :- multifile(extends_protocol_'/3).
//        :- dynamic(extends_protocol_'/3).
//        //Complemented_object_(Obj, Ctg, Dcl, Def, Rnm)
//        :- dynamic(Complemented_object_'/5).
//
//        //table of loaded files
//
//        //runtime flag values
//
//        //CurrentFlag_(Name, Value)
//        :- dynamic(CurrentFlag_'/2).
//
//
//        //static binding caches
//
//        //Send_to_objStatic_binding_(Obj, Pred, ExCtx, Call)
//        :- dynamic(Send_to_objStatic_binding_'/4).
//
//
//
//
//        //dynamic binding lookup cache for asserting and retracting dynamic facts
//
//        //DbLookupCache_(Obj, Fact, Sender, TFact, UClause)
//        :- dynamic(DbLookupCache_'/5).
//
//
//        //table of library paths
//
//        //logtalkLibrary_path(Library, Path)
//        :- multifile(logtalkLibrary_path/2).
//        :- dynamic(logtalkLibrary_path/2).
//
//
//        //extension points for logtalk_make/1
//
//        //logtalk_make_targetAction(Target)
//        :- multifile(logtalk_make_targetAction/1).
//        :- dynamic(logtalk_make_targetAction/1).
//
//
//        //term- and goal-expansion default compiler hooks
//
//        //hook_term_expansion_(Term, ExpandedTerms)
//        :- dynamic(hook_term_expansion_'/2).
//        //hook_goal_expansion_(Goal, ExpandedGoal)
//        :- dynamic(hook_goal_expansion_'/2).
//
//
//        //engines
//
//        //Current_engine_(Object, Engine, TermQueue, Id)
//        :- dynamic(Current_engine_'/4).
//
//
//        //counters
//
//        //Dynamic_entityCounter_(Kind, Base, Count)
//        :- dynamic(Dynamic_entityCounter_'/3).
//        //threaded_tagCounter_(Tag)
//        :- dynamic(threaded_tagCounter_'/1).
//
//
//        //debugging hook predicates
//
//        :- multifile('$logtalk#0.trace_event#2'/3).
//        :- dynamic('$logtalk#0.trace_event#2'/3).
//
//        :- multifile('$logtalk#0.debug_handler_provider#1'/2).
//
//        :- multifile('$logtalk#0.debug_handler#2'/3).
//
//
//        //internal initialization flags
//
//        :- dynamic(builtIn_entitiesLoaded_'/0).
//        :- dynamic(RuntimeInitializationCompleted_'/0).
//
//
//        //user-defined flags
//
//        //userDefinedFlag_(Flag, Access, Type)
//        :- dynamic(userDefinedFlag_'/3).
//
// /        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//        %
//        // compiler directives
//        %
//        // (used for source file compilation and runtime creation of new entities)
//        %
//        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
////
//        //ppFileCompilerFlag_(Name, Value)
//        :- dynamic(ppFileCompilerFlag_'/2).
//        //pp_entityCompilerFlag_(Name, Value)
//        :- dynamic(pp_entityCompilerFlag_'/2).
//
//        //ppDcl_(T)
//        :- dynamic(ppDcl_'/1).
//        //ppDef_(T)
//        :- dynamic(ppDef_'/1).
//        //ppDdef_(T)
//        :- dynamic(ppDdef_'/1).
//        //ppSuper_(T)
//        :- dynamic(ppSuper_'/1).
//
//        //ppSynchronized_(Head, Mutex)
//        :- dynamic(ppSynchronized_'/2).
//        //pp_predicate_mutexCounter_(Count)
//        :- dynamic(pp_predicate_mutexCounter_'/1).
//        //ppDynamic_(Head)
//        :- dynamic(ppDynamic_'/1).
//        //ppDiscontiguous_(Head)
//        :- dynamic(ppDiscontiguous_'/1).
//        //pp_mode_(Mode, Determinism, File, Lines)
//        :- dynamic(pp_mode_'/4).
//        //pp_public_(Functor, Arity)
//        :- dynamic(pp_public_'/2).
//        //pp_protected_(Functor, Arity)
//        :- dynamic(pp_protected_'/2).
//        //pp_private_(Functor, Arity)
//        :- dynamic(pp_private_'/2).
//        //pp_meta_predicate_(PredTemplate, MetaTemplate)
//        :- dynamic(pp_meta_predicate_'/2).
//        //pp_predicateAlias_(Entity, Pred, Alias, NonTerminalFlag, File, Lines)
//        :- dynamic(pp_predicateAlias_'/6).
//        //pp_non_terminal_(Functor, Arity, ExtArity)
//        :- dynamic(pp_non_terminal_'/3).
//        //pp_multifile_(Head, File, Lines)
//        :- dynamic(pp_multifile_'/3).
//        //ppCoinductive_(Head, TestHead, HeadExCtx, TCHead, BodyExCtx, THead, DHead)
//        :- dynamic(ppCoinductive_'/7).
//
//        //pp_object_(Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, Flags)
//        :- dynamic(pp_object_'/11).
//        //ppCategory_(Ctg, Prefix, Dcl, Def, Rnm, Flags)
//        :- dynamic(ppCategory_'/6).
//        //pp_protocol_(Ptc, Prefix, Dcl, Rnm, Flags)
//        :- dynamic(pp_protocol_'/5).
//        //pp_entity_(Type, Entity, Prefix, Dcl, Rnm)
//        :- dynamic(pp_entity_'/5).
//        //pp_module_(Module)
//        :- dynamic(pp_module_'/1).
//
//        //pp_parameter_variables_(ParameterVariables)
//        :- dynamic(pp_parameter_variables_'/1).
//
//        //pp_uses_predicate_(Obj, Predicate, Alias, Lines)
//        :- dynamic(pp_uses_predicate_'/4).
//        //pp_uses_non_terminal_(Obj, NonTerminal, NonTerminalAlias, Predicate, PredicateAlias, Lines)
//        :- dynamic(pp_uses_non_terminal_'/6).
//        //pp_use_module_predicate_(Module, Predicate, Alias, Lines)
//        :- dynamic(pp_use_module_predicate_'/4).
//        //pp_use_module_non_terminal_(Module, NonTerminal, NonTerminalAlias, Predicate, PredicateAlias, Lines)
//        :- dynamic(pp_use_module_non_terminal_'/6).
//        //pp_entityInfo_(List)
//        :- dynamic(pp_entityInfo_'/1).
//        //pp_predicateInfo_(Predicate, List)
//        :- dynamic(pp_predicateInfo_'/2).
//
//        //ppImplemented_protocol_(Ptc, ObjOrCtg, Prefix, Dcl, Scope)
//        :- dynamic(ppImplemented_protocol_'/5).
//        //ppImportedCategory_(Ctg, Obj, Prefix, Dcl, Def, Scope)
//        :- dynamic(ppImportedCategory_'/6).
//        //pp_extended_object_(Parent, Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Scope)
//        :- dynamic(pp_extended_object_'/11).
//        //ppInstantiatedClass_(Class, Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Scope)
//        :- dynamic(ppInstantiatedClass_'/11).
//        //ppSpecializedClass_(Superclass, Class, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Scope)
//        :- dynamic(ppSpecializedClass_'/11).
//        //pp_extended_protocol_(ExtPtc, Ptc, Prefix, Dcl, Scope)
//        :- dynamic(pp_extended_protocol_'/5).
//        //pp_extendedCategory_(ExtCtg, Ctg, Prefix, Dcl, Def, Scope)
//        :- dynamic(pp_extendedCategory_'/6).
//        //ppComplemented_object_(Obj, Ctg, Dcl, Def, Rnm)
//        :- dynamic(ppComplemented_object_'/5).
//
//        //ppFileInitialization_(Goal, Lines)
//        :- dynamic(ppFileInitialization_'/2).
//        //ppFile_objectInitialization_(Object, Goal, Lines)
//        :- dynamic(ppFile_objectInitialization_'/3).
//
//        //pp_objectInitialization_(Goal, SourceData, Lines)
//        :- dynamic(pp_objectInitialization_'/3).
//        //ppFinal_objectInitialization_(Goal, Lines)
//        :- dynamic(ppFinal_objectInitialization_'/2).
//
//        //pp_entity_metaDirective_(Directive, SourceData, Lines)
//        :- dynamic(pp_entity_metaDirective_'/3).
//
//        //ppRedefined_builtIn_(Head, ExCtx, THead)
//        :- dynamic(ppRedefined_builtIn_'/3).
//
//        //ppDirective_(Directive)
//        :- dynamic(ppDirective_'/1).
//        //pp_prolog_term_(Term, SourceData, Lines)
//        :- dynamic(pp_prolog_term_'/3).
//        //pp_entity_term_(Term, SourceData, Lines)
//        :- dynamic(pp_entity_term_'/3).
//        //ppFinal_entity_term_(Term, Lines)
//        :- dynamic(ppFinal_entity_term_'/2).
//        //pp_entityAuxClause_(T)
//        :- dynamic(pp_entityAuxClause_'/1).
//        //ppFinal_entityAuxClause_(T)
//        :- dynamic(ppFinal_entityAuxClause_'/1).
//
//        //pp_number_ofClausesRules_(Functor, Arity, NumberOfClauses, NumberOfRules)
//        :- dynamic(pp_number_ofClausesRules_'/4).
//        //pp_number_ofClausesRules_(Other, Functor, Arity, NumberOfClauses, NumberOfRules)
//        :- dynamic(pp_number_ofClausesRules_'/5).
//
//        //pp_predicateDeclarationLocation_(Functor, Arity, File, Line)
//        :- dynamic(pp_predicateDeclarationLocation_'/4).
//        //pp_predicateDefinitionLocation_(Functor, Arity, File, Line)
//        :- dynamic(pp_predicateDefinitionLocation_'/4).
//        //ppDefines_predicate_(Head, Functor/Arity, ExCtx, THead, Mode, Origin)
//        :- dynamic(ppDefines_predicate_'/6).
//        //ppInline_predicate_(Functor/Arity)
//        :- dynamic(ppInline_predicate_'/1).
//
//        //pp_predicateDefinitionLocation_(Other, Functor, Arity, File, Line)
//        :- dynamic(pp_predicateDefinitionLocation_'/5).
//
//        //ppCalls_predicate_(Functor/Arity, TFunctor/TArity, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(ppCalls_predicate_'/5).
//        //ppCallsSelf_predicate_(Functor/Arity, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(ppCallsSelf_predicate_'/4).
//        //ppCallsSuper_predicate_(Functor/Arity, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(ppCallsSuper_predicate_'/4).
//
//        //pp_updates_predicate_(Dynamic, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(pp_updates_predicate_'/4).
//
//        //pp_non_portable_predicate_(Head, File, Lines)
//        :- dynamic(pp_non_portable_predicate_'/3).
//        //pp_non_portableFunction_(Function, File, Lines)
//        :- dynamic(pp_non_portableFunction_'/3).
//
//        //pp_missing_meta_predicateDirective_(Head, File, Lines)
//        :- dynamic(pp_missing_meta_predicateDirective_'/3).
//        //pp_missingDynamicDirective_(Head, File, Lines)
//        :- dynamic(pp_missingDynamicDirective_'/3).
//        //pp_missingDiscontiguousDirective_(Head, File, Lines)
//        :- dynamic(pp_missingDiscontiguousDirective_'/3).
//
//        //pp_previous_predicate_(Head, Mode)
//        :- dynamic(pp_previous_predicate_'/2).
//
//        //ppDefines_non_terminal_(Functor, Arity)
//        :- dynamic(ppDefines_non_terminal_'/2).
//        //ppCalls_non_terminal_(Functor, Arity, Lines)
//        :- dynamic(ppCalls_non_terminal_'/3).
//
//        //ppReferenced_object_(Object, File, Lines)
//        :- dynamic(ppReferenced_object_'/3).
//        //ppReferenced_protocol_(Protocol, File, Lines)
//        :- dynamic(ppReferenced_protocol_'/3).
//        //ppReferencedCategory_(Category, File, Lines)
//        :- dynamic(ppReferencedCategory_'/3).
//        //ppReferenced_module_(Module, File, Lines)
//        :- dynamic(ppReferenced_module_'/3).
//
//        //ppReferenced_object_message_(Object, Functor/Arity, AliasFunctor/Arity, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(ppReferenced_object_message_'/6).
//        //ppReferenced_module_predicate_(Module, Functor/Arity, AliasFunctor/Arity, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(ppReferenced_module_predicate_'/6).
//
//        //pp_global_operator_(Priority, Specifier, Operator)
//        :- dynamic(pp_global_operator_'/3).
//        //ppFile_operator_(Priority, Specifier, Operator)
//        :- dynamic(ppFile_operator_'/3).
//        //pp_entity_operator_(Priority, Specifier, Operator, Scope)
//        :- dynamic(pp_entity_operator_'/4).
//
//        //pp_warnings_top_goal_(Goal)
//        :- dynamic(pp_warnings_top_goal_'/1).
//        //ppCompiling_warningsCounter_(Counter)
//        :- dynamic(ppCompiling_warningsCounter_'/1).
//        //ppLoading_warningsCounter_(Counter)
//        :- dynamic(ppLoading_warningsCounter_'/1).
//
//        //pp_hook_term_expansion_(Term, Terms)
//        :- dynamic(pp_hook_term_expansion_'/2).
//        //pp_hook_goal_expansion_(Goal, ExpandedGoal)
//        :- dynamic(pp_hook_goal_expansion_'/2).
//
//        //pp_builtIn_'
//        :- dynamic(pp_builtIn_'/0).
//        //ppDynamic_'
//        :- dynamic(ppDynamic_'/0).
//        //pp_threaded_'
//        :- dynamic(pp_threaded_'/0).
//
//        //ppFile_encoding_(LogtalkEncoding, PrologEncoding, Line)
//        :- dynamic(ppFile_encoding_'/3).
//        //ppFile_bom_(BOM)
//        :- dynamic(ppFile_bom_'/1).
//        //ppFile_pathsFlags_(Basename, Directory, SourceFile, ObjectFile, Flags)
//        :- dynamic(ppFile_pathsFlags_'/5).
//
//        //ppRuntimeClause_(T)
//        :- dynamic(ppRuntimeClause_'/1).
//
//        //ppCcIfFound_(Goal)
//        :- dynamic(ppCcIfFound_'/1).
//        //ppCcSkipping_'
//        :- dynamic(ppCcSkipping_'/0).
//        //ppCc_mode_(Action)
//        :- dynamic(ppCc_mode_'/1).
//
//        //pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines)
//        :- dynamic(pp_term_variable_namesFileLines_'/4).
//
//        //ppAux_predicateCounter_(Counter)
//        :- dynamic(ppAux_predicateCounter_'/1).


    protected
    void cacheCompilerFlags () {

    }
    //compiling and loading built-in predicates


    //CompilerFlag(+atom, ?nonvar)

    //gets/checks the current value of a compiler flag; the default flag
    //values and the backend Prolog feature flags are cached at startup

//        getCompilerFlag(Name, Value) :-
//                (	pp_entityCompilerFlag_(Name, CurrentValue) ->
//        //flag value as defined within the entity being compiled
//        Value = CurrentValue
//        ;	ppFileCompilerFlag_(Name, CurrentValue) ->
//        //flag value as defined in the flags argument of the
//        //compiling/loading predicates or in the source file
//        Value = CurrentValue
//        ;	CurrentFlag_(Name, Value)
//        //default value for the current Logtalk session,
//        //cached or set by calls to the setLogtalkFlag/2 predicate
//        ).
//

    /**
     * @param fileName
     */
    public
    void logtalkCompile ( List <String> fileName ) {

    }

    /**
     * @param fileName
     */
    public
    void logtalkCompile ( String fileName ) {
//         user, user, user, user, [], []
        executionContext.getFlags();

    }
    //logtalkCompile(@list(sourceFile_name))

    //compiles to disk a source file or list of source files using default flags

    //top-level calls use the current working directory for resolving any relative
    //source file paths while compiled calls in a source file use the source file
    //directory by default

//        logtalkCompile(Files) :-
//                executionContext(ExCtx, user, user, user, user, [], []),
//                CurrentDirectory(Directory),
//                LogtalkCompile(Files, Directory, ExCtx).
//
//
//        LogtalkCompile(Files, Directory, ExCtx) :-
//        catch(
//                LogtalkCompileFiles(Files, Directory),
//                error(Error, _),
//                LogtalkCompile_error_handler(Error, Files, ExCtx)
//        ).
//
//
//        LogtalkCompileFiles(Files, Directory) :-
//                Init_warningsCounter(logtalkCompile(Files)),
//                CheckAnd_expandSourceFiles(Files, ExpandedFiles),
//                CompileFiles(ExpandedFiles, ['$relative_to(Directory)]),
//        Report_warning_numbers(logtalkCompile(Files)),
//                Clean_ppFileClauses'.
//
//
//        LogtalkCompile_error_handler(Error, Files, ExCtx) :-
//                Clean_ppFileClauses',
//        Clean_pp_entityClauses',
//        Reset_warningsCounter',
//        throw(error(Error, logtalk(logtalkCompile(Files), ExCtx))).


    //logtalkCompile(@sourceFile_name, @list(compilerFlag))
    //logtalkCompile(@list(sourceFile_name), @list(compilerFlag))

    //compiles to disk a source file or a list of source files using a list of flags

    //top-level calls use the current working directory for resolving any relative
    //source file paths while compiled calls in a source file use the source file
    //directory by default

    //note that we can only clean the compiler flags after reporting warning numbers as the
    //report/1 flag might be included in the list of flags but we cannot test for it as its
//        //value should only be used in the default code for printing messages
//        protected void logtalkCompile (List < String > files, List < Flag > flags){
//
//        }

    //        }
//        CurrentDirectory(Directory), LogtalkCompile(Files, Flags, Directory, ExCtx).
//
//
//                LogtalkCompile(Files, Flags, Directory, ExCtx) :-
//        catch
//        (Logtalks(Files, Flags, Directory), error(Error, _), LogtalkCompile_error_handler(Error, Files, Flags, ExCtx)
//        ).
//
//
//        LogtalkCompileFiles(Files, Flags, Directory) :
//        -Init_warningsCounter(logtalkCompile(Files, Flags)), CheckAnd_expandSourceFiles(Files, ExpandedFiles), CheckCompilerFlags(Flags), (member(relative_to(_), Flags) ->
//        CompileFiles(ExpandedFiles, Flags);
//        CompileFiles(ExpandedFiles,['$relative_to(Directory)| Flags])
//        ),
//        Report_warning_numbers(logtalkCompile(Files, Flags)), Clean_ppFileClauses '.
//
//
//        LogtalkCompile_error_handler(Error, Files, Flags, ExCtx) :-Clean_ppFileClauses
//        ', Clean_pp_entityClauses ', Reset_warningsCounter
//        ', throw (error(Error, logtalk(logtalkCompile(Files, Flags), ExCtx))).
//
//
//        //predicates for compilation warning counting and reporting
//
    protected
    void resetWarningsCounters () {
//            retractall(pp_warnings_top_goal_(_)),
//            retractall(ppCompiling_warningsCounter_(_)),
//            retractall(ppLoading_warningsCounter_(_)).
    }

    //
//
//
    protected
    void initWarningsCounter ( Term goal ) {
//        (pp_warnings_top_goal_(_) ->

//        //not top compilation/loading goal; do nothing
//        true;	%remember top compilation / loading goal assertz (pp_warnings_top_goal_(Goal)),
//                //initialize compilation warnings counter
//                retractall(ppCompiling_warningsCounter_(_)), assertz(ppCompiling_warningsCounter_(0)),
//                //initialize loading warnings counter
//                retractall(ppLoading_warningsCounter_(_)), assertz(ppLoading_warningsCounter_(0))
//        ).
//
    }

    //
    void incrementCompilingWarningsCounter () {
//        ' :- retract(ppCompiling_warningsCounter_(Old)), New is Old + 1, assertz(ppCompiling_warningsCounter_(New)).
//
    }

    void incrementLoading_warningsCounter () {
    }
//        ' :- retract(ppLoading_warningsCounter_(Old)), New is Old + 1, assertz(ppLoading_warningsCounter_(New)).
//
//
//                Report_warning_numbers(Goal) :-(retract(pp_warnings_top_goal_(Goal)),
//                //top compilation/loading goal
//                retract(ppCompiling_warningsCounter_(CCounter)), retract(ppLoading_warningsCounter_(LCounter)) ->
//        //report compilation and loading warnings
//        print_message(comment(warnings), core, compilationAndLoading_warnings(CCounter, LCounter));	%not top
//        compilation / loading goal true
//        ).
//
//
//===============================================================================


//===============================================================================
//        //term-expansion errors result in a warning message and a failure
//
//        term_expansion_error(HookEntity, Term, Error) :
//        -SourceFileContext(File, Lines), (pp_entity_(Type, Entity, _, _, _) ->
//        print_message(warning(expansion), core, term_expansion_error(File, Lines, Type, Entity, HookEntity, Term, Error));
//        print_message(warning(expansion), core, term_expansion_error(File, Lines, HookEntity, Term, Error))
//        ),
//        fail.
//
//
//                //goal-expansion errors result in a warning message and a failure
//
//                        goal_expansion_error(HookEntity, Goal, Error) :
//        -SourceFileContext(File, Lines), (pp_entity_(Type, Entity, _, _, _) ->
//        print_message(warning(expansion), core, (File, Lines, Type, Entity, HookEntity, Goal, Error));
//        print_message(warning(expansion), core, (File, Lines, Type, Entity, HookEntity, Goal, Error));
//        print_message(warning(expansion), core, goal_expansion_error(File, Lines, HookEntity, Goal, Error))
//        ),
//        fail.
//
//
//                AssertCompilerFlags([]).
//        AssertCompilerFlags([Flag | Flags]) :- Flag =.. [Name, Value],
//        retractall(ppFileCompilerFlag_(Name, _)), assertz(ppFileCompilerFlag_(Name, Value)), AssertCompilerFlags(Flags).


    protected
    void startRuntimeThreading () {


    }

    protected
    void compileDefaultHooks () {

    }

    //=================================================================================


    /**
     * loads all built-in entities if not already loaded (when embedding
     * Logtalk, the pre-compiled entities are loaded prior to this file)
     */
    protected
    String loadBuiltInEntities () throws Exception {
        String scratchDir = getScratchDirectory();
        loadContext.reset();//TODO
        loadBuiltInEntity(EXPANDING, "expanding", scratchDir);
        loadBuiltInEntity(MONITORING, "monitoring", scratchDir);
        loadBuiltInEntity(FORWARDING, "forwarding", scratchDir);
        loadBuiltInEntity(USER, "user", scratchDir);
        loadBuiltInEntity(LOGTALK, "logtalk", scratchDir);
        loadBuiltInEntity(CORE_MESSAGES, "core_messages", scratchDir);
//        loadBuiltInEntity(ENUM, "enum", scratchDir);

        return scratchDir;
    }

    /**
     * expandLibraryAlias(Library, Path, Depth ):-
     * logtalkLibrary_path(Library, Location), !, (compound(Location), Location =.. [Prefix, Directory],
     * atom(Directory) ->
     * //assume library notation (a compound term)
     * Depth > 0, NewDepth is Depth -1, expandLibraryAlias(Prefix, PrefixPath0, NewDepth),
     * //make sure that the prefix path ends with a slash
     * (subAtom(PrefixPath0, _, 1, 0, '/') ->
     * atomConcat(PrefixPath0, Directory, Path);
     * atomConcat(PrefixPath0, '/', PrefixPath1), atomConcat(PrefixPath1, Directory, Path)
     * )
     * ;
     * atom(Location) ->
     * //assume the final component of the library path
     * Path = Location;
     * ground(Location) ->
     * throw (error(type_error(library_path, Location), _))
     * ;
     * throw (error(instantiation_error, _))
     * ).
     * //
     *
     * @return
     */
    protected
    Path expandLibraryAlias ( String library ) {
        Path location = logtalkLibraryPath(library);
        return location;
    }

    protected
    Path logtalkLibraryPath ( String library ) {

//        Path path = new Vfs2NioPath();
        return null;// resolve(library);
    }

    /**
     * print_message(Kind, Component, Message) :-
     * (	builtIn_entitiesLoaded_' ->
     * //"logtalk" built-in object loaded
     * executionContext(ExCtx, logtalk, logtalk, logtalk, logtalk, [], []),
     * '$logtalk#0.print_message#3(Kind, Component, Message, ExCtx)
     * ;	% still compiling the default built-in entities
     * CompilerFlag(report, off) ->
     * //no message printing required
     * true
     * ;	% bare-bones message printing
     * writeq(Component), write(' '), write(Kind), write(': '), writeq(Message), nl
     * ).
     */
//    protected
//    void printMessage ( Functor kind, Atom component, Atom message ){
//if (getCompilerFlag(FlagKey.REPORTS) != FlagValue.OFF) {
////
////
////        }
//        }
    public
    String getScratchDirectory () {
        return scratchDirectory;
    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
//    @Override
    public
    void setCompilerObserver ( LogicCompilerObserver <P, Q> observer ) {
        this.observer = observer;
    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
//    @Override
    public
    void endScope () throws SourceCodeException {

    }

    /**
     * @return
     */
    //    @Override
    public
    ICompiler <T, P, Q> getPreCompiler () {
        return preCompiler;
    }

    /**
     * @return
     */
    public
    String getFileName () {
        return fileName;
    }

    /**
     * @param fileName
     */
    public
    void setFileName ( String fileName ) {
        this.fileName = fileName;
    }

    /**
     * @return
     */
    @Override
    public
    IConfig getConfig () {
        return config;
    }

    @Override
    public
    void init () {

    }

    /**
     * @return
     */
    protected
    List <HtEntityIdentifier> hooksPipeline () {
//        Map <Functor, INameable <Functor>>[] tables = bkt.getTables();
        List <HtEntityIdentifier> l = new ArrayList <>();

        return l;
    }

//======================================================================
// remaining directives

    /**
     *
     */
    @Override
    public
    void start () throws Exception {
        initialize();

    }

    /**
     * @return
     */
    @Override
    public
    int end () {
        System.exit(0);
        return 0;
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStarted () {
        return started;
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStopped () {
        return !started;
    }

    /**
     *
     */
    @Override
    public
    void banner () {
        String product = "HiTalk system";
        String version = "v0.1.0.b#";
        int build = 0;
        String copyright = "(c) Anton Danilov, 2018-2019, All rights reserved";
        System.out.printf("\n%s, %s%d, %s.\n", product, version, build, copyright);
    }

    /**
     * @param tokenSource
     */
    public
    void setTokenSource ( TokenSource tokenSource ) {
        parser.setTokenSource((HtTokenSource) tokenSource);
    }

    /**
     * @param parser
     */
    public
    void setParser ( HtPrologParser parser ) {
        this.parser = parser;
    }

    /**
     * @return
     */
    public
    ExecutionContext getExecutionContext () {
        return executionContext;
    }

    /**
     * @return
     */
    @Override
    public
    Logger getConsole () {
        return null;
    }

    /**
     * @param sentence
     * @param flags
     * @throws SourceCodeException
     */
    @Override
    public
    void compile ( HtClause sentence, Flag... flags ) throws SourceCodeException {

    }

    /**
     * @param rule
     */
    @Override
    public
    void compileDcgRule ( DcgRule rule ) throws SourceCodeException {

    }

    /**
     * @param query
     */
    @Override
    public
    void compileQuery ( HtClause query ) {

    }

    /**
     * @param resolver
     */
    @Override
    public
    void setResolver ( Resolver <P, Q> resolver ) {
        this.resolver = resolver;
    }

    public
    Resolver <P, Q> getResolver () {
        return resolver;
    }
}

/**
 *
 */
class ClauseChainObserver<T extends HtClause, P, Q> implements LogicCompilerObserver <T, T> {
    protected ICompiler <T, P, Q> instructionCompiler;

    /**
     * @param instructionCompiler
     */
    ClauseChainObserver ( ICompiler <T, P, Q> instructionCompiler ) {
        this.instructionCompiler = instructionCompiler;
    }

    /**
     * {@inheritDoc}
     */
    public
    void onCompilation ( Sentence <T> sentence ) throws SourceCodeException {
        instructionCompiler.compile(sentence);
    }

    /**
     * {@inheritDoc}
     */
    public
    void onQueryCompilation ( Sentence <T> sentence ) throws SourceCodeException {
        instructionCompiler.compile(sentence);
    }
}

/**
 * ChainedCompilerObserver implements the compiler observer for this resolution engine. Compiled programs are added
 * to the resolvers domain. Compiled queries are executed.
 * <p>
 * <p/>If a chained observer is set up, all compiler outputs are forwarded onto it.
 */
class ChainedCompilerObserver<P, Q> implements LogicCompilerObserver <P, Q> {
    /**
     * Holds the chained observer for compiler outputs.
     */
    protected LogicCompilerObserver <P, Q> observer;
    protected Resolver <P, Q> resolver;

    /**
     * Sets the chained observer for compiler outputs.
     *
     * @param observer The chained observer.
     */
    public
    void setCompilerObserver ( LogicCompilerObserver <P, Q> observer ) {
        this.observer = observer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    void onCompilation ( Sentence <P> sentence ) throws SourceCodeException {
        if (observer != null) {
            observer.onCompilation(sentence);
        }

        getResolver().addToDomain(sentence.getT());
    }

//    /**
//     * {@inheritDoc}
//     */
//    @Override
//    public
//    void onQueryCompilation ( Sentence <Q> sentence ) throws SourceCodeException {
//        if (observer != null) {
//            observer.onQueryCompilation(sentence);
//        }
//
//        getResolver().setQuery(sentence.getT());
//    }

    /**
     * @return
     */
    public
    Resolver <P, Q> getResolver () {
        return resolver;
    }

    /**
     * @param resolver
     */
    public
    void setResolver ( Resolver <P, Q> resolver ) {
        this.resolver = resolver;
    }

    /**
     * Accepts notification of the completion of the compilation of a query into binary form.
     *
     * @param sentence The compiled query.
     * @throws SourceCodeException If there is an error in the compiled code that prevents its further processing.
     */
    @Override
    public
    void onQueryCompilation ( Sentence <Q> sentence ) throws SourceCodeException {

    }
}
