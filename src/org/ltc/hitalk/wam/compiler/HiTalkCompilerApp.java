package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.*;
import org.ltc.hitalk.compiler.bktables.BookKeepingTables.BkTableKind;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.context.CompilationContext;
import org.ltc.hitalk.entities.context.Context;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.term.Atom;
import org.ltc.hitalk.wam.machine.HiTalkWAMEngine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.ltc.hitalk.compiler.bktables.BookKeepingTables.BkTableKind.LOADED_ENTITIES;

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
class HiTalkCompilerApp extends HiTalkWAMEngine implements IApplication {

    private static final String DEFAULT_SCRATCH_DIRECTORY = "scratch";
    //
//    private static final String BANNER = "\nHiTalk compiler, v0.0.1-b#%d Anton Danilov (c) 2018-2019, All rights reserved\n";
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
    public final HiTalkFlag[] DEFAULT_FLAGS;
    //library entity names
    private final HtEntityIdentifier EXPANDING;
    private final HtEntityIdentifier MONITORING;
    private final HtEntityIdentifier FORWARDING;
    private final HtEntityIdentifier USER;
    private final HtEntityIdentifier LOGTALK;
    private final HtEntityIdentifier CORE_MESSAGES;
    //
    private final HtEntityIdentifier OBJECT;
    private final HtEntityIdentifier PROTOCOL;
    private final HtEntityIdentifier CATEGORY;
    private final HtEntityIdentifier ENUM;

    /**
     * Used for logging to the console.
     */
    private /*static*/ final Logger console = Logger.getLogger("CONSOLE." + getClass().getSimpleName());
    boolean compiling;
    /**
     * True if the system is compiling source files with the -c option or qcompile/1
     * into an intermediate code file.
     * Can be used to perform conditional code optimizations in term_expansion/2 (see also the -O option)
     * or to omit execution of directives during compilation.
     */
    private final ITermFactory tf;
    /**
     * Holds the instruction generating compiler.
     */
    private final HiTalkInstructionCompiler instructionCompiler;
    private final String scratchDirectory;
    //
    private final BookKeepingTables <Functor, INameable <Functor>> bkt = new BookKeepingTables <>();
    private final CompilationContext compilationContext;
    private final LoadContext loadContext;
    private final ExecutionContext executionContext;
    protected LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer;
    protected LogicCompilerObserver <Clause, Clause> observer2;
    private TokenSource tokenSource;
    /**
     * Holds the pre-compiler, for analyzing and transforming terms prior to compilation proper.
     */
    private HiTalkPreCompiler preCompiler;
    private String fileName;
    private IConfig config;
    //    private HiTalkCompilerApp app;
    private boolean started;
    private HtPrologParser parser;

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
                        HiTalkInstructionCompiler compiler,
                        HiTalkDefaultBuiltIn defaultBuiltIn ) {
        super(parser, interner, compiler);

        preCompiler = new HiTalkPreprocessor(symbolTable, interner, defaultBuiltIn);
        observer2 = new ClauseChainObserver();
        preCompiler.setCompilerObserver(observer2);

        instructionCompiler = compiler;
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
        OBJECT = tf.createIdentifier(HtEntityKind.OBJECT, "object");
        PROTOCOL = tf.createIdentifier(HtEntityKind.OBJECT, "protocol");
        CATEGORY = tf.createIdentifier(HtEntityKind.OBJECT, "category");

        ENUM = tf.createIdentifier(HtEntityKind.OBJECT, "enum");

        DEFAULT_FLAGS = new HiTalkFlag[]{
                tf.createFlag("access", "read_write"),//read_only
                tf.createFlag("keep", "false"),
                //
                tf.createFlag("type", "false"),

                };

        compilationContext = new CompilationContext();
        loadContext = new LoadContext(DEFAULT_FLAGS);
        executionContext = new ExecutionContext();
    }

    public static
    void main ( String[] args ) {
        try {
            SymbolTable <Integer, String, Object> symbolTable = new SymbolTableImpl <>();
            VariableAndFunctorInterner interner = new VariableAndFunctorInternerImpl("HiTalk_Variable_Namespace", "HiTalk_Functor_Namespace");
            TokenSource tokenSource = TokenSource.getTokenSourceForFile(new File(args[0]));
            HtPrologParser parser = new HtPrologParser(tokenSource, interner);
            HiTalkInstructionCompiler compiler = new HiTalkInstructionCompiler(symbolTable, interner);
            HiTalkDefaultBuiltIn defaultBuiltIn = new HiTalkDefaultBuiltIn(symbolTable, interner);//
//
            HiTalkCompilerApp app = new HiTalkCompilerApp(symbolTable, interner, parser, compiler, defaultBuiltIn);
            app.setFileName(args[0]);
            app.createFlags(app.loadContext, DEFAULT_SCRATCH_DIRECTORY);
            app.setTokenSource(tokenSource);
            app.setParser(parser);
            //
            app.banner();
            app.start();

        } catch (IOException | SourceCodeException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    /**
     * String basename;
     * Path directory;
     * HtEntityIdentifier entityIdentifier;
     * //    entity_prefix,
     * //    entity_type,IN eI
     * String file;
     * // HiTalkFlag[] flags;
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
    private
    HiTalkFlag[] createFlags ( LoadContext loadContext, String scratchDirectory ) {
        HiTalkFlag[] flags = new HiTalkFlag[]{///todo flags
                                              tf.createFlag("basename", fileName),//FIXME PARSE
                                              tf.createFlag("directory", fileName),//FIXME PARSE
                                              tf.createFlag("entity_identifier", new Functor(-1, Atom.EMPTY_TERM_ARRAY)),//FIXME PARSE
                                              tf.createFlag("file", fileName),//FIXME PARSE
                                              tf.createFlag("basename", fileName),//FIXME PARSE
        };

        return flags;
    }

    /**
     *
     */
    void initialize () throws IOException, SourceCodeException {
        initBookKeepingTables();
        initDirectives();
        cacheCompilerFlags();
        String scratchDirectory = loadBuiltInEntities();
        Object result = loadSettingsFile(scratchDirectory);
//        printMessage(BANNER, CORE, BANNER);
//        printMessage(comment(settings), CORE, DEFAULT_FLAGS);
//        expandGoals()
//        ;
//        compileHooks();
        startRuntimeThreading();
        reportSettingsFile(result);
    }

    private
    void reportSettingsFile ( Object result ) {

    }

    private
    Object loadSettingsFile ( String scratchDir ) {
        return null;
    }

    protected
    Map <Functor, INameable <Functor>> get ( BkTableKind tableKind ) {
        return bkt.getTables()[tableKind.ordinal()];
    }

    /**
     * @param identifier
     * @param fileName
     * @param scratchDir
     * @throws IOException
     * @throws SourceCodeException
     */
    private
    void loadBuiltInEntity ( HtEntityIdentifier identifier, String fileName, String scratchDir ) throws IOException,
                                                                                                        SourceCodeException {

        Map <Functor, INameable <Functor>> loadedEntities = get(LOADED_ENTITIES);
//        context = new Context(createFlags(scratchDir));
        if (!loadedEntities.containsKey(identifier)) {
            compileLoad(fileName, loadContext);///core()
        }
    }

    private
    HiTalkFlag[] createFlags ( String scratchDir ) {
        return new HiTalkFlag[]{
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
    void compileLoad ( List <String> fileNames, LoadContext context ) throws IOException, SourceCodeException {
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
    void compileLoad ( String fileName, LoadContext context ) throws IOException, SourceCodeException {
        compileFile(fileName, context);
        loadFile(fileName, context);//bytecode
    }

    public //todo read_files
    void loadFiles ( List <String> fileNames, LoadContext context ) throws IOException, SourceCodeException {
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
    void loadFile ( String fileName, LoadContext context ) throws IOException, SourceCodeException {
        compileFile(fileName, context);
        String baseName = context.get(LoadContext.Kind.Loading.BASENAME);
        loadTargetFile(fileName, context);
    }

    private
    void loadTargetFile ( String targetFile, LoadContext context ) {

    }

    private
    void compileFile ( String fileName, LoadContext context ) throws IOException, SourceCodeException {
        // Set up a parser on the token source.
        HtPrologParser parser = new HtPrologParser(tokenSource, interner);

        compiler.endScope();
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
    private
    void initBookKeepingTables () {

    }

    /**
     * compiles the user-defined default compiler hooks
     * (replacing any existing defined hooks)
     */
    private
    void compileHooks ( HtEntityIdentifier hookEntity ) {

        Map <?, ?> table; //= bkt.getTable(TERM_EXPANSION_DEFAULT_HOOKS);

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


    private
    void initDirectives () {
        initRuntimeDirectives();
        initCompilerDirectives();
    }

    private
    void initCompilerDirectives () {

    }

    private
    void initRuntimeDirectives () {

    }

    //Compile_message_to_object(@term, @objectIdentifier, -callable, +atom, +compilationContext)

    //compiles a message sending call

    //messages to the pseudo-object "user"

    private
    void compileMmessageToObject ( Term pred, HtEntityIdentifier obj, ICallable call, Atom atom, Context ctx ) {
        if (obj.equals(USER) && pred.isVar() || pred.isFunctor()) {

        }
    }

//    Obj == USER,
//            !,
//    Check(var_orCallable, Pred).
//
//    //convenient access to parametric object proxies
//
//    Compile_message_to_object(Pred, Obj, (CallProxy, TPred), Events, Ctx) :-
//    nonvar(Obj),
//    Obj = {Proxy},
//            !,
//    Check(var_orCallable, Proxy),
//        (	var(Proxy) ->
//    CallProxy = call(Proxy)
//            ;	CallProxy = Proxy
//        ),
//    CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
//    executionContext_this_entity(ExCtx, This, _),
//    Compile_message_to_object(Pred, Proxy, TPred, Events, Ctx).
//
//    //entityKind and lint checks
//
//    Compile_message_to_object(_, Obj, _, _, Ctx) :-
//            (	callable(Obj) ->
//    //remember the object receiving the message
//    AddReferenced_object(Obj, Ctx),
//    fail
//    ;	nonvar(Obj),
//    //invalid object identifier
//        throw(type_error(objectIdentifier, Obj))
//            ).
//
//    //suspicious use of ::/2 instead of ::/1 to send a message to "self"
//
//    Compile_message_to_object(Pred, Obj, _, _, Ctx) :-
//    CompCtx(Ctx, _, _, _, _, _, _, _, _, _, ExCtx, compile(_), _, _),
//    CompilerFlag(suspiciousCalls, warning),
//    executionContext(ExCtx, _, _, _, Self, _, _),
//    Self == Obj,
//    IncrementCompiling_warningsCounter',
//    SourceFileContext(File, Lines, Type, Entity),
//    print_message(warning(suspiciousCalls), core, suspiciousCall(File, Lines, Type, Entity, Obj::Pred, ::Pred)),
//            fail.
//
//                    //suspicious use of ::/2 in objects to call a local predicate
//
//                    Compile_message_to_object(Pred, Obj, _, _, Ctx) :-
//    CompCtx(Ctx, _, _, _, _, _, _, _, _, _, ExCtx, compile(_), _, _),
//    pp_entity_(object, _, _, _, _),
//    CompilerFlag(suspiciousCalls, warning),
//    executionContext(ExCtx, _, _, This, _, _, _),
//    This == Obj,
//    IncrementCompiling_warningsCounter',
//    SourceFileContext(File, Lines, Type, Entity),
//    print_message(warning(suspiciousCalls), core, suspiciousCall(File, Lines, Type, Entity, Obj::Pred, Pred)),
//            fail.
//
//                    //translation performed at runtime
//
//                    Compile_message_to_object(Pred, Obj, Send_to_objRt(Obj, Pred, Events, NewCtx), Events, Ctx) :-
//    var(Pred),
//        !,
//    CompCtx(Ctx, Head, _, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, _, Stack, Lines),
//    CompCtx(NewCtx, Head, _, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, runtime, Stack, Lines).
//
//    //broadcasting control constructs
//
//    Compile_message_to_object((Pred1, Pred2), Obj, (TPred1, TPred2), Events, Ctx) :-
//            !,
//    Compile_message_to_object(Pred1, Obj, TPred1, Events, Ctx),
//    Compile_message_to_object(Pred2, Obj, TPred2, Events, Ctx).
//
//    Compile_message_to_object((Pred1; Pred2), Obj, (TPred1; TPred2), Events, Ctx) :-
//            !,
//    Compile_message_to_object(Pred1, Obj, TPred1, Events, Ctx),
//    Compile_message_to_object(Pred2, Obj, TPred2, Events, Ctx).
//
//    Compile_message_to_object((Pred1 -> Pred2), Obj, (TPred1 -> TPred2), Events, Ctx) :-
//            !,
//    Compile_message_to_object(Pred1, Obj, TPred1, Events, Ctx),
//    Compile_message_to_object(Pred2, Obj, TPred2, Events, Ctx).
//
//    Compile_message_to_object('*->(Pred1, Pred2), Obj, '*->(TPred1, TPred2), Events, Ctx) :-
//    predicate_property('*->(_, _), builtIn),
//                               !,
//                       Compile_message_to_object(Pred1, Obj, TPred1, Events, Ctx),
//    Compile_message_to_object(Pred2, Obj, TPred2, Events, Ctx).
//
//    //built-in methods that cannot be redefined
//
//    Compile_message_to_object(!, Obj, (object_exists(Obj, !, ExCtx), !), _, Ctx) :-
//            !,
//    CompCtx_execCtx(Ctx, ExCtx).
//
//    Compile_message_to_object(true, Obj, (object_exists(Obj, true, ExCtx), true), _, Ctx) :-
//            !,
//    CompCtx_execCtx(Ctx, ExCtx).
//
//    Compile_message_to_object(fail, Obj, (object_exists(Obj, fail, ExCtx), fail), _, Ctx) :-
//            !,
//    CompCtx_execCtx(Ctx, ExCtx).
//
//    Compile_message_to_object(false, Obj, (object_exists(Obj, false, ExCtx), false), _, Ctx) :-
//            !,
//    CompCtx_execCtx(Ctx, ExCtx).
//
//    Compile_message_to_object(repeat, Obj, (object_exists(Obj, repeat, ExCtx), repeat), _, Ctx) :-
//            !,
//    CompCtx_execCtx(Ctx, ExCtx).
//
//    //reflection built-in predicates
//
//    Compile_message_to_object( current_op(Priority, Specifier, Operator ), Obj, Current_op(Obj, Priority, Specifier, Operator, This, p( p(p)), ExCtx), _, Ctx) :-
//            !,
//    Check(var_or_operator_priority, Priority),
//    Check(var_or_operatorSpecifier, Specifier),
//    Check(var_orAtom, Operator),
//    CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
//    executionContext_this_entity(ExCtx, This, _).
//
//    Compile_message_to_object(current_predicate(Pred), Obj, Current_predicate(Obj, Pred, This, p(p(p)), ExCtx), _, Ctx) :-
//            !,
//    Check(var_or_predicateIndicator, Pred),
//    CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
//    executionContext_this_entity(ExCtx, This, _).
//
//    Compile_message_to_object(predicate_property(Pred, Prop), Obj, predicate_property(Obj, Pred, Prop, This, p(p(p)), ExCtx), _, Ctx) :-
//            !,
//    Check(var_orCallable, Pred),
//    Check(var_or_predicate_property, Prop),
//    CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
//    executionContext_this_entity(ExCtx, This, _).
//
//    //database handling built-in predicates
//
//    Compile_message_to_object(abolish(Pred), Obj, TPred, _, Ctx) :-
//            !,
//    Check(var_or_predicateIndicator, Pred),
//    CompCtx(Ctx, Head, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
//    executionContext_this_entity(ExCtx, This, _),
//        (	var(Obj) ->
//    TPred = Abolish(Obj, Pred, This, p(p(p)), ExCtx)
//    ;	ground(Pred) ->
//    TPred = AbolishChecked(Obj, Pred, This, p(p(p)), ExCtx),
//    Remember_updated_predicate(Mode, Obj::Pred, Head)
//            ;	% partially instantiated predicate indicator; runtime check required
//            TPred = Abolish(Obj, Pred, This, p(p(p)), ExCtx)
//        ).
//
//    Compile_message_to_object(assert(Clause), Obj, TPred, Events, Ctx) :-
//            !,
//            (	CompCtx_mode(Ctx, compile(_)),
//    CompilerFlag(deprecated, warning),
//    SourceFileContext(File, Lines),
//    pp_entity_(Type, Entity, _, _, _) ->
//    IncrementCompiling_warningsCounter',
//    print_message(warning(deprecated), core, deprecated_predicate(File, Lines, Type, Entity, assert/1))
//    ;	true
//            ),
//    Compile_message_to_object(assertz(Clause), Obj, TPred, Events, Ctx).
//
//    Compile_message_to_object(asserta(Clause), Obj, TPred, _, Ctx) :-
//            !,
//    CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
//    executionContext_this_entity(ExCtx, This, _),
//        (	RuntimeCheckedDbClause(Clause) ->
//    TPred = Asserta(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx)
//    ;	var(Obj) ->
//    Check(clause_or_partialClause, Clause),
//    TPred = Asserta(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx)
//    ;	Check(clause_or_partialClause, Clause),
//        (	(Clause = (Head :- Body) -> Body == true; Clause = Head) ->
//            (	CompilerFlag(optimize, on),
//    Send_to_objDb_msgStatic_binding(Obj, Head, THead) ->
//    TPred = asserta(THead)
//            ;	TPred = AssertaFactChecked(Obj, Head, This, p(p(_)), p(p(p)), ExCtx)
//            ),
//    functor(Head, Functor, Arity),
//    Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
//            ;	TPred = AssertaRuleChecked(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx),
//    Clause = (Head :- _),
//    functor(Head, Functor, Arity),
//    Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
//        )
//                ).
//
//    Compile_message_to_object(assertz(Clause), Obj, TPred, _, Ctx) :-
//            !,
//    CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
//    executionContext_this_entity(ExCtx, This, _),
//        (	RuntimeCheckedDbClause(Clause) ->
//    TPred = Assertz(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx)
//    ;	var(Obj) ->
//    Check(clause_or_partialClause, Clause),
//    TPred = Assertz(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx)
//    ;	Check(clause_or_partialClause, Clause),
//        (	(Clause = (Head :- Body) -> Body == true; Clause = Head) ->
//            (	CompilerFlag(optimize, on),
//    Send_to_objDb_msgStatic_binding(Obj, Head, THead) ->
//    TPred = assertz(THead)
//            ;	TPred = AssertzFactChecked(Obj, Head, This, p(p(_)), p(p(p)), ExCtx)
//            ),
//    functor(Head, Functor, Arity),
//    Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
//            ;	TPred = AssertzRuleChecked(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx),
//    Clause = (Head :- _),
//    functor(Head, Functor, Arity),
//    Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
//        )
//                ).
//
//    Compile_message_to_object(clause(Head, Body), Obj, TPred, _, Ctx) :-
//            !,
//    CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
//    executionContext_this_entity(ExCtx, This, _),
//        (	RuntimeCheckedDbClause((Head :- Body)) ->
//    TPred = Clause(Obj, Head, Body, This, p(p(p)), ExCtx)
//    ;	Check(clause_or_partialClause, (Head :- Body)),
//            (	var(Obj) ->
//    TPred = Clause(Obj, Head, Body, This, p(p(p)), ExCtx)
//    ;	TPred = ClauseChecked(Obj, Head, Body, This, p(p(p)), ExCtx),
//    functor(Head, Functor, Arity),
//    Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
//        )
//                ).
//
//    Compile_message_to_object(retract(Clause), Obj, TPred, _, Ctx) :-
//            !,
//    CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
//    executionContext_this_entity(ExCtx, This, _),
//        (	RuntimeCheckedDbClause(Clause) ->
//    TPred = Retract(Obj, Clause, This, p(p(p)), ExCtx)
//    ;	var(Obj) ->
//    Check(clause_or_partialClause, Clause),
//    TPred = Retract(Obj, Clause, This, p(p(p)), ExCtx)
//    ;	Check(clause_or_partialClause, Clause),
//        (	Clause = (Head :- Body) ->
//            (	var(Body) ->
//    Retract_var_bodyChecked(Obj, Clause, This, p(p(p)), ExCtx)
//    ;	Body == true ->
//            (	CompilerFlag(optimize, on),
//    Send_to_objDb_msgStatic_binding(Obj, Head, THead) ->
//    TPred = retract(THead)
//            ;	TPred = RetractFactChecked(Obj, Head, This, p(p(p)), ExCtx)
//            )
//    ;	TPred = RetractRuleChecked(Obj, Clause, This, p(p(p)), ExCtx)
//            ),
//    functor(Head, Functor, Arity),
//    Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
//            ;	TPred = RetractFactChecked(Obj, Clause, This, p(p(p)), ExCtx),
//    functor(Clause, Functor, Arity),
//    Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
//        )
//                ).
//
//    Compile_message_to_object(retractall(Head), Obj, TPred, _, Ctx) :-
//            !,
//    CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
//    executionContext_this_entity(ExCtx, This, _),
//        (	var(Head) ->
//    TPred = Retractall(Obj, Head, This, p(p(p)), ExCtx)
//    ;	var(Obj) ->
//    Check(callable, Head),
//    TPred = Retractall(Obj, Head, This, p(p(p)), ExCtx)
//    ;	Check(callable, Head),
//        (	CompilerFlag(optimize, on),
//    Send_to_objDb_msgStatic_binding(Obj, Head, THead) ->
//    TPred = retractall(THead)
//            ;	TPred = RetractallChecked(Obj, Head, This, p(p(p)), ExCtx)
//            ),
//    functor(Head, Functor, Arity),
//    Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
//        ).
//
//    //term and goal expansion predicates
//
//    Compile_message_to_object(expand_term(Term, Expansion), Obj, expand_term_message(Obj, Term, Expansion, This, p(p(p)), ExCtx), _, Ctx) :-
//            !,
//    CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
//    executionContext_this_entity(ExCtx, This, _).
//
//    Compile_message_to_object(expand_goal(Goal, ExpandedGoal), Obj, expand_goal_message(Obj, Goal, ExpandedGoal, This, p(p(p))), _, Ctx) :-
//            !,
//    CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
//    executionContext_this_entity(ExCtx, This, _).
//
//    //compiler bypass control construct
//
//    Compile_message_to_object({Goal}, _, call(Goal), _, _) :-
//            !,
//    Check(var_orCallable, Goal).
//
//    //invalid message
//
//    Compile_message_to_object(Pred, _, _, _, _) :-
//            \+ callable(Pred),
//        throw(type_error(callable, Pred)).
//
//    //message is not a built-in control construct or a call to a built-in (meta-)predicate
//
//    Compile_message_to_object(Pred, Obj, TPred, Events, Ctx) :-
//    var(Obj),
//    //translation performed at runtime
//        !,
//    CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
//    AddReferenced_object_message(Mode, Obj, Pred, Pred, Head),
//        (	Events == allow ->
//    TPred = Send_to_obj(Obj, Pred, ExCtx)
//            ;	TPred = Send_to_obj_ne(Obj, Pred, ExCtx)
//        ).
//
//    Compile_message_to_object(Pred, Obj, TPred, Events, Ctx) :-
//    CompCtx(Ctx, Head, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
//    AddReferenced_object_message(Mode, Obj, Pred, Pred, Head),
//        (	Events == allow ->
//            (	CompilerFlag(optimize, on),
//    Send_to_objStatic_binding(Obj, Pred, Call, Ctx) ->
//    executionContext_this_entity(ExCtx, This, _),
//    TPred = guarded_methodCall(Obj, Pred, This, Call)
//            ;	TPred = Send_to_obj_(Obj, Pred, ExCtx)
//        )
//    ;	(	CompilerFlag(optimize, on),
//    Send_to_objStatic_binding(Obj, Pred, TPred, Ctx) ->
//            true
//    ;	TPred = Send_to_obj_ne_(Obj, Pred, ExCtx)
//        )
//                ).

//
//
//
//        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//        %
//        % runtime directives (bookkeeping tables)
//        %
//        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//
//
//
//        //tables of defined events and monitors
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
//
//        //table of loaded files
//
//
//
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
//
//
//
//        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//        %
//        // compiler directives
//        %
//        // (used for source file compilation and runtime creation of new entities)
//        %
//        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
//
//
//
//        //ppFileCompilerFlag_(Name, Value)
//        :- dynamic(ppFileCompilerFlag_'/2).
//        //pp_entityCompilerFlag_(Name, Value)
//        :- dynamic(pp_entityCompilerFlag_'/2).
//
//        //ppDcl_(Clause)
//        :- dynamic(ppDcl_'/1).
//        //ppDef_(Clause)
//        :- dynamic(ppDef_'/1).
//        //ppDdef_(Clause)
//        :- dynamic(ppDdef_'/1).
//        //ppSuper_(Clause)
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
//        //pp_entityAuxClause_(Clause)
//        :- dynamic(pp_entityAuxClause_'/1).
//        //ppFinal_entityAuxClause_(Clause)
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
//        //ppRuntimeClause_(Clause)
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


    private
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


    //logtalkCompile(@sourceFile_name)
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
//        private void logtalkCompile (List < String > files, List < Flag > flags){
//
//        }

//        }
//        CurrentDirectory(Directory), LogtalkCompile(Files, Flags, Directory, ExCtx).
//
//
//                LogtalkCompile(Files, Flags, Directory, ExCtx) :-
//        catch
//        (LogtalkCompileFiles(Files, Flags, Directory), error(Error, _), LogtalkCompile_error_handler(Error, Files, Flags, ExCtx)
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
//        Reset_warningsCounter ' :-
//        retractall(pp_warnings_top_goal_(_)), retractall(ppCompiling_warningsCounter_(_)), retractall(ppLoading_warningsCounter_(_)).
//
//
//                Init_warningsCounter(Goal) :-(pp_warnings_top_goal_(_) ->
//        //not top compilation/loading goal; do nothing
//        true;	%remember top compilation / loading goal assertz (pp_warnings_top_goal_(Goal)),
//                //initialize compilation warnings counter
//                retractall(ppCompiling_warningsCounter_(_)), assertz(ppCompiling_warningsCounter_(0)),
//                //initialize loading warnings counter
//                retractall(ppLoading_warningsCounter_(_)), assertz(ppLoading_warningsCounter_(0))
//        ).
//
//
//        IncrementCompiling_warningsCounter
//        ' :- retract(ppCompiling_warningsCounter_(Old)), New is Old + 1, assertz(ppCompiling_warningsCounter_(New)).
//
//
//                IncrementLoading_warningsCounter
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
//        //CheckAnd_expandSourceFiles(@nonvar, -nonvar)
//        //CheckAnd_expandSourceFiles(@list, -list)
//        %
//        //check if the source file names are valid (but not if the file exists)
//        //and return their absolute paths when using library notation or when
//        //they start with an environment variable (assumes environment variables
//        //use POSIX syntax in Prolog internal file paths)
//
//        CheckAnd_expandSourceFiles([File | Files], [Path | Paths]) :
//        -!, CheckAnd_expandSourceFile(File, Path), CheckAnd_expandSourceFiles(Files, Paths).
//
//                CheckAnd_expandSourceFiles([], []) :-!.
//
//        CheckAnd_expandSourceFiles(File, Path) :-CheckAnd_expandSourceFile(File, Path).
//
//
//                CheckAnd_expandSourceFile(File, Path) :-(atom(File) ->
//        prolog_osFile_name(NormalizedFile, File), (subAtom(NormalizedFile, 0, 1, _, '$') ->
//        expand_path(NormalizedFile, Path);
//        Path = NormalizedFile
//        )
//        ;
//        compound(File), File =.. [Library, Basename],
//        atom(Basename) ->
//        //library notation
//        prolog_osFile_name(NormalizedBasename, Basename), (expandLibraryAlias(Library, Directory) ->
//        atomConcat(Directory, NormalizedBasename, Path);
//        throw (error(existence_error(library, Library), _))
//        )
//        ;	%invalid source file specification ground(File) ->
//        throw (error(type_error(sourceFile_name, File), _))
//                ;
//        throw (error(instantiation_error, _))
//        ).
//
//
//        //expandLibraryAlias(+atom, -atom)
//        %
//        //converts a library alias into its corresponding path; uses a depth
//        //bound to prevent loops (inspired by similar code in SWI-Prolog)
//
//        expandLibraryAlias(Library, Path) :-expandLibraryAlias(Library, Path0, 16),
//                //expand the library path into an absolute path as it may
//                //contain environment variables that need to be expanded
//                expand_path(Path0, Path1),
//                //make sure that the library path ends with a slash
//                (subAtom(Path1, _, 1, 0, '/') ->
//        Path = Path1;
//        atomConcat(Path1, '/', Path)
//        ).
//
//
//        expandLibraryAlias(Library, Path, Depth) :
//        -logtalkLibrary_path(Library, Location), !, (compound(Location), Location =.. [Prefix, Directory],
//        atom(Directory) ->
//        //assume library notation (a compound term)
//        Depth > 0, NewDepth is Depth -1, expandLibraryAlias(Prefix, PrefixPath0, NewDepth),
//                //make sure that the prefix path ends with a slash
//                (subAtom(PrefixPath0, _, 1, 0, '/') ->
//        atomConcat(PrefixPath0, Directory, Path);
//        atomConcat(PrefixPath0, '/', PrefixPath1), atomConcat(PrefixPath1, Directory, Path)
//        )
//        ;
//        atom(Location) ->
//        //assume the final component of the library path
//        Path = Location;
//        ground(Location) ->
//        throw (error(type_error(library_path, Location), _))
//                ;
//        throw (error(instantiation_error, _))
//        ).
//
//
//        //CheckCompilerFlags(@list)
//        %
//        //checks if the compiler flags are valid
//
//        CheckCompilerFlags([Flag | Flags]) :-!, (var(Flag) ->
//        throw (error(instantiation_error, _))
//                ;
//        Flag =.. [Name, Value] ->
//        Check(read_writeFlag, Name, _), Check(flag_value, Name + Value, _);	%invalid flag syntax compound ( Flag ) ->
//        throw (error(domain_error(compilerFlag, Flag), _))
//                ;
//        throw (error(type_error(compound, Flag), _))
//        ),
//        CheckCompilerFlags(Flags).
//
//                CheckCompilerFlags([]) :-!.
//
//        CheckCompilerFlags(Flags) :- throw (error(type_error(list, Flags), _)).
//
//
//                //SetCompilerFlags(@list)
//                %
//                //sets the compiler flags
//
//                SetCompilerFlags(Flags) :-AssertCompilerFlags(Flags),
//                //only one of the optimize and debug flags can be turned on at the same time
//                (member(optimize(on), Flags) ->
//        retractall(ppFileCompilerFlag_(debug, _)), assertz(ppFileCompilerFlag_(debug, off));
//        member(debug(on), Flags) ->
//        retractall(ppFileCompilerFlag_(optimize, _)), assertz(ppFileCompilerFlag_(optimize, off));
//        true
//        ),
//        (ppFileCompilerFlag_(hook, HookEntity) ->
//        //pre-compile hooks in order to speed up entity compilation
//        (current_object(HookEntity) ->
//        CompCtx(Ctx, _, _, user, user, user, HookEntity, _,[], [],ExCtx, runtime, [],_),
//        executionContext(ExCtx, user, user, user, HookEntity,[], []),
//        CurrentFlag_(events, Events), Compile_message_to_object(term_expansion(Term, Terms), HookEntity, TermExpansionGoal, Events, Ctx), Compile_message_to_object(goal_expansion(Goal, ExpandedGoal), HookEntity, GoalExpansionGoal, Events, Ctx);
//        atom(HookEntity), prologFeature(modules, supported), current_module(HookEntity) ->
//        TermExpansionGoal = ':(HookEntity, term_expansion(Term, Terms)),
//        GoalExpansionGoal = ':(HookEntity, goal_expansion(Goal, ExpandedGoal)) ;
//        throw (error(existence_error(object, HookEntity), _))
//        ),
//        assertz((pp_hook_term_expansion_(Term, Terms) :-
//        catch(TermExpansionGoal, Error, term_expansion_error(HookEntity, Term, Error))
//        )),
//        assertz((pp_hook_goal_expansion_(Goal, ExpandedGoal) :-
//        catch(GoalExpansionGoal, Error, goal_expansion_error(HookEntity, Goal, Error))
//        ))
//        ;
//        true
//        ).
//
//
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

//        AssertCompilerFlags([Flag | Flags]) :-Flag =.. [Name, Value],
//        retractall(ppFileCompilerFlag_(Name, _)), assertz(ppFileCompilerFlag_(Name, Value)), AssertCompilerFlags(Flags).


    private
    void startRuntimeThreading () {


    }

    private
    void compileDefaultHooks () {

    }

    /**
     * loads all built-in entities if not already loaded (when embedding
     * Logtalk, the pre-compiled entities are loaded prior to this file)
     */
    private
    String loadBuiltInEntities () throws IOException, SourceCodeException {
        String scratchDir = getScratchDirectory();
        loadContext.reset();
        loadBuiltInEntity(EXPANDING, "expanding", scratchDir);
        loadBuiltInEntity(MONITORING, "monitoring", scratchDir);
        loadBuiltInEntity(FORWARDING, "forwarding", scratchDir);
        loadBuiltInEntity(USER, "user", scratchDir);
        loadBuiltInEntity(LOGTALK, "logtalk", scratchDir);
        loadBuiltInEntity(CORE_MESSAGES, "core_messages", scratchDir);
        loadBuiltInEntity(ENUM, "enum", scratchDir);

        return scratchDir;
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
//    private
//    void printMessage ( Functor kind, Atom component, Atom message ){
////        if (getCompilerFlag(FlagKey.REPORTS) != FlagValue.OFF) {
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
    void setCompilerObserver ( LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer ) {
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

    //    @Override
    public
    LogicCompiler <Clause, Clause, Clause> getPreCompiler () {
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

    /**
     * @param tokenSource
     * @param loadContext
     */
    Term[] compileTokenSource ( TokenSource tokenSource, LoadContext loadContext ) throws SourceCodeException {
        Term term = parser.term();
        List <HtEntityIdentifier> pipeLine = hooksPipeline();
        return preCompiler.compile(tokenSource, loadContext);
    }

    private
    List <HtEntityIdentifier> hooksPipeline () {
        Map <Functor, INameable <Functor>>[] tables = bkt.getTables();
        List <HtEntityIdentifier> l = new ArrayList <>();

        return l;
    }

    /**
     *
     */
    @Override
    public
    void start () throws IOException, SourceCodeException {
        initialize();
        compileTokenSource(tokenSource, loadContext);
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
        String version = "v0.0.1b#";
        int build = 0;
        String copyright = "(c) Anton Danilov, 2018-2019, All rights reserved";
        System.out.printf("\n%s, %s%d, %s.\n", product, version, build, copyright);
    }

    public
    TokenSource getTokenSource () {
        return tokenSource;
    }

    public
    void setTokenSource ( TokenSource tokenSource ) {
        this.tokenSource = tokenSource;
    }

    public
    void setParser ( HtPrologParser parser ) {
        this.parser = parser;
    }
}

/**
 *
 */
class ClauseChainObserver implements LogicCompilerObserver <Clause, Clause> {
    private HiTalkInstructionCompiler instructionCompiler;

    /**
     * {@inheritDoc}
     */
    public
    void onCompilation ( Sentence <Clause> sentence ) throws SourceCodeException {
        instructionCompiler.compile(sentence);
    }

    /**
     * {@inheritDoc}
     */
    public
    void onQueryCompilation ( Sentence <Clause> sentence ) throws SourceCodeException {
        instructionCompiler.compile(sentence);
    }
}

/**
 * ChainedCompilerObserver implements the compiler observer for this resolution engine. Compiled programs are added
 * to the resolvers domain. Compiled queries are executed.
 * <p>
 * <p/>If a chained observer is set up, all compiler outputs are forwarded onto it.
 */
//private
class ChainedCompilerObserver implements LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {
    /**
     * Holds the chained observer for compiler outputs.
     */
    private LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer;
    private Resolver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> resolver;

    /**
     * Sets the chained observer for compiler outputs.
     *
     * @param observer The chained observer.
     */
    public
    void setCompilerObserver ( LogicCompilerObserver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> observer ) {
        this.observer = observer;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    void onCompilation ( Sentence <HiTalkWAMCompiledPredicate> sentence ) throws SourceCodeException {
        if (observer != null) {
            observer.onCompilation(sentence);
        }

        getResolver().addToDomain(sentence.getT());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public
    void onQueryCompilation ( Sentence <HiTalkWAMCompiledQuery> sentence ) throws SourceCodeException {
        if (observer != null) {
            observer.onQueryCompilation(sentence);
        }

        getResolver().setQuery(sentence.getT());
    }

    public
    Resolver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> getResolver () {
        return resolver;
    }

    public
    void setResolver ( Resolver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> resolver ) {
        this.resolver = resolver;
    }
}

/*
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

   //top-level interpreter message sending calls

        Obj::Pred :-
        var(Obj),
        executionContext(ExCtx, user, user, user, Obj, [], []),
        throw(error(instantiation_error, logtalk(Obj::Pred, ExCtx))).

        {Obj}::Pred :-
        !,
   //use current default value of the "events" flag
        CurrentFlag_(events, Events),
        CompCtx(Ctx, _, _, user, user, user, Obj, _, [], [], ExCtx, runtime, [], _),
        executionContext(ExCtx, user, user, user, Obj, [], []),
        catch(
        Compile_message_to_object(Pred, {Obj}, Call, Events, Ctx),
        Error,
        Runtime_error_handler(error(Error, logtalk({Obj}::Pred, ExCtx)))
        ),
        (	nonvar(Obj),
        Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        catch(Debug(top_goal({Obj}::Pred, Call), ExCtx), Error, Runtime_error_handler(Error))
        ;	% object not compiled in debug mode or non-existing object or invalid object identifier
        catch(Call, Error, Runtime_error_handler(Error))
        ).

        Obj::Pred :-
   //use current default value of the "events" flag
        CurrentFlag_(events, Events),
        CompCtx(Ctx, _, _, user, user, user, Obj, _, [], [], ExCtx, runtime, [], _),
        executionContext(ExCtx, user, user, user, Obj, [], []),
        catch(
        Compile_message_to_object(Pred, Obj, Call, Events, Ctx),
        Error,
        Runtime_error_handler(error(Error, logtalk(Obj::Pred, ExCtx)))
        ),
        (	Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        catch(Debug(top_goal(Obj::Pred, Call), ExCtx), Error, Runtime_error_handler(Error))
        ;	% object not compiled in debug mode or non-existing object
        catch(Call, Error, Runtime_error_handler(Error))
        ).



   //top-level interpreter context-switch calls (debugging control construct)

        Obj<<Goal :-
        var(Obj),
        executionContext(ExCtx, user, user, user, Obj, [], []),
        throw(error(instantiation_error, logtalk(Obj<<Goal, ExCtx))).

        {Obj}<<Goal :-
        !,
        executionContext(ExCtx, user, user, user, Obj, [], []),
        catch(
        CompileContextSwitchCall({Obj}, Goal, Call, ExCtx),
        Error,
        Runtime_error_handler(error(Error, logtalk({Obj}<<Goal, ExCtx)))
        ),
        (	nonvar(Obj),
        Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        catch(Debug(top_goal({Obj}<<Goal, Call), ExCtx), Error, Runtime_error_handler(Error))
        ;	% object not compiled in debug mode or non-existing object or invalid object identifier
        catch(Call, Error, Runtime_error_handler(Error))
        ).

        Obj<<Goal :-
        executionContext(ExCtx, user, user, user, Obj, [], []),
        catch(
        CompileContextSwitchCall(Obj, Goal, Call, ExCtx),
        Error,
        Runtime_error_handler(error(Error, logtalk(Obj<<Goal, ExCtx)))
        ),
        (	Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        catch(Debug(top_goal(Obj<<Goal, Call), ExCtx), Error, Runtime_error_handler(Error))
        ;	% object not compiled in debug mode or non-existing object
        catch(Call, Error, Runtime_error_handler(Error))
        ).



   //Runtime_error_handler(@term)
        %
   //top-level runtime error handler
        %
   //it tries to decode internal predicate names and deal with variations of
   //Prolog error handling due to the lack of standardization by calling an
   //adapter file predicate to try to normalize the error terms

        Runtime_error_handler(Variable) :-
        var(Variable),
        throw(error(instantiation_error, logtalk(throw(_), _))).

        Runtime_error_handler(logtalkDebuggerAborted) :-
        !,
        print_message(comment(debugging), debugger, logtalkDebuggerAborted).

        Runtime_error_handler(error(Variable, Context)) :-
        var(Variable),
        throw(error(instantiation_error, logtalk(throw(_), Context))).

        Runtime_error_handler(error(error(Error, _), Context)) :-
        !,
        Runtime_error_handler(error(Error, Context)).

        Runtime_error_handler(error(existence_error(thread,Queue), TContext)) :-
        Runtime_thread_error_handler_helper(TContext, Context),
        throw(error(existence_error(thread,Queue), Context)).

        Runtime_error_handler(Error) :-
        (	normalize_error_term(Error, NormalizedError) ->
        Runtime_normalized_error_handler(NormalizedError)
        ;	Runtime_normalized_error_handler(Error)
        ).

        Runtime_normalized_error_handler(error(existence_error(procedure, ':(Module,PI)), Context)) :-
   //assuming we're running with a backend compiler supporting modules,
   //check that the error is the context of the module where Logtalk is loaded
        atom(Module),
        user_module_qualification(xx, ':(Module,xx)),
        Runtime_normalized_error_handler(error(existence_error(procedure, PI), Context)).

        Runtime_normalized_error_handler(error(existence_error(procedure, TFunctor/6), _)) :-
        (	atomConcat(Prefix, 'Idcl', TFunctor) ->
        true
        ;	atomConcat(Prefix, 'Dcl', TFunctor)
        ),
        prefix_to_entity(Prefix, Obj),
        (	InstantiatesClass_(_, Obj, _)
        ;	SpecializesClass_(_, Obj, _)
        ;	extends_object_(_, Obj, _)
        ;	Complemented_object_(Obj, _, _, _, _)
        ),
        \+ Current_object_(Obj, _, _, _, _, _, _, _, _, _, _),
        throw(error(existence_error(object, Obj), logtalk(_, _))).

        Runtime_normalized_error_handler(error(existence_error(procedure, TFunctor/5), _)) :-
        atomConcat(Prefix, 'Dcl', TFunctor),
        prefix_to_entity(Prefix, CtgOrPtc),
        (	Implements_protocol_(_, CtgOrPtc, _), \+ Current_protocol_(CtgOrPtc, _, _, _, _),
        throw(error(existence_error(protocol, CtgOrPtc), logtalk(_, _)))
        ;	extends_protocol_(_, CtgOrPtc, _), \+ Current_protocol_(CtgOrPtc, _, _, _, _),
        throw(error(existence_error(protocol, CtgOrPtc), logtalk(_, _)))
        ;	ImportsCategory_(_, CtgOrPtc, _), \+ CurrentCategory_(CtgOrPtc, _, _, _, _, _),
        throw(error(existence_error(category, CtgOrPtc), logtalk(_, _)))
        ;	extendsCategory_(_, CtgOrPtc, _), \+ CurrentCategory_(CtgOrPtc, _, _, _, _, _),
        throw(error(existence_error(category, CtgOrPtc), logtalk(_, _)))
        ).

        Runtime_normalized_error_handler(error(existence_error(procedure, TFunctor/TArity), logtalk(Goal, ExCtx))) :-
        Decompile_predicateIndicators(TFunctor/TArity, _, _, Functor/Arity),
        throw(error(existence_error(procedure, Functor/Arity), logtalk(Goal, ExCtx))).

        Runtime_normalized_error_handler(Error) :-
        throw(Error).


        Runtime_thread_error_handler_helper(logtalk(threaded_exit(TGoal),ExCtx), logtalk(threaded_exit(Goal),ExCtx)) :-
        Runtime_thread_error_tgoal_goal(TGoal, Goal).

        Runtime_thread_error_handler_helper(logtalk(threaded_exit(TGoal,Tag),ExCtx), logtalk(threaded_exit(Goal,Tag),ExCtx)) :-
        Runtime_thread_error_tgoal_goal(TGoal, Goal).

        Runtime_thread_error_handler_helper(Context, Context).


        Runtime_thread_error_tgoal_goal(Send_to_obj_ne_nv(Self,Goal0,_), Goal) :-
        (	Self == user ->
        Goal = Goal0
        ;	Goal = Self::Goal0
        ).

        Runtime_thread_error_tgoal_goal(Send_to_obj_nv(Self,Goal0,_), Goal) :-
        (	Self == user ->
        Goal = Goal0
        ;	Goal = Self::Goal0
        ).

        Runtime_thread_error_tgoal_goal(TGoal, Goal) :-
        functor(TGoal, TFunctor, TArity),
        Decompile_predicateIndicators(TFunctor/TArity, _, _, Functor/Arity),
        functor(Goal, Functor, Arity),
        unify_head_theadArguments(Goal, TGoal, _).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // built-in predicates
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //current_object(?objectIdentifier)

        current_object(Obj) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Current_object(Obj, ExCtx).


        Current_object(Obj, ExCtx) :-
        Check(var_or_objectIdentifier, Obj, logtalk(current_object(Obj), ExCtx)),
        Current_object_(Obj, _, _, _, _, _, _, _, _, _, _).



   //current_protocol(?protocolIdentifier)

        current_protocol(Ptc) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Current_protocol(Ptc, ExCtx).


        Current_protocol(Ptc, ExCtx) :-
        Check(var_or_protocolIdentifier, Ptc, logtalk(current_protocol(Ptc), ExCtx)),
        Current_protocol_(Ptc, _, _, _, _).



   //currentCategory(?categoryIdentifier)

        currentCategory(Ctg) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        CurrentCategory(Ctg, ExCtx).


        CurrentCategory(Ctg, ExCtx) :-
        Check(var_orCategoryIdentifier, Ctg, logtalk(currentCategory(Ctg), ExCtx)),
        CurrentCategory_(Ctg, _, _, _, _, _).



   //object_property(?objectIdentifier, ?object_property)
        %
   //the implementation ensures that no spurious choice-points are
   //created when the predicate is called with a bound property argument

        object_property(Obj, Prop) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        object_property(Obj, Prop, ExCtx).


        object_property(Obj, Prop, ExCtx) :-
        Check(var_or_objectIdentifier, Obj, logtalk(object_property(Obj, Prop), ExCtx)),
        Check(var_or_object_property, Prop, logtalk(object_property(Obj, Prop), ExCtx)),
        Current_object_(Obj, _, Dcl, Def, _, _, _, DDcl, DDef, Rnm, Flags),
        object_property(Prop, Obj, Dcl, Def, DDcl, DDef, Rnm, Flags).


        object_property(module, _, _, _, _, _, _, Flags) :-
        Flags /\ 1024 =:= 1024.
        object_property(debugging, _, _, _, _, _, _, Flags) :-
        Flags /\ 512 =:= 512.
        object_property(contextSwitchingCalls, _, _, _, _, _, _, Flags) :-
        Flags /\ 256 =:= 256.
        object_property(dynamicDeclarations, _, _, _, _, _, _, Flags) :-
        Flags /\ 128 =:= 128.
        object_property(complements(Complements), _, _, _, _, _, _, Flags) :-
        (	Flags /\ 64 =:= 64 ->
        Complements = allow
        ;	Flags /\ 32 =:= 32,
        Complements = restrict
        ).
        object_property(complements, _, _, _, _, _, _, Flags) :-
   //deprecated Logtalk 2.x object property
        (	Flags /\ 64 =:= 64 ->
        true
        ;	Flags /\ 32 =:= 32
        ).
        object_property(events, Obj, _, _, _, _, _, Flags) :-
        (	Obj == user ->
   //depends on the current default value of the flag
        CurrentFlag_(events, allow)
        ;	% fixed value (at compilation time) for all other objects
        Flags /\ 16 =:= 16
        ).
        object_property(sourceData, _, _, _, _, _, _, Flags) :-
        Flags /\ 8 =:= 8.
        object_property(threaded, _, _, _, _, _, _, Flags) :-
        Flags /\ 4 =:= 4.
        object_property((dynamic), _, _, _, _, _, _, Flags) :-
        Flags /\ 2 =:= 2.
        object_property(static, _, _, _, _, _, _, Flags) :-
        Flags /\ 2 =:= 0.
        object_property(builtIn, _, _, _, _, _, _, Flags) :-
        Flags /\ 1 =:= 1.
        object_property(file(Path), Obj, _, _, _, _, _, _) :-
        (	entity_property_(Obj, fileLines(Basename, Directory, _, _)) ->
        atomConcat(Directory, Basename, Path)
        ;	fail
        ).
        object_property(file(Basename, Directory), Obj, _, _, _, _, _, _) :-
        (	entity_property_(Obj, fileLines(Basename, Directory, _, _)) ->
        true
        ;	fail
        ).
        object_property(lines(Start, End), Obj, _, _, _, _, _, _) :-
        (	entity_property_(Obj, fileLines(_, _, Start, End)) ->
        true
        ;	fail
        ).
        object_property(info(Info), Obj, _, _, _, _, _, _) :-
        (	entity_property_(Obj, info(Info)) ->
        true
        ;	fail
        ).
        object_property(public(Resources), Obj, Dcl, _, DDcl, _, _, Flags) :-
        object_propertyResources(Obj, Dcl, DDcl, Flags, p(p(p)), Resources).
        object_property(protected(Resources), Obj, Dcl, _, DDcl, _, _, Flags) :-
        object_propertyResources(Obj, Dcl, DDcl, Flags, p(p), Resources).
        object_property(private(Resources), Obj, Dcl, _, DDcl, _, _, Flags) :-
        object_propertyResources(Obj, Dcl, DDcl, Flags, p, Resources).
        object_property(declares(Predicate, Properties), Obj, Dcl, _, DDcl, _, _, Flags) :-
        object_propertyDeclares(Obj, Dcl, DDcl, Flags, Predicate, Properties).
        object_property(defines(Predicate, Properties), Obj, _, Def, _, DDef, _, Flags) :-
        object_propertyDefines(Obj, Def, DDef, Predicate, Flags, Properties).
        object_property(includes(Predicate, From, Properties), Obj, _, _, _, _, _, _) :-
        entity_propertyIncludes(Obj, Predicate, From, Properties).
        object_property(provides(Predicate, To, Properties), Obj, _, _, _, _, _, _) :-
        entity_property_provides(Obj, Predicate, To, Properties).
        object_property(alias(Alias, Properties), Obj, _, _, _, _, Rnm, Flags) :-
        entity_propertyAlias(Obj, Rnm, Flags, Alias, Properties).
        object_property(calls(Predicate, Properties), Obj, _, _, _, _, _, _) :-
        entity_propertyCalls(Obj, Predicate, Properties).
        object_property(updates(Predicate, Properties), Obj, _, _, _, _, _, _) :-
        entity_property_updates(Obj, Predicate, Properties).
        object_property(number_ofClauses(Total), Obj, _, _, _, _, _, _) :-
        (	entity_property_(Obj, number_ofClauses(Total, _)) ->
        true
        ;	fail
        ).
        object_property(number_ofRules(Total), Obj, _, _, _, _, _, _) :-
        (	entity_property_(Obj, number_ofRules(Total, _)) ->
        true
        ;	fail
        ).
        object_property(number_of_userClauses(TotalUser), Obj, _, _, _, _, _, _) :-
        (	entity_property_(Obj, number_ofClauses(_, TotalUser)) ->
        true
        ;	fail
        ).
        object_property(number_of_userRules(TotalUser), Obj, _, _, _, _, _, _) :-
        (	entity_property_(Obj, number_ofRules(_, TotalUser)) ->
        true
        ;	fail
        ).


        object_propertyResources(Obj, Dcl, DDcl, Flags, Scope, Resources) :-
   //the caller uses this predicate to group object resources by scope
        findall(
        Resource,
        object_propertyResource(Obj, Dcl, DDcl, Flags, Scope, Resource),
        Resources
        ).


        object_propertyResource(_, Dcl, _, _, Scope, Functor/Arity) :-
        call(Dcl, Predicate, Scope, _, _),
        functor(Predicate, Functor, Arity).

        object_propertyResource(_, _, DDcl, Flags, Scope, Functor/Arity) :-
        Flags /\ 128 =:= 128,
   //dynamic predicate declarations are allowed
        call(DDcl, Predicate, Scope),
        functor(Predicate, Functor, Arity).

        object_propertyResource(Obj, _, _, _, Scope, op(Priority, Specifier, Operator)) :-
        entity_property_(Obj, op(Priority, Specifier, Operator, Scope)).



   //category_property(?categoryIdentifier, ?category_property)
        %
   //the implementation ensures that no spurious choice-points are
   //created when the predicate is called with a bound property argument

        category_property(Ctg, Prop) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Category_property(Ctg, Prop, ExCtx).


        Category_property(Ctg, Prop, ExCtx) :-
        Check(var_orCategoryIdentifier, Ctg, logtalk(category_property(Ctg, Prop), ExCtx)),
        Check(var_orCategory_property, Prop, logtalk(category_property(Ctg, Prop), ExCtx)),
        CurrentCategory_(Ctg, _, Dcl, Def, Rnm, Flags),
        Category_property(Prop, Ctg, Dcl, Def, Rnm, Flags).


        Category_property(debugging, _, _, _, _, Flags) :-
        Flags /\ 512 =:= 512.
        Category_property(events, _, _, _, _, Flags) :-
        Flags /\ 16 =:= 16.
        Category_property(sourceData, _, _, _, _, Flags) :-
        Flags /\ 8 =:= 8.
        Category_property((dynamic), _, _, _, _, Flags) :-
        Flags /\ 2 =:= 2.
        Category_property(static, _, _, _, _, Flags) :-
        Flags /\ 2 =:= 0.
        Category_property(builtIn, _, _, _, _, Flags) :-
        Flags /\ 1 =:= 1.
        Category_property(file(Path), Ctg, _, _, _, _) :-
        (	entity_property_(Ctg, fileLines(Basename, Directory, _, _)) ->
        atomConcat(Directory, Basename, Path)
        ;	fail
        ).
        Category_property(file(Basename, Directory), Ctg, _, _, _, _) :-
        (	entity_property_(Ctg, fileLines(Basename, Directory, _, _)) ->
        true
        ;	fail
        ).
        Category_property(lines(Start, End), Ctg, _, _, _, _) :-
        (	entity_property_(Ctg, fileLines(_, _, Start, End)) ->
        true
        ;	fail
        ).
        Category_property(info(Info), Ctg, _, _, _, _) :-
        (	entity_property_(Ctg, info(Info)) ->
        true
        ;	fail
        ).
        Category_property(public(Resources), Ctg, Dcl, _, _, Flags) :-
        Category_propertyResources(Ctg, Dcl, Flags, p(p(p)), Resources).
        Category_property(protected(Resources), Ctg, Dcl, _, _, Flags) :-
        Category_propertyResources(Ctg, Dcl, Flags, p(p), Resources).
        Category_property(private(Resources), Ctg, Dcl, _, _, Flags) :-
        Category_propertyResources(Ctg, Dcl, Flags, p, Resources).
        Category_property(declares(Predicate, Properties), Ctg, Dcl, _, _, _) :-
        Category_propertyDeclares(Ctg, Dcl, Predicate, Properties).
        Category_property(defines(Predicate, Properties), Ctg, _, Def, _, Flags) :-
        Category_propertyDefines(Ctg, Def, Predicate, Flags, Properties).
        Category_property(includes(Predicate, From, Properties), Ctg, _, _, _, _) :-
        entity_propertyIncludes(Ctg, Predicate, From, Properties).
        Category_property(provides(Predicate, To, Properties), Ctg, _, _, _, _) :-
        entity_property_provides(Ctg, Predicate, To, Properties).
        Category_property(calls(Predicate, Properties), Ctg, _, _, _, _) :-
        entity_propertyCalls(Ctg, Predicate, Properties).
        Category_property(updates(Predicate, Properties), Ctg, _, _, _, _) :-
        entity_property_updates(Ctg, Predicate, Properties).
        Category_property(alias(Alias, Properties), Ctg, _, _, Rnm, Flags) :-
        entity_propertyAlias(Ctg, Rnm, Flags, Alias, Properties).
        Category_property(number_ofClauses(Total), Ctg, _, _, _, _) :-
        (	entity_property_(Ctg, number_ofClauses(Total, _)) ->
        true
        ;	fail
        ).
        Category_property(number_ofRules(Total), Ctg, _, _, _, _) :-
        (	entity_property_(Ctg, number_ofRules(Total, _)) ->
        true
        ;	fail
        ).
        Category_property(number_of_userClauses(TotalUser), Ctg, _, _, _, _) :-
        (	entity_property_(Ctg, number_ofClauses(_, TotalUser)) ->
        true
        ;	fail
        ).
        Category_property(number_of_userRules(TotalUser), Ctg, _, _, _, _) :-
        (	entity_property_(Ctg, number_ofRules(_, TotalUser)) ->
        true
        ;	fail
        ).


        Category_propertyResources(Ctg, Dcl, Flags, Scope, Resources) :-
   //the caller uses this predicate to group object resources by scope
        findall(
        Resource,
        Category_propertyResource(Ctg, Dcl, Flags, Scope, Resource),
        Resources
        ).


        Category_propertyResource(Ctg, Dcl, _, Scope, Functor/Arity) :-
        call(Dcl, Predicate, Scope, _, _, Ctg),
        functor(Predicate, Functor, Arity).

        Category_propertyResource(Ctg, _, _, Scope, op(Priority, Specifier, Operator)) :-
        entity_property_(Ctg, op(Priority, Specifier, Operator, Scope)).


   //protocol_property(?protocolIdentifier, ?protocol_property)
        %
   //the implementation ensures that no spurious choice-points are
   //created when the predicate is called with a bound property argument

        protocol_property(Ptc, Prop) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        protocol_property(Ptc, Prop, ExCtx).


        protocol_property(Ptc, Prop, ExCtx) :-
        Check(var_or_protocolIdentifier, Ptc, logtalk(protocol_property(Ptc, Prop), ExCtx)),
        Check(var_or_protocol_property, Prop, logtalk(protocol_property(Ptc, Prop), ExCtx)),
        Current_protocol_(Ptc, _, Dcl, Rnm, Flags),
        protocol_property(Prop, Ptc, Dcl, Rnm, Flags).


        protocol_property(debugging, _, _, _, Flags) :-
        Flags /\ 512 =:= 512.
        protocol_property(sourceData, _, _, _, Flags) :-
        Flags /\ 8 =:= 8.
        protocol_property((dynamic), _, _, _, Flags) :-
        Flags /\ 2 =:= 2.
        protocol_property(static, _, _, _, Flags) :-
        Flags /\ 2 =:= 0.
        protocol_property(builtIn, _, _, _, Flags) :-
        Flags /\ 1 =:= 1.
        protocol_property(file(Path), Ptc, _, _, _) :-
        (	entity_property_(Ptc, fileLines(Basename, Directory, _, _)) ->
        atomConcat(Directory, Basename, Path)
        ;	fail
        ).
        protocol_property(file(Basename, Directory), Ptc, _, _, _) :-
        (	entity_property_(Ptc, fileLines(Basename, Directory, _, _)) ->
        true
        ;	fail
        ).
        protocol_property(lines(Start, End), Ptc, _, _, _) :-
        (	entity_property_(Ptc, fileLines(_, _, Start, End)) ->
        true
        ;	fail
        ).
        protocol_property(info(Info), Ptc, _, _, _) :-
        (	entity_property_(Ptc, info(Info)) ->
        true
        ;	fail
        ).
        protocol_property(public(Resources), Ptc, Dcl, _, Flags) :-
        protocol_propertyResources(Ptc, Dcl, Flags, p(p(p)), Resources).
        protocol_property(protected(Resources), Ptc, Dcl, _, Flags) :-
        protocol_propertyResources(Ptc, Dcl, Flags, p(p), Resources).
        protocol_property(private(Resources), Ptc, Dcl, _, Flags) :-
        protocol_propertyResources(Ptc, Dcl, Flags, p, Resources).
        protocol_property(declares(Predicate, Properties), Ptc, Dcl, _, _) :-
        protocol_propertyDeclares(Ptc, Dcl, Predicate, Properties).
        protocol_property(alias(Alias, Properties), Ptc, _, Rnm, Flags) :-
        entity_propertyAlias(Ptc, Rnm, Flags, Alias, Properties).


        protocol_propertyResources(Ptc, Dcl, Flags, Scope, Resources) :-
   //the caller uses this predicate to group object resources by scope
        findall(
        Resource,
        protocol_propertyResource(Ptc, Dcl, Flags, Scope, Resource),
        Resources
        ).


        protocol_propertyResource(Ptc, Dcl, _, Scope, Functor/Arity) :-
        call(Dcl, Predicate, Scope, _, _, Ptc),
        functor(Predicate, Functor, Arity).

        protocol_propertyResource(Ptc, _, _, Scope, op(Priority, Specifier, Operator)) :-
        entity_property_(Ptc, op(Priority, Specifier, Operator, Scope)).


        object_propertyDeclares(Obj, Dcl, DDcl, EntityFlags, Functor/Arity, Properties) :-
        (	call(Dcl, Predicate, Scope, Meta, Flags)
        ;	EntityFlags /\ 128 =:= 128,
   //dynamic predicate declarations are allowed
        call(DDcl, Predicate, Scope),
        Meta = no,
        Flags = 2
        ),
        functor(Predicate, Functor, Arity),
        Scope(ScopeAsAtom, Scope),
        entity_propertyDeclares(Obj, Functor/Arity, ScopeAsAtom, Meta, Flags, Properties).


        Category_propertyDeclares(Ctg, Dcl, Functor/Arity, Properties) :-
        call(Dcl, Predicate, Scope, Meta, Flags, Ctg),
        functor(Predicate, Functor, Arity),
        Scope(ScopeAsAtom, Scope),
        entity_propertyDeclares(Ctg, Functor/Arity, ScopeAsAtom, Meta, Flags, Properties).


        protocol_propertyDeclares(Ptc, Dcl, Functor/Arity, Properties) :-
        call(Dcl, Predicate, Scope, Meta, Flags, Ptc),
        functor(Predicate, Functor, Arity),
        Scope(ScopeAsAtom, Scope),
        entity_propertyDeclares(Ptc, Functor/Arity, ScopeAsAtom, Meta, Flags, Properties).


        entity_propertyDeclares(Entity, Functor/Arity, Scope, Meta, Flags, Properties) :-
        (	predicate_property_(Entity, Functor/Arity, info(Info)) ->
        Properties0 = [info(Info)]
        ;	Properties0 = []
        ),
        findall(mode(Mode, Solutions), predicate_property_(Entity, Functor/Arity, mode(Mode, Solutions)), Modes),
        Append(Modes, Properties0, Properties1),
        (	predicate_property_(Entity, Functor/Arity, declarationLocation(Location)) ->
        (	Location = File-Line ->
        Properties2 = [include(File), lineCount(Line)| Properties1]
        ;	Properties2 = [lineCount(Location)| Properties1]
        )
        ;	Properties2 = Properties1
        ),
        (	%Flags /\ 64 =:= 64,
        Meta == no ->
        Properties7 = Properties6
        ;	Properties7 = [meta_predicate(Meta)| Properties6]
        ),
        (	Flags /\ 32 =:= 32,
        predicate_property_(Entity, Functor/Arity, coinductive(Template)) ->
        Properties3 = [coinductive(Template)| Properties2]
        ;	Properties3 = Properties2
        ),
        (	Flags /\ 16 =:= 16 ->
        Properties4 = [(multifile)| Properties3]
        ;	Properties4 = Properties3
        ),
        (	Flags /\ 8 =:= 8 ->
        Arity2 is Arity - 2,
        Properties5 = [non_terminal(Functor//Arity2)| Properties4]
        ;	Properties5 = Properties4
        ),
        (	Flags /\ 4 =:= 4 ->
        Properties6 = [synchronized| Properties5]
        ;	Properties6 = Properties5
        ),
        (	Flags /\ 2 =:= 2 ->
        Properties = [Scope, scope(Scope), (dynamic)| Properties7]
        ;	Properties = [Scope, scope(Scope), static| Properties7]
        ).


        object_propertyDefines(Obj, Def, DDef, Functor/Arity, Flags, Properties) :-
        (	call(Def, Predicate, _, _)
        ;	call(DDef, Predicate, _, _)
        ),
        functor(Predicate, Functor, Arity),
        entity_propertyDefines(Obj, Functor/Arity, Flags, Properties).


        Category_propertyDefines(Ctg, Def, Functor/Arity, Flags, Properties) :-
        call(Def, Predicate, _, _, Ctg),
        functor(Predicate, Functor, Arity),
        entity_propertyDefines(Ctg, Functor/Arity, Flags, Properties).


        entity_propertyDefines(Entity, Functor/Arity, _, Properties) :-
        predicate_property_(Entity, Functor/Arity, flagsClausesRulesLocation(Flags, Clauses, Rules, Location)),
        !,
        (	Location = File-Line ->
        Properties0 = [include(File), lineCount(Line), number_ofClauses(Clauses), number_ofRules(Rules)]
        ;	Location =:= 0 ->
   //auxiliary predicate
        Properties0 = [number_ofClauses(Clauses), number_ofRules(Rules)]
        ;	Properties0 = [lineCount(Location), number_ofClauses(Clauses), number_ofRules(Rules)]
        ),
        (	Flags /\ 4 =:= 4 ->
        Properties1 = [inline| Properties0]
        ;	Properties1 = Properties0
        ),
        (	Flags /\ 2 =:= 2 ->
        Arity2 is Arity - 2,
        Properties2 = [non_terminal(Functor//Arity2)| Properties1]
        ;	Properties2 = Properties1
        ),
        (	Flags /\ 1 =:= 1 ->
        Properties = [auxiliary| Properties2]
        ;	Properties = Properties2
        ).
   //likely a dynamic or a multifile predicate with no local clauses
        entity_propertyDefines(_, _, Flags, [number_ofClauses(0), number_ofRules(0)]) :-
        Flags /\ 2 =:= 0,
   //static entity
        !.

   //dynamically created entity
        entity_propertyDefines(_, _, _, []).


        entity_propertyIncludes(Entity, Functor/Arity, From, Properties) :-
        predicate_property_(Entity, Functor/Arity, clausesRulesLocationFrom(Clauses, Rules, Location, From)),
        (	Location = File-Line ->
        LocationProperties = [include(File), lineCount(Line)]
        ;	LocationProperties = [lineCount(Location)]
        ),
        Properties = [number_ofClauses(Clauses), number_ofRules(Rules)| LocationProperties].


        entity_property_provides(Entity, Functor/Arity, To, Properties) :-
        predicate_property_(To, Functor/Arity, clausesRulesLocationFrom(Clauses, Rules, Location, Entity)),
        (	Location = File-Line ->
        LocationProperties = [include(File), lineCount(Line)]
        ;	LocationProperties = [lineCount(Location)]
        ),
        Properties = [number_ofClauses(Clauses), number_ofRules(Rules)| LocationProperties].


        entity_propertyAlias(Entity, Rnm, Flags, AliasFunctor/Arity, Properties) :-
        (	Flags /\ 8 =:= 8 ->
   //entity compiled with the sourceData flag turned on
        entity_property_(Entity, alias(From, OriginalFunctor/Arity, AliasFunctor/Arity, NonTerminalFlag, Location)),
        (	Location = File-Line ->
        LocationProperties = [include(File), lineCount(Line)]
        ;	LocationProperties = [lineCount(Location)]
        ),
        (	NonTerminalFlag =:= 1 ->
        Arity2 is Arity - 2,
        Properties = [non_terminal(AliasFunctor//Arity2), for(OriginalFunctor/Arity), from(From)| LocationProperties]
        ;	Properties = [for(OriginalFunctor/Arity), from(From)| LocationProperties]
        )
        ;	% entity compiled with the sourceData flag turned off
        call(Rnm, From, Original, Alias),
        nonvar(From),
        functor(Original, OriginalFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        Properties = [for(OriginalFunctor/Arity), from(From)]
        ).


        entity_propertyCalls(Entity, Call, Properties) :-
        entity_property_(Entity, calls(Call, Caller, Alias, NonTerminal, Location)),
        (	NonTerminal == no ->
        NonTerminalProperty = []
        ;	NonTerminalProperty = [non_terminal(NonTerminal)]
        ),
        (	Location = File-Line ->
        LocationProperties = [include(File), lineCount(Line)| NonTerminalProperty]
        ;	LocationProperties = [lineCount(Location)| NonTerminalProperty]
        ),
        (	Alias == no ->
        OtherProperties = LocationProperties
        ;	OtherProperties = [alias(Alias)| LocationProperties]
        ),
        Properties = [caller(Caller)| OtherProperties].


        entity_property_updates(Entity, Predicate, Properties) :-
        entity_property_(Entity, updates(Predicate, Updater, Alias, NonTerminal, Location)),
        (	NonTerminal == no ->
        NonTerminalProperty = []
        ;	NonTerminalProperty = [non_terminal(NonTerminal)]
        ),
        (	Location = File-Line ->
        LocationProperties = [include(File), lineCount(Line)| NonTerminalProperty]
        ;	LocationProperties = [lineCount(Location)| NonTerminalProperty]
        ),
        (	Alias == no ->
        OtherProperties = LocationProperties
        ;	OtherProperties = [alias(Alias)| LocationProperties]
        ),
        Properties = [updater(Updater)| OtherProperties].



   //create_object(?objectIdentifier, +list, +list, +list)

        create_object(Obj, Relations, Directives, Clauses) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Create_object(Obj, Relations, Directives, Clauses, ExCtx).


        Create_object(Obj, Relations, Directives, Clauses, ExCtx) :-
        nonvar(Obj),
        (	\+ callable(Obj),
        throw(error(type_error(objectIdentifier, Obj), logtalk(create_object(Obj, Relations, Directives, Clauses), ExCtx)))
        ;	Current_object_(Obj, _, _, _, _, _, _, _, _, _, _),
        throw(error(permission_error(modify, object, Obj), logtalk(create_object(Obj, Relations, Directives, Clauses), ExCtx)))
        ;	CurrentCategory_(Obj, _, _, _, _, _),
        throw(error(permission_error(modify, category, Obj), logtalk(create_object(Obj, Relations, Directives, Clauses), ExCtx)))
        ;	Current_protocol_(Obj, _, _, _, _),
        throw(error(permission_error(modify, protocol, Obj), logtalk(create_object(Obj, Relations, Directives, Clauses), ExCtx)))
        ;	functor(Obj, '{}', 1),
        throw(error(permission_error(create, object, Obj), logtalk(create_object(Obj, Relations, Directives, Clauses), ExCtx)))
        ).

        Create_object(Obj, Relations, Directives, Clauses, ExCtx) :-
        Check(list, Relations, logtalk(create_object(Obj, Relations, Directives, Clauses), ExCtx)),
        Check(list, Directives, logtalk(create_object(Obj, Relations, Directives, Clauses), ExCtx)),
        Check(list, Clauses, logtalk(create_object(Obj, Relations, Directives, Clauses), ExCtx)),
        catch(
        Create_objectChecked(Obj, Relations, Directives, Clauses),
        Error,
        Create_entity_error_handler(Error, create_object(Obj, Relations, Directives, Clauses), ExCtx)
        ).


        Create_objectChecked(Obj, Relations, Directives, Clauses) :-
        (	var(Obj) ->
        generate_entityIdentifier(object, Obj)
        ;	true
        ),
   //set the initial compilation context for compiling the object directives and clauses
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, runtime, _, '-(-1, -1)),
   //we need to compile the object relations first as we need to know if we are compiling
   //a prototype or an instance/class when compiling the object identifier as the generated
   //internal functors are different for each case
        Compile_objectRelations(Relations, Obj, Ctx),
        Compile_objectIdentifier(Obj, Ctx),
        assertz(ppDynamic_'),
        CompileLogtalkDirectives(Directives, Ctx),
   //the list of clauses may also include grammar rules
        CompileRuntime_terms(Clauses),
        generateDef_tableClauses(Ctx),
        Compile_predicateCalls',
        generate_objectClauses',
        generate_objectDirectives',
        AssertDynamic_entity(object),
        Restore_global_operator_table',
        Clean_ppCcClauses',
        Clean_pp_objectClauses',
        Clean_ppRuntimeClauses'.



   //createCategory(?categoryIdentifier, +list, +list, +list)

        createCategory(Ctg, Relations, Directives, Clauses) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        CreateCategory(Ctg, Relations, Directives, Clauses, ExCtx).


        CreateCategory(Ctg, Relations, Directives, Clauses, ExCtx) :-
        nonvar(Ctg),
        (	\+ callable(Ctg),
        throw(error(type_error(categoryIdentifier, Ctg), logtalk(createCategory(Ctg, Relations, Directives, Clauses), ExCtx)))
        ;	CurrentCategory_(Ctg, _, _, _, _, _),
        throw(error(permission_error(modify, category, Ctg), logtalk(createCategory(Ctg, Relations, Directives, Clauses), ExCtx)))
        ;	Current_object_(Ctg, _, _, _, _, _, _, _, _, _, _),
        throw(error(permission_error(modify, object, Ctg), logtalk(createCategory(Ctg, Relations, Directives, Clauses), ExCtx)))
        ;	Current_protocol_(Ctg, _, _, _, _),
        throw(error(permission_error(modify, protocol, Ctg), logtalk(createCategory(Ctg, Relations, Directives, Clauses), ExCtx)))
        ).

        CreateCategory(Ctg, Relations, Directives, Clauses, ExCtx) :-
        Check(list, Relations, logtalk(createCategory(Ctg, Relations, Directives, Clauses), ExCtx)),
        Check(list, Directives, logtalk(createCategory(Ctg, Relations, Directives, Clauses), ExCtx)),
        Check(list, Clauses, logtalk(createCategory(Ctg, Relations, Directives, Clauses), ExCtx)),
        catch(
        CreateCategoryChecked(Ctg, Relations, Directives, Clauses),
        Error,
        Create_entity_error_handler(Error, createCategory(Ctg, Relations, Directives, Clauses), ExCtx)
        ).


        CreateCategoryChecked(Ctg, Relations, Directives, Clauses) :-
        (	var(Ctg) ->
        generate_entityIdentifier(category, Ctg)
        ;	true
        ),
   //set the initial compilation context for compiling the category directives and clauses
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, runtime, _, '-(-1, -1)),
        CompileCategoryIdentifier(Ctg, Ctx),
        CompileCategoryRelations(Relations, Ctg, Ctx),
        assertz(ppDynamic_'),
        CompileLogtalkDirectives(Directives, Ctx),
   //the list of clauses may also include grammar rules
        CompileRuntime_terms(Clauses),
        generateDef_tableClauses(Ctx),
        Compile_predicateCalls',
        generateCategoryClauses',
        generateCategoryDirectives',
        AssertDynamic_entity(category),
        Restore_global_operator_table',
        Clean_ppCcClauses',
        Clean_ppCategoryClauses',
        Clean_ppRuntimeClauses',
   //complementing categories can invalidate dynamic binding cache entries
        (	member(Relation, Relations),
        functor(Relation, complements, _) ->
        CleanLookupCaches'
        ;	true
        ).



   //create_protocol(?protocolIdentifier, +list, +list)

        create_protocol(Ptc, Relations, Directives) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Create_protocol(Ptc, Relations, Directives, ExCtx).


        Create_protocol(Ptc, Relations, Directives, ExCtx) :-
        nonvar(Ptc),
        (	\+ atom(Ptc),
        throw(error(type_error(protocolIdentifier, Ptc), logtalk(create_protocol(Ptc, Relations, Directives), ExCtx)))
        ;	Current_protocol_(Ptc, _, _, _, _),
        throw(error(permission_error(modify, protocol, Ptc), logtalk(create_protocol(Ptc, Relations, Directives), ExCtx)))
        ;	Current_object_(Ptc, _, _, _, _, _, _, _, _, _, _),
        throw(error(permission_error(modify, object, Ptc), logtalk(create_protocol(Ptc, Relations, Directives), ExCtx)))
        ;	CurrentCategory_(Ptc, _, _, _, _, _),
        throw(error(permission_error(modify, category, Ptc), logtalk(create_protocol(Ptc, Relations, Directives), ExCtx)))
        ).

        Create_protocol(Ptc, Relations, Directives, ExCtx) :-
        Check(list, Relations, logtalk(create_protocol(Ptc, Relations, Directives), ExCtx)),
        Check(list, Directives, logtalk(create_protocol(Ptc, Relations, Directives), ExCtx)),
        catch(
        Create_protocolChecked(Ptc, Relations, Directives),
        Error,
        Create_entity_error_handler(Error, create_protocol(Ptc, Relations, Directives), ExCtx)
        ).


        Create_protocolChecked(Ptc, Relations, Directives) :-
        (	var(Ptc) ->
        generate_entityIdentifier(protocol, Ptc)
        ;	true
        ),
   //set the initial compilation context for compiling the protocol directives
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, runtime, _, '-(-1, -1)),
        Compile_protocolIdentifier(Ptc, Ctx),
        Compile_protocolRelations(Relations, Ptc, Ctx),
        assertz(ppDynamic_'),
        CompileLogtalkDirectives(Directives, Ctx),
        generate_protocolClauses',
        generate_protocolDirectives',
        AssertDynamic_entity(protocol),
        Restore_global_operator_table',
        Clean_ppCcClauses',
        Clean_pp_protocolClauses',
        Clean_ppRuntimeClauses'.



   //generate_entityIdentifier(+atom, -entityIdentifier)
        %
   //generates a new, unique, entity identifier by appending an integer to a base char
        %
   //note that it's possible to run out of (generated) entity identifiers when using a
   //backend Prolog compiler with bounded integer support

        generate_entityIdentifier(Kind, Identifier) :-
        retract(Dynamic_entityCounter_(Kind, Base, Count)),
        charCode(Base, Code),
        repeat,
        nextInteger(Count, New),
        numberCodes(New, Codes),
        atomCodes(Identifier, [Code| Codes]),
   //objects, protocols, and categories share a single namespace and there's
   //no guarantee that a user named entity will not clash with the generated
   //identifier despite the use of a per entity entityKind base character
        \+ Current_protocol_(Identifier, _, _, _, _),
        \+ Current_object_(Identifier, _, _, _, _, _, _, _, _, _, _),
        \+ CurrentCategory_(Identifier, _, _, _, _, _),
        asserta(Dynamic_entityCounter_(Kind, Base, New)),
        !.


        nextInteger(I, I).
        nextInteger(I, K) :-
        J is I + 1,
        nextInteger(J, K).



   //Create_entity_error_handler(@nonvar, @callable, @executionContext)
        %
   //error handler for the dynamic entity creation built-in predicates;
   //handles both compiler first stage and second stage errors

        Create_entity_error_handler(error(Error,_), Goal, ExCtx) :-
   //compiler second stage error; unwrap the error
        Create_entity_error_handler(Error, Goal, ExCtx).

        Create_entity_error_handler(Error, Goal, ExCtx) :-
        Restore_global_operator_table',
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        throw(error(Error, logtalk(Goal, ExCtx))).



   //abolish_object(+objectIdentifier)

        abolish_object(Obj) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Abolish_object(Obj, ExCtx).


        Abolish_object(Obj, ExCtx) :-
        Check(objectIdentifier, Obj, logtalk(abolish_object(Obj), ExCtx)),
        Abolish_objectChecked(Obj, ExCtx).


        Abolish_objectChecked(Obj, ExCtx) :-
        (	Current_object_(Obj, _, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, Flags) ->
        (	Flags /\ 2 =:= 2 ->
   //dynamic object
        Abolish_entity_predicates(Def),
        Abolish_entity_predicates(DDef),
        abolish(Dcl/4),
        abolish(Dcl/6),
        abolish(Def/3),
        abolish(Def/5),
        abolish(Super/5),
        abolish(IDcl/6),
        abolish(IDef/5),
        abolish(DDcl/2),
        abolish(DDef/3),
        abolish(Rnm/3),
        retractall(Current_object_(Obj, _, _, _, _, _, _, _, _, _, _)),
        retractall(entity_property_(Obj, _)),
        retractall(predicate_property_(Obj, _, _)),
        retractall(extends_object_(Obj, _, _)),
        retractall(InstantiatesClass_(Obj, _, _)),
        retractall(SpecializesClass_(Obj, _, _)),
        retractall(Implements_protocol_(Obj, _, _)),
        retractall(ImportsCategory_(Obj, _, _)),
        forall(
        Current_engine_(Obj, Engine, _, _),
        threaded_engineDestroy(Engine, ExCtx)
        ),
        CleanLookupCaches'
        ;	throw(error(permission_error(modify, static_object, Obj), logtalk(abolish_object(Obj), ExCtx)))
        )
        ;	throw(error(existence_error(object, Obj), logtalk(abolish_object(Obj), ExCtx)))
        ).



   //abolishCategory(+categoryIdentifier)

        abolishCategory(Ctg) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        AbolishCategory(Ctg, ExCtx).


        AbolishCategory(Ctg, ExCtx) :-
        Check(categoryIdentifier, Ctg, logtalk(abolishCategory(Ctg), ExCtx)),
        AbolishCategoryChecked(Ctg, ExCtx).


        AbolishCategoryChecked(Ctg, ExCtx) :-
        (	CurrentCategory_(Ctg, _, Dcl, Def, Rnm, Flags) ->
        (	Flags /\ 2 =:= 2 ->
   //dynamic category
        Abolish_entity_predicates(Def),
        abolish(Dcl/4),
        abolish(Dcl/5),
        abolish(Def/3),
        abolish(Def/4),
        abolish(Rnm/3),
        retractall(CurrentCategory_(Ctg, _, _, _, _, _)),
        retractall(entity_property_(Ctg, _)),
        retractall(predicate_property_(Ctg, _, _)),
        retractall(extendsCategory_(Ctg, _, _)),
        retractall(Implements_protocol_(Ctg, _, _)),
        retractall(Complemented_object_(_, Ctg, _, _, _)),
        CleanLookupCaches'
        ;	throw(error(permission_error(modify, staticCategory, Ctg), logtalk(abolishCategory(Ctg), ExCtx)))
        )
        ;	throw(error(existence_error(category, Ctg), logtalk(abolishCategory(Ctg), ExCtx)))
        ).



   //abolish_protocol(@protocolIdentifier)

        abolish_protocol(Ptc) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Abolish_protocol(Ptc, ExCtx).


        Abolish_protocol(Ptc, ExCtx) :-
        Check(protocolIdentifier, Ptc, logtalk(abolish_protocol(Ptc), ExCtx)),
        Abolish_protocolChecked(Ptc, ExCtx).


        Abolish_protocolChecked(Ptc, ExCtx) :-
        (	Current_protocol_(Ptc, _, Dcl, Rnm, Flags) ->
        (	Flags /\ 2 =:= 2 ->
   //dynamic protocol
        abolish(Dcl/4),
        abolish(Dcl/5),
        abolish(Rnm/3),
        retractall(Current_protocol_(Ptc, _, _, _, _)),
        retractall(entity_property_(Ptc, _)),
        retractall(predicate_property_(Ptc, _, _)),
        retractall(extends_protocol_(Ptc, _, _)),
        CleanLookupCaches'
        ;	throw(error(permission_error(modify, static_protocol, Ptc), logtalk(abolish_protocol(Ptc), ExCtx)))
        )
        ;	throw(error(existence_error(protocol, Ptc), logtalk(abolish_protocol(Ptc), ExCtx)))
        ).



   //Abolish_entity_predicates(+atom)
        %
   //auxiliary predicate used when abolishing objects and categories

        Abolish_entity_predicates(Def) :-
        call(Def, _, _, Call),
        unwrapCompiled_head(Call, Pred),
        functor(Pred, Functor, Arity),
        abolish(Functor/Arity),
        fail.

        Abolish_entity_predicates(_).



   //implements_protocol(?objectIdentifier, ?protocolIdentifier)
   //implements_protocol(?categoryIdentifier, ?protocolIdentifier)

        implements_protocol(ObjOrCtg, Ptc) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Implements_protocol(ObjOrCtg, Ptc, ExCtx).


        Implements_protocol(ObjOrCtg, Ptc, ExCtx) :-
        Check(var_or_objectIdentifier, ObjOrCtg, logtalk(implements_protocol(ObjOrCtg, Ptc), ExCtx)),
        Check(var_or_protocolIdentifier, Ptc, logtalk(implements_protocol(ObjOrCtg, Ptc), ExCtx)),
        Implements_protocol_(ObjOrCtg, Ptc, _).



   //implements_protocol(?objectIdentifier, ?protocolIdentifier, ?atom)
   //implements_protocol(?categoryIdentifier, ?protocolIdentifier, ?atom)

        implements_protocol(ObjOrCtg, Ptc, Scope) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Implements_protocol(ObjOrCtg, Ptc, Scope, ExCtx).


        Implements_protocol(ObjOrCtg, Ptc, Scope, ExCtx) :-
        Check(var_or_objectIdentifier, ObjOrCtg, logtalk(implements_protocol(ObjOrCtg, Ptc, Scope), ExCtx)),
        Check(var_or_protocolIdentifier, Ptc, logtalk(implements_protocol(ObjOrCtg, Ptc, Scope), ExCtx)),
        Check(var_orScope, Scope, logtalk(implements_protocol(ObjOrCtg, Ptc, Scope), ExCtx)),
        Implements_protocol_(ObjOrCtg, Ptc, Scope).



   //importsCategory(?objectIdentifier, ?categoryIdentifier)

        importsCategory(Obj, Ctg) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        ImportsCategory(Obj, Ctg, ExCtx).


        ImportsCategory(Obj, Ctg, ExCtx) :-
        Check(var_or_objectIdentifier, Obj, logtalk(importsCategory(Obj, Ctg), ExCtx)),
        Check(var_orCategoryIdentifier, Ctg, logtalk(importsCategory(Obj, Ctg), ExCtx)),
        ImportsCategory_(Obj, Ctg, _).



   //importsCategory(?objectIdentifier, ?categoryIdentifier, ?atom)

        importsCategory(Obj, Ctg, Scope) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        ImportsCategory(Obj, Ctg, Scope, ExCtx).


        ImportsCategory(Obj, Ctg, Scope, ExCtx) :-
        Check(var_or_objectIdentifier, Obj, logtalk(importsCategory(Obj, Ctg, Scope), ExCtx)),
        Check(var_orCategoryIdentifier, Ctg, logtalk(importsCategory(Obj, Ctg, Scope), ExCtx)),
        Check(var_orScope, Scope, logtalk(importsCategory(Obj, Ctg, Scope), ExCtx)),
        ImportsCategory_(Obj, Ctg, Scope).



   //instantiatesClass(?objectIdentifier, ?objectIdentifier)

        instantiatesClass(Obj, Class) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        InstantiatesClass(Obj, Class, ExCtx).


        InstantiatesClass(Obj, Class, ExCtx) :-
        Check(var_or_objectIdentifier, Obj, logtalk(instantiatesClass(Obj, Class), ExCtx)),
        Check(var_or_objectIdentifier, Class, logtalk(instantiatesClass(Obj, Class), ExCtx)),
        InstantiatesClass_(Obj, Class, _).



   //instantiatesClass(?objectIdentifier, ?objectIdentifier, ?atom)

        instantiatesClass(Obj, Class, Scope) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        InstantiatesClass(Obj, Class, Scope, ExCtx).


        InstantiatesClass(Obj, Class, Scope, ExCtx) :-
        Check(var_or_objectIdentifier, Obj, logtalk(instantiatesClass(Obj, Class, Scope), ExCtx)),
        Check(var_or_objectIdentifier, Class, logtalk(instantiatesClass(Obj, Class, Scope), ExCtx)),
        Check(var_orScope, Scope, logtalk(instantiatesClass(Obj, Class, Scope), ExCtx)),
        InstantiatesClass_(Obj, Class, Scope).



   //specializesClass(?objectIdentifier, ?objectIdentifier)

        specializesClass(Class, Superclass) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        SpecializesClass(Class, Superclass, ExCtx).


        SpecializesClass(Class, Superclass, ExCtx) :-
        Check(var_or_objectIdentifier, Class, logtalk(specializesClass(Class, Superclass), ExCtx)),
        Check(var_or_objectIdentifier, Superclass, logtalk(specializesClass(Class, Superclass), ExCtx)),
        SpecializesClass_(Class, Superclass, _).



   //specializesClass(?objectIdentifier, ?objectIdentifier, ?atom)

        specializesClass(Class, Superclass, Scope) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        SpecializesClass(Class, Superclass, Scope, ExCtx).


        SpecializesClass(Class, Superclass, Scope, ExCtx) :-
        Check(var_or_objectIdentifier, Class, logtalk(specializesClass(Class, Superclass, Scope), ExCtx)),
        Check(var_or_objectIdentifier, Superclass, logtalk(specializesClass(Class, Superclass, Scope), ExCtx)),
        Check(var_orScope, Scope, logtalk(specializesClass(Class, Superclass, Scope), ExCtx)),
        SpecializesClass_(Class, Superclass, Scope).



   //extendsCategory(?categoryIdentifier, ?categoryIdentifier)

        extendsCategory(Ctg, ExtCtg) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        extendsCategory(Ctg, ExtCtg, ExCtx).


        extendsCategory(Ctg, ExtCtg, ExCtx) :-
        Check(var_orCategoryIdentifier, Ctg, logtalk(extendsCategory(Ctg, ExtCtg), ExCtx)),
        Check(var_orCategoryIdentifier, ExtCtg, logtalk(extendsCategory(Ctg, ExtCtg), ExCtx)),
        extendsCategory_(Ctg, ExtCtg, _).



   //extendsCategory(?categoryIdentifier, ?categoryIdentifier, ?atom)

        extendsCategory(Ctg, ExtCtg, Scope) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        extendsCategory(Ctg, ExtCtg, Scope, ExCtx).


        extendsCategory(Ctg, ExtCtg, Scope, ExCtx) :-
        Check(var_orCategoryIdentifier, Ctg, logtalk(extendsCategory(Ctg, ExtCtg, Scope), ExCtx)),
        Check(var_orCategoryIdentifier, ExtCtg, logtalk(extendsCategory(Ctg, ExtCtg, Scope), ExCtx)),
        Check(var_orScope, Scope, logtalk(extendsCategory(Ctg, ExtCtg, Scope), ExCtx)),
        extendsCategory_(Ctg, ExtCtg, Scope).



   //extends_protocol(?protocolIdentifier, ?protocolIdentifier)

        extends_protocol(Ptc, ExtPtc) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        extends_protocol(Ptc, ExtPtc, ExCtx).


        extends_protocol(Ptc, ExtPtc, ExCtx) :-
        Check(var_or_protocolIdentifier, Ptc, logtalk(extends_protocol(Ptc, ExtPtc), ExCtx)),
        Check(var_or_protocolIdentifier, ExtPtc, logtalk(extends_protocol(Ptc, ExtPtc), ExCtx)),
        extends_protocol_(Ptc, ExtPtc, _).



   //extends_protocol(?protocolIdentifier, ?protocolIdentifier, ?atom)

        extends_protocol(Ptc, ExtPtc, Scope) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        extends_protocol(Ptc, ExtPtc, Scope, ExCtx).


        extends_protocol(Ptc, ExtPtc, Scope, ExCtx) :-
        Check(var_or_protocolIdentifier, Ptc, logtalk(extends_protocol(Ptc, ExtPtc, Scope), ExCtx)),
        Check(var_or_protocolIdentifier, ExtPtc, logtalk(extends_protocol(Ptc, ExtPtc, Scope), ExCtx)),
        Check(var_orScope, Scope, logtalk(extends_protocol(Ptc, ExtPtc, Scope), ExCtx)),
        extends_protocol_(Ptc, ExtPtc, Scope).



   //extends_object(?objectIdentifier, ?objectIdentifier)

        extends_object(Prototype, Parent) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        extends_object(Prototype, Parent, ExCtx).


        extends_object(Prototype, Parent, ExCtx) :-
        Check(var_or_objectIdentifier, Prototype, logtalk(extends_object(Prototype, Parent), ExCtx)),
        Check(var_or_objectIdentifier, Parent, logtalk(extends_object(Prototype, Parent), ExCtx)),
        extends_object_(Prototype, Parent, _).



   //extends_object(?objectIdentifier, ?objectIdentifier, ?atom)

        extends_object(Prototype, Parent, Scope) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        extends_object(Prototype, Parent, Scope, ExCtx).


        extends_object(Prototype, Parent, Scope, ExCtx) :-
        Check(var_or_objectIdentifier, Prototype, logtalk(extends_object(Prototype, Parent, Scope), ExCtx)),
        Check(var_or_objectIdentifier, Parent, logtalk(extends_object(Prototype, Parent, Scope), ExCtx)),
        Check(var_orScope, Scope, logtalk(extends_object(Prototype, Parent, Scope), ExCtx)),
        extends_object_(Prototype, Parent, Scope).



   //complements_object(?categoryIdentifier, ?objectIdentifier)

        complements_object(Category, Object) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Complements_object(Category, Object, ExCtx).


        Complements_object(Category, Object, ExCtx) :-
        Check(var_orCategoryIdentifier, Category, logtalk(complements_object(Category, Object), ExCtx)),
        Check(var_or_objectIdentifier, Object, logtalk(complements_object(Category, Object), ExCtx)),
        Complemented_object_(Object, Category, _, _, _).



   //conforms_to_protocol(?objectIdentifier, ?protocolIdentifier)
   //conforms_to_protocol(?categoryIdentifier, ?protocolIdentifier)

        conforms_to_protocol(ObjOrCtg, Protocol) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Conforms_to_protocol(ObjOrCtg, Protocol, ExCtx).


        Conforms_to_protocol(ObjOrCtg, Protocol, ExCtx) :-
        Check(var_or_objectIdentifier, ObjOrCtg, logtalk(conforms_to_protocol(ObjOrCtg, Protocol), ExCtx)),
        Check(var_or_protocolIdentifier, Protocol, logtalk(conforms_to_protocol(ObjOrCtg, Protocol), ExCtx)),
        (	var(ObjOrCtg) ->
        Conforms_to_protocolChecked(ObjOrCtg, Protocol, _)
        ;	var(Protocol) ->
        Conforms_to_protocolChecked(ObjOrCtg, Protocol, _)
        ;	% deterministic query
        Conforms_to_protocolChecked(ObjOrCtg, Protocol, _),
        !
        ).



   //conforms_to_protocol(?objectIdentifier, ?protocolIdentifier, ?atom)
   //conforms_to_protocol(?categoryIdentifier, ?protocolIdentifier, ?atom)

        conforms_to_protocol(ObjOrCtg, Protocol, Scope) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Conforms_to_protocol(ObjOrCtg, Protocol, Scope, ExCtx).


        Conforms_to_protocol(ObjOrCtg, Protocol, Scope, ExCtx) :-
        Check(var_or_objectIdentifier, ObjOrCtg, logtalk(conforms_to_protocol(ObjOrCtg, Protocol, Scope), ExCtx)),
        Check(var_or_protocolIdentifier, Protocol, logtalk(conforms_to_protocol(ObjOrCtg, Protocol, Scope), ExCtx)),
        Check(var_orScope, Scope, logtalk(conforms_to_protocol(ObjOrCtg, Protocol, Scope), ExCtx)),
        (	var(ObjOrCtg) ->
        Conforms_to_protocolChecked(ObjOrCtg, Protocol, Scope)
        ;	var(Protocol) ->
        Conforms_to_protocolChecked(ObjOrCtg, Protocol, Scope)
        ;	% deterministic query
        Conforms_to_protocolChecked(ObjOrCtg, Protocol, Scope),
        !
        ).


        Conforms_to_protocolChecked(Object, Protocol, Scope) :-
        Current_object_(Object, _, _, _, _, _, _, _, _, _, _),
        (	\+ InstantiatesClass_(Object, _, _),
        \+ SpecializesClass_(Object, _, _) ->
        prototypeConforms_to_protocol(Object, Protocol, Scope)
        ;	InstanceConforms_to_protocol(Object, Protocol, Scope)
        ).

        Conforms_to_protocolChecked(Category, Protocol, Scope) :-
        CurrentCategory_(Category, _, _, _, _, _),
        CategoryConforms_to_protocol(Category, Protocol, Scope).



        prototypeConforms_to_protocol(Prototype, Protocol, Scope) :-
        Complemented_object_(Prototype, Category, _, _, _),
        CategoryConforms_to_protocol(Category, Protocol, Scope).

        prototypeConforms_to_protocol(Prototype, Protocol, Scope) :-
        Implements_protocol_(Prototype, Protocol0, ImplementationScope),
        (	Protocol = Protocol0,
        Scope = ImplementationScope
        ;	protocolConforms_to_protocol(Protocol0, Protocol, InheritedScope),
        FilterScope(ImplementationScope, InheritedScope, Scope)
        ).

        prototypeConforms_to_protocol(Prototype, Protocol, Scope) :-
        ImportsCategory_(Prototype, Category, ImportScope),
        CategoryConforms_to_protocol(Category, Protocol, InheritedScope),
        FilterScope(ImportScope, InheritedScope, Scope).

        prototypeConforms_to_protocol(Prototype, Protocol, Scope) :-
        extends_object_(Prototype, Parent, ExtensionScope),
        prototypeConforms_to_protocol(Parent, Protocol, InheritedScope),
        FilterScope(ExtensionScope, InheritedScope, Scope).


        InstanceConforms_to_protocol(Instance, Protocol, Scope) :-
        InstantiatesClass_(Instance, Class, InstantiationScope),
        ClassConforms_to_protocol(Class, Protocol, InheritedScope),
        FilterScope(InstantiationScope, InheritedScope, Scope).


        ClassConforms_to_protocol(Class, Protocol, Scope) :-
        Complemented_object_(Class, Category, _, _, _),
        CategoryConforms_to_protocol(Category, Protocol, Scope).

        ClassConforms_to_protocol(Class, Protocol, Scope) :-
        Implements_protocol_(Class, Protocol0, ImplementationScope),
        (	Protocol = Protocol0,
        Scope = ImplementationScope
        ;	protocolConforms_to_protocol(Protocol0, Protocol, InheritedScope),
        FilterScope(ImplementationScope, InheritedScope, Scope)
        ).

        ClassConforms_to_protocol(Class, Protocol, Scope) :-
        ImportsCategory_(Class, Category, ImportScope),
        CategoryConforms_to_protocol(Category, Protocol, InheritedScope),
        FilterScope(ImportScope, InheritedScope, Scope).

        ClassConforms_to_protocol(Class, Protocol, Scope) :-
        SpecializesClass_(Class, Superclass, SpecializationScope),
        ClassConforms_to_protocol(Superclass, Protocol, InheritedScope),
        FilterScope(SpecializationScope, InheritedScope, Scope).


        protocolConforms_to_protocol(Protocol0, Protocol, Scope) :-
        extends_protocol_(Protocol0, Protocol1, ExtensionScope),
        (	Protocol = Protocol1,
        Scope = ExtensionScope
        ;	protocolConforms_to_protocol(Protocol1, Protocol, InheritedScope),
        FilterScope(ExtensionScope, InheritedScope, Scope)
        ).


        CategoryConforms_to_protocol(Category, Protocol, Scope) :-
        Implements_protocol_(Category, Protocol0, ImplementationScope),
        (	Protocol = Protocol0,
        Scope = ImplementationScope
        ;	protocolConforms_to_protocol(Protocol0, Protocol, InheritedScope),
        FilterScope(ImplementationScope, InheritedScope, Scope)
        ).

        CategoryConforms_to_protocol(Category, Protocol, Scope) :-
        extendsCategory_(Category, ExtendedCategory, ExtensionScope),
        CategoryConforms_to_protocol(ExtendedCategory, Protocol, InheritedScope),
        FilterScope(ExtensionScope, InheritedScope, Scope).


   //public relations don't change predicate scopes
        FilterScope((public), Scope, Scope).
   //protected relations change public predicates to protected predicates
        FilterScope(protected, Scope, protected) :-
        Scope \= (private).



   //current_event(?event, ?term, ?term, ?term, ?objectIdentifier)

        current_event(Event, Obj, Msg, Sender, Monitor) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Current_event(Event, Obj, Msg, Sender, Monitor, ExCtx).


        Current_event(Event, Obj, Msg, Sender, Monitor, ExCtx) :-
        Check(var_or_event, Event, logtalk(current_event(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_or_objectIdentifier, Obj, logtalk(current_event(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_orCallable, Msg, logtalk(current_event(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_or_objectIdentifier, Sender, logtalk(current_event(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_or_objectIdentifier, Monitor, logtalk(current_event(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Current_eventChecked(Event, Obj, Msg, Sender, Monitor).


        Current_eventChecked(before, Obj, Msg, Sender, Monitor) :-
        before_event_(Obj, Msg, Sender, Monitor, _).

        Current_eventChecked(after, Obj, Msg, Sender, Monitor) :-
        After_event_(Obj, Msg, Sender, Monitor, _).



   //define_events(@term, @term, @term, @term, +objectIdentifier)

        define_events(Event, Obj, Msg, Sender, Monitor) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Define_events(Event, Obj, Msg, Sender, Monitor, ExCtx).


        Define_events(Event, Obj, Msg, Sender, Monitor, ExCtx) :-
        Check(var_or_event, Event, logtalk(define_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_or_objectIdentifier, Obj, logtalk(define_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_orCallable, Msg, logtalk(define_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_or_objectIdentifier, Sender, logtalk(define_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(objectIdentifier, Monitor, logtalk(define_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        (	Current_object_(Monitor, _, _, Def, _, _, _, _, _, _, _) ->
        executionContext(MonitorExCtx, _, Monitor, Monitor, Monitor, [], []),
        (	var(Event) ->
        Define_events(before, Obj, Msg, Sender, Monitor, Def, MonitorExCtx),
        Define_events(after, Obj, Msg, Sender, Monitor, Def, MonitorExCtx)
        ;	Event == before ->
        Define_events(before, Obj, Msg, Sender, Monitor, Def, MonitorExCtx)
        ;	% Event == after
        Define_events(after, Obj, Msg, Sender, Monitor, Def, MonitorExCtx)
        )
        ;	throw(error(existence_error(object, Monitor), logtalk(define_events(Event, Obj, Msg, Sender, Monitor), ExCtx)))
        ).


        Define_events(before, Obj, Msg, Sender, Monitor, Def, ExCtx) :-
        (	call(Def, before(Obj, Msg, Sender), ExCtx, Call, _, _) ->
        retractall(before_event_(Obj, Msg, Sender, Monitor, _)),
        assertz(before_event_(Obj, Msg, Sender, Monitor, Call))
        ;	throw(error(existence_error(procedure, before/3), logtalk(define_events(before, Obj, Msg, Sender, Monitor), ExCtx)))
        ).

        Define_events(after, Obj, Msg, Sender, Monitor, Def, ExCtx) :-
        (	call(Def, after(Obj, Msg, Sender), ExCtx, Call, _, _) ->
        retractall(After_event_(Obj, Msg, Sender, Monitor, _)),
        assertz(After_event_(Obj, Msg, Sender, Monitor, Call))
        ;	throw(error(existence_error(procedure, after/3), logtalk(define_events(after, Obj, Msg, Sender, Monitor), ExCtx)))
        ).



   //abolish_events(@term, @term, @term, @term, @term)

        abolish_events(Event, Obj, Msg, Sender, Monitor) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Abolish_events(Event, Obj, Msg, Sender, Monitor, ExCtx).


        Abolish_events(Event, Obj, Msg, Sender, Monitor, ExCtx) :-
        Check(var_or_event, Event, logtalk(abolish_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_or_objectIdentifier, Obj, logtalk(abolish_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_orCallable, Msg, logtalk(abolish_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_or_objectIdentifier, Sender, logtalk(abolish_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        Check(var_or_objectIdentifier, Monitor, logtalk(abolish_events(Event, Obj, Msg, Sender, Monitor), ExCtx)),
        (	var(Event) ->
        retractall(before_event_(Obj, Msg, Sender, Monitor, _)),
        retractall(After_event_(Obj, Msg, Sender, Monitor, _))
        ;	Event == before ->
        retractall(before_event_(Obj, Msg, Sender, Monitor, _))
        ;	% Event == after
        retractall(After_event_(Obj, Msg, Sender, Monitor, _))
        ).



   //built-in multi-threading meta-predicates


   //threaded(+callable)

        threaded(Goals) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded(Goals), ExCtx))).

        threaded(Goals) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Check(qualifiedCallable, Goals, logtalk(threaded(Goals), ExCtx)),
        Compile_threadedCall(Goals, MTGoals),
        catch(MTGoals, Error, Runtime_error_handler(Error)).


   //threadedCall(@callable, -nonvar)

        threadedCall(Goal, Tag) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threadedCall(Goal, Tag), ExCtx))).

        threadedCall(Goal, Tag) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Check(var, Tag, logtalk(threadedCall(Goal, Tag), ExCtx)),
        catch(threadedCall_tagged(Goal, Goal, ExCtx, Tag), Error, Runtime_error_handler(Error)).


   //threadedCall(@callable)

        threadedCall(Goal) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threadedCall(Goal), ExCtx))).

        threadedCall(Goal) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        catch(threadedCall(Goal, Goal, ExCtx), Error, Runtime_error_handler(Error)).


   //threaded_once(@callable, -nonvar)

        threaded_once(Goal, Tag) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_once(Goal, Tag), ExCtx))).

        threaded_once(Goal, Tag) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Check(var, Tag, logtalk(threaded_once(Goal, Tag), ExCtx)),
        catch(threaded_once_tagged(Goal, Goal, ExCtx, Tag), Error, Runtime_error_handler(Error)).


   //threaded_once(@callable)

        threaded_once(Goal) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_once(Goal), ExCtx))).

        threaded_once(Goal) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        catch(threaded_once(Goal, Goal, ExCtx), Error, Runtime_error_handler(Error)).


   //threadedIgnore(@callable)

        threadedIgnore(Goal) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threadedIgnore(Goal), ExCtx))).

        threadedIgnore(Goal) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        catch(threadedIgnore(Goal, Goal, ExCtx), Error, Runtime_error_handler(Error)).


   //threaded_exit(+callable, +nonvar)

        threaded_exit(Goal, Tag) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_exit(Goal, Tag), ExCtx))).

        threaded_exit(Goal, Tag) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Check(qualifiedCallable, Goal, logtalk(threaded_exit(Goal, Tag), ExCtx)),
        catch(threaded_exit_tagged(Goal, ExCtx, Tag), Error, Runtime_error_handler(Error)).


   //threaded_exit(+callable)

        threaded_exit(Goal) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_exit(Goal), ExCtx))).

        threaded_exit(Goal) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Check(qualifiedCallable, Goal, logtalk(threaded_exit(Goal), ExCtx)),
        catch(threaded_exit(Goal, ExCtx), Error, Runtime_error_handler(Error)).


   //threaded_peek(+callable, +nonvar)

        threaded_peek(Goal, Tag) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_peek(Goal, Tag), ExCtx))).

        threaded_peek(Goal, Tag) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Check(qualifiedCallable, Goal, logtalk(threaded_peek(Goal, Tag), ExCtx)),
        catch(threaded_peek_tagged(Goal, ExCtx, Tag), Error, Runtime_error_handler(Error)).


   //threaded_peek(+callable)

        threaded_peek(Goal) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_peek(Goal), ExCtx))).

        threaded_peek(Goal) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        Check(qualifiedCallable, Goal, logtalk(threaded_peek(Goal), ExCtx)),
        catch(threaded_peek(Goal, ExCtx), Error, Runtime_error_handler(Error)).


   //threaded_engineCreate(@term, @callable, ?nonvar)

        threaded_engineCreate(AnswerTemplate, Goal, Engine) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engineCreate(AnswerTemplate, Goal, Engine), ExCtx))).

        threaded_engineCreate(AnswerTemplate, Goal, Engine) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        catch(threaded_engineCreate(AnswerTemplate, Goal, Goal, ExCtx, Engine), Error, Runtime_error_handler(Error)).


   //threaded_engine(?nonvar)

        threaded_engineSelf(Engine) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engineSelf(Engine), ExCtx))).

        threaded_engineSelf(Engine) :-
        threaded_engineSelf(user, Engine).


   //threaded_engine(?nonvar)

        threaded_engine(Engine) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engine(Engine), ExCtx))).

        threaded_engine(Engine) :-
        Current_engine(user, Engine).


   //threaded_engine_next(@nonvar, ?term)

        threaded_engine_next(Engine, Answer) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engine_next(Engine, Answer), ExCtx))).

        threaded_engine_next(Engine, Answer) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        catch(threaded_engine_next(Engine, Answer, ExCtx), Error, Runtime_error_handler(Error)).


   //threaded_engine_nextReified(@nonvar, ?term)

        threaded_engine_nextReified(Engine, Answer) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engine_nextReified(Engine, Answer), ExCtx))).

        threaded_engine_nextReified(Engine, Answer) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        catch(threaded_engine_nextReified(Engine, Answer, ExCtx), Error, Runtime_error_handler(Error)).


   //threaded_engine_yield(@term)

        threaded_engine_yield(Answer) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engine_yield(Answer), ExCtx))).

        threaded_engine_yield(Answer) :-
        catch(threaded_engine_yield(Answer, user), Error, Runtime_error_handler(Error)).


   //threaded_engine_post(@nonvar, @term)

        threaded_engine_post(Engine, Term) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engine_post(Engine, Term), ExCtx))).

        threaded_engine_post(Engine, Term) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        catch(threaded_engine_post(Engine, Term, ExCtx), Error, Runtime_error_handler(Error)).


   //threaded_engineFetch(?term)

        threaded_engineFetch(Term) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engineFetch(Term), ExCtx))).

        threaded_engineFetch(Term) :-
        catch(threaded_engineFetch(Term, user), Error, Runtime_error_handler(Error)).


   //threaded_engineDestroy(+nonvar)

        threaded_engineDestroy(Engine) :-
        \+ prologFeature(engines, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_engineDestroy(Engine), ExCtx))).

        threaded_engineDestroy(Engine) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        catch(threaded_engineDestroy(Engine, ExCtx), Error, Runtime_error_handler(Error)).


   //threaded_wait(?nonvar)

        threaded_wait(Message) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_wait(Message), ExCtx))).

        threaded_wait(Message) :-
        Current_object_(user, Prefix, _, _, _, _, _, _, _, _, _),
        threaded_wait(Message, Prefix).


   //threaded_notify(@term)

        threaded_notify(Message) :-
        \+ prologFeature(threads, supported),
        executionContext(ExCtx, user, user, user, user, [], []),
        throw(error(resource_error(threads), logtalk(threaded_notify(Message), ExCtx))).

        threaded_notify(Message) :-
        Current_object_(user, Prefix, _, _, _, _, _, _, _, _, _),
        threaded_notify(Message, Prefix).



   //compiling and loading built-in predicates


   //CompilerFlag(+atom, ?nonvar)
        %
   //gets/checks the current value of a compiler flag; the default flag
   //values and the backend Prolog feature flags are cached at startup

        CompilerFlag(Name, Value) :-
        (	pp_entityCompilerFlag_(Name, CurrentValue) ->
   //flag value as defined within the entity being compiled
        Value = CurrentValue
        ;	ppFileCompilerFlag_(Name, CurrentValue) ->
   //flag value as defined in the flags argument of the
   //compiling/loading predicates or in the source file
        Value = CurrentValue
        ;	CurrentFlag_(Name, Value)
   //default value for the current Logtalk session,
   //cached or set by calls to the setLogtalkFlag/2 predicate
        ).



   //logtalkCompile(@sourceFile_name)
   //logtalkCompile(@list(sourceFile_name))
        %
   //compiles to disk a source file or list of source files using default flags
        %
   //top-level calls use the current working directory for resolving any relative
   //source file paths while compiled calls in a source file use the source file
   //directory by default

        logtalkCompile(Files) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        CurrentDirectory(Directory),
        LogtalkCompile(Files, Directory, ExCtx).


        LogtalkCompile(Files, Directory, ExCtx) :-
        catch(
        LogtalkCompileFiles(Files, Directory),
        error(Error, _),
        LogtalkCompile_error_handler(Error, Files, ExCtx)
        ).


        LogtalkCompileFiles(Files, Directory) :-
        Init_warningsCounter(logtalkCompile(Files)),
        CheckAnd_expandSourceFiles(Files, ExpandedFiles),
        CompileFiles(ExpandedFiles, ['$relative_to(Directory)]),
        Report_warning_numbers(logtalkCompile(Files)),
        Clean_ppFileClauses'.


        LogtalkCompile_error_handler(Error, Files, ExCtx) :-
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        Reset_warningsCounter',
        throw(error(Error, logtalk(logtalkCompile(Files), ExCtx))).



   //logtalkCompile(@sourceFile_name, @list(compilerFlag))
   //logtalkCompile(@list(sourceFile_name), @list(compilerFlag))
        %
   //compiles to disk a source file or a list of source files using a list of flags
        %
   //top-level calls use the current working directory for resolving any relative
   //source file paths while compiled calls in a source file use the source file
   //directory by default
        %
   //note that we can only clean the compiler flags after reporting warning numbers as the
   //report/1 flag might be included in the list of flags but we cannot test for it as its
   //value should only be used in the default code for printing messages

        logtalkCompile(Files, Flags) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        CurrentDirectory(Directory),
        LogtalkCompile(Files, Flags, Directory, ExCtx).


        LogtalkCompile(Files, Flags, Directory, ExCtx) :-
        catch(
        LogtalkCompileFiles(Files, Flags, Directory),
        error(Error, _),
        LogtalkCompile_error_handler(Error, Files, Flags, ExCtx)
        ).


        LogtalkCompileFiles(Files, Flags, Directory) :-
        Init_warningsCounter(logtalkCompile(Files, Flags)),
        CheckAnd_expandSourceFiles(Files, ExpandedFiles),
        CheckCompilerFlags(Flags),
        (	member(relative_to(_), Flags) ->
        CompileFiles(ExpandedFiles, Flags)
        ;	CompileFiles(ExpandedFiles, ['$relative_to(Directory)| Flags])
        ),
        Report_warning_numbers(logtalkCompile(Files, Flags)),
        Clean_ppFileClauses'.


        LogtalkCompile_error_handler(Error, Files, Flags, ExCtx) :-
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        Reset_warningsCounter',
        throw(error(Error, logtalk(logtalkCompile(Files, Flags), ExCtx))).



   //predicates for compilation warning counting and reporting

        Reset_warningsCounter' :-
        retractall(pp_warnings_top_goal_(_)),
        retractall(ppCompiling_warningsCounter_(_)),
        retractall(ppLoading_warningsCounter_(_)).


        Init_warningsCounter(Goal) :-
        (	pp_warnings_top_goal_(_) ->
   //not top compilation/loading goal; do nothing
        true
        ;	% remember top compilation/loading goal
        assertz(pp_warnings_top_goal_(Goal)),
   //initialize compilation warnings counter
        retractall(ppCompiling_warningsCounter_(_)),
        assertz(ppCompiling_warningsCounter_(0)),
   //initialize loading warnings counter
        retractall(ppLoading_warningsCounter_(_)),
        assertz(ppLoading_warningsCounter_(0))
        ).


        IncrementCompiling_warningsCounter' :-
        retract(ppCompiling_warningsCounter_(Old)),
        New is Old + 1,
        assertz(ppCompiling_warningsCounter_(New)).


        IncrementLoading_warningsCounter' :-
        retract(ppLoading_warningsCounter_(Old)),
        New is Old + 1,
        assertz(ppLoading_warningsCounter_(New)).


        Report_warning_numbers(Goal) :-
        (	retract(pp_warnings_top_goal_(Goal)),
   //top compilation/loading goal
        retract(ppCompiling_warningsCounter_(CCounter)),
        retract(ppLoading_warningsCounter_(LCounter)) ->
   //report compilation and loading warnings
        print_message(comment(warnings), core, compilationAndLoading_warnings(CCounter, LCounter))
        ;	% not top compilation/loading goal
        true
        ).



   //CheckAnd_expandSourceFiles(@nonvar, -nonvar)
   //CheckAnd_expandSourceFiles(@list, -list)
        %
   //check if the source file names are valid (but not if the file exists)
   //and return their absolute paths when using library notation or when
   //they start with an environment variable (assumes environment variables
   //use POSIX syntax in Prolog internal file paths)

        CheckAnd_expandSourceFiles([File| Files], [Path| Paths]) :-
        !,
        CheckAnd_expandSourceFile(File, Path),
        CheckAnd_expandSourceFiles(Files, Paths).

        CheckAnd_expandSourceFiles([], []) :-
        !.

        CheckAnd_expandSourceFiles(File, Path) :-
        CheckAnd_expandSourceFile(File, Path).


        CheckAnd_expandSourceFile(File, Path) :-
        (	atom(File) ->
        prolog_osFile_name(NormalizedFile, File),
        (	subAtom(NormalizedFile, 0, 1, _, '$') ->
        expand_path(NormalizedFile, Path)
        ;	Path = NormalizedFile
        )
        ;	compound(File),
        File =.. [Library, Basename],
        atom(Basename) ->
   //library notation
        prolog_osFile_name(NormalizedBasename, Basename),
        (	expandLibraryAlias(Library, Directory) ->
        atomConcat(Directory, NormalizedBasename, Path)
        ;	throw(error(existence_error(library, Library), _))
        )
        ;	% invalid source file specification
        ground(File) ->
        throw(error(type_error(sourceFile_name, File), _))
        ;	throw(error(instantiation_error, _))
        ).



   //expandLibraryAlias(+atom, -atom)
        %
   //converts a library alias into its corresponding path; uses a depth
   //bound to prevent loops (inspired by similar code in SWI-Prolog)

        expandLibraryAlias(Library, Path) :-
        expandLibraryAlias(Library, Path0, 16),
   //expand the library path into an absolute path as it may
   //contain environment variables that need to be expanded
        expand_path(Path0, Path1),
   //make sure that the library path ends with a slash
        (	subAtom(Path1, _, 1, 0, '/') ->
        Path = Path1
        ;	atomConcat(Path1, '/', Path)
        ).


        expandLibraryAlias(Library, Path, Depth) :-
        logtalkLibrary_path(Library, Location), !,
        (	compound(Location),
        Location =.. [Prefix, Directory],
        atom(Directory) ->
   //assume library notation (a compound term)
        Depth > 0,
        NewDepth is Depth - 1,
        expandLibraryAlias(Prefix, PrefixPath0, NewDepth),
   //make sure that the prefix path ends with a slash
        (	subAtom(PrefixPath0, _, 1, 0, '/') ->
        atomConcat(PrefixPath0, Directory, Path)
        ;	atomConcat(PrefixPath0, '/', PrefixPath1),
        atomConcat(PrefixPath1, Directory, Path)
        )
        ;	atom(Location) ->
   //assume the final component of the library path
        Path = Location
        ;	ground(Location) ->
        throw(error(type_error(library_path, Location), _))
        ;	throw(error(instantiation_error, _))
        ).



   //CheckCompilerFlags(@list)
        %
   //checks if the compiler flags are valid

        CheckCompilerFlags([Flag| Flags]) :-
        !,
        (	var(Flag) ->
        throw(error(instantiation_error, _))
        ;	Flag =.. [Name, Value] ->
        Check(read_writeFlag, Name, _),
        Check(flag_value, Name+Value, _)
        ;	% invalid flag syntax
        compound(Flag) ->
        throw(error(domain_error(compilerFlag, Flag), _))
        ;	throw(error(type_error(compound, Flag), _))
        ),
        CheckCompilerFlags(Flags).

        CheckCompilerFlags([]) :-
        !.

        CheckCompilerFlags(Flags) :-
        throw(error(type_error(list, Flags), _)).



   //SetCompilerFlags(@list)
        %
   //sets the compiler flags

        SetCompilerFlags(Flags) :-
        AssertCompilerFlags(Flags),
   //only one of the optimize and debug flags can be turned on at the same time
        (	member(optimize(on), Flags) ->
        retractall(ppFileCompilerFlag_(debug, _)),
        assertz(ppFileCompilerFlag_(debug, off))
        ;	member(debug(on), Flags) ->
        retractall(ppFileCompilerFlag_(optimize, _)),
        assertz(ppFileCompilerFlag_(optimize, off))
        ;	true
        ),
        (	ppFileCompilerFlag_(hook, HookEntity) ->
   //pre-compile hooks in order to speed up entity compilation
        (	current_object(HookEntity) ->
        CompCtx(Ctx, _, _, user, user, user, HookEntity, _, [], [], ExCtx, runtime, [], _),
        executionContext(ExCtx, user, user, user, HookEntity, [], []),
        CurrentFlag_(events, Events),
        Compile_message_to_object(term_expansion(Term, Terms), HookEntity, TermExpansionGoal, Events, Ctx),
        Compile_message_to_object(goal_expansion(Goal, ExpandedGoal), HookEntity, GoalExpansionGoal, Events, Ctx)
        ;	atom(HookEntity),
        prologFeature(modules, supported),
        current_module(HookEntity) ->
        TermExpansionGoal = ':(HookEntity, term_expansion(Term, Terms)),
        GoalExpansionGoal = ':(HookEntity, goal_expansion(Goal, ExpandedGoal))
        ;	throw(error(existence_error(object, HookEntity), _))
        ),
        assertz((
        pp_hook_term_expansion_(Term, Terms) :-
        catch(TermExpansionGoal, Error, term_expansion_error(HookEntity, Term, Error))
        )),
        assertz((
        pp_hook_goal_expansion_(Goal, ExpandedGoal) :-
        catch(GoalExpansionGoal, Error, goal_expansion_error(HookEntity, Goal, Error))
        ))
        ;	true
        ).


   //term-expansion errors result in a warning message and a failure

        term_expansion_error(HookEntity, Term, Error) :-
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(expansion), core, term_expansion_error(File, Lines, Type, Entity, HookEntity, Term, Error))
        ;	print_message(warning(expansion), core, term_expansion_error(File, Lines, HookEntity, Term, Error))
        ),
        fail.


   //goal-expansion errors result in a warning message and a failure

        goal_expansion_error(HookEntity, Goal, Error) :-
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(expansion), core, goal_expansion_error(File, Lines, Type, Entity, HookEntity, Goal, Error))
        ;	print_message(warning(expansion), core, goal_expansion_error(File, Lines, HookEntity, Goal, Error))
        ),
        fail.



        AssertCompilerFlags([]).

        AssertCompilerFlags([Flag| Flags]) :-
        Flag =.. [Name, Value],
        retractall(ppFileCompilerFlag_(Name, _)),
        assertz(ppFileCompilerFlag_(Name, Value)),
        AssertCompilerFlags(Flags).



   //logtalkLoad(@sourceFile_name)
   //logtalkLoad(@list(sourceFile_name))
        %
   //compiles to disk and then loads to memory a source file or a list of source
   //files using default compiler flags
        %
   //top-level calls use the current working directory for resolving any relative
   //source file paths while compiled calls in a source file use the source file
   //directory by default

        logtalkLoad(Files) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        CurrentDirectory(Directory),
        LogtalkLoad(Files, Directory, ExCtx).


        LogtalkLoad(Files, Directory, ExCtx) :-
        catch(
        LogtalkLoadFiles(Files, Directory),
        error(Error, _),
        LogtalkLoad_error_handler(Error, Files, ExCtx)
        ).


        LogtalkLoadFiles(Files, Directory) :-
        Init_warningsCounter(logtalkLoad(Files)),
        CheckAnd_expandSourceFiles(Files, ExpandedFiles),
        LoadFiles(ExpandedFiles, ['$relative_to(Directory)]),
        Report_warning_numbers(logtalkLoad(Files)),
        Clean_ppFileClauses'.


        LogtalkLoad_error_handler(Error, Files, ExCtx) :-
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        Reset_warningsCounter',
        throw(error(Error, logtalk(logtalkLoad(Files), ExCtx))).



   //logtalkLoad(@sourceFile_name, @list(compilerFlag))
   //logtalkLoad(@list(sourceFile_name), @list(compilerFlag))
        %
   //compiles to disk and then loads to memory a source file or a list of source
   //files using a list of compiler flags
        %
   //top-level calls use the current working directory for resolving any relative
   //source file paths while compiled calls in a source file use the source file
   //directory by default
        %
   //note that we can only clean the compiler flags after reporting warning
   //numbers as the report/1 flag might be in the list of flags but we cannot
   //test for it as its value should only be used in the default code for
   //printing messages

        logtalkLoad(Files, Flags) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        CurrentDirectory(Directory),
        LogtalkLoad(Files, Flags, Directory, ExCtx).


        LogtalkLoad(Files, Flags, Directory, ExCtx) :-
        catch(
        LogtalkLoadFiles(Files, Flags, Directory),
        error(Error, _),
        LogtalkLoad_error_handler(Error, Files, Flags, ExCtx)
        ).


        LogtalkLoadFiles(Files, Flags, Directory) :-
        Init_warningsCounter(logtalkLoad(Files, Flags)),
        CheckAnd_expandSourceFiles(Files, ExpandedFiles),
        CheckCompilerFlags(Flags),
        (	member(relative_to(_), Flags) ->
        LoadFiles(ExpandedFiles, Flags)
        ;	LoadFiles(ExpandedFiles, ['$relative_to(Directory)| Flags])
        ),
        Report_warning_numbers(logtalkLoad(Files, Flags)),
        Clean_ppFileClauses'.


        LogtalkLoad_error_handler(Error, Files, Flags, ExCtx) :-
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        Reset_warningsCounter',
        throw(error(Error, logtalk(logtalkLoad(Files, Flags), ExCtx))).



   //logtalk_make
        %
   //reloads all Logtalk source files that have been modified since the
   //time they are last loaded

        logtalk_make :-
        logtalk_make(all).



   //logtalk_make(+atom)
        %
   //performs a make target

        logtalk_make(Target) :-
        (	var(Target) ->
        print_message(warning(make), core, no_make_targetSpecified),
        fail
        ;	validLogtalk_make_target(Target) ->
        Logtalk_make(Target),
        Logtalk_make_targetActions(Target)
        ;	print_message(warning(make), core, invalid_make_target(Target)),
        fail
        ).


   //reload of changed Logtalk source files
        validLogtalk_make_target(all).
   //recompile files in debug mode
        validLogtalk_make_target(debug).
   //recompile files in normal mode
        validLogtalk_make_target(normal).
   //recompile files in optimal mode
        validLogtalk_make_target(optimal).
   //clean all intermediate Prolog files
        validLogtalk_make_target(clean).
   //list missing entities and missing predicates
        validLogtalk_make_target(check).
   //list circular entity references
        validLogtalk_make_target(circular).
   //generate documentation
        validLogtalk_make_target(documentation).
   //clean dynamic binding caches
        validLogtalk_make_target(caches).


        Logtalk_make_targetActions(Target) :-
        logtalk_make_targetAction(Target),
        fail.
        Logtalk_make_targetActions(_).


        Logtalk_make(all) :-
        FailedFile_(Path),
   //the following predicate may no longer be defined depending on what caused the failure
        ppFile_pathsFlags_(_, _, Path, _, Flags),
        logtalkLoad(Path, Flags),
        fail.
        Logtalk_make(all) :-
        FailedFile_(Path),
        DecomposeFile_name(Path, Directory, Name, Extension),
        atomConcat(Name, Extension, Basename),
   //force reloding by changing the main file time stamp to 0.0, a value that in the standard term
   //comparison order used to compare time stamps comes before any integer or float actual time stamp
        retract(LoadedFile_(Basename, Directory, Mode, Flags, TextProperties, ObjectFile, _)),
        assertz(LoadedFile_(Basename, Directory, Mode, Flags, TextProperties, ObjectFile, 0.0)),
        logtalkLoad(Path, Flags),
        fail.
   //recompilation of changed source files since last loaded
        Logtalk_make(all) :-
        LoadedFile_(Basename, Directory, _, Flags, _, _, LoadingTimeStamp),
        atomConcat(Directory, Basename, Path),
        File_modification_time(Path, CurrentTimeStamp),
        LoadingTimeStamp @< CurrentTimeStamp,
        \+ member(reload(skip), Flags),
        logtalkLoad(Path, Flags),
        fail.
   //recompilation of included source files since last loaded
        Logtalk_make(all) :-
        IncludedFile_(Path, MainBasename, MainDirectory, LoadingTimeStamp),
        File_modification_time(Path, CurrentTimeStamp),
        LoadingTimeStamp @< CurrentTimeStamp,
   //force reloding by changing the main file time stamp to 0.0, a value that in the standard term
   //comparison order used to compare time stamps comes before any integer or float actual time stamp
        retract(LoadedFile_(MainBasename, MainDirectory, Mode, Flags, TextProperties, ObjectFile, _)),
        assertz(LoadedFile_(MainBasename, MainDirectory, Mode, Flags, TextProperties, ObjectFile, 0.0)),
        atomConcat(MainDirectory, MainBasename, MainPath),
        logtalkLoad(MainPath, Flags),
        fail.
   //recompilation due to a change to the compilation mode (e.g. from "normal" to "debug")
        Logtalk_make(all) :-
   //find all files impacted by a change to compilation mode (this excludes all files
   //that are compiled with an explicit compilation mode set using the corresponding
   //compiler option)
        findall(
        file(Path, Flags),
        (	LoadedFile_(Basename, Directory, Mode, Flags, _, _, _),
        ChangedCompilation_mode(Mode, Flags),
        atomConcat(Directory, Basename, Path)
        ),
        Files
        ),
   //filter files that will be reloaded by a parent file that will also be reloaded
        member(file(Path,Flags), Files),
        \+ (
        parentFile_(Path, Parent),
        member(file(Parent,_), Files)
        ),
        logtalkLoad(Path, Flags),
        fail.
        Logtalk_make(all) :-
        print_message(comment(make), core, modifiedFilesReloaded).

        Logtalk_make(debug) :-
        print_message(comment(make), core, reloadFilesIn_mode(debug)),
        SetCompilerFlag(debug, on),
        Logtalk_make(all).

        Logtalk_make(normal) :-
        print_message(comment(make), core, reloadFilesIn_mode(normal)),
        SetCompilerFlag(debug, off),
        SetCompilerFlag(optimize, off),
        Logtalk_make(all).

        Logtalk_make(optimal) :-
        print_message(comment(make), core, reloadFilesIn_mode(optimal)),
        SetCompilerFlag(optimize, on),
        Logtalk_make(all).

        Logtalk_make(clean) :-
        LoadedFile_(_, _, _, _, _, ObjectFile, _),
        DeleteIntermediateFiles(ObjectFile),
        fail.
        Logtalk_make(clean) :-
        print_message(comment(make), core, intermediateFilesDeleted).

        Logtalk_make(check) :-
        print_message(comment(make), core, scanningFor_missing_entities_predicates),
        setof(Protocol, missing_protocol(Protocol), Protocols),
        print_message(warning(make), core, missing_protocols(Protocols)),
        fail.
        Logtalk_make(check) :-
        setof(Category, missingCategory(Category), Categories),
        print_message(warning(make), core, missingCategories(Categories)),
        fail.
        Logtalk_make(check) :-
        setof(Object, missing_object(Object), Objects),
        print_message(warning(make), core, missing_objects(Objects)),
        fail.
        Logtalk_make(check) :-
        prologFeature(modules, supported),
        setof(Module, missing_module(Module), Modules),
        print_message(warning(make), core, missing_modules(Modules)),
        fail.
        Logtalk_make(check) :-
        setof(Predicate, missing_predicate(Predicate), Predicates),
        print_message(warning(make), core, missing_predicates(Predicates)),
        fail.
        Logtalk_make(check) :-
        print_message(comment(make), core, completedScanningFor_missing_entities_predicates).

        Logtalk_make(circular) :-
        print_message(comment(make), core, scanningForCircularDependencies),
        setof(CircularReference, CircularReference(CircularReference), CircularReferences),
        print_message(warning(make), core, circularReferences(CircularReferences)),
        fail.
        Logtalk_make(circular) :-
        print_message(comment(make), core, completedScanningForCircularDependencies).

        Logtalk_make(documentation) :-
        print_message(comment(make), core, runningAllDefinedDocumentationActions).

        Logtalk_make(caches) :-
        CleanLookupCaches',
        print_message(comment(make), core, dynamic_bindingCachesDeleted).


   //deal with changes to the default compilation mode
   //when no explicit compilation mode as specified

        ChangedCompilation_mode(debug, Flags) :-
        \+ member(debug(_), Flags),
        \+ CompilerFlag(debug, on).

        ChangedCompilation_mode(optimal, Flags) :-
        \+ member(optimize(_), Flags),
        \+ CompilerFlag(optimize, on).

        ChangedCompilation_mode(normal, _) :-
        (	CompilerFlag(debug, on) ->
        true
        ;	CompilerFlag(optimize, on)
        ).


   //find missing entities for logtalk_make(check)

        missing_protocol(Protocol-Reference) :-
        Implements_protocol_(Entity, Protocol, _),
        \+ Current_protocol_(Protocol, _, _, _, _),
        missingReference(Entity, _, Reference).

        missing_protocol(Protocol-Reference) :-
        extends_protocol_(Entity, Protocol, _),
        \+ Current_protocol_(Protocol, _, _, _, _),
        missingReference(Entity, _, Reference).


        missingCategory(Category-Reference) :-
        ImportsCategory_(Entity, Category, _),
        \+ CurrentCategory_(Category, _, _, _, _, _),
        missingReference(Entity, _, Reference).

        missingCategory(Category-Reference) :-
        extendsCategory_(Entity, Category, _),
        \+ CurrentCategory_(Category, _, _, _, _, _),
        missingReference(Entity, _, Reference).


        missing_object(Object-Reference) :-
        extends_object_(Entity, Object, _),
        \+ Current_object_(Object, _, _, _, _, _, _, _, _, _, _),
        missingReference(Entity, _, Reference).

        missing_object(Object-Reference) :-
        InstantiatesClass_(Entity, Object, _),
        \+ Current_object_(Object, _, _, _, _, _, _, _, _, _, _),
        missingReference(Entity, _, Reference).

        missing_object(Object-Reference) :-
        SpecializesClass_(Entity, Object, _),
        \+ Current_object_(Object, _, _, _, _, _, _, _, _, _, _),
        missingReference(Entity, _, Reference).

        missing_object(Object-Reference) :-
        Complemented_object_(Object, Entity, _, _, _),
        \+ Current_object_(Object, _, _, _, _, _, _, _, _, _, _),
        missingReference(Entity, _, Reference).

        missing_object(Object-Reference) :-
        entity_property_(Entity, calls(Object::_, _, _, _, Location)),
   //note that the next call always fails when Object is not bound
        \+ Current_object_(Object, _, _, _, _, _, _, _, _, _, _),
        missingReference(Entity, Location, Reference).


        missing_module(Module-Reference) :-
        entity_property_(Entity, calls(':(Module,_), _, _, _, Location)),
   //note that the next call always fails when Module is not bound;
   //given the call, assume that the backend compiler supports modules
        \+ current_module(Module),
        missingReference(Entity, Location, Reference).


   //find missing predicates for logtalk_make(check)

        missing_predicate((Object::Predicate)-Reference) :-
        entity_property_(Entity, calls(Object::Predicate, _, _, _, Location)),
   //the object may only be known at runtime; reject those cases
        nonvar(Object),
   //require loaded objects as the missing objects are already listed
        Current_object_(Object, _, _, _, _, _, _, _, _, _, _),
   //ignore objects that can forward the predicates calls
        \+ Implements_protocol_(Object, forwarding, _),
        \+ Current_predicate(Object, Predicate, Entity, p(p(p)), _),
        missingReference(Entity, Location, Reference).

        missing_predicate((^^Functor/Arity)-Reference) :-
        entity_property_(Entity, calls(^^Functor/Arity, _, _, _, Location)),
        functor(Template, Functor, Arity),
        (	Current_object_(Entity, _, Dcl, _, _, IDcl, _, _, _, _, _) ->
        (	\+ InstantiatesClass_(Entity, _, _),
        \+ SpecializesClass_(Entity, _, _) ->
   //prototype
        \+ call(Dcl, Template, _, _, _, _, _)
        ;	% instance and/or class
        \+ call(IDcl, Template, _, _, _, _, _)
        )
        ;	CurrentCategory_(Entity, _, Dcl, _, _, _),
        \+ call(Dcl, Template, _, _, _, _)
        ),
        missingReference(Entity, Location, Reference).

        missing_predicate((::Functor/Arity)-Reference) :-
        entity_property_(Entity, calls(::Functor/Arity, _, _, _, Location)),
        functor(Template, Functor, Arity),
        (	Current_object_(Entity, _, Dcl, _, _, IDcl, _, _, _, _, _) ->
        (	\+ InstantiatesClass_(Entity, _, _),
        \+ SpecializesClass_(Entity, _, _) ->
   //prototype
        \+ call(Dcl, Template, _, _, _, _, _)
        ;	% instance and/or class
        \+ call(IDcl, Template, _, _, _, _, _)
        )
        ;	CurrentCategory_(Entity, _, Dcl, _, _, _),
        \+ call(Dcl, Template, _, _, _, _)
        ),
        missingReference(Entity, Location, Reference).

        missing_predicate((Functor/Arity)-Reference) :-
        entity_property_(Entity, calls(Functor/Arity, _, _, _, Location)),
        (	Current_object_(Entity, _, Dcl, Def, _, _, _, DDcl, DDef, _, Flags) ->
        \+ object_propertyDeclares(Entity, Dcl, DDcl, Flags, Functor/Arity, _),
        \+ object_propertyDefines(Entity, Def, DDef, Functor/Arity, _, _)
        ;	CurrentCategory_(Entity, _, Dcl, Def, _, _),
        \+ Category_propertyDeclares(Entity, Dcl, Functor/Arity, _),
        \+ Category_propertyDefines(Entity, Def, Functor/Arity, _, _)
        ),
        missingReference(Entity, Location, Reference).

        missing_predicate((':(Module,Predicate))-Reference) :-
        prologFeature(modules, supported),
        entity_property_(Entity, calls(':(Module,Predicate), _, _, _, Location)),
   //the module may only be known at runtime; reject those cases
        nonvar(Module),
   //require loaded modules as the missing modules are already listed
        current_module(Module),
        \+ current_predicate(':(Module,Predicate)),
        missingReference(Entity, Location, Reference).


   //construct reference term for missing entities and predicates

        missingReference(Entity, Location, reference(Kind,Entity,Path,StartLine)) :-
   //find the entity entityKind
        (	Current_protocol_(Entity, _, _, _, _) ->
        Kind = protocol
        ;	CurrentCategory_(Entity, _, _, _, _, _) ->
        Kind = category
        ;	Current_object_(Entity, _, _, _, _, _, _, _, _, _, _),
        Kind = object
        ),
   //find the reference file and line
        (	nonvar(Location),
        Location = Path-Line ->
   //reference found in included file
        (	integer(Line) ->
        StartLine = Line
        ;	% backend Prolog system that doesn't report line numbers
        StartLine = -1
        )
        ;	% either reference found in main file or dynamically created entity
        (	entity_property_(Entity, fileLines(File,Directory,EntityLine,_)) ->
        atomConcat(Directory, File, Path)
        ;	% dynamically created entity
        Path = '',
        EntityLine = -1
        ),
        (	integer(Location) ->
        StartLine = Location
        ;	% either dynamically created entity or backend Prolog
   //system that doesn't report line numbers
        StartLine = EntityLine
        )
        ).


   //find circular dependencies for logtalk_make(circular); we only check
   //for mutual and triangular dependencies due to the computational cost

        CircularReference((Object1-Object2)-references([Path1-Line1,Path2-Line2])) :-
        Current_object_(Object1, _, _, _, _, _, _, _, _, _, _),
        Current_object_(Object2, _, _, _, _, _, _, _, _, _, _),
        Object1 \== Object2,
        functor(Object1, Functor1, Arity1),
        functor(Object2, Functor2, Arity2),
        Functor1-Arity1 @< Functor2-Arity2,
        (	entity_property_(Object1, calls(Entity2::_, _, _, _, Line1)),
        nonvar(Entity2), Entity2 = Object2,
        entity_property_(Object2, calls(Entity1::_, _, _, _, Line2)),
        nonvar(Entity1), Entity1 = Object1 ->
        true
        ;	fail
        ),
        CircularReference_object_path(Object1, Path1),
        CircularReference_object_path(Object2, Path2).

        CircularReference((Object1-Object2-Object3)-references([Path1-Line1,Path2-Line2,Path3-Line3])) :-
        Current_object_(Object1, _, _, _, _, _, _, _, _, _, _),
        Current_object_(Object2, _, _, _, _, _, _, _, _, _, _),
        Object1 \== Object2,
        Current_object_(Object3, _, _, _, _, _, _, _, _, _, _),
        Object1 \== Object3,
        Object2 \== Object3,
        functor(Object1, Functor1, Arity1),
        functor(Object2, Functor2, Arity2),
        Functor1-Arity1 @< Functor2-Arity2,
        functor(Object3, Functor3, Arity3),
        Functor2-Arity2 @< Functor3-Arity3,
        (	entity_property_(Object1, calls(Entity2::_, _, _, _, Line1)),
        nonvar(Entity2), Entity2 = Object2,
        entity_property_(Object2, calls(Entity3::_, _, _, _, Line2)),
        nonvar(Entity3), Entity3 = Object3,
        entity_property_(Object3, calls(Entity1::_, _, _, _, Line3)),
        nonvar(Entity1), Entity1 = Object1 ->
        true
        ;	fail
        ),
        CircularReference_object_path(Object1, Path1),
        CircularReference_object_path(Object2, Path2),
        CircularReference_object_path(Object3, Path3).


        CircularReference_object_path(Object, Path) :-
        (	entity_property_(Object, fileLines(File,Directory,_,_)) ->
        atomConcat(Directory, File, Path)
        ;	Path = ''
        ).



   //logtalkLoadContext(?atom, ?nonvar)
        %
   //provides access to the compilation/loading context
        %
   //this predicate is the Logtalk version of the prologLoadContext/2
   //predicate found on some compilers such as Quintus Prolog, SICStus
   //Prolog, SWI-Prolog, and YAP
        %
   //when called from initialization/1 directives, calls to this predicate
   //are resolved at compile time when the key is instantiated

        logtalkLoadContext(source, SourceFile) :-
        ppFile_pathsFlags_(_, _, SourceFile, _, _).

        logtalkLoadContext(directory, Directory) :-
        ppFile_pathsFlags_(_, Directory, _, _, _).

        logtalkLoadContext(basename, Basename) :-
        ppFile_pathsFlags_(Basename, _, _, _, _).

        logtalkLoadContext(target, ObjectFile) :-
   //full path of the generated intermediate Prolog file
        ppFile_pathsFlags_(_, _, _, ObjectFile, _).

        logtalkLoadContext(flags, Flags) :-
   //only returns the explicit flags passed in the second argument
   //of the logtalkCompile/2 and logtalkLoad/2 predicates
        ppFile_pathsFlags_(_, _, _, _, Flags).

        logtalkLoadContext(entity_name, Entity) :-
   //deprecated key; use entityIdentifier key instead
        pp_entity_(_, Entity, _, _, _).

        logtalkLoadContext(entityIdentifier, Entity) :-
        pp_entity_(_, Entity, _, _, _).

        logtalkLoadContext(entity_prefix, Prefix) :-
        pp_entity_(_, _, Prefix, _, _).

        logtalkLoadContext(entity_type, Type) :-
        (	pp_module_(_) ->
        Type = module
        ;	pp_entity_(Type, _, _, _, _)
        ).

        logtalkLoadContext(term, Term) :-
   //full file term being compiled
        pp_term_variable_namesFileLines_(Term, _, _, _).

        logtalkLoadContext(variable_names, VariableNames) :-
   //variable names for the full file term being compiled
        pp_term_variable_namesFileLines_(_, VariableNames, _, _).

        logtalkLoadContext(file, File) :-
   //when compiling terms from an included file, this key returns the full
   //path of the included file unlike the "source" key which always returns
   //the full path of the main file
        pp_term_variable_namesFileLines_(_, _, File, _).

        logtalkLoadContext(term_position, Lines) :-
   //term position of the full file term being compiled
        pp_term_variable_namesFileLines_(_, _, _, Lines).

        logtalkLoadContext(stream, Stream) :-
   //avoid a spurious choice-point with some backend Prolog compilers
        stream_property(Stream, alias(logtalkCompilerInput)), !.



   //setLogtalkFlag(+atom, +nonvar)
        %
   //sets a global flag value
        %
   //global flag values can always be overridden when compiling and loading source
   //files by using either a setLogtalkFlag/2 directive (whose scope is local to
   //the file or the entity containing it) or by passing a list of flag values in
   //the calls to the logtalkCompile/2 and logtalkLoad/2 predicates

        setLogtalkFlag(Flag, Value) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        SetLogtalkFlag(Flag, Value, ExCtx).


        SetLogtalkFlag(Flag, Value, ExCtx) :-
        Check(read_writeFlag, Flag, logtalk(setLogtalkFlag(Flag, Value), ExCtx)),
        Check(flag_value, Flag + Value, logtalk(setLogtalkFlag(Flag, Value), ExCtx)),
        SetCompilerFlag(Flag, Value).


        SetCompilerFlag(Flag, Value) :-
        retractall(CurrentFlag_(Flag, _)),
        assertz(CurrentFlag_(Flag, Value)),
   //only one of the optimize and debug flags can be turned on at the same time
        (	Flag == optimize, Value == on ->
        retractall(CurrentFlag_(debug, _)),
        assertz(CurrentFlag_(debug, off))
        ;	Flag == debug, Value == on ->
        retractall(CurrentFlag_(optimize, _)),
        assertz(CurrentFlag_(optimize, off))
        ;	true
        ),
        (	Flag == hook ->
   //pre-compile hook calls for better performance when compiling files
        Compile_hooks(Value)
        ;	true
        ).



   //currentLogtalkFlag(?atom, ?nonvar)
        %
   //tests/gets flag values

        currentLogtalkFlag(Flag, Value) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        CurrentLogtalkFlag(Flag, Value, ExCtx).


        CurrentLogtalkFlag(Flag, Value, ExCtx) :-
        (	var(Flag) ->
   //enumerate, by backtracking, existing flags
        (	validFlag(Flag)
        ;	userDefinedFlag_(Flag, _, _)
        ),
        CompilerFlag(Flag, Value)
        ;	validFlag(Flag) ->
        CompilerFlag(Flag, Value)
        ;	userDefinedFlag_(Flag, _, _) ->
        CompilerFlag(Flag, Value)
        ;	% invalid flag; generate error
        Check(flag, Flag, logtalk(currentLogtalkFlag(Flag, Value), ExCtx))
        ).



   //createLogtalkFlag(+atom, +ground, +list)
        %
   //creates a new flag
        %
   //based on the specification of the create_prologFlag/3
   //built-in predicate of SWI-Prolog

        createLogtalkFlag(Flag, Value, Options) :-
        executionContext(ExCtx, user, user, user, user, [], []),
        CreateLogtalkFlag(Flag, Value, Options, ExCtx).


        CreateLogtalkFlag(Flag, Value, Options, ExCtx) :-
        Check(atom, Flag, logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx)),
        Check(ground, Value, logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx)),
        Check(ground, Options, logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx)),
        Check(list, Options, logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx)),
        fail.

        CreateLogtalkFlag(Flag, Value, Options, ExCtx) :-
        validFlag(Flag),
        throw(error(permission_error(modify,flag,Flag), logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx))).

        CreateLogtalkFlag(Flag, Value, Options, ExCtx) :-
        member(Option, Options),
        Option \= access(_),
        Option \= keep(_),
        Option \= entityKind(_),
        throw(error(domain_error(flag_option,Option), logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx))).

        CreateLogtalkFlag(Flag, Value, Options, ExCtx) :-
        member(access(Access), Options),
        Access \== read_write,
        Access \== read_only,
        throw(error(domain_error(flag_option,access(Access)), logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx))).

        CreateLogtalkFlag(Flag, Value, Options, ExCtx) :-
        member(keep(Keep), Options),
        Keep \== true,
        Keep \== false,
        throw(error(domain_error(flag_option,keep(Keep)), logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx))).

        CreateLogtalkFlag(Flag, Value, Options, ExCtx) :-
        member(entityKind(Type0), Options),
        (	map_userDefinedFlag_type(Type0, Type) ->
        (	call(Type, Value) ->
        fail
        ;	throw(error(type_error(Type0,Value), logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx)))
        )
        ;	throw(error(domain_error(flag_option,entityKind(Type0)), logtalk(createLogtalkFlag(Flag, Value, Options), ExCtx)))
        ).

        CreateLogtalkFlag(Flag, _, Options, _) :-
        userDefinedFlag_(Flag, _, _),
        member(keep(true), Options),
        !.

        CreateLogtalkFlag(Flag, Value, Options, _) :-
        (	member(access(Access), Options) ->
        true
        ;	Access = read_write
        ),
        (	member(entityKind(Type0), Options) ->
        map_userDefinedFlag_type(Type0, Type)
        ;	% infer entityKind from the initial value
        Value == true ->
        Type = Is_boolean'
        ;	Value == false ->
        Type = Is_boolean'
        ;	atom(Value) ->
        Type = atom
        ;	integer(Value) ->
        Type = integer
        ;	float(Value) ->
        Type = float
        ;	% catchall
        Type = ground
        ),
        retractall(userDefinedFlag_(Flag, _, _)),
        assertz(userDefinedFlag_(Flag, Access, Type)),
        retractall(CurrentFlag_(Flag, _)),
        assertz(CurrentFlag_(Flag, Value)).


   //map the flag entityKind to a closure that can be called with the flag
   //value as argument for entityKind-checking
        map_userDefinedFlag_type(boolean, Is_boolean').
        map_userDefinedFlag_type(atom, atom).
        map_userDefinedFlag_type(integer, integer).
        map_userDefinedFlag_type(float, float).
        map_userDefinedFlag_type(term, ground).



   //versionData(?compound)
        %
   //current Logtalk version for use with the currentLogtalkFlag/2 predicate
        %
   //the last argument is an atom: 'aN' for alpha versions, 'bN' for beta
   //versions, 'rcN' for release candidates (with N being a natural number),
   //and 'stable' for stable versions

        versionData(logtalk(3, 23, 0, b03)).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // built-in methods
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //object_exists(@var, +callable, +executionContext)
   //object_exists(+objectIdentifier, +callable, +executionContext)
        %
   //checks if an object exists at runtime; this is necessary in order to
   //prevent trivial messages such as true/0 or repeat/0 from succeeding
   //when the target object doesn't exist; used in the compilation of ::/2
   //calls

        object_exists(Obj, Pred, ExCtx) :-
        (	var(Obj) ->
        throw(error(instantiation_error, logtalk(Obj::Pred, ExCtx)))
        ;	Current_object_(Obj, _, _, _, _, _, _, _, _, _, _) ->
        true
        ;	% we have already verified that we have is a valid object identifier
   //when we generated calls to this predicate
        throw(error(existence_error(object, Obj), logtalk(Obj::Pred, ExCtx)))
        ).



   //Current_op(+objectIdentifier, ?operator_priority, ?operatorSpecifier, ?atom, +objectIdentifier, +scope, @executionContext)
        %
   //current_op/3 built-in method
        %
   //local operator declarations without a scope declaration are invisible

        Current_op(Obj, Priority, Specifier, Operator, Sender, Scope, ExCtx) :-
        Check(object, Obj, logtalk(Obj::current_op(Priority, Specifier, Operator), ExCtx)),
        Check(var_or_operator_priority, Priority, logtalk(current_op(Priority, Specifier, Operator), ExCtx)),
        Check(var_or_operatorSpecifier, Specifier, logtalk(current_op(Priority, Specifier, Operator), ExCtx)),
        Check(var_orAtom, Operator, logtalk(current_op(Priority, Specifier, Operator), ExCtx)),
        (	Obj == user ->
        current_op(Priority, Specifier, Operator)
        ;	entity_property_(Obj, op(Priority, Specifier, Operator, OpScope)),
   //don't return local operator declarations
        OpScope \== l,
   //check that the operator declaration is within the scope of the caller
        \+ \+ (OpScope = Scope; Obj = Sender)
        ;	% also return global operators that aren't overridden by entity operators
        current_op(Priority, Specifier, Operator),
        \+ (
        entity_property_(Obj, op(_, OtherSpecifier, Operator, _)),
        Same_operatorClass(Specifier, OtherSpecifier)
        )
        ).



   //Current_predicate(+objectIdentifier, ?predicateIndicator, +objectIdentifier, +scope, @executionContext)
        %
   //current_predicate/1 built-in method
        %
   //local predicates without a scope declaration are invisible

        Current_predicate(Obj, Pred, _, _, ExCtx) :-
        Check(var_or_predicateIndicator, Pred, logtalk(current_predicate(Pred), ExCtx)),
        Check(object, Obj, logtalk(Obj::current_predicate(Pred), ExCtx)),
        fail.

        Current_predicate(user, Pred, _, _, _) :-
        !,
        current_predicate(Pred).

        Current_predicate(Obj, Functor/Arity, Sender, LookupScope, _) :-
        ground(Functor/Arity),
        !,
   //make the current_predicate/1 method deterministic when its argument is ground
        Current_object_(Obj, _, Dcl, _, _, _, _, _, _, _, _),
        (	call(Dcl, Pred, PredScope, _, _, SCtn, _),
        functor(Pred, Functor, Arity) ->
   //commit to the first solution found as an inherited
   //predicate can always be re-declared
        (	\+ \+ PredScope = LookupScope ->
        true
        ;	Sender = SCtn
        )
        ;	fail
        ).

        Current_predicate(Obj, Functor/Arity, Sender, LookupScope, _) :-
        Current_object_(Obj, _, Dcl, _, _, _, _, _, _, _, _),
   //use findall/3 + sort/2 to avoid a setof/3 call with five
   //existentially-qualified variables or an auxiliary predicate
        findall(Functor/Arity, (call(Dcl, Pred, _, _, _, _, _), functor(Pred, Functor, Arity)), Preds),
        sort(Preds, SortedPreds),
        member(Functor/Arity, SortedPreds),
        functor(Pred, Functor, Arity),
        (	call(Dcl, Pred, PredScope, _, _, SCtn, _) ->
   //commit to the first solution found as an inherited
   //predicate can always be re-declared
        (	\+ \+ PredScope = LookupScope ->
        true
        ;	Sender = SCtn
        )
        ;	fail
        ).



   //predicate_property(+objectIdentifier, @callable, ?predicate_property, +objectIdentifier, +scope, @executionContext)
        %
   //predicate_property/2 built-in method
        %
   //local predicates without a scope declaration are invisible and Prolog
   //built-in predicates are interpreted as private predicates
        %
   //the implementation ensures that no spurious choice-points are created when
   //the method is called with a bound and deterministic property argument

        predicate_property(Obj, Pred, Prop, _, _, ExCtx) :-
        Check(callable, Pred, logtalk(predicate_property(Pred, Prop), ExCtx)),
        Check(var_or_predicate_property, Prop, logtalk(predicate_property(Pred, Prop), ExCtx)),
        Check(object, Obj, logtalk(Obj::predicate_property(Pred, Prop), ExCtx)),
        fail.

        predicate_property(user, Pred, Prop, _, _, _) :-
        !,
        predicate_property(Pred, Prop).

        predicate_property(Obj, Pred, Prop, Sender, LookupScope, _) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, _, Rnm, ObjFlags),
        call(Dcl, Pred, PredScope, Meta, PredFlags, SCtn, TCtn),
   //predicate declaration found
        !,
        (	\+ \+ PredScope = LookupScope ->
        true
        ;	Sender = SCtn
        ),
   //query is within scope
        Scope(ScopeAsAtom, PredScope),
        (	Current_object_(TCtn, _, TCtnDcl, _, _, _, _, _, _, _, _) ->
        true
        ;	Current_protocol_(TCtn, _, TCtnDcl, _, _) ->
        true
        ;	CurrentCategory_(TCtn, _, TCtnDcl, _, _, _)
        ),
        (	call(TCtnDcl, Pred, _, _, _) ->
   //found static declaration for the predicate
        predicate_property_user(Prop, Pred, Pred, Obj, ScopeAsAtom, Meta, PredFlags, TCtn, Obj, Def, Rnm)
        ;	PredFlags /\ 2 =:= 2 ->
   //dynamically declared predicate; aliases can only be defined for staticly declared predicates
        predicate_property_user(Prop, Pred, Pred, Obj, ScopeAsAtom, Meta, PredFlags, TCtn, Obj, Def, Rnm)
        ;	% no predicate declaration; we may be querying properties of a predicate alias
        Find_original_predicate(Obj, Rnm, ObjFlags, Pred, Original, Entity),
        predicate_property_user(Prop, Pred, Original, Entity, ScopeAsAtom, Meta, PredFlags, TCtn, Obj, Def, Rnm)
        ).

        predicate_property(Obj, Pred, Prop, Sender, LookupScope, _) :-
        builtIn_method(Pred, PredScope, Meta, Flags),
        !,
        (	\+ \+ PredScope = LookupScope ->
        true
        ;	Sender = Obj
        ),
        Scope(ScopeAsAtom, PredScope),
        predicate_property_builtIn_method(Prop, Pred, ScopeAsAtom, Meta, Flags).

        predicate_property(Obj, Pred, Prop, Obj, _, _) :-
        Logtalk_builtIn_predicate(Pred, Meta),
        !,
        predicate_propertyLogtalk_builtIn(Prop, Meta).

        predicate_property(Obj, Pred, Prop, Obj, _, _) :-
        prolog_builtIn_predicate(Pred),
        !,
        predicate_property_prolog_builtIn(Prop, Pred).


        predicate_property_user(alias_of(Original), Alias, Original, _, _, _, _, _, _, _, _) :-
        Alias \= Original.
        predicate_property_user(aliasDeclaredIn(Entity), Alias, Original, Entity, _, _, _, _, _, _, _) :-
        Alias \= Original.
        predicate_property_user(aliasDeclaredIn(Entity, Line), Alias, Original, Entity, _, _, _, _, _, _, _) :-
        Alias \= Original,
        functor(Original, OriginalFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        entity_property_(Entity, alias(_, OriginalFunctor/Arity, AliasFunctor/Arity, _, Line)).
        predicate_property_user(logtalk, _, _, _, _, _, _, _, _, _, _).
        predicate_property_user(scope(Scope), _, _, _, Scope, _, _, _, _, _, _).
        predicate_property_user((public), _, _, _, (public), _, _, _, _, _, _).
        predicate_property_user(protected, _, _, _, protected, _, _, _, _, _, _).
        predicate_property_user((private), _, _, _, (private), _, _, _, _, _, _).
        predicate_property_user((dynamic), _, _, _, _, _, Flags, _, _, _, _) :-
        Flags /\ 2 =:= 2.
        predicate_property_user(static, _, _, _, _, _, Flags, _, _, _, _) :-
        Flags /\ 2 =\= 2.
        predicate_property_user(declaredIn(TCtn), _, _, _, _, _, _, TCtn, _, _, _).
        predicate_property_user(declaredIn(TCtn, Line), _, Original, _, _, _, _, TCtn, _, _, _) :-
        functor(Original, Functor, Arity),
        (	predicate_property_(TCtn, Functor/Arity, declarationLocation(Line)) ->
        true
        ;	fail
        ).
        predicate_property_user(meta_predicate(Meta), Alias, _, _, _, Meta0, _, _, _, _, _) :-
        Meta0 \== no,
        functor(Alias, AliasFunctor, _),
        Meta0 =.. [_| MetaArgs],
        Meta =.. [AliasFunctor| MetaArgs].
        predicate_property_user(coinductive(Template), Alias, Original, _, _, _, _, TCtn, _, _, _) :-
        functor(Original, Functor, Arity),
        (	predicate_property_(TCtn, Functor/Arity, coinductive(Template0)) ->
        functor(Alias, AliasFunctor, _),
        Template0 =.. [_| ModeArgs],
        Template =.. [AliasFunctor| ModeArgs]
        ;	fail
        ).
        predicate_property_user((multifile), _, _, _, _, _, PredFlags, _, _, _, _) :-
        PredFlags /\ 16 =:= 16.
        predicate_property_user(non_terminal(Functor//Arity), Alias, _, _, _, _, PredFlags, _, _, _, _) :-
        PredFlags /\ 8 =:= 8,
        functor(Alias, Functor, ExtArity),
        Arity is ExtArity - 2.
        predicate_property_user(synchronized, _, _, _, _, _, PredFlags, _, _, _, _) :-
        PredFlags /\ 4 =:= 4.
        predicate_property_user(definedIn(DCtn), Alias, _, _, _, _, _, _, _, Def, _) :-
        (	call(Def, Alias, _, _, _, DCtn) ->
        true
        ;	fail
        ).
        predicate_property_user(definedIn(DCtn, Line), Alias, Original, _, _, _, _, _, _, Def, _) :-
        (	call(Def, Alias, _, _, _, DCtn) ->
        (	functor(Original, Functor, Arity),
        predicate_property_(DCtn, Functor/Arity, flagsClausesRulesLocation(_, _, _, Line)) ->
        true
        ;	fail
        )
        ;	fail
        ).
        predicate_property_user(inline, Alias, Original, _, _, _, _, _, _, Def, _) :-
        (	call(Def, Alias, _, _, _, DCtn) ->
        (	functor(Original, Functor, Arity),
        predicate_property_(DCtn, Functor/Arity, flagsClausesRulesLocation(Flags, _, _, _)) ->
        Flags /\ 4 =:= 4
        ;	fail
        )
        ;	fail
        ).
        predicate_property_user(redefinedFrom(Super), Alias, _, _, _, _, _, _, Obj, Def, _) :-
        (	call(Def, Alias, _, _, _, DCtn) ->
        Find_overridden_predicate(DCtn, Obj, Alias, Super)
        ;	fail
        ).
        predicate_property_user(redefinedFrom(Super, Line), Alias, Original, _, _, _, _, _, Obj, Def, _) :-
        (	call(Def, Alias, _, _, _, DCtn) ->
        (	Find_overridden_predicate(DCtn, Obj, Alias, Super),
        functor(Original, Functor, Arity),
        predicate_property_(Super, Functor/Arity, flagsClausesRulesLocation(_, _, _, Line)) ->
        true
        ;	fail
        )
        ;	fail
        ).
        predicate_property_user(info(Info), _, Original, _, _, _, _, TCtn, _, _, _) :-
        functor(Original, Functor, Arity),
        (	predicate_property_(TCtn, Functor/Arity, info(Info)) ->
        true
        ;	fail
        ).
        predicate_property_user(mode(Mode, Solutions), Alias, Original, _, _, _, _, TCtn, _, _, _) :-
        functor(Original, Functor, Arity),
   //we cannot make the mode/2 property deterministic as a predicate can support several different modes
        predicate_property_(TCtn, Functor/Arity, mode(Mode0, Solutions)),
        functor(Alias, AliasFunctor, _),
        Mode0 =.. [_| ModeArgs],
        Mode =.. [AliasFunctor| ModeArgs].
        predicate_property_user(number_ofClauses(N), Alias, Original, _, _, _, PredFlags, _, Obj, Def, _) :-
        Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 2 =:= 0,
   //static object
        (	call(Def, Alias, _, _, _, DCtn) ->
        functor(Original, Functor, Arity),
        (	predicate_property_(DCtn, Functor/Arity, flagsClausesRulesLocation(_, N0, _, _)) ->
        true
        ;	N0 is 0
        ),
        (	PredFlags /\ 16 =:= 16 ->
   //multifile predicate
        findall(N1, predicate_property_(DCtn, Functor/Arity, clausesRulesLocationFrom(N1, _, _, _)), N1s),
        SumList([N0| N1s], N)
        ;	N is N0
        )
        ;	fail
        ).
        predicate_property_user(number_ofRules(N), Alias, Original, _, _, _, PredFlags, _, Obj, Def, _) :-
        Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 2 =:= 0,
   //static object
        (	call(Def, Alias, _, _, _, DCtn) ->
        functor(Original, Functor, Arity),
        (	predicate_property_(DCtn, Functor/Arity, flagsClausesRulesLocation(_, _, N0, _)) ->
        true
        ;	N0 is 0
        ),
        (	PredFlags /\ 16 =:= 16 ->
   //multifile predicate
        findall(N1, predicate_property_(DCtn, Functor/Arity, clausesRulesLocationFrom(_, N1, _, _)), N1s),
        SumList([N0| N1s], N)
        ;	N is N0
        )
        ;	fail
        ).


        predicate_property_builtIn_method(logtalk, _, _, _, _).
        predicate_property_builtIn_method(scope(Scope), _, Scope, _, _).
        predicate_property_builtIn_method((public), _, (public), _, _).
        predicate_property_builtIn_method(protected, _, protected, _, _).
        predicate_property_builtIn_method((private), _, (private), _, _).
        predicate_property_builtIn_method(builtIn, _, _, _, _).	%Flags /\ 1 =:= 1.
        predicate_property_builtIn_method((dynamic), _, _, _, Flags) :-
        Flags /\ 2 =:= 2.
        predicate_property_builtIn_method(static, _, _, _, Flags) :-
        Flags /\ 2 =\= 2.
        predicate_property_builtIn_method(meta_predicate(Meta), _, _, Meta, _) :-
        Meta \== no.
        predicate_property_builtIn_method((multifile), _, _, _, Flags) :-
        Flags /\ 16 =:= 16.
        predicate_property_builtIn_method(non_terminal(Functor//Arity), Pred, _, _, Flags) :-
        Flags /\ 8 =:= 8,
        functor(Pred, Functor, ExtArity),
        Arity is ExtArity - 2.
        predicate_property_builtIn_method(synchronized, _, _, _, Flags) :-
        Flags /\ 4 =:= 4.


        predicate_propertyLogtalk_builtIn(logtalk, _).
        predicate_propertyLogtalk_builtIn(scope(private), _).
        predicate_propertyLogtalk_builtIn((private), _).
        predicate_propertyLogtalk_builtIn(builtIn, _).
        predicate_propertyLogtalk_builtIn(static, _).
        predicate_propertyLogtalk_builtIn(meta_predicate(Meta), Meta) :-
        Meta \== no.


        predicate_property_prolog_builtIn(foreign, Pred) :-
        catch(predicate_property(Pred, foreign), _, fail).
        predicate_property_prolog_builtIn(prolog, Pred) :-
        \+ catch(predicate_property(Pred, foreign), _, fail).
        predicate_property_prolog_builtIn(scope(private), _).
        predicate_property_prolog_builtIn((private), _).
        predicate_property_prolog_builtIn(meta_predicate(Meta), Pred) :-
        prolog_meta_predicate(Pred, Meta0, _),
        Meta0 =.. [_| MetaArgs0],
        prolog_toLogtalk_metaArgumentSpecifiers(MetaArgs0, MetaArgs),
        Meta =.. [_| MetaArgs].
        predicate_property_prolog_builtIn(builtIn, _).
        predicate_property_prolog_builtIn((dynamic), Pred) :-
        predicate_property(Pred, (dynamic)).
        predicate_property_prolog_builtIn(static, Pred) :-
        predicate_property(Pred, static).
        predicate_property_prolog_builtIn((multifile), Pred) :-
        predicate_property(Pred, (multifile)).



   //Scope(?atom, ?nonvar).
        %
   //converts between user and internal scope representation;
   //this representation was chosen as it allows testing if a scope is either
   //public or protected by a single unification step with the p(_) term

        Scope((private), p).
        Scope(protected, p(p)).
        Scope((public),  p(p(p))).



   //FilterScope(@nonvar, -nonvar)
        %
   //filters the predicate scope;
   //used in the implementation of protected-qualified relations between entities;
   //public predicates become protected predicates, protected and private predicates
   //are unaffected

        FilterScope(p(_), p(p)).
        FilterScope(p, p).



   //FilterScopeContainer(@nonvar, @objectIdentifier, @objectIdentifier, -objectIdentifier)
        %
   //filters the predicate scope container;
   //used in the implementation of private-qualified relations between entities;
   //when the predicate is public or protected, the object inheriting the predicate
   //becomes the scope container; when the predicate is private, the scope container
   //is the inherited scope container

        FilterScopeContainer(p(_), _, SCtn, SCtn).
        FilterScopeContainer(p, SCtn, _, SCtn).



   //Find_original_predicate(+objectIdentifier, +atom, +integer, +callable, -callable, -entityIdentifier)
        %
   //finds the predicate pointed by an alias and the entity where the alias is declared

        Find_original_predicate(Obj, Rnm, Flags, Alias, Pred, Entity) :-
   //we add a fifth argument to properly handle class hierarchies if necessary
        Find_original_predicate(Obj, Rnm, Flags, Alias, Pred, Entity, _).


        Find_original_predicate(Obj, _, Flags, Alias, Pred, Entity, _) :-
        Flags /\ 64 =:= 64,		% "complements" flag set to "allow"
        Complemented_object_(Obj, Ctg, _, _, Rnm),
        Find_original_predicate(Ctg, Rnm, 0, Alias, Pred, Entity, _).

        Find_original_predicate(Entity, Rnm, _, Alias, Pred, Entity, _) :-
        once(call(Rnm, _, Pred, Alias)),
        Pred \= Alias,
        !.

        Find_original_predicate(Obj, _, Flags, Alias, Pred, Entity, _) :-
        Flags /\ 32 =:= 32,		% "complements" flag set to "restrict"
        Complemented_object_(Obj, Ctg, _, _, Rnm),
        Find_original_predicate(Ctg, Rnm, 0, Alias, Pred, Entity, _).

        Find_original_predicate(Obj, _, _, Alias, Pred, Entity, _) :-
        Implements_protocol_(Obj, Ptc, _),
        Current_protocol_(Ptc, _, _, Rnm, _),
        Find_original_predicate(Ptc, Rnm, 0, Alias, Pred, Entity, _).

        Find_original_predicate(Ptc, _, _, Alias, Pred, Entity, _) :-
        extends_protocol_(Ptc, ExtPtc, _),
        Current_protocol_(ExtPtc, _, _, Rnm, _),
        Find_original_predicate(ExtPtc, Rnm, 0, Alias, Pred, Entity, _).

        Find_original_predicate(Ctg, _, _, Alias, Pred, Entity, _) :-
        extendsCategory_(Ctg, ExtCtg, _),
        CurrentCategory_(ExtCtg, _, _, _, Rnm, _),
        Find_original_predicate(ExtCtg, Rnm, 0, Alias, Pred, Entity, _).

        Find_original_predicate(Obj, _, _, Alias, Pred, Entity, _) :-
        ImportsCategory_(Obj, Ctg, _),
        CurrentCategory_(Ctg, _, _, _, Rnm, _),
        Find_original_predicate(Ctg, Rnm, 0, Alias, Pred, Entity, _).

        Find_original_predicate(Obj, _, _, Alias, Pred, Entity, prototype) :-
        extends_object_(Obj, Parent, _),
        Current_object_(Parent, _, _, _, _, _, _, _, _, Rnm, Flags),
        Find_original_predicate(Parent, Rnm, Flags, Alias, Pred, Entity, prototype).

        Find_original_predicate(Instance, _, _, Alias, Pred, Entity, instance) :-
        InstantiatesClass_(Instance, Class, _),
        Current_object_(Class, _, _, _, _, _, _, _, _, Rnm, Flags),
        Find_original_predicate(Class, Rnm, Flags, Alias, Pred, Entity, superclass).

        Find_original_predicate(Class, _, _, Alias, Pred, Entity, superclass) :-
        SpecializesClass_(Class, Superclass, _),
        Current_object_(Superclass, _, _, _, _, _, _, _, _, Rnm, Flags),
        Find_original_predicate(Superclass, Rnm, Flags, Alias, Pred, Entity, superclass).



   //Find_overridden_predicate(+entityIdentifier, +entityIdentifier, +callable, -entityIdentifier)
        %
   //finds the entity containing the overridden predicate definition (assuming that the
   //start lookup entity contains a overriding definition for the predicate)

        Find_overridden_predicate(Obj, Self, Pred, DefCtn) :-
        Current_object_(Obj, _, _, _, Super, _, _, _, _, _, _),
   //for classes, we need to be sure we use the correct clause for "super" by looking into "self"
        executionContext(ExCtx, _, _, _, Self, _, _),
        call(Super, Pred, ExCtx, _, _, DefCtn),
        DefCtn \= Obj,
        !.

        Find_overridden_predicate(Ctg, _, Pred, DefCtn) :-
        CurrentCategory_(Ctg, _, _, Def, _, _),
        call(Def, Pred, _, _, DefCtn),
        DefCtn \= Ctg,
        !.



   //Abolish(+objectIdentifier, +predicateIndicator, +objectIdentifier, +scope, @executionContext)
        %
   //abolish/1 built-in method

        Abolish(Obj, Pred, Sender, TestScope, ExCtx) :-
        Check(objectIdentifier, Obj, logtalk(Obj::abolish(Pred), ExCtx)),
        Check(predicateIndicator, Pred, logtalk(abolish(Pred), ExCtx)),
        AbolishChecked(Obj, Pred, Sender, TestScope, ExCtx).


        AbolishChecked(user, Functor/Arity, _, _, _) :-
        !,
        abolish(Functor/Arity).

        AbolishChecked(Obj, Functor/Arity, Sender, TestScope, ExCtx) :-
        Current_object_(Obj, Prefix, Dcl, _, _, _, _, DDcl, DDef, _, ObjFlags),
        !,
        functor(Pred, Functor, Arity),
        (	call(Dcl, Pred, Scope, _, PredFlags) ->
   //local static predicate declaration found
        (	(Scope = TestScope; Sender = Obj) ->
   //predicate is within the scope of the sender
        (	PredFlags /\ 2 =:= 2 ->
   //static declaration for a dynamic predicate
        throw(error(permission_error(modify, predicateDeclaration, Functor/Arity), logtalk(abolish(Functor/Arity), ExCtx)))
        ;	% predicate is static
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(abolish(Functor/Arity), ExCtx)))
        )
        ;	% predicate is not within the scope of the sender
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(abolish(Functor/Arity), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(abolish(Functor/Arity), ExCtx)))
        )
        )
        ;	% no static predicate declaration...
        ObjFlags /\ 128 =:= 128,
   //... but dynamic declarations are allowed
        DDclClause =.. [DDcl, Pred, _],
        call(DDclClause) ->
   //dynamic predicate declaration found
        retractall(DDclClause),
        DDefClause =.. [DDef, Pred, _, TPred0],
        (	call(DDefClause) ->
   //predicate clauses exist
        unwrapCompiled_head(TPred0, TPred),
        functor(TPred, TFunctor, TArity),
        abolish(TFunctor/TArity),
        retractall(DDefClause),
        CleanLookupCaches(Pred)
        ;	% no predicate clauses currently exist but may have existed in the past
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        abolish(TFunctor/TArity)
        )
        ;	% no dynamic predicate declaration found
        DDefClause =.. [DDef, Pred, _, TPred0],
        call(DDefClause) ->
   //local dynamic predicate
        unwrapCompiled_head(TPred0, TPred),
        functor(TPred, TFunctor, TArity),
        abolish(TFunctor/TArity),
        retractall(DDefClause),
        CleanLookupCaches(Pred)
        ;	% no predicate declaration
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(abolish(Functor/Arity), ExCtx)))
        ).

        AbolishChecked(Obj, Pred, _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::abolish(Pred), ExCtx))).



   //Asserta(+objectIdentifier, @clause, +objectIdentifier, +scope, +scope, @executionContext)
        %
   //asserta/1 built-in method
        %
   //asserting facts uses a caching mechanism that saves the compiled form of the
   //facts to improve performance

        Asserta(Obj, Clause, Sender, _, _, _) :-
        nonvar(Obj),
        nonvar(Clause),
        DbLookupCache_(Obj, Clause, Sender, TClause, _),
        !,
        asserta(TClause).

        Asserta(Obj, Clause, Sender, TestScope, DclScope, ExCtx) :-
        Check(objectIdentifier, Obj, logtalk(Obj::asserta(Clause), ExCtx)),
        Check(clause, Clause, logtalk(asserta(Clause), ExCtx)),
        (	Clause = (Head :- Body) ->
        (	Body == true ->
        AssertaFactChecked(Obj, Head, Sender, TestScope, DclScope, ExCtx)
        ;	AssertaRuleChecked(Obj, Clause, Sender, TestScope, DclScope, ExCtx)
        )
        ;	AssertaFactChecked(Obj, Clause, Sender, TestScope, DclScope, ExCtx)
        ).


        AssertaRuleChecked(Obj, (Head:-Body), Sender, TestScope, DclScope, ExCtx) :-
        Current_object_(Obj, Prefix, Dcl, Def, _, _, _, DDcl, DDef, _, Flags),
        !,
        Assert_predDcl(Obj, Dcl, DDcl, DDef, Flags, Head, Scope, Type, Meta, SCtn, DclScope, asserta((Head:-Body)), ExCtx),
        (	(Type == (dynamic); Flags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        (	(Scope = TestScope; Sender = SCtn) ->
        Assert_predDef(Def, DDef, Flags, Prefix, Head, GExCtx, THead, _),
        goal_metaArguments(Meta, Head, MetaArgs),
        CompCtx(Ctx, Head, GExCtx, _, _, _, _, Prefix, MetaArgs, _, GExCtx, runtime, _, _),
        Compile_body(Body, TBody, DBody, Ctx),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        asserta((THead :- (nop(Body), Debug(rule(Obj, Head, 0, nil, 0), GExCtx), DBody)))
        ;	asserta((THead :- (nop(Body), TBody)))
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(asserta((Head:-Body)), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(asserta((Head:-Body)), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(asserta((Head:-Body)), ExCtx)))
        ).

        AssertaRuleChecked(Obj, Clause, _, _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), Obj::asserta(Clause), ExCtx)).


        AssertaFactChecked(Obj, Head, Sender, _, _, _) :-
        DbLookupCache_(Obj, Head, Sender, THead, _),
        !,
        asserta(THead).

        AssertaFactChecked(Obj, Head, Sender, TestScope, DclScope, ExCtx) :-
        Current_object_(Obj, Prefix, Dcl, Def, _, _, _, DDcl, DDef, _, Flags),
        !,
        Assert_predDcl(Obj, Dcl, DDcl, DDef, Flags, Head, Scope, Type, _, SCtn, DclScope, asserta(Head), ExCtx),
        (	(Type == (dynamic); Flags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        (	(Scope = TestScope; Sender = SCtn) ->
        Assert_predDef(Def, DDef, Flags, Prefix, Head, GExCtx, THead, Update),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        asserta((THead :- Debug(fact(Obj, Head, 0, nil, 0), GExCtx)))
        ;	AddDbLookupCache_entry(Obj, Head, SCtn, DclScope, Type, Sender, THead, DDef, Update),
        asserta(THead)
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(asserta(Head), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(asserta(Head), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(asserta(Head), ExCtx)))
        ).

        AssertaFactChecked(Obj, Head, _, _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::asserta(Head), ExCtx))).



   //Assertz(+objectIdentifier, @clause, +objectIdentifier, +scope, +scope, @executionContext)
        %
   //assertz/1 built-in method
        %
   //asserting facts uses a caching mechanism that saves the compiled form of the
   //facts to improve performance

        Assertz(Obj, Clause, Sender, _, _, _) :-
        nonvar(Obj),
        nonvar(Clause),
        DbLookupCache_(Obj, Clause, Sender, TClause, _),
        !,
        assertz(TClause).

        Assertz(Obj, Clause, Sender, TestScope, DclScope, ExCtx) :-
        Check(objectIdentifier, Obj, logtalk(Obj::assertz(Clause), ExCtx)),
        Check(clause, Clause, logtalk(assertz(Clause), ExCtx)),
        (	Clause = (Head :- Body) ->
        (	Body == true ->
        AssertzFactChecked(Obj, Head, Sender, TestScope, DclScope, ExCtx)
        ;	AssertzRuleChecked(Obj, Clause, Sender, TestScope, DclScope, ExCtx)
        )
        ;	AssertzFactChecked(Obj, Clause, Sender, TestScope, DclScope, ExCtx)
        ).


        AssertzRuleChecked(Obj, (Head:-Body), Sender, TestScope, DclScope, ExCtx) :-
        Current_object_(Obj, Prefix, Dcl, Def, _, _, _, DDcl, DDef, _, Flags),
        !,
        Assert_predDcl(Obj, Dcl, DDcl, DDef, Flags, Head, Scope, Type, Meta, SCtn, DclScope, assertz((Head:-Body)), ExCtx),
        (	(Type == (dynamic); Flags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        (	(Scope = TestScope; Sender = SCtn) ->
        Assert_predDef(Def, DDef, Flags, Prefix, Head, GExCtx, THead, _),
        goal_metaArguments(Meta, Head, MetaArgs),
        CompCtx(Ctx, Head, GExCtx, _, _, _, _, Prefix, MetaArgs, _, GExCtx, runtime, _, _),
        Compile_body(Body, TBody, DBody, Ctx),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        assertz((THead :- (nop(Body), Debug(rule(Obj, Head, 0, nil, 0), GExCtx), DBody)))
        ;	assertz((THead :- (nop(Body), TBody)))
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(assertz((Head:-Body)), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(assertz((Head:-Body)), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(assertz((Head:-Body)), ExCtx)))
        ).

        AssertzRuleChecked(Obj, Clause, _, _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::assertz(Clause), ExCtx))).


        AssertzFactChecked(Obj, Head, Sender, _, _, _) :-
        DbLookupCache_(Obj, Head, Sender, THead, _),
        !,
        assertz(THead).

        AssertzFactChecked(Obj, Head, Sender, TestScope, DclScope, ExCtx) :-
        Current_object_(Obj, Prefix, Dcl, Def, _, _, _, DDcl, DDef, _, Flags),
        !,
        Assert_predDcl(Obj, Dcl, DDcl, DDef, Flags, Head, Scope, Type, _, SCtn, DclScope, assertz(Head), ExCtx),
        (	(Type == (dynamic); Flags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        (	(Scope = TestScope; Sender = SCtn) ->
        Assert_predDef(Def, DDef, Flags, Prefix, Head, GExCtx, THead, Update),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        assertz((THead :- Debug(fact(Obj, Head, 0, nil, 0), GExCtx)))
        ;	AddDbLookupCache_entry(Obj, Head, SCtn, DclScope, Type, Sender, THead, DDef, Update),
        assertz(THead)
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(assertz(Head), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(assertz(Head), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(assertz(Head), ExCtx)))
        ).

        AssertzFactChecked(Obj, Head, _, _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::assertz(Head), ExCtx))).



   //gets or sets (if it doesn't exist) the declaration for an asserted predicate (but we must
   //not add a scope declaration when asserting clauses for a *local* dynamic predicate)

        Assert_predDcl(Obj, Dcl, DDcl, DDef, ObjFlags, Pred, Scope, Type, Meta, SCtn, DclScope, Goal, ExCtx) :-
        (	call(Dcl, Pred, Scope, Meta, PredFlags, SCtn, _) ->
   //predicate declaration found; get predicate entityKind
        (	PredFlags /\ 2 =:= 2 ->
        Type = (dynamic)
        ;	Type = (static)
        )
        ;	% no predicate declaration; check for a local dynamic predicate if we're asserting locally
        (DclScope == p, call(DDef, Pred, _, _)) ->
        Scope = DclScope, Type = (dynamic), Meta = no, SCtn = Obj
        ;	% not a declared predicate and not a local dynamic predicate
        (	DclScope == p
   //object asserting a new predicate in itself
        ;	ObjFlags /\ 128 =:= 128
   //dynamic declaration of new predicates allowed
        ) ->
        term_template(Pred, DPred),
        Clause =.. [DDcl, DPred, DclScope],
        assertz(Clause),
        Scope = DclScope, Type = (dynamic), Meta = no, SCtn = Obj
        ;	% object doesn't allow dynamic declaration of new predicates
        functor(Pred, Functor, Arity),
        throw(error(permission_error(create, predicateDeclaration, Functor/Arity), logtalk(Goal, ExCtx)))
        ).



   //gets or sets (if it doesn't exist) the compiled call for an asserted predicate

        Assert_predDef(Def, DDef, Flags, Prefix, Head, ExCtx, THead, NeedsUpdate) :-
        (	call(Def, Head, ExCtx, THead0) ->
   //static definition lookup entries don't require update goals
        unwrapCompiled_head(THead0, THead),
        NeedsUpdate = false
        ;	call(DDef, Head, ExCtx, THead0) ->
   //dynamic definition lookup entries always require update goals
        unwrapCompiled_head(THead0, THead),
        NeedsUpdate = true
        ;	% no definition lookup entry exists; construct and assert a dynamic one
        functor(Head, Functor, Arity),
        functor(GHead, Functor, Arity),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        functor(THead, TFunctor, TArity),
        unify_head_theadArguments(GHead, THead, ExCtx),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        DDefClause =.. [DDef, GHead, ExCtx, Debug(goal(GHead,THead), ExCtx)]
        ;	DDefClause =.. [DDef, GHead, ExCtx, THead]
        ),
        assertz(DDefClause),
        CleanLookupCaches(GHead),
        NeedsUpdate = true,
        GHead = Head
        ).



   //Clause(+objectIdentifier, +callable, ?callable, +objectIdentifier, +scope, @executionContext)
        %
   //clause/2 built-in method

        Clause(Obj, Head, Body, Sender, TestScope, ExCtx) :-
        Check(objectIdentifier, Obj, logtalk(Obj::clause(Head, Body), ExCtx)),
        Check(clause_or_partialClause, (Head:-Body), logtalk(clause(Head, Body), ExCtx)),
        ClauseChecked(Obj, Head, Body, Sender, TestScope, ExCtx).


        ClauseChecked(Obj, Head, Body, Sender, _, _) :-
        DbLookupCache_(Obj, Head, Sender, THead, _),
        !,
        clause(THead, TBody),
        (	TBody = (nop(Body), _) ->
   //rules (compiled both in normal and debug mode)
        true
        ;	TBody = Debug(fact(_, _, _, _, _), _) ->
   //facts compiled in debug mode
        Body = true
        ;	% facts compiled in normal mode
        TBody = Body
        ).

        ClauseChecked(Obj, Head, Body, Sender, TestScope, ExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, DDef, _, ObjFlags),
        !,
        (	call(Dcl, Head, Scope, _, PredFlags, SCtn, _) ->
        (	(PredFlags /\ 2 =:= 2; ObjFlags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        (	(Scope = TestScope; Sender = SCtn) ->
        (	(call(DDef, Head, _, THead0, HtEntityKind.P); call(Def, Head, _, THead0)) ->
        unwrapCompiled_head(THead0, THead),
        clause(THead, TBody),
        (	TBody = (nop(Body), _) ->
        true
        ;	TBody = Debug(fact(_, _, _, _, _), _) ->
        Body = true
        ;	TBody = Body
        )
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(clause(Head, Body), ExCtx)))
        ;	throw(error(permission_error(access, protected_predicate, Functor/Arity), logtalk(clause(Head, Body), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(access, static_predicate, Functor/Arity), logtalk(clause(Head, Body), ExCtx)))
        )
        ;	Obj = Sender,
        (call(DDef, Head, _, THead0, HtEntityKind.P); call(Def, Head, _, THead0)) ->
   //local dynamic predicate with no scope declaration
        unwrapCompiled_head(THead0, THead),
        clause(THead, TBody),
        (	TBody = (nop(Body), _) ->
        true
        ;	TBody = Debug(fact(_, _, _, _, _), _) ->
        Body = true
        ;	TBody = Body
        )
        ;	% unknown predicate
        functor(Head, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(clause(Head, Body), ExCtx)))
        ).

        ClauseChecked(Obj, Head, Body, _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::clause(Head, Body), ExCtx))).



   //Retract(+objectIdentifier, @clause, +objectIdentifier, +scope, @executionContext)
        %
   //retract/1 built-in method
        %
   //the implementation must ensure that retracting the last clause for a
   //predicate allows any inherited clauses to be found again as they are
   //no longer being overridden

        Retract(Obj, Clause, Sender, _, _) :-
        nonvar(Obj),
        nonvar(Clause),
        DbLookupCache_(Obj, Clause, Sender, TClause, UClause),
        !,
        retract(TClause),
        updateDdef_table_opt(UClause).

        Retract(Obj, Clause, Sender, TestScope, ExCtx) :-
        Check(objectIdentifier, Obj, logtalk(Obj::retract(Clause), ExCtx)),
        Check(clause_or_partialClause, Clause, logtalk(retract(Clause), ExCtx)),
        (	Clause = (Head :- Body) ->
        (	var(Body) ->
        Retract_var_bodyChecked(Obj, Clause, Sender, TestScope, ExCtx)
        ;	Body == true ->
        RetractFactChecked(Obj, Head, Sender, TestScope, ExCtx)
        ;	RetractRuleChecked(Obj, Clause, Sender, TestScope, ExCtx)
        )
        ;	RetractFactChecked(Obj, Clause, Sender, TestScope, ExCtx)
        ).


        Retract_var_bodyChecked(Obj, (Head:-Body), Sender, TestScope, ExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, DDef, _, ObjFlags),
        !,
        (	call(Dcl, Head, Scope, _, PredFlags, SCtn, _) ->
        (	(PredFlags /\ 2 =:= 2; ObjFlags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        (	(Scope = TestScope; Sender = SCtn) ->
        (	call(DDef, Head, _, THead0) ->
        unwrapCompiled_head(THead0, THead),
        retract((THead :- TBody)),
        (	TBody = (nop(Body), _) ->
        true
        ;	TBody = Debug(fact(_, _, _, _, _), _) ->
        Body = true
        ;	TBody = Body
        ),
        updateDdef_table(DDef, Head, THead)
        ;	call(Def, Head, _, THead0) ->
        unwrapCompiled_head(THead0, THead),
        retract((THead :- TBody)),
        (	TBody = (nop(Body), _) ->
        true
        ;	TBody = Debug(fact(_, _, _, _, _), _) ->
        Body = true
        ;	TBody = Body
        )
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(retract((Head:-Body)), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(retract((Head:-Body)), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(retract((Head:-Body)), ExCtx)))
        )
        ;	Obj = Sender,
        call(DDef, Head, _, THead0) ->
   //local dynamic predicate with no scope declaration
        unwrapCompiled_head(THead0, THead),
        retract((THead :- TBody)),
        (	TBody = (nop(Body), _) ->
        true
        ;	TBody = Debug(fact(_, _, _, _, _), _) ->
        Body = true
        ;	TBody = Body
        )
        ;	% unknown predicate
        functor(Head, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(retract((Head:-Body)), ExCtx)))
        ).

        Retract_var_bodyChecked(Obj, (Head:-Body), _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::retract((Head:-Body)), ExCtx))).


        RetractRuleChecked(Obj, (Head:-Body), Sender, TestScope, ExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, DDef, _, ObjFlags),
        !,
        (	call(Dcl, Head, Scope, _, PredFlags, SCtn, _) ->
        (	(PredFlags /\ 2 =:= 2; ObjFlags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        (	(Scope = TestScope; Sender = SCtn) ->
        (	call(DDef, Head, _, THead0) ->
        unwrapCompiled_head(THead0, THead),
        retract((THead :- (nop(Body), _))),
        updateDdef_table(DDef, Head, THead)
        ;	call(Def, Head, _, THead0) ->
        unwrapCompiled_head(THead0, THead),
        retract((THead :- (nop(Body), _)))
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(retract((Head:-Body)), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(retract((Head:-Body)), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(retract((Head:-Body)), ExCtx)))
        )
        ;	Obj = Sender,
        call(DDef, Head, _, THead0) ->
   //local dynamic predicate with no scope declaration
        unwrapCompiled_head(THead0, THead),
        retract((THead :- (nop(Body), _)))
        ;	% unknown predicate
        functor(Head, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(retract((Head:-Body)), ExCtx)))
        ).

        RetractRuleChecked(Obj, (Head:-Body), _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::retract((Head:-Body)), ExCtx))).


        RetractFactChecked(Obj, Head, Sender, _, _) :-
        DbLookupCache_(Obj, Head, Sender, THead, UClause),
        !,
        retract(THead),
        updateDdef_table_opt(UClause).

        RetractFactChecked(Obj, Head, Sender, TestScope, ExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, DDef, _, ObjFlags),
        !,
        (	call(Dcl, Head, Scope, _, PredFlags, SCtn, _) ->
        (	(PredFlags /\ 2 =:= 2; ObjFlags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        Type = (dynamic),
        (	(Scope = TestScope; Sender = SCtn) ->
        (	call(DDef, Head, _, THead0) ->
        unwrapCompiled_head(THead0, THead),
        (	ObjFlags /\ 512 =:= 512 ->
   //object compiled in debug mode
        retract((THead :- Debug(fact(_, _, _, _, _), _)))
        ;	AddDbLookupCache_entry(Obj, Head, SCtn, Scope, Type, Sender, THead, DDef, true),
        retract(THead)
        ),
        updateDdef_table(DDef, Head, THead)
        ;	call(Def, Head, _, THead0) ->
        unwrapCompiled_head(THead0, THead),
        (	ObjFlags /\ 512 =:= 512 ->
   //object compiled in debug mode
        retract((THead :- Debug(fact(_, _, _, _, _), _)))
        ;	AddDbLookupCache_entry(Obj, Head, Scope, Type, Sender, THead),
        retract(THead)
        )
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(retract(Head), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(retract(Head), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(retract(Head), ExCtx)))
        )
        ;	Obj = Sender,
        call(DDef, Head, _, THead0) ->
   //local dynamic predicate with no scope declaration
        unwrapCompiled_head(THead0, THead),
        (	ObjFlags /\ 512 =:= 512 ->
   //object compiled in debug mode
        retract((THead :- Debug(fact(_, _, _, _, _), _)))
        ;	AddDbLookupCache_entry(Obj, Head, p, (dynamic), Sender, THead),
        retract(THead)
        )
        ;	% unknown predicate
        functor(Head, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(retract(Head), ExCtx)))
        ).

        RetractFactChecked(Obj, Head, _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::retract(Head), ExCtx))).



   //Retractall(+objectIdentifier, @callable, +objectIdentifier, +scope, @executionContext)
        %
   //retractall/1 built-in method
        %
   //the implementation must ensure that retracting the last clause for a
   //predicate allows any inherited clauses to be found again as they are
   //no longer being overridden

        Retractall(Obj, Head, Sender, _, _) :-
        nonvar(Obj),
        nonvar(Head),
        DbLookupCache_(Obj, Head, Sender, THead, UClause),
        !,
        retractall(THead),
        updateDdef_table_opt(UClause).

        Retractall(Obj, Head, Sender, TestScope, ExCtx) :-
        Check(objectIdentifier, Obj, logtalk(Obj::retractall(Head), ExCtx)),
        Check(callable, Head, logtalk(retractall(Head), ExCtx)),
        RetractallChecked(Obj, Head, Sender, TestScope, ExCtx).


        RetractallChecked(Obj, Head, Sender, _, _) :-
        DbLookupCache_(Obj, Head, Sender, THead, UClause),
        !,
        retractall(THead),
        updateDdef_table_opt(UClause).

        RetractallChecked(Obj, Head, Sender, TestScope, ExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, DDef, _, ObjFlags),
        !,
        (	call(Dcl, Head, Scope, _, PredFlags, SCtn, _) ->
   //predicate scope declaration found
        (	(PredFlags /\ 2 =:= 2; ObjFlags /\ 2 =:= 2, Sender = SCtn) ->
   //either a dynamic predicate or a dynamic object that is both the sender and the predicate scope container
        Type = (dynamic),
        (	(Scope = TestScope; Sender = SCtn) ->
        (	call(DDef, Head, _, THead0) ->
        unwrapCompiled_head(THead0, THead),
        retractall(THead),
        updateDdef_table(DDef, Head, THead)
        ;	call(Def, Head, _, THead0) ->
        unwrapCompiled_head(THead0, THead),
        (	ObjFlags /\ 512 =:= 512 ->
   //object compiled in debug mode
        true
        ;	AddDbLookupCache_entry(Obj, Head, Scope, Type, Sender, THead)
        ),
        retractall(THead)
        ;	true
        )
        ;	% predicate is not within the scope of the sender
        functor(Head, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(modify, private_predicate, Functor/Arity), logtalk(retractall(Head), ExCtx)))
        ;	throw(error(permission_error(modify, protected_predicate, Functor/Arity), logtalk(retractall(Head), ExCtx)))
        )
        )
        ;	% predicate is static
        functor(Head, Functor, Arity),
        throw(error(permission_error(modify, static_predicate, Functor/Arity), logtalk(retractall(Head), ExCtx)))
        )
        ;	Obj = Sender,
        call(DDef, Head, _, THead0) ->
   //local dynamic predicate with no scope declaration
        unwrapCompiled_head(THead0, THead),
        (	ObjFlags /\ 512 =:= 512 ->
   //object compiled in debug mode
        true
        ;	AddDbLookupCache_entry(Obj, Head, p, (dynamic), Sender, THead)
        ),
        retractall(THead)
        ;	% unknown predicate
        functor(Head, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(retractall(Head), ExCtx)))
        ).

        RetractallChecked(Obj, Head, _, _, ExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::retractall(Head), ExCtx))).



   //nop(+clause)
        %
   //used as the first goal in the body of asserted predicate clauses that are
   //rules to save the original clause body and thus support the implementation
   //of the clause/2 built-in method

        nop(_).



   //AddDbLookupCache_entry(@objectIdentifier, @callable, @callable, +atom, @objectIdentifier, @callable)
        %
   //adds a new database lookup cache entry (when an update goal is not required)

        AddDbLookupCache_entry(Obj, Head, Scope, Type, Sender, THead) :-
        term_template(Obj, GObj),
        term_template(Head, GHead),
        term_template(THead, GTHead),
        unify_head_theadArguments(GHead, GTHead, _),
        (	(Scope = p(p(p)), Type == (dynamic)) ->
        asserta(DbLookupCache_(GObj, GHead, _, GTHead, true))
        ;	term_template(Sender, GSender),
        asserta(DbLookupCache_(GObj, GHead, GSender, GTHead, true))
        ).



   //AddDbLookupCache_entry(@objectIdentifier, @callable, @callable, @callable, +atom, @objectIdentifier, @callable, +atom, +atom)
        %
   //adds a new database lookup cache entry

        AddDbLookupCache_entry(Obj, Head, SCtn, Scope, Type, Sender, THead, DDef, NeedsUpdate) :-
        term_template(Obj, GObj),
        term_template(Head, GHead),
        term_template(THead, GTHead),
        unify_head_theadArguments(GHead, GTHead, _),
        (	NeedsUpdate == true, Sender \= SCtn ->
        term_template(Head, UHead),
        term_template(THead, UTHead),
        UClause =.. [DDef, UHead, _, _],
        (	(Scope = p(p(p)), Type == (dynamic)) ->
        asserta(DbLookupCache_(GObj, GHead, _, GTHead, update(UHead, UTHead, UClause)))
        ;	term_template(Sender, GSender),
        asserta(DbLookupCache_(GObj, GHead, GSender, GTHead, update(UHead, UTHead, UClause)))
        )
        ;	(	(Scope = p(p(p)), Type == (dynamic)) ->
        asserta(DbLookupCache_(GObj, GHead, _, GTHead, true))
        ;	term_template(Sender, GSender),
        asserta(DbLookupCache_(GObj, GHead, GSender, GTHead, true))
        )
        ).



   //unify_head_theadArguments(+callable, +callable, @term)
        %
   //compiled clause heads use an extra argument for passing the execution context

        unify_head_theadArguments(Head, THead, ExCtx) :-
        Head =.. [_| Args],
        THead =.. [_| TArgs],
        Append(Args, [ExCtx], TArgs).



   //phrase(+grbody, ?list, +executionContext)
        %
   //phrase/2 built-in method implementation for calls where the first argument is only known at runtime

        phrase(GRBody, Input, ExCtx) :-
        executionContext(ExCtx, Entity, Sender, This, Self, _, _),
        Check(callable, GRBody, logtalk(phrase(GRBody, Input), ExCtx)),
        Current_object_(This, Prefix, _, _, _, _, _, _, _, _, Flags),
        CompCtx(Ctx, _, ExCtx, Entity, Sender, This, Self, Prefix, [], _, ExCtx, runtime, _, _),
        catch(
        Dcg_body(GRBody, S0, S, Pred, Ctx),
        Error,
        throw(error(Error, logtalk(phrase(GRBody, Input), ExCtx)))
        ),
        Compile_body(Pred, TPred, DPred, Ctx),
        Input = S0, [] = S,
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        call(DPred)
        ;	call(TPred)
        ).



   //phrase(+grbody, ?list, ?list, +executionContext)
        %
   //phrase/3 built-in method implementation for calls where the first argument is only known at runtime

        phrase(GRBody, Input, Rest, ExCtx) :-
        executionContext(ExCtx, Entity, Sender, This, Self, _, _),
        Check(callable, GRBody, logtalk(phrase(GRBody, Input, Rest), ExCtx)),
        Current_object_(This, Prefix, _, _, _, _, _, _, _, _, Flags),
        CompCtx(Ctx, _, ExCtx, Entity, Sender, This, Self, Prefix, [], _, ExCtx, runtime, _, _),
        catch(
        Dcg_body(GRBody, S0, S, Pred, Ctx),
        Error,
        throw(error(Error, logtalk(phrase(GRBody, Input, Rest), ExCtx)))
        ),
        Compile_body(Pred, TPred, DPred, Ctx),
        Input = S0, Rest = S,
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        call(DPred)
        ;	call(TPred)
        ).



   //expand_termLocal(+objectIdentifier,   ?term, ?term, @executionContext)
   //expand_termLocal(+categoryIdentifier, ?term, ?term, @executionContext)
        %
   //expand_term/2 local calls
        %
   //calls the term_expansion/2 user-defined hook predicate if defined and within scope

        expand_termLocal(Entity, Term, Expansion, ExCtx) :-
        (	var(Term) ->
        Expansion = Term
        ;	term_expansionLocal(Entity, Term, Expand, ExCtx) ->
        Expansion = Expand
        ;	Term = (_ --> _) ->
   //default grammar rule expansion
        CompCtx(Ctx, _, _, _, _, _, _, _, [], _, _, runtime, _, _),
        catch(
        DcgRule(Term, Clause, Ctx),
        Error,
        throw(error(Error, logtalk(expand_term(Term,_), ExCtx)))
        ),
        (	Clause = (Head :- Body),
        CompilerFlag(optimize, on) ->
        Simplify_goal(Body, SBody),
        (	SBody == true ->
        Expansion = Head
        ;	Expansion = (Head :- SBody)
        )
        ;	% fact and/or optimization disabled
        Expansion = Clause
        )
        ;	Expansion = Term
        ).


   //term_expansionLocal(+objectIdentifier, ?term, ?term, +executionContext)
        %
   //to avoid failures when the call is made from a multifile predicate clause,
   //first the term_expansion/2 definition container is located and then the
   //call is reduced to a local call

        term_expansionLocal(Obj, Term, Expansion, ExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, DDef, _, _),
        !,
        (	call(Dcl, term_expansion(_, _), Scope, _, _, SCtn, _) ->
        (	(Scope = p(_); Obj = SCtn) ->
        (	call(Def, term_expansion(_, _), _, _, _, DCtn) ->
        (	Current_object_(DCtn, _, _, DCtnDef, _, _, _, _, DCtnDDef, _, _) ->
        (	call(DCtnDef, term_expansion(Term, Expansion), ExCtx, Call) ->
        true
        ;	call(DCtnDDef, term_expansion(Term, Expansion), ExCtx, Call)
        )
        ;	CurrentCategory_(DCtn, _, _, DCtnDef, _, _),
        call(DCtnDef, term_expansion(Term, Expansion), ExCtx, Call)
        )
        ;	% no definition found
        fail
        )
        ;	% declaration is out of scope but we can still try a local definition
        call(Def, term_expansion(Term, Expansion), ExCtx, Call) ->
        true
        ;	call(DDef, term_expansion(Term, Expansion), ExCtx, Call)
        )
        ;	% no declaration for the term_expansion/2 hook predicate found;
   //check for a local definition
        call(Def, term_expansion(Term, Expansion), ExCtx, Call) ->
        true
        ;	call(DDef, term_expansion(Term, Expansion), ExCtx, Call)
        ),
        !,
        once(Call).

        term_expansionLocal(Ctg, Term, Expansion, ExCtx) :-
        CurrentCategory_(Ctg, _, Dcl, Def, _, _),
        (	call(Dcl, term_expansion(_, _), Scope, _, _, DclCtn) ->
        (	(Scope = p(_); Ctg = DclCtn) ->
        (	call(Def, term_expansion(_, _), _, _, DCtn) ->
        CurrentCategory_(DCtn, _, _, DCtnDef, _, _),
        call(DCtnDef, term_expansion(Term, Expansion), ExCtx, Call)
        ;	% no definition found
        fail
        )
        ;	% declaration is out of scope but we can still try a local definition
        call(Def, term_expansion(Term, Expansion), ExCtx, Call)
        )
        ;	% no declaration for the term_expansion/2 hook predicate found;
   //check for a local definition
        call(Def, term_expansion(Term, Expansion), ExCtx, Call)
        ),
        !,
        once(Call).



   //expand_term_message(+objectIdentifier,   ?term, ?term, +objectIdentifier, @scope, @executionContext)
        %
   //expand_term/2 messages
        %
   //calls the term_expansion/2 user-defined hook predicate if defined and within scope

        expand_term_message(Entity, Term, Expansion, Sender, Scope, ExCtx) :-
        (	var(Term) ->
        Expansion = Term
        ;	term_expansion_message(Entity, Term, Expand, Sender, Scope) ->
        Expansion = Expand
        ;	Term = (_ --> _) ->
   //default grammar rule expansion
        CompCtx(Ctx, _, _, _, _, _, _, _, [], _, _, runtime, _, _),
        catch(
        DcgRule(Term, Clause, Ctx),
        Error,
        throw(error(Error, logtalk(expand_term(Term,_), ExCtx)))
        ),
        (	Clause = (Head :- Body),
        CompilerFlag(optimize, on) ->
        Simplify_goal(Body, SBody),
        (	SBody == true ->
        Expansion = Head
        ;	Expansion = (Head :- SBody)
        )
        ;	% fact and/or optimization disabled
        Expansion = Clause
        )
        ;	Expansion = Term
        ).


   //term_expansion_message(+objectIdentifier, ?term, ?term, +objectIdentifier, @scope)

        term_expansion_message(Obj, Term, Expansion, Sender, LookupScope) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, _, _, _),
        (	call(Dcl, term_expansion(_, _), PredScope, _, _, SCtn, _) ->
        (	(PredScope = LookupScope; Sender = SCtn) ->
        executionContext(ExCtx, Obj, Sender, Obj, Obj, [], []),
        call(Def, term_expansion(Term, Expansion), ExCtx, Call, _, _)
        ;	% message is out of scope
        fail
        )
        ;	% no declaration for the term_expansion/2 hook predicate found
        fail
        ),
        !,
        once(Call).



   //expand_goalLocal(+objectIdentifier,   ?term, ?term, @executionContext)
   //expand_goalLocal(+categoryIdentifier, ?term, ?term, @executionContext)
        %
   //expand_goal/2 local calls
        %
   //calls the goal_expansion/2 user-defined hook predicate if defined and within scope

        expand_goalLocal(Obj, Goal, ExpandedGoal, ExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, DDef, _, _),
        !,
        (	call(Dcl, goal_expansion(_, _), Scope, _, _, SCtn, _) ->
        (	(Scope = p(_); Obj = SCtn) ->
        expand_goal_objectScoped(Goal, ExpandedGoal, Def, ExCtx)
        ;	% declaration is out of scope but we can still try a local definition
        expand_goal_objectLocal(Goal, ExpandedGoal, Def, DDef, ExCtx)
        )
        ;	% no declaration for the goal_expansion/2 hook predicate found;
   //try to use a local definition if it exists
        expand_goal_objectLocal(Goal, ExpandedGoal, Def, DDef, ExCtx)
        ).

        expand_goalLocal(Ctg, Goal, ExpandedGoal, ExCtx) :-
        CurrentCategory_(Ctg, _, Dcl, Def, _, _),
        (	call(Dcl, goal_expansion(_, _), Scope, _, _, DclCtn) ->
        (	(Scope = p(_); Ctg = DclCtn) ->
        expand_goalCategoryScoped(Goal, ExpandedGoal, Def, ExCtx)
        ;	% declaration is out of scope but we can still try a local definition
        expand_goalCategoryLocal(Goal, ExpandedGoal, Def, ExCtx)
        )
        ;	% no declaration for the goal_expansion/2 hook predicate found;
   //try to use a local definition if it exists
        expand_goalCategoryLocal(Goal, ExpandedGoal, Def, ExCtx)
        ).


   //expand_goal_objectScoped(?term, ?term, +atom, +executionContext)
        %
   //to avoid failures when the call is made from a multifile predicate clause,
   //first the goal_expansion/2 definition container is located and then the
   //call is reduced to a local call

        expand_goal_objectScoped(Goal, ExpandedGoal, Def, ExCtx) :-
        (	call(Def, goal_expansion(_, _), _, _, _, DCtn) ->
        (	Current_object_(DCtn, _, _, DCtnDef, _, _, _, _, DCtnDDef, _, _) ->
        expand_goal_objectLocal(Goal, ExpandedGoal, DCtnDef, DCtnDDef, ExCtx)
        ;	CurrentCategory_(DCtn, _, _, DCtnDef, _, _),
        expand_goalCategoryLocal(Goal, ExpandedGoal, DCtnDef, ExCtx)
        )
        ;	% no goal_expansion/2 hook predicate definition found
        ExpandedGoal = Goal
        ).


   //expand_goal_objectLocal(?term, ?term, +atom, +atom, +executionContext)

        expand_goal_objectLocal(Goal, ExpandedGoal, Def, DDef, ExCtx) :-
        (	var(Goal) ->
        ExpandedGoal = Goal
        ;	% lookup local goal_expansion/2 hook predicate definition
        (	call(Def, goal_expansion(Goal, ExpandedGoal0), ExCtx, Call)
        ;	call(DDef, goal_expansion(Goal, ExpandedGoal0), ExCtx, Call)
        ) ->
        (	call(Call),
        Goal \== ExpandedGoal0 ->
        expand_goal_objectLocal(ExpandedGoal0, ExpandedGoal, Def, DDef, ExCtx)
        ;	% fixed-point found
        ExpandedGoal = Goal
        )
        ;	% no local goal_expansion/2 hook predicate definition found
        ExpandedGoal = Goal
        ).


   //expand_goalCategoryScoped(?term, ?term, +atom, +executionContext)
        %
   //to avoid failures when the call is made from a multifile predicate clause,
   //first the goal_expansion/2 definition container is located and then the
   //call is reduced to a local call

        expand_goalCategoryScoped(Goal, ExpandedGoal, Def, ExCtx) :-
        (	call(Def, goal_expansion(_, _), _, _, DCtn) ->
        CurrentCategory_(DCtn, _, _, DCtnDef, _, _),
        expand_goalCategoryLocal(Goal, ExpandedGoal, DCtnDef, ExCtx)
        ;	% no local goal_expansion/2 hook predicate definition found
        ExpandedGoal = Goal
        ).


   //expand_goal_objectLocal(?term, ?term, +atom, +executionContext)

        expand_goalCategoryLocal(Goal, ExpandedGoal, Def, ExCtx) :-
        (	var(Goal) ->
        ExpandedGoal = Goal
        ;	% lookup local goal_expansion/2 hook predicate definition
        call(Def, goal_expansion(Goal, ExpandedGoal0), ExCtx, Call) ->
        (	call(Call),
        Goal \== ExpandedGoal0 ->
        expand_goalCategoryLocal(ExpandedGoal0, ExpandedGoal, Def, ExCtx)
        ;	% fixed-point found
        ExpandedGoal = Goal
        )
        ;	% no local goal_expansion/2 hook predicate definition found
        ExpandedGoal = Goal
        ).



   //expand_goal_message(+objectIdentifier, ?term, ?term, +objectIdentifier, @scope)
        %
   //expand_goal/2 messages
        %
   //calls the goal_expansion/2 user-defined hook predicate if defined and within scope

        expand_goal_message(Obj, Goal, ExpandedGoal, Sender, LookupScope) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, _, _, _),
        (	% lookup visible goal_expansion/2 hook predicate declaration
        call(Dcl, goal_expansion(_, _), PredScope, _, _, SCtn, _) ->
        (	(PredScope = LookupScope; Sender = SCtn) ->
        executionContext(ExCtx, Obj, Sender, Obj, Obj, [], []),
        expand_goal_message(Goal, ExpandedGoal, Def, ExCtx)
        ;	% message is out of scope
        ExpandedGoal = Goal
        )
        ;	% no declaration for the goal_expansion/2 hook predicate found
        ExpandedGoal = Goal
        ).


   //expand_goal_message(?term, ?term, +atom, +executionContext)

        expand_goal_message(Goal, ExpandedGoal, Def, ExCtx) :-
        (	var(Goal) ->
        ExpandedGoal = Goal
        ;	% lookup visible goal_expansion/2 hook predicate definition
        call(Def, goal_expansion(Goal, ExpandedGoal0), ExCtx, Call, _, _) ->
        (	call(Call),
        Goal \== ExpandedGoal0 ->
        expand_goal_message(ExpandedGoal0, ExpandedGoal, Def, ExCtx)
        ;	% fixed-point found
        ExpandedGoal = Goal
        )
        ;	% no visible goal_expansion/2 hook predicate definition found
        ExpandedGoal = Goal
        ).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // message sending
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //Send_toSelf(?term, +compilationContext)
        %
   //runtime processing of a message sending call when the message is not
   //known at compile time

        Send_toSelf(Pred, Ctx) :-
   //we must ensure that the argument is valid before compiling the message
   //sending goal otherwise there would be a potential for an endless loop
        CompCtx_execCtx(Ctx, ExCtx),
        Check(callable, Pred, logtalk(::Pred, ExCtx)),
        catch(Compile_message_toSelf(Pred, TPred, Ctx), Error, throw(error(Error, logtalk(::Pred, ExCtx)))),
        call(TPred).



   //Send_toSelf_(+objectIdentifier, +callable, +executionContext)
        %
   //the last clause of this dynamic binding cache predicate must always exist
   //and must call the predicate that generates the missing cache entry

        Send_toSelf_(Obj, Pred, SenderExCtx) :-
        Send_toSelf_nv(Obj, Pred, SenderExCtx).



   //Send_toSelf_nv(+objectIdentifier, +callable, +executionContext)
        %
   //runtime processing of a message sending call when the arguments have already
   //been entityKind-checked; generates a cache entry to speed up future calls

        Send_toSelf_nv(Obj, Pred, SenderExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, _, _, _),
        executionContext(SenderExCtx, _, _, Sender, _, _, _),
        (	% lookup predicate declaration
        call(Dcl, Pred, Scope, Meta, _, SCtn, _) ->
        (	% check scope
        (Scope = p(_); Sender = SCtn) ->
        (	% construct predicate, object, and "sender" templates
        term_template(Pred, GPred),
        term_template(Obj, GObj),
        term_template(Sender, GSender),
   //construct list of the meta-arguments that will be called in the "sender"
        goal_metaCallContext(Meta, GPred, GSenderExCtx, GSender, GMetaCallCtx),
   //lookup predicate definition
        executionContext(GExCtx, _, GSender, GObj, GObj, GMetaCallCtx, []),
        call(Def, GPred, GExCtx, GCall, _, _) ->
   //cache lookup result (the cut prevents backtracking into the catchall clause)
        asserta((Send_toSelf_(GObj, GPred, GSenderExCtx) :- !, GCall)),
   //unify message arguments and call method
        GObj = Obj, GPred = Pred, GSender = Sender, GSenderExCtx = SenderExCtx,
        call(GCall)
        ;	% no definition found; fail as per closed-world assumption
        fail
        )
        ;	% message is not within the scope of the sender
        functor(Pred, Functor, Arity),
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(::Pred, SenderExCtx)))
        )
        ;	% no predicate declaration, check if it's a private built-in method
        builtIn_method(Pred, p, _, _) ->
        functor(Pred, Functor, Arity),
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(::Pred, SenderExCtx)))
        ;	% message not understood; check for a message forwarding handler
        call(Def, forward(Pred), ExCtx, Call, _, _) ->
        executionContext(ExCtx, _, Sender, Obj, Obj, [], []),
        call(Call)
        ;	% give up and throw an existence error
        functor(Pred, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(::Pred, SenderExCtx)))
        ).



   //Send_to_objRt(?term, ?term, +atom, +compilationContext)
        %
   //runtime processing of a message sending call when the message and
   //possibly the receiver object are not known at compile time

        Send_to_objRt(Obj, Pred, Events, Ctx) :-
   //we must ensure that the message is valid before compiling the
   //message sending goal otherwise an endless loop could result
        CompCtx_execCtx(Ctx, ExCtx),
        Check(callable, Pred, logtalk(Obj::Pred, ExCtx)),
        catch(
        Compile_message_to_object(Pred, Obj, TPred, Events, Ctx),
        Error,
        throw(error(Error, logtalk(Obj::Pred, ExCtx)))
        ),
        call(TPred).



   //Send_to_obj(+objectIdentifier, +callable, +executionContext)
        %
   //runtime processing of an event-aware message sending call when the
   //receiver object is not known at compile time; as using the cache
   //only requires a bound first argument, we delay errors other than an
   //instantiation error for a small performance gain

        Send_to_obj(Obj, Pred, SenderExCtx) :-
        (	nonvar(Obj) ->
        Send_to_obj_(Obj, Pred, SenderExCtx)
        ;	throw(error(instantiation_error, logtalk(Obj::Pred, SenderExCtx)))
        ).



   //Send_to_obj_(+objectIdentifier, +callable, +executionContext)
        %
   //the last clause of this dynamic binding cache predicate must always exist
   //and must call the predicate that generates the missing cache entry

        Send_to_obj_(Obj, Pred, SenderExCtx) :-
        Send_to_obj_nv(Obj, Pred, SenderExCtx).



   //Send_to_obj_nv(+objectIdentifier, +callable, +executionContext)
        %
   //runtime processing of an event-aware message sending call when the arguments
   //have already been entityKind-checked; generates a cache entry to speed up future calls

        Send_to_obj_nv(Obj, Pred, SenderExCtx) :-
        executionContext(SenderExCtx, _, _, Sender, _, _, _),
   //call all before event handlers
        \+ (before_event_(Obj, Pred, Sender, _, Before), \+ Before),
   //process the message; we cannot simply call Send_to_obj_ne'/3
   //as the generated cache entries are different
        Send_to_obj_nvInner(Obj, Pred, Sender, SenderExCtx),
   //call all after event handlers
        \+ (After_event_(Obj, Pred, Sender, _, After), \+ After).


        Send_to_obj_nvInner(Obj, Pred, Sender, SenderExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, _, _, _),
        !,
        (	% lookup predicate declaration
        call(Dcl, Pred, Scope, Meta, _, SCtn, _) ->
        (	% check public scope
        Scope = p(p(_)) ->
        (	% construct predicate and object templates
        term_template(Pred, GPred),
        term_template(Obj, GObj),
   //construct list of the meta-arguments that will be called in the "sender"
        goal_metaCallContext(Meta, GPred, GSenderExCtx, GSender, GMetaCallCtx),
   //lookup predicate definition
        executionContext(GExCtx, _, GSender, GObj, GObj, GMetaCallCtx, []),
        call(Def, GPred, GExCtx, GCall, _, _) ->
        GGCall = guarded_methodCall(GObj, GPred, GSender, GCall),
   //cache lookup result (the cut prevents backtracking into the catchall clause)
        asserta((Send_to_obj_(GObj, GPred, GSenderExCtx) :- !, GGCall)),
   //unify message arguments and call method
        GObj = Obj, GPred = Pred, GSender = Sender, GSenderExCtx = SenderExCtx,
        call(GCall)
        ;	% no definition found; fail as per closed-world assumption
        fail
        )
        ;	% protected or private scope: check if sender and scope container are the same
        Sender = SCtn ->
        (	% construct predicate, object, and "sender" templates
        term_template(Pred, GPred),
        term_template(Obj, GObj),
        term_template(Sender, GSender),
        executionContext(GExCtx, _, GSender, GObj, GObj, _, []),
   //lookup predicate definition
        call(Def, GPred, GExCtx, GCall, _, _) ->
        GGCall = guarded_methodCall(GObj, GPred, GSender, GCall),
   //cache lookup result (the cut prevents backtracking into the catchall clause)
        asserta((Send_to_obj_(GObj, GPred, GSenderExCtx) :- !, GGCall)),
   //unify message arguments and call method
        GObj = Obj, GPred = Pred, GSender = Sender, GSenderExCtx = SenderExCtx,
        call(GCall)
        ;	% no definition found; fail as per closed-world assumption
        fail
        )
        ;	% message is not within the scope of the sender
        functor(Pred, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(Obj::Pred, SenderExCtx)))
        ;	throw(error(permission_error(access, protected_predicate, Functor/Arity), logtalk(Obj::Pred, SenderExCtx)))
        )
        )
        ;	% no predicate declaration, check if it's a private built-in method
        builtIn_method(Pred, p, _, _) ->
        functor(Pred, Functor, Arity),
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(Obj::Pred, SenderExCtx)))
        ;	% message not understood; check for a message forwarding handler
        call(Def, forward(Pred), ExCtx, Call, _, _) ->
        executionContext(ExCtx, _, Sender, Obj, Obj, [], []),
        call(Call)
        ;	% give up and throw an existence error
        functor(Pred, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(Obj::Pred, SenderExCtx)))
        ).

        Send_to_obj_nvInner({Proxy}, Pred, _, SenderExCtx) :-
        !,
   //parametric object proxy
        catch(Proxy, error(Error, _), throw(error(Error, logtalk({Proxy}::Pred, SenderExCtx)))),
        Send_to_obj_(Proxy, Pred, SenderExCtx).

        Send_to_obj_nvInner(Obj, Pred, _, _) :-
        atom(Obj),
        prologFeature(modules, supported),
        current_module(Obj),
        !,
   //allow Obj::Pred to be used as a shortcut for calling module predicates
        ':(Obj, Pred).

        Send_to_obj_nvInner(Obj, Pred, _, SenderExCtx) :-
        \+ callable(Obj),
        throw(error(type_error(objectIdentifier, Obj), logtalk(Obj::Pred, SenderExCtx))).

        Send_to_obj_nvInner(Obj, Pred, _, SenderExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::Pred, SenderExCtx))).



   //guarded_methodCall(+objectIdentifier, +callable, +objectIdentifier, +callable)
        %
   //wraps the method call with the before and after event handler calls; the "before" event handler
   //may prevent a method from being executed by failing and an "after" event handler may prevent a
   //method from succeeding by failing; however, event handlers cannot modify the method call

        guarded_methodCall(Obj, Msg, Sender, Method) :-
   //call before event handlers
        \+ (before_event_(Obj, Msg, Sender, _, Before), \+ Before),
   //call method
        call(Method),
   //call after event handlers
        \+ (After_event_(Obj, Msg, Sender, _, After), \+ After).



   //Send_to_obj_ne(+objectIdentifier, +callable, +executionContext)
        %
   //runtime processing of an event-transparent message sending call when
   //the receiver object is not known at compile time; as using the cache
   //only requires a bound first argument, we delay errors other than an
   //instantiation error for a small performance gain

        Send_to_obj_ne(Obj, Pred, SenderExCtx) :-
        (	nonvar(Obj) ->
        Send_to_obj_ne_(Obj, Pred, SenderExCtx)
        ;	throw(error(instantiation_error, logtalk(Obj::Pred, SenderExCtx)))
        ).



   //Send_to_obj_ne_(+objectIdentifier, +callable, +executionContext)
        %
   //the last clause of this dynamic binding cache predicate must always exist
   //and must call the predicate that generates the missing cache entry

        Send_to_obj_ne_(Obj, Pred, SenderExCtx) :-
        Send_to_obj_ne_nv(Obj, Pred, SenderExCtx).



   //Send_to_obj_ne_nv(+objectIdentifier, +term, +executionContext)
        %
   //runtime processing of an event-transparent message sending call when the arguments
   //have already been entityKind-checked; generates a cache entry to speed up future calls

        Send_to_obj_ne_nv(Obj, Pred, SenderExCtx) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, _, _, _),
        !,
        executionContext(SenderExCtx, _, _, Sender, _, _, _),
        (	% lookup predicate declaration
        call(Dcl, Pred, Scope, Meta, _, SCtn, _) ->
        (	% check public scope
        Scope = p(p(_)) ->
        (	% construct predicate and object templates
        term_template(Pred, GPred),
        term_template(Obj, GObj),
   //construct list of the meta-arguments that will be called in the "sender"
        goal_metaCallContext(Meta, GPred, GSenderExCtx, GSender, GMetaCallCtx),
   //lookup predicate definition
        executionContext(GExCtx, _, GSender, GObj, GObj, GMetaCallCtx, []),
        call(Def, GPred, GExCtx, GCall, _, _) ->
   //cache lookup result (the cut prevents backtracking into the catchall clause)
        asserta((Send_to_obj_ne_(GObj, GPred, GSenderExCtx) :- !, GCall)),
   //unify message arguments and call method
        GObj = Obj, GPred = Pred, GSender = Sender, GSenderExCtx = SenderExCtx,
        call(GCall)
        ;	% no definition found; fail as per closed-world assumption
        fail
        )
        ;	% protected or private scope: check if sender and scope container are the same
        Sender = SCtn ->
        (	% construct predicate, object, and "sender" templates
        term_template(Pred, GPred),
        term_template(Obj, GObj),
        term_template(Sender, GSender),
   //lookup predicate definition
        executionContext(GExCtx, _, GSender, GObj, GObj, _, []),
        call(Def, GPred, GExCtx, GCall, _, _) ->
   //cache lookup result (the cut prevents backtracking into the catchall clause)
        asserta((Send_to_obj_ne_(GObj, GPred, GSenderExCtx) :- !, GCall)),
   //unify message arguments and call method
        GObj = Obj, GPred = Pred, GSender = Sender, GSenderExCtx = SenderExCtx,
        call(GCall)
        ;	% no definition found; fail as per closed-world assumption
        fail
        )
        ;	% message is not within the scope of the sender
        functor(Pred, Functor, Arity),
        (	Scope == p ->
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(Obj::Pred, SenderExCtx)))
        ;	throw(error(permission_error(access, protected_predicate, Functor/Arity), logtalk(Obj::Pred, SenderExCtx)))
        )
        )
        ;	% no predicate declaration, check if it's a private built-in method
        builtIn_method(Pred, p, _, _) ->
        functor(Pred, Functor, Arity),
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(Obj::Pred, SenderExCtx)))
        ;	% message not understood; check for a message forwarding handler
        call(Def, forward(Pred), ExCtx, Call, _, _) ->
        executionContext(ExCtx, _, Sender, Obj, Obj, [], []),
        call(Call)
        ;	% give up and throw an existence error
        functor(Pred, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(Obj::Pred, SenderExCtx)))
        ).

        Send_to_obj_ne_nv({Proxy}, Pred, SenderExCtx) :-
        !,
   //parametric object proxy
        catch(Proxy, error(Error, _), throw(error(Error, logtalk({Proxy}::Pred, SenderExCtx)))),
        Send_to_obj_ne_(Proxy, Pred, SenderExCtx).

        Send_to_obj_ne_nv(Obj, Pred, _) :-
        atom(Obj),
        prologFeature(modules, supported),
        current_module(Obj),
        !,
   //allow Obj::Pred to be used as a shortcut for calling module predicates
        ':(Obj, Pred).

        Send_to_obj_ne_nv(Obj, Pred, SenderExCtx) :-
        \+ callable(Obj),
        throw(error(type_error(objectIdentifier, Obj), logtalk(Obj::Pred, SenderExCtx))).

        Send_to_obj_ne_nv(Obj, Pred, SenderExCtx) :-
        throw(error(existence_error(object, Obj), logtalk(Obj::Pred, SenderExCtx))).



   //objSuperCall(+atom, +term, +executionContext)
        %
   //runtime processing of an object "super" call when the predicate called is
   //not known at compile time; as using the cache only requires a bound first
   //argument, we delay errors other than an instantiation error for a small
   //performance gain

        objSuperCall(Super, Pred, ExCtx) :-
        (	nonvar(Pred) ->
        objSuperCall_(Super, Pred, ExCtx)
        ;	throw(error(instantiation_error, logtalk(^^Pred, ExCtx)))
        ).



   //objSuperCall_(+atom, +callable, +executionContext)
        %
   //the last clause of this dynamic binding cache predicate must always exist
   //and must call the predicate that generates the missing cache entry

        objSuperCall_(Super, Pred, ExCtx) :-
        objSuperCall_nv(Super, Pred, ExCtx).



   //objSuperCall_nv(+atom, +callable, +executionContext)
        %
   //runtime processing of an object "super" call when the arguments have already
   //been entityKind-checked; generates a cache entry to speed up future calls
        %
   //we may need to pass "self" when looking for the inherited predicate definition
   //in order to be able to select the correct "super" clause for those cases where
   //"this" both instantiates and specializes other objects

        objSuperCall_nv(Super, Pred, ExCtx) :-
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(Self, _, Dcl, _, _, _, _, _, _, _, _),
        (	% lookup predicate declaration (the predicate must not be
   //declared in the same entity making the "super" call)
        call(Dcl, Pred, Scope, _, _, SCtn, TCtn), TCtn \= This ->
        (	% check scope
        (Scope = p(_); This = SCtn) ->
        (	% construct predicate, "this", and "self" templates
        term_template(Pred, GPred),
        term_template(This, GThis),
        term_template(Self, GSelf),
   //check if we have a dependency on "self" to select the correct "super" clause
        (	extends_object_(GThis, _, _) ->
        true
        ;	executionContext(GExCtx, _, _, GThis, GSelf, _, _)
        ),
   //lookup predicate definition (the predicate must not be
   //defined in the same entity making the "super" call)
        call(Super, GPred, GExCtx, GCall, _, DefCtn), DefCtn \= GThis ->
   //cache lookup result (the cut prevents backtracking into the catchall clause)
        asserta((objSuperCall_(Super, GPred, GExCtx) :- !, GCall)),
   //unify message arguments and call inherited definition
        GPred = Pred, GExCtx = ExCtx,
        call(GCall)
        ;	% no definition found; fail as per closed-world assumption
        fail
        )
        ;	% predicate is not within the scope of the sender
        functor(Pred, Functor, Arity),
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(^^Pred, ExCtx)))
        )
        ;	% no predicate declaration, check if it's a private built-in method
        builtIn_method(Pred, p, _, _) ->
        functor(Pred, Functor, Arity),
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(^^Pred, ExCtx)))
        ;	% non-callable term error
        \+ callable(Pred) ->
        throw(error(type_error(callable, Pred), logtalk(^^Pred, ExCtx)))
        ;	% give up and throw an existence error
        functor(Pred, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(^^Pred, ExCtx)))
        ).



   //CtgSuperCall(+categoryIdentifier, +term, +executionContext)
        %
   //runtime processing of a category "super" call when the predicate called
   //is not known at compile time;  as using the cache only requires a bound
   //first argument, we delay errors other than an instantiation error for a
   //small performance gain

        CtgSuperCall(Ctg, Pred, ExCtx) :-
        (	nonvar(Pred) ->
        CtgSuperCall_(Ctg, Pred, ExCtx)
        ;	throw(error(instantiation_error, logtalk(^^Pred, ExCtx)))
        ).




   //CtgSuperCall_(+categoryIdentifier, +callable, +executionContext)
        %
   //the last clause of this dynamic binding cache predicate must always exist
   //and must call the predicate that generates the missing cache entry

        CtgSuperCall_(Ctg, Pred, ExCtx) :-
        CtgSuperCall_nv(Ctg, Pred, ExCtx).



   //CtgSuperCall_nv(+categoryIdentifier, +callable, +executionContext)
        %
   //runtime processing of a category "super" call when the arguments have already
   //been entityKind-checked; generates a cache entry to speed up future calls

        CtgSuperCall_nv(Ctg, Pred, ExCtx) :-
        CurrentCategory_(Ctg, _, Dcl, Def, _, _),
        (	% lookup predicate declaration (the predicate must not be
   //declared in the same entity making the "super" call)
        call(Dcl, Pred, Scope, _, _, DclCtn), DclCtn \= Ctg ->
        (	% check that the call is within scope (i.e. public or protected)
        Scope = p(_) ->
        (	% construct category and predicate templates
        term_template(Ctg, GCtg),
        term_template(Pred, GPred),
   //lookup predicate definition (the predicate must not be
   //defined in the same entity making the "super" call)
        call(Def, GPred, GExCtx, GCall, DefCtn), DefCtn \= Ctg ->
   //cache lookup result (the cut prevents backtracking into the catchall clause)
        asserta((CtgSuperCall_(GCtg, GPred, GExCtx) :- !, GCall)),
   //unify message arguments and call inherited definition
        GCtg = Ctg, GPred = Pred, GExCtx = ExCtx,
        call(GCall)
        ;	% no definition found; fail as per closed-world assumption
        fail
        )
        ;	% predicate is not within the scope of the sender
        functor(Pred, Functor, Arity),
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(^^Pred, ExCtx)))
        )
        ;	% no predicate declaration, check if it's a private built-in method
        builtIn_method(Pred, p, _, _) ->
        functor(Pred, Functor, Arity),
        throw(error(permission_error(access, private_predicate, Functor/Arity), logtalk(^^Pred, ExCtx)))
        ;	% non-callable term error
        \+ callable(Pred) ->
        throw(error(type_error(callable, Pred), logtalk(^^Pred, ExCtx)))
        ;	% give up and throw an existence error
        functor(Pred, Functor, Arity),
        throw(error(existence_error(predicateDeclaration, Functor/Arity), logtalk(^^Pred, ExCtx)))
        ).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // meta-calls
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //Lambda(+curly_bracketed_term, @callable)
        %
   //calls a lambda-call with free variables but no parameters (Free/Goal) where the
   //arguments are already checked and compiled; typically used in bagof/3 and setof/3
   //as an alternative to the enumeration of all existentially quantified variables

        Lambda(Free, Goal) :-
        Copy_term_withoutConstraints(Free/Goal, Free/GoalCopy),
        call(GoalCopy).



   //metacall(?term, +list, +executionContext)
        %
   //performs a runtime meta-call constructed from a closure and a list of additional arguments

        metacall(Closure, ExtraArgs, ExCtx) :-
        var(Closure),
        Call =.. [call, Closure| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx))).

        metacall(Closure(TFunctor, TArgs, ExCtx), ExtraArgs, _) :-
        !,
        Append(TArgs, ExtraArgs, FullArgs),
        TGoal =.. [TFunctor| FullArgs],
        call(TGoal, ExCtx).

        metacall({Closure}, ExtraArgs, ExCtx) :-
        !,
   //compiler bypass (call of external code)
        (	atom(Closure) ->
        Goal =.. [Closure| ExtraArgs],
        call(Goal)
        ;	compound(Closure) ->
        Closure =.. [Functor| Args],
        Append(Args, ExtraArgs, FullArgs),
        Goal =.. [Functor| FullArgs],
        call(Goal)
        ;	var(Closure) ->
        Call =.. [call, {Closure}| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	Call =.. [call, {Closure}| ExtraArgs],
        throw(error(type_error(callable, Closure), logtalk(Call, ExCtx)))
        ).

        metacall(::Closure, ExtraArgs, ExCtx) :-
        !,
        executionContext(ExCtx, _, _, _, Self0, MetaCallCtx, _),
        (	MetaCallCtx = CallerExCtx-MetaArgs,
        member_var(::Closure, MetaArgs) ->
        executionContext(CallerExCtx, _, _, _, Self, _, _),
        SelfExCtx = CallerExCtx
        ;	Self = Self0,
        SelfExCtx = ExCtx
        ),
        (	atom(Closure) ->
        Goal =.. [Closure| ExtraArgs],
        Send_toSelf_(Self, Goal, SelfExCtx)
        ;	compound(Closure) ->
        Closure =.. [Functor| Args],
        Append(Args, ExtraArgs, FullArgs),
        Goal =.. [Functor| FullArgs],
        Send_toSelf_(Self, Goal, SelfExCtx)
        ;	var(Closure) ->
        Call =.. [call, ::Closure| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	Call =.. [call, ::Closure| ExtraArgs],
        throw(error(type_error(callable, ::Closure), logtalk(Call, ExCtx)))
        ).

        metacall(^^Closure, ExtraArgs, ExCtx) :-
        !,
        executionContext(ExCtx, Entity0, _, _, _, MetaCallCtx, _),
        (	MetaCallCtx = CallerExCtx-MetaArgs,
        member_var(^^Closure, MetaArgs) ->
        executionContext(CallerExCtx, Entity, _, _, _, _, _),
        SuperExCtx = CallerExCtx
        ;	Entity = Entity0,
        SuperExCtx = ExCtx
        ),
        (	atom(Closure) ->
        Goal =.. [Closure| ExtraArgs]
        ;	compound(Closure) ->
        Closure =.. [Functor| Args],
        Append(Args, ExtraArgs, FullArgs),
        Goal =.. [Functor| FullArgs]
        ;	var(Closure) ->
        Call =.. [call, ^^Closure| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	Call =.. [call, ^^Closure| ExtraArgs],
        throw(error(type_error(callable, Closure), logtalk(Call, ExCtx)))
        ),
        (	Current_object_(Entity, _, _, _, Super, _, _, _, _, _, _) ->
        objSuperCall_(Super, Goal, SuperExCtx)
        ;	CurrentCategory_(Entity, _, _, _, _, _),
        CtgSuperCall_(Entity, Goal, SuperExCtx)
        ).

        metacall(Obj::Closure, ExtraArgs, ExCtx) :-
        !,
        executionContext(ExCtx, _, Sender0, This, _, MetaCallCtx, _),
        (	MetaCallCtx = CallerExCtx-MetaArgs,
        member_var(Obj::Closure, MetaArgs) ->
        Sender = Sender0
        ;	CallerExCtx = ExCtx,
        Sender = This
        ),
        (	callable(Obj), callable(Closure) ->
        Closure =.. [Functor| Args],
        Append(Args, ExtraArgs, FullArgs),
        Goal =.. [Functor| FullArgs],
        (	Current_object_(Sender, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 16 =:= 16 ->
        Send_to_obj_(Obj, Goal, CallerExCtx)
        ;	Send_to_obj_ne_(Obj, Goal, CallerExCtx)
        )
        ;	var(Obj) ->
        Call =.. [call, Obj::Closure| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	var(Closure) ->
        Call =.. [call, Obj::Closure| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	\+ callable(Closure) ->
        Call =.. [call, Obj::Closure| ExtraArgs],
        throw(error(type_error(callable, Closure), logtalk(Call, ExCtx)))
        ;	Call =.. [call, Obj::Closure| ExtraArgs],
        throw(error(type_error(objectIdentifier, Obj), logtalk(Call, ExCtx)))
        ).

        metacall([Obj::Closure], ExtraArgs, ExCtx) :-
        !,
        executionContext(ExCtx, _, Sender0, _, _, MetaCallCtx0, _),
        (	MetaCallCtx0 = CallerExCtx0-_ ->
        executionContext(CallerExCtx0, _, Sender, _, _, _, _)
        ;	CallerExCtx0 = ExCtx,
        Sender = Sender0
        ),
        (	callable(Obj), callable(Closure), Obj \= Sender ->
        Closure =.. [Functor| Args],
        Append(Args, ExtraArgs, FullArgs),
        Goal =.. [Functor| FullArgs],
   //prevent the original sender, which is perserved when delegating a message, to be reset to "this"
        executionContext(CallerExCtx0, Entity, Sender, _, Self, MetaCallCtx, Stack),
        executionContext(CallerExCtx, Entity, Sender, Sender, Self, MetaCallCtx, Stack),
        (	Current_object_(Sender, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 16 =:= 16 ->
        Send_to_obj_(Obj, Goal, CallerExCtx)
        ;	Send_to_obj_ne_(Obj, Goal, CallerExCtx)
        )
        ;	var(Obj) ->
        Call =.. [call, [Obj::Closure]| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	var(Closure) ->
        Call =.. [call, [Obj::Closure]| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	\+ callable(Closure) ->
        Call =.. [call, [Obj::Closure]| ExtraArgs],
        throw(error(type_error(callable, Closure), logtalk(Call, ExCtx)))
        ;	\+ callable(Obj) ->
        Call =.. [call, [Obj::Closure]| ExtraArgs],
        throw(error(type_error(objectIdentifier, Obj), logtalk(Call, ExCtx)))
        ;	% Obj = Sender ->
        Call =.. [call, [Obj::Closure]| ExtraArgs],
        throw(error(permission_error(access, object, Sender), logtalk(Call, ExCtx)))
        ).

        metacall(Obj<<Closure, ExtraArgs, ExCtx) :-
        !,
        (	callable(Obj), callable(Closure) ->
        Closure =.. [Functor| Args],
        Append(Args, ExtraArgs, FullArgs),
        Goal =.. [Functor| FullArgs],
        Call_withinContext_nv(Obj, Goal, ExCtx)
        ;	var(Obj) ->
        Call =.. [call, Obj<<Closure| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	var(Closure) ->
        Call =.. [call, Obj<<Closure| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	\+ callable(Closure) ->
        Call =.. [call, Obj<<Closure| ExtraArgs],
        throw(error(type_error(callable, Closure), logtalk(Call, ExCtx)))
        ;	Call =.. [call, Obj<<Closure| ExtraArgs],
        throw(error(type_error(objectIdentifier, Obj), logtalk(Call, ExCtx)))
        ).

        metacall(':(Module, Closure), ExtraArgs, ExCtx) :-
        !,
        (	atom(Module), callable(Closure) ->
        Closure =.. [Functor| Args],
        Append(Args, ExtraArgs, FullArgs),
        Goal =.. [Functor| FullArgs],
        ':(Module, Goal)
        ;	var(Module) ->
        Call =.. [call, ':(Module, Closure)| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	var(Closure) ->
        Call =.. [call, ':(Module, Closure)| ExtraArgs],
        throw(error(instantiation_error, logtalk(Call, ExCtx)))
        ;	\+ atom(Module) ->
        Call =.. [call, ':(Module, Closure)| ExtraArgs],
        throw(error(type_error(moduleIdentifier, Module), logtalk(Call, ExCtx)))
        ;	Call =.. [call, ':(Module, Closure)| ExtraArgs],
        throw(error(type_error(callable, Closure), logtalk(Call, ExCtx)))
        ).

        metacall(Free/Lambda, ExtraArgs, ExCtx) :-
        !,
        Check(curly_bracketed_term, Free, logtalk(Free/Lambda, ExCtx)),
        executionContext(ExCtx, Entity, Sender, This, Self, LambdaMetaCallCtx, Stack),
        ReduceLambda_metacallCtx(LambdaMetaCallCtx, Free/Lambda, MetaCallCtx),
        Copy_term_withoutConstraints(Free/Lambda+MetaCallCtx, Free/LambdaCopy+MetaCallCtxCopy),
        executionContext(NewExCtx, Entity, Sender, This, Self, MetaCallCtxCopy, Stack),
        metacall(LambdaCopy, ExtraArgs, NewExCtx).

        metacall(Free/Parameters>>Lambda, ExtraArgs, ExCtx) :-
        !,
        Check(curly_bracketed_term, Free, logtalk(Free/Parameters>>Lambda, ExCtx)),
        executionContext(ExCtx, Entity, Sender, This, Self, LambdaMetaCallCtx, Stack),
        ReduceLambda_metacallCtx(LambdaMetaCallCtx, Free/Parameters>>Lambda, MetaCallCtx),
        Copy_term_withoutConstraints(Free/Parameters>>Lambda+MetaCallCtx, Free/ParametersCopy>>LambdaCopy+MetaCallCtxCopy),
        unifyLambda_parameters(ParametersCopy, ExtraArgs, Rest, Free/Parameters>>Lambda, This),
        executionContext(NewExCtx, Entity, Sender, This, Self, MetaCallCtxCopy, Stack),
        metacall(LambdaCopy, Rest, NewExCtx).

        metacall(Parameters>>Lambda, ExtraArgs, ExCtx) :-
        !,
        executionContext(ExCtx, Entity, Sender, This, Self, LambdaMetaCallCtx, Stack),
        ReduceLambda_metacallCtx(LambdaMetaCallCtx, Parameters>>Lambda, MetaCallCtx),
        Copy_term_withoutConstraints(Parameters>>Lambda+MetaCallCtx, ParametersCopy>>LambdaCopy+MetaCallCtxCopy),
        unifyLambda_parameters(ParametersCopy, ExtraArgs, Rest, Parameters>>Lambda, ExCtx),
        executionContext(NewExCtx, Entity, Sender, This, Self, MetaCallCtxCopy, Stack),
        metacall(LambdaCopy, Rest, NewExCtx).

        metacall(Closure, ExtraArgs, ExCtx) :-
        (	atom(Closure) ->
        Goal =.. [Closure| ExtraArgs]
        ;	compound(Closure) ->
        Closure =.. [Functor| Args],
        Append(Args, ExtraArgs, FullArgs),
        Goal =.. [Functor| FullArgs]
        ;	Call =.. [call, Closure| ExtraArgs],
        throw(error(type_error(callable, Closure), logtalk(Call, ExCtx)))
        ),
        (	executionContext(ExCtx, _, _, _, _, CallerExCtx-MetaArgs, _),
        member_var(Closure, MetaArgs) ->
        metacallSender(Goal, ExCtx, CallerExCtx, ExtraArgs)
        ;	metacallLocal(Goal, ExCtx)
        ).


        unifyLambda_parameters((-), _, _, Lambda, ExCtx) :-
   //catch variables and lists with unbound tails
        (	Lambda = _/Parameters>>_
        ;	Lambda = Parameters>>_
        ),
        throw(error(type_error(list, Parameters), logtalk(Lambda, ExCtx))).

        unifyLambda_parameters([], ExtraArguments, ExtraArguments, _, _) :-
        !.

        unifyLambda_parameters([Parameter| Parameters], [Argument| Arguments], ExtraArguments, Lambda, ExCtx) :-
        !,
        Parameter = Argument,
        unifyLambda_parameters(Parameters, Arguments, ExtraArguments, Lambda, ExCtx).

        unifyLambda_parameters(_, _, _, Lambda, ExCtx) :-
        throw(error(representation_error(lambda_parameters), logtalk(Lambda, ExCtx))).


   //when using currying, the "inner" lambda expressions must be executed in the same context as the "outer"
   //lambda expressions; the same for the "inner" closure; this forces the update of the meta-call context

        ReduceLambda_metacallCtx((-), _, _).

        ReduceLambda_metacallCtx([], _, []).

        ReduceLambda_metacallCtx(CallerExCtx-[Meta| Metas], Lambda, CallerExCtx-Reduced) :-
        ReduceLambda_metacallCtx_metaArgs(Meta, Metas, Lambda, Reduced).


        ReduceLambda_metacallCtx_metaArgs(Free/Closure, Metas, Free/Closure, [Closure| Metas]) :-
        !.

        ReduceLambda_metacallCtx_metaArgs(Parameters>>Closure, Metas, Parameters>>Closure, [Closure| Metas]) :-
        !.

        ReduceLambda_metacallCtx_metaArgs(Meta, Metas, Lambda, [Meta| Reduced]) :-
   //not the meta-argument we're looking for; proceed to the next one
        (	Metas = [NextMeta| RestMetas] ->
        ReduceLambda_metacallCtx_metaArgs(NextMeta, RestMetas, Lambda, Reduced)
        ;	Reduced = []
        ).



   //metacall(?term, +executionContext)
        %
   //performs a meta-call at runtime

        metacall(Goal, ExCtx) :-
        var(Goal),
        throw(error(instantiation_error, logtalk(call(Goal), ExCtx))).

        metacall({Goal}, ExCtx) :-
   //pre-compiled meta-calls or calls in "user" (compiler bypass)
        !,
        (	callable(Goal) ->
        call(Goal)
        ;	var(Goal) ->
        throw(error(instantiation_error, logtalk({Goal}, ExCtx)))
        ;	throw(error(type_error(callable, Goal), logtalk({Goal}, ExCtx)))
        ).

        metacall(Goal, ExCtx) :-
        (	executionContext(ExCtx, _, _, _, _, CallerExCtx-MetaArgs, _),
        member_var(Goal, MetaArgs) ->
        metacallSender(Goal, ExCtx, CallerExCtx, [])
        ;	metacallLocal(Goal, ExCtx)
        ).



   //quantified_metacall(?term, ?term, +executionContext)
        %
   //performs a possibly qualified meta-call at runtime for goals within bagof/3 and setof/3 calls
        %
   //the first argument is the original goal in the bagof/3 or setof/3 call and it's used to check
   //in which context the meta-call should take place
        %
   //the second argument is the original goal without existential variables that will be meta-called

        quantified_metacall(Goal, _, ExCtx) :-
        var(Goal),
        throw(error(instantiation_error, logtalk(call(Goal), ExCtx))).

        quantified_metacall({Goal}, _, ExCtx) :-
   //pre-compiled meta-calls or calls in "user" (compiler bypass)
        !,
        (	callable(Goal) ->
        call(Goal)
        ;	var(Goal) ->
        throw(error(instantiation_error, logtalk({Goal}, ExCtx)))
        ;	throw(error(type_error(callable, Goal), logtalk({Goal}, ExCtx)))
        ).

        quantified_metacall(QGoal, Goal, ExCtx) :-
        (	executionContext(ExCtx, _, _, _, _, CallerExCtx-MetaArgs, _),
        member_var(QGoal, MetaArgs) ->
        metacallSender(Goal, ExCtx, CallerExCtx, [])
        ;	metacallLocal(Goal, ExCtx)
        ).



   //metacallLocal(+callable, +executionContext)
        %
   //performs a local meta-call at runtime

        metacallLocal(Pred, ExCtx) :-
        executionContext(ExCtx, Entity, Sender, This, Self, _, Stack),
        (	Current_object_(Entity, Prefix, _, Def, _, _, _, _, DDef, _, Flags) ->
        (	% in the most common case we're meta-calling a user defined static predicate
        call(Def, Pred, ExCtx, TPred) ->
        call(TPred)
        ;	% or a user defined dynamic predicate
        call(DDef, Pred, ExCtx, TPred) ->
        call(TPred)
        ;	% in the worst case we need to compile the meta-call
        CompCtx(Ctx, _, ExCtx, Entity, Sender, This, Self, Prefix, [], _, ExCtx, runtime, Stack, _),
        catch(Compile_body(Pred, TPred, DPred, Ctx), Error, throw(error(Error, logtalk(call(Pred), ExCtx)))),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        catch(DPred, error(Error,_), throw(error(Error, logtalk(call(Pred), ExCtx))))
        ;	catch(TPred, error(Error,_), throw(error(Error, logtalk(call(Pred), ExCtx))))
        )
        )
        ;	CurrentCategory_(Entity, Prefix, _, Def, _, Flags),
        (	% in the most common case we're meta-calling a user defined predicate
        call(Def, Pred, ExCtx, TPred) ->
        call(TPred)
        ;	% in the worst case we need to compile the meta-call
        CompCtx(Ctx, _, ExCtx, Entity, Sender, This, Self, Prefix, [], _, ExCtx, runtime, [], _),
        catch(Compile_body(Pred, TPred, DPred, Ctx), Error, throw(error(Error, logtalk(call(Pred), ExCtx)))),
        (	Flags /\ 512 =:= 512 ->
   //category compiled in debug mode
        catch(DPred, error(Error,_), throw(error(Error, logtalk(call(Pred), ExCtx))))
        ;	catch(TPred, error(Error,_), throw(error(Error, logtalk(call(Pred), ExCtx))))
        )
        )
        ).



   //metacallSender(+callable, +executionContext, +executionContext, +list)
        %
   //performs a meta-call in "sender" at runtime

        metacallSender(Pred, ExCtx, CallerExCtx, ExtraVars) :-
        executionContext(CallerExCtx, CallerEntity, Sender, This, Self, _, Stack),
        (	CallerEntity == user ->
        catch(Pred, error(Error,_), throw(error(Error, logtalk(call(Pred), CallerExCtx))))
        ;	Current_object_(CallerEntity, CallerPrefix, _, Def, _, _, _, _, DDef, _, Flags) ->
        (	% in the most common case we're meta-calling a user defined static predicate
        call(Def, Pred, CallerExCtx, TPred) ->
        call(TPred)
        ;	% or a user defined dynamic predicate
        call(DDef, Pred, CallerExCtx, TPred) ->
        call(TPred)
        ;	% in the worst case we have a control construct or a built-in predicate
        (	ExtraVars == [] ->
        MetaCallCtx = []
        ;	MetaCallCtx = ExCtx-ExtraVars
        ),
        executionContext(NewCallerExCtx, CallerEntity, Sender, This, Self, MetaCallCtx, Stack),
        CompCtx(Ctx, _, NewCallerExCtx, CallerEntity, Sender, This, Self, CallerPrefix, ExtraVars, MetaCallCtx, NewCallerExCtx, runtime, Stack, _),
        catch(Compile_body(Pred, TPred, DPred, Ctx), Error, throw(error(Error, logtalk(call(Pred), CallerExCtx)))),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        catch(DPred, error(Error,_), throw(error(Error, logtalk(call(Pred), CallerExCtx))))
        ;	catch(TPred, error(Error,_), throw(error(Error, logtalk(call(Pred), CallerExCtx))))
        )
        )
        ;	CurrentCategory_(CallerEntity, CallerPrefix, _, Def, _, Flags),
        (	% in the most common case we're meta-calling a user defined static predicate
        call(Def, Pred, CallerExCtx, TPred) ->
        call(TPred)
        ;	% in the worst case we have a control construct or a built-in predicate
        (	ExtraVars == [] ->
        MetaCallCtx = []
        ;	MetaCallCtx = ExCtx-ExtraVars
        ),
        executionContext(NewCallerExCtx, CallerEntity, Sender, This, Self, MetaCallCtx, Stack),
        CompCtx(Ctx, _, NewCallerExCtx, CallerEntity, Sender, This, Self, CallerPrefix, ExtraVars, MetaCallCtx, NewCallerExCtx, runtime, Stack, _),
        catch(Compile_body(Pred, TPred, DPred, Ctx), Error, throw(error(Error, logtalk(call(Pred), CallerExCtx)))),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        catch(DPred, error(Error,_), throw(error(Error, logtalk(call(Pred), CallerExCtx))))
        ;	catch(TPred, error(Error,_), throw(error(Error, logtalk(call(Pred), CallerExCtx))))
        )
        )
        ).



   //Call_withinContext(?term, ?term, +objectIdentifier)
        %
   //calls a goal within the context of the specified object when the object and/or the
   //goal are only known at runtime
        %
   //used mostly for debugging and for writing unit tests, the permission to perform a
   //context-switching call can be disabled in a per-object basis by using the compiler
   //flag "contextSwitchingCalls"

        Call_withinContext(Obj, Goal, ExCtx) :-
        Check(objectIdentifier, Obj, logtalk(Obj<<Goal, ExCtx)),
        Check(callable, Goal, logtalk(Obj<<Goal, ExCtx)),
        CompileContextSwitchCall(Obj, Goal, TGoal, ExCtx),
        call(TGoal).



   //Call_withinContext_nv(+objectIdentifier, +callable, +executionContext)
        %
   //calls a goal within the context of the specified object (arguments entityKind-checked
   //at compile time)

        Call_withinContext_nv(Obj, Goal, ExCtx) :-
        (	Obj == user ->
        catch(Goal, Error, Runtime_error_handler(error(Error, logtalk(user<<Goal, ExCtx))))
        ;	Current_object_(Obj, Prefix, _, Def, _, _, _, _, DDef, _, Flags) ->
        (	Flags /\ 256 =:= 256 ->
   //object compiled with context-switching calls allowed
        executionContext(ObjExCtx, Obj, Obj, Obj, Obj, [], []),
        (	% in the most common case we're calling a user defined static predicate
        call(Def, Goal, ObjExCtx, TGoal) ->
        catch(TGoal, Error, Runtime_error_handler(error(Error, logtalk(Obj<<Goal, ExCtx))))
   //or a user defined dynamic predicate
        ;	call(DDef, Goal, ObjExCtx, TGoal) ->
        catch(TGoal, Error, Runtime_error_handler(error(Error, logtalk(Obj<<Goal, ExCtx))))
        ;	% in the worst case we need to compile the goal
        CompCtx(ObjCtx, _, ObjExCtx, Obj, Obj, Obj, Obj, Prefix, [], _, ObjExCtx, runtime, [], _),
        catch(Compile_body(Goal, TGoal, DGoal, ObjCtx), Error, throw(error(Error, logtalk(Obj<<Goal, ExCtx)))),
        (	Flags /\ 512 =:= 512 ->
   //object compiled in debug mode
        catch(DGoal, Error, throw(error(Error, logtalk(Obj<<Goal, ExCtx))))
        ;	catch(TGoal, Error, Runtime_error_handler(error(Error, logtalk(Obj<<Goal, ExCtx))))
        )
        )
        ;	throw(error(permission_error(access, database, Goal), logtalk(Obj<<Goal, ExCtx)))
        )
        ;	throw(error(existence_error(object, Obj), logtalk(Obj<<Goal, ExCtx)))
        ).



   //CallIn_this(+callable, +executionContext)
        %
   //calls a dynamic predicate in "this" from within a category at runtime

        CallIn_this(Pred, ExCtx) :-
        executionContext_this_entity(ExCtx, This, _),
        Current_object_(This, _, _, Def, _, _, _, _, DDef, _, _),
        (	% the object definition may include some initial clauses for the dynamic predicate
        call(Def, Pred, ExCtx, TPred) ->
        call(TPred)
        ;	% or the clauses for the dynamic predicate may be defined only at runtime
        call(DDef, Pred, ExCtx, TPred) ->
        call(TPred)
        ;	% no definition found; fail as per closed-world assumption
        fail
        ).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // support for categories that complement objects (hot patching)
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //lookup predicate declarations in any category that complements the given object

        Complemented_object(This, ThisDcl, Alias, Scope, Meta, Flags, SCtn, TCtn) :-
        Complemented_object_(This, _, Dcl, _, Rnm),
        (	call(Dcl, Alias, Scope, Meta, Flags, TCtn),
        SCtn = This
        ;	% categories can define aliases for complemented object predicates
        call(Rnm, This, Pred, Alias),
        Pred \= Alias,
        call(ThisDcl, Pred, Scope, Meta, Flags, SCtn, TCtn)
        ).



   //lookup predicate definitions in any category that complements the given object

        Complemented_object(This, ThisDef, Alias, OExCtx, Call, Ctn) :-
        Complemented_object_(This, Ctg, _, Def, Rnm),
        executionContext_update_this_entity(OExCtx, This, This, CExCtx, This, Ctg),
        (	call(Def, Alias, CExCtx, Call, Ctn)
        ;	% categories may also define aliases for complemented object predicates
        call(Rnm, This, Pred, Alias),
        Pred \= Alias,
        call(ThisDef, Pred, OExCtx, Call, _, Ctn)
        ).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // debugging base support
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //Debug(+compound, @executionContext)
        %
   //calls all defined trace event handlers and either use a loaded debug
   //handler provider for the debug event or simply call the debugging goals
   //to prevent execution of code compiled in debug mode to simply fail
        %
   //we can have multiple trace event handlers but only one debug handler
   //(the compiler prints a warning when attempting to load a second handler)

        Debug(Event, ExCtx) :-
        '$logtalk#0.trace_event#2(Event, ExCtx, _),
        fail.

        Debug(Event, ExCtx) :-
        '$logtalk#0.debug_handler_provider#1(_, _),
        !,
        '$logtalk#0.debug_handler#2(Event, ExCtx, _).

   //top_goal(Goal, TGoal)
        Debug(top_goal(_, TGoal), _) :-
        call(TGoal).

   //goal(Goal, TGoal)
        Debug(goal(_, TGoal), _) :-
        call(TGoal).

   //fact(Entity, Fact, ClauseNumber, File, BeginLine)
        Debug(fact(_, _, _, _, _), _).

   //rule(Entity, Head, ClauseNumber, File, BeginLine)
        Debug(rule(_, _, _, _, _), _).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // message printing support
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //print_message(+atom_orCompound, +atom, +nonvar)
        %
   //internal predicate used by the compiler and runtime to print a message;
   //we fake the execution context argument to call the corresponding method
   //in the "logtalk" built-in object

        print_message(Kind, Component, Message) :-
        (	builtIn_entitiesLoaded_' ->
   //"logtalk" built-in object loaded
        executionContext(ExCtx, logtalk, logtalk, logtalk, logtalk, [], []),
        '$logtalk#0.print_message#3(Kind, Component, Message, ExCtx)
        ;	% still compiling the default built-in entities
        CompilerFlag(report, off) ->
   //no message printing required
        true
        ;	% bare-bones message printing
        writeq(Component), write(' '), write(Kind), write(': '), writeq(Message), nl
        ).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // compiler
        %
   // compiles Logtalk source files into intermediate Prolog source files
   // and calls the backend Prolog compiler on the generated files
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //LoadFiles(@sourceFile_name, @list(compilerFlag))
   //LoadFiles(@list(sourceFile_name), @list(compilerFlag))
        %
   //compiles to disk and then loads to memory a source file or a list of source files
        %
   //a call to this predicate can trigger other calls to it, therefore we must clean
   //the compilation auxiliary predicates before compiling a file

        LoadFiles([], _) :-
        !.

        LoadFiles([File| Files], Flags) :-
        !,
        Clean_ppFileClauses',
        SetCompilerFlags(Flags),
        LoadFile(File, Flags),
        LoadFiles(Files, Flags).

        LoadFiles(File, Flags) :-
        LoadFiles([File], Flags).



   //LoadFile(@sourceFile_name, @list)
        %
   //compiles to disk and then loads to memory a source file

        LoadFile(File, [RelativeTo| Flags]) :-
        (	SourceFile_name(File, [RelativeTo| Flags], Directory, Name, Extension, SourceFile),
        File_exists(SourceFile) ->
        true
        ;	throw(error(existence_error(file, File), _))
        ),
        objectFile_name(Directory, Name, Extension, ObjectFile),
        atomConcat(Name, Extension, Basename),
        retractall(ppFile_pathsFlags_(_, _, _, _, _)),
        assertz(ppFile_pathsFlags_(Basename, Directory, SourceFile, ObjectFile, Flags)),
        (	LoadedFile_(Basename, Directory, PreviousMode, PreviousFlags, _, _, LoadingTimeStamp),
        \+ FailedFile_(SourceFile) ->
   //we're attempting to reload a file
        (	member(reload(Reload), PreviousFlags) ->
        true
        ;	CompilerFlag(reload, Reload)
        ),
        (	Reload == skip ->
   //skip reloading already loaded files
        print_message(comment(loading), core, skippingReloadingFile(SourceFile, Flags)),
   //but save the file loading dependency on a parent file if it exists
        SaveFileLoadingDependency(SourceFile)
        ;	Reload == changed,
        PreviousFlags == Flags,
        \+ ChangedCompilation_mode(PreviousMode, PreviousFlags),
        File_modification_time(SourceFile, CurrentTimeStamp),
        CurrentTimeStamp @=< LoadingTimeStamp ->
   //file was not modified since loaded and same explicit flags and compilation mode as before
        print_message(comment(loading), core, skippingReloadingFile(SourceFile, Flags)),
   //but save the file loading dependency on a parent file if it exists
        SaveFileLoadingDependency(SourceFile)
        ;	% we're reloading a source file
        print_message(silent(loading), core, reloadingFile(SourceFile, Flags)),
        CompileAndLoadFile(SourceFile, Flags, ObjectFile, Directory),
        print_message(comment(loading), core, reloadedFile(SourceFile, Flags))
        )
        ;	% first time loading this source file or previous attempt failed due to compilation error
        print_message(silent(loading), core, loadingFile(SourceFile, Flags)),
        CompileAndLoadFile(SourceFile, Flags, ObjectFile, Directory),
        print_message(comment(loading), core, loadedFile(SourceFile, Flags))
        ).


        CompileAndLoadFile(SourceFile, Flags, ObjectFile, Directory) :-
        retractall(FailedFile_(SourceFile)),
   //save the file loading dependency on a parent file if it exists
        SaveFileLoadingDependency(SourceFile),
        retractall(FileLoadingStack_(SourceFile, Directory)),
        asserta(FileLoadingStack_(SourceFile, Directory)),
   //compile the source file to an intermediate Prolog file on disk;
   //a syntax error while reading the terms in a source file results
   //in a printed message and failure instead of an exception but we
   //need to pass the failure up to the caller
        (	CompileFile(SourceFile, Flags, ObjectFile, loading) ->
        true
        ;	retractall(FileLoadingStack_(SourceFile, Directory)),
        propagateFailure_to_parentFiles(SourceFile),
        fail
        ),
   //compile and load the intermediate Prolog file
        LoadCompiledFile(SourceFile, Flags, ObjectFile),
        retractall(FileLoadingStack_(SourceFile, _)),
        retractall(ppFile_pathsFlags_(_, _, _, _, _)).


        SaveFileLoadingDependency(SourceFile) :-
        (	FileLoadingStack_(ParentSourceFile, _),
        SourceFile \== ParentSourceFile ->
   //as a file can have multiple parents, we only
   //ensure that there aren't duplicated entries
        retractall(parentFile_(SourceFile, ParentSourceFile)),
        asserta(parentFile_(SourceFile, ParentSourceFile))
        ;	% no parent file
        true
        ).


        LoadCompiledFile(SourceFile, Flags, ObjectFile) :-
   //retrieve the backend Prolog specific file loading options
        CompilerFlag(prologLoader, DefaultOptions),
   //loading a file can result in the redefinition of existing
   //entities thus potentially invalidating cache entries
        CleanLookupCaches',
        ReportRedefined_entities',
        (	ppFile_encoding_(_, Encoding, _) ->
   //use the same encoding as the original source file but do not use the inferred
   //bom/1 option as it would only work with some backend Prolog compilers
        Options = [encoding(Encoding)| DefaultOptions]
        ;	Options = DefaultOptions
        ),
   //clean all runtime clauses as an initialization goal in the intermediate Prolog file
   //that is loaded next may create dynamic entities
        Clean_ppRuntimeClauses',
   //load the generated intermediate Prolog file but cope with unexpected error or failure
        (	(	catch(Load_prologCode(ObjectFile, SourceFile, Options), Error, true) ->
        (	var(Error) ->
        true
        ;	% an error while loading the generated intermediate Prolog files
   //is usually caused by writeCanonical/2 and/or read_term/3 bugs
        print_message(error, core, loading_error(SourceFile, Error)),
        fail
        )
        ;	print_message(error, core, loadingFailure(SourceFile)),
        fail
        ) ->
        true
        ;	% loading of the intermediate Prolog file failed
        retractall(FileLoadingStack_(SourceFile, _)),
        propagateFailure_to_parentFiles(SourceFile),
        DeleteIntermediateFiles(ObjectFile),
        fail
        ),
   //cleanup intermediate files if necessary
        (	member(clean(on), Flags) ->
        DeleteIntermediateFiles(ObjectFile)
        ;	CompilerFlag(clean, on) ->
        DeleteIntermediateFiles(ObjectFile)
        ;	true
        ).


        DeleteIntermediateFiles(ObjectFile) :-
   //try to delete the intermediate Prolog file (ignore failure or error)
        File_exists(ObjectFile),
        catch(DeleteFile(ObjectFile), _, true),
        fail.

        DeleteIntermediateFiles(ObjectFile) :-
   //try to delete any Prolog dialect specific auxiliary files (ignore failure or error)
        File_extension(object, ObjectExtension),
        atomConcat(Name, ObjectExtension, ObjectFile),
        File_extension(tmp, TmpExtension),
        atomConcat(Name, TmpExtension, TmpFile),
        File_exists(TmpFile),
        catch(DeleteFile(TmpFile), _, true),
        fail.

        DeleteIntermediateFiles(_).



   //ReportRedefined_entities'
        %
   //prints a warning for all entities that are about to be redefined
        %
   //also retracts old runtime clauses for the entity being redefined for safety

        ReportRedefined_entities' :-
        (	ppRuntimeClause_(Current_protocol_(Entity, _, _, _, _))
        ;	ppRuntimeClause_(CurrentCategory_(Entity, _, _, _, _, _))
        ;	ppRuntimeClause_(Current_object_(Entity, _, _, _, _, _, _, _, _, _, _))
        ),
        Redefined_entity(Entity, Type, OldFile, NewFile, Lines),
        ReportRedefined_entity(Type, Entity, OldFile, NewFile, Lines),
        Retract_oldRuntimeClauses(Type, Entity),
        fail.

        ReportRedefined_entities'.



   //Redefined_entity(@entityIdentifier, -atom, -atom, -atom, -pair(integer))
        %
   //true if an entity of the same name is already loaded; returns entity entityKind

        Redefined_entity(Entity, Type, OldFile, NewFile, Lines) :-
   //check that an entity with the same identifier is already loaded
        (	Current_object_(Entity, _, _, _, _, _, _, _, _, _, Flags) ->
        Type = object
        ;	Current_protocol_(Entity, _, _, _, Flags) ->
        Type = protocol
        ;	CurrentCategory_(Entity, _, _, _, _, Flags),
        Type = category
        ),
        (	Flags /\ 1 =:= 1 ->
   //built-in entity; no redefinition allowed
        throw(permission_error(modify, Type, Entity))
        ;	% redefinable entity but, in the presence of entity dynamic predicates, when
   //using some backend Prolog compilers, some old dynamic clauses may persist
        true
        ),
        (	% check file information using the fileLines/4 entity property, if available
        entity_property_(Entity, fileLines(OldBasename, OldDirectory, _, _)),
        ppRuntimeClause_(entity_property_(Entity, fileLines(NewBasename, NewDirectory, Start, End))) ->
        atomConcat(OldDirectory, OldBasename, OldFile),
        atomConcat(NewDirectory, NewBasename, NewFile),
        Lines = Start-End
        ;	% no fileLines/4 entity property (due to compilation with the sourceData flag turned off)
        OldFile = nil,
        NewFile = nil,
        Lines = '-(-1, -1)
        ).



   //ReportRedefined_entity(+atom, @entityIdentifier, +atom, +atom, +pair(integer))
        %
   //prints an informative message or a warning for a redefined entity

        ReportRedefined_entity(Type, Entity, OldFile, NewFile, Lines) :-
        (	OldFile == NewFile ->
   //either reloading the same source file or no source file data is available; assume entity redefinition normal
        print_message(comment(loading), core, redefining_entity(Type, Entity))
        ;	% we've conflicting entity definitions coming from different source files
        IncrementLoading_warningsCounter',
        print_message(warning(loading), core, redefining_entityFromFile(NewFile, Lines, Type, Entity, OldFile))
        ).



   //Retract_oldRuntimeClauses(+atom, @entityIdentifier)
        %
   //cleans all references to an entity that is about to be redefined
   //from the runtime tables

        Retract_oldRuntimeClauses(object, Entity) :-
        retractall(before_event_(_, _, _, Entity, _)),
        retractall(After_event_(_, _, _, Entity, _)),
        retractall(Current_object_(Entity, _, _, _, _, _, _, _, _, _, _)),
        retractall(entity_property_(Entity, _)),
        retractall(predicate_property_(Entity, _, _)),
        retractall(Implements_protocol_(Entity, _, _)),
        retractall(ImportsCategory_(Entity, _, _)),
        retractall(InstantiatesClass_(Entity, _, _)),
        retractall(SpecializesClass_(Entity, _, _)),
        retractall(extends_object_(Entity, _, _)),
        retractall(Current_engine_(Entity, _, _, _)).

        Retract_oldRuntimeClauses(protocol, Entity) :-
        retractall(Current_protocol_(Entity, _, _, _, _)),
        retractall(entity_property_(Entity, _)),
        retractall(predicate_property_(Entity, _, _)),
        retractall(extends_protocol_(Entity, _, _)).

        Retract_oldRuntimeClauses(category, Entity) :-
        retractall(CurrentCategory_(Entity, _, _, _, _, _)),
        retractall(entity_property_(Entity, _)),
        retractall(predicate_property_(Entity, _, _)),
        retractall(Implements_protocol_(Entity, _, _)),
        retractall(extendsCategory_(Entity, _, _)),
        retractall(Complemented_object_(_, Entity, _, _, _)).



   //CompileFiles(@sourceFile_name, @list(compilerFlag))
   //CompileFiles(@list(sourceFile_name), @list(compilerFlag))
        %
   //compiles to disk a source file or a list of source files
        %
   //a call to this predicate can trigger other calls to it, therefore we must clean
   //the compilation auxiliary predicates before compiling a file

        CompileFiles([], _) :-
        !.

        CompileFiles([File| Files], [RelativeTo| Flags]) :-
        !,
        Clean_ppFileClauses',
        SetCompilerFlags(Flags),
        (	SourceFile_name(File, [RelativeTo| Flags], Directory, Name, Extension, SourceFile),
        File_exists(SourceFile) ->
        true
        ;	throw(error(existence_error(file, File), _))
        ),
        objectFile_name(Directory, Name, Extension, ObjectFile),
        atomConcat(Name, Extension, Basename),
        retractall(ppFile_pathsFlags_(_, _, _, _, _)),
        assertz(ppFile_pathsFlags_(Basename, Directory, SourceFile, ObjectFile, Flags)),
        CompileFile(SourceFile, Flags, ObjectFile, compiling),
        CompileFiles(Files, [RelativeTo| Flags]).

        CompileFiles(File, Flags) :-
        CompileFiles([File], Flags).



   //CompileFile(@sourceFile_name, @list, @sourceFile_name, +atom)
        %
   //compiles to disk a source file

        CompileFile(SourceFile, Flags, ObjectFile, Action) :-
        (	% interpret a clean(on) setting as (also) meaning that any
   //existing intermediate Prolog files should be disregarded
        CompilerFlag(clean, off),
        File_exists(ObjectFile),
        CompareFile_modification_times(Result, SourceFile, ObjectFile),
        Result \== (>) ->
        print_message(silent(compiling), core, up_toDateFile(SourceFile, Flags))
        ;	% the intermediate Prolog file doesn't exist or it's outdated
        print_message(silent(compiling), core, compilingFile(SourceFile, Flags)),
        CompileFile(SourceFile, ObjectFile),
        CompilerFlag(prologCompiler, Options),
        Compile_prologCode(ObjectFile, SourceFile, Options),
        (	Action == loading ->
        print_message(silent(compiling), core, compiledFile(SourceFile, Flags))
        ;	% Action == compiling,
        print_message(comment(compiling), core, compiledFile(SourceFile, Flags))
        )
        ).


   //a file can be loaded by a loader file that, in turn, may also be loaded by
   //another loader file; propagating a file loading failure to its parent files
   //provides better top-level usability allowing realoding of fixed files by
   //simply relaoding the loader files, which also ensures loading of any files
   //to be loaded after the broken file that were not loaded in the previous
   //attempt

        propagateFailure_to_parentFiles(File) :-
        (	parentFile_(File, Parent) ->
        propagateFailure_to_parentFiles(Parent)
        ;	assertz(FailedFile_(File))
        ).



   //CompareFile_modification_times(?atom, +atom, +atom)
        %
   //compare file modification times; same argument order as the
   //standard compare/3 predicate and same possible values for the
   //first argument (<, >, or =)

        CompareFile_modification_times(Result, File1, File2) :-
        File_modification_time(File1, Time1),
        File_modification_time(File2, Time2),
        compare(Result, Time1, Time2).



   //write_entityCode'
        %
   //writes to disk the entity compiled code

        write_entityCode' :-
   //avoid a spurious choice-point with some backend Prolog compilers
        stream_property(Output, alias(logtalkCompiler_output)), !,
        catch(
        write_entityCode(Output),
        Error,
        Compiler_outputStream_error_handler(Output, Error)
        ).


        write_entityCode(Output) :-
        ppFile_pathsFlags_(_, _, Path, _, _),
   //write any plain Prolog terms that may precede the entity definition
        write_prolog_terms(Output, Path),
        write_entityDirectives(Output, Path),
        pp_entity_(_, _, _, _, Rnm),
        write_entityClauses(Output, Path, Rnm).



   //SourceFile_name(+atom, +list(callable), -atom, -atom, -atom, -atom)
        %
   //converts a source file specification into a source file directory, basename,
   //and full path
        %
   //the source file specification can be either absolute or relative and may or
   //may not include a file name extension
        %
   //when the source file specification doesn't include a file extension, this
   //predicate provides a solution for each defined Logtalk and Prolog source
   //file extension; callers should test if the returned full path exists and
   //commit to that solution when not simply generating all possible solutions

        SourceFile_name(FilePath, Flags, Directory, Name, Extension, SourceFile) :-
        (	expand_path(FilePath, FilePath) ->
   //assume full path
        SourceFile0 = FilePath
        ;	% assume relative path and try possible alternatives
        (	once(FileLoadingStack_(_, ParentDirectory)),
   //parent file exists; try first a path relative to its directory
        atomConcat(ParentDirectory, FilePath, SourceFile0)
        ;	(	member(relative_to(BasePath), Flags)
        ;	member('$relative_to(BasePath), Flags)
        ),
        (	subAtom(BasePath, _, 1, 0, '/') ->
        atomConcat(BasePath, FilePath, SourceFile0)
        ;	atomConcat(BasePath, '/', BasePathSlash),
        atomConcat(BasePathSlash, FilePath, SourceFile0)
        )
        ;	% we may have a relative file path without any parent file
   //(e.g. when the user changes the working directory to the
   //directory containing the file to be loaded)
        expand_path(FilePath, SourceFile0)
        )
        ),
        DecomposeFile_name(SourceFile0, Directory, Name0, Extension0),
        (	% file extensions are defined in the Prolog adapter files (there
   //might be multiple extensions defined for the same entityKind of file)
        File_extension(logtalk, Extension0) ->
   //declared extension for this entityKind of file is present
        SourceFile = SourceFile0,
        Name = Name0,
        Extension = Extension0
        ;	File_extension(prolog, Extension0) ->
   //assume Prolog file being compiled as a Logtalk file
        SourceFile = SourceFile0,
        Name = Name0,
        Extension = Extension0
        ;	% no Logtalk or Prolog extension for this entityKind of file; generate possible
   //basenames starting with Logtalk extensions followed by Prolog extensions
        (	File_extension(logtalk, Extension)
        ;	File_extension(prolog, Extension)
        ),
        atomConcat(SourceFile0, Extension, SourceFile),
        atomConcat(Name0, Extension0, Name)
        ;	% use basename as-is
        SourceFile = SourceFile0,
        atomConcat(Name0, Extension0, Name),
        Extension = ''
        ).



   //objectFile_name(+atom, +atom, +atom, -atom)
        %
   //converts a source file full path into an object file full path

        objectFile_name(SourceDirectory, SourceName, SourceExtension, ObjectFile) :-
   //temporary files are stored in the defined scratch directory
        CompilerFlag(scratchDirectory, ScratchDirectory0),
   //allow using library notation to specify the scratch directory
        CheckAnd_expandSourceFile(ScratchDirectory0, ScratchDirectory1),
   //make sure that the scratch directory path ends with a slash
        (	subAtom(ScratchDirectory1, _, _, 0, '/') ->
        ScratchDirectory = ScratchDirectory1
        ;	atomConcat(ScratchDirectory1, '/', ScratchDirectory)
        ),
        (	subAtom(ScratchDirectory, 0, 2, _, './') ->
   //relative directory path
        subAtom(ScratchDirectory, 2, _, 0, ScratchDirectorySuffix),
        atomConcat(SourceDirectory, ScratchDirectorySuffix, ObjectDirectory)
        ;	% assume absolute directory path
        ObjectDirectory = ScratchDirectory
        ),
   //append (if supported by the backend compiler) a directory hash value to the
   //intermediate Prolog file name to try to avoid file name collisions when
   //collecting all the intermediate files in the same directory for embedding
        Directory_hashAsAtom(SourceDirectory, Hash),
        atomConcat('_', Hash, UnderscoreHash),
   //add a suffix based on the original extension to the file name to avoid
   //intermediate and temporary file name conflicts when compiling two or
   //more source files that share the same name but use different extensions
        (	Source_extensionSuffix(SourceExtension, Suffix) ->
        true
        ;	subAtom(SourceExtension, 1, _, 0, Suffix0) ->
        atomConcat('_', Suffix0, Suffix)
        ),
        atomConcat(SourceName, UnderscoreHash, ObjectName0),
        atomConcat(ObjectName0, Suffix, ObjectName),
   //there must be a single object file extension defined in the Prolog adapter files
        File_extension(object, ObjectExtension),
        atomConcat(ObjectName, ObjectExtension, ObjectBasename),
        atomConcat(ObjectDirectory, ObjectBasename, ObjectFile),
   //make sure the scratch directory exists
        makeDirectory(ObjectDirectory).


   //common source extensions and corresponding precomputed suffixes
        Source_extensionSuffix('.lgt', 'Lgt').
        Source_extensionSuffix('.logtalk', 'Logtalk').
        Source_extensionSuffix('.pl', '_pl').
        Source_extensionSuffix('.prolog', '_prolog').
        Source_extensionSuffix('', '').



   //CompileFile(+atom, +atom)
        %
   //compiles a source file storing the resulting code in memory

        CompileFile(SourceFile, ObjectFile) :-
   //open the Logtalk source code file for reading
        catch(
        open(SourceFile, read, Input, [alias(logtalkCompilerInput)]),
        OpenError,
        CompilerStream_error_handler(OpenError)
        ),
   //look for an encoding/1 directive that, when present, must be the first term on a source file
        catch(
        ReadFile_term(SourceFile, Input, Term, [singletons(Singletons)], Lines),
        InputError,
        CompilerFirst_term_error_handler(SourceFile, Lines, InputError)
        ),
        catch(
        CheckFor_encodingDirective(Term, SourceFile, Input, NewInput, OutputOptions),
        FirstTermError,
        CompilerFirst_term_error_handler(SourceFile, Lines, FirstTermError)
        ),
   //open a corresponding Prolog file for writing generated code using any found encoding/1 directive
        catch(
        open(ObjectFile, write, Output, [alias(logtalkCompiler_output)| OutputOptions]),
        OpenError,
        CompilerStream_error_handler(OpenError)
        ),
        catch(
        write_encodingDirective(Output, SourceFile),
        WriteError,
        CompilerStream_error_handler(WriteError)
        ),
   //generate a begin_ofFile term for term-expansion
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, compile(user), _, 0-0),
        CompileFile_term(begin_ofFile, Ctx),
   //read and compile the remaining terms in the Logtalk source file
        catch(
        CompileFile_term(Term, Singletons, Lines, SourceFile, NewInput),
        Error,
        FirstStage_error_handler(Error)
        ),
        Close(NewInput),
   //finish writing the generated Prolog file
        catch(
        writeRuntime_tables(Output),
        OutputError,
        Compiler_outputStream_error_handler(Output, OutputError)
        ),
        Close(Output),
        Restore_global_operator_table'.


        writeRuntime_tables(Output) :-
        generateLoadedFile_table_entry(SourceFile),
   //write out any Prolog code occurring after the last source file entity
        write_prolog_terms(Output, SourceFile),
   //write entity runtime directives and clauses
        writeRuntimeClauses(Output, SourceFile),
   //write initialization/1 directive at the end of the file to improve
   //compatibility with non-ISO compliant Prolog compilers
        writeInitializationDirective(Output, SourceFile).


        generateLoadedFile_table_entry(SourceFile) :-
        ppFile_pathsFlags_(Basename, Directory, SourceFile, ObjectFile, Flags),
   //the make predicate will reload a file if the compilation mode changed ...
        (	CompilerFlag(debug, on) ->
        Mode = debug
        ;	CompilerFlag(optimize, on) ->
        Mode = optimal
        ;	Mode = normal
        ),
   //... or if the file modification date changed (e.g. to fix compilation errors)
        File_modification_time(SourceFile, TimeStamp),
   //compute text properties that are only available after successful file compilation
        (	ppFile_encoding_(Encoding, _, _) ->
        (	ppFile_bom_(BOM) ->
        TextProperties = [encoding(Encoding), BOM]
        ;	TextProperties = [encoding(Encoding)]
        )
        ;	TextProperties = []
        ),
        assertz(ppRuntimeClause_(LoadedFile_(Basename, Directory, Mode, Flags, TextProperties, ObjectFile, TimeStamp))).



   //CheckFor_encodingDirective(?term, +atom, @stream, -stream, -list)
        %
   //encoding/1 directives must be used during entity compilation and for the
   //encoding of the generated Prolog files; a BOM present in the source file
   //is inherited by the generated Prolog file

        CheckFor_encodingDirective(Term, _, _, _, _) :-
        var(Term),
        throw(error(instantiation_error, term(Term))).

        CheckFor_encodingDirective((:- Term), _, _, _, _) :-
        var(Term),
        throw(error(instantiation_error, directive(Term))).

        CheckFor_encodingDirective((:- encoding(LogtalkEncoding)), Source, Input, NewInput, [encoding(PrologEncoding)|BOM]) :-
        !,
        (	var(LogtalkEncoding) ->
        throw(error(instantiation_error, directive(encoding(LogtalkEncoding))))
        ;	prologFeature(encodingDirective, unsupported) ->
        throw(error(resource_error(text_encodingSupport), directive(encoding(LogtalkEncoding))))
        ;	% the conversion between Logtalk and Prolog encodings is defined in the adapter files
        Logtalk_prolog_encoding(LogtalkEncoding, PrologEncoding, Input) ->
        SourceFileContext(File, BeginLine-EndLine),
        assertz(ppFile_encoding_(LogtalkEncoding, PrologEncoding, BeginLine)),
   //check that the encoding/1 directive is found in the first line
        (	BeginLine =:= 1 ->
        true
        ;	IncrementCompiling_warningsCounter',
        print_message(warning(general), core, misplaced_encodingDirective(File, BeginLine-EndLine))
        ),
   //close and reopen the source file using the specified encoding
        Close(Input),
        open(Source, read, NewInput, [alias(logtalkCompilerInput), encoding(PrologEncoding)]),
        (	(	catch(stream_property(NewInput, bom(Boolean)), _, fail)
   //SWI-Prolog and YAP
        ;	catch(stream_property(NewInput, encodingSignature(Boolean)), _, fail)
   //SICStus Prolog
        ) ->
        BOM = [bom(Boolean)],
        assertz(ppFile_bom_(bom(Boolean)))
        ;	BOM = []
        ),
   //throw away the already processed encoding/1 directive
        ReadFile_term(File, NewInput, _, [singletons(_)], _)
        ;	% encoding not recognized
        atom(LogtalkEncoding) ->
        throw(error(domain_error(text_encoding, LogtalkEncoding), directive(encoding(LogtalkEncoding))))
        ;	throw(error(type_error(atom, LogtalkEncoding), directive(encoding(LogtalkEncoding))))
        ).

   //assume no encoding/1 directive present on the source file
        CheckFor_encodingDirective(_, _, Input, Input, []).



   //CompileFile_term(@term, +list, +pair(intger), +atom, @stream)

        CompileFile_term((-), _, _, _, _) :-
   //catch variables
        throw(error(instantiation_error, term(_))).

        CompileFile_term(end_ofFile, _, Lines, _, _) :-
        !,
   //set the initial compilation context and the position for compiling the end_ofFile term
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, compile(user), _, Lines),
   //allow for term-expansion of the end_ofFile term
        CompileFile_term(end_ofFile, Ctx).

        CompileFile_term(Term, _, _, File, Input) :-
        ppCcSkipping_',
   //we're performing conditional compilation and skipping terms ...
        \+ IsConditionalCompilationDirective(Term),
   //... except for conditional compilation directives
        !,
        ReadFile_term(File, Input, Next, [singletons(NextSingletons)], NextLines),
        CompileFile_term(Next, NextSingletons, NextLines, File, Input).

        CompileFile_term(Term, Singletons, Lines, File, Input) :-
        ReportSingleton_variables(Singletons, Term),
   //set the initial compilation context and the position for compiling the term
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, compile(user), _, Lines),
        CompileFile_term(Term, Ctx),
        ReadFile_term(File, Input, Next, [singletons(NextSingletons)], NextLines),
        CompileFile_term(Next, NextSingletons, NextLines, File, Input).



   //AddReferenced_object(@objectIdentifier, @compilationContext)
        %
   //adds referenced object for later checking of references to unknown objects;
   //we also save the line numbers for the first reference to the object
        %
   //the definition is optimized to minimize the number of inferences for
   //runtime resolved ::/2 calls

        AddReferenced_object(Obj, Ctx) :-
        CompCtx_mode(Ctx, Mode),
        (	Mode == runtime ->
        true
        ;	Mode == compile(aux) ->
        true
        ;	% compiling a reference in a source file
        ppReferenced_object_(Obj, _, _) ->
   //not the first reference to this object
        true
        ;	atom(Obj) ->
        SourceFileContext(File, Lines),
        assertz(ppReferenced_object_(Obj, File, Lines))
        ;	% parametric object
        term_template(Obj, Template),
        SourceFileContext(File, Lines),
        assertz(ppReferenced_object_(Template, File, Lines))
        ).



   //AddReferenced_protocol(@protocolIdentifier, @compilationContext)
        %
   //adds referenced protocol for later checking of references to unknown protocols
   //we also save the line numbers for the first reference to the protocol

        AddReferenced_protocol(Ptc, Ctx) :-
        CompCtx_mode(Ctx, Mode),
        (	Mode == runtime ->
        true
        ;	Mode == compile(aux) ->
        true
        ;	% compiling a reference in a source file
        ppReferenced_protocol_(Ptc, _, _) ->
   //not the first reference to this protocol
        true
        ;	SourceFileContext(File, Lines),
        assertz(ppReferenced_protocol_(Ptc, File, Lines))
        ).



   //AddReferencedCategory(@categoryIdentifier, @compilationContext)
        %
   //adds referenced category for later checking of references to unknown categories
   //we also save the line numbers for the first reference to the category

        AddReferencedCategory(Ctg, Ctx) :-
        CompCtx_mode(Ctx, Mode),
        (	Mode == runtime ->
        true
        ;	Mode == compile(aux) ->
        true
        ;	% compiling a reference in a source file
        ppReferencedCategory_(Ctg, _, _) ->
   //not the first reference to this category
        true
        ;	atom(Ctg) ->
        SourceFileContext(File, Lines),
        assertz(ppReferencedCategory_(Ctg, File, Lines))
        ;	% parametric category
        term_template(Ctg, Template),
        SourceFileContext(File, Lines),
        assertz(ppReferencedCategory_(Template, File, Lines))
        ).



   //AddReferenced_module(@term, @compilationContext)
        %
   //adds referenced module for later checking of references to unknown modules
   //we also save the line numbers for the first reference to the module

        AddReferenced_module(Module, Ctx) :-
        CompCtx_mode(Ctx, Mode),
        (	Mode == runtime ->
        true
        ;	Mode == compile(aux) ->
        true
        ;	% compiling a reference in a source file
        var(Module) ->
   //module instantiated only at runtime
        true
        ;	ppReferenced_module_(Module, _, _) ->
   //not the first reference to this module
        true
        ;	SourceFileContext(File, Lines),
        assertz(ppReferenced_module_(Module, File, Lines))
        ).



   //AddReferenced_object_message(@compilation_mode, @term, @callable, @callable, @term)
        %
   //adds referenced object and message for supporting using reflection to
   //retrieve cross-reference information

        AddReferenced_object_message(runtime, _, _, _, _).

        AddReferenced_object_message(compile(aux), _, _, _, _) :-
        !.

        AddReferenced_object_message(compile(user), Obj, Pred, Alias, Head) :-
        (	var(Head) ->
   //not compiling a clause
        true
        ;	% add reference if first but be careful to not instantiate the object argument which may only be known at runtime
        functor(Pred, PredFunctor, PredArity),
        functor(Head, HeadFunctor, HeadArity),
        SourceFileContext(File, Lines),
        (	\+ \+ ppReferenced_object_message_(Obj, PredFunctor/PredArity, _, HeadFunctor/HeadArity, File, Lines) ->
        true
        ;	functor(Alias, AliasFunctor, PredArity),
        (	compound(Obj) ->
   //compile-time parametric object
        term_template(Obj, Template),
        assertz(ppReferenced_object_message_(Template, PredFunctor/PredArity, AliasFunctor/PredArity, HeadFunctor/HeadArity, File, Lines))
        ;	% runtime instantiated object or non-parametric object
        assertz(ppReferenced_object_message_(Obj, PredFunctor/PredArity, AliasFunctor/PredArity, HeadFunctor/HeadArity, File, Lines))
        )
        )
        ).



   //AddReferenced_module_predicate(@compilation_mode, @term, @callable, @callable, @term)
        %
   //adds referenced module for later checking of references to unknown modules
   //we also save the line numbers for the first reference to the module

        AddReferenced_module_predicate(runtime, _, _, _, _).

        AddReferenced_module_predicate(compile(aux), _, _, _, _) :-
        !.

        AddReferenced_module_predicate(compile(user), Module, Pred, Alias, Head) :-
        (	var(Head) ->
   //not compiling a clause
        true
        ;	% add reference if first but be careful to not instantiate the module argument which may only be known at runtime
        functor(Pred, PredFunctor, PredArity),
        functor(Head, HeadFunctor, HeadArity),
        SourceFileContext(File, Lines),
        (	\+ \+ ppReferenced_module_predicate_(Module, PredFunctor/PredArity, _, HeadFunctor/HeadArity, File, Lines) ->
        true
        ;	functor(Alias, AliasFunctor, PredArity),
        assertz(ppReferenced_module_predicate_(Module, PredFunctor/PredArity, AliasFunctor/PredArity, HeadFunctor/HeadArity, File, Lines))
        )
        ).



   //Add_entitySourceData(@atom, @entityIdentifier)
        %
   //adds entity source data

        Add_entitySourceData(Kind, Entity) :-
        (	CompilerFlag(sourceData, on) ->
        ppFile_pathsFlags_(_, _, MainFile, _, _),
        Add_entity_properties(Kind, Entity, MainFile),
        Add_entity_predicate_properties(Entity, MainFile)
        ;	true
        ).



   //Add_entity_properties(@atom, @entityIdentifier, +atom)
        %
   //adds entity properties related to the entity source file

        Add_entity_properties(Kind, Entity, _) :-
        ppFile_pathsFlags_(Basename, Directory, _, _, _),
        (	Kind == object ->
        ppReferenced_object_(Entity, _, Start-End)
        ;	Kind == protocol ->
        ppReferenced_protocol_(Entity, _, Start-End)
        ;	% Kind == category,
        ppReferencedCategory_(Entity, _, Start-End)
        ),
        assertz(ppRuntimeClause_(entity_property_(Entity, fileLines(Basename, Directory, Start, End)))),
        fail.

        Add_entity_properties(_, Entity, MainFile) :-
        ppReferenced_object_message_(Object, PredicateFunctor/Arity, AliasFunctor/Arity, Caller, File, Line-_),
        propertyLocation(MainFile, File, Line, Location),
        functor(Predicate, PredicateFunctor, Arity),
        (	pp_uses_non_terminal_(Object, _, _, Predicate, _, _) ->
        Arity2 is Arity - 2,
        NonTerminal = PredicateFunctor//Arity2
        ;	NonTerminal = no
        ),
        (	PredicateFunctor == AliasFunctor ->
        assertz(ppRuntimeClause_(entity_property_(Entity, calls(Object::PredicateFunctor/Arity, Caller, no, NonTerminal, Location))))
        ;	assertz(ppRuntimeClause_(entity_property_(Entity, calls(Object::PredicateFunctor/Arity, Caller, AliasFunctor/Arity, NonTerminal, Location))))
        ),
        fail.

        Add_entity_properties(_, Entity, MainFile) :-
        ppReferenced_module_predicate_(Module, PredicateFunctor/Arity, AliasFunctor/Arity, Caller, File, Line-_),
        propertyLocation(MainFile, File, Line, Location),
        functor(Predicate, PredicateFunctor, Arity),
        (	pp_use_module_non_terminal_(Module, _, _, Predicate, _, _) ->
        Arity2 is Arity - 2,
        NonTerminal = PredicateFunctor//Arity2
        ;	NonTerminal = no
        ),
        (	PredicateFunctor == AliasFunctor ->
        assertz(ppRuntimeClause_(entity_property_(Entity, calls(':(Module,PredicateFunctor/Arity), Caller, no, NonTerminal, Location))))
        ;	assertz(ppRuntimeClause_(entity_property_(Entity, calls(':(Module,PredicateFunctor/Arity), Caller, AliasFunctor/Arity, NonTerminal, Location))))
        ),
        fail.

        Add_entity_properties(_, Entity, MainFile) :-
        ppCallsSelf_predicate_(Predicate, Caller, File, Line-_),
        propertyLocation(MainFile, File, Line, Location),
        assertz(ppRuntimeClause_(entity_property_(Entity, calls(::Predicate, Caller, no, no, Location)))),
        fail.

        Add_entity_properties(_, Entity, MainFile) :-
        ppCallsSuper_predicate_(Predicate, Caller, File, Line-_),
        propertyLocation(MainFile, File, Line, Location),
        assertz(ppRuntimeClause_(entity_property_(Entity, calls(^^Predicate, Caller, no, no, Location)))),
        fail.

        Add_entity_properties(_, Entity, MainFile) :-
        ppCalls_predicate_(Predicate, _, Caller, File, Line-_),
        propertyLocation(MainFile, File, Line, Location),
        assertz(ppRuntimeClause_(entity_property_(Entity, calls(Predicate, Caller, no, no, Location)))),
        fail.

        Add_entity_properties(_, Entity, MainFile) :-
        pp_updates_predicate_(Dynamic, Updater, File, Line-_),
        propertyLocation(MainFile, File, Line, Location),
        assertz(ppRuntimeClause_(entity_property_(Entity, updates(Dynamic, Updater, no, no, Location)))),
        fail.

        Add_entity_properties(_, Entity, _) :-
        pp_entityInfo_(Info),
        assertz(ppRuntimeClause_(entity_property_(Entity, info(Info)))),
        fail.

        Add_entity_properties(_, Entity, _) :-
        findall(Define, pp_number_ofClausesRules_(_, _, Define, _), Defines),
        SumList(Defines, TotalDefines),
        findall(AuxDefine, (ppDefines_predicate_(_, Functor/Arity, _, _, compile(aux), _), pp_number_ofClausesRules_(Functor, Arity, AuxDefine, _)), AuxDefines),
        SumList(AuxDefines, TotalAuxDefines),
        findall(Provide, pp_number_ofClausesRules_(_, _, _, Provide, _), Provides),
        SumList(Provides, TotalProvides),
        Total is TotalDefines + TotalProvides,
        TotalUser is Total - TotalAuxDefines,
        assertz(ppRuntimeClause_(entity_property_(Entity, number_ofClauses(Total, TotalUser)))),
        fail.

        Add_entity_properties(_, Entity, _) :-
        findall(Define, pp_number_ofClausesRules_(_, _, _, Define), Defines),
        SumList(Defines, TotalDefines),
        findall(AuxDefine, (ppDefines_predicate_(_, Functor/Arity, _, _, compile(aux), _), pp_number_ofClausesRules_(Functor, Arity, _, AuxDefine)), AuxDefines),
        SumList(AuxDefines, TotalAuxDefines),
        findall(Provide, pp_number_ofClausesRules_(_, _, _, _, Provide), Provides),
        SumList(Provides, TotalProvides),
        Total is TotalDefines + TotalProvides,
        TotalUser is Total - TotalAuxDefines,
        assertz(ppRuntimeClause_(entity_property_(Entity, number_ofRules(Total, TotalUser)))),
        fail.

        Add_entity_properties(_, Entity, MainFile) :-
        pp_predicateAlias_(For, Original, Alias, NonTerminalFlag, File, Line-_),
        propertyLocation(MainFile, File, Line, Location),
        functor(Original, OriginalFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        assertz(ppRuntimeClause_(entity_property_(Entity, alias(For, OriginalFunctor/Arity, AliasFunctor/Arity, NonTerminalFlag, Location)))),
        fail.

        Add_entity_properties(_, _, _).



   //Add_entity_predicate_properties(@entityIdentifier, +atom)
        %
   //saves all entity predicate properties (at the end of entity compilation)
   //for use with the reflection built-in predicates and methods

        Add_entity_predicate_properties(Entity, MainFile) :-
        pp_predicateDefinitionLocation_(Other, Functor, Arity, File, Line),
        propertyLocation(MainFile, File, Line, Location),
        pp_number_ofClausesRules_(Other, Functor, Arity, Clauses, Rules),
        assertz(ppRuntimeClause_(predicate_property_(Other, Functor/Arity, clausesRulesLocationFrom(Clauses,Rules,Location,Entity)))),
        fail.

        Add_entity_predicate_properties(Entity, MainFile) :-
        pp_predicateDeclarationLocation_(Functor, Arity, File, Line),
        propertyLocation(MainFile, File, Line, Location),
        assertz(ppRuntimeClause_(predicate_property_(Entity, Functor/Arity, declarationLocation(Location)))),
        \+ ppDefines_predicate_(_, Functor/Arity, _, _, _, _),
        assertz(ppRuntimeClause_(predicate_property_(Entity, Functor/Arity, flagsClausesRulesLocation(0, 0, 0)))),
        fail.

        Add_entity_predicate_properties(Entity, MainFile) :-
        ppDefines_predicate_(_, Functor/Arity, _, _, Mode, _),
        (	ppInline_predicate_(Functor/Arity) ->
        Flags0 is 4
        ;	Flags0 is 0
        ),
        (	Arity2 is Arity - 2,
        Arity2 >= 0,
        ppDefines_non_terminal_(Functor, Arity2) ->
        Flags1 is Flags0 + 2
        ;	Flags1 is Flags0
        ),
        (	Mode == compile(aux) ->
        Flags is Flags1 + 1,
        File = MainFile,
        Line is 0
        ;	Flags is Flags1,
        pp_predicateDefinitionLocation_(Functor, Arity, File, Line)
        ),
        pp_number_ofClausesRules_(Functor, Arity, Clauses, Rules),
        propertyLocation(MainFile, File, Line, Location),
        assertz(ppRuntimeClause_(predicate_property_(Entity, Functor/Arity, flagsClausesRulesLocation(Flags, Clauses, Rules, Location)))),
        fail.

        Add_entity_predicate_properties(Entity, _) :-
        pp_mode_(Mode, Solutions, _, _),
        functor(Mode, Functor, Arity),
        (	pp_non_terminal_(Functor, Arity, ExtArity) ->
        assertz(ppRuntimeClause_(predicate_property_(Entity, Functor/ExtArity, mode(Mode, Solutions))))
        ;	assertz(ppRuntimeClause_(predicate_property_(Entity, Functor/Arity, mode(Mode, Solutions))))
        ),
        fail.

        Add_entity_predicate_properties(Entity, _) :-
        pp_predicateInfo_(Predicate, Info),
        assertz(ppRuntimeClause_(predicate_property_(Entity, Predicate, info(Info)))),
        fail.

        Add_entity_predicate_properties(_, _).


        propertyLocation(MainFile, MainFile, Line, Line) :-
        !.

        propertyLocation(_, File, Line, File-Line).



   //ReportSingleton_variables(@list, @term)
        %
   //reports the singleton variables found while compiling an entity term

        ReportSingleton_variables([], _).

        ReportSingleton_variables([Singleton| Singletons], Term) :-
        (	CompilerFlag(singleton_variables, warning),
        FilterSingleton_variable_names([Singleton| Singletons], Term, Names),
        Names \== [] ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(singleton_variables), core, singleton_variables(File, Lines, Type, Entity, Names, Term))
        ;	print_message(warning(singleton_variables), core, singleton_variables(File, Lines, Names, Term))
        )
        ;	true
        ).



   //FilterSingleton_variable_names(@list, -list(atom))
        %
   //filters variables whose name start with an underscore from a singletons list if
   //the corresponding compiler flag sets their interpretation to don't care variables

        FilterSingleton_variable_names(Singletons, Term, Names) :-
        (	CompilerFlag(underscore_variables, dontCare) ->
        FilterDontCare_variables(Singletons, SingletonsFiltered)
        ;	SingletonsFiltered = Singletons
        ),
        (	pp_parameter_variables_(ParameterVariables) ->
        Filter_parameter_variables(SingletonsFiltered, ParameterVariables, Names)
        ;	Term = (:- Directive),
        nonvar(Directive),
        Logtalk_openingDirective(Directive) ->
        Filter_parameter_variables(SingletonsFiltered, Names)
        ;	Singleton_variable_names(SingletonsFiltered, Names)
        ).


        Singleton_variable_names([], []).

        Singleton_variable_names([Name = _| Singletons], [Name| Names]) :-
        Singleton_variable_names(Singletons, Names).


        FilterDontCare_variables([], []).

        FilterDontCare_variables([Name = Variable| VariableNames], FilteredVariableNames) :-
        (	parameter_variable_name(Name) ->
        FilteredVariableNames = [Name = Variable| Rest],
        FilterDontCare_variables(VariableNames, Rest)
        ;	subAtom(Name, 0, 1, _, '_') ->
        FilterDontCare_variables(VariableNames, FilteredVariableNames)
        ;	FilteredVariableNames = [Name = Variable| Rest],
        FilterDontCare_variables(VariableNames, Rest)
        ).


        Filter_parameter_variables([], _, []).

        Filter_parameter_variables([Name = _| VariableNames], ParameterVariables, Names) :-
        (	member(Name-_, ParameterVariables) ->
        Filter_parameter_variables(VariableNames, ParameterVariables, Names)
        ;	Names = [Name| Rest],
        Filter_parameter_variables(VariableNames, ParameterVariables, Rest)
        ).


        Filter_parameter_variables([], []).

        Filter_parameter_variables([Name = _| VariableNames], Names) :-
        (	parameter_variable_name(Name) ->
        Filter_parameter_variables(VariableNames, Names)
        ;	Names = [Name| Rest],
        Filter_parameter_variables(VariableNames, Rest)
        ).



   //FirstStage_error_handler(@compound)
        %
   //error handler for the compiler first stage

        FirstStage_error_handler(Error) :-
        ppFile_pathsFlags_(_, _, MainSourceFile, ObjectFile, _),
        (	SourceFileContext(SourceFile, Lines) ->
        true
        ;	% no file context information available for last term read; likely
   //due to a syntax error when trying to read a main file term as syntax
   //errors in included files are handled when reading a file to terms
        SourceFile = MainSourceFile,
        (	stream_property(Input, alias(logtalkCompilerInput)),
        StreamCurrentLine_number(Input, Line) ->
        Lines = Line-Line
        ;	% some backend Prolog compilers do not support, or do not always support
   //(e.g. when a syntax error occurs) querying a stream line number
        Lines = '-(-1, -1)
        )
        ),
        Compiler_error_handler(SourceFile, ObjectFile, Lines, Error).



   //Compiler_error_handler(+atom, +atom, +pair(integer), @compound)
        %
   //closes the streams being used for reading and writing terms, restores
   //the operator table, reports the compilation error found, and, finally,
   //fails in order to abort the compilation process

        Compiler_error_handler(SourceFile, ObjectFile, Lines, Error) :-
        stream_property(Input, alias(logtalkCompilerInput)),
        stream_property(Output, alias(logtalkCompiler_output)), !,
        print_message(error, core, compiler_error(SourceFile, Lines, Error)),
        Restore_global_operator_table',
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        Reset_warningsCounter',
        catch(Close(Input), _, true),
        (	nonvar(Output) ->
        catch(Close(Output), _, true),
   //try to delete the intermediate Prolog files in order to prevent
   //problems by mistaken the broken files by good ones
        DeleteIntermediateFiles(ObjectFile)
        ;	true
        ),
        !,
        fail.



   //CompilerFirst_term_error_handler(+atom, +pair(integer), @compound)
        %
   //closes the stream being used for reading, restores the operator table,
   //reports the compilation error found, and, finally, fails in order to
   //abort the compilation process

        CompilerFirst_term_error_handler(SourceFile, Lines, Error) :-
        (	nonvar(Lines) ->
        true
        ;	% no line information available likely due to a syntax error
        stream_property(Input, alias(logtalkCompilerInput)),
        StreamCurrentLine_number(Input, Line) ->
        Lines = Line-Line
        ;	% some backend Prolog compilers do not support, or do not always support
   //(e.g. when a syntax error occurs) querying a stream line number
        Lines = '-(-1, -1)
        ),
        stream_property(Input, alias(logtalkCompilerInput)), !,
        print_message(error, core, compiler_error(SourceFile, Lines, Error)),
        Restore_global_operator_table',
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        Reset_warningsCounter',
        catch(Close(Input), _, true),
        !,
        fail.



   //Compiler_outputStream_error_handler(@stream, @compound)
        %
   //closes the stream being used for writing compiled terms, restores
   //the operator table, reports the compilation error found, and, finally,
   //fails in order to abort the compilation process

        Compiler_outputStream_error_handler(Stream, Error) :-
        print_message(error, core, compilerStream_error(Error)),
        Restore_global_operator_table',
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        Reset_warningsCounter',
        catch(Close(Stream), _, true),
        !,
        fail.



   //CompilerStream_error_handler(@compound)
        %
   //closes input and output streams if open, restores the operator table,
   //reports the compilation error found, and, finally, fails in order to
   //abort the compilation process

        CompilerStream_error_handler(Error) :-
        (	stream_property(Input, alias(logtalkCompilerInput)) ->
        catch(Close(Input), _, true)
        ;	true
        ),
        (	stream_property(Output, alias(logtalkCompiler_output)) ->
        catch(Close(Output), _, true)
        ;	true
        ),
        print_message(error, core, compilerStream_error(Error)),
        Restore_global_operator_table',
        Clean_ppFileClauses',
        Clean_pp_entityClauses',
        Reset_warningsCounter',
        !,
        fail.



   //ReadFile_term(+atom, @stream, -term, @list, -pair(integer))
        %
   //remember term position and variable names in order to support the
   //logtalkLoadContext/2 predicate and more informative compiler warning
   //and error messages

        ReadFile_term(File, Stream, Term, Options, Lines) :-
   //we retract first the position and variable names for the previous
   //read term as we may get a syntax error while reading the next term;
   //this will allow us to use the stream position if necessary to find
   //the approximated position of the error
        retractall(pp_term_variable_namesFileLines_(_, _, _, _)),
   //the actual read term predicate is defined in the adapter files
        Read_term(Stream, Term, Options, Lines, VariableNames),
        assertz(pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines)).



   //SecondStage(+atom, @entityIdentifier, +compilationContext)
        %
   //compiler second stage (initialization/1 goals and clause body goals)

        SecondStage(Type, Entity, Ctx) :-
        catch(
        Compile_entity(Type, Entity, Ctx),
        Error,
        SecondStage_error_handler(Error)
        ).


        Compile_entity(Type, Entity, Ctx) :-
        generate_entityCode(Type, Ctx),
        InlineCalls(Type),
        ReportLintIssues(Type, Entity),
        write_entityCode',
        Add_entitySourceData(Type, Entity),
        Save_entityRuntimeClause(Type),
        RestoreFile_operator_table',
        Clean_pp_entityClauses(Type).


        SecondStage_error_handler(Error) :-
        ppFile_pathsFlags_(_, _, _, ObjectFile, _),
        SourceFileContext(SourceFile, Lines),
        Compiler_error_handler(SourceFile, ObjectFile, Lines, Error).



   //Compile_entityFlags(+atom, -integer)
        %
   //defines the entity flags value when compiling or dynamically creating a new entity
        %
   //we use integers in decimal notation instead of binary notation to avoid standards
   //compliance issues with some Prolog compilers

        Compile_entityFlags(protocol, Flags) :-
        (	CompilerFlag(debug, on) ->
        Debug = 512
        ;	Debug = 0
        ),
        (	CompilerFlag(sourceData, on) ->
        SourceData = 8
        ;	SourceData = 0
        ),
        (	ppDynamic_' ->
        Dynamic = 2
        ;	Dynamic = 0
        ),
        (	pp_builtIn_' ->
        BuiltIn = 1
        ;	BuiltIn = 0
        ),
        Flags is Debug + SourceData + Dynamic + BuiltIn.

        Compile_entityFlags(category, Flags) :-
        (	CompilerFlag(debug, on) ->
        Debug = 512
        ;	Debug = 0
        ),
        (	CompilerFlag(events, allow) ->
        Events = 16
        ;	Events = 0
        ),
        (	CompilerFlag(sourceData, on) ->
        SourceData = 8
        ;	SourceData = 0
        ),
        (	ppDynamic_' ->
        Dynamic = 2
        ;	Dynamic = 0
        ),
        (	pp_builtIn_' ->
        BuiltIn = 1
        ;	BuiltIn = 0
        ),
        Flags is Debug + Events + SourceData + Dynamic + BuiltIn.

        Compile_entityFlags(object, Flags) :-
        (	pp_module_(_) ->
        Module = 1024
        ;	Module = 0
        ),
        (	CompilerFlag(debug, on) ->
        Debug = 512
        ;	Debug = 0
        ),
        (	CompilerFlag(contextSwitchingCalls, allow) ->
        ContextSwitchingCalls = 256
        ;	ContextSwitchingCalls = 0
        ),
        (	CompilerFlag(dynamicDeclarations, allow) ->
        DynamicDeclarations = 128
        ;	DynamicDeclarations = 0
        ),
        CompilerFlag(complements, ComplementsFlag),
        (	ComplementsFlag == deny ->
        Complements = 0
        ;	ComplementsFlag == allow ->
        Complements = 64
        ;	% ComplementsFlag == restrict,
        Complements = 32
        ),
        (	CompilerFlag(events, allow) ->
        Events = 16
        ;	Events = 0
        ),
        (	CompilerFlag(sourceData, on) ->
        SourceData = 8
        ;	SourceData = 0
        ),
        (	pp_threaded_' ->
        Threaded = 4
        ;	Threaded = 0
        ),
        (	ppDynamic_' ->
        Dynamic = 2
        ;	Dynamic = 0
        ),
        (	pp_builtIn_' ->
        BuiltIn = 1
        ;	BuiltIn = 0
        ),
        Flags is Module + Debug + ContextSwitchingCalls + DynamicDeclarations + Complements + Events + SourceData + Threaded + Dynamic + BuiltIn.



   //saves the entity runtime clause after computing the final value of its flags

        Save_entityRuntimeClause(object) :-
        pp_object_(Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, _),
        Compile_entityFlags(object, Flags),
        assertz(ppRuntimeClause_(Current_object_(Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, Flags))).

        Save_entityRuntimeClause(protocol) :-
        pp_protocol_(Ptc, Prefix, Dcl, Rnm, _),
        Compile_entityFlags(protocol, Flags),
        assertz(ppRuntimeClause_(Current_protocol_(Ptc, Prefix, Dcl, Rnm, Flags))).

        Save_entityRuntimeClause(category) :-
        ppCategory_(Ctg, Prefix, Dcl, Def, Rnm, _),
        Compile_entityFlags(category, Flags),
        assertz(ppRuntimeClause_(CurrentCategory_(Ctg, Prefix, Dcl, Def, Rnm, Flags))).



   //cleans up all dynamic predicates used during source file compilation

        Clean_ppFileClauses' :-
        retractall(ppFileInitialization_(_, _)),
        retractall(ppFile_objectInitialization_(_, _, _)),
        retractall(ppFile_encoding_(_, _, _)),
        retractall(ppFile_bom_(_)),
        retractall(ppFileCompilerFlag_(_, _)),
        retractall(pp_term_variable_namesFileLines_(_, _, _, _)),
   //a Logtalk source file may contain only plain Prolog terms
   //instead of plain Prolog terms intermixed between entities
   //definitions; there might also be plain Prolog terms after
   //the last entity definition
        retractall(pp_prolog_term_(_, _, _)),
   //retract all file-specific flag values
        retractall(ppFileCompilerFlag_(_, _)),
   //retract all file-specific term and goal expansion hooks
        retractall(pp_hook_term_expansion_(_, _)),
        retractall(pp_hook_goal_expansion_(_, _)),
        Clean_ppCcClauses',
        Clean_ppRuntimeClauses'.



   //cleans up all dynamic predicates used for conditional compilation

        Clean_ppCcClauses' :-
        retractall(ppCcIfFound_(_)),
        retractall(ppCcSkipping_'),
        retractall(ppCc_mode_(_)).



   //cleans up the dynamic predicate used for entity runtime clauses

        Clean_ppRuntimeClauses' :-
        retractall(ppRuntimeClause_(_)).



   //cleans up all dynamic predicates used during entity compilation

        Clean_pp_entityClauses' :-
        Clean_pp_objectClauses',
        Clean_pp_protocolClauses',
        Clean_ppCategoryClauses'.

        Clean_pp_entityClauses(object) :-
        Clean_pp_objectClauses'.

        Clean_pp_entityClauses(protocol) :-
        Clean_pp_protocolClauses'.

        Clean_pp_entityClauses(category) :-
        Clean_ppCategoryClauses'.

        Clean_pp_objectClauses' :-
        retractall(pp_object_(_, _, _, _, _, _, _, _, _, _, _)),
        retractall(pp_module_(_)),
        retractall(pp_objectInitialization_(_, _, _)),
        retractall(ppFinal_objectInitialization_(_, _)),
        retractall(ppImportedCategory_(_, _, _, _, _, _)),
        retractall(pp_extended_object_(_, _, _, _, _, _, _, _, _, _, _)),
        retractall(ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _)),
        retractall(ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _)),
        retractall(pp_threaded_'),
        Clean_ppCommon_objectCategoryClauses',
        Clean_ppCommon_entityClauses'.

        Clean_pp_protocolClauses' :-
        retractall(pp_protocol_(_, _, _, _, _)),
        retractall(pp_extended_protocol_(_, _, _, _, _)),
        Clean_ppCommon_entityClauses'.

        Clean_ppCategoryClauses' :-
        retractall(ppCategory_(_, _, _, _, _, _)),
        retractall(ppComplemented_object_(_, _, _, _, _)),
        retractall(pp_extendedCategory_(_, _, _, _, _, _)),
        Clean_ppCommon_objectCategoryClauses',
        Clean_ppCommon_entityClauses'.

        Clean_ppCommon_objectCategoryClauses' :-
        retractall(ppImplemented_protocol_(_, _, _, _, _)),
        retractall(pp_parameter_variables_(_)),
        retractall(pp_uses_predicate_(_, _, _, _)),
        retractall(pp_uses_non_terminal_(_, _, _, _, _, _)),
        retractall(pp_use_module_predicate_(_, _, _, _)),
        retractall(pp_use_module_non_terminal_(_, _, _, _, _, _)),
        retractall(ppDef_(_)),
        retractall(ppDdef_(_)),
        retractall(ppSuper_(_)),
        retractall(pp_number_ofClausesRules_(_, _, _, _)),
        retractall(pp_number_ofClausesRules_(_, _, _, _, _)),
        retractall(pp_predicateDefinitionLocation_(_, _, _, _)),
        retractall(pp_predicateDefinitionLocation_(_, _, _, _, _)),
        retractall(ppRedefined_builtIn_(_, _, _)),
        retractall(ppDefines_predicate_(_, _, _, _, _, _)),
        retractall(ppInline_predicate_(_)),
        retractall(ppCalls_predicate_(_, _, _, _, _)),
        retractall(ppCallsSelf_predicate_(_, _, _, _)),
        retractall(ppCallsSuper_predicate_(_, _, _, _)),
        retractall(pp_updates_predicate_(_, _, _, _)),
        retractall(pp_non_portable_predicate_(_, _, _)),
        retractall(pp_non_portableFunction_(_, _, _)),
        retractall(pp_missing_meta_predicateDirective_(_, _, _)),
        retractall(pp_missingDynamicDirective_(_, _, _)),
        retractall(pp_missingDiscontiguousDirective_(_, _, _)),
        retractall(pp_previous_predicate_(_, _)),
        retractall(ppDefines_non_terminal_(_, _)),
        retractall(ppCalls_non_terminal_(_, _, _)),
        retractall(ppReferenced_object_(_, _, _)),
        retractall(ppReferencedCategory_(_, _, _)),
        retractall(ppReferenced_module_(_, _, _)),
        retractall(ppReferenced_object_message_(_, _, _, _, _, _)),
        retractall(ppReferenced_module_predicate_(_, _, _, _, _, _)).

        Clean_ppCommon_entityClauses' :-
        retractall(pp_entityCompilerFlag_(_, _)),
        retractall(pp_entity_(_, _, _, _, _)),
        retractall(pp_entityInfo_(_)),
        retractall(pp_predicateInfo_(_, _)),
        retractall(ppDirective_(_)),
        retractall(ppSynchronized_(_, _)),
        retractall(pp_predicate_mutexCounter_(_)),
        retractall(pp_public_(_, _)),
        retractall(pp_protected_(_, _)),
        retractall(pp_private_(_, _)),
        retractall(ppDynamic_(_)),
        retractall(ppDiscontiguous_(_)),
        retractall(pp_multifile_(_, _, _)),
        retractall(ppCoinductive_(_, _, _, _, _, _, _)),
        retractall(pp_mode_(_, _, _, _)),
        retractall(pp_meta_predicate_(_, _)),
        retractall(pp_predicateAlias_(_, _, _, _, _, _)),
        retractall(pp_non_terminal_(_, _, _)),
        retractall(pp_entity_metaDirective_(_, _, _)),
        retractall(ppDcl_(_)),
   //clean any plain Prolog terms appearing before an entity definition
        retractall(pp_prolog_term_(_, _, _)),
        retractall(pp_entity_term_(_, _, _)),
        retractall(ppFinal_entity_term_(_, _)),
        retractall(pp_entityAuxClause_(_)),
        retractall(ppFinal_entityAuxClause_(_)),
        retractall(pp_predicateDeclarationLocation_(_, _, _, _)),
        retractall(ppReferenced_protocol_(_, _, _)),
        retractall(pp_builtIn_'),
        retractall(ppDynamic_'),
        retractall(ppAux_predicateCounter_(_)).



   //CleanLookupCaches'
        %
   //cleans all entries for all dynamic binding lookup caches
        %
   //this also have the side-effect of removing the catchall clauses
   //that generate the cache entries which we must then re-assert

        CleanLookupCaches' :-
        retractall(Send_to_obj_(_, _, _)),
        retractall(Send_to_obj_ne_(_, _, _)),
        retractall(Send_toSelf_(_, _, _)),
        retractall(objSuperCall_(_, _, _)),
        retractall(CtgSuperCall_(_, _, _)),
        retractall(DbLookupCache_(_, _, _, _, _)),
        ReassertLookupCacheCatchallClauses'.



   //CleanLookupCaches(@callable)
        %
   //cleans all entries for a given predicate for all dynamic
   //binding lookup caches
        %
   //this also have the side-effect of removing the catchall clauses
   //that generate the cache entries which we must then re-assert

        CleanLookupCaches(Pred) :-
        retractall(Send_to_obj_(_, Pred, _)),
        retractall(Send_to_obj_ne_(_, Pred, _)),
        retractall(Send_toSelf_(_, Pred, _)),
        retractall(objSuperCall_(_, Pred, _)),
        retractall(CtgSuperCall_(_, Pred, _)),
        retractall(DbLookupCache_(_, Pred, _, _, _)),
        ReassertLookupCacheCatchallClauses'.



   //ReassertLookupCacheCatchallClauses'
        %
   //reasserts the catchall clauses for the dynamic binding
   //lookup cache predicates that generate the cache entries

        ReassertLookupCacheCatchallClauses' :-
        assertz((Send_to_obj_(Obj, Pred, ExCtx) :- Send_to_obj_nv(Obj, Pred, ExCtx))),
        assertz((Send_to_obj_ne_(Obj, Pred, ExCtx) :- Send_to_obj_ne_nv(Obj, Pred, ExCtx))),
        assertz((Send_toSelf_(Obj, Pred, ExCtx) :- Send_toSelf_nv(Obj, Pred, ExCtx))),
        assertz((objSuperCall_(Super, Pred, ExCtx) :- objSuperCall_nv(Super, Pred, ExCtx))),
        assertz((CtgSuperCall_(Ctg, Pred, ExCtx) :- CtgSuperCall_nv(Ctg, Pred, ExCtx))),
   //support runtime resolved database messages to the "user" pseudo-object
        assertz(DbLookupCache_(user, Clause, _, Clause, true)).



   //Restore_global_operator_table'
        %
   //restores the global operator table
        %
   //called after compiling a source file or after dynamically creating a new entity

        Restore_global_operator_table' :-
        retract(pp_entity_operator_(_, Specifier, Operator, _)),
        op(0, Specifier, Operator),
        fail.

        Restore_global_operator_table' :-
        retract(ppFile_operator_(_, Specifier, Operator)),
        op(0, Specifier, Operator),
        fail.

        Restore_global_operator_table' :-
        retract(pp_global_operator_(Priority, Specifier, Operator)),
        op(Priority, Specifier, Operator),
        fail.

        Restore_global_operator_table'.



   //RestoreFile_operator_table'
        %
   //restores the file operator table
        %
   //called after compiling a source file entity

        RestoreFile_operator_table' :-
        retract(pp_entity_operator_(_, Specifier, Operator, _)),
        op(0, Specifier, Operator),
        fail.

        RestoreFile_operator_table' :-
        retract(ppFile_operator_(Priority, Specifier, Operator)),
        op(Priority, Specifier, Operator),
        fail.

        RestoreFile_operator_table'.



   //ActivateFile_operators(+integer, +operatorSpecifier, +atom_orAtomList)
        %
   //activates local file operator definitions
        %
   //any conflicting global operator is saved so that it can be restored later

        ActivateFile_operators(_, _, []) :-
        !.

        ActivateFile_operators(Priority, Specifier, [Operator| Operators]) :-
        !,
        ActivateFile_operator(Priority, Specifier, Operator),
        ActivateFile_operators(Priority, Specifier, Operators).

        ActivateFile_operators(Priority, Specifier, Operator) :-
        ActivateFile_operator(Priority, Specifier, Operator).


        ActivateFile_operator(Priority, Specifier, Operator) :-
        (	current_op(OriginalPriority, OriginalSpecifier, Operator),
        Same_operatorClass(Specifier, OriginalSpecifier) ->
        assertz(pp_global_operator_(OriginalPriority, OriginalSpecifier, Operator))
        ;	true
        ),
        op(Priority, Specifier, Operator),
        assertz(ppFile_operator_(Priority, Specifier, Operator)).



   //Activate_entity_operators(+integer, +operatorSpecifier, +atom_orAtomList, +scope)
        %
   //activates local entity operator definitions
        %
   //any conflicting file operator is saved so that it can be restored later

        Activate_entity_operators(_, _, [], _) :-
        !.

        Activate_entity_operators(Priority, Specifier, [Operator| Operators], Scope) :-
        !,
        Activate_entity_operator(Priority, Specifier, Operator, Scope),
        Activate_entity_operators(Priority, Specifier, Operators, Scope).

        Activate_entity_operators(Priority, Specifier, Operator, Scope) :-
        Activate_entity_operator(Priority, Specifier, Operator, Scope).


        Activate_entity_operator(Priority, Specifier, Operator, Scope) :-
        (	current_op(OriginalPriority, OriginalSpecifier, Operator),
        Same_operatorClass(Specifier, OriginalSpecifier) ->
        assertz(ppFile_operator_(OriginalPriority, OriginalSpecifier, Operator))
        ;	true
        ),
        op(Priority, Specifier, Operator),
        assertz(pp_entity_operator_(Priority, Specifier, Operator, Scope)),
        pp_entity_(_, Entity, _, _, _),
        assertz(ppRuntimeClause_(entity_property_(Entity, op(Priority, Specifier, Operator, Scope)))).



   //expandFileDirective_goal(+callable, -callable)
        %
   //expands a file directive goal
        %
   //used to expand file level initialization/1 goals and conditional
   //compilation directive goals (if/1 and elif/1) and deal with some
   //special cases

        expandFileDirective_goal(Goal, call(Goal)) :-
        var(Goal),
        !.

        expandFileDirective_goal({Goal}, Goal) :-
        !.

        expandFileDirective_goal(Goal, ExpandedGoal) :-
        expandFile_goal(Goal, ExpandedGoal0),
        Goal \== ExpandedGoal0,
        !,
        expandFileDirective_goal(ExpandedGoal0, ExpandedGoal).

        expandFileDirective_goal((Goal1, Goal2), (ExpandedGoal1, ExpandedGoal2)) :-
        !,
        expandFileDirective_goal(Goal1, ExpandedGoal1),
        expandFileDirective_goal(Goal2, ExpandedGoal2).

        expandFileDirective_goal((IfThen; Else), (TIf -> TThen; TElse)) :-
        nonvar(IfThen),
        IfThen = (If -> Then),
        !,
        expandFileDirective_goal(If, TIf),
        expandFileDirective_goal(Then, TThen),
        expandFileDirective_goal(Else, TElse).

        expandFileDirective_goal((IfThen; Else), ('*->(TIf, TThen); TElse)) :-
        nonvar(IfThen),
        IfThen = '*->(If, Then),
        predicate_property('*->(_, _), builtIn),
        !,
        expandFileDirective_goal(If, TIf),
        expandFileDirective_goal(Then, TThen),
        expandFileDirective_goal(Else, TElse).

        expandFileDirective_goal((Goal1; Goal2), (ExpandedGoal1; ExpandedGoal2)) :-
        !,
        expandFileDirective_goal(Goal1, ExpandedGoal0),
        FixDisjunctionLeftSide(ExpandedGoal0, ExpandedGoal1),
        expandFileDirective_goal(Goal2, ExpandedGoal2).

        expandFileDirective_goal('*->(Goal1, Goal2), '*->(ExpandedGoal1, ExpandedGoal2)) :-
        predicate_property('*->(_, _), builtIn),
        !,
        expandFileDirective_goal(Goal1, ExpandedGoal1),
        expandFileDirective_goal(Goal2, ExpandedGoal2).

        expandFileDirective_goal((Goal1 -> Goal2), (ExpandedGoal1 -> ExpandedGoal2)) :-
        !,
        expandFileDirective_goal(Goal1, ExpandedGoal1),
        expandFileDirective_goal(Goal2, ExpandedGoal2).

        expandFileDirective_goal(\+ Goal, \+ ExpandedGoal) :-
        !,
        expandFileDirective_goal(Goal, ExpandedGoal).

        expandFileDirective_goal(catch(Goal, Catcher, Recovery), catch(ExpandedGoal, Catcher, ExpandedRecovery)) :-
        !,
        expandFileDirective_goal(Goal, ExpandedGoal),
        expandFileDirective_goal(Recovery, ExpandedRecovery).

   //workaround lack of compliance by some backend Prolog compilers

        expandFileDirective_goal(predicate_property(Pred, Prop), predicate_property(Pred, Prop)) :-
        !.

   //expand calls to setLogtalkFlag/2 when possible to avoid the need of runtime entityKind-checking

        expandFileDirective_goal(setLogtalkFlag(Flag, Value), SetCompilerFlag(Flag, Value)) :-
        nonvar(Flag),
        nonvar(Value),
        !,
        Check(read_writeFlag, Flag),
        Check(flag_value, Flag + Value).

   //expand calls to the logtalkCompile/1-2 and logtalkLoad/1-2 predicates to
   //add a directory argument for default resolving of relative file paths

        expandFileDirective_goal(logtalkCompile(Files), LogtalkCompile(Files, Directory, ExCtx)) :-
        !,
        ppFile_pathsFlags_(_, Directory, _, _, _),
        executionContext(ExCtx, user, user, user, user, [], []).

        expandFileDirective_goal(logtalkCompile(Files, Flags), LogtalkCompile(Files, Flags, Directory, ExCtx)) :-
        !,
        ppFile_pathsFlags_(_, Directory, _, _, _),
        executionContext(ExCtx, user, user, user, user, [], []).

        expandFileDirective_goal(logtalkLoad(Files), LogtalkLoad(Files, Directory, ExCtx)) :-
        !,
        ppFile_pathsFlags_(_, Directory, _, _, _),
        executionContext(ExCtx, user, user, user, user, [], []).

        expandFileDirective_goal(logtalkLoad(Files, Flags), LogtalkLoad(Files, Flags, Directory, ExCtx)) :-
        !,
        ppFile_pathsFlags_(_, Directory, _, _, _),
        executionContext(ExCtx, user, user, user, user, [], []).

   //expand if possible calls to the logtalkLoadContext/2 predicate to support
   //embedded applications where the compiled code may no longer be loaded using
   //the Logtalk runtime

        expandFileDirective_goal(logtalkLoadContext(Key, Value), true) :-
        nonvar(Key),
        logtalkLoadContext(Key, Value),
        !.

   //catchall clause

        expandFileDirective_goal(Goal, Goal).



   //expandFile_goal(+callable, -callable)
        %
   //expands a goal; fails if no goal expansion hook is defined
        %
   //the callers of this predicate must ensure that a goal
   //is repeatedly expanded until a fixed-point is reached
        %
   //the callers must also take care of the case where the
   //goal is wrapped with the {}/1 control construct

        expandFile_goal(Goal, ExpandedGoal) :-
        (	% source-file specific compiler hook
        pp_hook_goal_expansion_(Goal, ExpandedGoal) ->
        true
        ;	% default compiler hook
        hook_goal_expansion_(Goal, ExpandedGoal) ->
        true
        ;	% dialect specific expansion
        prolog_goal_expansion(Goal, ExpandedGoal) ->
        prolog_goal_expansion_portability_warnings(Goal, ExpandedGoal)
        ;	% no compiler hook defined
        fail
        ),
   //the following check means that an expanded goal is checked twice but that
   //allows us to distinguish between user errors and goal-expansion errors
        Check(callable, ExpandedGoal, goal_expansion(Goal, ExpandedGoal)).


        prolog_goal_expansion_portability_warnings(Goal, ExpandedGoal) :-
        (	CompilerFlag(portability, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(portability), core, prologDialect_goal_expansion(File, Lines, Type, Entity, Goal, ExpandedGoal))
        ;	print_message(warning(portability), core, prologDialect_goal_expansion(File, Lines, Goal, ExpandedGoal))
        )
        ;	true
        ).



   //CompileIncludeFile_terms(@list(term), +atom, +compilationContext)
        %
   //compiles a list of file terms (clauses, directives, or grammar rules)
   //found in an included file

        CompileIncludeFile_terms([Term-sd(Lines,VariableNames)| Terms], File, Ctx) :-
        retractall(pp_term_variable_namesFileLines_(_, _, _, _)),
        assertz(pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines)),
        Check(nonvar, Term, term(Term)),
   //only the compilation context mode and position should be shared between different terms
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, Mode, _, _),
        CompCtx(NewCtx, _, _, _, _, _, _, _, _, _, _, Mode, _, Lines),
        CompileFile_term(Term, NewCtx),
        CompileIncludeFile_terms(Terms, File, Ctx).

        CompileIncludeFile_terms([], _, _).



   //CompileFile_term(@nonvar, +compilationContext)
        %
   //compiles a source file term (clause, directive, or grammar rule)
        %
   //we allow non-callable terms to be term-expanded; only if that fails
   //we throw an error

        CompileFile_term(Term, Ctx) :-
        unify_parameter_variables(Term, Ctx),
        (	Term = {_} ->
   //bypass control construct; skip term-expansion
        Compile_expanded_term(Term, Term, Ctx)
        ;	pp_hook_term_expansion_(Term, ExpandedTerms) ->
   //source-file specific compiler hook
        Compile_expanded_terms(ExpandedTerms, Term, Ctx)
        ;	hook_term_expansion_(Term, ExpandedTerms) ->
   //default compiler hook
        Compile_expanded_terms(ExpandedTerms, Term, Ctx)
        ;	prolog_term_expansion(Term, ExpandedTerms) ->
   //dialect specific expansion
        prolog_term_expansion_portability_warnings(Term, ExpandedTerms),
        Compile_expanded_terms(ExpandedTerms, Term, Ctx)
        ;	% no compiler hook defined
        callable(Term) ->
        Compile_expanded_term(Term, Term, Ctx)
        ;	throw(error(type_error(callable, Term), term(Term)))
        ).


        prolog_term_expansion_portability_warnings(Term, ExpandedTerms) :-
        (	CompilerFlag(portability, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(portability), core, prologDialect_term_expansion(File, Lines, Type, Entity, Term, ExpandedTerms))
        ;	print_message(warning(portability), core, prologDialect_term_expansion(File, Lines, Term, ExpandedTerms))
        )
        ;	true
        ).



   //Compile_expanded_terms(@list(term), @term, +compilationContext)
   //Compile_expanded_terms(@term, @term, +compilationContext)
        %
   //compiles the expanded terms (which can be a list of terms);
   //the second argument is the original term and is used for more
   //informative exception terms in case of error
        %
   //note that the clause order ensures that instantiation errors will be
   //caught by the call to the Compile_expanded_term'/3 predicate

        Compile_expanded_terms([ExpandedTerm| ExpandedTerms], Term, Ctx) :-
        !,
        Compile_expanded_term(ExpandedTerm, Term, Ctx),
   //ensure that only the compilation context mode and the entity prefix are
   //shared between different clauses but keep the current clause position
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, _, Mode, _, Lines),
        CompCtx(NewCtx, _, _, _, _, _, _, Prefix, _, _, _, Mode, _, Lines),
        Compile_expanded_terms(ExpandedTerms, Term, NewCtx).

        Compile_expanded_terms([], _, _) :-
        !.

        Compile_expanded_terms(ExpandedTerm, Term, Ctx) :-
        Compile_expanded_term(ExpandedTerm, Term, Ctx).



   //Compile_expanded_term(@term, @term, +compilationContext)
        %
   //compiles a source file term (a clause, directive, or grammar rule);
   //the second argument is the original term and is used for more
   //informative exception terms in case of error

        Compile_expanded_term((-), Term, _) :-
   //catch variables
        throw(error(instantiantion_error, term_expansion(Term, _))).

        Compile_expanded_term(begin_ofFile, _, _) :-
        !.

        Compile_expanded_term(end_ofFile, _, Ctx) :-
        pp_module_(Module),
        !,
   //module definitions start with an opening module/1-2 directive and are assumed
   //to end at the end of a source file; there is no module closing directive;
   //set the initial compilation context and the position for compiling the end_ofFile term
        SecondStage(object, Module, Ctx),
        print_message(silent(compiling), core, compiled_entity(module, Module)).

        Compile_expanded_term(end_ofFile, _, _) :-
        pp_entity_(Type, _, _, _, _),
   //unexpected end-of-file while compiling an entity
        (	Type == object ->
        throw(error(existence_error(directive, end_object/0), term(end_ofFile)))
        ;	Type == protocol ->
        throw(error(existence_error(directive, end_protocol/0), term(end_ofFile)))
        ;	% Type == category,
        throw(error(existence_error(directive, endCategory/0), term(end_ofFile)))
        ).

        Compile_expanded_term(end_ofFile, _, _) :-
        ppCcIfFound_(_),
   //unexpected end-of-file while compiling a conditional compilation block
        throw(error(existence_error(directive, endif/0), term(end_ofFile))).

        Compile_expanded_term(end_ofFile, _, _) :-
        !.

        Compile_expanded_term({ExpandedTerm}, Term, _) :-
   //bypass control construct; expanded term is final
        !,
        (	callable(ExpandedTerm) ->
        (	pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines) ->
        SourceData = sd(Term, VariableNames, File, Lines)
        ;	SourceData = nil, Lines = '-(-1, -1)
        ),
        (	pp_entity_(_, _, _, _, _) ->
   //ensure that the relative order of the entity terms is kept
        assertz(pp_entity_term_({ExpandedTerm}, SourceData, Lines))
        ;	% non-entity terms
        assertz(pp_prolog_term_(ExpandedTerm, SourceData, Lines))
        )
        ;	var(ExpandedTerm) ->
        throw(error(instantiantion_error, term_expansion(Term, {ExpandedTerm})))
        ;	throw(error(type_error(callable, Term), term_expansion(Term, {ExpandedTerm})))
        ).

        Compile_expanded_term((Head :- Body), _, Ctx) :-
        !,
        CompileClause((Head :- Body), Ctx).

        Compile_expanded_term((:- Directive), _, Ctx) :-
        !,
        CompileDirective(Directive, Ctx).

        Compile_expanded_term((Head --> Body), _, Ctx) :-
        !,
        Compile_grammarRule((Head --> Body), Ctx).

        Compile_expanded_term(ExpandedTerm, Term, Ctx) :-
        (	callable(ExpandedTerm) ->
   //fact
        CompileClause(ExpandedTerm, Ctx)
        ;	throw(error(type_error(callable, ExpandedTerm), term_expansion(Term, ExpandedTerm)))
        ).



   //CompileRuntimeIncludeFile_terms(@list(term), +atom)
        %
   //compiles a list of runtime terms (clauses, directives, or grammar rules)
   //found in an included file
        %
   //note that the clause order ensures that instantiation errors will be caught
   //by the call to the CompileRuntime_term'/2 predicate

        CompileRuntimeIncludeFile_terms([Term-_| Terms], File) :-
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, runtime, _, '-(0,0)),
        CompileRuntime_term(Term, Ctx),
        CompileRuntimeIncludeFile_terms(Terms, File).

        CompileRuntimeIncludeFile_terms([], _).



   //CompileRuntime_terms(@list(term))
        %
   //compiles a list of runtime terms (clauses, directives, or grammar rules)
        %
   //note that the clause order ensures that instantiation errors will be caught
   //by the call to the CompileRuntime_term'/2 predicate

        CompileRuntime_terms([Term| Terms]) :-
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, runtime, _, '-(0,0)),
        CompileRuntime_term(Term, Ctx),
        CompileRuntime_terms(Terms).

        CompileRuntime_terms([]).



   //CompileRuntime_term(@term, +compilationContext)
        %
   //compiles a runtime term (a clause, directive, or grammar rule)

        CompileRuntime_term((-), _) :-
   //catch variables
        throw(error(instantiantion_error, term(_))).

        CompileRuntime_term(begin_ofFile, _) :-
        !.

        CompileRuntime_term(end_ofFile, _) :-
        !.

        CompileRuntime_term({Term}, _) :-
   //bypass control construct; term is final
        !,
        (	callable(Term) ->
        assertz(pp_entity_term_({Term}, nil, '-(0,0)))
        ;	var(Term) ->
        throw(error(instantiantion_error, term({Term})))
        ;	throw(error(type_error(callable, Term), term({Term})))
        ).

        CompileRuntime_term((Head :- Body), Ctx) :-
        !,
        CompileClause((Head :- Body), Ctx).

        CompileRuntime_term((:- Directive), Ctx) :-
        !,
        CompileDirective(Directive, Ctx).

        CompileRuntime_term((Head --> Body), Ctx) :-
        !,
        Compile_grammarRule((Head --> Body), Ctx).

        CompileRuntime_term(Term, _) :-
        \+ callable(Term),
        throw(error(type_error(callable, Term), term(Term))).

        CompileRuntime_term(Term, Ctx) :-
   //fact
        CompileClause(Term, Ctx).



   //CompileDirective(@term, +compilationContext)
        %
   //compiles a directive

        CompileDirective((-), _) :-
   //catch variables
        throw(error(instantiantion_error, directive(_))).

   //conditional compilation directives

        CompileDirective(if(Goal), Ctx) :-
        (	Goal = {UserGoal} ->
   //final goal
        Check(callable, UserGoal, directive(if(Goal))),
        fail
        ;	Check(callable, Goal, directive(if(Goal))),
   //only expand goals when compiling a source file
        CompCtx_mode(Ctx, compile(_)),
        expandFileDirective_goal(Goal, ExpandedGoal),
        Goal \== ExpandedGoal,
        !,
        CompileDirective(if({ExpandedGoal}), Ctx)
        ).

        CompileDirective(if(Goal), _) :-
        ppCc_mode_(Value),
   //not top-level if/1 directive
        !,
        asserta(ppCcIfFound_(Goal)),
        (	Value == ignore ->
   //another if ... endif to ignore
        asserta(ppCc_mode_(ignore))
        ;	Value == seek_else ->
   //we're looking for an else; ignore this if ... endif
        asserta(ppCc_mode_(ignore))
        ;	Value == skipAll ->
        asserta(ppCc_mode_(ignore))
        ;	% Value == skip_else,
        (	(	Goal = {UserGoal} ->
        catch(UserGoal, Error, FirstStage_error_handler(Error))
        ;	catch(Goal, Error, FirstStage_error_handler(Error))
        ) ->
        asserta(ppCc_mode_(skip_else))
        ;	asserta(ppCc_mode_(seek_else)),
        retractall(ppCcSkipping_'),
        assertz(ppCcSkipping_')
        )
        ).

        CompileDirective(if(Goal), _) :-
   //top-level if
        !,
        asserta(ppCcIfFound_(Goal)),
        (	(	Goal = {UserGoal} ->
        catch(UserGoal, Error, FirstStage_error_handler(Error))
        ;	catch(Goal, Error, FirstStage_error_handler(Error))
        ) ->
        asserta(ppCc_mode_(skip_else))
        ;	asserta(ppCc_mode_(seek_else)),
        retractall(ppCcSkipping_'),
        assertz(ppCcSkipping_')
        ).

        CompileDirective(elif(Goal), _) :-
        \+ ppCcIfFound_(_),
        throw(error(existence_error(directive, if/1), directive(elif(Goal)))).

        CompileDirective(elif(Goal), Ctx) :-
        (	Goal = {UserGoal} ->
   //final goal
        Check(callable, UserGoal, directive(elif(Goal))),
        fail
        ;	Check(callable, Goal, directive(elif(Goal))),
   //only expand goals when compiling a source file
        CompCtx_mode(Ctx, compile(_)),
        expandFileDirective_goal(Goal, ExpandedGoal),
        Goal \== ExpandedGoal,
        !,
        CompileDirective(elif({ExpandedGoal}), Ctx)
        ).

        CompileDirective(elif(Goal), _) :-
        ppCc_mode_(Mode),
        (	Mode == ignore ->
   //we're inside an if ... endif that we're ignoring
        true
        ;	Mode == skip_else ->
   //the corresponding if is true so we must skip this elif
        retractall(ppCcSkipping_'),
        assertz(ppCcSkipping_'),
        retract(ppCc_mode_(_)),
        asserta(ppCc_mode_(skipAll))
        ;	Mode == skipAll ->
        true
        ;	% Mode == seek_else,
   //the corresponding if is false
        retract(ppCc_mode_(_)),
        (	(	Goal = {UserGoal} ->
        catch(UserGoal, Error, FirstStage_error_handler(Error))
        ;	catch(Goal, Error, FirstStage_error_handler(Error))
        ) ->
        retractall(ppCcSkipping_'),
        asserta(ppCc_mode_(skip_else))
        ;	asserta(ppCc_mode_(seek_else))
        )
        ),
        !.

        CompileDirective(else, _) :-
        \+ ppCcIfFound_(_),
        throw(error(existence_error(directive, if/1), directive(else))).

        CompileDirective(else, _) :-
        ppCc_mode_(Mode),
        (	Mode == ignore ->
   //we're inside an if ... endif that we're ignoring
        true
        ;	Mode == skip_else ->
   //the corresponding if is true so we must skip this else
   //and any enclose if ... endif
        retractall(ppCcSkipping_'),
        assertz(ppCcSkipping_'),
        retract(ppCc_mode_(_)),
        asserta(ppCc_mode_(skipAll))
        ;	Mode == skipAll ->
        true
        ;	% Mode == seek_else ->
   //the corresponding if is false
        retract(ppCc_mode_(_)),
        asserta(ppCc_mode_(compile)),
        retractall(ppCcSkipping_')
        ),
        !.

        CompileDirective(endif, _) :-
        \+ ppCcIfFound_(_),
        throw(error(existence_error(directive, if/1), directive(endif))).

        CompileDirective(endif, _) :-
        retract(ppCcIfFound_(_)),
        retract(ppCc_mode_(Mode)),
        (	Mode \== ignore ->
        retractall(ppCcSkipping_')
        ;	\+ ppCcIfFound_(_) ->
        retractall(ppCcSkipping_'),
        retractall(ppCc_mode_(_))
        ;	true
        ),
        !.

   //remaining directives

        CompileDirective(Directive, Ctx) :-
        \+ pp_entity_(_, _, _, _, _),
   //not compiling an entity
        \+ Logtalk_openingDirective(Directive),
   //directive occurs before opening entity directive
        !,
        (	LogtalkClosingDirective(Directive) ->
   //closing entity directive occurs before the opening entity directive;
   //the opening directive is probably missing or misspelt
        (	Directive == end_object ->
        throw(error(existence_error(directive, object/1), directive(Directive)))
        ;	Directive == end_protocol ->
        throw(error(existence_error(directive, protocol/1), directive(Directive)))
        ;	% Directive == endCategory ->
        throw(error(existence_error(directive, category/1), directive(Directive)))
        )
        ;	% compile it as a source file-level directive
        catch(
        CompileFileDirective(Directive, Ctx),
        Error,
        throw(error(Error, directive(Directive)))
        )
        ).

        CompileDirective(Directive, Ctx) :-
        LogtalkDirective(Directive),
        !,
        catch(
        CompileLogtalkDirective(Directive, Ctx),
        Error,
        throw(error(Error, directive(Directive)))
        ).

        CompileDirective(Directive, Ctx) :-
        prolog_metaDirective(Directive, Meta),
   //as defined in the Prolog adapter files
        !,
        (	CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(portability, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(portability), core, compiling_proprietary_prologDirective(File, Lines, Type, Entity, Directive))
        ;	true
        ),
   //save the source data information for use in the second compiler stage
   //(where it might be required by calls to the logtalkLoadContext/2
   //predicate during goal expansion)
        (	pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines) ->
        SourceData = sd(Term, VariableNames, File, Lines)
        ;	SourceData = nil, Lines = '-(-1, -1)
        ),
        assertz(pp_entity_metaDirective_(directive(Directive, Meta), SourceData, Lines)).

        CompileDirective(Directive, Ctx) :-
        pp_module_(_),
   //we're compiling a module as an object
        (	ppDefines_predicate_(Directive, _, _, _, _, _)
        ;	pp_uses_predicate_(_, _, Directive, _)
   //directive is a query for a locally defined predicate
        ;	pp_use_module_predicate_(_, _, Directive, _)
   //or a predicate referenced in a use_module/2 directive
        ;	builtIn_predicate(Directive)
   //or a built-in predicate
        ),
   //but not unsupported directives that the backend Prolog compiler adapter
   //file failed to expand into supported use_module/2 directives
        Directive \= use_module(_),
        Directive \= ensureLoaded(_),
        !,
   //compile query as an initialization goal
        (	CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(portability, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(portability), core, compiling_queryAsInitialization_goal(File, Lines, Type, Entity, Directive))
        ;	true
        ),
        CompileLogtalkDirective(initialization(Directive), Ctx).

        CompileDirective(Directive, _) :-
        functor(Directive, Functor, Arity),
        throw(error(domain_error(directive, Functor/Arity), directive(Directive))).



   //CompileFileDirective(@nonvar, +compilationContext)
        %
   //compiles file-level directives, i.e. directives that are not encapsulated in a Logtalk
   //entity; error-checking is delegated in most cases to the backend Prolog compiler

        CompileFileDirective(encoding(Encoding), Ctx) :-
        !,
        (	ppFile_encoding_(Encoding, _, Line),
        CompCtxLines(Ctx, Line-_) ->
   //encoding/1 directive already processed
        true
        ;	% out-of-place encoding/1 directive, which must be the first term in a source file
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        print_message(warning(general), core, ignoredDirective(File, Lines, encoding/1))
        ).

        CompileFileDirective(ensureLoaded(FileSpec), _) :-
        !,
   //perform basic error checking
        Check(ground, FileSpec),
   //try to expand the file spec as the directive may be found in an included file
        expand_moduleFileSpecification(FileSpec, ExpandedFile),
   //assume that ensureLoaded/1 is also a built-in predicate
        ensureLoaded(ExpandedFile),
        pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines),
        assertz(pp_prolog_term_((:- ensureLoaded(ExpandedFile)), sd(Term,VariableNames,File,Lines), Lines)).

        CompileFileDirective(use_module(FileSpec), _) :-
        !,
   //perform basic error checking
        Check(ground, FileSpec),
   //try to expand the file spec as the directive may be found in an included file
        expand_moduleFileSpecification(FileSpec, ExpandedFile),
   //assume that use_module/1 is also a built-in predicate
        use_module(ExpandedFile),
        pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines),
        assertz(pp_prolog_term_((:- use_module(ExpandedFile)), sd(Term,VariableNames,File,Lines), Lines)).

        CompileFileDirective(use_module(FileSpec, Imports), _) :-
        !,
   //perform basic error checking
        Check(ground, FileSpec),
        Check(ground, Imports),
   //try to expand the file spec as the directive may be found in an included file
        expand_moduleFileSpecification(FileSpec, ExpandedFile),
   //assume that use_module/2 is also a built-in predicate
        use_module(ExpandedFile, Imports),
        pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines),
        assertz(pp_prolog_term_((:- use_module(ExpandedFile, Imports)), sd(Term,VariableNames,File,Lines), Lines)).

   //handling of this Prolog directive is necessary to
   //support the Logtalk term-expansion mechanism
        CompileFileDirective(include(File), Ctx) :-
        !,
   //read the file terms for compilation
        CompCtx_mode(Ctx, Mode),
        ReadFile_to_terms(Mode, File, Directory, Path, Terms),
   //save the dependency in the main file to support make
        ppFile_pathsFlags_(MainBasename, MainDirectory, _, _, _),
        File_modification_time(Path, TimeStamp),
        assertz(ppRuntimeClause_(IncludedFile_(Path, MainBasename, MainDirectory, TimeStamp))),
   //save loading stack to deal with failed compilation
        retractall(FileLoadingStack_(Path, Directory)),
        asserta(FileLoadingStack_(Path, Directory)),
   //compile the included file terms
        catch(
        CompileIncludeFile_terms(Terms, Path, Ctx),
        Error,
        (retract(FileLoadingStack_(Path, Directory)), throw(Error))
        ),
        retract(FileLoadingStack_(Path, Directory)).

        CompileFileDirective(initialization(Goal), Ctx) :-
        !,
   //perform basic error checking
        Check(callable, Goal),
   //initialization directives are collected and moved to the end of file
   //to minimize compatibility issues with backend Prolog compilers
        SourceFileContext(File, Lines),
        (	Goal = {UserGoal} ->
   //final goal
        Check(callable, UserGoal),
        assertz(ppFileInitialization_(Goal, Lines))
        ;	CompCtx_mode(Ctx, compile(_)),
   //goals are only expanded when compiling a source file
        expandFileDirective_goal(Goal, ExpandedGoal),
        Goal \== ExpandedGoal ->
        assertz(ppFileInitialization_(ExpandedGoal, Lines))
        ;	assertz(ppFileInitialization_(Goal, Lines))
        ).

        CompileFileDirective(op(Priority, Specifier, Operators), _) :-
        !,
        Check(operatorSpecification, op(Priority, Specifier, Operators)),
        ActivateFile_operators(Priority, Specifier, Operators),
        pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines),
        assertz(pp_prolog_term_((:- op(Priority, Specifier, Operators)), sd(Term,VariableNames,File,Lines), Lines)).

        CompileFileDirective(setLogtalkFlag(Name, Value), _) :-
        !,
        Check(read_writeFlag, Name),
        Check(flag_value, Name+Value),
   //local scope (restricted to the source file being compiled)
        Flag =.. [Name, Value],
        SetCompilerFlags([Flag]).

        CompileFileDirective(set_prologFlag(Flag, Value), Ctx) :-
        !,
   //perform basic error and portability checking
        Compile_body(set_prologFlag(Flag, Value), _, _, Ctx),
   //require a bound value
        Check(nonvar, Value),
   //setting the flag during compilation may or may not work as expected
   //depending on the flag and on the backend Prolog compiler
        set_prologFlag(Flag, Value),
   //we also copy the directive to the generated intermediate Prolog file
        pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines),
        assertz(pp_prolog_term_((:- set_prologFlag(Flag, Value)), sd(Term,VariableNames,File,Lines), Lines)).

        CompileFileDirective(multifile(Preds), _) :-
   //perform basic error checking
        Flatten_toList(Preds, PredsFlatted),
        CheckFile_predicateDirectiveArguments(PredsFlatted, (multifile)),
        fail.

        CompileFileDirective(dynamic(Preds), _) :-
   //perform basic error checking
        Flatten_toList(Preds, PredsFlatted),
        CheckFile_predicateDirectiveArguments(PredsFlatted, (dynamic)),
        fail.

        CompileFileDirective(discontiguous(Preds), _) :-
   //perform basic error checking
        Flatten_toList(Preds, PredsFlatted),
        CheckFile_predicateDirectiveArguments(PredsFlatted, (discontiguous)),
        fail.

        CompileFileDirective(Directive, Ctx) :-
   //directive will be copied to the generated Prolog file
        pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines),
        assertz(pp_prolog_term_((:- Directive), sd(Term,VariableNames,File,Lines), Lines)),
   //report a possible portability issue if warranted
        (	CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(portability, warning),
        \+ FileDirective(Directive) ->
        IncrementCompiling_warningsCounter',
        print_message(warning(portability), core, nonStandardFileDirective(File, Lines, Directive))
        ;	true
        ).


   //auxiliar predicate for performing basic error checking if file level
   //predicate directive arguments

        CheckFile_predicateDirectiveArguments([Pred| Preds], Property) :-
        !,
        CheckFile_predicateDirectiveArgument(Pred, Property),
        CheckFile_predicateDirectiveArguments(Preds, Property).

        CheckFile_predicateDirectiveArguments([], _).


        CheckFile_predicateDirectiveArgument(Obj::Pred, Property) :-
   //Logtalk entity predicates must be defined within an entity but be
   //sure there aren't instantiation or entityKind errors in the directive
        !,
        Check(objectIdentifier, Obj),
        Check(predicate_or_non_terminalIndicator, Pred),
        throw(permission_error(declare, Property, Obj::Pred)).

        CheckFile_predicateDirectiveArgument(':(Module,Pred), _) :-
        !,
        Check(moduleIdentifier, Module),
        Check(predicate_or_non_terminalIndicator, Pred).

        CheckFile_predicateDirectiveArgument(Pred, _) :-
        Check(predicate_or_non_terminalIndicator, Pred).


        expand_moduleFileSpecification(FileSpec, ExpandedFile) :-
        (	atom(FileSpec),
   //try to expand to an existing Prolog file
        SourceFile_name(FileSpec, [], _, _, Extension, ExpandedFile),
        File_extension(prolog, Extension),
        File_exists(ExpandedFile) ->
        true
        ;	% otherwise try the file spec as-is
        ExpandedFile = FileSpec
        ).



   //CompileLogtalkDirectives(+list(term), +compilationContext)
        %
   //compiles a list of Logtalk directives
        %
   //note that the clause order ensures that instantiation errors will be caught
   //by the call to the CompileLogtalkDirective'/2 predicate

        CompileLogtalkDirectives([Directive| Directives], Ctx) :-
   //only the compilation context mode should be shared between different directives
        CompCtx_mode(Ctx, Mode),
        CompCtx_mode(NewCtx, Mode),
        (	LogtalkDirective(Directive) ->
        CompileLogtalkDirective(Directive, NewCtx),
        CompileLogtalkDirectives(Directives, Ctx)
        ;	functor(Directive, Functor, Arity),
        throw(domain_error(directive, Functor/Arity))
        ).

        CompileLogtalkDirectives([], _).



   //CompileLogtalkDirective(@term, +compilationContext)
        %
   //compiles a Logtalk directive and its (possibly empty) list of arguments

        CompileLogtalkDirective((-), _) :-
   //catch variables
        throw(instantiation_error).

        CompileLogtalkDirective(include(File), Ctx) :-
   //read the file terms for compilation
        CompCtx_mode(Ctx, Mode),
        ReadFile_to_terms(Mode, File, Directory, Path, Terms),
   //save the dependency in the main file to support make if compiling a source file
        (	Mode == runtime ->
        true
        ;	ppFile_pathsFlags_(MainBasename, MainDirectory, _, _, _),
        File_modification_time(Path, TimeStamp),
        assertz(ppRuntimeClause_(IncludedFile_(Path, MainBasename, MainDirectory, TimeStamp)))
        ),
   //save loading stack to deal with failed compilation
        retractall(FileLoadingStack_(Path, Directory)),
        asserta(FileLoadingStack_(Path, Directory)),
   //compile the included file terms
        catch(
        (	Mode == runtime ->
        CompileRuntimeIncludeFile_terms(Terms, Path)
        ;	CompileIncludeFile_terms(Terms, Path, Ctx)
        ),
        Error,
        (retract(FileLoadingStack_(Path, Directory)), throw(Error))
        ),
        retract(FileLoadingStack_(Path, Directory)).

   //object opening and closing directives

        CompileLogtalkDirective(object(Obj), Ctx) :-
        CompileLogtalkDirective(object_(Obj, []), Ctx).

        CompileLogtalkDirective(object(Obj, Relation), Ctx) :-
        CompileLogtalkDirective(object_(Obj, [Relation]), Ctx).

        CompileLogtalkDirective(object(Obj, Relation1, Relation2), Ctx) :-
        CompileLogtalkDirective(object_(Obj, [Relation1, Relation2]), Ctx).

        CompileLogtalkDirective(object(Obj, Relation1, Relation2, Relation3), Ctx) :-
        CompileLogtalkDirective(object_(Obj, [Relation1, Relation2, Relation3]), Ctx).

        CompileLogtalkDirective(object(Obj, Relation1, Relation2, Relation3, Relation4), Ctx) :-
        CompileLogtalkDirective(object_(Obj, [Relation1, Relation2, Relation3, Relation4]), Ctx).

   //auxiliary predicate to compile all variants to the object opening directive
        CompileLogtalkDirective(object_(Obj, Relations), Ctx) :-
        (	var(Obj) ->
        throw(instantiation_error)
        ;	\+ callable(Obj) ->
        throw(type_error(objectIdentifier, Obj))
        ;	ppRuntimeClause_(Current_object_(Obj, _, _, _, _, _, _, _, _, _, _)) ->
   //an object with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, object, Obj))
        ;	ppRuntimeClause_(Current_protocol_(Obj, _, _, _, _)) ->
   //a protocol with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, protocol, Obj))
        ;	ppRuntimeClause_(CurrentCategory_(Obj, _, _, _, _, _)) ->
   //a category with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, category, Obj))
        ;	functor(Obj, '{}', 1) ->
   //reserved syntax for object proxies
        throw(permission_error(create, object, Obj))
        ;	pp_entity_(Type, _, _, _, _) ->
   //opening object directive found while still compiling the previous entity
        (	Type == object ->
        throw(existence_error(directive, end_object/0))
        ;	Type == protocol ->
        throw(existence_error(directive, end_protocol/0))
        ;	% Type == category,
        throw(existence_error(directive, endCategory/0))
        )
        ;	print_message(silent(compiling), core, compiling_entity(object, Obj)),
        Compile_objectRelations(Relations, Obj, Ctx),
        Compile_objectIdentifier(Obj, Ctx),
        Save_parameter_variables(Obj)
        ).

        CompileLogtalkDirective(end_object, Ctx) :-
        (	pp_object_(Obj, _, _, _, _, _, _, _, _, _, _) ->
   //we're indeed compiling an object
        retract(ppReferenced_object_(Obj, _, Start-_)),
        CompCtxLines(Ctx, _-End),
        assertz(ppReferenced_object_(Obj, _, Start-End)),
        SecondStage(object, Obj, Ctx),
        print_message(silent(compiling), core, compiled_entity(object, Obj))
        ;	% entity ending directive mismatch
        throw(existence_error(directive, object/1))
        ).

   //protocol opening and closing directives

        CompileLogtalkDirective(protocol(Ptc), Ctx) :-
        CompileLogtalkDirective(protocol_(Ptc, []), Ctx).

        CompileLogtalkDirective(protocol(Ptc, Relation), Ctx) :-
        CompileLogtalkDirective(protocol_(Ptc, [Relation]), Ctx).

   //auxiliary predicate to compile all variants to the protocol opening directive
        CompileLogtalkDirective(protocol_(Ptc, Relations), Ctx) :-
        (	var(Ptc) ->
        throw(instantiation_error)
        ;	\+ atom(Ptc) ->
        throw(type_error(protocolIdentifier, Ptc))
        ;	ppRuntimeClause_(Current_object_(Ptc, _, _, _, _, _, _, _, _, _, _)) ->
   //an object with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, object, Ptc))
        ;	ppRuntimeClause_(Current_protocol_(Ptc, _, _, _, _)) ->
   //a protocol with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, protocol, Ptc))
        ;	ppRuntimeClause_(CurrentCategory_(Ptc, _, _, _, _, _)) ->
   //a category with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, category, Ptc))
        ;	pp_entity_(Type, _, _, _, _) ->
   //opening protocol directive found while still compiling the previous entity
        (	Type == object ->
        throw(existence_error(directive, end_object/0))
        ;	Type == protocol ->
        throw(existence_error(directive, end_protocol/0))
        ;	% Type == category,
        throw(existence_error(directive, endCategory/0))
        )
        ;	print_message(silent(compiling), core, compiling_entity(protocol, Ptc)),
        Compile_protocolIdentifier(Ptc, Ctx),
        Compile_protocolRelations(Relations, Ptc, Ctx)
        ).

        CompileLogtalkDirective(end_protocol, Ctx) :-
        (	pp_protocol_(Ptc, _, _, _, _) ->
   //we're indeed compiling a protocol
        retract(ppReferenced_protocol_(Ptc, _, Start-_)),
        CompCtxLines(Ctx, _-End),
        assertz(ppReferenced_protocol_(Ptc, _, Start-End)),
        SecondStage(protocol, Ptc, Ctx),
        print_message(silent(compiling), core, compiled_entity(protocol, Ptc))
        ;	% entity ending directive mismatch
        throw(existence_error(directive, protocol/1))
        ).

   //category opening and closing directives

        CompileLogtalkDirective(category(Ctg), Ctx) :-
        CompileLogtalkDirective(category_(Ctg, []), Ctx).

        CompileLogtalkDirective(category(Ctg, Relation), Ctx) :-
        CompileLogtalkDirective(category_(Ctg, [Relation]), Ctx).

        CompileLogtalkDirective(category(Ctg, Relation1, Relation2), Ctx) :-
        CompileLogtalkDirective(category_(Ctg, [Relation1, Relation2]), Ctx).

        CompileLogtalkDirective(category(Ctg, Relation1, Relation2, Relation3), Ctx) :-
        CompileLogtalkDirective(category_(Ctg, [Relation1, Relation2, Relation3]), Ctx).

   //auxiliary predicate to compile all variants to the category opening directive
        CompileLogtalkDirective(category_(Ctg, Relations), Ctx) :-
        (	var(Ctg) ->
        throw(instantiation_error)
        ;	\+ callable(Ctg) ->
        throw(type_error(categoryIdentifier, Ctg))
        ;	ppRuntimeClause_(Current_object_(Ctg, _, _, _, _, _, _, _, _, _, _)) ->
   //an object with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, object, Ctg))
        ;	ppRuntimeClause_(Current_protocol_(Ctg, _, _, _, _)) ->
   //a protocol with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, protocol, Ctg))
        ;	ppRuntimeClause_(CurrentCategory_(Ctg, _, _, _, _, _)) ->
   //a category with the same identifier was defined earlier in the same source file
        throw(permission_error(modify, category, Ctg))
        ;	pp_entity_(Type, _, _, _, _) ->
   //opening protocol directive found while still compiling the previous entity
        (	Type == object ->
        throw(existence_error(directive, end_object/0))
        ;	Type == protocol ->
        throw(existence_error(directive, end_protocol/0))
        ;	% Type == category,
        throw(existence_error(directive, endCategory/0))
        )
        ;	print_message(silent(compiling), core, compiling_entity(category, Ctg)),
        CompileCategoryIdentifier(Ctg, Ctx),
        CompileCategoryRelations(Relations, Ctg, Ctx),
        Save_parameter_variables(Ctg)
        ).

        CompileLogtalkDirective(endCategory, Ctx) :-
        (	ppCategory_(Ctg, _, _, _, _, _) ->
   //we're indeed compiling a category
        retract(ppReferencedCategory_(Ctg, _, Start-_)),
        CompCtxLines(Ctx, _-End),
        assertz(ppReferencedCategory_(Ctg, _, Start-End)),
        SecondStage(category, Ctg, Ctx),
        print_message(silent(compiling), core, compiled_entity(category, Ctg))
        ;	% entity ending directive mismatch
        throw(existence_error(directive, category/1))
        ).

   //compile modules as objects

        CompileLogtalkDirective(module(Module), Ctx) :-
   //empty export list
        CompileLogtalkDirective(module(Module, []), Ctx).

        CompileLogtalkDirective(module(Module, Exports), Ctx) :-
        Check(moduleIdentifier, Module),
        Check(list, Exports),
   //remember we are compiling a module
        assertz(pp_module_(Module)),
        print_message(silent(compiling), core, compiling_entity(module, Module)),
        Compile_objectIdentifier(Module, Ctx),
   //make the export list the public resources list
        CompileLogtalkDirective(public(Exports), Ctx).

   //setLogtalkFlag/2 entity directive

        CompileLogtalkDirective(setLogtalkFlag(Flag, Value), _) :-
        Check(read_writeFlag, Flag),
        Check(flag_value, Flag+Value),
        retractall(pp_entityCompilerFlag_(Flag, _)),
        assertz(pp_entityCompilerFlag_(Flag, Value)).

   //declare an entity as built-in

        CompileLogtalkDirective(builtIn, _) :-
        assertz(pp_builtIn_').

   //create a message queue at object initialization

        CompileLogtalkDirective(threaded, _) :-
        pp_entity_(Type, _, _, _, _),
        (	prologFeature(engines, unsupported),
        prologFeature(threads, unsupported) ->
        throw(resource_error(threads))
        ;	Type == object ->
        assertz(pp_threaded_')
        ;	Type == protocol ->
        throw(domain_error(protocolDirective, threaded/0))
        ;	% Type == category,
        throw(domain_error(categoryDirective, threaded/0))
        ).

   //dynamic/0 entity directive
        %
   //(entities are static by default but can be declared dynamic using this directive)

        CompileLogtalkDirective((dynamic), _) :-
        assertz(ppDynamic_').

   //initialization/1 object directive
        %
   //this directive cannot be used in categories and protocols as it's not always
   //possible to correctly compile initialization goals as there's no valid
   //compilation context values for "sender", "this", and "self"

        CompileLogtalkDirective(initialization(Goal), Ctx) :-
        pp_entity_(Type, Entity, Prefix, _, _),
        (	Type == object ->
   //MetaVars = [] as we're compiling a local call
        CompCtx(Ctx, (:- initialization(Goal)), _, Entity, Entity, Entity, Entity, Prefix, [], _, ExCtx, _, [], Lines),
        executionContext(ExCtx, Entity, Entity, Entity, Entity, [], []),
   //save the source data information for use in the second compiler stage
   //(where it might be required by calls to the logtalkLoadContext/2
   //predicate during goal expansion)
        (	pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines) ->
        SourceData = sd(Term, VariableNames, File, Lines)
        ;	SourceData = nil
        ),
        (	CompilerFlag(debug, on) ->
        assertz(pp_objectInitialization_(dgoal(Goal,Ctx), SourceData, Lines))
        ;	assertz(pp_objectInitialization_(goal(Goal,Ctx), SourceData, Lines))
        )
        ;	Type == protocol ->
        throw(domain_error(protocolDirective, (initialization)/1))
        ;	% Type == category,
        throw(domain_error(categoryDirective, (initialization)/1))
        ).

   //op/3 entity directive (operators are local to entities)

        CompileLogtalkDirective(op(Priority, Specifier, Operators), _) :-
        Check(operatorSpecification, op(Priority, Specifier, Operators)),
        Activate_entity_operators(Priority, Specifier, Operators, l).

   //uses/2 entity directive

        CompileLogtalkDirective(uses(Obj, Resources), Ctx) :-
        Check(objectIdentifier, Obj),
        AddReferenced_object(Obj, Ctx),
        Compile_usesDirective(Resources, Resources, Obj, Ctx).

   //use_module/2 module directive
        %
   //the first argument must be a module identifier; when a file specification
   //is used, as it's usual in Prolog, it must be expanded at the adapter file
   //level into a module identifier

        CompileLogtalkDirective(use_module(Module, Imports), Ctx) :-
        Check(moduleIdentifier, Module),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        CompileLogtalkDirective(uses(Module, Imports), Ctx)
        ;	% we're calling module predicates within an object or a category
        AddReferenced_module(Module, Ctx),
        Compile_use_moduleDirective(Imports, Imports, Module, Ctx)
        ).

   //reexport/2 module directive
        %
   //the first argument must be a module identifier; when a file specification
   //is used, as it's usual in Prolog, it must be expanded at the adapter file
   //level into a module identifier

        CompileLogtalkDirective(reexport(Module, Exports), Ctx) :-
   //we must be compiling a module as an object
        pp_module_(_),
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Check(moduleIdentifier, Module),
        Check(list, Exports),
        CompileReexportDirective(Exports, Module, Ctx).

   //info/1 entity directive

        CompileLogtalkDirective(info(Pairs), _) :-
        Compile_entityInfoDirective(Pairs, TPairs),
        assertz(pp_entityInfo_(TPairs)).

   //info/2 predicate directive

        CompileLogtalkDirective(info(Pred, Pairs), _) :-
        (	valid_predicateIndicator(Pred, Functor, Arity) ->
        Compile_predicateInfoDirective(Pairs, Functor, Arity, TPairs),
        assertz(pp_predicateInfo_(Functor/Arity, TPairs))
        ;	valid_non_terminalIndicator(Pred, Functor, Arity, ExtArity) ->
        Compile_predicateInfoDirective(Pairs, Functor, Arity, TPairs),
        assertz(pp_predicateInfo_(Functor/ExtArity, TPairs))
        ;	var(Pred) ->
        throw(instantiation_error)
        ;	throw(type_error(predicateIndicator, Pred))
        ).

   //synchronized/1 predicate directive

        CompileLogtalkDirective(synchronized(Resources), _) :-
        Flatten_toList(Resources, ResourcesFlatted),
        CompileSynchronizedDirective(ResourcesFlatted).

   //scope directives

        CompileLogtalkDirective(public(Resources), Ctx) :-
        Flatten_toList(Resources, ResourcesFlatted),
        SourceFileContext(Ctx, File, Line-_),
        CompileScopeDirective(ResourcesFlatted, (public), File, Line).

        CompileLogtalkDirective(protected(Resources), Ctx) :-
        Flatten_toList(Resources, ResourcesFlatted),
        SourceFileContext(Ctx, File, Line-_),
        CompileScopeDirective(ResourcesFlatted, protected, File, Line).

        CompileLogtalkDirective(private(Resources), Ctx) :-
        Flatten_toList(Resources, ResourcesFlatted),
        SourceFileContext(Ctx, File, Line-_),
        CompileScopeDirective(ResourcesFlatted, (private), File, Line).

   //export/1 module directive
        %
   //module exported directives are compiled as object public directives

        CompileLogtalkDirective(export(Exports), Ctx) :-
   //we must be compiling a module as an object
        pp_module_(_),
   //make the export list public resources
        CompileLogtalkDirective(public(Exports), Ctx).

   //dynamic/1 and discontiguous/1 predicate directives

        CompileLogtalkDirective(dynamic(Resources), _) :-
        Flatten_toList(Resources, ResourcesFlatted),
        CompileDynamicDirective(ResourcesFlatted).

        CompileLogtalkDirective(discontiguous(Resources), _) :-
        Flatten_toList(Resources, ResourcesFlatted),
        CompileDiscontiguousDirective(ResourcesFlatted).

   //meta_predicate/2 and meta_non_terminal/1 predicate directives

        CompileLogtalkDirective(meta_predicate(Preds), _) :-
        Flatten_toList(Preds, PredsFlatted),
        (	pp_module_(_) ->
   //we're compiling a module as an object
        Compile_module_meta_predicateDirective(PredsFlatted, TPredsFlatted)
        ;	% we're compiling a Logtalk entity
        TPredsFlatted = PredsFlatted
        ),
        Compile_meta_predicateDirective(TPredsFlatted).

        CompileLogtalkDirective(meta_non_terminal(Preds), _) :-
        Flatten_toList(Preds, PredsFlatted),
        Compile_meta_non_terminalDirective(PredsFlatted).

   //mode/2 predicate directive

        CompileLogtalkDirective(mode(Mode, Solutions), _) :-
        (var(Mode); var(Solutions)),
        throw(instantiation_error).

        CompileLogtalkDirective(mode(Mode, _), _) :-
        \+ valid_mode_template(Mode),
        throw(type_error(mode_term, Mode)).

        CompileLogtalkDirective(mode(_, Solutions), _) :-
        \+ valid_number_of_proofs(Solutions),
        throw(type_error(number_of_proofs, Solutions)).

        CompileLogtalkDirective(mode(Mode, Solutions), Ctx) :-
        SourceFileContext(Ctx, File, Lines),
        assertz(pp_mode_(Mode, Solutions, File, Lines)).

   //multifile/2 predicate directive

        CompileLogtalkDirective(multifile(Preds), Ctx) :-
        Flatten_toList(Preds, PredsFlatted),
        Compile_multifileDirective(PredsFlatted, Ctx).

   //coinductive/1 predicate directive

        CompileLogtalkDirective(coinductive(Preds), _) :-
        (	prologFeature(coinduction, supported) ->
        Flatten_toList(Preds, PredsFlatted),
        CompileCoinductiveDirective(PredsFlatted)
        ;	throw(resource_error(coinduction))
        ).

   //alias/2 entity directive

        CompileLogtalkDirective(alias(Entity, Resources), Ctx) :-
        Check(entityIdentifier, Entity),
        CompileAliasDirective(Resources, Resources, Entity, Ctx).



   //CompileAliasDirective(+list, +list, @entityIdentifier, +compilationContext)
        %
   //auxiliary predicate for compiling alias/2 directives

        CompileAliasDirective(_, _, Entity, _) :-
        \+ pp_extended_protocol_(Entity, _, _, _, _),
        \+ ppImplemented_protocol_(Entity, _, _, _, _),
        \+ pp_extendedCategory_(Entity, _, _, _, _, _),
        \+ ppImportedCategory_(Entity, _, _, _, _, _),
        \+ pp_extended_object_(Entity, _, _, _, _, _, _, _, _, _, _),
        \+ ppInstantiatedClass_(Entity, _, _, _, _, _, _, _, _, _, _),
        \+ ppSpecializedClass_(Entity, _, _, _, _, _, _, _, _, _, _),
        \+ ppComplemented_object_(Entity, _, _, _, _),
        throw(reference_error(entityIdentifier, Entity)).

        CompileAliasDirective([Resource| Resources], Argument, Entity, Ctx) :-
        !,
        Check(ground, Resource),
        CompileAliasDirectiveResource(Resource, Entity, Ctx),
        CompileAliasDirective(Resources, Argument, Entity, Ctx).

        CompileAliasDirective([], _, _, _) :-
        !.

        CompileAliasDirective(_, Argument, _, _) :-
        throw(type_error(list, Argument)).


        CompileAliasDirectiveResource(as(Original,Alias), Entity, Ctx) :-
        !,
        CompileAliasDirectiveResource(Original::Alias, Entity, Ctx).

        CompileAliasDirectiveResource(Original::Alias, Entity, Ctx) :-
        !,
        Check(predicate_or_non_terminalIndicator, Original),
        Check(predicate_or_non_terminalIndicator, Alias),
        CompileAliasDirectiveResource(Original, Alias, Entity, Ctx).

        CompileAliasDirectiveResource(Resource, _, _) :-
        throw(type_error(predicateAliasSpecification, Resource)).


        CompileAliasDirectiveResource(Functor1/Arity, Functor2/Arity, Entity, Ctx) :-
        !,
        functor(Pred, Functor1, Arity),
        Pred =.. [Functor1| Args],
        Alias =.. [Functor2| Args],
        SourceFileContext(Ctx, File, Lines),
        assertz(pp_predicateAlias_(Entity, Pred, Alias, 0, File, Lines)).

        CompileAliasDirectiveResource(Functor1//Arity, Functor2//Arity, Entity, Ctx) :-
        !,
        ExtArity is Arity + 2,
        functor(Pred, Functor1, ExtArity),
        Pred =.. [Functor1| Args],
        Alias =.. [Functor2| Args],
        SourceFileContext(Ctx, File, Lines),
        assertz(pp_predicateAlias_(Entity, Pred, Alias, 1, File, Lines)).

        CompileAliasDirectiveResource(_//Arity1, _//Arity2, _, _) :-
        throw(domain_error({Arity1}, Arity2)).

        CompileAliasDirectiveResource(_/Arity1, _/Arity2, _, _) :-
        throw(domain_error({Arity1}, Arity2)).

        CompileAliasDirectiveResource(_/_, Functor2//Arity2, _, _) :-
        throw(type_error(predicateIndicator, Functor2//Arity2)).

        CompileAliasDirectiveResource(_//_, Functor2/Arity2, _, _) :-
        throw(type_error(non_terminalIndicator, Functor2/Arity2)).



   //CompileSynchronizedDirective(+list)
        %
   //auxiliary predicate for compiling synchronized/1 directives

        CompileSynchronizedDirective(Resources) :-
        new_predicate_mutex(Mutex),
        CompileSynchronizedDirective(Resources, Mutex).


        new_predicate_mutex(Mutex) :-
        pp_entity_(_, _, Prefix, _, _),
        retract(pp_predicate_mutexCounter_(Old)),
        New is Old + 1,
        asserta(pp_predicate_mutexCounter_(New)),
        numberCodes(New, Codes),
        atomCodes(Atom, Codes),
        atomConcat(Prefix, 'pred_mutex_', Aux),
        atomConcat(Aux, Atom, Mutex).


   //note that the clause order ensures that instantiation errors will be caught by
   //the call to the CompileSynchronizedDirectiveResource'/1 predicate

        CompileSynchronizedDirective([Resource| Resources], Mutex) :-
        CompileSynchronizedDirectiveResource(Resource, Mutex),
        CompileSynchronizedDirective(Resources, Mutex).

        CompileSynchronizedDirective([], _).


        CompileSynchronizedDirectiveResource(Pred, Mutex) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        functor(Head, Functor, Arity),
        (	ppDynamic_(Head) ->
   //synchronized predicates must be static
        throw(permission_error(modify, dynamic_predicate, Functor/Arity))
        ;	ppDefines_predicate_(Head, _, _, _, _, _) ->
   //synchronized/1 directives must precede the definitions for the declared predicates
        throw(permission_error(modify, predicateInterpretation, Functor/Arity))
        ;	assertz(ppSynchronized_(Head, Mutex))
        ).

        CompileSynchronizedDirectiveResource(NonTerminal, Mutex) :-
        valid_non_terminalIndicator(NonTerminal, Functor, Arity, ExtArity),
        !,
        functor(Head, Functor, ExtArity),
        (	ppDynamic_(Head) ->
   //synchronized non-terminals must be static
        throw(permission_error(modify, dynamic_non_terminal, Functor//Arity))
        ;	ppDefines_non_terminal_(Functor, Arity) ->
        throw(permission_error(modify, non_terminalInterpretation, Functor//Arity))
        ;	ppDefines_predicate_(Head, _, _, _, _, _) ->
   //synchronized/1 directives must precede the definitions for the declared non-terminals
        throw(permission_error(modify, non_terminalInterpretation, Functor//Arity))
        ;	assertz(ppSynchronized_(Head, Mutex))
        ).

        CompileSynchronizedDirectiveResource(Resource, _) :-
        ground(Resource),
        throw(type_error(predicateIndicator, Resource)).

        CompileSynchronizedDirectiveResource(_, _) :-
        throw(instantiation_error).



   //CompileScopeDirective(+list, @scope, +atom, +integer)
        %
   //auxiliary predicate for compiling scope directives
        %
   //note that the clause order ensures that instantiation errors will be caught
   //by the call to the CompileScopeDirectiveResource'/1 predicate

        CompileScopeDirective([Resource| Resources], Scope, File, Line) :-
        CompileScopeDirectiveResource(Resource, Scope, File, Line),
        CompileScopeDirective(Resources, Scope, File, Line).

        CompileScopeDirective([], _, _, _).



   //CompileScopeDirectiveResource(@term, @scope, +integer)
        %
   //auxiliary predicate for compiling scope directive resources

        CompileScopeDirectiveResource(op(Priority, Specifier, Operators), Scope, _, _) :-
        Check(operatorSpecification, op(Priority, Specifier, Operators)),
        !,
        CheckForDuplicatedScopeDirectives(op(Priority, Specifier, Operators), Scope),
        Scope(Scope, InternalScope),
        Activate_entity_operators(Priority, Specifier, Operators, InternalScope).

        CompileScopeDirectiveResource(Functor/Arity, Scope, File, Line) :-
        valid_predicateIndicator(Functor/Arity, Functor, Arity),
        !,
        CheckForDuplicatedScopeDirectives(Functor/Arity, Scope),
        Add_predicateScopeDirective(Scope, Functor, Arity),
        assertz(pp_predicateDeclarationLocation_(Functor, Arity, File, Line)).

        CompileScopeDirectiveResource(Functor//Arity, Scope, File, Line) :-
        valid_non_terminalIndicator(Functor//Arity, Functor, Arity, ExtArity),
        !,
        CheckForDuplicatedScopeDirectives(Functor//Arity+ExtArity, Scope),
        assertz(pp_non_terminal_(Functor, Arity, ExtArity)),
        Add_predicateScopeDirective(Scope, Functor, ExtArity),
        assertz(pp_predicateDeclarationLocation_(Functor, ExtArity, File, Line)).

        CompileScopeDirectiveResource(Resource, _, _, _) :-
        ground(Resource),
        throw(type_error(predicateIndicator, Resource)).

        CompileScopeDirectiveResource(_, _, _, _) :-
        throw(instantiation_error).


        Add_predicateScopeDirective((public), Functor, Arity) :-
        assertz(pp_public_(Functor, Arity)).

        Add_predicateScopeDirective(protected, Functor, Arity) :-
        assertz(pp_protected_(Functor, Arity)).

        Add_predicateScopeDirective((private), Functor, Arity) :-
        assertz(pp_private_(Functor, Arity)).


        CheckForDuplicatedScopeDirectives(op(_, _, []), _) :-
        !.

        CheckForDuplicatedScopeDirectives(op(Priority, Specifier, [Operator| Operators]), Scope) :-
        !,
        (	pp_entity_operator_(Priority, Specifier, Operator, Scope) ->
        (	CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        Directive =.. [Scope, op(Priority, Specifier, Operator)],
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, Directive))
        ;	true
        )
        ;	pp_entity_operator_(Priority, Specifier, Operator, _) ->
        throw(permission_error(modify, operatorScope, op(Priority, Specifier, Operator)))
        ;	CheckForDuplicatedScopeDirectives(op(Priority, Specifier, Operators), Scope)
        ).

        CheckForDuplicatedScopeDirectives(op(Priority, Specifier, Operator), Scope) :-
        (	pp_entity_operator_(Priority, Specifier, Operator, Scope) ->
        (	CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        Directive =.. [Scope, op(Priority, Specifier, Operator)],
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, Directive))
        ;	true
        )
        ;	pp_entity_operator_(Priority, Specifier, Operator, _) ->
        throw(permission_error(modify, predicateScope, op(Priority, Specifier, Operator)))
        ;	true
        ).

        CheckForDuplicatedScopeDirectives(Functor/Arity, Scope) :-
        (	(	Scope == (public), pp_public_(Functor, Arity)
        ;	Scope == protected, pp_protected_(Functor, Arity)
        ;	Scope == (private), pp_private_(Functor, Arity)
        ) ->
        (	CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        Directive =.. [Scope, Functor/Arity],
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, Directive))
        ;	true
        )
        ;	(	pp_public_(Functor, Arity)
        ;	pp_protected_(Functor, Arity)
        ;	pp_private_(Functor, Arity)
        ) ->
        throw(permission_error(modify, predicateScope, Functor/Arity))
        ;	true
        ).

        CheckForDuplicatedScopeDirectives(Functor//Arity+ExtArity, Scope) :-
        (	(	Scope == (public), pp_public_(Functor, ExtArity)
        ;	Scope == protected, pp_protected_(Functor, ExtArity)
        ;	Scope == (private), pp_private_(Functor, ExtArity)
        ) ->
        (	CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        Directive =.. [Scope, Functor//Arity],
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, Directive))
        ;	true
        )
        ;	(	pp_public_(Functor, ExtArity)
        ;	pp_protected_(Functor, ExtArity)
        ;	pp_private_(Functor, ExtArity)
        ) ->
        throw(permission_error(modify, non_terminalScope, Functor//Arity))
        ;	true
        ).



   //CompileDynamicDirective(+list)
        %
   //auxiliary predicate for compiling dynamic/1 directives
        %
   //note that the clause order ensures that instantiation errors will be caught
   //by the call to the CompileDynamicDirectiveResource'/1 predicate

        CompileDynamicDirective([Resource| Resources]) :-
        CompileDynamicDirectiveResource(Resource),
        CompileDynamicDirective(Resources).

        CompileDynamicDirective([]).


        CompileDynamicDirectiveResource(Entity::Pred) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        (	Entity == user ->
        CheckForDuplicatedDirective(dynamic(Functor/Arity), dynamic(Entity::Pred)),
        assertz(ppDirective_(dynamic(Functor/Arity)))
        ;	Check(entityIdentifier, Entity),
        entity_to_prefix(Entity, Prefix),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        CheckForDuplicatedDirective(dynamic(TFunctor/TArity), dynamic(Entity::Pred)),
        assertz(ppDirective_(dynamic(TFunctor/TArity)))
        ).

        CompileDynamicDirectiveResource(Entity::NonTerminal) :-
        valid_non_terminalIndicator(NonTerminal, Functor, _, ExtArity),
        !,
        (	Entity == user ->
        CheckForDuplicatedDirective(dynamic(Functor/ExtArity), dynamic(Entity::NonTerminal)),
        assertz(ppDirective_(dynamic(Functor/ExtArity)))
        ;	Check(entityIdentifier, Entity),
        entity_to_prefix(Entity, Prefix),
        Compile_predicateIndicator(Prefix, Functor/ExtArity, TFunctor/TArity),
        CheckForDuplicatedDirective(dynamic(TFunctor/TArity), dynamic(Entity::NonTerminal)),
        assertz(ppDirective_(dynamic(TFunctor/TArity)))
        ).

        CompileDynamicDirectiveResource(':(Module, Pred)) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        (	Module == user ->
        CheckForDuplicatedDirective(dynamic(Functor/Arity), dynamic(':(Module, Pred))),
        assertz(ppDirective_(dynamic(Functor/Arity)))
        ;	Check(moduleIdentifier, Module),
        CheckForDuplicatedDirective(dynamic(':(Module, Functor/Arity)), dynamic(':(Module, Pred))),
        assertz(ppDirective_(dynamic(':(Module, Functor/Arity))))
        ).

        CompileDynamicDirectiveResource(':(Module, NonTerminal)) :-
        valid_non_terminalIndicator(NonTerminal, Functor, _, ExtArity),
        !,
        (	Module == user ->
        CheckForDuplicatedDirective(dynamic(Functor/ExtArity), dynamic(':(Module, NonTerminal))),
        assertz(ppDirective_(dynamic(Functor/ExtArity)))
        ;	Check(moduleIdentifier, Module),
        CheckForDuplicatedDirective(dynamic(':(Module, Functor/ExtArity)), dynamic(':(Module, NonTerminal))),
        assertz(ppDirective_(dynamic(':(Module, Functor/ExtArity))))
        ).

        CompileDynamicDirectiveResource(Pred) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        functor(Head, Functor, Arity),
        (	ppSynchronized_(Head, _) ->
   //synchronized predicates must be static
        throw(permission_error(modify, synchronized_predicate, Functor/Arity))
        ;	CheckForDuplicatedDynamicDirective(Head, Pred),
        assertz(ppDynamic_(Head))
        ).

        CompileDynamicDirectiveResource(NonTerminal) :-
        valid_non_terminalIndicator(NonTerminal, Functor, Arity, ExtArity),
        !,
        functor(Head, Functor, ExtArity),
        (	ppSynchronized_(Head, _) ->
   //synchronized non-terminals must be static
        throw(permission_error(modify, synchronized_non_terminal, Functor//Arity))
        ;	CheckForDuplicatedDynamicDirective(Head, NonTerminal),
        assertz(ppDynamic_(Head))
        ).

        CompileDynamicDirectiveResource(Resource) :-
        ground(Resource),
        throw(type_error(predicateIndicator, Resource)).

        CompileDynamicDirectiveResource(_) :-
        throw(instantiation_error).


        CheckForDuplicatedDynamicDirective(Head, PI) :-
        (	ppDynamic_(Head),
        CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, dynamic(PI)))
        ;	true
        ).



   //CompileDiscontiguousDirective(+list)
        %
   //auxiliary predicate for compiling discontiguous/1 directives
        %
   //note that the clause order ensures that instantiation errors will be caught by
   //the call to the CompileDiscontiguousDirectiveResource'/1 predicate

        CompileDiscontiguousDirective([Resource| Resources]) :-
        CompileDiscontiguousDirectiveResource(Resource),
        CompileDiscontiguousDirective(Resources).

        CompileDiscontiguousDirective([]).


        CompileDiscontiguousDirectiveResource(Entity::Pred) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        (	Entity == user ->
        CheckForDuplicatedDirective(discontiguous(Functor/Arity), discontiguous(Entity::Pred)),
        assertz(ppDirective_(discontiguous(Functor/Arity)))
        ;	Check(entityIdentifier, Entity),
        entity_to_prefix(Entity, Prefix),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        CheckForDuplicatedDirective(discontiguous(TFunctor/TArity), discontiguous(Entity::Pred)),
        assertz(ppDirective_(discontiguous(TFunctor/TArity)))
        ).

        CompileDiscontiguousDirectiveResource(Entity::NonTerminal) :-
        valid_non_terminalIndicator(NonTerminal, Functor, _, ExtArity),
        !,
        (	Entity == user ->
        CheckForDuplicatedDirective(discontiguous(Functor/ExtArity), discontiguous(Entity::NonTerminal)),
        assertz(ppDirective_(discontiguous(Functor/ExtArity)))
        ;	Check(entityIdentifier, Entity),
        entity_to_prefix(Entity, Prefix),
        Compile_predicateIndicator(Prefix, Functor/ExtArity, TFunctor/TArity),
        CheckForDuplicatedDirective(discontiguous(TFunctor/TArity), discontiguous(Entity::NonTerminal)),
        assertz(ppDirective_(discontiguous(TFunctor/TArity)))
        ).

        CompileDiscontiguousDirectiveResource(':(Module, Pred)) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        (	Module == user ->
        CheckForDuplicatedDirective(discontiguous(Functor/Arity), discontiguous(':(Module, Pred))),
        assertz(ppDirective_(discontiguous(Functor/Arity)))
        ;	Check(moduleIdentifier, Module),
        CheckForDuplicatedDirective(discontiguous(':(Module, Functor/Arity)), discontiguous(':(Module, Pred))),
        assertz(ppDirective_(discontiguous(':(Module, Functor/Arity))))
        ).

        CompileDiscontiguousDirectiveResource(':(Module, NonTerminal)) :-
        valid_non_terminalIndicator(NonTerminal, Functor, _, ExtArity),
        !,
        (	Module == user ->
        CheckForDuplicatedDirective(discontiguous(Functor/ExtArity), discontiguous(':(Module, NonTerminal))),
        assertz(ppDirective_(discontiguous(Functor/ExtArity)))
        ;	Check(moduleIdentifier, Module),
        CheckForDuplicatedDirective(discontiguous(':(Module, Functor/ExtArity)), discontiguous(':(Module, NonTerminal))),
        assertz(ppDirective_(discontiguous(':(Module, Functor/ExtArity))))
        ).

        CompileDiscontiguousDirectiveResource(Pred) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        functor(Head, Functor, Arity),
        CheckForDuplicatedDiscontiguousDirective(Head, Pred),
        assertz(ppDiscontiguous_(Head)).

        CompileDiscontiguousDirectiveResource(NonTerminal) :-
        valid_non_terminalIndicator(NonTerminal, Functor, _, ExtArity),
        !,
        functor(Head, Functor, ExtArity),
        CheckForDuplicatedDiscontiguousDirective(Head, NonTerminal),
        assertz(ppDiscontiguous_(Head)).

        CompileDiscontiguousDirectiveResource(Resource) :-
        ground(Resource),
        throw(type_error(predicateIndicator, Resource)).

        CompileDiscontiguousDirectiveResource(_) :-
        throw(instantiation_error).


        CheckForDuplicatedDiscontiguousDirective(Head, PI) :-
        (	ppDiscontiguous_(Head),
        CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, discontiguous(PI)))
        ;	true
        ).



   //Compile_meta_predicateDirective(+list)
        %
   //auxiliary predicate for compiling meta_predicate/1 directives
        %
   //note that the clause order ensures that instantiation errors will be caught by
   //the call to the Compile_meta_predicateDirectiveResource'/1 predicate

        Compile_meta_predicateDirective([Meta| Metas]) :-
        Compile_meta_predicateDirectiveResource(Meta),
        Compile_meta_predicateDirective(Metas).

        Compile_meta_predicateDirective([]).


        Compile_meta_predicateDirectiveResource(Entity::Meta) :-
        valid_meta_predicate_template(Meta),
        !,
        Check(entityIdentifier, Entity),
        term_template(Meta, Template),
        CheckForDuplicated_meta_predicateDirective(Entity::Template, Entity::Meta),
        assertz(pp_meta_predicate_(Entity::Template, Entity::Meta)).

        Compile_meta_predicateDirectiveResource(':(Module, Meta)) :-
        valid_meta_predicate_template(Meta),
        !,
        Check(moduleIdentifier, Module),
        term_template(Meta, Template),
        CheckForDuplicated_meta_predicateDirective(':(Module,Template), ':(Module,Meta)),
        assertz(pp_meta_predicate_(':(Module,Template), ':(Module,Meta))).

        Compile_meta_predicateDirectiveResource(Meta) :-
        valid_meta_predicate_template(Meta),
        !,
        term_template(Meta, Template),
        CheckForDuplicated_meta_predicateDirective(Template, Meta),
        assertz(pp_meta_predicate_(Template, Meta)).

        Compile_meta_predicateDirectiveResource(Meta) :-
        ground(Meta),
        throw(type_error(meta_predicate_template, Meta)).

        Compile_meta_predicateDirectiveResource(_) :-
        throw(instantiation_error).


        CheckForDuplicated_meta_predicateDirective(Template, Meta) :-
        (	pp_meta_predicate_(Template, Meta) ->
        (	CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, meta_predicate(Meta)))
        ;	true
        )
        ;	pp_meta_predicate_(Template, _) ->
        throw(permission_error(modify, meta_predicate_template, Meta))
        ;	true
        ).



   //Compile_meta_non_terminalDirective(+list)
        %
   //auxiliary predicate for compiling meta_non_terminal/1 directives
        %
   //note that the clause order ensures that instantiation errors will be caught by the
   //call to the Compile_meta_non_terminalDirectiveResource'/1 predicate

        Compile_meta_non_terminalDirective([Meta| Metas]) :-
        Compile_meta_non_terminalDirectiveResource(Meta),
        Compile_meta_non_terminalDirective(Metas).

        Compile_meta_non_terminalDirective([]).


        Compile_meta_non_terminalDirectiveResource(Entity::Meta) :-
        valid_meta_predicate_template(Meta),
        !,
        Check(entityIdentifier, Entity),
        extend_meta_non_terminal_template(Meta, ExtendedMeta),
        term_template(ExtendedMeta, Template),
        CheckForDuplicated_meta_non_terminalDirective(Entity::Template, Entity::ExtendedMeta, Entity::Meta),
        assertz(pp_meta_predicate_(Entity::Template, Entity::ExtendedMeta)).

        Compile_meta_non_terminalDirectiveResource(':(Module, Meta)) :-
        valid_meta_predicate_template(Meta),
        !,
        Check(moduleIdentifier, Module),
        extend_meta_non_terminal_template(Meta, ExtendedMeta),
        term_template(ExtendedMeta, Template),
        CheckForDuplicated_meta_non_terminalDirective(':(Module, Template), ':(Module, ExtendedMeta), ':(Module, Meta)),
        assertz(pp_meta_predicate_(':(Module, Template), ':(Module, ExtendedMeta))).

        Compile_meta_non_terminalDirectiveResource(Meta) :-
        valid_meta_predicate_template(Meta),
        !,
        extend_meta_non_terminal_template(Meta, ExtendedMeta),
        term_template(ExtendedMeta, Template),
        CheckForDuplicated_meta_non_terminalDirective(Template, ExtendedMeta, Meta),
        assertz(pp_meta_predicate_(Template, ExtendedMeta)).

        Compile_meta_non_terminalDirectiveResource(Meta) :-
        ground(Meta),
        throw(type_error(meta_non_terminal_template, Meta)).

        Compile_meta_non_terminalDirectiveResource(_) :-
        throw(instantiation_error).


        extend_meta_non_terminal_template(Meta, ExtendedMeta) :-
        Meta =.. [Functor| Args],
        Compile_meta_non_terminalDirectiveArgs(Args, ExtendedArgs),
        ExtendedMeta =.. [Functor| ExtendedArgs].


        Compile_meta_non_terminalDirectiveArgs([], [*, *]).

        Compile_meta_non_terminalDirectiveArgs([Arg| Args], [ExtendedArg| ExtendedArgs]) :-
        (	integer(Arg) ->
        ExtendedArg is Arg + 2
        ;	ExtendedArg = Arg
        ),
        Compile_meta_non_terminalDirectiveArgs(Args, ExtendedArgs).


        CheckForDuplicated_meta_non_terminalDirective(Template, ExtendedMeta, Meta) :-
        (	pp_meta_predicate_(Template, ExtendedMeta) ->
        (	CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, meta_non_terminal(Meta)))
        ;	true
        )
        ;	pp_meta_predicate_(Template, _) ->
        throw(permission_error(modify, meta_non_terminal_template, Meta))
        ;	true
        ).



   //Compile_multifileDirective(+list, +compilationContext)
        %
   //auxiliary predicate for compiling multifile/1 directives
        %
   //when the multifile predicate (or non-terminal) is declared for the module
   //"user", the module prefix is removed to ensure code portability when using
   //backend Prolog compilers without a module system
        %
   //note that the clause order ensures that instantiation errors will be caught
   //by the call to the Compile_multifileDirectiveResource'/1 predicate

        Compile_multifileDirective([Resource| Resources], Ctx) :-
        Compile_multifileDirectiveResource(Resource, Ctx),
        Compile_multifileDirective(Resources, Ctx).

        Compile_multifileDirective([], _).


        Compile_multifileDirectiveResource(Entity::Pred, _) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        (	Entity == user ->
        CheckForDuplicatedDirective(multifile(Functor/Arity), multifile(Entity::Pred)),
        assertz(ppDirective_(multifile(Functor/Arity)))
        ;	Check(entityIdentifier, Entity),
        functor(Template, Functor, Arity),
        Check_primary_multifileDeclaration(Entity, Template) ->
        entity_to_prefix(Entity, Prefix),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        CheckForDuplicatedDirective(multifile(TFunctor/TArity), multifile(Entity::Pred)),
        assertz(ppDirective_(multifile(TFunctor/TArity)))
        ;	throw(permission_error(modify, predicateDeclaration, Pred))
        ).

        Compile_multifileDirectiveResource(Entity::NonTerminal, _) :-
        valid_non_terminalIndicator(NonTerminal, Functor, _, ExtArity),
        !,
        (	Entity == user ->
        CheckForDuplicatedDirective(multifile(Functor/ExtArity), multifile(Entity::NonTerminal)),
        assertz(ppDirective_(multifile(Functor/ExtArity)))
        ;	Check(entityIdentifier, Entity),
        functor(Template, Functor, ExtArity),
        Check_primary_multifileDeclaration(Entity, Template) ->
        entity_to_prefix(Entity, Prefix),
        Compile_predicateIndicator(Prefix, Functor/ExtArity, TFunctor/TArity),
        CheckForDuplicatedDirective(multifile(TFunctor/TArity), multifile(Entity::NonTerminal)),
        assertz(ppDirective_(multifile(TFunctor/TArity)))
        ;	throw(permission_error(modify, non_terminalDeclaration, NonTerminal))
        ).

        Compile_multifileDirectiveResource(':(Module, Pred), _) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        (	Module == user ->
        CheckForDuplicatedDirective(multifile(Functor/Arity), multifile(':(Module, Pred))),
        assertz(ppDirective_(multifile(Functor/Arity)))
        ;	Check(moduleIdentifier, Module),
        CheckForDuplicatedDirective(multifile(':(Module, Functor/Arity)), multifile(':(Module, Pred))),
        assertz(ppDirective_(multifile(':(Module, Functor/Arity))))
        ).

        Compile_multifileDirectiveResource(':(Module, NonTerminal), _) :-
        valid_non_terminalIndicator(NonTerminal, Functor, _, ExtArity),
        !,
        (	Module == user ->
        CheckForDuplicatedDirective(multifile(Functor/ExtArity), multifile(':(Module, NonTerminal))),
        assertz(ppDirective_(multifile(Functor/ExtArity)))
        ;	Check(moduleIdentifier, Module),
        CheckForDuplicatedDirective(multifile(':(Module, Functor/ExtArity)), multifile(':(Module, NonTerminal))),
        assertz(ppDirective_(multifile(':(Module, Functor/ExtArity))))
        ).

        Compile_multifileDirectiveResource(Pred, Ctx) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        pp_entity_(Type, _, Prefix, _, _),
        (	Type == protocol ->
   //protocols cannot contain predicate definitions
        throw(permission_error(declare, multifile, Functor/Arity))
        ;	functor(Head, Functor, Arity),
        SourceFileContext(Ctx, File, Lines),
        assertz(pp_multifile_(Head, File, Lines)),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        CheckForDuplicatedDirective(multifile(TFunctor/TArity), multifile(Pred)),
        assertz(ppDirective_(multifile(TFunctor/TArity)))
        ).

        Compile_multifileDirectiveResource(NonTerminal, Ctx) :-
        valid_non_terminalIndicator(NonTerminal, Functor, Arity, ExtArity),
        !,
        pp_entity_(Type, _, Prefix, _, _),
        (	Type == protocol ->
   //protocols cannot contain non-terminal definitions
        throw(permission_error(declare, multifile, Functor//Arity))
        ;	functor(Head, Functor, ExtArity),
        SourceFileContext(Ctx, File, Lines),
        assertz(pp_multifile_(Head, File, Lines)),
        Compile_predicateIndicator(Prefix, Functor/ExtArity, TFunctor/TArity),
        CheckForDuplicatedDirective(multifile(TFunctor/TArity), multifile(NonTerminal)),
        assertz(ppDirective_(multifile(TFunctor/TArity)))
        ).

        Compile_multifileDirectiveResource(Resource, _) :-
        ground(Resource),
        throw(type_error(predicateIndicator, Resource)).

        Compile_multifileDirectiveResource(_, _) :-
        throw(instantiation_error).


        Check_primary_multifileDeclaration(Entity, Pred) :-
   //the object or categry holding the primary declaration must be loaded
        (	Current_object_(Entity, _, Dcl, _, _, _, _, _, _, _, _)
        ;	CurrentCategory_(Entity, _, Dcl, _, _, _)
        ), !,
   //the predicate must be declared (i.e. have a scope directive) and multifile
        (	call(Dcl, Pred, Scope, _, Flags) ->
        functor(Scope, p, _),
        Flags /\ 16 =:= 16
        ;	fail
        ).



   //CompileCoinductiveDirective(+list)
        %
   //auxiliary predicate for compiling coinductive/1 directives
        %
   //note that the clause order ensures that instantiation errors will be caught by
   //the call to the CompileCoinductiveDirectiveResource'/1 predicate

        CompileCoinductiveDirective([Pred| Preds]) :-
        CompileCoinductiveDirectiveResource(Pred),
        CompileCoinductiveDirective(Preds).

        CompileCoinductiveDirective([]).


        CompileCoinductiveDirectiveResource(Pred) :-
        validCoinductive_template(Pred, Functor, Arity, Head, TestHead, Template),
        !,
        (	ppDefines_predicate_(Head, _, _, _, _, _) ->
   //coinductive/1 directives must precede the definitions for the declared predicates
        throw(permission_error(modify, predicateInterpretation, Functor/Arity))
        ;	true
        ),
   //construct functor for the auxiliary predicate
        atomConcat(Functor, '_Coinductive', CFunctor),
   //construct functor for debugging calls to the auxiliary predicate
        atomConcat(Functor, '_Coinduction_preflight', DFunctor),
        functor(DHead, DFunctor, Arity),
        Head =.. [_| Args],
        DHead =.. [_| Args],
        pp_entity_(_, Entity, Prefix, _, _),
        Compile_predicateIndicator(Prefix, CFunctor/Arity, TCFunctor/TCArity),
        functor(TCHead, TCFunctor, TCArity),
        unify_head_theadArguments(Head, TCHead, HeadExCtx),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        functor(THead, TFunctor, TArity),
        unify_head_theadArguments(Head, THead, BodyExCtx),
        assertz(ppCoinductive_(Head, TestHead, HeadExCtx, TCHead, BodyExCtx, THead, DHead)),
        assertz(ppRuntimeClause_(predicate_property_(Entity, Functor/Arity, coinductive(Template)))).

        CompileCoinductiveDirectiveResource(Pred) :-
        ground(Pred),
        throw(type_error(predicateIndicator, Pred)).

        CompileCoinductiveDirectiveResource(_) :-
        throw(instantiation_error).


        CheckCoinductiveSuccess(Hypothesis, [Hypothesis| _], Hypothesis).

        CheckCoinductiveSuccess(TestHead, [_| Stack], Hypothesis) :-
        CheckCoinductiveSuccess(TestHead, Stack, Hypothesis).


        validCoinductive_template(PredicateIndicator, Functor, Arity, Head, Head, Template) :-
        valid_predicateIndicator(PredicateIndicator, Functor, Arity),
        !,
        functor(Head, Functor, Arity),
        Construct_extendedCoinductive_template(Functor, Arity, Template).

        validCoinductive_template(Template, Functor, Arity, Head, TestHead, Template) :-
        Check(callable, Template),
        Check(ground, Template),
        functor(Template, Functor, Arity),
        functor(Head, Functor, Arity),
        Template =.. [Functor| TemplateArgs],
        Head =.. [Functor| HeadArgs],
        mapCoinductive_templateArgs(TemplateArgs, HeadArgs, TestHeadArgs),
        TestHead =.. [Functor| TestHeadArgs].


        Construct_extendedCoinductive_template(Functor, Arity, Template) :-
        functor(Template, Functor, Arity),
        Template =.. [Functor| Args],
        Construct_extendedCoinductive_templateArgs(Args).


        Construct_extendedCoinductive_templateArgs([]).

        Construct_extendedCoinductive_templateArgs([(+)| Args]) :-
        Construct_extendedCoinductive_templateArgs(Args).


        mapCoinductive_templateArgs([], [], []).

        mapCoinductive_templateArgs([(+)| TemplateArgs], [Arg| HeadArgs], [Arg| TestHeadArgs]) :-
        mapCoinductive_templateArgs(TemplateArgs, HeadArgs, TestHeadArgs).

        mapCoinductive_templateArgs([(-)| TemplateArgs], [_| HeadArgs], [_| TestHeadArgs]) :-
        mapCoinductive_templateArgs(TemplateArgs, HeadArgs, TestHeadArgs).



   //Compile_usesDirective(+list, +list, @objectIdentifier, +compilationContext)
        %
   //auxiliary predicate for compiling uses/2 directives

        Compile_usesDirective([Resource| Resources], Argument, Obj, Ctx) :-
        !,
        Check(ground, Resource),
        Compile_usesDirectiveResource(Resource, Obj, Ctx),
        Compile_usesDirective(Resources, Argument, Obj, Ctx).

        Compile_usesDirective([], _, _, _) :-
        !.

        Compile_usesDirective(_, Argument, _, _) :-
        throw(type_error(list, Argument)).


        Compile_usesDirectiveResource(op(Priority, Specifier, Operators), _, _) :-
        Check(operatorSpecification, op(Priority, Specifier, Operators)),
        !,
        Activate_entity_operators(Priority, Specifier, Operators, l).

        Compile_usesDirectiveResource(as(Original,Alias), Obj, Ctx) :-
        !,
        Compile_usesDirectiveResource(Original::Alias, Obj, Ctx).

        Compile_usesDirectiveResource(Original::Alias, Obj, Ctx) :-
        valid_predicateIndicator(Original, OriginalFunctor, OriginalArity),
        valid_predicateIndicator(Alias, AliasFunctor, AliasArity),
        !,
        (	OriginalArity =:= AliasArity ->
        Compile_usesDirective_predicateResource(OriginalFunctor, AliasFunctor, OriginalArity, Obj, Ctx)
        ;	throw(domain_error({OriginalArity}, AliasArity))
        ).

        Compile_usesDirectiveResource(Original::Alias, Obj, Ctx) :-
        valid_non_terminalIndicator(Original, OriginalFunctor, OriginalArity, ExtendedArity),
        valid_non_terminalIndicator(Alias, AliasFunctor, AliasArity, _),
        !,
        (	OriginalArity =:= AliasArity ->
        Compile_usesDirective_non_terminalResource(OriginalFunctor, AliasFunctor, OriginalArity, ExtendedArity, Obj, Ctx)
        ;	throw(domain_error({OriginalArity}, AliasArity))
        ).

        Compile_usesDirectiveResource(Pred, Obj, Ctx) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        Compile_usesDirective_predicateResource(Functor, Functor, Arity, Obj, Ctx).

        Compile_usesDirectiveResource(NonTerminal, Obj, Ctx) :-
        valid_non_terminalIndicator(NonTerminal, Functor, Arity, ExtArity),
        !,
        Compile_usesDirective_non_terminalResource(Functor, Functor, Arity, ExtArity, Obj, Ctx).

        Compile_usesDirectiveResource(Resource, _, _) :-
        throw(type_error(predicateIndicator, Resource)).


        Compile_usesDirective_predicateResource(OriginalFunctor, AliasFunctor, Arity, Obj, Ctx) :-
        functor(Original, OriginalFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        \+ pp_uses_non_terminal_(_, _, _, _, Alias, _),
        \+ pp_use_module_non_terminal_(_, _, _, _, Alias, _),
   //no clash with an earlier uses/2 or a use_module/2 directive non-terminal
        \+ pp_uses_predicate_(_, _, Alias, _),
        \+ pp_use_module_predicate_(_, _, Alias, _),
   //no clash with an earlier uses/2 or a use_module/2 directive predicate
        !,
   //unify arguments of TOriginal and TAlias
        Original =.. [_| Args],
        Alias =.. [_| Args],
   //allow for runtime use by adding a local definition that calls the remote definition
   //except when the remote is a built-in predicate in "user" with no alias being defined
   //or a built-in method that would clash with the local definition
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, Mode, _, Lines),
        (	Obj == user,
        OriginalFunctor == AliasFunctor,
        predicate_property(Original, builtIn) ->
   //no need for a local definition
        true
        ;	builtIn_method(Alias, _, _, _) ->
   //local definition would clash with a built-in method
        true
        ;	% safe to add local definition
        CompileAuxClauses([(Alias :- Obj::Original)])
        ),
   //ensure that this uses/2 directive is found when looking for senders of this message
        AddReferenced_object_message(Mode, Obj, Original, Alias, Alias),
        assertz(pp_uses_predicate_(Obj, Original, Alias, Lines)).

        Compile_usesDirective_predicateResource(_, AliasFunctor, Arity, _, _) :-
        throw(permission_error(modify, uses_object_predicate, AliasFunctor/Arity)).


        Compile_usesDirective_non_terminalResource(OriginalFunctor, AliasFunctor, Arity, ExtArity, Obj, Ctx) :-
        functor(Original, OriginalFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        functor(Pred, OriginalFunctor, ExtArity),
        functor(PredAlias, AliasFunctor, ExtArity),
        \+ pp_uses_non_terminal_(_, _, _, _,  PredAlias, _),
        \+ pp_use_module_non_terminal_(_, _, _, _, PredAlias, _),
   //no clash with an earlier uses/2 or a use_module/2 directive non-terminal
        \+ pp_uses_predicate_(_, _, PredAlias, _),
        \+ pp_use_module_predicate_(_, _, PredAlias, _),
   //no clash with an earlier uses/2 or a use_module/2 directive predicate
        !,
   //unify arguments of TOriginal and TAlias
        Original =.. [_| Args],
        Alias =.. [_| Args],
   //allow for runtime use by adding a local definition that calls the remote definition
        CompCtx_mode(NewCtx, compile(aux)),
        Compile_grammarRule((Alias --> Obj::Original), NewCtx),
   //ensure that the this uses/2 directive is found when looking for senders of this message
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, Mode, _, Lines),
        AddReferenced_object_message(Mode, Obj, Pred, PredAlias, PredAlias),
        assertz(pp_uses_non_terminal_(Obj, Original, Alias, Pred, PredAlias, Lines)).

        Compile_usesDirective_non_terminalResource(_, AliasFunctor, Arity, _, _, _) :-
        throw(permission_error(modify, uses_object_non_terminal, AliasFunctor//Arity)).



   //Compile_use_moduleDirective(+list, +list, +atom, +compilationContext)
        %
   //auxiliary predicate for compiling use_module/2 directives in objects or categories

        Compile_use_moduleDirective([Resource| Resources], Argument, Module, Ctx) :-
        !,
        Check(ground, Resource),
        Compile_use_moduleDirectiveResource(Resource, Module, Ctx),
        Compile_use_moduleDirective(Resources, Argument, Module, Ctx).

        Compile_use_moduleDirective([], _, _, _) :-
        !.

        Compile_use_moduleDirective(_, Argument, _, _) :-
        throw(type_error(list, Argument)).


        Compile_use_moduleDirectiveResource(op(Priority, Specifier, Operators), _, _) :-
        Check(operatorSpecification, op(Priority, Specifier, Operators)),
        !,
        Activate_entity_operators(Priority, Specifier, Operators, l).

        Compile_use_moduleDirectiveResource(as(Original, Alias), Module, Ctx) :-
        !,
        Compile_use_moduleDirectiveResource(':(Original, Alias), Module, Ctx).

        Compile_use_moduleDirectiveResource(':(Original, Alias), Module, Ctx) :-
        valid_predicateIndicator(Original, OriginalFunctor, OriginalArity),
        valid_predicateIndicator(Alias, AliasFunctor, AliasArity),
        !,
        (	OriginalArity =:= AliasArity ->
        Compile_use_moduleDirective_predicateResource(OriginalFunctor, AliasFunctor, OriginalArity, Module, Ctx)
        ;	throw(domain_error({OriginalArity}, AliasArity))
        ).

        Compile_use_moduleDirectiveResource(':(Original, Alias), Module, Ctx) :-
        valid_non_terminalIndicator(Original, OriginalFunctor, OriginalArity, ExtendedArity),
        valid_non_terminalIndicator(Alias, AliasFunctor, AliasArity, _),
        !,
        (	OriginalArity =:= AliasArity ->
        Compile_use_moduleDirective_non_terminalResource(OriginalFunctor, AliasFunctor, OriginalArity, ExtendedArity, Module, Ctx)
        ;	throw(domain_error({OriginalArity}, AliasArity))
        ).

        Compile_use_moduleDirectiveResource(Pred, Module, Ctx) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        Compile_use_moduleDirective_predicateResource(Functor, Functor, Arity, Module, Ctx).

        Compile_use_moduleDirectiveResource(NonTerminal, Module, Ctx) :-
        valid_non_terminalIndicator(NonTerminal, Functor, Arity, ExtArity),
        !,
        Compile_use_moduleDirective_non_terminalResource(Functor, Functor, Arity, ExtArity, Module, Ctx).

        Compile_use_moduleDirectiveResource(Resource, _, _) :-
        throw(type_error(predicateIndicator, Resource)).


        Compile_use_moduleDirective_predicateResource(OriginalFunctor, AliasFunctor, Arity, Module, Ctx) :-
        functor(Original, OriginalFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        \+ pp_uses_non_terminal_(_, _, _, _, Alias, _),
        \+ pp_use_module_non_terminal_(_, _, _, _, Alias, _),
   //no clash with an earlier uses/2 or a use_module/2 directive non-terminal
        \+ pp_uses_predicate_(_, _, Alias, _),
        \+ pp_use_module_predicate_(_, _, Alias, _),
   //no clash with an earlier uses/2 or a use_module/2 directive predicate
        !,
   //unify arguments of TOriginal and TAlias
        Original =.. [_| Args],
        Alias =.. [_| Args],
   //allow for runtime use by adding a local definition that calls the remote definition
   //except when the remote is a built-in predicate in "user" with no alias being defined
   //or a built-in method that would clash with the local definition
        (	Module == user,
        OriginalFunctor == AliasFunctor,
        predicate_property(Original, builtIn) ->
   //no need for a local definition
        true
        ;	builtIn_method(Alias, _, _, _) ->
   //local definition would clash with a built-in method
        true
        ;	% safe to add local definition
        CompileAuxClauses([(Alias :- ':(Module, Original))])
        ),
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, Mode, _, Lines),
   //ensure that this use_module/2 directive is found when looking for callers of this module predicate
        AddReferenced_module_predicate(Mode, Module, Original, Alias, Alias),
        assertz(pp_use_module_predicate_(Module, Original, Alias, Lines)).

        Compile_use_moduleDirective_predicateResource(_, AliasFunctor, Arity, _, _) :-
        throw(permission_error(modify, uses_module_predicate, AliasFunctor/Arity)).


        Compile_use_moduleDirective_non_terminalResource(OriginalFunctor, AliasFunctor, Arity, ExtArity, Module, Ctx) :-
        functor(Original, OriginalFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        functor(Pred, AliasFunctor, ExtArity),
        functor(PredAlias, AliasFunctor, ExtArity),
        \+ pp_uses_non_terminal_(_, _, _, _, PredAlias, _),
        \+ pp_use_module_non_terminal_(_, _, _, _, PredAlias, _),
   //no clash with an earlier uses/2 or a use_module/2 directive non-terminal
        \+ pp_uses_predicate_(_, _, PredAlias, _),
        \+ pp_use_module_predicate_(_, _, PredAlias, _),
   //no clash with an earlier uses/2 or a use_module/2 directive predicate
        !,
   //unify arguments of TOriginal and TAlias
        Original =.. [_| Args],
        Alias =.. [_| Args],
   //allow for runtime use by adding a local definition that calls the remote definition
        CompCtx_mode(NewCtx, compile(aux)),
        Compile_grammarRule((Alias --> ':(Module, Original)), NewCtx),
   //ensure that the this use_module/2 directive is found when looking for callers of this module non-terminal
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, Mode, _, Lines),
        AddReferenced_module_predicate(Mode, Module, Pred, PredAlias, PredAlias),
        assertz(pp_use_module_non_terminal_(Module, Original, Alias, Pred, PredAlias, Lines)).

        Compile_use_moduleDirective_non_terminalResource(_, AliasFunctor, Arity, _, _, _) :-
        throw(permission_error(modify, uses_module_non_terminal, AliasFunctor//Arity)).



   //CompileReexportDirective(+list, +atom, +compilationContext)
        %
   //auxiliary predicate for compiling module reexport/2 directives;
   //the predicate renaming operator as/2 found on SWI-Prolog and YAP
   //is also supported (iff we're compiling a module as an object)

        CompileReexportDirective([], _, _).

        CompileReexportDirective([Resource| Resources], Module, Ctx) :-
        CompileReexportDirectiveResource(Resource, Module, Ctx),
        CompileReexportDirective(Resources, Module, Ctx).


        CompileReexportDirectiveResource(op(Priority, Specifier, Operators), _, _) :-
        Check(operatorSpecification, op(Priority, Specifier, Operators)),
        !,
        Activate_entity_operators(Priority, Specifier, Operators, l).

        CompileReexportDirectiveResource(as(Pred, NewFunctor), Module, Ctx) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        atom(NewFunctor),
        !,
        CompileLogtalkDirective(public(NewFunctor/Arity), Ctx),
        functor(NewHead, NewFunctor, Arity),
        functor(Head, Functor, Arity),
        CompileClause((NewHead :- Module::Head), Ctx).

        CompileReexportDirectiveResource(as(NonTerminal, NewFunctor), Module, Ctx) :-
        valid_non_terminalIndicator(NonTerminal, Functor, Arity, _),
        atom(NewFunctor),
        !,
        CompileLogtalkDirective(public(NewFunctor//Arity), Ctx),
        functor(NewHead, NewFunctor, Arity),
        functor(Head, Functor, Arity),
        Compile_grammarRule((NewHead --> Module::Head), Ctx).

        CompileReexportDirectiveResource(Pred, Module, Ctx) :-
        valid_predicateIndicator(Pred, Functor, Arity),
        !,
        CompileLogtalkDirective(public(Pred), Ctx),
        functor(Head, Functor, Arity),
        CompileClause((Head :- Module::Head), Ctx).

        CompileReexportDirectiveResource(NonTerminal, Module, Ctx) :-
        valid_non_terminalIndicator(NonTerminal, Functor, Arity, _),
        !,
        CompileLogtalkDirective(public(NonTerminal), Ctx),
        functor(Head, Functor, Arity),
        Compile_grammarRule((Head --> Module::Head), Ctx).

        CompileReexportDirectiveResource(Resource, _, _) :-
        ground(Resource),
        throw(type_error(predicateIndicator, Resource)).

        CompileReexportDirectiveResource(_, _, _) :-
        throw(instantiation_error).



   //auxiliary predicate for compiling module's meta predicate directives
   //into Logtalk ones by translating the meta-argument specifiers

        Compile_module_meta_predicateDirective([Template| Templates], [ConvertedTemplate| ConvertedTemplates]) :-
        Check(callable, Template),
        Template =.. [Functor| Args],
        prolog_toLogtalk_metaArgumentSpecifiers(Args, ConvertedArgs),
        ConvertedTemplate =.. [Functor| ConvertedArgs],
        Compile_module_meta_predicateDirective(Templates, ConvertedTemplates).

        Compile_module_meta_predicateDirective([], []).



   //CheckForDuplicatedDirective(@callable, @callable)

        CheckForDuplicatedDirective(TDirective, Directive) :-
        (	ppDirective_(TDirective),
        CompilerFlag(duplicatedDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(duplicatedDirectives), core, duplicatedDirective(File, Lines, Type, Entity, Directive))
        ;	true
        ).



   //auxiliary predicate for translating Prolog dialect meta-argument
   //predicate specifiers into Logtalk specifiers

        prolog_toLogtalk_metaArgumentSpecifiers([], []).

        prolog_toLogtalk_metaArgumentSpecifiers([Arg| Args], [TArg| TArgs]) :-
        (	\+ ground(Arg) ->
        throw(instantiation_error)
        ;	prolog_toLogtalk_metaArgumentSpecifier_hook(Arg, TArg) ->
        true
        ;	prolog_toLogtalk_metaArgumentSpecifier(Arg, TArg) ->
        true
        ;	throw(domain_error(metaArgumentSpecifier, Arg))
        ),
        prolog_toLogtalk_metaArgumentSpecifiers(Args, TArgs).


   //goals and closures are denoted by integers >= 0
        prolog_toLogtalk_metaArgumentSpecifier(N, N) :-
        integer(N).
   //Prolog to Logtalk notation; this is fragile due to the lack of standardization
        prolog_toLogtalk_metaArgumentSpecifier((:), (::)).
   //mixed-up notation or overriding meta-predicate template being used
        prolog_toLogtalk_metaArgumentSpecifier((::), (::)).
   //predicate indicator
        prolog_toLogtalk_metaArgumentSpecifier((/), (/)).
   //non-terminal indicator
        prolog_toLogtalk_metaArgumentSpecifier((//), (//)).
   //list of goals/closures
        prolog_toLogtalk_metaArgumentSpecifier([N], [N]) :-
        integer(N).
   //list of predicate indicators
        prolog_toLogtalk_metaArgumentSpecifier([/], [/]).
   //list of non-terminal indicators
        prolog_toLogtalk_metaArgumentSpecifier([//], [//]).
   //goal with possible existential variables qualification
        prolog_toLogtalk_metaArgumentSpecifier((^), (^)).
   //instantiation modes (non meta-arguments)
        prolog_toLogtalk_metaArgumentSpecifier((@), (*)).
        prolog_toLogtalk_metaArgumentSpecifier((+), (*)).
        prolog_toLogtalk_metaArgumentSpecifier((-), (*)).
        prolog_toLogtalk_metaArgumentSpecifier((?), (*)).
   //non meta-arguments
        prolog_toLogtalk_metaArgumentSpecifier((*), (*)).



   //Compile_objectRelations(@list(term), @objectIdentifier, @compilationContext)
        %
   //compiles the relations of an object with other entities

        Compile_objectRelations([Relation| Relations], Obj, Ctx) :-
        (	var(Relation) ->
        throw(instantiation_error)
        ;	Compile_objectRelation(Relation, Obj, Ctx) ->
        true
        ;	callable(Relation) ->
        functor(Relation, Functor, Arity),
        throw(domain_error(objectRelation, Functor/Arity))
        ;	throw(type_error(callable, Relation))
        ),
        Compile_objectRelations(Relations, Obj, Ctx).

        Compile_objectRelations([], _, _).



   //Compile_objectRelation(@nonvar, @objectIdentifier, @compilationContext)
        %
   //compiles a relation between an object (the last argument) with other entities

        Compile_objectRelation(implements(_), _, _) :-
        ppImplemented_protocol_(_, _, _, _, _),
        throw(permission_error(repeat, entityRelation, implements/1)).

        Compile_objectRelation(implements(Ptcs), Obj, Ctx) :-
        Flatten_toList(Ptcs, FlattenedPtcs),
        CompileImplements_protocolRelation(FlattenedPtcs, Obj, Ctx).


        Compile_objectRelation(imports(_), _, _) :-
        ppImportedCategory_(_, _, _, _, _, _),
        throw(permission_error(repeat, entityRelation, imports/1)).

        Compile_objectRelation(imports(Ctgs), Obj, Ctx) :-
        Flatten_toList(Ctgs, FlattenedCtgs),
        CompileImportsCategoryRelation(FlattenedCtgs, Obj, Ctx).


        Compile_objectRelation(instantiates(_), _, _) :-
        ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _),
        throw(permission_error(repeat, entityRelation, instantiates/1)).

        Compile_objectRelation(instantiates(Classes), Instance, Ctx) :-
        Flatten_toList(Classes, FlattenedClasses),
        CompileInstantiatesClassRelation(FlattenedClasses, Instance, Ctx).


        Compile_objectRelation(specializes(_), _, _) :-
        ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _),
        throw(permission_error(repeat, entityRelation, specializes/1)).

        Compile_objectRelation(specializes(Superclasses), Class, Ctx) :-
        Flatten_toList(Superclasses, FlattenedSuperclasses),
        CompileSpecializesClassRelation(FlattenedSuperclasses, Class, Ctx).


        Compile_objectRelation(extends(_), _, _) :-
        pp_extended_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(permission_error(repeat, entityRelation, extends/1)).

        Compile_objectRelation(extends(Parents), Prototype, Ctx) :-
        Flatten_toList(Parents, FlattenedParents),
        Compile_extends_objectRelation(FlattenedParents, Prototype, Ctx).



   //Compile_protocolRelations(@list(term), @protocolIdentifier, @compilationContext)
        %
   //compiles the relations of a protocol with other entities

        Compile_protocolRelations([Relation| Relations], Ptc, Ctx) :-
        (	var(Relation) ->
        throw(instantiation_error)
        ;	Compile_protocolRelation(Relation, Ptc, Ctx) ->
        true
        ;	callable(Relation) ->
        functor(Relation, Functor, Arity),
        throw(domain_error(protocolRelation, Functor/Arity))
        ;	throw(type_error(callable, Relation))
        ),
        Compile_protocolRelations(Relations, Ptc, Ctx).

        Compile_protocolRelations([], _, _).



   //Compile_protocolRelation(@nonvar, @protocolIdentifier, @compilationContext)
        %
   //compiles a relation between a protocol (the last argument) with other entities

        Compile_protocolRelation(extends(_), _, _) :-
        pp_extended_protocol_(_, _, _, _, _),
        throw(permission_error(repeat, entityRelation, extends/1)).

        Compile_protocolRelation(extends(Ptcs), Ptc, Ctx) :-
        Flatten_toList(Ptcs, FlattenedPtcs),
        Compile_extends_protocolRelation(FlattenedPtcs, Ptc, Ctx).



   //CompileCategoryRelations(@list(term), @categoryIdentifier, @compilationContext)
        %
   //compiles the relations of a category with other entities

        CompileCategoryRelations([Relation| Relations], Ctg, Ctx) :-
        (	var(Relation) ->
        throw(instantiation_error)
        ;	CompileCategoryRelation(Relation, Ctg, Ctx) ->
        true
        ;	callable(Relation) ->
        functor(Relation, Functor, Arity),
        throw(domain_error(categoryRelation, Functor/Arity))
        ;	throw(type_error(callable, Relation))
        ),
        CompileCategoryRelations(Relations, Ctg, Ctx).

        CompileCategoryRelations([], _, _).



   //CompileCategoryRelation(@nonvar, @categoryIdentifier, @compilationContext)
        %
   //compiles a relation between a category (the last argument) with other entities

        CompileCategoryRelation(implements(_), _, _) :-
        ppImplemented_protocol_(_, _, _, _, _),
        throw(permission_error(repeat, entityRelation, implements/1)).

        CompileCategoryRelation(implements(Ptcs), Ctg, Ctx) :-
        Flatten_toList(Ptcs, FlattenedPtcs),
        CompileImplements_protocolRelation(FlattenedPtcs, Ctg, Ctx).


        CompileCategoryRelation(extends(_), _, _) :-
        pp_extendedCategory_(_, _, _, _, _, _),
        throw(permission_error(repeat, entityRelation, extends/1)).

        CompileCategoryRelation(extends(Ctgs), Ctg, Ctx) :-
        Flatten_toList(Ctgs, FlattenedCtgs),
        Compile_extendsCategoryRelation(FlattenedCtgs, Ctg, Ctx).


        CompileCategoryRelation(complements(_), _, _) :-
        ppComplemented_object_(_, _, _, _, _),
        throw(permission_error(repeat, entityRelation, complements/1)).

        CompileCategoryRelation(complements(Objs), Ctg, Ctx) :-
        Flatten_toList(Objs, FlattenedObjs),
        CompileComplements_objectRelation(FlattenedObjs, Ctg, Ctx).



   //Compile_entityInfoDirective(@list(term), -list(pair))
        %
   //compiles the entity info/1 directive key-value pairs

        Compile_entityInfoDirective([Pair| Pairs], [TPair| TPairs]) :-
        (	validInfo_key_value_pair(Pair, Key, Value) ->
        Compile_entityInfoDirective_pair(Key, Value, TPair),
        Compile_entityInfoDirective(Pairs, TPairs)
        ;	% non-valid pair; generate an error
        Check(key_valueInfo_pair, Pair)
        ).

        Compile_entityInfoDirective([], []).



   //Compile_entityInfoDirective_pair(+atom, @nonvar, -compound)
        %
   //compiles an entity info/1 directive key-value pair

        Compile_entityInfoDirective_pair(author, Author, author(Author)) :-
        !,
        (	Author = {EntityName}, atom(EntityName) ->
        true
        ;	Check(atom_orString, Author)
        ).

        Compile_entityInfoDirective_pair(comment, Comment, comment(Comment)) :-
        !,
        Check(atom_orString, Comment).

        Compile_entityInfoDirective_pair(date, Date, date(Date)) :-
        !,
        (	Date = Year/Month/Day ->
        Check(integer, Year),
        Check(integer, Month),
        Check(integer, Day)
        ;	throw(type_error(date, Date))
        ).

        Compile_entityInfoDirective_pair(parameters, Parameters, parameters(Parameters)) :-
        !,
        pp_entity_(_, Entity, _, _, _),
        functor(Entity, _, Arity),
        Check_entityInfo_parameters(Parameters, Parameters, 0, Arity).

        Compile_entityInfoDirective_pair(parnames, Parnames, parnames(Parnames)) :-
        !,
        pp_entity_(_, Entity, _, _, _),
        functor(Entity, _, Arity),
        Check_entityInfo_parnames(Parnames, Parnames, 0, Arity).

        Compile_entityInfoDirective_pair(remarks, Remarks, remarks(Remarks)) :-
        !,
        Check(list, Remarks),
        (	member(Remark, Remarks), \+ validRemark(Remark) ->
        throw(type_error(remark, Remark))
        ;	true
        ).

        Compile_entityInfoDirective_pair(seeAlso, References, seeAlso(References)) :-
        !,
        Check(list(entityIdentifier), References).

        Compile_entityInfoDirective_pair(version, Version, version(Version)) :-
        !,
        Check(atomic_orString, Version).

        Compile_entityInfoDirective_pair(copyright, Copyright, copyright(Copyright)) :-
        !,
        (	Copyright = {EntityName}, atom(EntityName) ->
        true
        ;	Check(atom_orString, Copyright)
        ).

        Compile_entityInfoDirective_pair(license, License, license(License)) :-
        !,
        (	License = {EntityName}, atom(EntityName) ->
        true
        ;	Check(atom_orString, License)
        ).

   //user-defined entity info pair; no checking
        Compile_entityInfoDirective_pair(Key, Value, TPair) :-
        TPair =.. [Key, Value].


        Check_entityInfo_parameters([Pair| Pairs], Parameters, Counter0, Arity) :-
        !,
        (	Pair = Name - Description ->
        Check(atom_orString, Name),
        Check(atom_orString, Description),
        Counter1 is Counter0 + 1,
        Check_entityInfo_parameters(Pairs, Parameters, Counter1, Arity)
        ;	throw(type_error(pair, Pair))
        ).

        Check_entityInfo_parameters([], _, Counter, Arity) :-
        !,
        (	Counter =:= Arity ->
        true
        ;	throw(domain_error({Arity}, Counter))
        ).

        Check_entityInfo_parameters(_, Parameters, _, _) :-
        throw(type_error(list, Parameters)).


        Check_entityInfo_parnames([Name| Names], Parnames, Counter0, Arity) :-
        !,
        Check(atom_orString, Name),
        Counter1 is Counter0 + 1,
        Check_entityInfo_parnames(Names, Parnames, Counter1, Arity).

        Check_entityInfo_parnames([], _, Counter, Arity) :-
        !,
        (	Counter =:= Arity ->
        true
        ;	throw(domain_error({Arity}, Counter))
        ).

        Check_entityInfo_parnames(_, Parnames, _, _) :-
        throw(type_error(list, Parnames)).



   //Compile_predicateInfoDirective(@list(term), +atom, +integer, -list(pair))
        %
   //compiles the predicate info/2 directive key-value pairs

        Compile_predicateInfoDirective([Pair| Pairs], Functor, Arity, [TPair| TPairs]) :-
        (	validInfo_key_value_pair(Pair, Key, Value) ->
        Compile_predicateInfoDirective_pair(Key, Value, Functor, Arity, TPair),
        Compile_predicateInfoDirective(Pairs, Functor, Arity, TPairs)
        ;	% non-valid pair; generate an error
        Check(key_valueInfo_pair, Pair)
        ).

        Compile_predicateInfoDirective([], _, _, []).



   //Compile_predicateInfoDirective_pair(+atom, @nonvar, +atom, +integer, -compound)
        %
   //compiles a predicate info/2 directive key-value pair

        Compile_predicateInfoDirective_pair(allocation, Allocation, _, _, allocation(Allocation)) :-
        !,
        Check(atom, Allocation),
        (	valid_predicateAllocation(Allocation) ->
        true
        ;	throw(domain_error(allocation, Allocation))
        ).

        Compile_predicateInfoDirective_pair(arguments, Arguments, _, Arity, arguments(Arguments)) :-
        !,
        Check_predicateInfoArguments(Arguments, Arguments, 0, Arity).

        Compile_predicateInfoDirective_pair(argnames, Argnames, _, Arity, argnames(Argnames)) :-
        !,
        Check_predicateInfoArgnames(Argnames, Argnames, 0, Arity).

        Compile_predicateInfoDirective_pair(comment, Comment, _, _, comment(Comment)) :-
        !,
        Check(atom_orString, Comment).

        Compile_predicateInfoDirective_pair(exceptions, Exceptions, _, _, exceptions(Exceptions)) :-
        !,
        Check(list, Exceptions),
        (	member(Exception, Exceptions), \+ valid_predicate_exception(Exception) ->
        throw(type_error(exception, Exception))
        ;	true
        ).

        Compile_predicateInfoDirective_pair(remarks, Remarks, _, _, remarks(Remarks)) :-
        !,
        Check(list, Remarks),
        (	member(Remark, Remarks), \+ validRemark(Remark) ->
        throw(type_error(remark, Remark))
        ;	true
        ).

        Compile_predicateInfoDirective_pair(examples, Examples, Functor, Arity, examples(Examples)) :-
        !,
        Check(list, Examples),
        (	member(Example, Examples), \+ valid_predicateCall_example(Example, Functor, Arity) ->
        throw(type_error(example, Example))
        ;	true
        ).

        Compile_predicateInfoDirective_pair(redefinition, Redefinition, _, _, redefinition(Redefinition)) :-
        !,
        Check(atom, Redefinition),
        (	valid_predicateRedefinition(Redefinition) ->
        true
        ;	throw(domain_error(redefinition, Redefinition))
        ).

   //user-defined predicate info pair; no checking
        Compile_predicateInfoDirective_pair(Key, Value, _, _, TPair) :-
        TPair =.. [Key, Value].


        Check_predicateInfoArguments([Pair| Pairs], Arguments, Counter0, Arity) :-
        !,
        (	Pair = Name - Description ->
        Check(atom_orString, Name),
        Check(atom_orString, Description),
        Counter1 is Counter0 + 1,
        Check_predicateInfoArguments(Pairs, Arguments, Counter1, Arity)
        ;	throw(type_error(pair, Pair))
        ).

        Check_predicateInfoArguments([], _, Counter, Arity) :-
        !,
        (	Counter =:= Arity ->
        true
        ;	throw(domain_error({Arity}, Counter))
        ).

        Check_predicateInfoArguments(_, Arguments, _, _) :-
        throw(type_error(list, Arguments)).


        Check_predicateInfoArgnames([Name| Names], Arguments, Counter0, Arity) :-
        !,
        Check(atom_orString, Name),
        Counter1 is Counter0 + 1,
        Check_predicateInfoArgnames(Names, Arguments, Counter1, Arity).

        Check_predicateInfoArgnames([], _, Counter, Arity) :-
        !,
        (	Counter =:= Arity ->
        true
        ;	throw(domain_error({Arity}, Counter))
        ).

        Check_predicateInfoArgnames(_, Arguments, _, _) :-
        throw(type_error(list, Arguments)).



   //Compile_grammarRules(+list, +compilationContext)

        Compile_grammarRules([GrammarRule| GrammarRules], Ctx) :-
        Compile_grammarRule(GrammarRule, Ctx),
        Compile_grammarRules(GrammarRules, Ctx).

        Compile_grammarRules([], _).



   //Compile_grammarRule(+grammarRule, +compilationContext)

        Compile_grammarRule(GrammarRule, Ctx) :-
        catch(
        DcgRule(GrammarRule, Clause, Ctx),
        Error,
        throw(error(Error, grammarRule(GrammarRule)))
        ),
        CompileClause(Clause, Ctx).



   //CompileClauses(+list, +compilationContext)

        CompileClauses([Clause| Clauses], Ctx) :-
        CompileClause(Clause, Ctx),
        CompileClauses(Clauses, Ctx).

        CompileClauses([], _).



   //CompileClause(+clause, +compilationContext)
        %
   //compiles a source file clause

        CompileClause(Clause, Ctx) :-
        Check(clause, Clause, clause(Clause)),
        pp_entity_(Type, Entity, Prefix, _, _),
   //compiling an entity clause
        (	Type == protocol ->
   //protocols cannot contain predicate definitions
        throw(error(permission_error(define, clause, Entity), clause(Clause)))
        ;	true
        ),
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, _, Mode, _, Lines),
        catch(
        CompileClause(Clause, Entity, TClause, DClause, Ctx),
        Error,
        throw(error(Error, clause(Clause)))
        ),
   //sucessful first stage compilation; save the source data information for
   //use in the second compiler stage (where it might be required by calls to
   //the logtalkLoadContext/2 predicate during goal expansion)
        (	pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines) ->
        SourceData = sd(Term, VariableNames, File, Lines)
        ;	SourceData = nil
        ),
   //check which compile clause to save (normal/optimal and debug) and
   //if we have a clause defined by the user or an auxiliary clause
        (	CompilerFlag(debug, on) ->
        (	Mode == compile(aux) ->
        assertz(pp_entityAuxClause_(DClause))
        ;	assertz(pp_entity_term_(DClause, SourceData, Lines))
        )
        ;	(	Mode == compile(aux) ->
        assertz(pp_entityAuxClause_(TClause))
        ;	assertz(pp_entity_term_(TClause, SourceData, Lines))
        )
        ),
        !.

        CompileClause(Clause, _) :-
        \+ pp_entity_(_, _, _, _, _),
   //clause occurs before an opening entity directive
        !,
   //save the source data information for use in the second compiler stage
   //(where it might be required by calls to the logtalkLoadContext/2
   //predicate during goal expansion)
        (	pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines) ->
        SourceData = sd(Term, VariableNames, File, Lines)
        ;	SourceData = nil, Lines = '-(-1, -1)
        ),
   //copy the clause unchanged to the generated Prolog file
        assertz(pp_prolog_term_(Clause, SourceData, Lines)).

        CompileClause(Clause, _) :-
   //deal with unexpected clause translation failures
        throw(error(system_error, clause(Clause))).



   //CompileClause(+clause, +entityIdentifier, -clause, -clause, +compilationContext)
        %
   //compiles an entity clause into a normal clause and a debug clause
        %
   //in this first compiler stage only the clause heads are compiled, which
   //allows collecting information about all entity defined predicates; the
   //compilation of clause bodies is delayed to the compiler second stage to
   //take advantage of the collected information to notably simplify handling
   //of redefined built-in predicates
        %
   //in the case of a clause rule for a multifile predicate, the clause body
   //is compiled in the context of the entity defining the clause; still, any
   //calls to the parameter/2 method in the clause body will access parameters
   //for the defining entity; parameters for the entity for which the clause
   //is defined can be accessed through simple unification at the clause head

        CompileClause((Head:-Body), Entity, TClause, DClause, Ctx) :-
        !,
        head_meta_variables(Head, MetaVars),
        CompCtx(Ctx, Head, ExCtx, _, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, Mode, Stack, _),
        SourceFileContext(Ctx, File, BeginLine-EndLine),
        Compile_head(Head, PI, THead, Ctx),
        (	Head = {UserHead} ->
   //clause for a multifile predicate in "user"
        DHead = Debug(rule(Entity, user::UserHead, N, File, BeginLine), ExCtx),
        CompCtx(BodyCtx, Head, ExCtx, _, _, _, _, Prefix, _, _, BodyExCtx, Mode, _, BeginLine-EndLine),
        executionContext_this_entity(ExCtx, _, user),
        (	pp_object_(_, _, _, _, _, _, _, _, _, _, _) ->
   //ensure that ::/1-2 and ^^/2 calls are compiled in the correct context
        executionContext(BodyExCtx, Entity, Entity, Entity, Entity, [], [])
        ;	executionContext(BodyExCtx, Entity, Sender, This, Self, MetaCallCtx, Stack)
        )
        ;	Head = Other::_ ->
   //clause for an object or category multifile predicate
        DHead = Debug(rule(Entity, Head, N, File, BeginLine), ExCtx),
        CompCtx(BodyCtx, Head, ExCtx, _, _, _, _, Prefix, _, _, BodyExCtx, Mode, _, BeginLine-EndLine),
        term_variables(Other, OtherVars),
        term_variables(Head, HeadVars),
        Intersection(OtherVars, HeadVars, CommonVars),
        (	CommonVars == [] ->
        true
        ;	% parametric entity sharing variables with the clause head
        executionContext_this_entity(ExCtx, _, Other)
        ),
        (	pp_object_(_, _, _, _, _, _, _, _, _, _, _) ->
   //ensure that ::/1-2 and ^^/2 calls are compiled in the correct context
        executionContext(BodyExCtx, Entity, Entity, Entity, Entity, [], [])
        ;	executionContext(BodyExCtx, Entity, Sender, This, Self, MetaCallCtx, Stack)
        )
        ;	Head = ':(_, _) ->
   //clause for a module multifile predicate
        DHead = Debug(rule(Entity, Head, N, File, BeginLine), ExCtx),
        CompCtx(BodyCtx, Head, ExCtx, _, _, _, _, Prefix, _, _, BodyExCtx, Mode, _, BeginLine-EndLine),
        (	pp_object_(_, _, _, _, _, _, _, _, _, _, _) ->
   //ensure that ::/1-2 and ^^/2 calls are compiled in the correct context
        executionContext(BodyExCtx, Entity, Entity, Entity, Entity, [], [])
        ;	executionContext(BodyExCtx, Entity, Sender, This, Self, MetaCallCtx, Stack)
        )
        ;	% clause for a local predicate
        DHead = Debug(rule(Entity, Head, N, File, BeginLine), ExCtx),
        BodyCtx = Ctx
        ),
        (	ppDynamic_(Head) ->
        TClause = drule(THead, nop(Body), Body, BodyCtx),
        DClause = ddrule(THead, nop(Body), DHead, Body, BodyCtx)
        ;	TClause = srule(THead, Body, BodyCtx),
        DClause = dsrule(THead, DHead, Body, BodyCtx)
        ),
        Clause_number(PI, rule, File, BeginLine, N).

        CompileClause(Fact, Entity, fact(TFact), dfact(TFact,DHead), Ctx) :-
        Compile_head(Fact, PI, TFact, Ctx),
        CompCtx_execCtx(Ctx, ExCtx),
        SourceFileContext(Ctx, File, BeginLine-_),
        (	Fact = {UserFact} ->
   //fact for a multifile predicate in "user"
        DHead = Debug(fact(Entity, user::UserFact, N, File, BeginLine), ExCtx)
        ;	Fact = Other::_ ->
   //fact for an entity multifile predicate
        DHead = Debug(fact(Entity, Fact, N, File, BeginLine), ExCtx),
        term_variables(Other, OtherVars),
        term_variables(Fact, FactVars),
        Intersection(OtherVars, FactVars, CommonVars),
        (	CommonVars == [] ->
        true
        ;	% parametric entity sharing variables with the fact
        CompCtx(Ctx, _, _, Other, _, _, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, _, Other)
        )
        ;	Fact = ':(_, _) ->
   //fact for a module multifile predicate
        DHead = Debug(fact(Entity, Fact, N, File, BeginLine), ExCtx)
        ;	% other facts
        (	var(ExCtx) ->
        true
        ;	unify_head_theadArguments(Fact, TFact, ExCtx)
        ),
        DHead = Debug(fact(Entity, Fact, N, File, BeginLine), ExCtx)
        ),
        Clause_number(PI, fact, File, BeginLine, N).



   //Clause_number(@callable, +atom, +atom, +integer, -integer)
        %
   //returns the clause number for a compiled predicate; when the clause is the
   //first one for the predicate, we also save the definition line in the source
   //file (assuming that we're not compiling a clause for a dynamically created
   //entity) for use with the reflection built-in predicates and methods

        Clause_number(Other::Functor/Arity, fact, File, Line, Clauses) :-
        !,
   //object or category multifile predicate
        (	retract(pp_number_ofClausesRules_(Other, Functor, Arity, Clauses0, Rules)) ->
        Clauses is Clauses0 + 1
        ;	% first clause found for this predicate
        Clauses = 1,
        Rules = 0,
        assertz(pp_predicateDefinitionLocation_(Other, Functor, Arity, File, Line))
        ),
        assertz(pp_number_ofClausesRules_(Other, Functor, Arity, Clauses, Rules)).

        Clause_number(Other::Functor/Arity, rule, File, Line, Clauses) :-
   //object or category multifile predicate
        (	retract(pp_number_ofClausesRules_(Other, Functor, Arity, Clauses0, Rules0)) ->
        Clauses is Clauses0 + 1,
        Rules is Rules0 + 1
        ;	% first clause found for this predicate
        Clauses = 1,
        Rules = 1,
        assertz(pp_predicateDefinitionLocation_(Other, Functor, Arity, File, Line))
        ),
        assertz(pp_number_ofClausesRules_(Other, Functor, Arity, Clauses, Rules)).

   //module multifile predicate clause
        Clause_number(':(_, _), _, _, _, 0).

        Clause_number({Head}, Kind, File, Line, Clauses) :-
   //pre-compiled predicate clause head
        Clause_number(user::Head, Kind, File, Line, Clauses).

        Clause_number(Functor/Arity, fact, File, Line, Clauses) :-
        !,
   //predicate clause for the entity being compiled
        (	retract(pp_number_ofClausesRules_(Functor, Arity, Clauses0, Rules)) ->
        Clauses is Clauses0 + 1
        ;	% first clause found for this predicate
        Clauses = 1,
        Rules = 0,
        assertz(pp_predicateDefinitionLocation_(Functor, Arity, File, Line))
        ),
        assertz(pp_number_ofClausesRules_(Functor, Arity, Clauses, Rules)).

        Clause_number(Functor/Arity, rule, File, Line, Clauses) :-
   //predicate clause for the entity being compiled
        (	retract(pp_number_ofClausesRules_(Functor, Arity, Clauses0, Rules0)) ->
        Clauses is Clauses0 + 1,
        Rules is Rules0 + 1
        ;	% first clause found for this predicate
        Clauses = 1,
        Rules = 1,
        assertz(pp_predicateDefinitionLocation_(Functor, Arity, File, Line))
        ),
        assertz(pp_number_ofClausesRules_(Functor, Arity, Clauses, Rules)).



   //Compile_head(+callable, -callable, -callable, +compilationContext)
        %
   //compiles an entity clause head; also returns a term constructed from the
   //head predicate indicator to be used as key to compute the clause number


   //pre-compiled clause head (we only check for basic instantiation and entityKind errors)

        Compile_head({Head}, {Functor/Arity}, Head, _) :-
        !,
        Check(callable, Head),
        functor(Head, Functor, Arity).

   //not the first clause for this predicate; reuse the compiled head template

        Compile_head(Head, Functor/Arity, THead, Ctx) :-
        ppDefines_predicate_(Head, Functor/Arity, ExCtx, THead, Mode, Origin),
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        !,
   //only check for a discontiguous predicate for user-defined predicates
        (	Origin == aux ->
        true
        ;	pp_previous_predicate_(Head, Origin) ->
        true
        ;	% clauses for the predicate are discontiguous
        CheckDiscontiguousDirective(Head, Ctx)
        ).

   //definition of dynamic predicates inside categories is not allowed

        Compile_head(Head, _, _, _) :-
        ppCategory_(_, _, _, _, _, _),
        ppDynamic_(Head),
        functor(Head, Functor, Arity),
        throw(permission_error(define, dynamic_predicate, Functor/Arity)).

   //redefinition of Logtalk built-in methods is not allowed

        Compile_head(Head, _, _, _) :-
        builtIn_method(Head, _, _, Flags),
        Head \= _::_,
        Head \= ':(_, _),
   //not a clause for a multifile predicate
        Flags /\ 2 =\= 2,
   //not a (user defined) dynamic built-in predicate
        functor(Head, Functor, Arity),
        throw(permission_error(modify, builtIn_method, Functor/Arity)).

   //conflict with a predicate specified in a uses/2 directive

        Compile_head(Alias, _, _, _) :-
        pp_uses_predicate_(_, _, Alias, _),
        functor(Alias, Functor, Arity),
        throw(permission_error(modify, uses_object_predicate, Functor/Arity)).

   //conflict with a predicate specified in a use_module/2 directive

        Compile_head(Alias, _, _, _) :-
        pp_use_module_predicate_(_, _, Alias, _),
        functor(Alias, Functor, Arity),
        throw(permission_error(modify, uses_module_predicate, Functor/Arity)).

   //definition of a reserved predicate without reference to the built-in protocol declaring it

        Compile_head(Head, _, _, Ctx) :-
        Reserved_predicate_protocol(Head, Protocol),
        CompCtx_mode(Ctx, compile(_)),
        \+ pp_module_(_),
        \+ ppImplemented_protocol_(Protocol, _, _, _, _),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(general), core, missingReference_to_builtIn_protocol(File, Lines, Type, Entity, Protocol)),
        fail.

   //compile the head of a clause of another entity predicate (which we check if declared multifile)

        Compile_head(Other::Head, _, _, _) :-
        Check(entityIdentifier, Other),
        Check(callable, Head),
        fail.

        Compile_head(user::Head, user::Functor/Arity, Head, Ctx) :-
        !,
        functor(Head, Functor, Arity),
        (	ppDirective_(multifile(Functor/Arity)) ->
        true
        ;	CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(missingDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(missingDirectives), core, missing_predicateDirective(File, Lines, Type, Entity, (multifile), user::Functor/Arity))
        ;	true
        ),
        CompCtx_head(Ctx, user::Head).

        Compile_head(logtalk::debug_handler_provider(_), _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(_)),
        '$logtalk#0.debug_handler_provider#1(Provider, _),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(general), core, debug_handler_providerAlready_exists(File, Lines, Type, Entity, Provider)),
        fail.

        Compile_head(Other::Head, Other::Functor/Arity, THead, Ctx) :-
        !,
        functor(Head, Functor, Arity),
        entity_to_prefix(Other, Prefix),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        (	ppDirective_(multifile(TFunctor/TArity)) ->
        true
        ;	throw(existence_error(directive, multifile(Other::Functor/Arity)))
        ),
        functor(THead, TFunctor, TArity),
        unify_head_theadArguments(Head, THead, ExCtx),
        CompCtx_execCtx(Ctx, ExCtx),
        CompCtx_head(Ctx, Other::Head).

   //compile the head of a clause of a module predicate (which we check if declared multifile)

        Compile_head(':(Module, Head), ':(Module, Functor/Arity), THead, Ctx) :-
        !,
        Check(callable, Head),
        functor(Head, Functor, Arity),
        (	Module == user ->
        THead = Head
        ;	Check(moduleIdentifier, Module),
        THead = ':(Module, Head)
        ),
        (	Module == user, ppDirective_(multifile(Functor/Arity)) ->
        true
        ;	ppDirective_(multifile(':(Module, Functor/Arity))) ->
        true
        ;	CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(missingDirectives, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(missingDirectives), core, missing_predicateDirective(File, Lines, Type, Entity, (multifile), ':(Module,Functor/Arity)))
        ;	true
        ),
        CompCtx_head(Ctx, ':(Module, Head)).

   //compile the head of a clause of a user defined predicate

        Compile_head(Head, Functor/Arity, THead, Ctx) :-
   //first clause for this predicate
        functor(Head, Functor, Arity),
        (	ppDynamic_(Head),
        \+ pp_public_(Functor, Arity),
        \+ pp_protected_(Functor, Arity),
        \+ pp_private_(Functor, Arity) ->
   //dynamic predicate without a scope directive; can be abolished if declared
   //in an object and the abolish message sender is the object itself
        AddDdefClause(Head, Functor, Arity, THead, Ctx)
        ;	% static predicate and/or scoped dynamic predicate; cannot be abolished
        AddDefClause(Head, Functor, Arity, THead, Ctx)
        ).



   //Compile_body(@term, -callable, -callable, +compilationContext)
        %
   //compiles an entity clause body


   //runtime resolved meta-calls

        Compile_body(Pred, TPred, Debug(goal(Pred, TPred), ExCtx), Ctx) :-
        var(Pred),
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        CheckFor_meta_predicateDirective(Mode, Head, Pred),
        TPred = metacall(Pred, ExCtx).

   //compiler bypass control construct (opaque to cuts)

        %Compile_body({Pred}, _, _, Ctx) :-
        %	callable(Pred),
        %	CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, compile(_), _, _),
        %	CompilerFlag(suspiciousCalls, warning),
        %	IsoSpec_predicate(Pred),
        %	\+ builtIn_method(Pred, _, _, _),
        %	% not a Logtalk built-in method that have a Prolog counterpart
        %	\+ ControlConstruct(Pred),
        %	\+ ppDefines_predicate_(Pred, _, _, _, _, _),
        %	% call to a standard Prolog predicate that is not being locally redefined
   //	IncrementCompiling_warningsCounter',
   //	SourceFileContext(File, Lines, Type, Entity),
        %	print_message(warning(suspiciousCalls), core, suspiciousCall(File, Lines, Type, Entity, {Pred}, Pred)),
        %	fail.

        Compile_body({Pred}, TPred, Debug(goal({Pred}, TPred), ExCtx), Ctx) :-
        !,
        Check(var_orCallable, Pred),
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        (	var(Pred) ->
        TPred = call(Pred),
        CheckFor_meta_predicateDirective(Mode, Head, Pred)
        ;	Pred == ! ->
        TPred = true
        ;	Cut_transparentControlConstruct(Pred) ->
   //we need to keep the call/1 wrapper to preserve {}/1 cut-opaque semantics
        TPred = call(Pred)
        ;	TPred = Pred
        ).

   //goal expansion (only applied at compile time)

        Compile_body(Pred, TPred, DPred, Ctx) :-
        CompCtx_mode(Ctx, compile(_)),
        expandFile_goal(Pred, ExpandedPred),
        Pred \== ExpandedPred,
        !,
        Compile_body(ExpandedPred, TPred, DPred, Ctx).

   //message delegation (send a message while preserving the original sender)

        Compile_body([Goal], _, _, _) :-
        Check(callable, Goal),
        \+ functor(Goal, (::), 2),
        throw(domain_error(messageSending_goal, Goal)).

        Compile_body([Obj::Pred], TPred, Debug(goal([Obj::Pred], TPred), ExCtx), Ctx) :-
        !,
   //as delegation keeps the original sender, we cannot use a recursive call
   //to the Compile_body'/4 predicate to compile the ::/2 goal as that
   //would reset the sender to "this"
        CompCtx(Ctx, Head, _, _, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, Mode, Stack, Lines),
        executionContext(ExCtx, _, Sender, This, _, _, _),
        CompCtx(NewCtx, Head, _, _, Sender, Sender, Self, Prefix, MetaVars, MetaCallCtx, NewExCtx, Mode, Stack, Lines),
        executionContext_this_entity(NewExCtx, Sender, _),
        CompilerFlag(events, Events),
        Compile_message_to_object(Pred, Obj, TPred0, Events, NewCtx),
   //ensure that this control construct cannot be used to break object encapsulation
        TPred = (Obj \= Sender -> TPred0; throw(error(permission_error(access,object,Sender), logtalk([Obj::Pred],ExCtx)))).

   //existential quantifier outside bagof/3 and setof/3 calls

        Compile_body(_^_, _, _, _) :-
   //in some unusual cases, the user may be defining a (^)/2 predicate ...
        \+ ppDefines_predicate_(_^_, _, _, _, _, _),
   //... but otherwise (^)/2 cannot be used outside bagof/3 and setof/3 calls
        throw(existence_error(procedure, (^)/2)).

   //control constructs

        Compile_body((Pred1, Pred2), (TPred1, TPred2), (DPred1, DPred2), Ctx) :-
        !,
        Compile_body(Pred1, TPred1, DPred1, Ctx),
        Compile_body(Pred2, TPred2, DPred2, Ctx).

        Compile_body((IfThen; Else), (TIf -> TThen; TElse), (DIf -> DThen; DElse), Ctx) :-
        nonvar(IfThen),
        IfThen = (If -> Then),
        !,
        Compile_body(If, TIf, DIf, Ctx),
        Compile_body(Then, TThen, DThen, Ctx),
        Compile_body(Else, TElse, DElse, Ctx).

        Compile_body((IfThen; Else), ('*->(TIf, TThen); TElse), ('*->(DIf, DThen); DElse), Ctx) :-
        nonvar(IfThen),
        IfThen = '*->(If, Then),
        predicate_property('*->(_, _), builtIn),
        !,
        Compile_body(If, TIf, DIf, Ctx),
        Compile_body(Then, TThen, DThen, Ctx),
        Compile_body(Else, TElse, DElse, Ctx).

        Compile_body((Pred1; Pred2), (TPred1; TPred2), (DPred1; DPred2), Ctx) :-
        !,
        Compile_body(Pred1, TPred10, DPred10, Ctx),
        FixDisjunctionLeftSide(TPred10, TPred1),
        FixDisjunctionLeftSide(DPred10, DPred1),
        Compile_body(Pred2, TPred2, DPred2, Ctx).

        Compile_body('*->(Pred1, Pred2), '*->(TPred1, TPred2), '*->(DPred1, DPred2), Ctx) :-
        predicate_property('*->(_, _), builtIn),
        !,
        Compile_body(Pred1, TPred1, DPred1, Ctx),
        Compile_body(Pred2, TPred2, DPred2, Ctx).

        Compile_body((Pred1 -> Pred2), (TPred1 -> TPred2), (DPred1 -> DPred2), Ctx) :-
        !,
        Compile_body(Pred1, TPred1, DPred1, Ctx),
        Compile_body(Pred2, TPred2, DPred2, Ctx).

        Compile_body(\+ Pred, \+ TPred, Debug(goal(\+ Pred, \+ DPred), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Pred, TPred, DPred, Ctx).

   //when processing the debug event, the compiled goal is meta-called but
   //this would make the cut local, changing the semantics of the user code;
   //the solution is to use a conjunction for the debug goal of the debug
   //event with a cut
        Compile_body(!, !, (Debug(goal(!, true), ExCtx), !), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(true, true, Debug(goal(true, true), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(fail, fail, Debug(goal(fail, fail), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(false, false, Debug(goal(false, false), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(repeat, repeat, Debug(goal(repeat, repeat), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(call(Goal), TPred, Debug(goal(call(Goal), DPred), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx),
        (	functor(TGoal, metacall', _) ->
        TPred = TGoal,
        DPred = DGoal
        ;	Cut_transparentControlConstruct(TGoal) ->
   //we need to keep the call/1 wrapper to preserve call/1 cut-opaque semantics
        TPred = call(TGoal),
        DPred = call(DGoal)
        ;	TPred = TGoal,
        DPred = DGoal
        ).

        Compile_body(CallN(Closure, ExtraArgs), _, _, Ctx) :-
        var(Closure),
        CompCtx(Ctx, Head, _, _, _, _, _, _, MetaVars, _, _, _, _, _),
        nonvar(Head),
   //ignore multifile predicates
        Head \= ':(_, _),
        Head \= _::_,
        pp_meta_predicate_(Head, Meta),
   //we're compiling a clause for a meta-predicate
        once(member_var(Closure, MetaVars)),
   //the closure is a meta-argument
        Length(ExtraArgs, 0, NExtraArgs),
        Meta =.. [_| MetaArgs],
   //check that the call/N call complies with the meta-predicate declaration
        notSame_metaArg_extraArgs(MetaArgs, MetaVars, Closure, NExtraArgs, Domain),
        throw(domain_error(Domain, NExtraArgs)).

        Compile_body(CallN(Closure, ExtraArgs), TPred, DPred, Ctx) :-
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        (	var(Closure) ->
   //we're compiling a runtime meta-call
        CheckFor_meta_predicateDirective(Mode, Head, Closure),
        TPred = metacall(Closure, ExtraArgs, ExCtx)
        ;	extendClosure(Closure, ExtraArgs, Goal),
        \+ (functor(Goal, call, Arity), Arity >= 2) ->
   //not a call to call/2-N itself; safe to compile it
        Compile_body(Goal, TPred0, _, Ctx),
        (	Cut_transparentControlConstruct(TPred0) ->
   //we need to keep the call/1 wrapper to preserve call/2-N cut-opaque semantics
        TPred = call(TPred0)
        ;	TPred = TPred0
        )
        ;	% runtime resolved meta-call (e.g. a lambda expression)
        TPred = metacall(Closure, ExtraArgs, ExCtx)
        ),
        CallN =.. [call, Closure| ExtraArgs],
        DPred = Debug(goal(CallN, TPred), ExCtx).

        Compile_body(once(Goal), (TGoal -> true), Debug(goal(once(Goal), (DGoal -> true)), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx).

        Compile_body(ignore(Goal), (TGoal -> true; true), Debug(goal(ignore(Goal), (DGoal -> true; true)), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx).

   //error handling and throwing predicates

        Compile_body(catch(Goal, Catcher, Recovery), catch(TGoal, Catcher, TRecovery), Debug(goal(catch(Goal, Catcher, Recovery), catch(DGoal, Catcher, DRecovery)), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx),
        Compile_body(Recovery, TRecovery, DRecovery, Ctx).

        Compile_body(throw(Error), throw(Error), Debug(goal(throw(Error), throw(Error)), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(instantiation_error, TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(instantiation_error, TPred, DPred, Ctx).

        Compile_body(type_error(Type,Culprit), TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(type_error(Type,Culprit), TPred, DPred, Ctx).

        Compile_body(domain_error(Domain,Culprit), TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(domain_error(Domain,Culprit), TPred, DPred, Ctx).

        Compile_body(existence_error(Thing,Culprit), TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(existence_error(Thing,Culprit), TPred, DPred, Ctx).

        Compile_body(permission_error(Operation,Permission,Culprit), TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(permission_error(Operation,Permission,Culprit), TPred, DPred, Ctx).

        Compile_body(representation_error(Flag), TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(representation_error(Flag), TPred, DPred, Ctx).

        Compile_body(evaluation_error(Error), TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(evaluation_error(Error), TPred, DPred, Ctx).

        Compile_body(resource_error(Resource), TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(resource_error(Resource), TPred, DPred, Ctx).

        Compile_body(syntax_error(Description), TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(syntax_error(Description), TPred, DPred, Ctx).

        Compile_body(system_error, TPred, DPred, Ctx) :-
        !,
        Compile_error_predicate(system_error, TPred, DPred, Ctx).

   //lambda expressions

        Compile_body(Parameters>>Lambda, _, _, Ctx) :-
        CheckLambda_expression(Parameters>>Lambda, Ctx),
        fail.

        Compile_body(Free/Parameters>>Lambda, TPred, DPred, Ctx) :-
        nonvar(Parameters),
        !,
        (	Parameters == [] ->
        Compile_body(Free/Lambda, TPred, DPred, Ctx)
        ;	throw(representation_error(lambda_parameters))
        ).

        Compile_body(Free/Parameters>>Lambda, TPred, DPred, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
   //lambda expressions are handled as meta-calls
        TPred = metacall(Free/Parameters>>Lambda, [], ExCtx),
        DPred = Debug(goal(Free/Parameters>>Lambda, TPred), ExCtx).

        Compile_body(Parameters>>Lambda, TPred, DPred, Ctx) :-
        nonvar(Parameters),
        !,
        (	Parameters == [] ->
        Compile_body(Lambda, TPred, DPred, Ctx)
        ;	throw(representation_error(lambda_parameters))
        ).

        Compile_body(Parameters>>Lambda, TPred, DPred, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
   //lambda expressions are handled as meta-calls
        TPred = metacall(Parameters>>Lambda, [], ExCtx),
        DPred = Debug(goal(Parameters>>Lambda, TPred), ExCtx).

        Compile_body(Free/Lambda, _, _, Ctx) :-
        CheckLambda_expression(Free/Lambda, Ctx),
        fail.

        Compile_body(Free/Lambda, TPred, DPred, Ctx) :-
        nonvar(Free),
        nonvar(Lambda),
        !,
        (	CompCtx_mode(Ctx, compile(_)),
        CompCtx_meta_vars(Ctx, []) ->
   //generate an auxiliary predicate to replace the lambda expression
        generateAux_predicateFunctor('Lambda_', Functor),
        (	Free = {Terms} ->
        Conjunction_toList(Terms, Args)
        ;	Args = []
        ),
        Head =.. [Functor| Args],
        CompileAuxClauses([(Head :- Lambda)]),
        Compile_body(Head, TPred, DPred, Ctx)
        ;	% either runtime translation or the lambda expression appears in the
   //body of a meta-predicate clause
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Lambda, TLambda, DLambda, Ctx),
        TPred = Lambda(Free, TLambda),
        DPred = Debug(goal(Free/Lambda, Lambda(Free, DLambda)), ExCtx)
        ).

        Compile_body(Free/Lambda, TPred, DPred, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
   //lambda expressions are handled as meta-calls
        TPred = metacall(Free/Lambda, [], ExCtx),
        DPred = Debug(goal(Free/Lambda, TPred), ExCtx).

   //built-in meta-predicates

        Compile_body(bagof(Term, QGoal, List), TPred, DPred, Ctx) :-
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        (	var(QGoal) ->
   //runtime meta-call
        CheckFor_meta_predicateDirective(Mode, Head, QGoal),
        TPred = bagof(Term, QGoal, List, ExCtx),
        DPred = Debug(goal(bagof(Term, QGoal, List), TPred), ExCtx)
        ;	% compile time local call
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_quantified_body(QGoal, TGoal, DGoal, Ctx),
        TPred = bagof(Term, TGoal, List),
        DPred = Debug(goal(bagof(Term, QGoal, List), bagof(Term, DGoal, List)), ExCtx)
        ).

        Compile_body(findall(Term, Goal, List), findall(Term, TGoal, List), Debug(goal(findall(Term, Goal, List), findall(Term, DGoal, List)), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx).

        Compile_body(findall(Term, Goal, List, Tail), findall(Term, TGoal, List, Tail), Debug(goal(findall(Term, Goal, List, Tail), findall(Term, DGoal, List, Tail)), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx).

        Compile_body(forall(Gen, Test), \+ (TGen, \+ TTest), Debug(goal(forall(Gen, Test), \+ (DGen, \+ DTest)), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Gen, TGen, DGen, Ctx),
        Compile_body(Test, TTest, DTest, Ctx).

        Compile_body(setof(Term, QGoal, List), TPred, DPred, Ctx) :-
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        (	var(QGoal) ->
   //runtime meta-call
        CheckFor_meta_predicateDirective(Mode, Head, QGoal),
        TPred = Setof(Term, QGoal, List, ExCtx),
        DPred = Debug(goal(setof(Term, QGoal, List), TPred), ExCtx)
        ;	% compile time local call
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_quantified_body(QGoal, TGoal, DGoal, Ctx),
        TPred = setof(Term, TGoal, List),
        DPred = Debug(goal(setof(Term, QGoal, List), setof(Term, DGoal, List)), ExCtx)
        ).

   //file compilation and loading predicates

        Compile_body(logtalkCompile(Files), TPred, DPred, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        ppFile_pathsFlags_(_, Directory, _, _, _),
        TPred = LogtalkCompile(Files, Directory, ExCtx),
        DPred = Debug(goal(logtalkCompile(Files), TPred), ExCtx).

        Compile_body(logtalkCompile(Files, Flags), TPred, DPred, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        ppFile_pathsFlags_(_, Directory, _, _, _),
        TPred = LogtalkCompile(Files, Flags, Directory, ExCtx),
        DPred = Debug(goal(logtalkCompile(Files, Flags), TPred), ExCtx).

        Compile_body(logtalkLoad(Files), TPred, DPred, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        ppFile_pathsFlags_(_, Directory, _, _, _),
        TPred = LogtalkLoad(Files, Directory, ExCtx),
        DPred = Debug(goal(logtalkLoad(Files), TPred), ExCtx).

        Compile_body(logtalkLoad(Files, Flags), TPred, DPred, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        ppFile_pathsFlags_(_, Directory, _, _, _),
        TPred = LogtalkLoad(Files, Flags, Directory, ExCtx),
        DPred = Debug(goal(logtalkLoad(Files, Flags), TPred), ExCtx).

   //file compilation/loading context

        Compile_body(logtalkLoadContext(Key, Value), TPred, DPred, Ctx) :-
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, _, _, _),
        nonvar(Key),
        nonvar(Head),
        functor(Head, (:-), 1),
   //compiling a directive (usually an initialization/1 directive)
        logtalkLoadContext(Key, Value),
   //expand goal to support embedded applications where the compiled
   //code may no longer be loaded using the Logtalk runtime
        !,
        TPred = true,
        DPred = Debug(goal(logtalkLoadContext(Key, Value), TPred), ExCtx).

   //entity enumeration predicates

        Compile_body(current_object(Obj), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Obj),
        CompCtx_execCtx(Ctx, ExCtx),
        (	var(Obj) ->
        TPred = Current_object(Obj, ExCtx)
        ;	TPred = Current_object_(Obj, _, _, _, _, _, _, _, _, _, _)
        ),
        DPred = Debug(goal(current_object(Obj), TPred), ExCtx).

        Compile_body(current_protocol(Ptc), TPred, DPred, Ctx) :-
        !,
        Check(var_or_protocolIdentifier, Ptc),
        CompCtx_execCtx(Ctx, ExCtx),
        (	var(Ptc) ->
        TPred = Current_protocol(Ptc, ExCtx)
        ;	TPred = Current_protocol_(Ptc, _, _, _, _)
        ),
        DPred = Debug(goal(current_protocol(Ptc), TPred), ExCtx).

        Compile_body(currentCategory(Ctg), TPred, DPred, Ctx) :-
        !,
        Check(var_orCategoryIdentifier, Ctg),
        CompCtx_execCtx(Ctx, ExCtx),
        (	var(Ctg) ->
        TPred = CurrentCategory(Ctg, ExCtx)
        ;	TPred = CurrentCategory_(Ctg, _, _, _, _, _)
        ),
        DPred = Debug(goal(currentCategory(Ctg), TPred), ExCtx).

   //entity property predicates

        Compile_body(object_property(Obj, Prop), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Obj),
        Check(var_or_object_property, Prop),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = object_property(Obj, Prop, ExCtx),
        DPred = Debug(goal(object_property(Obj, Prop), TPred), ExCtx).

        Compile_body(protocol_property(Ptc, Prop), TPred, DPred, Ctx) :-
        !,
        Check(var_or_protocolIdentifier, Ptc),
        Check(var_or_protocol_property, Prop),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = protocol_property(Ptc, Prop, ExCtx),
        DPred = Debug(goal(protocol_property(Ptc, Prop), TPred), ExCtx).

        Compile_body(category_property(Ctg, Prop), TPred, DPred, Ctx) :-
        !,
        Check(var_orCategoryIdentifier, Ctg),
        Check(var_orCategory_property, Prop),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Category_property(Ctg, Prop, ExCtx),
        DPred = Debug(goal(category_property(Ctg, Prop), TPred), ExCtx).

   //dynamic entity creation predicates

        Compile_body(create_object(Obj, Relations, Directives, Clauses), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Obj),
        Check(list_or_partialList, Relations),
        Check(list_or_partialList, Directives),
        Check(list_or_partialList, Clauses),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Create_object(Obj, Relations, Directives, Clauses, ExCtx),
        DPred = Debug(goal(create_object(Obj, Relations, Directives, Clauses), TPred), ExCtx).

        Compile_body(create_protocol(Ptc, Relations, Directives), TPred, DPred, Ctx) :-
        !,
        Check(var_or_protocolIdentifier, Ptc),
        Check(list_or_partialList, Relations),
        Check(list_or_partialList, Directives),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Create_protocol(Ptc, Relations, Directives, ExCtx),
        DPred = Debug(goal(create_protocol(Ptc, Relations, Directives), TPred), ExCtx).

        Compile_body(createCategory(Ctg, Relations, Directives, Clauses), TPred, DPred, Ctx) :-
        !,
        Check(var_orCategoryIdentifier, Ctg),
        Check(list_or_partialList, Relations),
        Check(list_or_partialList, Directives),
        Check(list_or_partialList, Clauses),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = CreateCategory(Ctg, Relations, Directives, Clauses, ExCtx),
        DPred = Debug(goal(createCategory(Ctg, Relations, Directives, Clauses), TPred), ExCtx).

   //dynamic entity abolishing predicates

        Compile_body(abolish_object(Obj), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Obj),
        CompCtx_execCtx(Ctx, ExCtx),
        (	var(Obj) ->
        TPred = Abolish_object(Obj, ExCtx)
        ;	TPred = Abolish_objectChecked(Obj, ExCtx)
        ),
        DPred = Debug(goal(abolish_object(Obj), TPred), ExCtx).

        Compile_body(abolish_protocol(Ptc), TPred, DPred, Ctx) :-
        !,
        Check(var_or_protocolIdentifier, Ptc),
        CompCtx_execCtx(Ctx, ExCtx),
        (	var(Ptc) ->
        TPred = Abolish_protocol(Ptc, ExCtx)
        ;	TPred = Abolish_protocolChecked(Ptc, ExCtx)
        ),
        DPred = Debug(goal(abolish_protocol(Ptc), TPred), ExCtx).

        Compile_body(abolishCategory(Ctg), TPred, DPred, Ctx) :-
        !,
        Check(var_orCategoryIdentifier, Ctg),
        CompCtx_execCtx(Ctx, ExCtx),
        (	var(Ctg) ->
        TPred = AbolishCategory(Ctg, ExCtx)
        ;	TPred = AbolishCategoryChecked(Ctg, ExCtx)
        ),
        DPred = Debug(goal(abolishCategory(Ctg), TPred), ExCtx).

   //entity relations predicates

        Compile_body(extends_protocol(Ptc, ExtPtc), TPred, DPred, Ctx) :-
        !,
        Check(var_or_protocolIdentifier, Ptc),
        Check(var_or_protocolIdentifier, ExtPtc),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = extends_protocol(Ptc, ExtPtc, ExCtx),
        DPred = Debug(goal(extends_protocol(Ptc, ExtPtc), TPred), ExCtx).

        Compile_body(extends_protocol(Ptc, ExtPtc, Scope), TPred, DPred, Ctx) :-
        !,
        Check(var_or_protocolIdentifier, Ptc),
        Check(var_or_protocolIdentifier, ExtPtc),
        Check(var_orScope, Scope),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = extends_protocol(Ptc, ExtPtc, Scope, ExCtx),
        DPred = Debug(goal(extends_protocol(Ptc, ExtPtc, Scope), TPred), ExCtx).


        Compile_body(implements_protocol(ObjOrCtg, Ptc), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, ObjOrCtg),
        Check(var_or_protocolIdentifier, Ptc),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Implements_protocol(ObjOrCtg, Ptc, ExCtx),
        DPred = Debug(goal(implements_protocol(ObjOrCtg, Ptc), TPred), ExCtx).

        Compile_body(implements_protocol(ObjOrCtg, Ptc, Scope), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, ObjOrCtg),
        Check(var_or_protocolIdentifier, Ptc),
        Check(var_orScope, Scope),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Implements_protocol(ObjOrCtg, Ptc, Scope, ExCtx),
        DPred = Debug(goal(implements_protocol(ObjOrCtg, Ptc, Scope), TPred), ExCtx).


        Compile_body(importsCategory(Obj, Ctg), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Obj),
        Check(var_orCategoryIdentifier, Ctg),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = ImportsCategory(Obj, Ctg, ExCtx),
        DPred = Debug(goal(importsCategory(Obj, Ctg), TPred), ExCtx).

        Compile_body(importsCategory(Obj, Ctg, Scope), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Obj),
        Check(var_orCategoryIdentifier, Ctg),
        Check(var_orScope, Scope),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = ImportsCategory(Obj, Ctg, Scope, ExCtx),
        DPred = Debug(goal(importsCategory(Obj, Ctg, Scope), TPred), ExCtx).


        Compile_body(instantiatesClass(Obj, Class), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Obj),
        Check(var_or_objectIdentifier, Class),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = InstantiatesClass(Obj, Class, ExCtx),
        DPred = Debug(goal(instantiatesClass(Obj, Class), TPred), ExCtx).

        Compile_body(instantiatesClass(Obj, Class, Scope), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Obj),
        Check(var_or_objectIdentifier, Class),
        Check(var_orScope, Scope),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = InstantiatesClass(Obj, Class, Scope, ExCtx),
        DPred = Debug(goal(instantiatesClass(Obj, Class, Scope), TPred), ExCtx).


        Compile_body(specializesClass(Class, Superclass), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Class),
        Check(var_or_objectIdentifier, Superclass),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = SpecializesClass(Class, Superclass, ExCtx),
        DPred = Debug(goal(specializesClass(Class, Superclass), TPred), ExCtx).

        Compile_body(specializesClass(Class, Superclass, Scope), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Class),
        Check(var_or_objectIdentifier, Superclass),
        Check(var_orScope, Scope),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = SpecializesClass(Class, Superclass, Scope, ExCtx),
        DPred = Debug(goal(specializesClass(Class, Superclass, Scope), TPred), ExCtx).


        Compile_body(extendsCategory(Ctg, ExtCtg), TPred, DPred, Ctx) :-
        !,
        Check(var_orCategoryIdentifier, Ctg),
        Check(var_orCategoryIdentifier, ExtCtg),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = extendsCategory(Ctg, ExtCtg, ExCtx),
        DPred = Debug(goal(extendsCategory(Ctg, ExtCtg), TPred), ExCtx).

        Compile_body(extendsCategory(Ctg, ExtCtg, Scope), TPred, DPred, Ctx) :-
        !,
        Check(var_orCategoryIdentifier, Ctg),
        Check(var_orCategoryIdentifier, ExtCtg),
        Check(var_orScope, Scope),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = extendsCategory(Ctg, ExtCtg, Scope, ExCtx),
        DPred = Debug(goal(extendsCategory(Ctg, ExtCtg, Scope), TPred), ExCtx).


        Compile_body(extends_object(Prototype, Parent), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Prototype),
        Check(var_or_objectIdentifier, Parent),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = extends_object(Prototype, Parent, ExCtx),
        DPred = Debug(goal(extends_object(Prototype, Parent), TPred), ExCtx).

        Compile_body(extends_object(Prototype, Parent, Scope), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, Prototype),
        Check(var_or_objectIdentifier, Parent),
        Check(var_orScope, Scope),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = extends_object(Prototype, Parent, Scope, ExCtx),
        DPred = Debug(goal(extends_object(Prototype, Parent, Scope), TPred), ExCtx).


        Compile_body(complements_object(Category, Object), TPred, DPred, Ctx) :-
        !,
        Check(var_orCategoryIdentifier, Category),
        Check(var_or_objectIdentifier, Object),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Complements_object(Category, Object, ExCtx),
        DPred = Debug(goal(complements_object(Category, Object), TPred), ExCtx).


        Compile_body(conforms_to_protocol(ObjOrCtg, Protocol), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, ObjOrCtg),
        Check(var_or_protocolIdentifier, Protocol),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Conforms_to_protocol(ObjOrCtg, Protocol, ExCtx),
        DPred = Debug(goal(conforms_to_protocol(ObjOrCtg, Protocol), TPred), ExCtx).

        Compile_body(conforms_to_protocol(ObjOrCtg, Protocol, Scope), TPred, DPred, Ctx) :-
        !,
        Check(var_or_objectIdentifier, ObjOrCtg),
        Check(var_or_protocolIdentifier, Protocol),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Conforms_to_protocol(ObjOrCtg, Protocol, Scope, ExCtx),
        DPred = Debug(goal(conforms_to_protocol(ObjOrCtg, Protocol), TPred), ExCtx).

   //events predicates

        Compile_body(current_event(Event, Obj, Msg, Sender, Monitor), TPred, DPred, Ctx) :-
        !,
        Check(var_or_event, Event),
        Check(var_or_objectIdentifier, Obj),
        Check(var_orCallable, Msg),
        Check(var_or_objectIdentifier, Sender),
        Check(var_or_objectIdentifier, Monitor),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Current_event(Event, Obj, Msg, Sender, Monitor, ExCtx),
        DPred = Debug(goal(current_event(Event, Obj, Msg, Sender, Monitor), TPred), ExCtx).

        Compile_body(define_events(Event, Obj, Msg, Sender, Monitor), TPred, DPred, Ctx) :-
        !,
        Check(var_or_event, Event),
        Check(var_or_objectIdentifier, Obj),
        Check(var_orCallable, Msg),
        Check(var_or_objectIdentifier, Sender),
        Check(var_or_objectIdentifier, Monitor),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Define_events(Event, Obj, Msg, Sender, Monitor, ExCtx),
        DPred = Debug(goal(define_events(Event, Obj, Msg, Sender, Monitor), TPred), ExCtx).

        Compile_body(abolish_events(Event, Obj, Msg, Sender, Monitor), TPred, DPred, Ctx) :-
        !,
        Check(var_or_event, Event),
        Check(var_or_objectIdentifier, Obj),
        Check(var_orCallable, Msg),
        Check(var_or_objectIdentifier, Sender),
        Check(var_or_objectIdentifier, Monitor),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = Abolish_events(Event, Obj, Msg, Sender, Monitor, ExCtx),
        DPred = Debug(goal(abolish_events(Event, Obj, Msg, Sender, Monitor), TPred), ExCtx).

   //multi-threading meta-predicates

        Compile_body(threaded(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded(Goals), MTGoals, Debug(goal(threaded(Goals), MDGoals), ExCtx), Ctx) :-
        !,
        Compile_body(Goals, TGoals, DGoals, Ctx),
        Compile_threadedCall(TGoals, MTGoals),
        Compile_threadedCall(DGoals, MDGoals),
        CompCtx_execCtx(Ctx, ExCtx).


        Compile_body(threadedCall(_, _), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threadedCall(Goal, Tag), MTGoal, Debug(goal(threadedCall(Goal, Tag), MDGoal), ExCtx), Ctx) :-
        !,
        Check(var, Tag),
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx),
        MTGoal = threadedCall_tagged(Goal, TGoal, ExCtx, Tag),
        MDGoal = threadedCall_tagged(Goal, DGoal, ExCtx, Tag).


        Compile_body(threadedCall(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threadedCall(Goal), MTGoal, Debug(goal(threadedCall(Goal), MDGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx),
        MTGoal = threadedCall(Goal, TGoal, ExCtx),
        MDGoal = threadedCall(Goal, DGoal, ExCtx).

        Compile_body(threaded_once(_, _), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_once(Goal, Tag), MTGoal, Debug(goal(threaded_once(Goal, Tag), MDGoal), ExCtx), Ctx) :-
        !,
        Check(var, Tag),
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx),
        MTGoal = threaded_once_tagged(Goal, TGoal, ExCtx, Tag),
        MDGoal = threaded_once_tagged(Goal, DGoal, ExCtx, Tag).


        Compile_body(threaded_once(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_once(Goal), MTGoal, Debug(goal(threaded_once(Goal), MDGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx),
        MTGoal = threaded_once(Goal, TGoal, ExCtx),
        MDGoal = threaded_once(Goal, DGoal, ExCtx).


        Compile_body(threadedIgnore(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threadedIgnore(Goal), MTGoal, Debug(goal(threadedIgnore(Goal), MDGoal), ExCtx), Ctx) :-
        !,
        Compile_body(Goal, TGoal, DGoal, Ctx),
        CompCtx_execCtx(Ctx, ExCtx),
        MTGoal = threadedIgnore(Goal, TGoal, ExCtx),
        MDGoal = threadedIgnore(Goal, DGoal, ExCtx).


        Compile_body(threaded_exit(_, _), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_exit(Goal, Tag), TGoal, Debug(goal(threaded_exit(Goal, Tag), TGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
   //compile the goal just for entityKind-checking and collecting source data
        Compile_body(Goal, _, _, Ctx),
        TGoal = threaded_exit_tagged(Goal, ExCtx, Tag).


        Compile_body(threaded_exit(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_exit(Goal), TGoal, Debug(goal(threaded_exit(Goal), TGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
   //compile the goal just for entityKind-checking and collecting source data
        Compile_body(Goal, _, _, Ctx),
        TGoal = threaded_exit(Goal, ExCtx).


        Compile_body(threaded_peek(_, _), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_peek(Goal, Tag), TGoal, Debug(goal(threaded_peek(Goal, Tag), TGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
   //compile the goal just for entityKind-checking and collecting source data
        Compile_body(Goal, _, _, Ctx),
        TGoal = threaded_peek_tagged(Goal, ExCtx, Tag).


        Compile_body(threaded_peek(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_peek(Goal), TGoal, Debug(goal(threaded_peek(Goal), TGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
   //compile the goal just for entityKind-checking and collecting source data
        Compile_body(Goal, _, _, Ctx),
        TGoal = threaded_peek(Goal, ExCtx).


        Compile_body(threaded_engineCreate(_, _, _), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engineCreate(AnswerTemplate, Goal, Engine), MTGoal, Debug(goal(threaded_engineCreate(AnswerTemplate, Goal, Engine), MDGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Goal, TGoal, DGoal, Ctx),
        MTGoal = threaded_engineCreate(AnswerTemplate, Goal, TGoal, ExCtx, Engine),
        MDGoal = threaded_engineCreate(AnswerTemplate, Goal, DGoal, ExCtx, Engine).


        Compile_body(threaded_engineSelf(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engineSelf(Engine), MTGoal, Debug(goal(threaded_engineSelf(Engine), MTGoal), ExCtx), Ctx) :-
        !,
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        MTGoal = threaded_engineSelf(This, Engine),
        executionContext(ExCtx, _, _, This, _, _, _).


        Compile_body(threaded_engine(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engine(Engine), MTGoal, Debug(goal(threaded_engine(Engine), MTGoal), ExCtx), Ctx) :-
        !,
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        MTGoal = Current_engine(This, Engine),
        executionContext(ExCtx, _, _, This, _, _, _).


        Compile_body(threaded_engine_next(_, _), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engine_next(Engine, Answer), MTGoal, Debug(goal(threaded_engine_nextReified(Engine, Answer), MTGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        MTGoal = threaded_engine_next(Engine, Answer, ExCtx).


        Compile_body(threaded_engine_nextReified(_, _), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engine_nextReified(Engine, Answer), MTGoal, Debug(goal(threaded_engine_nextReified(Engine, Answer), MTGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        MTGoal = threaded_engine_nextReified(Engine, Answer, ExCtx).


        Compile_body(threaded_engine_yield(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engine_yield(Answer), MTGoal, Debug(goal(threaded_engine_yield(Answer), MTGoal), ExCtx), Ctx) :-
        !,
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        MTGoal = threaded_engine_yield(Answer, This),
        executionContext(ExCtx, _, _, This, _, _, _).


        Compile_body(threaded_engine_post(_, _), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engine_post(Engine, Message), MTGoal, Debug(goal(threaded_engine_post(Engine, Message), MTGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        MTGoal = threaded_engine_post(Engine, Message, ExCtx).


        Compile_body(threaded_engineFetch(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engineFetch(Message), MTGoal, Debug(goal(threaded_engineFetch(Message), MTGoal), ExCtx), Ctx) :-
        !,
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        MTGoal = threaded_engineFetch(Message, This),
        executionContext(ExCtx, _, _, This, _, _, _).


        Compile_body(threaded_engineDestroy(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_engineDestroy(Engine), MTGoal, Debug(goal(threaded_engineDestroy(Engine), MTGoal), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        MTGoal = threaded_engineDestroy(Engine, ExCtx).


        Compile_body(threaded_wait(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_wait(Msg), MTPred, Debug(goal(threaded_wait(Msg), MTPred), ExCtx), Ctx) :-
        !,
        (	pp_entity_(Type, _, Prefix, _, _) ->
        true
        ;	Type = object	% <</2 call
        ),
        CompCtx(Ctx, Head, _, _, _, _, _, Prefix, _, _, ExCtx, _, _, _),
        (	nonvar(Head),
        ppSynchronized_(Head, Mutex) ->
        (	Type == object ->
   //we're compiling an object predicate
        MTPred = threaded_waitSynch(Mutex, Msg, Prefix)
        ;	% we're compiling a category predicate
        CompCtx_this(Ctx, This),
        executionContext_this_entity(ExCtx, This, _),
        MTPred = threaded_waitSynchCtg(Mutex, Msg, This)
        )
        ;	(	Type == object ->
   //we're compiling an object predicate
        MTPred = threaded_wait(Msg, Prefix)
        ;	% we're compiling a category predicate
        CompCtx_this(Ctx, This),
        executionContext_this_entity(ExCtx, This, _),
        MTPred = threaded_waitCtg(Msg, This)
        )
        ).


        Compile_body(threaded_notify(_), _, _, _) :-
        \+ pp_threaded_',
        pp_object_(_, _, _, _, _, _, _, _, _, _, _),
        throw(resource_error(threads)).

        Compile_body(threaded_notify(Msg), MTPred, Debug(goal(threaded_notify(Msg), MTPred), ExCtx), Ctx) :-
        !,
        (	pp_entity_(Type, _, Prefix, _, _) ->
        true
        ;	Type = object	% <</2 call
        ),
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, ExCtx, _, _, _),
        (	Type == object ->
   //we're compiling an object predicate
        MTPred = threaded_notify(Msg, Prefix)
        ;	% we're compiling a category predicate
        CompCtx_this(Ctx, This),
        executionContext_this_entity(ExCtx, This, _),
        MTPred = threaded_notifyCtg(Msg, This)
        ).

   //message sending

        Compile_body(Obj::Pred, TPred, Debug(goal(Obj::Pred, TPred), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        CompilerFlag(events, Events),
        Compile_message_to_object(Pred, Obj, TPred, Events, Ctx).

        Compile_body(::Pred, TPred, Debug(goal(::Pred, TPred), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_message_toSelf(Pred, TPred, Ctx).

        Compile_body(^^Pred, TPred, Debug(goal(^^Pred, TPred), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        CompileSuperCall(Pred, TPred, Ctx).

   //context-switching

        Compile_body(Obj<<Pred, TPred, Debug(goal(Obj<<Pred, TPred), ExCtx), Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        CompileContextSwitchCall(Obj, Pred, TPred, ExCtx).

   //calling explicitly qualified module predicates

        Compile_body(':(_, Callable), TPred, DPred, Ctx) :-
        nonvar(Callable),
        Callable = ':(Module, Pred),
        !,
        Compile_body(':(Module, Pred), TPred, DPred, Ctx).

        Compile_body(':(Module, Pred), TPred, DPred, Ctx) :-
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Pred),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::Pred, TPred, DPred, Ctx)
        ;	var(Module) ->
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = ':(Module, Pred),
        DPred = Debug(goal(':(Module, Pred), TPred), ExCtx)
        ;	var(Pred) ->
        AddReferenced_module(Module, Ctx),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = ':(Module, Pred),
        DPred = Debug(goal(':(Module, Pred), TPred), ExCtx)
        ;	\+ prolog_builtInDatabase_predicate(Pred),
   //the meta-predicate templates for the backend Prolog database predicates are usually
   //not usable from Logtalk due the ambiguity of the ":" meta-argument qualifier but they
   //pose no problems when operating in a module database; in this particular case, the
   //explicit-qualified call can be compiled as-is
        (	pp_meta_predicate_(':(Module, Pred), ':(Module, Meta))
   //we're either overriding the original meta-predicate template or working around a
   //backend Prolog compiler limitation in providing access to meta-predicate templates
        ;	catch(predicate_property(':(Module, Pred), meta_predicate(Meta)), _, fail)
        ) ->
   //we're compiling a call to a module meta-predicate
        AddReferenced_module(Module, Ctx),
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        AddReferenced_module_predicate(Mode, Module, Pred, Pred, Head),
        Pred =.. [Functor| Args],
        Meta =.. [Functor| MArgs],
        prolog_toLogtalk_metaArgumentSpecifiers(MArgs, CMArgs),
        (	member(CMArg, CMArgs), CMArg == (::) ->
   //the "::" meta-argument specifier is ambiguous in this context
        throw(domain_error(metaArgumentSpecifier, Meta))
        ;	Compile_prolog_metaArguments(Args, CMArgs, Ctx, TArgs, DArgs) ->
        TPred0 =.. [Functor| TArgs],
        TPred = ':(Module, TPred0),
        DPred0 =.. [Functor| DArgs],
        DPred = Debug(goal(':(Module, Pred), DPred0), ExCtx)
        ;	throw(domain_error(metaDirective_template, Meta))
        )
        ;	% we're compiling a call to a module predicate
        AddReferenced_module(Module, Ctx),
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        AddReferenced_module_predicate(Mode, Module, Pred, Pred, Head),
        TPred = ':(Module, Pred),
        DPred = Debug(goal(':(Module, Pred), TPred), ExCtx)
        ).

   //reflection built-in predicates

        Compile_body(current_op(Priority, Specifier, Operator), TPred, DPred, Ctx) :-
        CompCtx(Ctx, _, _, Entity, _, _, _, _, _, _, ExCtx, _, _, _),
        Entity == user,
   //usually a call from an initialization or conditional compilation directive
        !,
        TPred = current_op(Priority, Specifier, Operator),
        DPred = Debug(goal(current_op(Priority, Specifier, Operator), TPred), ExCtx).

        Compile_body(current_op(Priority, Specifier, Operator), TPred, DPred, Ctx) :-
        !,
        Check(var_or_operator_priority, Priority),
        Check(var_or_operatorSpecifier, Specifier),
        Check(var_orAtom, Operator),
        CompCtx(Ctx, _, _, Entity, _, This, _, _, _, _, ExCtx, _, _, _),
        DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        TPred = Current_op(Database, Priority, Specifier, Operator, Database, p(_), ExCtx),
        DPred = Debug(goal(current_op(Priority, Specifier, Operator), TPred), ExCtx).

        Compile_body(current_predicate(Term), TPred, DPred, Ctx) :-
        CompCtx(Ctx, _, _, Entity, _, _, _, _, _, _, ExCtx, _, _, _),
        Entity == user,
   //usually a call from an initialization or conditional compilation directive
        !,
        TPred = current_predicate(Term),
        DPred = Debug(goal(current_predicate(Term), TPred), ExCtx).

        Compile_body(current_predicate(Term), TPred, DPred, Ctx) :-
        nonvar(Term),
        Term = ':(Module, Pred),
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Pred),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::current_predicate(Pred), TPred, DPred, Ctx)
        ;	% we're using modules together with objects
        AddReferenced_module(Module, Ctx),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = current_predicate(':(Module, Pred)),
        DPred = Debug(goal(current_predicate(':(Module, Pred)), TPred), ExCtx)
        ).

        Compile_body(current_predicate(Term), TPred, DPred, Ctx) :-
        valid_predicateIndicator(Term, AliasFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        functor(Head, HeadFunctor, Arity),
        Compile_body(Obj::current_predicate(HeadFunctor/Arity), TPred, DPred, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        functor(Head, HeadFunctor, Arity),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = current_predicate(':(Module, HeadFunctor/Arity)),
        DPred = Debug(goal(current_predicate(':(Module, HeadFunctor/Arity)), TPred), ExCtx)
        ;	fail
        ),
        !.

        Compile_body(current_predicate(Pred), TPred, DPred, Ctx) :-
        !,
        Check(var_or_predicateIndicator, Pred),
        CompCtx(Ctx, _, _, Entity, _, This, _, _, _, _, ExCtx, _, _, _),
        DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        TPred = Current_predicate(Database, Pred, Database, p(_), ExCtx),
        DPred = Debug(goal(current_predicate(Pred), TPred), ExCtx).

        Compile_body(predicate_property(Term, Prop), TPred, DPred, Ctx) :-
        CompCtx(Ctx, _, _, Entity, _, _, _, _, _, _, ExCtx, _, _, _),
        Entity == user,
   //usually a call from an initialization or conditional compilation directive
        !,
        TPred = predicate_property(Term, Prop),
        DPred = Debug(goal(predicate_property(Term, Prop), TPred), ExCtx).

        Compile_body(predicate_property(Term, Prop), TPred, DPred, Ctx) :-
        nonvar(Term),
        Term = ':(Module, Head),
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Head),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::predicate_property(Head, Prop), TPred, DPred, Ctx)
        ;	% we're using modules together with objects
        AddReferenced_module(Module, Ctx),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = predicate_property(':(Module, Head), Prop),
        DPred = Debug(goal(predicate_property(':(Module,Head), Prop), TPred), ExCtx)
        ).

        Compile_body(predicate_property(Alias, Prop), TPred, DPred, Ctx) :-
        nonvar(Alias),
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::predicate_property(Head, Prop), TPred, DPred, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = predicate_property(':(Module, Head), Prop),
        DPred = Debug(goal(predicate_property(':(Module,Head), Prop), TPred), ExCtx)
        ;	fail
        ),
        !.

        Compile_body(predicate_property(Pred, Prop), TPred, DPred, Ctx) :-
        !,
        Check(var_orCallable, Pred),
        Check(var_or_predicate_property, Prop),
        CompCtx(Ctx, _, _, Entity, _, This, _, _, _, _, ExCtx, _, _, _),
        DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        TPred = predicate_property(Database, Pred, Prop, Database, p(_), ExCtx),
        DPred = Debug(goal(predicate_property(Pred, Prop), TPred), ExCtx).

   //database handling built-in predicates

        Compile_body(abolish(Term), TCond, DCond, Ctx) :-
        nonvar(Term),
        Term = ':(Module, Pred),
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Pred),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::abolish(Pred), TCond, DCond, Ctx)
        ;	% we're using modules together with objects
        AddReferenced_module(Module, Ctx),
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = abolish(':(Module, Pred)),
        DCond = Debug(goal(abolish(':(Module, Pred)), TCond), ExCtx),
        (	ground(Term) ->
        Remember_updated_predicate(Mode, ':(Module, Pred), CallerHead)
        ;	true
        )
        ).

        Compile_body(abolish(Pred), TCond, DCond, Ctx) :-
        valid_predicateIndicator(Pred, AliasFunctor, Arity),
        functor(Alias, AliasFunctor, Arity),
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        functor(Head, HeadFunctor, Arity),
        Compile_body(Obj::abolish(HeadFunctor/Arity), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        functor(Head, HeadFunctor, Arity),
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = abolish(':(Module, HeadFunctor/Arity)),
        DCond = Debug(goal(abolish(':(Module, HeadFunctor/Arity)), TCond), ExCtx),
        Remember_updated_predicate(Mode, ':(Module, HeadFunctor/Arity), CallerHead)
        ;	% proceed to next clause
        fail
        ),
        !.

        Compile_body(abolish(Pred), TCond, DCond, Ctx) :-
        !,
        CompCtx(Ctx, Head, _, Entity, _, This, _, _, _, _, ExCtx, Mode, _, _),
        DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        Check(var_or_predicateIndicator, Pred),
        CheckDynamicDirective(Mode, Pred),
        (	ground(Pred) ->
        TCond = AbolishChecked(Database, Pred, Database, p(_), ExCtx),
        Remember_updated_predicate(Mode, Pred, Head)
        ;	% partially instantiated predicate indicator; runtime check required
        TCond = Abolish(Database, Pred, Database, p(_), ExCtx)
        ),
        DCond = Debug(goal(abolish(Pred), TCond), ExCtx).

        Compile_body(assert(Clause), TCond, DCond, Ctx) :-
        !,
        (	CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(deprecated, warning),
        SourceFileContext(File, Lines),
        pp_entity_(Type, Entity, _, _, _) ->
        IncrementCompiling_warningsCounter',
        print_message(warning(deprecated), core, deprecated_predicate(File, Lines, Type, Entity, assert/1))
        ;	true
        ),
        Compile_body(assertz(Clause), TCond, DCond, Ctx).

        Compile_body(asserta(QClause), TCond, DCond, Ctx) :-
        nonvar(QClause),
        (	QClause = (QHead :- Body),
        nonvar(QHead),
        QHead = ':(Module,Head) ->
        Clause = (Head :- Body)
        ;	QClause = ':(Module,Head),
        Clause = Head,
        Body = true
        ),
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Head),
        Check(var_orCallable, Body),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::asserta(Clause), TCond, DCond, Ctx)
        ;	% we're using modules together with objects
        AddReferenced_module(Module, Ctx),
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = asserta(QClause),
        DCond = Debug(goal(asserta(QClause), TCond), ExCtx),
        (	ground(QClause) ->
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	true
        )
        ).

        Compile_body(asserta(Clause), TCond, DCond, Ctx) :-
        nonvar(Clause),
        (	Clause = (Alias :- Body) ->
        nonvar(Alias),
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::asserta((Head :- Body)), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = asserta((':(Module,Head) :- Body)),
        DCond = Debug(goal(asserta((':(Module,Head) :- Body)), TCond), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	% proceed to next clause
        fail
        )
        ;	Clause = Alias,
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::asserta(Head), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = asserta(':(Module,Head)),
        DCond = Debug(goal(asserta(':(Module,Head)), TCond), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	% proceed to next clause
        fail
        )
        ),
        !.

        Compile_body(asserta(Clause), TCond, DCond, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, Entity, _, This, _, _, _, _, ExCtx, Mode, _, _),
        (	optimizableLocalDbCall(Clause, TClause) ->
        TCond = asserta(TClause),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ;	DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        (	RuntimeCheckedDbClause(Clause) ->
        TCond = Asserta(Database, Clause, Database, p(_), p)
        ;	Check(clause_or_partialClause, Clause),
        (	Clause = (Head :- Body) ->
        (	Body == true ->
        TCond = AssertaFactChecked(Database, Head, Database, p(_), p, ExCtx)
        ;	TCond = AssertaRuleChecked(Database, Clause, Database, p(_), p, ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ;	TCond = AssertaFactChecked(Database, Clause, Database, p(_), p, ExCtx),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        )
        ),
        CheckDynamicDirective(Mode, Clause)
        ),
        DCond = Debug(goal(asserta(Clause), TCond), ExCtx).

        Compile_body(assertz(QClause), TCond, DCond, Ctx) :-
        nonvar(QClause),
        (	QClause = (QHead :- Body),
        nonvar(QHead),
        QHead = ':(Module,Head) ->
        Clause = (Head :- Body)
        ;	QClause = ':(Module,Head),
        Clause = Head,
        Body = true
        ),
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Head),
        Check(var_orCallable, Body),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::assertz(Clause), TCond, DCond, Ctx)
        ;	% we're using modules together with objects
        AddReferenced_module(Module, Ctx),
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = assertz(QClause),
        DCond = Debug(goal(assertz(QClause), TCond), ExCtx),
        (	ground(QClause) ->
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	true
        )
        ).

        Compile_body(assertz(Clause), TCond, DCond, Ctx) :-
        nonvar(Clause),
        (	Clause = (Alias :- Body) ->
        nonvar(Alias),
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::assertz((Head :- Body)), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = assertz((':(Module,Head) :- Body)),
        DCond = Debug(goal(assertz((':(Module,Head) :- Body)), TCond), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	% proceed to next clause
        fail
        )
        ;	Clause = Alias,
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::assertz(Head), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = assertz(':(Module,Head)),
        DCond = Debug(goal(assertz(':(Module,Head)), TCond), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	% proceed to next clause
        fail
        )
        ),
        !.

        Compile_body(assertz(Clause), TCond, DCond, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, Entity, _, This, _, _, _, _, ExCtx, Mode, _, _),
        (	optimizableLocalDbCall(Clause, TClause) ->
        TCond = assertz(TClause),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ;	DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        (	RuntimeCheckedDbClause(Clause) ->
        TCond = Assertz(Database, Clause, Database, p(_), p, ExCtx)
        ;	Check(clause_or_partialClause, Clause),
        (	Clause = (Head :- Body) ->
        (	Body == true ->
        TCond = AssertzFactChecked(Database, Head, Database, p(_), p, ExCtx)
        ;	TCond = AssertzRuleChecked(Database, Clause, Database, p(_), p, ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ;	TCond = AssertzFactChecked(Database, Clause, Database, p(_), p, ExCtx),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        )
        ),
        CheckDynamicDirective(Mode, Clause)
        ),
        DCond = Debug(goal(assertz(Clause), TCond), ExCtx).

        Compile_body(clause(QHead, Body), TCond, DCond, Ctx) :-
        nonvar(QHead),
        QHead = ':(Module, Head),
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Head),
        Check(var_orCallable, Body),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::clause(Head, Body), TCond, DCond, Ctx)
        ;	% we're using modules together with objects
        AddReferenced_module(Module, Ctx),
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = clause(QHead, Body),
        DCond = Debug(goal(clause(QHead, Body), TCond), ExCtx),
        (	ground(QHead) ->
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	true
        )
        ).

        Compile_body(clause(Alias, Body), TCond, DCond, Ctx) :-
        nonvar(Alias),
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::clause(Head, Body), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = clause(':(Module,Head), Body),
        DCond = Debug(goal(clause(':(Module,Head), Body), TCond), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	fail
        ),
        !.

        Compile_body(clause(Head, Body), TCond, DCond, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, Entity, _, This, _, _, _, _, ExCtx, Mode, _, _),
        (	optimizableLocalDbCall(Head, THead) ->
        Check(var_orCallable, Body),
        TCond = (clause(THead, TBody), (TBody = (nop(Body), _) -> true; TBody = Body)),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ;	DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        (	RuntimeCheckedDbClause((Head :- Body)) ->
        TCond = Clause(Database, Head, Body, Database, p(_), ExCtx)
        ;	Check(clause_or_partialClause, (Head :- Body)),
        TCond = ClauseChecked(Database, Head, Body, Database, p(_), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ),
        CheckDynamicDirective(Mode, Head)
        ),
        DCond = Debug(goal(clause(Head, Body), TCond), ExCtx).

        Compile_body(retract(QClause), TCond, DCond, Ctx) :-
        nonvar(QClause),
        (	QClause = (QHead :- Body),
        nonvar(QHead),
        QHead = ':(Module,Head) ->
        Clause = (Head :- Body)
        ;	QClause = ':(Module,Head),
        Clause = Head,
        Body = true
        ),
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Head),
        Check(var_orCallable, Body),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::retract(Clause), TCond, DCond, Ctx)
        ;	% we're using modules together with objects
        AddReferenced_module(Module, Ctx),
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = retract(QClause),
        DCond = Debug(goal(retract(QClause), TCond), ExCtx),
        (	ground(QClause) ->
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	true
        )
        ).

        Compile_body(retract(Clause), TCond, DCond, Ctx) :-
        nonvar(Clause),
        (	Clause = (Alias :- Body) ->
        nonvar(Alias),
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::retract((Head :- Body)), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = retract((':(Module,Head) :- Body)),
        DCond = Debug(goal(retract((':(Module,Head) :- Body)), TCond), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	% proceed to next clause
        fail
        )
        ;	Clause = Alias,
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::retract(Head), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = retract(':(Module,Head)),
        DCond = Debug(goal(retract(':(Module,Head)), TCond), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	% proceed to next clause
        fail
        )
        ),
        !.

        Compile_body(retract(Clause), TCond, DCond, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, Entity, _, This, _, _, _, _, ExCtx, Mode, _, _),
        (	optimizableLocalDbCall(Clause, TClause) ->
        TCond = retract(TClause),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ;	DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        (	RuntimeCheckedDbClause(Clause) ->
        TCond = Retract(Database, Clause, Database, p(_), ExCtx)
        ;	Check(clause_or_partialClause, Clause),
        (	Clause = (Head :- Body) ->
        (	var(Body) ->
        Retract_var_bodyChecked(Database, Clause, Database, p(_), ExCtx)
        ;	Body == true ->
        TCond = RetractFactChecked(Database, Head, Database, p(_), ExCtx)
        ;	TCond = RetractRuleChecked(Database, Clause, Database, p(_), ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ;	TCond = RetractFactChecked(Database, Clause, Database, p(_), ExCtx),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        )
        ),
        CheckDynamicDirective(Mode, Clause)
        ),
        DCond = Debug(goal(retract(Clause), TCond), ExCtx).

        Compile_body(retractall(QHead), TCond, DCond, Ctx) :-
        nonvar(QHead),
        QHead = ':(Module, Head),
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Head),
        (	pp_module_(_) ->
   //we're compiling a module as an object; assume referenced modules are also compiled as objects
        Compile_body(Module::retractall(Head), TCond, DCond, Ctx)
        ;	% we're using modules together with objects
        AddReferenced_module(Module, Ctx),
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = retractall(QHead),
        DCond = Debug(goal(retractall(QHead), TCond), ExCtx),
        (	ground(QHead) ->
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	true
        )
        ).

        Compile_body(retractall(Alias), TCond, DCond, Ctx) :-
        nonvar(Alias),
        (	pp_uses_predicate_(Obj, Head, Alias, _) ->
        Compile_body(Obj::retractall(Head), TCond, DCond, Ctx)
        ;	pp_use_module_predicate_(Module, Head, Alias, _) ->
        CompCtx(Ctx, CallerHead, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TCond = retractall(':(Module,Head)),
        DCond = Debug(goal(retractall(':(Module,Head)), TCond), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ':(Module, Functor/Arity), CallerHead)
        ;	% proceed to next clause
        fail
        ),
        !.

        Compile_body(retractall(Head), TCond, DCond, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, Entity, _, This, _, _, _, _, ExCtx, Mode, _, _),
        (	optimizableLocalDbCall(Head, THead) ->
        TCond = retractall(THead),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ;	DbCallDatabase_executionContext(Entity, This, Database, ExCtx),
        (	var(Head) ->
        TCond = Retractall(Database, Head, Database, p(_), ExCtx)
        ;	Check(callable, Head),
        TCond = RetractallChecked(Database, Head, Database, p(_), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Functor/Arity, CallerHead)
        ),
        CheckDynamicDirective(Mode, Head)
        ),
        DCond = Debug(goal(retractall(Head), TCond), ExCtx).

   //term and goal expansion predicates

        Compile_body(expand_term(Term, Expansion), TPred, Debug(goal(expand_term(Term, Expansion), TPred), ExCtx), Ctx) :-
        !,
        CompCtx(Ctx, _, _, Entity, _, _, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, _, Entity),
        TPred = expand_termLocal(Entity, Term, Expansion, ExCtx).

        Compile_body(expand_goal(Goal, ExpandedGoal), TPred, Debug(goal(expand_goal(Goal, ExpandedGoal), TPred), ExCtx), Ctx) :-
        !,
        CompCtx(Ctx, _, _, Entity, _, _, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, _, Entity),
        TPred = expand_goalLocal(Entity, Goal, ExpandedGoal, ExCtx).


   //DCG predicates

        Compile_body(phrase(GRBody, Input), TPred, Debug(goal(phrase(GRBody, Input), TPred), ExCtx), Ctx) :-
        var(GRBody),
        !,
        Check(list_or_partialList, Input),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = phrase(GRBody, Input, ExCtx).

        Compile_body(phrase(GRBody, Input), TPred, Debug(goal(phrase(GRBody, Input), DPred), ExCtx), Ctx) :-
        !,
   //the Dcg_body'/5 predicate already checks that the grammar rule body is callable
        Dcg_body(GRBody, S0, S, Pred, Ctx),
        Check(list_or_partialList, Input),
        TPred = (Input = S0, [] = S, TPred0),
        DPred = (Input = S0, [] = S, DPred0),
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Pred, TPred0, DPred0, Ctx).

        Compile_body(phrase(GRBody, Input, Rest), TPred, Debug(goal(phrase(GRBody, Input, Rest), TPred), ExCtx), Ctx) :-
        var(GRBody),
        !,
        Check(list_or_partialList, Input),
        Check(list_or_partialList, Rest),
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = phrase(GRBody, Input, Rest, ExCtx).

        Compile_body(phrase(GRBody, Input, Rest), TPred, Debug(goal(phrase(GRBody, Input, Rest), DPred), ExCtx), Ctx) :-
        !,
   //the Dcg_body'/5 predicate already checks that the grammar rule body is callable
        Dcg_body(GRBody, S0, S, Pred, Ctx),
        Check(list_or_partialList, Input),
        Check(list_or_partialList, Rest),
        TPred = (Input = S0, Rest = S, TPred0),
        DPred = (Input = S0, Rest = S, DPred0),
        CompCtx_execCtx(Ctx, ExCtx),
        Compile_body(Pred, TPred0, DPred0, Ctx).

   //execution-context methods
        %
   //calls to these methods are compiled inline whenever possible by unifying
   //the method argument with the corresponding execution context argument;
   //with the exception of context/1, calls with instantiated arguments are
   //not inlined as the call may be used as e.g. a condition in an if-then-else
   //control construct

        Compile_body(context(Context), true, Debug(goal(context(Context), true), ExCtx), Ctx) :-
        !,
        Check(var, Context),
        CompCtx_head(Ctx, Head0),
        (	Head0 = _::Head ->
        true
        ;	Head0 = ':(_,Head) ->
        true
        ;	Head0 = Head
        ),
        CompCtx_head_execCtx(Ctx, ExCtx),
        Context = logtalk(Head, ExCtx).

        Compile_body(sender(Sender), TPred, Debug(goal(sender(DSender), DPred), ExCtx), Ctx) :-
        !,
        CompCtx_head_execCtx(Ctx, ExCtx),
        executionContext(ExCtx, _, Sender0, _, _, _, _),
        (	var(Sender) ->
   //compile time unification
        Sender0 = Sender,
        TPred = true,
        DPred = (DSender = Sender)
        ;	% we must delay unification to runtime
        TPred = (Sender0 = Sender),
        DPred = TPred,
        DSender = Sender
        ).

        Compile_body(this(This), TPred, Debug(goal(this(DThis), DPred), ExCtx), Ctx) :-
        !,
        CompCtx_head_execCtx(Ctx, ExCtx),
        executionContext(ExCtx, _, _, This0, _, _, _),
        (	var(This) ->
   //compile time unification
        This0 = This,
        TPred = true,
        DPred = (DThis = This)
        ;	% we must delay unification to runtime
        TPred = (This0 = This),
        DPred = TPred,
        DThis = This
        ).

        Compile_body(self(Self), TPred, Debug(goal(self(DSelf), DPred), ExCtx), Ctx) :-
        !,
        CompCtx_head_execCtx(Ctx, ExCtx),
        executionContext(ExCtx, _, _, _, Self0, _, _),
        (	var(Self) ->
   //compile time unification
        Self0 = Self,
        TPred = true,
        DPred = (DSelf = Self)
        ;	% we must delay unification to runtime
        TPred = (Self0 = Self),
        DPred = TPred,
        DSelf = Self
        ).

        Compile_body(parameter(Arg, _), _, _, Ctx) :-
        Check(integer, Arg),
        (	pp_entity_(_, Entity, _, _, _) ->
   //compile time
        true
        ;	% runtime <</2 call
        CompCtx_entity(Ctx, Entity)
        ),
        \+ compound(Entity),
        throw(type_error(parametric_entity, Entity)).

        Compile_body(parameter(Arg, Value), TPred, Debug(goal(parameter(Arg, DValue), DPred), ExCtx), Ctx) :-
        (	pp_entity_(_, Entity, _, _, _) ->
   //compile time; instantiate the Entity argument in the compilation context
        true
        ;	% runtime <</2 call; Entity alreay instantiated in the compilation context
        true
        ),
        CompCtx(Ctx, _, _, Entity, _, _, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, _, Entity),
        functor(Entity, _, Arity),
        (	1 =< Arg, Arg =< Arity ->
        arg(Arg, Entity, Value0),
        (	var(Value) ->
   //parameter compile time unification
        Value0 = Value,
        TPred = true,
        DPred = (DValue = Value)
        ;	% we must delay unification to runtime
        TPred = (Value0 = Value),
        DPred = TPred,
        DValue = Value
        )
        ;	throw(domain_error([1,Arity], Arg))
        ).

   //term input predicates that need to be operator aware
   //(these translations are only applied if there are local entity operators declared)

        Compile_body(read_term(Stream, Term, Options), IsoRead_term(Stream, Term, Options, Ops), Debug(goal(read_term(Stream, Term, Options), IsoRead_term(Stream, Term, Options, Ops)), ExCtx), Ctx) :-
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

        Compile_body(read_term(Term, Options), IsoRead_term(Term, Options, Ops), Debug(goal(read_term(Term, Options), IsoRead_term(Term, Options, Ops)), ExCtx), Ctx) :-
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

        Compile_body(read(Stream, Term), IsoRead(Stream, Term, Ops), Debug(goal(read(Stream, Term), IsoRead(Stream, Term, Ops)), ExCtx), Ctx) :-
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

        Compile_body(read(Term), IsoRead(Term, Ops), Debug(goal(read(Term), IsoRead(Term, Ops)), ExCtx), Ctx) :-
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

   //term output predicates that need to be operator aware
   //(these translations are only applied if there are local entity operators declared)

        Compile_body(write_term(Stream, Term, Options), Iso_write_term(Stream, Term, Options, Ops), Debug(goal(write_term(Stream, Term, Options), Iso_write_term(Stream, Term, Options, Ops)), ExCtx), Ctx) :-
        (member(ignore_ops(Value), Options) -> Value \== true; true),
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

        Compile_body(write_term(Term, Options), Iso_write_term(Term, Options, Ops), Debug(goal(write_term(Term, Options), Iso_write_term(Term, Options, Ops)), ExCtx), Ctx) :-
        (member(ignore_ops(Value), Options) -> Value \== true; true),
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

        Compile_body(write(Stream, Term), Iso_write(Stream, Term, Ops), Debug(goal(write(Stream, Term), Iso_write(Stream, Term, Ops)), ExCtx), Ctx) :-
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

        Compile_body(write(Term), Iso_write(Term, Ops), Debug(goal(write(Term), Iso_write(Term, Ops)), ExCtx), Ctx) :-
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

        Compile_body(writeq(Stream, Term), Iso_writeq(Stream, Term, Ops), Debug(goal(writeq(Stream, Term), Iso_writeq(Stream, Term, Ops)), ExCtx), Ctx) :-
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

        Compile_body(writeq(Term), Iso_writeq(Term, Ops), Debug(goal(writeq(Term), Iso_writeq(Term, Ops)), ExCtx), Ctx) :-
        bagof(op(Pr, Spec, Op), Scope^pp_entity_operator_(Pr, Spec, Op, Scope), Ops),
        CompCtx_execCtx(Ctx, ExCtx),
        !.

   //Logtalk flag predicates

        Compile_body(setLogtalkFlag(Flag, Value), TPred, Debug(goal(DPred, TPred), ExCtx), Ctx) :-
        nonvar(Flag),
        nonvar(Value),
        !,
        Check(read_writeFlag, Flag),
        Check(flag_value, Flag + Value),
        TPred = SetCompilerFlag(Flag, Value),
        DPred = setLogtalkFlag(Flag, Value),
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(setLogtalkFlag(Flag, Value), TPred, Debug(goal(DPred, TPred), ExCtx), Ctx) :-
        !,
        Check(var_orRead_writeFlag, Flag),
        TPred = SetLogtalkFlag(Flag, Value, ExCtx),
        DPred = setLogtalkFlag(Flag, Value),
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(currentLogtalkFlag(Flag, Value), TPred, Debug(goal(DPred, TPred), ExCtx), Ctx) :-
        nonvar(Flag),
        nonvar(Value),
        !,
        Check(flag, Flag),
        Check(flag_value, Flag + Value),
        TPred = CompilerFlag(Flag, Value),
        DPred = currentLogtalkFlag(Flag, Value),
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(currentLogtalkFlag(Flag, Value), TPred, Debug(goal(DPred, TPred), ExCtx), Ctx) :-
        !,
        Check(var_orFlag, Flag),
        TPred = CurrentLogtalkFlag(Flag, Value, ExCtx),
        DPred = currentLogtalkFlag(Flag, Value),
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_body(createLogtalkFlag(Flag, Value, Options), TPred, Debug(goal(DPred, TPred), ExCtx), Ctx) :-
        !,
        Check(atom, Flag),
        Check(ground, Value),
        Check(ground, Options),
        Check(list, Options),
        TPred = CreateLogtalkFlag(Flag, Value, Options, ExCtx),
        DPred = createLogtalkFlag(Flag, Value, Options),
        CompCtx_execCtx(Ctx, ExCtx).

   //Prolog flag predicates (just basic error and portability checking)

        Compile_body(set_prologFlag(Flag, _), _, _, Ctx) :-
        Check(var_orAtom, Flag),
        nonvar(Flag),
        CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(portability, warning),
        \+ IsoSpecFlag(Flag),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(portability), core, nonStandard_prologFlag(File, Lines, Type, Entity, Flag))
        ;	print_message(warning(portability), core, nonStandard_prologFlag(File, Lines, Flag))
        ),
        fail.

        Compile_body(set_prologFlag(Flag, Value), _, _, Ctx) :-
        nonvar(Flag),
        nonvar(Value),
        CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(portability, warning),
        IsoSpecFlag(Flag),
        \+ IsoSpecFlag_value(Flag, Value),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(portability), core, nonStandard_prologFlag_value(File, Lines, Type, Entity, Flag, Value))
        ;	print_message(warning(portability), core, nonStandard_prologFlag_value(File, Lines, Flag, Value))
        ),
        fail.

        Compile_body(current_prologFlag(Flag, _), _, _, Ctx) :-
        Check(var_orAtom, Flag),
        nonvar(Flag),
        CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(portability, warning),
        \+ IsoSpecFlag(Flag),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(portability), core, nonStandard_prologFlag(File, Lines, Type, Entity, Flag))
        ;	print_message(warning(portability), core, nonStandard_prologFlag(File, Lines, Flag))
        ),
        fail.

        Compile_body(current_prologFlag(Flag, Value), _, _, Ctx) :-
        nonvar(Flag),
        nonvar(Value),
        CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(portability, warning),
        IsoSpecFlag(Flag),
        \+ IsoSpecFlag_value(Flag, Value),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        (	pp_entity_(Type, Entity, _, _, _) ->
        print_message(warning(portability), core, nonStandard_prologFlag_value(File, Lines, Type, Entity, Flag, Value))
        ;	print_message(warning(portability), core, nonStandard_prologFlag_value(File, Lines, Flag, Value))
        ),
        fail.

   //arithmetic predicates (portability checks)

        Compile_body(_ is Exp, _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(user)),
        CompilerFlag(portability, warning),
        Check_non_portableFunctions(Exp),
        fail.
        Compile_body(Exp1 =:= Exp2, _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(user)),
        CompilerFlag(portability, warning),
        Check_non_portableFunctions(Exp1),
        Check_non_portableFunctions(Exp2),
        fail.
        Compile_body(Exp1 =\= Exp2, _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(user)),
        CompilerFlag(portability, warning),
        Check_non_portableFunctions(Exp1),
        Check_non_portableFunctions(Exp2),
        fail.
        Compile_body(Exp1 < Exp2, _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(user)),
        CompilerFlag(portability, warning),
        Check_non_portableFunctions(Exp1),
        Check_non_portableFunctions(Exp2),
        fail.
        Compile_body(Exp1 =< Exp2, _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(user)),
        CompilerFlag(portability, warning),
        Check_non_portableFunctions(Exp1),
        Check_non_portableFunctions(Exp2),
        fail.
        Compile_body(Exp1 > Exp2, _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(user)),
        CompilerFlag(portability, warning),
        Check_non_portableFunctions(Exp1),
        Check_non_portableFunctions(Exp2),
        fail.
        Compile_body(Exp1 >= Exp2, _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(user)),
        CompilerFlag(portability, warning),
        Check_non_portableFunctions(Exp1),
        Check_non_portableFunctions(Exp2),
        fail.

   //blackboard predicates (requires a backend Prolog compiler natively supporting these built-in predicates)

        Compile_body(bb_put(Key, Term), TPred, DPred, Ctx) :-
        prolog_builtIn_predicate(bb_put(_, _)),
        \+ ppDefines_predicate_(bb_put(_, _), _, _, _, _, _),
        !,
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, ExCtx, _, _, _),
        (	atomic(Key) ->
        Compile_bb_key(Key, Prefix, TKey),
        TPred = bb_put(TKey, Term),
        DPred = Debug(goal(bb_put(Key, Term), TPred), ExCtx)
        ;	var(Key) ->
   //runtime key translation
        TPred = (Compile_bb_key(Key, Prefix, TKey, bb_put(Key, Term)), bb_put(TKey, Term)),
        DPred = Debug(goal(bb_put(Key, Term), TPred), ExCtx)
        ;	throw(type_error(atomic, Key))
        ).

        Compile_body(bb_get(Key, Term), TPred, DPred, Ctx) :-
        prolog_builtIn_predicate(bb_get(_, _)),
        \+ ppDefines_predicate_(bb_get(_, _), _, _, _, _, _),
        !,
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, ExCtx, _, _, _),
        (	atomic(Key) ->
        Compile_bb_key(Key, Prefix, TKey),
        TPred = bb_get(TKey, Term),
        DPred = Debug(goal(bb_get(Key, Term), TPred), ExCtx)
        ;	var(Key) ->
   //runtime key translation
        TPred = (Compile_bb_key(Key, Prefix, TKey, bb_get(Key, Term)), bb_get(TKey, Term)),
        DPred = Debug(goal(bb_get(Key, Term), TPred), ExCtx)
        ;	throw(type_error(atomic, Key))
        ).

        Compile_body(bbDelete(Key, Term), TPred, DPred, Ctx) :-
        prolog_builtIn_predicate(bbDelete(_, _)),
        \+ ppDefines_predicate_(bbDelete(_, _), _, _, _, _, _),
        !,
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, ExCtx, _, _, _),
        (	atomic(Key) ->
        Compile_bb_key(Key, Prefix, TKey),
        TPred = bbDelete(TKey, Term),
        DPred = Debug(goal(bbDelete(Key, Term), TPred), ExCtx)
        ;	var(Key) ->
   //runtime key translation
        TPred = (Compile_bb_key(Key, Prefix, TKey, bbDelete(Key, Term)), bbDelete(TKey, Term)),
        DPred = Debug(goal(bbDelete(Key, Term), TPred), ExCtx)
        ;	throw(type_error(atomic, Key))
        ).

        Compile_body(bb_update(Key, Term, New), TPred, DPred, Ctx) :-
        prolog_builtIn_predicate(bb_update(_, _, _)),
        \+ ppDefines_predicate_(bb_update(_, _, _), _, _, _, _, _),
        !,
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, ExCtx, _, _, _),
        (	atomic(Key) ->
        Compile_bb_key(Key, Prefix, TKey),
        TPred = bb_update(TKey, Term, New),
        DPred = Debug(goal(bb_update(Key, Term, New), TPred), ExCtx)
        ;	var(Key) ->
   //runtime key translation
        TPred = (Compile_bb_key(Key, Prefix, TKey, bb_update(Key, Term, New)), bb_update(TKey, Term, New)),
        DPred = Debug(goal(bb_update(Key, Term, New), TPred), ExCtx)
        ;	throw(type_error(atomic, Key))
        ).

   //call/2-N built-in control construct

        Compile_body(CallN, TPred, DPred, Ctx) :-
        functor(CallN, call, Arity),
        Arity >= 2,
        CallN =.. [call, Closure| ExtraArgs],
        !,
        CheckClosure(Closure, Ctx),
        Compile_body(CallN(Closure, ExtraArgs), TPred, DPred, Ctx).

   //call to a meta-predicate from a user-defined meta-predicate;
   //must check the number of arguments for shared closures
        %
   //note that getting the meta-predicate template for non-declared
   //built-in meta-predicates or for module meta-predicates is fragile
   //due to lack of standardization of meta-predicate specifications

        Compile_body(Pred, _, _, Ctx) :-
        CompCtx(Ctx, Head, _, _, _, _, _, _, [_| _], _, _, compile(_), _, _),
   //we're compiling a clause for a meta-predicate as the list of meta-variables is not empty
        (	pp_meta_predicate_(Pred, Meta) ->
   //user-defined meta-predicate
        true
        ;	prolog_meta_predicate(Pred, Meta, predicate) ->
   //proprietary built-in meta-predicate declared in the adapter files
        true
        ;	predicate_property(Pred, builtIn),
        catch(predicate_property(Pred, meta_predicate(Meta)), _, fail) ->
   //non-declared proprietary built-in meta-predicate
        true
        ;	pp_use_module_predicate_(Module, Original, Pred, _),
        catch(predicate_property(':(Module, Original), meta_predicate(Meta)), _, fail) ->
   //meta-predicates specified in a use_module/2 directive
        true
        ;	pp_uses_predicate_(user, Original, Pred, _),
        catch(predicate_property(Original, meta_predicate(Meta)), _, fail) ->
   //Prolog meta-predicate undeclared in the adapter file (may not be a built-in)
        true
        ;	fail
        ),
        Pred =.. [_| PredArgs],
        Meta =.. [_| MetaArgs],
        prolog_toLogtalk_metaArgumentSpecifiers(MetaArgs, CMetaArgs),
        nonvar(Head),
   //ignore multifile predicates
        Head \= ':(_, _),
        Head \= _::_,
        pp_meta_predicate_(Head, HeadMeta),
        Head =.. [_| HeadArgs],
        HeadMeta =.. [_| HeadMetaArgs],
        Same_number_ofClosure_extraArgs(PredArgs, CMetaArgs, HeadArgs, HeadMetaArgs),
        fail.

   //predicates specified in use_module/2 directives

        Compile_body(Alias, TPred, Debug(goal(Alias, TPred), ExCtx), Ctx) :-
        pp_use_module_predicate_(Module, Pred, Alias, _),
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        AddReferenced_module_predicate(Mode, Module, Pred, Alias, Head),
        Compile_body(':(Module,Pred), TPred, _, Ctx).

   //predicates specified in uses/2 directives
        %
   //in the case of predicates defined in the pseudo-object "user", the uses/2
   //directive is typically used to help document dependencies on Prolog-defined
   //predicates (usually, but not necessarily, built-in predicates)

        Compile_body(Alias, TPred, DPred, Ctx) :-
        pp_uses_predicate_(Obj, Pred, Alias, _),
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        (	Obj == user ->
        (	(	prolog_meta_predicate(Pred, Meta, Type)
   //built-in Prolog meta-predicate declared in the adapter file in use
        ;	catch(predicate_property(Pred, meta_predicate(Meta)), _, fail)
   //Prolog meta-predicate undeclared in the adapter file (may not be a built-in)
        ) ->
   //meta-predicate
        Pred =.. [Functor| Args],
        Meta =.. [Functor| MArgs],
        (	prolog_toLogtalk_metaArgumentSpecifiers(MArgs, CMArgs),
        Compile_prolog_metaArguments(Args, CMArgs, Ctx, TArgs, DArgs) ->
        TPred =.. [Functor| TArgs],
        DGoal =.. [Functor| DArgs],
        (	Type == controlConstruct ->
        DPred = DGoal
        ;	DPred = Debug(goal(Alias, DGoal), ExCtx)
        )
        ;	% meta-predicate template is not usable
        throw(domain_error(meta_predicate_template, Meta))
        )
        ;	% non meta-predicate
        TPred = Pred,
        DPred = Debug(goal(Alias, Pred), ExCtx),
        CompCtx_execCtx(Ctx, ExCtx)
        ),
        AddReferenced_object_message(Mode, Obj, Pred, Alias, Head)
        ;	% objects other than the pseudo-object "user"
        AddReferenced_object_message(Mode, Obj, Pred, Alias, Head),
        Compile_body(Obj::Pred, TPred, _, Ctx),
        DPred = Debug(goal(Alias, TPred), ExCtx)
        ).

   //goal is a call to a dynamic predicate within a category; the predicate is called
   //instead in the object importing the category (implicit dynamic binding)

        Compile_body(Pred, TPred, Debug(goal(Pred, TPred), ExCtx), Ctx) :-
        ppCategory_(_, _, _, _, _, _),
        ppDynamic_(Pred),
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = CallIn_this(Pred, ExCtx).

   //non-callable terms

        Compile_body(Pred, _, _, _) :-
        \+ callable(Pred),
        throw(type_error(callable, Pred)).

   //runtime compilation of a call (usually a meta-call) to a user-defined predicate
        %
   //required to deal with meta-calls instantiated at runtime

        Compile_body(Pred, TPred, Debug(goal(Pred, TPred), ExCtx), Ctx) :-
        CompCtx(Ctx, _, _, Entity, Sender, This, Self, _, MetaVars, MetaCallCtx, ExCtx, runtime, Stack, _),
        nonvar(Entity),
   //in the most common case, we're meta-calling the predicate
        executionContext(ExCtx, Entity, Sender, This, Self, MetaCallCtx, Stack),
        (	member_var(Pred, MetaVars) ->
        MetaCallCtx = CallerExCtx-_,
   //goal is a call to a user-defined predicate in sender (i.e. a meta-argument)
        TPred = metacallSender(Pred, ExCtx, CallerExCtx, [])
        ;	% goal is a local call to a user-defined predicate
        Current_object_(Entity, _, _, Def, _, _, _, _, DDef, _, _) ->
        (	call(Def, Pred, ExCtx, TPred)
        ;	call(DDef, Pred, ExCtx, TPred)
        )
        ;	CurrentCategory_(Entity, _, _, Def, _, _),
        call(Def, Pred, ExCtx, TPred)
        ),
        !.

   //call to a local user-defined predicate

        Compile_body(Pred, TPred, DPred, Ctx) :-
        CompCtx(Ctx, _, _, Entity, _, _, _, _, _, _, ExCtx, _, _, _),
        Entity == user,
   //usually a call from an initialization or conditional compilation directive
        !,
        TPred = Pred,
        DPred = Debug(goal(Pred, TPred), ExCtx).

        Compile_body(Pred, TPred, Debug(goal(DPred, TPred), ExCtx), Ctx) :-
        ppCoinductive_(Pred, _, ExCtx, TCPred, _, _, DCPred),
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, Prefix, _, _, ExCtx, Mode, _, _),
        (	ppDefines_predicate_(Pred, Functor/Arity, _, TPred0, _, _) ->
        CheckFor_trivialFails(Mode, Pred, TPred0, Head),
   //convert the call to the original coinductive predicate into a call to the auxiliary
   //predicate whose compiled normal and debug forms are already computed
        functor(TCPred, TCFunctor, TCArity),
        RememberCalled_predicate(Mode, Functor/Arity, TCFunctor/TCArity, Head),
        TPred = TCPred,
        DPred = DCPred
        ;	% undefined coinductive predicate
        functor(Pred, Functor, Arity),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        RememberCalled_predicate(Mode, Functor/Arity, TFunctor/TArity, Head),
   //closed-world assumption: calls to static, declared but undefined
   //predicates must fail instead of throwing an exception,
        Report_undefined_predicateCall(Mode, Functor/Arity),
        TPred = fail,
        DPred = Pred
        ).

        Compile_body(Pred, TPred, Debug(goal(Pred, TPred), ExCtx), Ctx) :-
        ppSynchronized_(Pred, Mutex),
        CompCtx(Ctx, Head, _, _, _, _, _, Prefix, _, _, ExCtx, Mode, _, _),
        functor(Pred, Functor, Arity),
        \+ (nonvar(Head), functor(Head, Functor, Arity)),
   //not a recursive call
        !,
        (	ppDefines_predicate_(Pred, _, ExCtx, TPred0, _, _) ->
        CheckFor_trivialFails(Mode, Pred, TPred0, Head),
        (	prologFeature(threads, supported) ->
        TPred = with_mutex(Mutex, TPred0)
        ;	% in single-threaded systems, with_mutex/2 is equivalent to once/1
        TPred = once(TPred0)
        ),
        functor(TPred0, TFunctor, TArity)
        ;	% undefined synchronized predicate
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
   //closed-world assumption: calls to static, declared but undefined
   //predicates must fail instead of throwing an exception,
        Report_undefined_predicateCall(Mode, Functor/Arity),
        TPred = fail
        ),
        RememberCalled_predicate(Mode, Functor/Arity, TFunctor/TArity, Head).

        Compile_body(Pred, TPred, Debug(goal(Pred, TPred), ExCtx), Ctx) :-
        ppDefines_predicate_(Pred, Functor/Arity, ExCtx, TPred0, _, _),
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        CheckFor_trivialFails(Mode, Pred, TPred0, Head),
        functor(TPred0, TFunctor, TArity),
        (	pp_meta_predicate_(Pred, Meta),
   //local user-defined meta-predicate
        Pred =.. [Functor| Args],
        Meta =.. [Functor| MArgs],
        CompileStatic_binding_metaArguments(Args, MArgs, Ctx, TArgs0) ->
        Append(TArgs0, [ExCtx], TArgs),
        TPred =.. [TFunctor| TArgs]
        ;	% non meta-predicate or runtime compilation of meta-arguments
        TPred = TPred0
        ),
        RememberCalled_predicate(Mode, Functor/Arity, TFunctor/TArity, Head).

   //call to a declared but undefined predicate

        Compile_body(Pred, TPred, Debug(goal(Pred, TPred), ExCtx), Ctx) :-
        (	ppDynamic_(Pred)
        ;	pp_multifile_(Pred, _, _)
        ),
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, Prefix, _, _, ExCtx, Mode, _, _),
        functor(Pred, Functor, Arity),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        functor(TPred, TFunctor, TArity),
        unify_head_theadArguments(Pred, TPred, ExCtx),
        RememberCalled_predicate(Mode, Functor/Arity, TFunctor/TArity, Head).

        Compile_body(Pred, fail, Debug(goal(Pred, fail), ExCtx), Ctx) :-
        functor(Pred, Functor, Arity),
        (	pp_public_(Functor, Arity)
        ;	pp_protected_(Functor, Arity)
        ;	pp_private_(Functor, Arity)
        ;	ppSynchronized_(Pred, _)
        ;	ppCoinductive_(Pred, _, _, _, _, _, _)
        ;	ppDiscontiguous_(Pred)
        ),
        !,
   //closed-world assumption: calls to static, non-multifile, declared
   //but undefined predicates must fail instead of throwing an exception
        CompCtx(Ctx, Head, _, _, _, _, _, Prefix, _, _, ExCtx, Mode, _, _),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        RememberCalled_predicate(Mode, Functor/Arity, TFunctor/TArity, Head),
        Report_undefined_predicateCall(Mode, Functor/Arity).

   //call to a Prolog built-in predicate

        Compile_body(Pred, TPred, DPred, Ctx) :-
        prolog_builtIn_predicate(Pred),
        !,
        (	(	prolog_meta_predicate(Pred, Meta, Type) ->
   //built-in Prolog meta-predicate declared in the adapter file in use
        true
        ;	% lack of standardization of the predicate_property/2 predicate
   //means that the next call may fail to recognize the predicate as
   //a meta-predicate and retrieve a usable meta-predicate template
        catch(predicate_property(Pred, meta_predicate(Meta)), _, fail)
        ) ->
   //meta-predicate
        Pred =.. [Functor| Args],
        Meta =.. [Functor| MArgs],
        (	prolog_toLogtalk_metaArgumentSpecifiers(MArgs, CMArgs),
        Compile_prolog_metaArguments(Args, CMArgs, Ctx, TArgs, DArgs) ->
        TGoal =.. [Functor| TArgs],
        DGoal =.. [Functor| DArgs],
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        TPred = TGoal,
        (	Type == controlConstruct ->
        DPred = DGoal
        ;	DPred = Debug(goal(Pred, DGoal), ExCtx)
        ),
        Check_non_portable_prolog_builtInCall(Mode, Pred)
        ;	% meta-predicate template is not usable
        throw(domain_error(meta_predicate_template, Meta))
        )
        ;	% non meta-predicate
        TPred = Pred,
        DPred = Debug(goal(Pred, Pred), ExCtx),
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        Check_non_portable_prolog_builtInCall(Mode, Pred),
        CheckFor_tautology_orFalsehood_goal(Mode, Pred)
        ).

   //call to a Logtalk built-in predicate (that is not already handled)

        Compile_body(Pred, Pred, Debug(goal(Pred, Pred), ExCtx), Ctx) :-
        Logtalk_builtIn_predicate(Pred, _),
        !,
        CompCtx_execCtx(Ctx, ExCtx).

   //call to a unknown predicate

        Compile_body(Pred, TPred, Debug(goal(Pred, TPred), ExCtx), Ctx) :-
        CompCtx(Ctx, Head, _, _, _, _, _, Prefix, _, _, ExCtx, Mode, _, _),
        functor(Pred, Functor, Arity),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        functor(TPred, TFunctor, TArity),
        unify_head_theadArguments(Pred, TPred, ExCtx),
        RememberCalled_predicate(Mode, Functor/Arity, TFunctor/TArity, Head),
        Report_unknown_predicateCall(Mode, Functor/Arity).



   //bagof/3 and setof/3 existential quantifiers

        Compile_quantified_body(Term^Pred, Term^TPred, Term^DPred, Ctx) :-
        !,
        (	var(Pred) ->
   //meta-call resolved at runtime
        Compile_body(Pred, TPred, DPred, Ctx)
        ;	% we can have Term1^Term2^...^Pred
        Compile_quantified_body(Pred, TPred, DPred, Ctx)
        ).

        Compile_quantified_body(Pred, TPred, DPred, Ctx) :-
        Compile_body(Pred, TPred, DPred, Ctx).



   //FixDisjunctionLeftSide(@callable, -callable)
        %
   //check if the compilation of the disjunction left-side produced an if-then or
   //a soft-cut (e.g. due to goal-expansion) and fix it if necessary to avoid
   //converting the disjunction into an if-then-else or a soft-cut with an else part

        FixDisjunctionLeftSide(Goal0, Goal) :-
        (	Goal0 = (_ -> _) ->
        Goal = (Goal0, true)
        ;	Goal0 = '*->(_, _),
        predicate_property('*->(_, _), builtIn) ->
        Goal = (Goal0, true)
        ;	Goal = Goal0
        ).



   //Compile_error_predicate(+compilationContext, -compound)
        %
   //compiles a call to one of the built-in error predicates;
   //these predicates are shorthands to context/1 + throw/1

        Compile_error_predicate(Exception, TPred, DPred, Ctx) :-
        CompCtx_head(Ctx, Head0),
        (	Head0 = _::Head ->
   //object (or category) multifile predicate clause
        true
        ;	Head0 = ':(_,Head) ->
   //module multifile predicate clause
        true
        ;	% non-multifile predicate
        Head0 = Head
        ),
        CompCtx_head_execCtx(Ctx, ExCtx),
        TPred = throw(error(Exception, logtalk(Head, ExCtx))),
        DPred = Debug(goal(Exception, TPred), ExCtx).



   //CheckFor_meta_predicateDirective(@compilation_mode, @callable, @term)
        %
   //remember missing meta_predicate/1 directives

        CheckFor_meta_predicateDirective(runtime, _, _).

        CheckFor_meta_predicateDirective(compile(aux), _, _) :-
        !.

        CheckFor_meta_predicateDirective(compile(user), Head, MetaArg) :-
        term_template(Head, Template),
        (	pp_meta_predicate_(Template, _) ->
   //meta_predicate/1 directive is present
        true
        ;	pp_missing_meta_predicateDirective_(Template, _, _) ->
   //missing meta_predicate/1 directive already recorded
        true
        ;	term_variables(MetaArg, MetaArgVars),
        term_variables(Head, HeadVars),
        member(MetaArgVar, MetaArgVars),
        member_var(MetaArgVar, HeadVars) ->
   //the meta-argument is a head argument
        SourceFileContext(File, Lines),
   //delay reporting to the end of entity compilation to avoid repeated reports for
   //the same missing directive when a meta-predicate have two or more clauses
        assertz(pp_missing_meta_predicateDirective_(Template, File, Lines))
        ;	true
        ).



   //Check_non_portable_prolog_builtInCall(@compilation_mode, @callable)
        %
   //remember non-portable Prolog built-in predicate calls

        Check_non_portable_prolog_builtInCall(runtime, _).

        Check_non_portable_prolog_builtInCall(compile(aux), _) :-
        !.

        Check_non_portable_prolog_builtInCall(compile(user), Pred) :-
        (	\+ pp_non_portable_predicate_(Pred, _, _),
   //not already recorded as a non portable call
        \+ IsoSpec_predicate(Pred) ->
   //bona fide non-portable Prolog built-in predicate
        term_template(Pred, Template),
        SourceFileContext(File, Lines),
        assertz(pp_non_portable_predicate_(Template, File, Lines))
        ;	true
        ).



   //CheckFor_tautology_orFalsehood_goal(@compilation_mode, @callable)
        %
   //check for likely typos in calls to some Prolog built-in predicates
   //that result in either tautologies or falsehoods

        CheckFor_tautology_orFalsehood_goal(runtime, _).

        CheckFor_tautology_orFalsehood_goal(compile(aux), _) :-
        !.

        CheckFor_tautology_orFalsehood_goal(compile(user), Goal) :-
        (	(	Candidate_tautology_orFalsehood_goal(Goal)
        ;	Candidate_tautology_orFalsehood_goal_hook(Goal)
        ),
        ground(Goal),
        CompilerFlag(always_true_orFalse_goals, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        (	call(Goal) ->
        print_message(warning(always_true_orFalse_goals), core, goalIsAlways_true(File, Lines, Type, Entity, Goal))
        ;	print_message(warning(always_true_orFalse_goals), core, goalIsAlwaysFalse(File, Lines, Type, Entity, Goal))
        )
        ;	true
        ).



   //CheckFor_trivialFails(@compilation_mode, @callable, @callable, @callable)
        %
   //check for trivial fails due to no matching local clause being available for a goal;
   //this check is only performed for local static predicates as dynamic or multifile
   //predicates can get new clauses at runtime

        CheckFor_trivialFails(runtime, _, _, _).

        CheckFor_trivialFails(compile(aux), _, _, _) :-
        !.

        CheckFor_trivialFails(compile(user), Call, TCall, Head) :-
        (	CompilerFlag(trivial_goalFails, warning),
   //workaround possible creation of a cyclic term with some backend
   //Prolog compilers implementation of the \=2 predicate
        copy_term(Head, HeadCopy),
        Call \= HeadCopy,
   //not a recursive call which can originate from a predicate with a single clause
        \+ ppDynamic_(Call),
        \+ pp_multifile_(Call, _, _),
   //not a dynmaic or multifile predicate
        \+ pp_entity_term_(fact(TCall), _, _),
        \+ pp_entity_term_(srule(TCall, _, _), _, _),
        \+ pp_entity_term_(dfact(TCall, _), _, _),
        \+ pp_entity_term_(dsrule(TCall, _, _, _), _, _),
   //not a yet to be compiled user-defined fact or rule
        \+ ppFinal_entity_term_(TCall, _),
        \+ ppFinal_entity_term_((TCall :- _), _),
   //not an already compiled user-defined fact or rule
        \+ pp_entityAuxClause_(fact(TCall)),
        \+ pp_entityAuxClause_(srule(TCall, _, _)),
        \+ pp_entityAuxClause_(dfact(TCall, _)),
        \+ pp_entityAuxClause_(dsrule(TCall, _, _, _)),
   //not a yet to be compiled auxiliary fact or rule
        \+ ppFinal_entityAuxClause_(TCall),
        \+ ppFinal_entityAuxClause_((TCall :- _)) ->
   //not an already compiled auxiliary fact or rule
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(trivial_goalFails), core, no_matchingClauseFor_goal(File, Lines, Type, Entity, Call))
        ;	true
        ).



   //Candidate_tautology_orFalsehood_goal(@callable).

   //unification
        Candidate_tautology_orFalsehood_goal(_ = _).
        Candidate_tautology_orFalsehood_goal(unify_with_occursCheck(_, _)).
        Candidate_tautology_orFalsehood_goal(_ \= _).
   //term comparison
        Candidate_tautology_orFalsehood_goal(_ == _).
        Candidate_tautology_orFalsehood_goal(_ \== _).
        Candidate_tautology_orFalsehood_goal(_ @< _).
        Candidate_tautology_orFalsehood_goal(_ @=< _).
        Candidate_tautology_orFalsehood_goal(_ @> _).
        Candidate_tautology_orFalsehood_goal(_ @>= _).
   //arithmetic comparison
        Candidate_tautology_orFalsehood_goal(_ < _).
        Candidate_tautology_orFalsehood_goal(_ =< _).
        Candidate_tautology_orFalsehood_goal(_ > _).
        Candidate_tautology_orFalsehood_goal(_ >= _).
        Candidate_tautology_orFalsehood_goal(_ =:= _).
        Candidate_tautology_orFalsehood_goal(_ =\= _).
        Candidate_tautology_orFalsehood_goal(compare(_, _, _)).
   //entityKind testing
        Candidate_tautology_orFalsehood_goal(acyclic_term(_)).
        Candidate_tautology_orFalsehood_goal(atom(_)).
        Candidate_tautology_orFalsehood_goal(atomic(_)).
        Candidate_tautology_orFalsehood_goal(callable(_)).
        Candidate_tautology_orFalsehood_goal(compound(_)).
        Candidate_tautology_orFalsehood_goal(float(_)).
        Candidate_tautology_orFalsehood_goal(ground(_)).
        Candidate_tautology_orFalsehood_goal(integer(_)).
        Candidate_tautology_orFalsehood_goal(nonvar(_)).
        Candidate_tautology_orFalsehood_goal(number(_)).
        Candidate_tautology_orFalsehood_goal(var(_)).



   //RememberCalled_predicate(@compilation_mode, +predicateIndicator, +predicateIndicator, @callable)
        %
   //used for checking calls to undefined predicates and for collecting cross-referencing information

        RememberCalled_predicate(runtime, _, _, _).

        RememberCalled_predicate(compile(aux), _, _, _) :-
        !.

        RememberCalled_predicate(compile(user), Functor/Arity, TFunctor/TArity, Head) :-
   //currently, the returned line numbers are for the start and end lines of the clause containing the call
        (	Head = Object::Predicate ->
   //call from the body of a Logtalk multifile predicate clause
        Caller = Object::HeadFunctor/HeadArity
        ;	Head = ':(Module,Predicate) ->
   //call from the body of a Prolog module multifile predicate clause
        Caller = ':(Module,HeadFunctor/HeadArity)
        ;	% call from the body of a local entity clause
        Head = Predicate,
        Caller = HeadFunctor/HeadArity
        ),
        functor(Predicate, HeadFunctor, HeadArity),
        SourceFileContext(File, Lines),
        (	Caller == Functor/Arity ->
   //recursive call
        true
        ;	ppCalls_predicate_(Functor/Arity, _, Caller, File, Lines) ->
   //already recorded for the current clause being compiled
        true
        ;	assertz(ppCalls_predicate_(Functor/Arity, TFunctor/TArity, Caller, File, Lines))
        ).



   //RememberCalledSelf_predicate(@compilation_mode, +predicateIndicator, @callable)
        %
   //used for checking calls to undefined predicates and for collecting cross-referencing information

        RememberCalledSelf_predicate(runtime, _, _).

        RememberCalledSelf_predicate(compile(aux), _, _) :-
        !.

        RememberCalledSelf_predicate(compile(user), Functor/Arity, Head) :-
   //currently, the returned line numbers are for the start and end lines of the clause containing the call
        (	Head = Object::Predicate ->
   //call from the body of a Logtalk multifile predicate clause
        Caller = Object::HeadFunctor/HeadArity
        ;	Head = ':(Module,Predicate) ->
   //call from the body of a Prolog module multifile predicate clause
        Caller = ':(Module,HeadFunctor/HeadArity)
        ;	% call from the body of a local entity clause
        Head = Predicate,
        Caller = HeadFunctor/HeadArity
        ),
        functor(Predicate, HeadFunctor, HeadArity),
        SourceFileContext(File, Lines),
        (	ppCallsSelf_predicate_(Functor/Arity, Caller, File, Lines) ->
   //already recorded for the current clause being compiled (however unlikely!)
        true
        ;	assertz(ppCallsSelf_predicate_(Functor/Arity, Caller, File, Lines))
        ).



   //RememberCalledSuper_predicate(@compilation_mode, +predicateIndicator, @callable)
        %
   //used for checking calls to undefined predicates and for collecting cross-referencing information

        RememberCalledSuper_predicate(runtime, _, _).

        RememberCalledSuper_predicate(compile(aux), _, _) :-
        !.

        RememberCalledSuper_predicate(compile(user), Functor/Arity, Head) :-
   //currently, the returned line numbers are for the start and end lines of the clause containing the call
        (	Head = Object::Predicate ->
   //call from the body of a Logtalk multifile predicate clause
        Caller = Object::HeadFunctor/HeadArity
        ;	Head = ':(Module,Predicate) ->
   //call from the body of a Prolog module multifile predicate clause
        Caller = ':(Module,HeadFunctor/HeadArity)
        ;	% call from the body of a local entity clause
        Head = Predicate,
        Caller = HeadFunctor/HeadArity
        ),
        functor(Predicate, HeadFunctor, HeadArity),
        SourceFileContext(File, Lines),
        (	ppCallsSuper_predicate_(Functor/Arity, Caller, File, Lines) ->
   //already recorded for the current clause being compiled (however unlikely!)
        true
        ;	assertz(ppCallsSuper_predicate_(Functor/Arity, Caller, File, Lines))
        ).



   //Remember_updated_predicate(@compilation_mode, @term, @callable)
        %
   //used for collecting cross-referencing information

        Remember_updated_predicate(runtime, _, _).

        Remember_updated_predicate(compile(aux), _, _) :-
        !.

        Remember_updated_predicate(compile(user), Dynamic, Head) :-
   //currently, the returned line numbers are for the start and end lines of the clause containing the call
        (	Head = Object::Predicate ->
   //update from the body of a Logtalk multifile predicate clause
        Updater = Object::HeadFunctor/HeadArity
        ;	Head = ':(Module,Predicate) ->
   //update from the body of a Prolog module multifile predicate clause
        Updater = ':(Module,HeadFunctor/HeadArity)
        ;	% update from the body of a local entity clause
        Head = Predicate,
        Updater = HeadFunctor/HeadArity
        ),
        functor(Predicate, HeadFunctor, HeadArity),
        SourceFileContext(File, Lines),
        (	pp_updates_predicate_(Dynamic, Updater, File, Lines) ->
   //already recorded for the current clause being compiled (however unlikely!)
        true
        ;	assertz(pp_updates_predicate_(Dynamic, Updater, File, Lines))
        ).



   //bagof(?term, ?term, ?term, +executionContext)
        %
   //handles bagof/3 calls with goals only known at runtime

        bagof(Term, QGoal, List, ExCtx) :-
        Convert_quantified_goal(QGoal, Goal, quantified_metacall(QGoal, Goal, ExCtx), TQGoal),
        bagof(Term, TQGoal, List).



   //Setof(?term, ?term, ?term, +executionContext)
        %
   //handles setof/3 calls with goals only known at runtime

        Setof(Term, QGoal, List, ExCtx) :-
        Convert_quantified_goal(QGoal, Goal, quantified_metacall(QGoal, Goal, ExCtx), TQGoal),
        setof(Term, TQGoal, List).



   //Convert_quantified_goal(@callable, -callable, +callable, -callable)
        %
   //converts a ^/2 goal at runtime (used with bagof/3 and setof/3 calls)
        %
   //returns both the original goal without existential variables and the compiled
   //goal that will be used as the argument for the bagof/3 and setof/3 calls

        Convert_quantified_goal(Goal, Goal, TGoal, TGoal) :-
        var(Goal),
        !.

        Convert_quantified_goal(Var^Term, Goal, TGoal, Var^TTerm) :-
        !,
        Convert_quantified_goal(Term, Goal, TGoal, TTerm).

        Convert_quantified_goal(Goal, Goal, TGoal, TGoal).



   //generateAux_predicateFunctor(+atom, -atom)
        %
   //generates a new functor for an auxiliary predicate
   //based on a base atom and an entity global counter

        generateAux_predicateFunctor(Base, Functor) :-
        (	retract(ppAux_predicateCounter_(Old)) ->
        New is Old + 1
        ;	New is 1
        ),
        asserta(ppAux_predicateCounter_(New)),
        numberCodes(New, NewCodes),
        atomCodes(NewAtom, NewCodes),
        atomConcat(Base, NewAtom, Functor).



   //Compile_bb_key(@term, +atom, -atom)
        %
   //compile-time translation of a blackboard key

        Compile_bb_key(Key, Prefix, TKey) :-
        (	atom(Key) ->
        atomConcat(Prefix, Key, TKey)
        ;	integer(Key) ->
        numberCodes(Key, KeyCodes),
        atomCodes(AtomKey, KeyCodes),
        atomConcat(Prefix, AtomKey, TKey)
        ;	throw(type_error(atomic, Key))
        ).



   //Compile_bb_key(@term, +atom, -atom, @callable)
        %
   //runtime translation of a blackboard key

        Compile_bb_key(Key, Prefix, TKey, Goal) :-
        (	var(Key) ->
        throw(error(instantiation_error, Goal))
        ;	atomic(Key) ->
        Compile_bb_key(Key, Prefix, TKey)
        ;	throw(error(type_error(atomic, Key), Goal))
        ).



   //Compile_threadedCall(+callable, -callable)
        %
   //compiles the argument of a call to the built-in predicate threaded/1

        Compile_threadedCall((TGoal; TGoals), threaded_or(Queue, MTGoals, Results)) :-
        !,
        Compile_threaded_orCall((TGoal; TGoals), Queue, MTGoals, Results).

        Compile_threadedCall((TGoal, TGoals), threadedAnd(Queue, MTGoals, Results)) :-
        !,
        Compile_threadedAndCall((TGoal, TGoals), Queue, MTGoals, Results).

        Compile_threadedCall(TGoal, (TGoal -> true; fail)).


        Compile_threaded_orCall((TGoal; TGoals), Queue, (MTGoal, MTGoals), [Result| Results]) :-
        !,
        Compile_threaded_goal(TGoal, Queue, MTGoal, Result),
        Compile_threaded_orCall(TGoals, Queue, MTGoals, Results).

        Compile_threaded_orCall(TGoal, Queue, MTGoal, [Result]) :-
        Compile_threaded_goal(TGoal, Queue, MTGoal, Result).


        Compile_threadedAndCall((TGoal, TGoals), Queue, (MTGoal, MTGoals), [Result| Results]) :-
        !,
        Compile_threaded_goal(TGoal, Queue, MTGoal, Result),
        Compile_threadedAndCall(TGoals, Queue, MTGoals, Results).

        Compile_threadedAndCall(TGoal, Queue, MTGoal, [Result]) :-
        Compile_threaded_goal(TGoal, Queue, MTGoal, Result).

        Compile_threaded_goal(TGoal, Queue, threaded_goal(TGoal, TVars, Queue, Id), id(Id, TVars, _)).



   //Compile_prolog_metaArguments(@list, @list, +compilationContext, -list, -list)
        %
   //compiles the meta-arguments contained in the list of arguments of a call to a Prolog
   //meta-predicate or meta-directive (assumes Logtalk meta-predicate notation)
        %
   //this predicate fails when meta-arguments other than goal and closures are not
   //sufficiently instantiated or a meta-argument mode indicator is not supported

        Compile_prolog_metaArguments([], [], _, [], []).

        Compile_prolog_metaArguments([Arg| Args], [MArg| MArgs], Ctx, [TArg| TArgs], [DArg| DArgs]) :-
        (	nonvar(Arg),
        module_metaArgument(MArg, Arg),
        prologFeature(modules, supported) ->
   //explicitly-qualified meta-argument
        TArg = Arg, DArg = Arg
        ;	integer(MArg),
        MArg > 0 ->
   //closure meta-argument
        Compile_prolog_metaArgument(closure(MArg), Arg, Ctx, TArg, DArg)
        ;	% remaining cases
        Compile_prolog_metaArgument(MArg, Arg, Ctx, TArg, DArg)
        ),
        Compile_prolog_metaArguments(Args, MArgs, Ctx, TArgs, DArgs).


        module_metaArgument(0, ':(_,_)).
        module_metaArgument(1, ':(_)).


        Compile_prolog_metaArgument(closure(N), Arg, Ctx, TArg, DArg) :-
   //closure
        Check(var_orCallable, Arg),
        Length(ExtArgs, 0, N),
        (	var(Arg) ->
        ExtArg =.. [call, Arg| ExtArgs]
        ;	extendClosure(Arg, ExtArgs, ExtArg) ->
        true
        ;	throw(domain_error(closure, Arg))
        ),
        Compile_body(ExtArg, TArg0, DArg0, Ctx),
   //generate an auxiliary predicate to allow the meta-predicate to extend
   //the closure without clashing with the execution-context argument
        generateAux_predicateFunctor('Closure_', HelperFunctor),
        pp_entity_(_, _, Prefix, _, _),
        atomConcat(Prefix, HelperFunctor, THelperFunctor),
        CompCtx_execCtx(Ctx, ExCtx),
        THelper =.. [THelperFunctor, Arg, ExCtx],
        TExtHelper =.. [THelperFunctor, Arg, ExCtx| ExtArgs],
        (	CompilerFlag(debug, on) ->
        assertz(pp_entityAuxClause_({(TExtHelper :- DArg0)}))
        ;	assertz(pp_entityAuxClause_({(TExtHelper :- TArg0)}))
        ),
        (	pp_object_(Entity, _, _, Def, _, _, _, _, _, _, _) ->
        true
        ;	ppCategory_(Entity, _, _, Def, _, _)
        ),
   //add a def clause to ensure that we don't loose track of the auxiliary clause
        Arity is N + 2,
        Length(TemplateArgs, 0, Arity),
        ExtHelperTemplate =.. [HelperFunctor| TemplateArgs],
        TExtHelperTemplate =.. [THelperFunctor| TemplateArgs],
        Clause =.. [Def, ExtHelperTemplate, _, TExtHelperTemplate],
        assertz(ppDef_(Clause)),
   //add, if applicable, source data information for the auxiliary clause
        (	CompilerFlag(sourceData, on) ->
        assertz(ppRuntimeClause_(predicate_property_(Entity, HelperFunctor/Arity, flagsClausesRulesLocation(1,1,1,0))))
        ;	true
        ),
        (	prologFeature(modules, supported) ->
   //make sure the calls are made in the correct context
        user_module_qualification(THelper, TArg),
        user_module_qualification(THelper, DArg)
        ;	TArg = THelper,
        DArg = THelper
        ).

        Compile_prolog_metaArgument((*), Arg, _, Arg, Arg).

        Compile_prolog_metaArgument((0), Arg, Ctx, TArg, DArg) :-
   //goal
        Compile_body(Arg, TArg0, DArg0, Ctx),
        (	TArg0 = ':(_, _) ->
   //the compiled call is already explicitly-qualified
        TArg = TArg0,
        DArg = DArg0
        ;	prologFeature(modules, supported) ->
   //make sure the calls are made in the correct context
        user_module_qualification(TArg0, TArg),
        user_module_qualification(DArg0, DArg)
        ;	TArg = TArg0,
        DArg = DArg0
        ).

        Compile_prolog_metaArgument((^), Arg, Ctx, TArg, DArg) :-
   //existentially-quantified goal
        (	Arg = Vars^Arg0 ->
        Compile_body(Arg0, TArg0, DArg0, Ctx),
        TArg = Vars^TArg0,
        DArg = Vars^DArg0
        ;	Compile_body(Arg, TArg, DArg, Ctx)
        ).

        Compile_prolog_metaArgument([0], [], _, [], []) :- !.
        Compile_prolog_metaArgument([0], [Arg| Args], Ctx, [TArg| TArgs], [DArg| DArgs]) :-
        Compile_prolog_metaArgument((0), Arg, Ctx, TArg, DArg),
        Compile_prolog_metaArgument([0], Args, Ctx, TArgs, DArgs).

        Compile_prolog_metaArgument((/), [Arg| Args], Ctx, [TArg| TArgs], [DArg| DArgs]) :-
        !,
        nonvar(Arg),
        Compile_prolog_metaArgument((/), Arg, Ctx, TArg, DArg),
        Compile_prolog_metaArgument([/], Args, Ctx, TArgs, DArgs).
        Compile_prolog_metaArgument((/), (Arg, Args), Ctx, (TArg, TArgs), (DArg, DArgs)) :-
        !,
        nonvar(Arg),
        Compile_prolog_metaArgument((/), Arg, Ctx, TArg, DArg),
        Compile_prolog_metaArgument((/), Args, Ctx, TArgs, DArgs).
        Compile_prolog_metaArgument((/), Arg, _, TArg, TArg) :-
        Compile_predicateIndicators(Arg, _, TArg0),
        (	prologFeature(modules, supported) ->
   //make sure the predicate indicator refers to the correct context
        user_module_qualification(TArg0, TArg)
        ;	TArg = TArg0
        ).

        Compile_prolog_metaArgument((//), Args, Ctx, TArgs, DArgs) :-
        Compile_prolog_metaArgument((/), Args, Ctx, TArgs, DArgs).

        Compile_prolog_metaArgument([/], [], _, [], []) :- !.
        Compile_prolog_metaArgument([/], [Arg| Args], Ctx, [TArg| TArgs], [DArg| DArgs]) :-
        nonvar(Arg),
        Compile_prolog_metaArgument((/), Arg, Ctx, TArg, DArg),
        Compile_prolog_metaArgument([/], Args, Ctx, TArgs, DArgs).

        Compile_prolog_metaArgument([//], Args, Ctx, TArgs, DArgs) :-
        Compile_prolog_metaArgument([/], Args, Ctx, TArgs, DArgs).



   //extendClosure(@callable, @list(term), -callable)
        %
   //extends a closure by appending a list of arguments to construct a goal
        %
   //this predicate fails if the closure can only be extended at runtime

        extendClosure(Obj::Closure, ExtArgs, Goal) :-
        Obj == user,
        !,
        extendClosure({Closure}, ExtArgs, Goal).

        extendClosure(Obj::Closure, ExtArgs, Obj::Msg) :-
        !,
        extendClosure_basic(Closure, ExtArgs, Msg).

        extendClosure([Obj::Closure], ExtArgs, [Obj::Msg]) :-
        !,
        extendClosure_basic(Closure, ExtArgs, Msg).

        extendClosure(::Closure, ExtArgs, ::Msg) :-
        !,
        extendClosure_basic(Closure, ExtArgs, Msg).

        extendClosure(^^Closure, ExtArgs, ^^Msg) :-
        !,
        extendClosure_basic(Closure, ExtArgs, Msg).

        extendClosure(Obj<<Closure, ExtArgs, Obj<<Goal) :-
        !,
        extendClosure_basic(Closure, ExtArgs, Goal).

        extendClosure({Closure}, ExtArgs, {Goal}) :-
        !,
        extendClosure_basic(Closure, ExtArgs, Goal).

        extendClosure(Free/Lambda, ExtArgs, Goal) :-
        !,
        Goal =.. [call, Free/Lambda| ExtArgs].

        extendClosure(Parameters>>Lambda, ExtArgs, Goal) :-
        !,
        Goal =.. [call, Parameters>>Lambda| ExtArgs].

        extendClosure(':(Module,Closure), ExtArgs, ':(Module,Goal)) :-
        !,
        extendClosure_basic(Closure, ExtArgs, Goal).

        extendClosure(Closure, ExtArgs, Goal) :-
        extendClosure_basic(Closure, ExtArgs, Alias),
        (	pp_uses_predicate_(Object, Original, Alias, _) ->
        Goal = Object::Original
        ;	pp_use_module_predicate_(Module, Original, Alias, _) ->
        Goal = ':(Module, Original)
        ;	Goal = Alias
        ).


        extendClosure_basic(Closure, ExtArgs, Goal) :-
        callable(Closure),
   //compile-time closure extension possible
        Closure =.. [Functor| Args],
        Append(Args, ExtArgs, FullArgs),
        Goal =.. [Functor| FullArgs].



   //notSame_metaArg_extraArgs(@list(nonvar), @list(var), @var, +integer, -compound)
        %
   //checks that the number of additional arguments being appended to a closure
   //in a call/N call matches the corresponding meta-predicate declaration
   //(the relative ordering of the meta-vars is the same of the corresponding
   //meta-arguments; assumes Logtalk meta-predicate notation)

        notSame_metaArg_extraArgs(MetaArgs, _, _, ExtraArgs, Domain) :-
        findall(Integer, (member(Integer, MetaArgs), integer(Integer)), Integers),
        SumList(Integers, Sum),
        Sum < ExtraArgs,
        !,
        (	Integers == [] ->
        Domain = {}
        ;	Integers = [MetaArg| _],
        Domain = {MetaArg}
        ).

        notSame_metaArg_extraArgs([(*)| MetaArgs], MetaVars, Closure, ExtraArgs, Domain) :-
        !,
        notSame_metaArg_extraArgs(MetaArgs, MetaVars, Closure, ExtraArgs, Domain).

        notSame_metaArg_extraArgs([(::)| MetaArgs], MetaVars, Closure, ExtraArgs, Domain) :-
        !,
        notSame_metaArg_extraArgs(MetaArgs, MetaVars, Closure, ExtraArgs, Domain).

        notSame_metaArg_extraArgs([0| MetaArgs], MetaVars, Closure, ExtraArgs, Domain) :-
        !,
        notSame_metaArg_extraArgs(MetaArgs, MetaVars, Closure, ExtraArgs, Domain).

        notSame_metaArg_extraArgs([MetaArg| _], [MetaVar| _], Closure, ExtraArgs, Domain) :-
        MetaVar == Closure,
        !,
        integer(MetaArg),
        MetaArg =\= ExtraArgs,
        Domain = {MetaArg}.

        notSame_metaArg_extraArgs([_| MetaArgs], [_| MetaVars], Closure, ExtraArgs, Domain) :-
        notSame_metaArg_extraArgs(MetaArgs, MetaVars, Closure, ExtraArgs, Domain).



   //Same_number_ofClosure_extraArgs(@list, @list, @list, @list)
        %
   //checks that the number of additional arguments being appended to a closure is kept
   //when passing a closure from the clause head to a meta-predicate call in the body

        Same_number_ofClosure_extraArgs([], _, _, _).

        Same_number_ofClosure_extraArgs([PredArg| PredArgs], [PredMetaArg| PredMetaArgs], HeadArgs, HeadMetaArgs) :-
        (	var(PredArg),
        integer(PredMetaArg), PredMetaArg > 0,
   //argument is a closure
        SharedClosureArg(PredArg, HeadArgs, HeadMetaArgs, HeadMetaArg) ->
   //shared closure argument
        (	PredMetaArg = HeadMetaArg ->
   //same number of closure extra args
        Same_number_ofClosure_extraArgs(PredArgs, PredMetaArgs, HeadArgs, HeadMetaArgs)
        ;	throw(domain_error({HeadMetaArg}, PredMetaArg))
        )
        ;	Same_number_ofClosure_extraArgs(PredArgs, PredMetaArgs, HeadArgs, HeadMetaArgs)
        ).


        SharedClosureArg(PredArg, [HeadArg| _], [HeadMetaArg| _], HeadMetaArg) :-
        PredArg == HeadArg.

        SharedClosureArg(PredArg, [_| HeadArgs], [_| HeadMetaArgs], HeadMetaArg) :-
        SharedClosureArg(PredArg, HeadArgs, HeadMetaArgs, HeadMetaArg).



   //CheckDynamicDirective(@compilation_mode, @term)
        %
   //checks for a dynamic/1 directive for a predicate that is an argument to the
   //database built-in methods; the predicate may be non-instantiated or only
   //partially instantiated but must be valid

        CheckDynamicDirective(runtime, _).

        CheckDynamicDirective(compile(_), Term) :-
        CheckDynamicDirective(Term).


        CheckDynamicDirective(Term) :-
        var(Term),
   //runtime binding argument
        !.

        CheckDynamicDirective((Head :- _)) :-
        !,
        CheckDynamicDirective(Head).

        CheckDynamicDirective(Functor/Arity) :-
        !,
        (	ground(Functor/Arity) ->
        functor(Head, Functor, Arity),
        CheckDynamicDirective(Head)
        ;	true
        ).

        CheckDynamicDirective(Head) :-
        (	ppDynamic_(Head) ->
   //dynamic/1 directive is present
        true
        ;	pp_missingDynamicDirective_(Head, _, _) ->
   //missing dynamic/1 directive already recorded
        true
        ;	term_template(Head, Template),
        SourceFileContext(File, Lines),
   //delay reporting to the end of entity compilation to avoid repeated reports for
   //the same missing directive when a dynamic predicate have two or more clauses
        assertz(pp_missingDynamicDirective_(Template, File, Lines))
        ).



   //CheckDiscontiguousDirective(@callable, @compilationContext)
        %
   //checks for a discontiguous/1 directive for a predicate

        CheckDiscontiguousDirective(Head, Ctx) :-
        retractall(pp_previous_predicate_(_, user)),
        assertz(pp_previous_predicate_(Head, user)),
        (	ppDiscontiguous_(Head) ->
   //discontiguous directive present
        true
        ;	pp_missingDiscontiguousDirective_(Head, _, _) ->
   //missing discontiguous/1 directive already recorded
        true
        ;	CompCtx_mode(Ctx, compile(user)) ->
   //compiling a source file clause; record missing discontiguous directive
        term_template(Head, Template),
        SourceFileContext(File, Lines),
   //delay reporting to the end of entity compilation to avoid repeated reports for the same
   //missing directive when there multiple discontiguous blocks for the same predicate
        assertz(pp_missingDiscontiguousDirective_(Template, File, Lines))
        ;	% runtime compilation or compiling an auxiliary predicate clause
        true
        ).



   //optimizableLocalDbCall(@term, -callable)
        %
   //checks if a call to a database built-in method can be optimized by direct
   //translation to a call to the corresponding Prolog built-in predicate

        optimizableLocalDbCall(Pred, TPred) :-
        nonvar(Pred),
   //only for objects
        pp_entity_(object, _, Prefix, _, _),
   //only for facts
        (	Pred = (Head :- Body) ->
        Body == true
        ;	Head = Pred
        ),
        callable(Head),
   //a dynamic directive must be present
        ppDynamic_(Head),
   //a scope directive must be present
        functor(Head, Functor, Arity),
        (	pp_public_(Functor, Arity)
        ;	pp_protected_(Functor, Arity)
        ;	pp_private_(Functor, Arity)
        ), !,
   //not compiled in debug mode
        CompilerFlag(debug, off),
   //compile the fact
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        functor(TPred, TFunctor, TArity),
        unify_head_theadArguments(Head, TPred, _).



   //DbCallDatabase_executionContext(@term, @term, -term, +executionContext)
        %
   //returns the database where a database method call should take place and sets the
   //execution context accordingly
        %
   //this auxiliary predicate ensures that, when calling database methods in the body
   //of a multifile predicate clause defined in an object, the object database will be
   //used instead of the database of the entity hoding the multifile predicate primary
   //declaration (which could be a category, making the calls invalid)

        DbCallDatabase_executionContext(Entity, This, Database, ExCtx) :-
        (	pp_entity_(object, _, _, _, _) ->
        Database = Entity,
        executionContext_this_entity(ExCtx, _, Entity)
        ;	% category
        Database = This,
        executionContext_this_entity(ExCtx, This, _)
        ).



   //RuntimeCheckedDbClause(@term)
        %
   //true if the argument forces runtime validity check

        RuntimeCheckedDbClause(Pred) :-
        var(Pred),
        !.

        RuntimeCheckedDbClause((Head :- Body)) :-
        var(Head),
        !,
        Check(var_orCallable, Body).

        RuntimeCheckedDbClause((Head :- Body)) :-
        var(Body),
        Check(var_orCallable, Head).



   //Check_non_portableFunctions(@term)
        %
   //checks an arithmetic expression for calls to non-standard Prolog functions

        Check_non_portableFunctions(Expression) :-
        callable(Expression),
   //assume function
        !,
        (	IsoSpecFunction(Expression) ->
   //portable call (we assume...!)
        true
        ;	pp_non_portableFunction_(Expression, _, _) ->
   //non-portable function already recorded
        true
        ;	% first occurrence of this non-portable function; record it
        term_template(Expression, Template),
        SourceFileContext(File, Lines),
        assertz(pp_non_portableFunction_(Template, File, Lines))
        ),
        Expression =.. [_| Expressions],
        Check_non_portableFunctionArgs(Expressions).

        Check_non_portableFunctions(_).	% variables and numbers


        Check_non_portableFunctionArgs([]).

        Check_non_portableFunctionArgs([Expression| Expressions]) :-
        Check_non_portableFunctions(Expression),
        Check_non_portableFunctionArgs(Expressions).



   //Compile_message_to_object(@term, @objectIdentifier, -callable, +atom, +compilationContext)
        %
   //compiles a message sending call


   //messages to the pseudo-object "user"

        Compile_message_to_object(Pred, Obj, Pred, _, _) :-
        Obj == user,
        !,
        Check(var_orCallable, Pred).

   //convenient access to parametric object proxies

        Compile_message_to_object(Pred, Obj, (CallProxy, TPred), Events, Ctx) :-
        nonvar(Obj),
        Obj = {Proxy},
        !,
        Check(var_orCallable, Proxy),
        (	var(Proxy) ->
        CallProxy = call(Proxy)
        ;	CallProxy = Proxy
        ),
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, This, _),
        Compile_message_to_object(Pred, Proxy, TPred, Events, Ctx).

   //entityKind and lint checks

        Compile_message_to_object(_, Obj, _, _, Ctx) :-
        (	callable(Obj) ->
   //remember the object receiving the message
        AddReferenced_object(Obj, Ctx),
        fail
        ;	nonvar(Obj),
   //invalid object identifier
        throw(type_error(objectIdentifier, Obj))
        ).

   //suspicious use of ::/2 instead of ::/1 to send a message to "self"

        Compile_message_to_object(Pred, Obj, _, _, Ctx) :-
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, ExCtx, compile(_), _, _),
        CompilerFlag(suspiciousCalls, warning),
        executionContext(ExCtx, _, _, _, Self, _, _),
        Self == Obj,
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(suspiciousCalls), core, suspiciousCall(File, Lines, Type, Entity, Obj::Pred, ::Pred)),
        fail.

   //suspicious use of ::/2 in objects to call a local predicate

        Compile_message_to_object(Pred, Obj, _, _, Ctx) :-
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, ExCtx, compile(_), _, _),
        pp_entity_(object, _, _, _, _),
        CompilerFlag(suspiciousCalls, warning),
        executionContext(ExCtx, _, _, This, _, _, _),
        This == Obj,
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(suspiciousCalls), core, suspiciousCall(File, Lines, Type, Entity, Obj::Pred, Pred)),
        fail.

   //translation performed at runtime

        Compile_message_to_object(Pred, Obj, Send_to_objRt(Obj, Pred, Events, NewCtx), Events, Ctx) :-
        var(Pred),
        !,
        CompCtx(Ctx, Head, _, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, _, Stack, Lines),
        CompCtx(NewCtx, Head, _, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, runtime, Stack, Lines).

   //broadcasting control constructs

        Compile_message_to_object((Pred1, Pred2), Obj, (TPred1, TPred2), Events, Ctx) :-
        !,
        Compile_message_to_object(Pred1, Obj, TPred1, Events, Ctx),
        Compile_message_to_object(Pred2, Obj, TPred2, Events, Ctx).

        Compile_message_to_object((Pred1; Pred2), Obj, (TPred1; TPred2), Events, Ctx) :-
        !,
        Compile_message_to_object(Pred1, Obj, TPred1, Events, Ctx),
        Compile_message_to_object(Pred2, Obj, TPred2, Events, Ctx).

        Compile_message_to_object((Pred1 -> Pred2), Obj, (TPred1 -> TPred2), Events, Ctx) :-
        !,
        Compile_message_to_object(Pred1, Obj, TPred1, Events, Ctx),
        Compile_message_to_object(Pred2, Obj, TPred2, Events, Ctx).

        Compile_message_to_object('*->(Pred1, Pred2), Obj, '*->(TPred1, TPred2), Events, Ctx) :-
        predicate_property('*->(_, _), builtIn),
        !,
        Compile_message_to_object(Pred1, Obj, TPred1, Events, Ctx),
        Compile_message_to_object(Pred2, Obj, TPred2, Events, Ctx).

   //built-in methods that cannot be redefined

        Compile_message_to_object(!, Obj, (object_exists(Obj, !, ExCtx), !), _, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_message_to_object(true, Obj, (object_exists(Obj, true, ExCtx), true), _, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_message_to_object(fail, Obj, (object_exists(Obj, fail, ExCtx), fail), _, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_message_to_object(false, Obj, (object_exists(Obj, false, ExCtx), false), _, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

        Compile_message_to_object(repeat, Obj, (object_exists(Obj, repeat, ExCtx), repeat), _, Ctx) :-
        !,
        CompCtx_execCtx(Ctx, ExCtx).

   //reflection built-in predicates

        Compile_message_to_object(current_op(Priority, Specifier, Operator), Obj, Current_op(Obj, Priority, Specifier, Operator, This, p(p(p)), ExCtx), _, Ctx) :-
        !,
        Check(var_or_operator_priority, Priority),
        Check(var_or_operatorSpecifier, Specifier),
        Check(var_orAtom, Operator),
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, This, _).

        Compile_message_to_object(current_predicate(Pred), Obj, Current_predicate(Obj, Pred, This, p(p(p)), ExCtx), _, Ctx) :-
        !,
        Check(var_or_predicateIndicator, Pred),
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, This, _).

        Compile_message_to_object(predicate_property(Pred, Prop), Obj, predicate_property(Obj, Pred, Prop, This, p(p(p)), ExCtx), _, Ctx) :-
        !,
        Check(var_orCallable, Pred),
        Check(var_or_predicate_property, Prop),
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, This, _).

   //database handling built-in predicates

        Compile_message_to_object(abolish(Pred), Obj, TPred, _, Ctx) :-
        !,
        Check(var_or_predicateIndicator, Pred),
        CompCtx(Ctx, Head, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
        executionContext_this_entity(ExCtx, This, _),
        (	var(Obj) ->
        TPred = Abolish(Obj, Pred, This, p(p(p)), ExCtx)
        ;	ground(Pred) ->
        TPred = AbolishChecked(Obj, Pred, This, p(p(p)), ExCtx),
        Remember_updated_predicate(Mode, Obj::Pred, Head)
        ;	% partially instantiated predicate indicator; runtime check required
        TPred = Abolish(Obj, Pred, This, p(p(p)), ExCtx)
        ).

        Compile_message_to_object(assert(Clause), Obj, TPred, Events, Ctx) :-
        !,
        (	CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(deprecated, warning),
        SourceFileContext(File, Lines),
        pp_entity_(Type, Entity, _, _, _) ->
        IncrementCompiling_warningsCounter',
        print_message(warning(deprecated), core, deprecated_predicate(File, Lines, Type, Entity, assert/1))
        ;	true
        ),
        Compile_message_to_object(assertz(Clause), Obj, TPred, Events, Ctx).

        Compile_message_to_object(asserta(Clause), Obj, TPred, _, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
        executionContext_this_entity(ExCtx, This, _),
        (	RuntimeCheckedDbClause(Clause) ->
        TPred = Asserta(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx)
        ;	var(Obj) ->
        Check(clause_or_partialClause, Clause),
        TPred = Asserta(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx)
        ;	Check(clause_or_partialClause, Clause),
        (	(Clause = (Head :- Body) -> Body == true; Clause = Head) ->
        (	CompilerFlag(optimize, on),
        Send_to_objDb_msgStatic_binding(Obj, Head, THead) ->
        TPred = asserta(THead)
        ;	TPred = AssertaFactChecked(Obj, Head, This, p(p(_)), p(p(p)), ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
        ;	TPred = AssertaRuleChecked(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx),
        Clause = (Head :- _),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
        )
        ).

        Compile_message_to_object(assertz(Clause), Obj, TPred, _, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
        executionContext_this_entity(ExCtx, This, _),
        (	RuntimeCheckedDbClause(Clause) ->
        TPred = Assertz(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx)
        ;	var(Obj) ->
        Check(clause_or_partialClause, Clause),
        TPred = Assertz(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx)
        ;	Check(clause_or_partialClause, Clause),
        (	(Clause = (Head :- Body) -> Body == true; Clause = Head) ->
        (	CompilerFlag(optimize, on),
        Send_to_objDb_msgStatic_binding(Obj, Head, THead) ->
        TPred = assertz(THead)
        ;	TPred = AssertzFactChecked(Obj, Head, This, p(p(_)), p(p(p)), ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
        ;	TPred = AssertzRuleChecked(Obj, Clause, This, p(p(_)), p(p(p)), ExCtx),
        Clause = (Head :- _),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
        )
        ).

        Compile_message_to_object(clause(Head, Body), Obj, TPred, _, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
        executionContext_this_entity(ExCtx, This, _),
        (	RuntimeCheckedDbClause((Head :- Body)) ->
        TPred = Clause(Obj, Head, Body, This, p(p(p)), ExCtx)
        ;	Check(clause_or_partialClause, (Head :- Body)),
        (	var(Obj) ->
        TPred = Clause(Obj, Head, Body, This, p(p(p)), ExCtx)
        ;	TPred = ClauseChecked(Obj, Head, Body, This, p(p(p)), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
        )
        ).

        Compile_message_to_object(retract(Clause), Obj, TPred, _, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
        executionContext_this_entity(ExCtx, This, _),
        (	RuntimeCheckedDbClause(Clause) ->
        TPred = Retract(Obj, Clause, This, p(p(p)), ExCtx)
        ;	var(Obj) ->
        Check(clause_or_partialClause, Clause),
        TPred = Retract(Obj, Clause, This, p(p(p)), ExCtx)
        ;	Check(clause_or_partialClause, Clause),
        (	Clause = (Head :- Body) ->
        (	var(Body) ->
        Retract_var_bodyChecked(Obj, Clause, This, p(p(p)), ExCtx)
        ;	Body == true ->
        (	CompilerFlag(optimize, on),
        Send_to_objDb_msgStatic_binding(Obj, Head, THead) ->
        TPred = retract(THead)
        ;	TPred = RetractFactChecked(Obj, Head, This, p(p(p)), ExCtx)
        )
        ;	TPred = RetractRuleChecked(Obj, Clause, This, p(p(p)), ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
        ;	TPred = RetractFactChecked(Obj, Clause, This, p(p(p)), ExCtx),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
        )
        ).

        Compile_message_to_object(retractall(Head), Obj, TPred, _, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
        executionContext_this_entity(ExCtx, This, _),
        (	var(Head) ->
        TPred = Retractall(Obj, Head, This, p(p(p)), ExCtx)
        ;	var(Obj) ->
        Check(callable, Head),
        TPred = Retractall(Obj, Head, This, p(p(p)), ExCtx)
        ;	Check(callable, Head),
        (	CompilerFlag(optimize, on),
        Send_to_objDb_msgStatic_binding(Obj, Head, THead) ->
        TPred = retractall(THead)
        ;	TPred = RetractallChecked(Obj, Head, This, p(p(p)), ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, Obj::Functor/Arity, CallerHead)
        ).

   //term and goal expansion predicates

        Compile_message_to_object(expand_term(Term, Expansion), Obj, expand_term_message(Obj, Term, Expansion, This, p(p(p)), ExCtx), _, Ctx) :-
        !,
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, This, _).

        Compile_message_to_object(expand_goal(Goal, ExpandedGoal), Obj, expand_goal_message(Obj, Goal, ExpandedGoal, This, p(p(p))), _, Ctx) :-
        !,
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, This, _).

   //compiler bypass control construct

        Compile_message_to_object({Goal}, _, call(Goal), _, _) :-
        !,
        Check(var_orCallable, Goal).

   //invalid message

        Compile_message_to_object(Pred, _, _, _, _) :-
        \+ callable(Pred),
        throw(type_error(callable, Pred)).

   //message is not a built-in control construct or a call to a built-in (meta-)predicate

        Compile_message_to_object(Pred, Obj, TPred, Events, Ctx) :-
        var(Obj),
   //translation performed at runtime
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        AddReferenced_object_message(Mode, Obj, Pred, Pred, Head),
        (	Events == allow ->
        TPred = Send_to_obj(Obj, Pred, ExCtx)
        ;	TPred = Send_to_obj_ne(Obj, Pred, ExCtx)
        ).

        Compile_message_to_object(Pred, Obj, TPred, Events, Ctx) :-
        CompCtx(Ctx, Head, _, _, _, This, _, _, _, _, ExCtx, Mode, _, _),
        AddReferenced_object_message(Mode, Obj, Pred, Pred, Head),
        (	Events == allow ->
        (	CompilerFlag(optimize, on),
        Send_to_objStatic_binding(Obj, Pred, Call, Ctx) ->
        executionContext_this_entity(ExCtx, This, _),
        TPred = guarded_methodCall(Obj, Pred, This, Call)
        ;	TPred = Send_to_obj_(Obj, Pred, ExCtx)
        )
        ;	(	CompilerFlag(optimize, on),
        Send_to_objStatic_binding(Obj, Pred, TPred, Ctx) ->
        true
        ;	TPred = Send_to_obj_ne_(Obj, Pred, ExCtx)
        )
        ).



   //Compile_message_toSelf(@term, -callable, @executionContext)
        %
   //compiles the sending of a message to self


   //translation performed at runtime

        Compile_message_toSelf(Pred, Send_toSelf(Pred, NewCtx), Ctx) :-
        var(Pred),
        !,
        CompCtx(Ctx, Head, _, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, _, Stack, Lines),
        CompCtx(NewCtx, Head, _, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, runtime, Stack, Lines).

   //suspicious use of ::/1 instead of a local predicate call in clauses that
   //apparently are meant to implement recursive predicate definitions where
   //the user intention is to call the local predicate

        Compile_message_toSelf(Pred, _, Ctx) :-
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, _, compile(_), _, _),
        CompilerFlag(suspiciousCalls, warning),
        functor(Pred, Functor, Arity),
        functor(Head, Functor, Arity),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(suspiciousCalls), core, suspiciousCall(File, Lines, Type, Entity, ::Pred, Pred)),
        fail.

   //broadcasting control constructs

        Compile_message_toSelf((Pred1, Pred2), (TPred1, TPred2), Ctx) :-
        !,
        Compile_message_toSelf(Pred1, TPred1, Ctx),
        Compile_message_toSelf(Pred2, TPred2, Ctx).

        Compile_message_toSelf((Pred1; Pred2), (TPred1; TPred2), Ctx) :-
        !,
        Compile_message_toSelf(Pred1, TPred1, Ctx),
        Compile_message_toSelf(Pred2, TPred2, Ctx).

        Compile_message_toSelf((Pred1 -> Pred2), (TPred1 -> TPred2), Ctx) :-
        !,
        Compile_message_toSelf(Pred1, TPred1, Ctx),
        Compile_message_toSelf(Pred2, TPred2, Ctx).

        Compile_message_toSelf('*->(Pred1, Pred2), '*->(TPred1, TPred2), Ctx) :-
        predicate_property('*->(_, _), builtIn),
        !,
        Compile_message_toSelf(Pred1, TPred1, Ctx),
        Compile_message_toSelf(Pred2, TPred2, Ctx).

   //built-in methods that cannot be redefined

        Compile_message_toSelf(!, !, _) :-
        !.

        Compile_message_toSelf(true, true, _) :-
        !.

        Compile_message_toSelf(false, false, _) :-
        !.

        Compile_message_toSelf(fail, fail, _) :-
        !.

        Compile_message_toSelf(repeat, repeat, _) :-
        !.

   //reflection built-in predicates

        Compile_message_toSelf(current_op(Priority, Specifier, Operator), Current_op(Self, Priority, Specifier, Operator, This, p(_), ExCtx), Ctx) :-
        !,
        Check(var_or_operator_priority, Priority),
        Check(var_or_operatorSpecifier, Specifier),
        Check(var_orAtom, Operator),
        CompCtx(Ctx, _, _, _, _, This, Self, _, _, _, ExCtx, _, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _).

        Compile_message_toSelf(current_predicate(Pred), Current_predicate(Self, Pred, This, p(_), ExCtx), Ctx) :-
        !,
        Check(var_or_predicateIndicator, Pred),
        CompCtx(Ctx, _, _, _, _, This, Self, _, _, _, ExCtx, _, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _).

        Compile_message_toSelf(predicate_property(Pred, Prop), predicate_property(Self, Pred, Prop, This, p(_), ExCtx), Ctx) :-
        !,
        Check(var_orCallable, Pred),
        Check(var_or_predicate_property, Prop),
        CompCtx(Ctx, _, _, _, _, This, Self, _, _, _, ExCtx, _, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _).

   //database handling built-in predicates

        Compile_message_toSelf(abolish(Pred), TPred, Ctx) :-
        !,
        Check(var_or_predicateIndicator, Pred),
        CompCtx(Ctx, Head, _, _, _, This, Self, _, _, _, ExCtx, Mode, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _),
        (	ground(Pred) ->
        TPred = AbolishChecked(Self, Pred, This, p(_), ExCtx),
        Remember_updated_predicate(Mode, ::Pred, Head)
        ;	% partially instantiated predicate indicator; runtime check required
        TPred = Abolish(Self, Pred, This, p(_), ExCtx)
        ).

        Compile_message_toSelf(assert(Clause), TPred, Ctx) :-
        !,
        (	CompCtx_mode(Ctx, compile(_)),
        CompilerFlag(deprecated, warning),
        SourceFileContext(File, Lines),
        pp_entity_(Type, Entity, _, _, _) ->
        IncrementCompiling_warningsCounter',
        print_message(warning(deprecated), core, deprecated_predicate(File, Lines, Type, Entity, assert/1))
        ;	true
        ),
        Compile_message_toSelf(assertz(Clause), TPred, Ctx).

        Compile_message_toSelf(asserta(Clause), TPred, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, Self, _, _, _, ExCtx, Mode, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _),
        (	RuntimeCheckedDbClause(Clause) ->
        TPred = Asserta(Self, Clause, This, p(_), p(p), ExCtx)
        ;	Check(clause_or_partialClause, Clause),
        (	Clause = (Head :- Body) ->
        (	Body == true ->
        TPred = AssertaFactChecked(Self, Head, This, p(_), p(p), ExCtx)
        ;	TPred = AssertaRuleChecked(Self, Clause, This, p(_), p(p), ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ::Functor/Arity, CallerHead)
        ;	TPred = AssertaFactChecked(Self, Clause, This, p(_), p(p), ExCtx),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, ::Functor/Arity, CallerHead)
        )
        ).

        Compile_message_toSelf(assertz(Clause), TPred, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, Self, _, _, _, ExCtx, Mode, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _),
        (	RuntimeCheckedDbClause(Clause) ->
        TPred = Assertz(Self, Clause, This, p(_), p(p), ExCtx)
        ;	Check(clause_or_partialClause, Clause),
        (	Clause = (Head :- Body) ->
        (	Body == true ->
        TPred = AssertzFactChecked(Self, Head, This, p(_), p(p), ExCtx)
        ;	TPred = AssertzRuleChecked(Self, Clause, This, p(_), p(p), ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ::Functor/Arity, CallerHead)
        ;	TPred = AssertzFactChecked(Self, Clause, This, p(_), p(p), ExCtx),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, ::Functor/Arity, CallerHead)
        )
        ).

        Compile_message_toSelf(clause(Head, Body), TPred, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, Self, _, _, _, ExCtx, Mode, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _),
        (	RuntimeCheckedDbClause((Head :- Body)) ->
        TPred = Clause(Self, Head, Body, This, p(_), ExCtx)
        ;	Check(clause_or_partialClause, (Head :- Body)),
        TPred = ClauseChecked(Self, Head, Body, This, p(_), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ::Functor/Arity, CallerHead)
        ).

        Compile_message_toSelf(retract(Clause), TPred, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, Self, _, _, _, ExCtx, Mode, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _),
        (	RuntimeCheckedDbClause(Clause) ->
        TPred = Retract(Self, Clause, This, p(_), ExCtx)
        ;	Check(clause_or_partialClause, Clause),
        (	Clause = (Head :- Body) ->
        (	var(Body) ->
        Retract_var_bodyChecked(Self, Clause, This, p(_), ExCtx)
        ;	Body == true ->
        TPred = RetractFactChecked(Self, Head, This, p(_), ExCtx)
        ;	TPred = RetractRuleChecked(Self, Clause, This, p(_), ExCtx)
        ),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ::Functor/Arity, CallerHead)
        ;	TPred = RetractFactChecked(Self, Clause, This, p(_), ExCtx),
        functor(Clause, Functor, Arity),
        Remember_updated_predicate(Mode, ::Functor/Arity, CallerHead)
        )
        ).

        Compile_message_toSelf(retractall(Head), TPred, Ctx) :-
        !,
        CompCtx(Ctx, CallerHead, _, _, _, This, Self, _, _, _, ExCtx, Mode, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _),
        (	var(Head) ->
        TPred = Retractall(Self, Head, This, p(_), ExCtx)
        ;	Check(callable, Head),
        TPred = RetractallChecked(Self, Head, This, p(_), ExCtx),
        functor(Head, Functor, Arity),
        Remember_updated_predicate(Mode, ::Functor/Arity, CallerHead)
        ).

   //term and goal expansion predicates

        Compile_message_toSelf(expand_term(Term, Expansion), expand_term_message(Self, Term, Expansion, This, p(_), ExCtx), Ctx) :-
        !,
        CompCtx(Ctx, _, _, _, _, This, Self, _, _, _, ExCtx, _, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _).

        Compile_message_toSelf(expand_goal(Goal, ExpandedGoal), expand_goal_message(Self, Goal, ExpandedGoal, This, p(_)), Ctx) :-
        !,
        CompCtx(Ctx, _, _, _, _, This, Self, _, _, _, ExCtx, _, _, _),
        executionContext(ExCtx, _, _, This, Self, _, _).

   //compiler bypass control construct

        Compile_message_toSelf({Goal}, call(Goal), _) :-
        !,
        Check(var_orCallable, Goal).

   //invalid message

        Compile_message_toSelf(Pred, _, _) :-
        \+ callable(Pred),
        throw(type_error(callable, Pred)).

   //message is not a built-in control construct or a call to a built-in
   //(meta-)predicate: translation performed at runtime

        Compile_message_toSelf(Pred, Send_toSelf_(Self, Pred, ExCtx), Ctx) :-
        CompCtx(Ctx, Head, _, _, _, _, Self, _, _, _, ExCtx, Mode, _, _),
        executionContext(ExCtx, _, _, _, Self, _, _),
        functor(Pred, Functor, Arity),
        RememberCalledSelf_predicate(Mode, Functor/Arity, Head),
        !.



   //CompileSuperCall(@term, -callable, +compilationContext)
        %
   //compiles calling of redefined predicates ("super" calls)

        CompileSuperCall(Pred, TPred, Ctx) :-
        pp_object_(Obj, _, _, _, Super, _, _, _, _, _, _),
        !,
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        (	\+ pp_extended_object_(_, _, _, _, _, _, _, _, _, _, _),
        \+ ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _),
        \+ ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _),
        \+ ppImportedCategory_(_, _, _, _, _, _) ->
   //invalid goal (no ancestor entity)
        throw(existence_error(ancestor, object))
        ;	var(Pred) ->
   //translation performed at runtime
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = objSuperCall(Super, Pred, ExCtx)
        ;	callable(Pred) ->
        (	CompilerFlag(optimize, on),
        objRelated_entitiesAreStatic',
        objSuperCallStatic_binding(Obj, Pred, ExCtx, TPred) ->
        true
        ;	TPred = objSuperCall_(Super, Pred, ExCtx)
        ),
        functor(Pred, Functor, Arity),
        RememberCalledSuper_predicate(Mode, Functor/Arity, Head)
        ;	throw(type_error(callable, Pred))
        ).

        CompileSuperCall(Pred, TPred, Ctx) :-
        ppComplemented_object_(Obj, _, _, _, _),
   //super calls from predicates defined in complementing categories
   //lookup inherited definitions in the complemented object ancestors
        !,
        CompCtx_execCtx(Ctx, ExCtx),
        (	var(Pred) ->
        TPred = (
        Current_object_(Obj, _, _, _, Super, _, _, _, _, _, _),
        objSuperCall(Super, Pred, ExCtx)
        )
        ;	callable(Pred) ->
        TPred = (
        Current_object_(Obj, _, _, _, Super, _, _, _, _, _, _),
        objSuperCall_(Super, Pred, ExCtx)
        )
        ;	throw(type_error(callable, Pred))
        ).

        CompileSuperCall(Pred, TPred, Ctx) :-
        ppCategory_(Ctg, _, _, _, _, _),
        (	\+ pp_extendedCategory_(_, _, _, _, _, _) ->
   //invalid goal (not an extended category)
        throw(existence_error(ancestor, category))
        ;	var(Pred) ->
   //translation performed at runtime
        CompCtx_execCtx(Ctx, ExCtx),
        TPred = CtgSuperCall(Ctg, Pred, ExCtx)
        ;	callable(Pred) ->
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, ExCtx, Mode, _, _),
        (	CompilerFlag(optimize, on),
        CtgRelated_entitiesAreStatic',
        CtgSuperCallStatic_binding(Ctg, Pred, ExCtx, TPred) ->
        true
        ;	TPred = CtgSuperCall_(Ctg, Pred, ExCtx)
        ),
        functor(Pred, Functor, Arity),
        RememberCalledSuper_predicate(Mode, Functor/Arity, Head)
        ;	throw(type_error(callable, Pred))
        ).


        objRelated_entitiesAreStatic' :-
        forall(
        pp_extended_object_(Obj, _, _, _, _, _, _, _, _, _, _),
        (Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags), Flags /\ 2 =:= 0)
        ),
        forall(
        ppInstantiatedClass_(Obj, _, _, _, _, _, _, _, _, _, _),
        (Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags), Flags /\ 2 =:= 0)
        ),
        forall(
        ppSpecializedClass_(Obj, _, _, _, _, _, _, _, _, _, _),
        (Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags), Flags /\ 2 =:= 0)
        ),
        forall(
        ppImportedCategory_(Ctg, _, _, _, _, _),
        (CurrentCategory_(Ctg, _, _, _, _, Flags), Flags /\ 2 =:= 0)
        ),
        forall(
        ppImplemented_protocol_(Ptc, _, _, _, _),
        (Current_protocol_(Ptc, _, _, _, Flags), Flags /\ 2 =:= 0)
        ).


        CtgRelated_entitiesAreStatic' :-
        forall(
        pp_extendedCategory_(Ctg, _, _, _, _, _),
        (CurrentCategory_(Ctg, _, _, _, _, Flags), Flags /\ 2 =:= 0)
        ),
        forall(
        ppImplemented_protocol_(Ptc, _, _, _, _),
        (Current_protocol_(Ptc, _, _, _, Flags), Flags /\ 2 =:= 0)
        ).



   //CompileContextSwitchCall(@term, @term, -callable, @executionContext)
        %
   //compiles context switching calls

        CompileContextSwitchCall(Obj, Goal, TGoal, ExCtx) :-
        (	var(Obj) ->
        Check(var_orCallable, Goal),
        TGoal = Call_withinContext(Obj, Goal, ExCtx)
        ;	Obj = {Proxy} ->
        Check(var_orCallable, Proxy),
        (	var(Proxy) ->
        CallProxy = call(Proxy)
        ;	CallProxy = Proxy
        ),
        CompileContextSwitchCall(Proxy, Goal, TGoal0, ExCtx),
        TGoal = (CallProxy, TGoal0)
        ;	var(Goal) ->
        Check(var_or_objectIdentifier, Obj),
        TGoal = Call_withinContext(Obj, Goal, ExCtx)
        ;	Check(objectIdentifier, Obj),
        Check(callable, Goal),
        TGoal = Call_withinContext_nv(Obj, Goal, ExCtx)
        ).



   //head_meta_variables(+callable, -list(variable))
        %
   //constructs a list of all variables that occur in a position corresponding
   //to a meta-argument in the head of clause being compiled

        head_meta_variables(Head, MetaVars) :-
        (	pp_meta_predicate_(Head, Meta) ->
        (	Head = Entity::Pred ->
        Meta = Entity::Template
        ;	Head = ':(Module, Pred) ->
        Meta = ':(Module, Template)
        ;	Pred = Head,
        Template = Meta
        ),
        Pred =.. [_| Args],
        Template =.. [_| MArgs],
        extract_meta_variables(Args, MArgs, MetaVars)
        ;	MetaVars = []
        ).


        extract_meta_variables([], [], []).

        extract_meta_variables([Arg| Args], [MArg| MArgs], MetaVars) :-
        (	MArg == (*) ->
        extract_meta_variables(Args, MArgs, MetaVars)
        ;	integer(MArg),
        nonvar(Arg) ->
        throw(type_error(variable, Arg))
        ;	var(Arg) ->
        MetaVars = [Arg| RestMetaVars],
        extract_meta_variables(Args, MArgs, RestMetaVars)
        ;	extract_meta_variables(Args, MArgs, MetaVars)
        ).



   //goal_metaArguments(+callable, +callable, -list(term))
        %
   //constructs a list of all meta-arguments in a goal

        goal_metaArguments(no, _, []) :-
        !.

        goal_metaArguments(Meta, Goal, MetaArgs) :-
        Meta =.. [_| MArgs],
        Goal =.. [_| Args],
        extract_metaArguments(MArgs, Args, MetaArgs).


        extract_metaArguments([], [], []).

        extract_metaArguments([MArg| MArgs], [Arg| Args], MetaArgs) :-
        (	MArg == (*) ->
        extract_metaArguments(MArgs, Args, MetaArgs)
        ;	MetaArgs = [Arg| RestMetaArgs],
        extract_metaArguments(MArgs, Args, RestMetaArgs)
        ).



   //goal_metaCallContext(+callable, +callable, @term, @term, -callable)
        %
   //constructs the meta-call execution context

        goal_metaCallContext(Meta, Pred, ExCtx, This, MetaCallCtx) :-
        executionContext_this_entity(ExCtx, This, _),
        goal_metaArguments(Meta, Pred, MetaArgs),
        (	MetaArgs == [] ->
        MetaCallCtx = []
        ;	MetaCallCtx = ExCtx-MetaArgs
        ).



   //IsoRead_term(@stream, ?term, +read_optionsList, @list)
        %
   //wraps read_term/3 call with the necessary operator settings

        IsoRead_term(Stream, Term, Options, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        read_term(Stream, Term, Options),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //IsoRead_term(?term, +read_optionsList, @list)
        %
   //wraps read_term/2 call with the necessary operator settings

        IsoRead_term(Term, Options, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        read_term(Term, Options),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //IsoRead(@stream, ?term, @list)
        %
   //wraps read/2 call with the necessary operator settings

        IsoRead(Stream, Term, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        read(Stream, Term),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //IsoRead(?term, @list)
        %
   //wraps read/1 call with the necessary operator settings

        IsoRead(Term, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        read(Term),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //Iso_write_term(@stream_orAlias, @term, @write_optionsList, @list)
        %
   //wraps write_term/3 call with the necessary operator settings

        Iso_write_term(Stream, Term, Options, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        write_term(Stream, Term, Options),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //Iso_write_term(@term, @write_optionsList, @list)
        %
   //wraps write_term/2 call with the necessary operator settings

        Iso_write_term(Term, Options, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        write_term(Term, Options),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //Iso_write(@stream_orAlias, @term, @list)
        %
   //wraps write/2 call with the necessary operator settings

        Iso_write(Stream, Term, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        write(Stream, Term),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //Iso_write(@term, @list)
        %
   //wraps write/1 call with the necessary operator settings

        Iso_write(Term, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        write(Term),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //Iso_writeq(@stream_orAlias, @term, @list)
        %
   //wraps writeq/2 call with the necessary operator settings

        Iso_writeq(Stream, Term, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        writeq(Stream, Term),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //Iso_writeq(@term, @list)
        %
   //wraps writeq/1 call with the necessary operator settings

        Iso_writeq(Term, Operators) :-
        catch(
        (Save_operators(Operators, Saved),
        Add_operators(Operators),
        writeq(Term),
        Remove_operators(Operators),
        Add_operators(Saved)),
        Error,
        IsoStreamInput_output_error_handler(Operators, Saved, Error)
        ).



   //Save_operators(@list, -list)
        %
   //saves currently defined operators that might be
   //redefined when a list of operators is added

        Save_operators([], []).

        Save_operators([op(_, Specifier, Operator)| Operators], Saved) :-
        (	current_op(Priority, SCSpecifier, Operator),
        Same_operatorClass(Specifier, SCSpecifier) ->
        Saved = [op(Priority, SCSpecifier, Operator)| Saved2]
        ;	Saved = Saved2
        ),
        Save_operators(Operators, Saved2).



   //Add_operators(@list)
        %
   //adds operators to the global operator table

        Add_operators([]).

        Add_operators([op(Priority, Specifier, Operator)| Operators]) :-
        op(Priority, Specifier, Operator),
        Add_operators(Operators).



   //Remove_operators(@list)
        %
   //removes operators from the global operator table

        Remove_operators([]).

        Remove_operators([op(_, Specifier, Operator)| Operators]) :-
        op(0, Specifier, Operator),
        Remove_operators(Operators).



   //IsoStreamInput_output_error_handler(@list, @list, @nonvar)
        %
   //restores operator table to its state before the call
   //to one of the IsoRead...' that raised an error

        IsoStreamInput_output_error_handler(Operators, Saved, Error) :-
        Remove_operators(Operators),
        Add_operators(Saved),
        throw(Error).



   //Simplify_goal(+callable, -callable)
        %
   //simplify the body of a compiled clause by folding left unifications (usually
   //resulting from the compilation of grammar rules or from inlined calls to the
   //execution-context built-in methods) and by removing redundant calls to true/0
   //(but we must be careful with control constructs that are opaque to cuts such
   //as call/1 and once/1)

        Simplify_goal(Goal, SGoal) :-
        FlattenConjunctions(Goal, SGoal0),
        FoldLeft_unifications(SGoal0, SGoal1),
        RemoveRedundantCalls(SGoal1, SGoal).



   //FlattenConjunctions(+callable, -callable)
        %
   //flattens conjunction of goals
        %
   //only standard or de facto standard control constructs are traversed to avoid
   //compiler performance penalties

        FlattenConjunctions(Goal, Goal) :-
        var(Goal),
        !.

        FlattenConjunctions('*->(Goal1, Goal2), '*->(SGoal1, SGoal2)) :-
        predicate_property('*->(_, _), builtIn),
        !,
        FlattenConjunctions(Goal1, SGoal1),
        FlattenConjunctions(Goal2, SGoal2).

        FlattenConjunctions((Goal1 -> Goal2), (SGoal1 -> SGoal2)) :-
        !,
        FlattenConjunctions(Goal1, SGoal1),
        FlattenConjunctions(Goal2, SGoal2).

        FlattenConjunctions((Goal1; Goal2), (SGoal1; SGoal2)) :-
        !,
        FlattenConjunctions(Goal1, SGoal1),
        FlattenConjunctions(Goal2, SGoal2).

        FlattenConjunctions((Goal1, Goal2), (Goal1, SGoal2)) :-
        var(Goal1),
        !,
        FlattenConjunctions(Goal2, SGoal2).

        FlattenConjunctions(((Goal1, Goal2), Goal3), Body) :-
        !,
        FlattenConjunctions((Goal1, (Goal2, Goal3)), Body).

        FlattenConjunctions((Goal1, Goal2), (Goal1, Goal3)) :-
        !,
        FlattenConjunctions(Goal2, Goal3).

        FlattenConjunctions(\+ Goal, \+ SGoal) :-
        !,
        FlattenConjunctions(Goal, SGoal).

        FlattenConjunctions(Goal, Goal).



   //FoldLeft_unifications(+goal, -goal)
        %
   //folds left unifications; right unifications cannot be folded otherwise
   //we may loose steadfastness; the left unifications are typically produced
   //when compiling grammar rules to clauses
        %
   //as the clauses containing the goals being simplified will be asserted
   //between the compiler stages, we must be careful to not create cyclic
   //terms when performing term unification

        FoldLeft_unifications(Goal, Goal) :-
        var(Goal),
        !.

        FoldLeft_unifications((Term1 = Term2), Folded) :-
        \+ \+ (Term1 = Term2, acyclic_term(Term1)),
        !,
        (	Term1 = Term2 ->
        Folded = true
        ;	Folded = fail
        ).

        FoldLeft_unifications(((Term1 = Term2), Goal), Folded) :-
        \+ \+ (Term1 = Term2, acyclic_term(Term1)),
        !,
        (	Term1 = Term2 ->
        FoldLeft_unifications(Goal, Folded)
        ;	Folded = fail
        ).

        FoldLeft_unifications(Goal, Goal).



   //RemoveRedundantCalls(+callable, -callable)
        %
   //removes redundant calls to true/0 from a compiled clause body (we must
   //be careful with control constructs that are opaque to cuts such as call/1
   //and once/1) and folds pairs of consecutive variable unifications
   //(Var1 = Var2, Var2 = Var3) that are usually generated as a by-product of
   //the compilation of grammar rules; only standard or de facto standard control
   //constructs and meta-predicates are traversed

        RemoveRedundantCalls(Goal, Goal) :-
        var(Goal),
        !.

        RemoveRedundantCalls(catch(Goal1, Error, Goal2), catch(SGoal1, Error, SGoal2)) :-
        !,
        RemoveRedundantCalls(Goal1, SGoal1),
        RemoveRedundantCalls(Goal2, SGoal2).

        RemoveRedundantCalls(call(Goal), true) :-
        Goal == !,
        !.
        RemoveRedundantCalls(call(Goal), SGoal) :-
        callable(Goal),
        functor(Goal, Functor, _),
        subAtom(Functor, 0, _, _, '),	% e.g. metacall'
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(call(Goal), call(SGoal)) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(once(Goal), true) :-
        Goal == !,
        !.
        RemoveRedundantCalls(once(Goal), once(SGoal)) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(ignore(Goal), ignore(SGoal)) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(bagof(Term, Goal, List), bagof(Term, SGoal, List)) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(setof(Term, Goal, List), setof(Term, SGoal, List)) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(findall(Term, Goal, List), findall(Term, SGoal, List)) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(findall(Term, Goal, List, Tail), findall(Term, SGoal, List, Tail)) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(forall(Goal1, Goal2), forall(SGoal1, SGoal2)) :-
        !,
        RemoveRedundantCalls(Goal1, SGoal1),
        RemoveRedundantCalls(Goal2, SGoal2).

        RemoveRedundantCalls((IfThen; Else), (SIf -> SThen; SElse)) :-
        nonvar(IfThen),
        IfThen = (If -> Then),
        !,
        RemoveRedundantCalls(If, SIf),
        RemoveRedundantCalls(Then, SThen),
        RemoveRedundantCalls(Else, SElse).

        RemoveRedundantCalls((IfThen; Else), ('*->(SIf, SThen); SElse)) :-
        nonvar(IfThen),
        IfThen = '*->(If, Then),
        predicate_property('*->(_, _), builtIn),
        !,
        RemoveRedundantCalls(If, SIf),
        RemoveRedundantCalls(Then, SThen),
        RemoveRedundantCalls(Else, SElse).

        RemoveRedundantCalls((Goal1; Goal2), (SGoal1; SGoal2)) :-
        !,
        RemoveRedundantCalls(Goal1, SGoal10),
        FixDisjunctionLeftSide(SGoal10, SGoal1),
        RemoveRedundantCalls(Goal2, SGoal2).

        RemoveRedundantCalls((Goal1 -> Goal2), (SGoal1 -> SGoal2)) :-
        !,
        RemoveRedundantCalls(Goal1, SGoal1),
        RemoveRedundantCalls(Goal2, SGoal2).

        RemoveRedundantCalls('*->(Goal1, Goal2), '*->(SGoal1, SGoal2)) :-
        predicate_property('*->(_, _), builtIn),
        !,
        RemoveRedundantCalls(Goal1, SGoal1),
        RemoveRedundantCalls(Goal2, SGoal2).

        RemoveRedundantCalls((Goal1, Goal2), (Goal1, SGoal2)) :-
        var(Goal1),
        !,
        RemoveRedundantCalls(Goal2, SGoal2).

        RemoveRedundantCalls((Goal1, Goal2), (SGoal1, Goal2)) :-
        var(Goal2),
        !,
        RemoveRedundantCalls(Goal1, SGoal1).

        RemoveRedundantCalls((Var1 = Var2a, Var2b = Var3, Goal), SGoal) :-
        Var2a == Var2b,
        RemoveRedundantCalls((Var1 = Var3, Goal), SGoal),
        !.

        RemoveRedundantCalls((Var1 = Var2a, Var2b = Var3), (Var1 = Var3)) :-
        Var2a == Var2b,
        !.

        RemoveRedundantCalls((Var1 = Var2, Goal), (Var1 = Var2, SGoal)) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls((true, Goal), SGoal) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls((Goal, true), SGoal) :-
   //make sure that we don't arrive here while simplifying a (((If->Then),true);Goal) goal (or a
   //as (((If*->Then),true);Goal) goal) as removing the call to true/0 would wrongly convert the
   //disjunction into an if-then-else goal (or a soft-cut goal with an else part)
        Goal \= (_ -> _),
        (	predicate_property('*->(_, _), builtIn) ->
        Goal \= '*->(_, _)
        ;	true
        ),
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls((Goal1, Goal2), (SGoal1, SGoal2)) :-
        !,
        RemoveRedundantCalls(Goal1, SGoal1),
        RemoveRedundantCalls(Goal2, SGoal2).

        RemoveRedundantCalls(\+ Goal, \+ SGoal) :-
        !,
        RemoveRedundantCalls(Goal, SGoal).

        RemoveRedundantCalls(Goal, Goal).



   //Save_parameter_variables(@objectIdentifier)
   //Save_parameter_variables(@categoryIdentifier)
        %
   //saves the parameter variable names and positions found
   //in parametric entity identifiers for later processing

        Save_parameter_variables(Entity) :-
        compound(Entity),
        pp_term_variable_namesFileLines_(_, VariableNames, _, _),
        parameter_variable_pairs(VariableNames, 1, ParameterVariablePairs),
        ParameterVariablePairs \== [],
        !,
        assertz(pp_parameter_variables_(ParameterVariablePairs)).

        Save_parameter_variables(_).


        parameter_variable_pairs([], _, []).

        parameter_variable_pairs([VariableName=_| VariableNames], Position, [VariableName-Position| ParameterVariablePairs]) :-
        parameter_variable_name(VariableName),
        !,
        NextPosition is Position + 1,
        parameter_variable_pairs(VariableNames, NextPosition, ParameterVariablePairs).

        parameter_variable_pairs([_| VariableNames], Position, ParameterVariablePairs) :-
        NextPosition is Position + 1,
        parameter_variable_pairs(VariableNames, NextPosition, ParameterVariablePairs).



   //parameter_variable_name(+atom)
        %
   //checks if a variable name is a parameter variable name

        parameter_variable_name(VariableName) :-
        subAtom(VariableName, 0, 1, After, '_'),
        After >= 2,
        subAtom(VariableName, _, 1, 0, '_').



   //unify_parameter_variables(+callable, +compilationContext)
        %
   //unifies any parameter variables found in a parametric entity term
   //with the corresponding entity parameters

        unify_parameter_variables(Term, Ctx) :-
        pp_parameter_variables_(ParameterVariables),
        pp_term_variable_namesFileLines_(Term, VariableNames, _, _),
        VariableNames \== [],
        (	pp_entity_(_, Entity, _, _, _) ->
   //compile time; instantiate the Entity argument in the compilation context
        true
        ;	% runtime <</2 call; Entity alreay instantiated in the compilation context
        true
        ),
        CompCtx(Ctx, _, _, Entity, _, _, _, _, _, _, ExCtx, _, _, _),
        executionContext_this_entity(ExCtx, _, Entity),
        unify_parameter_variables(VariableNames, ParameterVariables, Entity, Unified),
   //ensure that the compilation context is only further instantiated when the
   //term contains at least a parameter variable that is successfully unified
        Unified == true,
        !.

        unify_parameter_variables(_, _).


        unify_parameter_variables([], _, _, _).

        unify_parameter_variables([VariableName=Variable| VariableNames], ParameterVariables, Entity, true) :-
        member(VariableName-Position, ParameterVariables),
        !,
        arg(Position, Entity, Variable),
        unify_parameter_variables(VariableNames, ParameterVariables, Entity, true).

        unify_parameter_variables([_| VariableNames], ParameterVariables, Entity, Unified) :-
        unify_parameter_variables(VariableNames, ParameterVariables, Entity, Unified).



   //Compile_objectIdentifier(@objectIdentifier, @compilationContext)
        %
   //from the object identifier construct the set of
   //functor prefixes used in the compiled code clauses

        Compile_objectIdentifier(Obj, Ctx) :-
        (	atom(Obj) ->
        GObj = Obj
        ;	% parametric object
        term_template(Obj, GObj)
        ),
        AddReferenced_object(GObj, Ctx),
        (	ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        ConstructIcFunctors(GObj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm)
        ;	ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        ConstructIcFunctors(GObj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm)
        ;	Construct_prototypeFunctors(GObj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm)
        ),
   //the object flags are only computed at the end of the entity compilation
        assertz(pp_object_(GObj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, _)),
   //provide quick access to some common used data on the entity being compiled
        assertz(pp_entity_(object, Obj, Prefix, Dcl, Rnm)),
   //initialize the predicate mutex counter
        asserta(pp_predicate_mutexCounter_(0)).



   //CompileCategoryIdentifier(@categoryIdentifier, @compilationContext)
        %
   //from the category identifier construct the set of
   //functor prefixes used in the compiled code clauses

        CompileCategoryIdentifier(Ctg, Ctx) :-
        (	atom(Ctg) ->
        GCtg = Ctg
        ;	% parametric category
        term_template(Ctg, GCtg)
        ),
        AddReferencedCategory(GCtg, Ctx),
        ConstructCategoryFunctors(GCtg, Prefix, Dcl, Def, Rnm),
   //the category flags are only computed at the end of the entity compilation
        assertz(ppCategory_(GCtg, Prefix, Dcl, Def, Rnm, _)),
   //provide quick access to some common used data on the entity being compiled
        assertz(pp_entity_(category, Ctg, Prefix, Dcl, Rnm)),
   //initialize the predicate mutex counter
        asserta(pp_predicate_mutexCounter_(0)).



   //Compile_protocolIdentifier(@protocolIdentifier, @compilationContext)
        %
   //from the protocol identifier construct the set of
   //functor prefixes used in the compiled code clauses

        Compile_protocolIdentifier(Ptc, Ctx) :-
        AddReferenced_protocol(Ptc, Ctx),
        Construct_protocolFunctors(Ptc, Prefix, Dcl, Rnm),
   //the protocol flags are only computed at the end of the entity compilation
        assertz(pp_protocol_(Ptc, Prefix, Dcl, Rnm, _)),
   //provide quick access to some common used data on the entity being compiled
        assertz(pp_entity_(protocol, Ptc, Prefix, Dcl, Rnm)),
   //initialize the predicate mutex counter; necessary in order to be able to
   //save synchronized predicate properties
        asserta(pp_predicate_mutexCounter_(0)).



   //CompileImplements_protocolRelation('+list, @objectIdentifier, @compilationContext)
   //CompileImplements_protocolRelation(+list, @categoryIdentifier, @compilationContext)
        %
   //compiles an "implements" relation between a category or an object and a list of protocols
        %
   //note that the clause order ensures that instantiation errors will be caught by the call to
   //the Check_entityReference'/4 predicate


        CompileImplements_protocolRelation([Ref| Refs], ObjOrCtg, Ctx) :-
        Check_entityReference(protocol, Ref, Scope, Ptc),
        (	ObjOrCtg == Ptc ->
        throw(permission_error(implement, self, ObjOrCtg))
        ;	Is_object(Ptc) ->
        throw(type_error(protocol, Ptc))
        ;	IsCategory(Ptc) ->
        throw(type_error(protocol, Ptc))
        ;	AddReferenced_protocol(Ptc, Ctx),
        assertz(ppRuntimeClause_(Implements_protocol_(ObjOrCtg, Ptc, Scope))),
        Construct_protocolFunctors(Ptc, Prefix, Dcl, _),
        assertz(ppImplemented_protocol_(Ptc, ObjOrCtg, Prefix, Dcl, Scope)),
        CompileImplements_protocolRelation(Refs, ObjOrCtg, Ctx)
        ).

        CompileImplements_protocolRelation([], _, _).



   //CompileImportsCategoryRelation(+list, @objectIdentifier, @compilationContext)
        %
   //compiles an "imports" relation between an object and a list of categories
        %
   //note that the clause order ensures that instantiation errors will be caught by the call to
   //the Check_entityReference'/4 predicate

        CompileImportsCategoryRelation([Ref| Refs], Obj, Ctx) :-
        Check_entityReference(category, Ref, Scope, Ctg),
        (	term_template(Obj, Ctg) ->
        throw(permission_error(import, self, Obj))
        ;	Is_object(Ctg) ->
        throw(type_error(category, Ctg))
        ;	Is_protocol(Ctg) ->
        throw(type_error(category, Ctg))
        ;	AddReferencedCategory(Ctg, Ctx),
        assertz(ppRuntimeClause_(ImportsCategory_(Obj, Ctg, Scope))),
        ConstructCategoryFunctors(Ctg, Prefix, Dcl, Def, _),
        assertz(ppImportedCategory_(Ctg, Obj, Prefix, Dcl, Def, Scope)),
        CompileImportsCategoryRelation(Refs, Obj, Ctx)
        ).

        CompileImportsCategoryRelation([], _, _).



   //CompileInstantiatesClassRelation(+list, @objectIdentifier, @compilationContext)
        %
   //compiles an "instantiates" relation between an instance and a list of classes
        %
   //note that the clause order ensures that instantiation errors will be caught by the call to
   //the Check_entityReference'/4 predicate

        CompileInstantiatesClassRelation([Ref| Refs], Obj, Ctx) :-
        Check_entityReference(object, Ref, Scope, Class),
        (	Is_protocol(Class) ->
        throw(type_error(object, Class))
        ;	IsCategory(Class) ->
        throw(type_error(object, Class))
        ;	Is_prototype(Class) ->
        throw(domain_error(class, Class))
        ;	pp_extended_object_(_, _, _, _, _, _, _, _, _, _, _) ->
        throw(permission_error(instantiate, class, Class))
        ;	AddReferenced_object(Class, Ctx),
        assertz(ppRuntimeClause_(InstantiatesClass_(Obj, Class, Scope))),
        ConstructIcFunctors(Class, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, _),
        assertz(ppInstantiatedClass_(Class, Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Scope)),
        CompileInstantiatesClassRelation(Refs, Obj, Ctx)
        ).

        CompileInstantiatesClassRelation([], _, _).



   //CompileSpecializesClassRelation(+list, @objectIdentifier, @compilationContext)
        %
   //compiles a "specializes" relation between a class and a list of superclasses
        %
   //note that the clause order ensures that instantiation errors will be caught by the call to
   //the Check_entityReference'/4 predicate

        CompileSpecializesClassRelation([Ref| Refs], Class, Ctx) :-
        Check_entityReference(object, Ref, Scope, Superclass),
        (	term_template(Class, Superclass) ->
        throw(permission_error(generalize, self, Class))
        ;	Is_protocol(Superclass) ->
        throw(type_error(object, Superclass))
        ;	IsCategory(Superclass) ->
        throw(type_error(object, Superclass))
        ;	Is_prototype(Superclass) ->
        throw(domain_error(class, Superclass))
        ;	pp_extended_object_(_, _, _, _, _, _, _, _, _, _, _) ->
        throw(permission_error(generalize, class, Superclass))
        ;	AddReferenced_object(Superclass, Ctx),
        assertz(ppRuntimeClause_(SpecializesClass_(Class, Superclass, Scope))),
        ConstructIcFunctors(Superclass, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, _),
        assertz(ppSpecializedClass_(Superclass, Class, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Scope)),
        CompileSpecializesClassRelation(Refs, Class, Ctx)
        ).

        CompileSpecializesClassRelation([], _, _).



   //Compile_extends_objectRelation(+list, @objectIdentifier, @compilationContext)
        %
   //compiles an "extends" relation between a prototype and a list of parents
        %
   //note that the clause order ensures that instantiation errors will be caught by the call to
   //the Check_entityReference'/4 predicate

        Compile_extends_objectRelation([Ref| Refs], Obj, Ctx) :-
        Check_entityReference(object, Ref, Scope, Parent),
        (	term_template(Obj, Parent) ->
        throw(permission_error(extend, self, Obj))
        ;	Is_protocol(Parent) ->
        throw(type_error(object, Parent))
        ;	IsCategory(Parent) ->
        throw(type_error(object, Parent))
        ;	IsClass(Parent) ->
        throw(domain_error(prototype, Parent))
        ;	ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        throw(permission_error(extend, prototype, Parent))
        ;	ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        throw(permission_error(extend, prototype, Parent))
        ;	AddReferenced_object(Parent, Ctx),
        assertz(ppRuntimeClause_(extends_object_(Obj, Parent, Scope))),
        Construct_prototypeFunctors(Parent, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, _),
        assertz(pp_extended_object_(Parent, Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Scope)),
        Compile_extends_objectRelation(Refs, Obj, Ctx)
        ).

        Compile_extends_objectRelation([], _, _).



   //Compile_extends_protocolRelation(+list, @protocolIdentifier, @compilationContext)
        %
   //compiles an "extends" relation between a protocol and a list of protocols
        %
   //note that the clause order ensures that instantiation errors will be caught by the call to
   //the Check_entityReference'/4 predicate

        Compile_extends_protocolRelation([Ref| Refs], Ptc, Ctx) :-
        Check_entityReference(protocol, Ref, Scope, ExtPtc),
        (	Ptc == ExtPtc ->
        throw(permission_error(extend, self, Ptc))
        ;	Is_object(ExtPtc) ->
        throw(type_error(protocol, ExtPtc))
        ;	IsCategory(ExtPtc) ->
        throw(type_error(protocol, ExtPtc))
        ;	AddReferenced_protocol(ExtPtc, Ctx),
        assertz(ppRuntimeClause_(extends_protocol_(Ptc, ExtPtc, Scope))),
        Construct_protocolFunctors(ExtPtc, Prefix, Dcl, _),
        assertz(pp_extended_protocol_(ExtPtc, Ptc, Prefix, Dcl, Scope)),
        Compile_extends_protocolRelation(Refs, Ptc, Ctx)
        ).

        Compile_extends_protocolRelation([], _, _).



   //Compile_extendsCategoryRelation(+list, @categoryIdentifier, @compilationContext)
        %
   //compiles an "extends" relation between a category and a list of categories
        %
   //note that the clause order ensures that instantiation errors will be caught by the call to
   //the Check_entityReference'/4 predicate

        Compile_extendsCategoryRelation([Ref| Refs], Ctg, Ctx) :-
        Check_entityReference(category, Ref, Scope, ExtCtg),
        (	term_template(Ctg, ExtCtg) ->
        throw(permission_error(extend, self, Ctg))
        ;	Is_object(ExtCtg) ->
        throw(type_error(category, ExtCtg))
        ;	Is_protocol(ExtCtg) ->
        throw(type_error(category, ExtCtg))
        ;	AddReferencedCategory(ExtCtg, Ctx),
        assertz(ppRuntimeClause_(extendsCategory_(Ctg, ExtCtg, Scope))),
        ConstructCategoryFunctors(ExtCtg, Prefix, Dcl, Def, _),
        assertz(pp_extendedCategory_(ExtCtg, Ctg, Prefix, Dcl, Def, Scope)),
        Compile_extendsCategoryRelation(Refs, Ctg, Ctx)
        ).

        Compile_extendsCategoryRelation([], _, _).



   //CompileComplements_objectRelation(+list, @categoryIdentifier, @compilationContext)
        %
   //compiles a "complements" relation between a category and a list of objects
        %
   //note that the clause order ensures that instantiation errors will be caught by the call to
   //the Check_entityReference'/4 predicate

        CompileComplements_objectRelation(Objs, Ctg, Ctx) :-
        ppCategory_(Ctg, _, Dcl, Def, Rnm, _),
        CompileComplements_objectRelation(Objs, Ctg, Dcl, Def, Rnm, Ctx).


        CompileComplements_objectRelation([Obj| _], Ctg, _, _, _, _) :-
        Check(objectIdentifier, Obj),
        (	term_template(Obj, Ctg) ->
        throw(permission_error(complement, self, Ctg))
        ;	Is_protocol(Obj) ->
        throw(type_error(object, Obj))
        ;	IsCategory(Obj) ->
        throw(type_error(object, Obj))
        ;	fail
        ).

        CompileComplements_objectRelation([Obj| _], Ctg, _, _, _, Ctx) :-
        CompCtx_mode(Ctx, compile(_)),
        (	Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags) ->
   //loaded object
        true
        ;	ppRuntimeClause_(Current_object_(Obj, _, _, _, _, _, _, _, _, _, Flags))
   //object being redefined in the same file as the complementing category;
   //possible but unlikely in practice (except, maybe, in classroom examples)
        ),
        Flags /\ 64 =\= 64,
        Flags /\ 32 =\= 32,
   //object compiled with complementing categories support disabled
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines),
        print_message(warning(general), core, complementingCategoryIgnored(File, Lines, Ctg, Obj)),
        fail.

        CompileComplements_objectRelation([Obj| Objs], Ctg, Dcl, Def, Rnm, Ctx) :-
        AddReferenced_object(Obj, Ctx),
   //ensure that a new complementing category will take preference over
   //any previously loaded complementing category for the same object
        CompCtxLines(Ctx, Lines),
        asserta(ppFileInitialization_(asserta(Complemented_object_(Obj, Ctg, Dcl, Def, Rnm)), Lines)),
        assertz(ppComplemented_object_(Obj, Ctg, Dcl, Def, Rnm)),
        CompileComplements_objectRelation(Objs, Ctg, Dcl, Def, Rnm, Ctx).

        CompileComplements_objectRelation([], _, _, _, _, _).



   //Is_prototype(+entityIdentifier)
        %
   //true if the argument is a defined prototype or a prototype being compiled

        Is_prototype(Obj) :-
        (	Current_object_(Obj, _, _, _, _, _, _, _, _, _, _) ->
   //existing object; first, check that is not being compiled as a different kind of entity
        \+ ppRuntimeClause_(Current_protocol_(Obj, _, _, _, _)),
        \+ ppRuntimeClause_(CurrentCategory_(Obj, _, _, _, _, _)),
   //second, check that it's a prototype
        \+ InstantiatesClass_(Obj, _, _),
        \+ InstantiatesClass_(_, Obj, _),
        \+ SpecializesClass_(Obj, _, _),
        \+ SpecializesClass_(_, Obj, _)
        ;	ppRuntimeClause_(Current_object_(Obj, _, _, _, _, _, _, _, _, _, _)) ->
   //object defined in the same file we're compiling; check that it's a prototype
        \+ ppRuntimeClause_(InstantiatesClass_(Obj, _, _)),
        \+ ppRuntimeClause_(InstantiatesClass_(_, Obj, _)),
        \+ ppRuntimeClause_(SpecializesClass_(Obj, _, _)),
        \+ ppRuntimeClause_(SpecializesClass_(_, Obj, _))
        ;	fail
        ).



   //IsClass(+entityIdentifier)
        %
   //true if the argument is a defined class or a class being compiled

        IsClass(Obj) :-
        (	Current_object_(Obj, _, _, _, _, _, _, _, _, _, _) ->
   //existing object; first, check that is not being compiled as a different kind of entity
        \+ ppRuntimeClause_(Current_protocol_(Obj, _, _, _, _)),
        \+ ppRuntimeClause_(CurrentCategory_(Obj, _, _, _, _, _)),
   //second, check that it's an instance or a class
        (	InstantiatesClass_(Obj, _, _)
        ;	InstantiatesClass_(_, Obj, _)
        ;	SpecializesClass_(Obj, _, _)
        ;	SpecializesClass_(_, Obj, _)
        ), !
        ;	ppRuntimeClause_(Current_object_(Obj, _, _, _, _, _, _, _, _, _, _)) ->
   //object defined in the same file we're compiling; check that it's an instance or a class
        (	ppRuntimeClause_(InstantiatesClass_(Obj, _, _))
        ;	ppRuntimeClause_(InstantiatesClass_(_, Obj, _))
        ;	ppRuntimeClause_(SpecializesClass_(Obj, _, _))
        ;	ppRuntimeClause_(SpecializesClass_(_, Obj, _))
        ), !
        ;	fail
        ).



   //Is_object(+entityIdentifier)
        %
   //true if the argument is a defined object or an object being compiled

        Is_object(Obj) :-
        (	Current_object_(Obj, _, _, _, _, _, _, _, _, _, _) ->
   //existing object; check that is not being compiled as a different kind of entity
        \+ ppRuntimeClause_(Current_protocol_(Obj, _, _, _, _)),
        \+ ppRuntimeClause_(CurrentCategory_(Obj, _, _, _, _, _))
        ;	pp_object_(Obj, _, _, _, _, _, _, _, _, _, _) ->
   //object being compiled
        true
        ;	ppRuntimeClause_(Current_object_(Obj, _, _, _, _, _, _, _, _, _, _)) ->
   //object defined in the same file we're compiling
        true
        ;	fail
        ).



   //Is_protocol(+entityIdentifier)
        %
   //true if the argument is a defined protocol or a protocol being compiled

        Is_protocol(Ptc) :-
        (	Current_protocol_(Ptc, _, _, _, _) ->
   //existing protocol; check that is not being compiled as a different kind of entity
        \+ ppRuntimeClause_(Current_object_(Ptc, _, _, _, _, _, _, _, _, _, _)),
        \+ ppRuntimeClause_(CurrentCategory_(Ptc, _, _, _, _, _))
        ;	pp_protocol_(Ptc, _, _, _, _) ->
   //protocol being compiled
        true
        ;	ppRuntimeClause_(Current_protocol_(Ptc, _, _, _, _)) ->
   //protocol defined in the same file we're compiling
        true
        ;	fail
        ).



   //IsCategory(+entityIdentifier)
        %
   //true if the argument is a defined category or a category being compiled

        IsCategory(Ctg) :-
        (	CurrentCategory_(Ctg, _, _, _, _, _) ->
   //existing category; check that is not being compiled as a different kind of entity
        \+ ppRuntimeClause_(Current_object_(Ctg, _, _, _, _, _, _, _, _, _, _)),
        \+ ppRuntimeClause_(Current_protocol_(Ctg, _, _, _, _))
        ;	ppCategory_(Ctg, _, _, _, _, _) ->
   //category being compiled
        true
        ;	ppRuntimeClause_(CurrentCategory_(Ctg, _, _, _, _, _)) ->
   //category defined in the same file we're compiling
        true
        ;	fail
        ).



   //InlineCalls(+atom)
        %
   //inline calls in linking clauses to Prolog module, built-in, and
   //foreign predicates when compiling source files in optimal mode

        InlineCalls(protocol).

        InlineCalls(category) :-
        ppCategory_(_, _, _, Def, _, _),
        InlineCallsDef(Def).

        InlineCalls(object) :-
        pp_object_(_, _, _, Def, _, _, _, _, _, _, _),
        InlineCallsDef(Def).


        InlineCallsDef(Def) :-
        CompilerFlag(optimize, on),
        \+ ppDynamic_',
   //static entity
        pp_number_ofClausesRules_(Functor, Arity, 1, _),
   //predicate with a single clause
        functor(Head, Functor, Arity),
        \+ ppDynamic_(Head),
        \+ pp_multifile_(Head, _, _),
        \+ ppSynchronized_(Head, _),
   //static, non-multifile, and no synchronization wrapper
        ppDefines_predicate_(Head, _, ExCtx, THead, compile(_), user),
   //source file user-defined predicate
        ppFinal_entity_term_((THead :- TBody), _),
        (	TBody = ':(_, _) ->
   //allow inlining of Prolog module predicate calls
        true
        ;	\+ ControlConstruct(TBody),
        \+ Logtalk_meta_predicate(TBody, _, _)
   //body is not otherwise a control construct or a built-in meta-predicate
        ),
        (	TBody = ':(Module, Body) ->
   //call to a Prolog module predicate
        atom(Module),
        callable(Body)
        ;	predicate_property(TBody, builtIn),
        \+ predicate_property(TBody, meta_predicate(_)) ->
   //Prolog built-in predicate
        Body = TBody
        ;	% not all backend Prolog systems support a "foreign" predicate property
        catch(predicate_property(TBody, foreign), _, fail),
        \+ predicate_property(TBody, meta_predicate(_)) ->
   //Prolog foreign predicate
        Body = TBody
        ;	functor(TBody, TFunctor, TArity),
        ppReferenced_object_message_(Object, TFunctor/TArity, _, Functor/Arity, _, _),
        Object == user ->
   //message to the "user" pseudo-object
        Body = TBody
        ;	ppDefines_predicate_(Body, _, _, TBody, compile(_), user),
        \+ pp_meta_predicate_(Body, _) ->
   //call to a local predicate
        true
        ;	fail
        ),
        Head =.. [_| HeadArguments],
        Body =.. [_| BodyArguments],
        forall(
        member(Argument, HeadArguments),
        (var(Argument), member_var(Argument, BodyArguments))
        ),
   //all head arguments are variables and exist in the body
        DefClauseOld =.. [Def, Head, _, _],
        retractall(ppDef_(DefClauseOld)),
        DefClauseNew =.. [Def, Head, ExCtx, TBody],
        asserta(ppDef_(DefClauseNew)),
        assertz(ppInline_predicate_(Functor/Arity)),
   //next candidate predicate
        fail.

        InlineCallsDef(_).



   //ControlConstruct(?callable)
        %
   //partial table of control constructs; mainly used to help decide
   //if a predicate definition should be compiled inline

        ControlConstruct((_ , _)).
        ControlConstruct((_ ; _)).
        ControlConstruct((_ -> _)).
        ControlConstruct(\+ _).
        ControlConstruct(^^ _).
        ControlConstruct(_ :: _).
        ControlConstruct(:: _).
        ControlConstruct(_ / _).
        ControlConstruct(_ >> _).
        ControlConstruct(_ << _).
        ControlConstruct({_}).
        ControlConstruct(':(_, _)).
        ControlConstruct(throw(_)).
        ControlConstruct('*->(_, _)) :-
        prolog_builtIn_predicate('*->(_, _)).



   //Cut_transparentControlConstruct(?callable)
        %
   //table of cut-transparent control constructs; used during
   //compilation to check if call/1-N wrappers need to be keep
   //for preserving source code semantics when the goal/closure
   //argument is bound

        Cut_transparentControlConstruct(!).
        Cut_transparentControlConstruct((_ , _)).
        Cut_transparentControlConstruct((_ ; _)).
        Cut_transparentControlConstruct((_ -> _)).
        Cut_transparentControlConstruct('*->(_, _)) :-
        prolog_builtIn_predicate('*->(_, _)).



   //ReportLintIssues(+atom, @entityIdentifier)
        %
   //reports detected lint issues found while compiling an entity
   //(note that some lint issues are reported during compilation)

        ReportLintIssues(Type, Entity) :-
        Report_missingDirectives(Type, Entity),
        Report_non_portableCalls(Type, Entity),
        Report_unknown_entities(Type, Entity).



   //SourceFileContext(-atom, -pair(integer), -atom, -entityIdentifier)
        %
   //returns file, lines, and entity source context for the last term read;
   //it fails if the last attempt to read a term resulted in a syntax error

        SourceFileContext(File, Lines, Type, Entity) :-
        pp_term_variable_namesFileLines_(_, _, File, Lines),
        pp_entity_(Type, Entity, _, _, _).



   //SourceFileContext(-atom, -pair(integer))
        %
   //returns file and lines source context for the last term read;
   //it fails if the last attempt to read a term resulted in a syntax error

        SourceFileContext(File, Lines) :-
        pp_term_variable_namesFileLines_(_, _, File, Lines).



   //SourceFileContext(@compilationContext, -atom, -pair(integer))
        %
   //in the context of compiling a file, returns file and lines source context
   //for the last term read and fails if the last attempt to read a term
   //resulted in a syntax error; in the context of runtime compilation, returns
   //dummy values

        SourceFileContext(Ctx, File, Lines) :-
        (	CompCtx_mode(Ctx, runtime) ->
        File = nil, Lines = 0-0
        ;	pp_term_variable_namesFileLines_(_, _, File, Lines) ->
        true
        ;	% e.g. when compiling auxiliary clauses at runtime
        File = nil, Lines = 0-0
        ).



   //Report_unknown_entities(+atom, @entityIdentifier, +atom)
        %
   //reports any unknown referenced entities found while compiling an entity

        Report_unknown_entities(Type, Entity) :-
        (	CompilerFlag(unknown_entities, warning) ->
        Report_unknown_objects(Type, Entity),
        Report_unknown_protocols(Type, Entity),
        Report_unknownCategories(Type, Entity),
        Report_unknown_modules(Type, Entity)
        ;	true
        ).



   //Report_unknown_objects(+atom, @entityIdentifier)
        %
   //reports any references to unknown objects found while compiling an entity

        Report_unknown_objects(Type, Entity) :-
        ppReferenced_object_(Object, File, Lines),
   //not a currently loaded object
        \+ Current_object_(Object, _, _, _, _, _, _, _, _, _, _),
   //not the object being compiled (self reference)
        \+ pp_object_(Object, _, _, _, _, _, _, _, _, _, _),
   //not an object defined in the source file being compiled
        \+ ppRuntimeClause_(Current_object_(Object, _, _, _, _, _, _, _, _, _, _)),
   //not a currently loaded module
        \+ (atom(Object), prologFeature(modules, supported), current_module(Object)),
        IncrementCompiling_warningsCounter',
        print_message(warning(unknown_entities), core, reference_to_unknown_object(File, Lines, Type, Entity, Object)),
        fail.

        Report_unknown_objects(_, _).



   //Report_unknown_protocols(+atom, @entityIdentifier)
        %
   //reports any references to unknown protocols found while compiling an entity

        Report_unknown_protocols(Type, Entity) :-
        ppReferenced_protocol_(Protocol, File, Lines),
   //not a currently loaded protocol
        \+ Current_protocol_(Protocol, _, _, _, _),
   //not the protocol being compiled (self reference)
        \+ pp_protocol_(Protocol, _, _, _, _),
   //not a protocol defined in the source file being compiled
        \+ ppRuntimeClause_(Current_protocol_(Protocol, _, _, _, _)),
        IncrementCompiling_warningsCounter',
        print_message(warning(unknown_entities), core, reference_to_unknown_protocol(File, Lines, Type, Entity, Protocol)),
        fail.

        Report_unknown_protocols(_, _).



   //Report_unknownCategories(+atom, @entityIdentifier)
        %
   //reports any references to unknown categories found while compiling an entity

        Report_unknownCategories(Type, Entity) :-
        ppReferencedCategory_(Category, File, Lines),
   //not a currently loaded category
        \+ CurrentCategory_(Category, _, _, _, _, _),
   //not the category being compiled (self reference)
        \+ ppCategory_(Category, _, _, _, _, _),
   //not a category defined in the source file being compiled
        \+ ppRuntimeClause_(CurrentCategory_(Category, _, _, _, _, _)),
        IncrementCompiling_warningsCounter',
        print_message(warning(unknown_entities), core, reference_to_unknownCategory(File, Lines, Type, Entity, Category)),
        fail.

        Report_unknownCategories(_, _).



   //Report_unknown_modules(+atom, @entityIdentifier)
        %
   //reports any references to unknown modules found while compiling an entity

        Report_unknown_modules(Type, Entity) :-
        ppReferenced_module_(Module, File, Lines),
   //not a currently loaded module
        \+ current_module(Module),
   //not the module being compiled (self reference)
        \+ pp_module_(Module),
        IncrementCompiling_warningsCounter',
        print_message(warning(unknown_entities), core, reference_to_unknown_module(File, Lines, Type, Entity, Module)),
        fail.

        Report_unknown_modules(_, _).



   //AddDefClause(+callable, +atom, +integer, -callable, +compilationContext)
        %
   //adds a "def" clause (used to translate between user predicate names and internal names)
   //and returns the compiled clause head

        AddDefClause(Head, Functor, Arity, THead, Ctx) :-
        functor(HeadTemplate, Functor, Arity),
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, ExCtx, Mode, _, Lines),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        functor(THeadTemplate, TFunctor, TArity),
        unify_head_theadArguments(HeadTemplate, THeadTemplate, ExCtxTemplate),
        (	pp_object_(_, _, _, Def, _, _, _, _, _, _, _) ->
        true
        ;	ppCategory_(_, _, _, Def, _, _)
        ),
        ConstructDefClause(Def, HeadTemplate, ExCtxTemplate, THeadTemplate, Clause),
        assertz(ppDef_(Clause)),
   //the following two calls have side effects, thus ...
        CheckForRedefined_builtIn(Mode, HeadTemplate, ExCtxTemplate, THeadTemplate, Lines),
        RememberDefined_predicate(Mode, HeadTemplate, Functor/Arity, ExCtxTemplate, THeadTemplate),
   //... we need to delay output unifications to after they succeed
        Head = HeadTemplate,
        ExCtx = ExCtxTemplate,
        THead = THeadTemplate.



   //AddDdefClause(+callable, +atom, +integer, -callable, +compilationContext)
        %
   //adds a "ddef" clause (used to translate between user predicate names and internal names)
   //and returns the compiled clause head

        AddDdefClause(Head, Functor, Arity, THead, Ctx) :-
        functor(HeadTemplate, Functor, Arity),
        CompCtx(Ctx, _, _, _, _, _, _, Prefix, _, _, ExCtx, Mode, _, Lines),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        functor(THeadTemplate, TFunctor, TArity),
        unify_head_theadArguments(HeadTemplate, THeadTemplate, ExCtxTemplate),
   //only objects can define clauses for dynamic predicates
        pp_object_(_, _, _, _, _, _, _, _, DDef, _, _),
        ConstructDefClause(DDef, HeadTemplate, ExCtxTemplate, THeadTemplate, Clause),
        assertz(ppDdef_(Clause)),
   //the following two calls have side effects, thus ...
        CheckForRedefined_builtIn(Mode, HeadTemplate, ExCtxTemplate, THeadTemplate, Lines),
        RememberDefined_predicate(Mode, HeadTemplate, Functor/Arity, ExCtxTemplate, THeadTemplate),
   //... we need to delay output unifications to after they succeed
        Head = HeadTemplate,
        ExCtx = ExCtxTemplate,
        THead = THeadTemplate.



   //ConstructDefClause(+callable, +callable, +executionContext, +callable, -clause)
        %
   //constructs a "def" or "ddef" clause (used to translate between user predicate names and internal names)

        ConstructDefClause(Def, Head, ExCtx, THead, Clause) :-
        (	ppSynchronized_(Head, Mutex) ->
        wrapCompiled_head(Head, THead, ExCtx, Call),
        (	prologFeature(threads, supported) ->
        Clause =.. [Def, Head, ExCtx, with_mutex(Mutex,Call)]
        ;	% in single-threaded systems, with_mutex/2 is equivalent to once/1
        Clause =.. [Def, Head, ExCtx, once(Call)]
        )
        ;	ppCoinductive_(Head, _, ExCtx, TCHead, _, _, _) ->
        wrapCompiled_head(Head, TCHead, ExCtx, Call),
        Clause =.. [Def, Head, ExCtx, Call]
        ;	wrapCompiled_head(Head, THead, ExCtx, Call),
        Clause =.. [Def, Head, ExCtx, Call]
        ).



   //predicates for wrapping/unwrapping compiled predicate heads to deal with
   //compilation in debug mode
        %
   //the wrapping when in compilation mode ensures that indirect predicate calls
   //(e.g. when sending a message) can also be intercepted by debug handlers

        wrapCompiled_head(Head, THead, ExCtx, Call) :-
        (	CompilerFlag(debug, on) ->
        Call = Debug(goal(Head,THead), ExCtx)
        ;	Call = THead
        ).


        unwrapCompiled_head(Debug(goal(_,THead), _), THead) :-
        !.

        unwrapCompiled_head(THead, THead).



   //AddDefFailClause(@callable, @compilationContext)
        %
   //adds a "def clause" (used to translate a predicate call) where the
   //definition is simply fail due to the predicate being declared, static,
   //but undefined (as per closed-world assumption)

        AddDefFailClause(Head, Ctx) :-
        (	pp_object_(_, _, _, Def, _, _, _, _, _, _, _) ->
        true
        ;	ppCategory_(_, _, _, Def, _, _)
        ),
        Clause =.. [Def, Head, _, fail],
        assertz(ppDef_(Clause)),
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, Mode, _, Lines),
        CheckForRedefined_builtIn(Mode, Head, _, fail, Lines).



   //CheckForRedefined_builtIn(@compilation_mode, @callable, @executionContext, @callable, @pair)
        %
   //this predicate is called when adding a def/ddef clause after finding the first clause
   //for a predicate or when no clauses are defined for a declared predicate

        CheckForRedefined_builtIn(runtime, _, _, _, _).

        CheckForRedefined_builtIn(compile(_), Head, ExCtx, THead, Lines) :-
        Logtalk_builtIn_predicate(Head, _),
        !,
        assertz(ppRedefined_builtIn_(Head, ExCtx, THead)),
        retractall(pp_non_portable_predicate_(Head, _)),
        (	CompilerFlag(redefined_builtIns, warning) ->
        functor(Head, Functor, Arity),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, _, Type, Entity),
        print_message(warning(redefined_builtIns), core, redefinedLogtalk_builtIn_predicate(File, Lines, Type, Entity, Functor/Arity))
        ;	true
        ).

        CheckForRedefined_builtIn(compile(_), Head, ExCtx, THead, Lines) :-
        prolog_builtIn_predicate(Head),
        !,
        assertz(ppRedefined_builtIn_(Head, ExCtx, THead)),
        retractall(pp_non_portable_predicate_(Head, _)),
        (	CompilerFlag(redefined_builtIns, warning) ->
        functor(Head, Functor, Arity),
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, _, Type, Entity),
        print_message(warning(redefined_builtIns), core, redefined_prolog_builtIn_predicate(File, Lines, Type, Entity, Functor/Arity))
        ;	true
        ).

        CheckForRedefined_builtIn(compile(_), _, _, _, _).



   //RememberDefined_predicate(@compilation_mode, @callable, +predicateIndicator, +executionContext, @callable)
        %
   //it's necessary to remember which predicates are defined in order to deal with
   //redefinition of built-in predicates, detect missing predicate directives, and
   //speed up compilation of other clauses for the same predicates

        RememberDefined_predicate(Mode, Head, PI, ExCtx, THead) :-
        (	Mode == compile(aux) ->
        assertz(ppDefines_predicate_(Head, PI, ExCtx, THead, Mode, aux)),
        retractall(pp_previous_predicate_(_, aux)),
        assertz(pp_previous_predicate_(Head, aux))
        ;	% compile(user) or runtime
        assertz(ppDefines_predicate_(Head, PI, ExCtx, THead, Mode, user)),
        retractall(pp_previous_predicate_(_, user)),
        assertz(pp_previous_predicate_(Head, user))
        ).



   //updateDdef_table(+atom, @callable, @callable)
        %
   //retracts a dynamic "ddef clause" (used to translate a predicate call)
   //and updated the predicate lookup caches if there are no more (local)
   //clauses for the predicate otherwise does nothing; this is required in
   //order to allow definitions in ancestor entities to be found

        updateDdef_table(DDef, Head, THead) :-
        term_template(THead, GTHead),
        (	clause(GTHead, _) ->
        true
        ;	DDefClause =.. [DDef, Head, _, _],
        retractall(DDefClause),
        CleanLookupCaches(Head)
        ).



   //updateDdef_table_opt(+callable)
        %
   //retracts a dynamic "ddef clause" (used to translate a predicate call)
   //and updated the predicate lookup caches if there are no more (local)
   //clauses for the predicate otherwise does nothing; this is required in
   //order to allow definitions in ancestor entities to be found

        updateDdef_table_opt(true).

        updateDdef_table_opt(update(Head, THead, Clause)) :-
        (	clause(THead, _) ->
        true
        ;	retractall(Clause),
        CleanLookupCaches(Head)
        ).



   //generate_entityCode(+atom, +compilationContext)
        %
   //generates code for the entity being compiled

        generate_entityCode(protocol, _) :-
        generate_protocolClauses',
        generate_protocolDirectives'.

        generate_entityCode(object, Ctx) :-
        generateDef_tableClauses(Ctx),
        Compile_predicateCalls',
        generate_objectClauses',
        generate_objectDirectives',
        generateFile_objectInitialization_goal'.

        generate_entityCode(category, Ctx) :-
        generateDef_tableClauses(Ctx),
        Compile_predicateCalls',
        generateCategoryClauses',
        generateCategoryDirectives'.



        generate_objectDirectives' :-
        generate_objectDynamicDirectives',
        generate_objectDiscontiguousDirectives'.



        generateCategoryDirectives' :-
        generateCategoryDynamicDirectives',
        generateCategoryDiscontiguousDirectives'.



        generate_protocolDirectives' :-
        (	ppDynamic_' ->
        pp_protocol_(_, _, Dcl, Rnm, _),
        assertz(ppDirective_(dynamic(Dcl/4))),
        assertz(ppDirective_(dynamic(Dcl/5))),
        assertz(ppDirective_(dynamic(Rnm/3)))
        ;	true
        ).



        generate_objectDynamicDirectives' :-
        (	ppDynamic_' ->
        generateDynamic_objectDynamicDirectives'
        ;	generateStatic_objectDynamicDirectives'
        ).



        generateDynamic_objectDynamicDirectives' :-
        pp_object_(_, _, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, _),
        assertz(ppDirective_(dynamic(Dcl/4))),
        assertz(ppDirective_(dynamic(Dcl/6))),
        assertz(ppDirective_(dynamic(Def/3))),
        assertz(ppDirective_(dynamic(Def/5))),
        assertz(ppDirective_(dynamic(Super/5))),
        assertz(ppDirective_(dynamic(IDcl/6))),
        assertz(ppDirective_(dynamic(IDef/5))),
        (	CompilerFlag(dynamicDeclarations, allow) ->
        assertz(ppDirective_(dynamic(DDcl/2)))
        ;	true
        ),
        assertz(ppDirective_(dynamic(DDef/3))),
        assertz(ppDirective_(dynamic(Rnm/3))),
        generateDynamic_entityDynamic_predicateDirectives'.


        generateDynamic_entityDynamic_predicateDirectives' :-
        ppDef_(Clause),
   //only local table; reject linking clauses
        Clause \= (_ :- _),
        arg(3, Clause, Call),
        unwrapCompiled_head(Call, Pred),
        functor(Pred, Functor, Arity),
        assertz(ppDirective_(dynamic(Functor/Arity))),
        fail.

        generateDynamic_entityDynamic_predicateDirectives'.



        generateStatic_objectDynamicDirectives' :-
        pp_object_(_, Prefix, _, _, _, _, _, DDcl, DDef, _, _),
        (	CompilerFlag(dynamicDeclarations, allow) ->
        assertz(ppDirective_(dynamic(DDcl/2)))
        ;	true
        ),
        assertz(ppDirective_(dynamic(DDef/3))),
        ppDynamic_(Head),
        functor(Head, Functor, Arity),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        assertz(ppDirective_(dynamic(TFunctor/TArity))),
        fail.

        generateStatic_objectDynamicDirectives'.



        generate_objectDiscontiguousDirectives' :-
        pp_object_(_, Prefix, _, _, _, _, _, _, _, _, _),
        ppDiscontiguous_(Head),
        functor(Head, Functor, Arity),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        assertz(ppDirective_(discontiguous(TFunctor/TArity))),
        fail.

        generate_objectDiscontiguousDirectives'.



        generateCategoryDynamicDirectives' :-
        (	ppDynamic_' ->
        ppCategory_(_, _, Dcl, Def, Rnm, _),
        assertz(ppDirective_(dynamic(Dcl/4))),
        assertz(ppDirective_(dynamic(Dcl/5))),
        assertz(ppDirective_(dynamic(Def/3))),
        assertz(ppDirective_(dynamic(Rnm/3))),
        generateDynamic_entityDynamic_predicateDirectives'
        ;	true
        ).



        generateCategoryDiscontiguousDirectives' :-
        ppCategory_(_, Prefix, _, _, _, _),
        ppDiscontiguous_(Head),
        functor(Head, Functor, Arity),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        assertz(ppDirective_(discontiguous(TFunctor/TArity))),
        fail.

        generateCategoryDiscontiguousDirectives'.



        generate_objectClauses' :-
        (	ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        generateIcClauses'
        ;	ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        generateIcClauses'
        ;	% objects without an instantiation or specialization relation
   //are always compiled as prototypes
        generate_prototypeClauses'
        ).



   //generateDcl_tableClauses(-atom)
        %
   //a predicate declaration table clause is only generated if there is a
   //scope declaration for the predicate; the single argument returns the
   //atom "true" if there are local clauses and the atom "false" otherwise

        generateDcl_tableClauses(_) :-
        pp_entity_(_, _, _, Dcl, _),
        (	pp_public_(Functor, Arity), Scope = p(p(p))
        ;	pp_protected_(Functor, Arity), Scope = p(p)
        ;	pp_private_(Functor, Arity), Scope = p
        ),
        functor(Pred, Functor, Arity),
        (	pp_meta_predicate_(Pred, Template) ->
        Meta = Template,
        MetaPredicate = 64				% 0b01000000
        ;	Meta = no,
        MetaPredicate = 0
        ),
        (	ppCoinductive_(Pred, _, _, _, _, _, _) ->
        Coinductive = 32				% 0b00100000
        ;	Coinductive = 0
        ),
        (	pp_multifile_(Pred, _, _) ->
        Multifile = 16					% 0b00010000
        ;	Multifile = 0
        ),
        (	pp_non_terminal_(Functor, _, Arity) ->
        NonTerminal = 8					% 0b00001000
        ;	NonTerminal = 0
        ),
        (	ppSynchronized_(Pred, _) ->
        Synchronized = 4				% 0b00000100
        ;	Synchronized = 0
        ),
        (	ppDynamic_' ->
        Dynamic = 2						% 0b00000010
        ;	ppDynamic_(Pred) ->
        Dynamic = 2						% 0b00000010
        ;	Dynamic = 0
        ),
        Flags is MetaPredicate + Coinductive + Multifile + NonTerminal + Synchronized + Dynamic,
        Fact =.. [Dcl, Pred, Scope, Meta, Flags],
        assertz(ppDcl_(Fact)),
        fail.

        generateDcl_tableClauses(Local) :-
        (	ppDcl_(_) ->
        Local = true
        ;	Local = false
        ).



   //generateDef_tableClauses(+compilationContext)
        %
   //generates predicate definition table clauses for undefined but
   //declared (using a predicate directive) predicates

        generateDef_tableClauses(Ctx) :-
        \+ ppDynamic_',
   //static entities only otherwise abolishing the dynamic entity would result
   //in an attempt to retract a clause for the fail/0 built-in control construct
        (	ppComplemented_object_(_, _, _, _, _) ->
        CompilerFlag(complements, restrict)
        ;	true
        ),
   //complementing categories can add a scope directive for predicates that
   //are defined in the complemented objects; for objects compiled with the
   //complements flag set to allow, we must allow lookup of the predicate
   //definition in the object itself (and elsewhere in its ancestors)
        (	pp_public_(Functor, Arity)
        ;	pp_protected_(Functor, Arity)
        ;	pp_private_(Functor, Arity)
        ;	ppSynchronized_(Head, _)
        ;	ppCoinductive_(Head, _, _, _, _, _, _)
        ;	ppDiscontiguous_(Head)
        ),
        functor(Head, Functor, Arity),
        \+ pp_multifile_(Head, _, _),
        \+ ppDynamic_(Head),
        \+ ppDefines_predicate_(Head, _, _, _, _, _),
   //declared, static, but undefined predicate;
   //local calls must fail (as per closed-world assumption)
        AddDefFailClause(Head, Ctx),
        fail.

        generateDef_tableClauses(Ctx) :-
        pp_entity_(Type, _, Prefix, _, _),
        (	Type == object,
   //categories cannot contain clauses for dynamic predicates
        ppDynamic_(Head)
        ;	pp_multifile_(Head, _, _),
        \+ ppDynamic_(Head)
        ),
        \+ ppDefines_predicate_(Head, _, _, _, _, _),
   //dynamic and/or multifile predicate with no initial set of clauses
        CompCtx_prefix(Ctx, Prefix),
        functor(Head, Functor, Arity),
        (	\+ pp_public_(Functor, Arity),
        \+ pp_protected_(Functor, Arity),
        \+ pp_private_(Functor, Arity),
        \+ ppSynchronized_(Head, _),
        \+ ppCoinductive_(Head, _, _, _, _, _, _),
        \+ pp_multifile_(Head, _, _) ->
        AddDdefClause(Head, Functor, Arity, _, Ctx)
        ;	AddDefClause(Head, Functor, Arity, _, Ctx)
        ),
        fail.

        generateDef_tableClauses(_).



        generate_protocolClauses' :-
        pp_protocol_(Ptc, _, Dcl, Rnm, _),
   //first, generate the local table of predicate declarations:
        generateDcl_tableClauses(Local),
   //second, generate linking clauses for accessing both local
   //declarations and declarations in related entities (some
   //linking clauses depend on the existence of local predicate
   //declarations)
        generate_protocolLocalClauses(Local, Ptc, Dcl),
        generate_protocol_extendsClauses(Dcl, Rnm),
   //third, add a catchall clause if necessary
        generate_protocolCatchallClauses(Dcl).



        generate_protocolLocalClauses(true, Ptc, PDcl) :-
        Head =.. [PDcl, Pred, Scope, Meta, Flags, Ptc],
        Body =.. [PDcl, Pred, Scope, Meta, Flags],
        assertz(ppDcl_((Head:-Body))).

        generate_protocolLocalClauses(false, _, _).



        generate_protocol_extendsClauses(Dcl, Rnm) :-
        pp_extended_protocol_(ExtPtc, _, _, ExtDcl, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [ExtDcl, Pred, Scope, Meta, Flags, Ctn]
        ;	RelationScope == protected ->
        Lookup0 =.. [ExtDcl, Pred, Scope2, Meta, Flags, Ctn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup =.. [ExtDcl, Pred, _, Meta, Flags, Ctn]
        ),
        (	pp_predicateAlias_(ExtPtc, _, _, _, _, _) ->
        Head =.. [Dcl, Alias, Scope, Meta, Flags, Ctn],
        Rename =.. [Rnm, ExtPtc, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [Dcl, Pred, Scope, Meta, Flags, Ctn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generate_protocol_extendsClauses(_, _).



   //when a static protocol is empty, i.e. when it does not contain any predicate
   //declarations, and does not extend other protocols, we need a catchall clause
   //in order to prevent predicate existence errors when sending a message to an
   //object implementing (directly or indirectly) the protocol

        generate_protocolCatchallClauses(Dcl) :-
        (	ppDcl_(_) ->
   //local or inherited predicate declarations
        true
        ;	% empty, standalone protocol
        ppDynamic_' ->
   //dynamic protocol
        true
        ;	% generate a catchall clause for static protocols
        functor(Head, Dcl, 5),
        assertz(ppDcl_((Head:-fail)))
        ).



        generateCategoryClauses' :-
        ppCategory_(Ctg, _, Dcl, Def, Rnm, _),
        generateCategoryDclClauses(Ctg, Dcl, Rnm),
        generateCategoryDefClauses(Ctg, Def, Rnm).



        generateCategoryDclClauses(Ctg, Dcl, Rnm) :-
   //first, generate the local table of predicate declarations:
        generateDcl_tableClauses(Local),
   //second, generate linking clauses for accessing both local
   //declarations and declarations in related entities (some
   //linking clauses depend on the existence of local predicate
   //declarations)
        generateCategoryLocalDclClauses(Local, Ctg, Dcl),
        generateCategoryImplementsDclClauses(Dcl, Rnm),
        generateCategory_extendsDclClauses(Dcl, Rnm),
   //third, add a catchall clause if necessary
        generateCategoryCatchallDclClauses(Dcl).



        generateCategoryLocalDclClauses(true, Ctg, CDcl) :-
        Head =.. [CDcl, Pred, Scope, Meta, Flags, Ctg],
        Body =.. [CDcl, Pred, Scope, Meta, Flags],
        assertz(ppDcl_((Head:-Body))).

        generateCategoryLocalDclClauses(false, _, _).



        generateCategoryImplementsDclClauses(CDcl, Rnm) :-
        ppImplemented_protocol_(Ptc, _, _, PDcl, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [PDcl, Pred, Scope, Meta, Flags, Ctn]
        ;	RelationScope == protected ->
        Lookup0 =.. [PDcl, Pred, Scope2, Meta, Flags, Ctn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup =.. [PDcl, Pred, _, Meta, Flags, Ctn]
        ),
        (	pp_predicateAlias_(Ptc, _, _, _, _, _) ->
        Head =.. [CDcl, Alias, Scope, Meta, Flags, Ctn],
        Rename =.. [Rnm, Ptc, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [CDcl, Pred, Scope, Meta, Flags, Ctn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generateCategoryImplementsDclClauses(_, _).



        generateCategory_extendsDclClauses(CDcl, Rnm) :-
        pp_extendedCategory_(Ctg, _, _, ECDcl, _, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [ECDcl, Pred, Scope, Meta, Flags, Ctn]
        ;	RelationScope == protected ->
        Lookup0 =.. [ECDcl, Pred, Scope2, Meta, Flags, Ctn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup =.. [ECDcl, Pred, _, Meta, Flags, Ctn]
        ),
        (	pp_predicateAlias_(Ctg, _, _, _, _, _) ->
        Head =.. [CDcl, Alias, Scope, Meta, Flags, Ctn],
        Rename =.. [Rnm, Ctg, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [CDcl, Pred, Scope, Meta, Flags, Ctn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generateCategory_extendsDclClauses(_, _).



   //when a static category contains no predicate declarations, does not implement any
   //protocol, and does not extend other categories, we need a catchall clause in order
   //to prevent predicate existence errors when sending a message to an object importing
   //(directly or indirectly) the category

        generateCategoryCatchallDclClauses(Dcl) :-
        (	ppDcl_(_) ->
   //local or inherited predicate declarations
        true
        ;	% standalone category with no local or inherited predicate declarations
        ppDynamic_' ->
   //dynamic category
        true
        ;	% generate a catchall clause for static categories
        functor(Head, Dcl, 5),
        assertz(ppDcl_((Head:-fail)))
        ).



        generateCategoryDefClauses(Ctg, Def, Rnm) :-
        generateCategoryLocalDefClauses(Ctg, Def),
        generateCategory_extendsDefClauses(Def, Rnm).



        generateCategoryLocalDefClauses(Ctg, Def) :-
        executionContext_this_entity(ExCtx, _, Ctg),
        Head =.. [Def, Pred, ExCtx, Call, Ctg],
        (	ppDef_(_) ->
        Body =.. [Def, Pred, ExCtx, Call]
        ;	Body = fail
        ),
        assertz(ppDef_((Head:-Body))).



        generateCategory_extendsDefClauses(Def, Rnm) :-
        pp_extendedCategory_(ExtCtg, Ctg, _, _, ExtDef, _),
        executionContext_update_this_entity(CExCtx, This, Ctg, EExCtx, This, ExtCtg),
        Lookup =.. [ExtDef, Pred, EExCtx, Call, Ctn],
        (	pp_predicateAlias_(ExtCtg, _, _, _, _, _) ->
        Head =.. [Def, Alias, CExCtx, Call, Ctn],
        Rename =.. [Rnm, ExtCtg, Pred, Alias],
        assertz(ppDef_((Head :- Rename, Lookup)))
        ;	Head =.. [Def, Pred, CExCtx, Call, Ctn],
        assertz(ppDef_((Head:-Lookup)))
        ),
        fail.

        generateCategory_extendsDefClauses(_, _).



   //the database built-in methods need to check if a local declaration or a local definition
   //exists for a predicate; in order to avoid predicate existence errors, we need to generate
   //a catchall clause for static objects when there are no local predicate declarations or no
   //local predicate definitions

        generate_objectCatchallLocalDclClause(true, _).

        generate_objectCatchallLocalDclClause(false, Dcl) :-
        (	ppDynamic_' ->
   //dynamic object
        true
        ;	% generate a catchall clause for static objects
        functor(Head, Dcl, 4),
        assertz(ppDcl_((Head:-fail)))
        ).



        generate_objectCatchallDefClauses(true, _).

        generate_objectCatchallDefClauses(false, Def) :-
        (	ppDynamic_' ->
   //dynamic object
        true
        ;	% generate a catchall clause for static objects
        functor(Head, Def, 3),
        assertz(ppDef_((Head:-fail)))
        ).



        generate_prototypeClauses' :-
        pp_object_(Obj, _, Dcl, Def, Super, _, _, DDcl, DDef, Rnm, _),
        CompilerFlag(complements, Complements),
        generate_prototypeDclClauses(Obj, Dcl, DDcl, Rnm, Complements),
        generate_prototypeDefClauses(Obj, Def, DDef, Rnm, Complements),
        generate_prototypeSuperClauses(Super, Rnm).



        generate_prototypeDclClauses(Obj, Dcl, DDcl, Rnm, Complements) :-
   //first, generate the local table of predicate declarations:
        generateDcl_tableClauses(Local),
   //second, generate linking clauses for accessing both local
   //declarations and declarations in related entities (some
   //linking clauses depend on the existence of local predicate
   //declarations
        (	Complements == allow ->
   //complementing categories are allowed to override local predicate declarations
        generate_prototypeComplementsDclClauses(Obj, Dcl),
        generate_prototypeLocalDclClauses(Local, Complements, Obj, Dcl, DDcl)
        ;	Complements == restrict ->
   //complementing categories can add to but not override local predicate declarations
        generate_prototypeLocalDclClauses(Local, Complements, Obj, Dcl, DDcl),
        generate_prototypeComplementsDclClauses(Obj, Dcl)
        ;	% Complements == deny ->
        generate_prototypeLocalDclClauses(Local, Complements, Obj, Dcl, DDcl)
        ),
        generate_prototypeImplementsDclClauses(Dcl, Rnm),
        generate_prototypeImportsDclClauses(Dcl, Rnm),
        generate_prototype_extendsDclClauses(Dcl, Rnm),
   //third, add a catchall clause if necessary
        generate_objectCatchallLocalDclClause(Local, Dcl).



        generate_prototypeComplementsDclClauses(Obj, Dcl) :-
        Head =.. [Dcl, Pred, Scope, Meta, Flags, SCtn, TCtn],
        Lookup = Complemented_object(Obj, Dcl, Pred, Scope, Meta, Flags, SCtn, TCtn),
        assertz(ppDcl_((Head:-Lookup))).



        generate_prototypeLocalDclClauses(true, _, Obj, Dcl, DDcl) :-
   //there are local (compile-time) predicate declarations
        HeadDcl =.. [Dcl, Pred, Scope, Meta, Flags, Obj, Obj],
        BodyDcl =.. [Dcl, Pred, Scope, Meta, Flags],
   //lookup access to local, static, predicate declarations
        assertz(ppDcl_((HeadDcl:-BodyDcl))),
        (	CompilerFlag(dynamicDeclarations, allow) ->
        HeadDDcl =.. [Dcl, Pred, Scope, no, 2, Obj, Obj],
        BodyDDcl =.. [DDcl, Pred, Scope],
   //lookup access to local, dynamic, (runtime) predicate declarations
        assertz(ppDcl_((HeadDDcl:-BodyDDcl)))
        ;	true
        ).

        generate_prototypeLocalDclClauses(false, Complements, Obj, Dcl, DDcl) :-
   //no local (compile-time) predicate declarations
        (	CompilerFlag(dynamicDeclarations, allow) ->
        HeadDDcl =.. [Dcl, Pred, Scope, no, 2, Obj, Obj],
        BodyDDcl =.. [DDcl, Pred, Scope],
   //lookup access to local, dynamic, (runtime) predicate declarations
        assertz(ppDcl_((HeadDDcl:-BodyDDcl)))
        ;	Complements == deny,
        \+ ppImplemented_protocol_(_, _, _, _, _),
        \+ ppImportedCategory_(_, _, _, _, _, _),
        \+ pp_extended_object_(_, _, _, _, _, _, _, _, _, _, _) ->
   //standalone prototype with no access to predicate declarations
        functor(HeadDDcl, Dcl, 6),
   //catchall clause to avoid lookup errors
        assertz(ppDcl_((HeadDDcl:-fail)))
        ;	true
        ).



        generate_prototypeImplementsDclClauses(ODcl, Rnm) :-
        ppImplemented_protocol_(Ptc, Obj, _, PDcl, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [PDcl, Pred, Scope, Meta, Flags, TCtn]
        ;	RelationScope == protected ->
        Lookup0 =.. [PDcl, Pred, Scope2, Meta, Flags, TCtn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup =.. [PDcl, Pred, _, Meta, Flags, TCtn]
        ),
        (	pp_predicateAlias_(Ptc, _, _, _, _, _) ->
        Head =.. [ODcl, Alias, Scope, Meta, Flags, Obj, TCtn],
        Rename =.. [Rnm, Ptc, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [ODcl, Pred, Scope, Meta, Flags, Obj, TCtn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generate_prototypeImplementsDclClauses(_, _).



        generate_prototypeImportsDclClauses(ODcl, Rnm) :-
        ppImportedCategory_(Ctg, Obj, _, CDcl, _, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [CDcl, Pred, Scope, Meta, Flags, TCtn]
        ;	RelationScope == protected ->
        Lookup0 =.. [CDcl, Pred, Scope2, Meta, Flags, TCtn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup =.. [CDcl, Pred, _, Meta, Flags, TCtn]
        ),
        (	pp_predicateAlias_(Ctg, _, _, _, _, _) ->
        Head =.. [ODcl, Alias, Scope, Meta, Flags, Obj, TCtn],
        Rename =.. [Rnm, Ctg, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [ODcl, Pred, Scope, Meta, Flags, Obj, TCtn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generate_prototypeImportsDclClauses(_, _).



        generate_prototype_extendsDclClauses(ODcl, Rnm) :-
        pp_extended_object_(Parent, Obj, _, PDcl, _, _, _, _, _, _, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [PDcl, Pred, Scope, Meta, Flags, SCtn, TCtn]
        ;	RelationScope == protected ->
        Lookup0 =.. [PDcl, Pred, Scope2, Meta, Flags, SCtn, TCtn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup0 =.. [PDcl, Pred, Scope2, Meta, Flags, SCtn2, TCtn],
        Lookup = (Lookup0, FilterScopeContainer(Scope2, SCtn2, Obj, SCtn))
        ),
        (	pp_predicateAlias_(Parent, _, _, _, _, _) ->
        Head =.. [ODcl, Alias, Scope, Meta, Flags, SCtn, TCtn],
        Rename =.. [Rnm, Parent, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [ODcl, Pred, Scope, Meta, Flags, SCtn, TCtn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generate_prototype_extendsDclClauses(_, _).



        generate_prototypeDefClauses(Obj, Def, DDef, Rnm, Complements) :-
   //some linking clauses depend on the existence of local predicate definitions
        (	ppDef_(_) ->
        Local = true
        ;	Local = false
        ),
        (	Complements == allow ->
   //complementing categories are allowed to override local predicate definitions
        generate_prototypeComplementsDefClauses(Obj, Def),
        generate_prototypeLocalDefClauses(Local, Obj, Def, DDef)
        ;	Complements == restrict ->
   //complementing categories can add to but not override local predicate definitions
        generate_prototypeLocalDefClauses(Local, Obj, Def, DDef),
        generate_prototypeComplementsDefClauses(Obj, Def)
        ;	% Complements == deny ->
        generate_prototypeLocalDefClauses(Local, Obj, Def, DDef)
        ),
        generate_prototypeImportsDefClauses(Def, Rnm),
        generate_prototype_extendsDefClauses(Def, Rnm),
   //add a catchall clause if necessary
        generate_objectCatchallDefClauses(Local, Def).



        generate_prototypeComplementsDefClauses(Obj, Def) :-
        Head =.. [Def, Pred, ExCtx, Call, Obj, TCtn],
        Lookup = Complemented_object(Obj, Def, Pred, ExCtx, Call, TCtn),
        assertz(ppDef_((Head:-Lookup))).



        generate_prototypeLocalDefClauses(true, Obj, Def, DDef) :-
   //there are local (compile-time) predicate definitions
        executionContext_this_entity(ExCtx, Obj, Obj),
        Head =.. [Def, Pred, ExCtx, Call, Obj, Obj],
        BodyDef =.. [Def, Pred, ExCtx, Call],
   //lookup access to local, static, predicate definitions
        assertz(ppDef_((Head:-BodyDef))),
        BodyDDef =.. [DDef, Pred, ExCtx, Call],
   //lookup access to local, dynamic, (runtime) predicate definitions
        assertz(ppDef_((Head:-BodyDDef))).

        generate_prototypeLocalDefClauses(false, Obj, Def, DDef) :-
   //no local (compile-time) predicate definitions
        executionContext_this_entity(ExCtx, Obj, Obj),
        Head =.. [Def, Pred, ExCtx, Call, Obj, Obj],
        BodyDDef =.. [DDef, Pred, ExCtx, Call],
   //lookup access to local, dynamic, (runtime) predicate definitions
        assertz(ppDef_((Head:-BodyDDef))).



        generate_prototypeImportsDefClauses(ODef, Rnm) :-
        ppImportedCategory_(Ctg, Obj, _, _, CDef, _),
        executionContext_update_this_entity(OExCtx, Obj, Obj, CExCtx, Obj, Ctg),
        Lookup =.. [CDef, Pred, CExCtx, Call, TCtn],
        (	pp_predicateAlias_(Ctg, _, _, _, _, _) ->
        Head =.. [ODef, Alias, OExCtx, Call, Obj, TCtn],
        Rename =.. [Rnm, Ctg, Pred, Alias],
        assertz(ppDef_((Head :- Rename, Lookup)))
        ;	Head =.. [ODef, Pred, OExCtx, Call, Obj, TCtn],
        assertz(ppDef_((Head:-Lookup)))
        ),
        fail.

        generate_prototypeImportsDefClauses(_, _).



        generate_prototype_extendsDefClauses(ODef, Rnm) :-
        pp_extended_object_(Parent, Obj, _, _, PDef, _, _, _, _, _, _),
        executionContext_update_this_entity(OExCtx, Obj, Obj, PExCtx, Parent, Parent),
        Lookup =.. [PDef, Pred, PExCtx, Call, SCtn, TCtn],
        (	pp_predicateAlias_(Parent, _, _, _, _, _) ->
        Head =.. [ODef, Alias, OExCtx, Call, SCtn, TCtn],
        Rename =.. [Rnm, Parent, Pred, Alias],
        assertz(ppDef_((Head :- Rename, Lookup)))
        ;	Head =.. [ODef, Pred, OExCtx, Call, SCtn, TCtn],
        assertz(ppDef_((Head:-Lookup)))
        ),
        fail.

        generate_prototype_extendsDefClauses(_, _).



   //we can have a root object where super have nowhere to go ...

        generate_prototypeSuperClauses(Super, _) :-
        \+ ppImportedCategory_(_, _, _, _, _, _),
        \+ pp_extended_object_(_, _, _, _, _, _, _, _, _, _, _),
        functor(Head, Super, 5),
        assertz(ppSuper_((Head:-fail))),
        !.

   //... or we may import some categories

        generate_prototypeSuperClauses(Super, Rnm) :-
        ppImportedCategory_(Ctg, Obj, _, _, CDef, _),
   //the entity in the object execution context is usually the object itself
   //but it can also be a complementing category; thus, the argument must be
   //left uninstantiated but it will be bound by the runtime
        executionContext_update_this_entity(OExCtx, Obj, _, CExCtx, Obj, Ctg),
        Lookup =.. [CDef, Pred, CExCtx, Call, TCtn],
        (	pp_predicateAlias_(Ctg, _, _, _, _, _) ->
        Head =.. [Super, Alias, OExCtx, Call, Obj, TCtn],
        Rename =.. [Rnm, Ctg, Pred, Alias],
        assertz(ppSuper_((Head :- Rename, Lookup)))
        ;	Head =.. [Super, Pred, OExCtx, Call, Obj, TCtn],
        assertz(ppSuper_((Head:-Lookup)))
        ),
        fail.

   //... or we may extend some objects

        generate_prototypeSuperClauses(Super, Rnm) :-
        pp_extended_object_(Parent, Obj, _, _, PDef, _, _, _, _, _, _),
   //the entity in the object execution context is usually the object itself
   //but it can also be a complementing category; thus, the argument must be
   //left uninstantiated but it will be bound by the runtime
        executionContext_update_this_entity(OExCtx, Obj, _, PExCtx, Parent, Parent),
        Lookup =.. [PDef, Pred, PExCtx, Call, SCtn, TCtn],
        (	pp_predicateAlias_(Parent, _, _, _, _, _) ->
        Head =.. [Super, Alias, OExCtx, Call, SCtn, TCtn],
        Rename =.. [Rnm, Parent, Pred, Alias],
        assertz(ppSuper_((Head :- Rename, Lookup)))
        ;	Head =.. [Super, Pred, OExCtx, Call, SCtn, TCtn],
        assertz(ppSuper_((Head:-Lookup)))
        ),
        fail.

        generate_prototypeSuperClauses(_, _).



        generateIcClauses' :-
        pp_object_(Obj, _, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, _),
        CompilerFlag(complements, Complements),
        generateIcDclClauses(Obj, Dcl, IDcl, DDcl, Rnm, Complements),
        generateIcDefClauses(Obj, Def, IDef, DDef, Rnm, Complements),
        generateIcSuperClauses(Obj, Super, Rnm).



        generateIcDclClauses(Obj, Dcl, IDcl, DDcl, Rnm, Complements) :-
   //first, generate the local table of predicate declarations:
        generateDcl_tableClauses(Local),
   //second, generate linking clauses for accessing declarations
   //in related entities (for an instance, the lookup for a predicate
   //declaration always start at its classes)
        generateIcInstantiatesDclClauses(Dcl, Rnm),
   //third, add a catchall clause if necessary
        generate_objectCatchallLocalDclClause(Local, Dcl),
   //finaly, generate linking clauses for accessing declarations
   //when we reach the class being compiled during a lookup
   //from a descendant instance
        generateIcIdclClauses(Local, Obj, Dcl, IDcl, DDcl, Rnm, Complements).



        generateIcInstantiatesDclClauses(ODcl, _) :-
        \+ ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _),
   //no meta-class for the class we're compiling
        !,
        functor(Head, ODcl, 6),
        assertz(ppDcl_((Head:-fail))).

        generateIcInstantiatesDclClauses(ODcl, Rnm) :-
        ppInstantiatedClass_(Class, Obj, _, _, _, _, CIDcl, _, _, _, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [CIDcl, Pred, Scope, Meta, Flags, SCtn, TCtn]
        ;	RelationScope == protected ->
        Lookup0 =.. [CIDcl, Pred, Scope2, Meta, Flags, SCtn, TCtn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup0 =.. [CIDcl, Pred, Scope2, Meta, Flags, SCtn2, TCtn],
        Lookup = (Lookup0, FilterScopeContainer(Scope2, SCtn2, Obj, SCtn))
        ),
        (	pp_predicateAlias_(Class, _, _, _, _, _) ->
        Head =.. [ODcl, Alias, Scope, Meta, Flags, SCtn, TCtn],
        Rename =.. [Rnm, Class, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [ODcl, Pred, Scope, Meta, Flags, SCtn, TCtn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generateIcInstantiatesDclClauses(_, _).



   //generates the declaration linking clauses that are used
   //when traversing specialization links in order to lookup
   //a predicate declaration for a descendant instance

        generateIcIdclClauses(Local, Obj, Dcl, IDcl, DDcl, Rnm, Complements) :-
   //generate linking clauses for accessing declarations in related entities
        (	Complements == allow ->
   //complementing categories are allowed to override local predicate declarations
        generateIcComplementsIdclClauses(Obj, IDcl),
        generateIcLocalIdclClauses(Local, Complements, Obj, Dcl, IDcl, DDcl)
        ;	Complements == restrict ->
   //complementing categories can add to but not override local predicate declarations
        generateIcLocalIdclClauses(Local, Complements, Obj, Dcl, IDcl, DDcl),
        generateIcComplementsIdclClauses(Obj, IDcl)
        ;	% Complements == deny ->
        generateIcLocalIdclClauses(Local, Complements, Obj, Dcl, IDcl, DDcl)
        ),
        generateIcImplementsIdclClauses(IDcl, Rnm),
        generateIcImportsIdclClauses(IDcl, Rnm),
        generateIcSpecializesIdclClauses(IDcl, Rnm).



        generateIcComplementsIdclClauses(Obj, IDcl) :-
        Head =.. [IDcl, Pred, Scope, Meta, Flags, SCtn, TCtn],
        Lookup = Complemented_object(Obj, IDcl, Pred, Scope, Meta, Flags, SCtn, TCtn),
        assertz(ppDcl_((Head:-Lookup))).



        generateIcLocalIdclClauses(true, _, Obj, Dcl, IDcl, DDcl) :-
   //there are local (compile-time) predicate declarations
        HeadDcl =.. [IDcl, Pred, Scope, Meta, Flags, Obj, Obj],
        BodyDcl =.. [Dcl, Pred, Scope, Meta, Flags],
   //lookup access to local, static, predicate declarations
        assertz(ppDcl_((HeadDcl:-BodyDcl))),
        (	CompilerFlag(dynamicDeclarations, allow) ->
        HeadDDcl =.. [IDcl, Pred, Scope, no, 2, Obj, Obj],
        BodyDDcl =.. [DDcl, Pred, Scope],
   //lookup access to local, dynamic, (runtime) predicate declarations
        assertz(ppDcl_((HeadDDcl:-BodyDDcl)))
        ;	true
        ).

        generateIcLocalIdclClauses(false, Complements, Obj, _, IDcl, DDcl) :-
   //no local (compile-time) predicate declarations
        (	CompilerFlag(dynamicDeclarations, allow) ->
        HeadDDcl =.. [IDcl, Pred, Scope, no, 2, Obj, Obj],
        BodyDDcl =.. [DDcl, Pred, Scope],
   //lookup access to local, dynamic, (runtime) predicate declarations
        assertz(ppDcl_((HeadDDcl:-BodyDDcl)))
        ;	Complements == deny,
        \+ ppImplemented_protocol_(_, _, _, _, _),
        \+ ppImportedCategory_(_, _, _, _, _, _),
        \+ ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
   //standalone class with no access to predicate declarations
        functor(HeadDDcl, IDcl, 6),
   //catchall clause to avoid lookup errors
        assertz(ppDcl_((HeadDDcl:-fail)))
        ;	true
        ).



        generateIcImplementsIdclClauses(OIDcl, Rnm) :-
        ppImplemented_protocol_(Ptc, Obj, _, PDcl, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [PDcl, Pred, Scope, Meta, Flags, TCtn]
        ;	RelationScope == protected ->
        Lookup0 =.. [PDcl, Pred, Scope2, Meta, Flags, TCtn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup =.. [PDcl, Pred, _, Meta, Flags, TCtn]
        ),
        (	pp_predicateAlias_(Ptc, _, _, _, _, _) ->
        Head =.. [OIDcl, Alias, Scope, Meta, Flags, Obj, TCtn],
        Rename =.. [Rnm, Ptc, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [OIDcl, Pred, Scope, Meta, Flags, Obj, TCtn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generateIcImplementsIdclClauses(_, _).



        generateIcImportsIdclClauses(OIDcl, Rnm) :-
        ppImportedCategory_(Ctg, Obj, _, CDcl, _, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [CDcl, Pred, Scope, Meta, Flags, TCtn]
        ;	RelationScope == protected ->
        Lookup0 =.. [CDcl, Pred, Scope2, Meta, Flags, TCtn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup =.. [CDcl, Pred, _, Meta, Flags, TCtn]
        ),
        (	pp_predicateAlias_(Ctg, _, _, _, _, _) ->
        Head =.. [OIDcl, Alias, Scope, Meta, Flags, Obj, TCtn],
        Rename =.. [Rnm, Ctg, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [OIDcl, Pred, Scope, Meta, Flags, Obj, TCtn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generateIcImportsIdclClauses(_, _).



        generateIcSpecializesIdclClauses(CIDcl, Rnm) :-
        ppSpecializedClass_(Super, Obj, _, _, _, _, SIDcl, _, _, _, RelationScope),
        (	RelationScope == (public) ->
        Lookup =.. [SIDcl, Pred, Scope, Meta, Flags, SCtn, TCtn]
        ;	RelationScope == protected ->
        Lookup0 =.. [SIDcl, Pred, Scope2, Meta, Flags, SCtn, TCtn],
        Lookup = (Lookup0, FilterScope(Scope2, Scope))
        ;	Scope = p,
        Lookup0 =.. [SIDcl, Pred, Scope2, Meta, Flags, SCtn2, TCtn],
        Lookup = (Lookup0, FilterScopeContainer(Scope2, SCtn2, Obj, SCtn))
        ),
        (	pp_predicateAlias_(Super, _, _, _, _, _) ->
        Head =.. [CIDcl, Alias, Scope, Meta, Flags, SCtn, TCtn],
        Rename =.. [Rnm, Super, Pred, Alias],
        assertz(ppDcl_((Head :- Rename, Lookup)))
        ;	Head =.. [CIDcl, Pred, Scope, Meta, Flags, SCtn, TCtn],
        assertz(ppDcl_((Head:-Lookup)))
        ),
        fail.

        generateIcSpecializesIdclClauses(_, _).



   //lookup of predicate definitions start at the instance itself
   //(not at its classes as it's the case for predicate declarations)

        generateIcDefClauses(Obj, Def, IDef, DDef, Rnm, Complements) :-
   //some linking clauses depend on the existence of local predicate definitions
        (	ppDef_(_) ->
        Local = true
        ;	Local = false
        ),
        (	Complements == allow ->
   //complementing categories are allowed to override local predicate definitions
        generateIcComplementsDefClauses(Obj, Def),
        generateIcLocalDefClauses(Local, Obj, Def, DDef)
        ;	Complements == restrict ->
   //complementing categories can add to but not override local predicate definitions
        generateIcLocalDefClauses(Local, Obj, Def, DDef),
        generateIcComplementsDefClauses(Obj, Def)
        ;	% Complements == deny ->
        generateIcLocalDefClauses(Local, Obj, Def, DDef)
        ),
        generateIcImportsDefClauses(Def, Rnm),
        generateIcInstantiatesDefClauses(Def, Rnm),
   //add a catchall clause if necessary
        generate_objectCatchallDefClauses(Local, Def),
   //generate linking clauses for accessing definitions when
   //we reach the class being compiled during a lookup from
   //a descendant instance
        generateIcIdefClauses(Local, Obj, Def, IDef, DDef, Rnm, Complements).



        generateIcComplementsDefClauses(Obj, Def) :-
        Head =.. [Def, Pred, ExCtx, Call, Obj, TCtn],
        Lookup = Complemented_object(Obj, Def, Pred, ExCtx, Call, TCtn),
        assertz(ppDef_((Head:-Lookup))).



        generateIcLocalDefClauses(true, Obj, Def, DDef) :-
   //there are local (compile-time) predicate definitions
        executionContext_this_entity(ExCtx, Obj, Obj),
        Head =.. [Def, Pred, ExCtx, Call, Obj, Obj],
        BodyDef =.. [Def, Pred, ExCtx, Call],
   //lookup access to local, static, predicate definitions
        assertz(ppDef_((Head:-BodyDef))),
        BodyDDef =.. [DDef, Pred, ExCtx, Call],
   //lookup access to local, dynamic, (runtime) predicate definitions
        assertz(ppDef_((Head:-BodyDDef))).

        generateIcLocalDefClauses(false, Obj, Def, DDef) :-
   //no local (compile-time) predicate definitions
        executionContext_this_entity(ExCtx, Obj, Obj),
        Head =.. [Def, Pred, ExCtx, Call, Obj, Obj],
        BodyDDef =.. [DDef, Pred, ExCtx, Call],
   //lookup access to local, dynamic, (runtime) predicate definitions
        assertz(ppDef_((Head:-BodyDDef))).



        generateIcImportsDefClauses(ODef, Rnm) :-
        ppImportedCategory_(Ctg, Obj, _, _, CDef, _),
        executionContext_update_this_entity(OExCtx, Obj, Obj, CExCtx, Obj, Ctg),
        Lookup =.. [CDef, Pred, CExCtx, Call, TCtn],
        (	pp_predicateAlias_(Ctg, _, _, _, _, _) ->
        Head =.. [ODef, Alias, OExCtx, Call, Obj, TCtn],
        Rename =.. [Rnm, Ctg, Pred, Alias],
        assertz(ppDef_((Head :- Rename, Lookup)))
        ;	Head =.. [ODef, Pred, OExCtx, Call, Obj, TCtn],
        assertz(ppDef_((Head:-Lookup)))
        ),
        fail.

        generateIcImportsDefClauses(_, _).



        generateIcInstantiatesDefClauses(ODef, Rnm) :-
        ppInstantiatedClass_(Class, Obj, _, _, _, _, _, CIDef, _, _, _),
        executionContext_update_this_entity(OExCtx, Obj, Obj, CExCtx, Class, Class),
        Lookup =.. [CIDef, Pred, CExCtx, Call, SCtn, TCtn],
        (	pp_predicateAlias_(Class, _, _, _, _, _) ->
        Head =.. [ODef, Alias, OExCtx, Call, SCtn, TCtn],
        Rename =.. [Rnm, Class, Pred, Alias],
        assertz(ppDef_((Head :- Rename, Lookup)))
        ;	Head =.. [ODef, Pred, OExCtx, Call, SCtn, TCtn],
        assertz(ppDef_((Head:-Lookup)))
        ),
        fail.

        generateIcInstantiatesDefClauses(_, _).



   //generates the definition linking clauses that are used
   //when traversing specialization links in order to lookup
   //a predicate definition for a descendant instance

        generateIcIdefClauses(Local, Obj, Def, IDef, DDef, Rnm, Complements) :-
        (	Complements == allow ->
   //complementing categories are allowed to override local predicate definitions
        generateIcComplementsIdefClauses(Obj, IDef),
        generateIcLocalIdefClauses(Local, Obj, Def, IDef, DDef)
        ;	Complements == restrict ->
   //complementing categories can add to but not override local predicate definitions
        generateIcLocalIdefClauses(Local, Obj, Def, IDef, DDef),
        generateIcComplementsIdefClauses(Obj, IDef)
        ;	% Complements == deny ->
        generateIcLocalIdefClauses(Local, Obj, Def, IDef, DDef)
        ),
        generateIcComplementsIdefClauses(Obj, IDef),
        generateIcLocalIdefClauses(Local, Obj, Def, IDef, DDef),
        generateIcImportsIdefClauses(IDef, Rnm),
        generateIcSpecializesIdefClauses(IDef, Rnm).



        generateIcComplementsIdefClauses(Obj, IDef) :-
        Head =.. [IDef, Pred, ExCtx, Call, Obj, TCtn],
        Lookup = Complemented_object(Obj, IDef, Pred, ExCtx, Call, TCtn),
        assertz(ppDef_((Head:-Lookup))).



        generateIcLocalIdefClauses(true, Obj, Def, IDef, DDef) :-
   //there are local (compile-time) predicate definitions
        executionContext_this_entity(ExCtx, Obj, Obj),
        Head =.. [IDef, Pred, ExCtx, Call, Obj, Obj],
        BodyDef =.. [Def, Pred, ExCtx, Call],
   //lookup access to local, static, predicate definitions
        assertz(ppDef_((Head:-BodyDef))),
        BodyDDef =.. [DDef, Pred, ExCtx, Call],
   //lookup access to local, dynamic, (runtime) predicate definitions
        assertz(ppDef_((Head:-BodyDDef))).

        generateIcLocalIdefClauses(false, Obj, _, IDef, DDef) :-
   //no local (compile-time) predicate definitions
        executionContext_this_entity(ExCtx, Obj, Obj),
        Head =.. [IDef, Pred, ExCtx, Call, Obj, Obj],
        BodyDDef =.. [DDef, Pred, ExCtx, Call],
   //lookup access to local, dynamic, (runtime) predicate definitions
        assertz(ppDef_((Head:-BodyDDef))).



        generateIcImportsIdefClauses(OIDef, Rnm) :-
        ppImportedCategory_(Ctg, Obj, _, _, CDef, _),
        executionContext_update_this_entity(OExCtx, Obj, Obj, CExCtx, Obj, Ctg),
        Lookup =.. [CDef, Pred, CExCtx, Call, TCtn],
        (	pp_predicateAlias_(Ctg, _, _, _, _, _) ->
        Head =.. [OIDef, Alias, OExCtx, Call, Obj, TCtn],
        Rename =.. [Rnm, Ctg, Pred, Alias],
        assertz(ppDef_((Head :- Rename, Lookup)))
        ;	Head =.. [OIDef, Pred, OExCtx, Call, Obj, TCtn],
        assertz(ppDef_((Head:-Lookup)))
        ),
        fail.

        generateIcImportsIdefClauses(_, _).



        generateIcSpecializesIdefClauses(CIDef, Rnm) :-
        ppSpecializedClass_(Super, Class, _, _, _, _, _, SIDef, _, _, _),
        executionContext_update_this_entity(CExCtx, Class, Class, SExCtx, Super, Super),
        Lookup =.. [SIDef, Pred, SExCtx, Call, SCtn, TCtn],
        (	pp_predicateAlias_(Super, _, _, _, _, _) ->
        Head =.. [CIDef, Alias, CExCtx, Call, SCtn, TCtn],
        Rename =.. [Rnm, Super, Pred, Alias],
        assertz(ppDef_((Head :- Rename, Lookup)))
        ;	Head =.. [CIDef, Pred, CExCtx, Call, SCtn, TCtn],
        assertz(ppDef_((Head:-Lookup)))
        ),
        fail.

        generateIcSpecializesIdefClauses(_, _).



   //we can have a root object where "super" have nowhere to go ...

        generateIcSuperClauses(Obj, Super, _) :-
        \+ ppImportedCategory_(_, _, _, _, _, _),
        \+ ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _),
        \+ (ppInstantiatedClass_(Class, _, _, _, _, _, _, _, _, _, _), Class \= Obj),
        functor(Head, Super, 5),
        assertz(ppSuper_((Head:-fail))),
        !.

   //... or we may import some categories

        generateIcSuperClauses(Obj, Super, Rnm) :-
        ppImportedCategory_(Ctg, Obj, _, _, CDef, _),
   //the entity in the object execution context is usually the object itself
   //but it can also be a complementing category; thus, the argument must be
   //left uninstantiated but it will be bound by the runtime
        executionContext_update_this_entity(OExCtx, _, Obj, CExCtx, Obj, Ctg),
        Lookup =.. [CDef, Pred, CExCtx, Call, TCtn],
        (	pp_predicateAlias_(Ctg, _, _, _, _, _) ->
        Head =.. [Super, Alias, OExCtx, Call, Obj, TCtn],
        Rename =.. [Rnm, Ctg, Pred, Alias],
        assertz(ppSuper_((Head :- Rename, Lookup)))
        ;	Head =.. [Super, Pred, OExCtx, Call, Obj, TCtn],
        assertz(ppSuper_((Head:-Lookup)))
        ),
        fail.

   //... or predicates can be redefined in instances...

        generateIcSuperClauses(Obj, Super, Rnm) :-
        ppInstantiatedClass_(Class, Obj, _, _, _, _, _, CIDef, _, _, _),
   //we can ignore class self-instantiation, which is often used in reflective designs
        Class \= Obj,
   //the entity in the object execution context is usually the object itself
   //but it can also be a complementing category; thus, the argument must be
   //left uninstantiated but it will be bound by the runtime
        executionContext_update_this_entity(OExCtx, _, Obj, CExCtx, Class, Class),
        Lookup =.. [CIDef, Pred, CExCtx, Call, SCtn, TCtn],
   //the following restriction allows us to distinguish the two "super" clauses that
   //are generated when an object both instantiates and specializes other objects
        executionContext(OExCtx, _, _, Obj, Obj, _, _),
        (	pp_predicateAlias_(Class, _, _, _, _, _) ->
        Head =.. [Super, Alias, OExCtx, Call, SCtn, TCtn],
        Rename =.. [Rnm, Class, Pred, Alias],
        assertz(ppSuper_((Head :- Rename, Lookup)))
        ;	Head =.. [Super, Pred, OExCtx, Call, SCtn, TCtn],
        assertz(ppSuper_((Head:-Lookup)))
        ),
        fail.

   //... or/and in subclasses...

        generateIcSuperClauses(Class, Super, Rnm) :-
        ppSpecializedClass_(Superclass, Class, _, _, _, _, _, SIDef, _, _, _),
   //the entity in the object execution context is usually the class itself
   //but it can also be a complementing category; thus, the argument must be
   //left uninstantiated but it will be bound by the runtime
        executionContext_update_this_entity(CExCtx, _, Class, SExCtx, Superclass, Superclass),
        Lookup =.. [SIDef, Pred, SExCtx, Call, SCtn, TCtn],
        (	pp_predicateAlias_(Superclass, _, _, _, _, _) ->
        Head =.. [Super, Alias, CExCtx, Call, SCtn, TCtn],
        Rename =.. [Rnm, Superclass, Pred, Alias],
        assertz(ppSuper_((Head :- Rename, Lookup)))
        ;	Head =.. [Super, Pred, CExCtx, Call, SCtn, TCtn],
        assertz(ppSuper_((Head:-Lookup)))
        ),
        fail.

        generateIcSuperClauses(_, _, _).



   //Compile_predicateCalls'
        %
   //compiles predicate calls in entity clause rules and in initialization goals
        %
   //all predicate calls are compiled on this compiler second stage to take advantage
   //of the information about declared and defined predicates collected on the first
   //stage, thus making predicate declaration and definition order irrelevant; this
   //allows us to deal with e.g. meta-predicate directives and redefined built-in
   //predicates which may be textually defined in an entity after their calls

        Compile_predicateCalls' :-
        retractall(pp_term_variable_namesFileLines_(_, _, _, _)),
   //avoid querying the optimize flag for each compiled term
        CompilerFlag(optimize, Optimize),
        Compile_predicateCalls(Optimize).


        Compile_predicateCalls(Optimize) :-
   //user-defined terms
        retract(pp_entity_term_(Term, SourceData, Lines)),
        Compile_predicateCalls(Term, SourceData, Optimize, TTerm),
        assertz(ppFinal_entity_term_(TTerm, Lines)),
        fail.

        Compile_predicateCalls(_) :-
   //coinductive auxiliary clauses
        ppCoinductive_(Head, TestHead, HeadExCtx, TCHead, BodyExCtx, THead, DHead),
        ppDefines_predicate_(Head, _, _, _, _, _),
        AddCoinductive_predicateAuxClause(Head, TestHead, HeadExCtx, TCHead, BodyExCtx, THead, DHead),
        fail.

        Compile_predicateCalls(Optimize) :-
   //other auxiliary clauses
        retract(pp_entityAuxClause_(Clause)),
        Compile_predicateCalls(Clause, nil, Optimize, TClause),
        assertz(ppFinal_entityAuxClause_(TClause)),
        fail.

        Compile_predicateCalls(Optimize) :-
   //initialization/1 goals
        retract(pp_objectInitialization_(Goal, SourceData, Lines)),
        Compile_predicateCalls(Goal, SourceData, Optimize, TGoal),
        assertz(ppFinal_objectInitialization_(TGoal, Lines)),
        fail.

        Compile_predicateCalls(Optimize) :-
   //other initialization goals found on proprietary Prolog directives
        retract(pp_entity_metaDirective_(Directive, SourceData, _)),
        Compile_predicateCalls(Directive, SourceData, Optimize, TDirective),
        assertz(ppDirective_(TDirective)),
        fail.

        Compile_predicateCalls(_).



   //Compile_predicateCalls(+callable, +compound, +atom, -callable)

        Compile_predicateCalls(Term, SourceData, Optimize, TTerm) :-
        (	catch(
        Compile_predicateCallsInner(Term, SourceData, Optimize, TTerm),
        Error,
        Compile_predicateCalls_error_handler(Term, Error)
        ) ->
        true
        ;	% unexpected compilation failure
        Compile_predicateCalls_error_handler(Term, system_error)
        ).


        Compile_predicateCallsInner(Body, SourceData, Optimize, TRule) :-
        (	SourceData = sd(Term, VariableNames, File, Lines) ->
        assertz(pp_term_variable_namesFileLines_(Term, VariableNames, File, Lines))
        ;	true
        ),
        Compile_predicateCalls(Body, Optimize, TRule),
        retractall(pp_term_variable_namesFileLines_(_, _, _, _)).



        Compile_predicateCalls_error_handler(Term, Error) :-
        Internal_term_to_user_term(Term, UserTerm),
        throw(error(Error,UserTerm)).


        Internal_term_to_user_term({Term}, term(Term)).

        Internal_term_to_user_term(srule(_,Body,Ctx), clause((Head:-Body))) :-
        CompCtx_head(Ctx, Head).

        Internal_term_to_user_term(dsrule(_,_,Body,Ctx), clause((Head:-Body))) :-
        CompCtx_head(Ctx, Head).

        Internal_term_to_user_term(drule(_,_,Body,Ctx), clause((Head:-Body))) :-
        CompCtx_head(Ctx, Head).

        Internal_term_to_user_term(ddrule(_,_,_,Body,Ctx), clause((Head:-Body))) :-
        CompCtx_head(Ctx, Head).

        Internal_term_to_user_term(goal(Body,_), directive(initialization(Body))).

        Internal_term_to_user_term(dgoal(Body,_), directive(initialization(Body))).

        Internal_term_to_user_term(directive(Directive,_), directive(Directive)).


   //entity term is final
        Compile_predicateCalls({Term}, _, Term).

   //static predicate rule
        Compile_predicateCalls(srule(THead,Body,Ctx), Optimize, TClause) :-
        Compile_body(Body, FBody, _, Ctx),
        (	Optimize == on ->
        Simplify_goal(FBody, SBody)
        ;	SBody = FBody
        ),
        (	SBody == true ->
        TClause = THead
        ;	TClause = (THead:-SBody)
        ).

   //debug version of static predicate rule
        Compile_predicateCalls(dsrule(THead,DHead,Body,Ctx), _, (THead:-DHead,DBody)) :-
        Compile_body(Body, _, DBody, Ctx).

   //dynamic predicate rule
        Compile_predicateCalls(drule(THead,Nop,Body,Ctx), Optimize, TClause) :-
        Compile_body(Body, TBody0, _, Ctx),
        (	Optimize == on ->
        Simplify_goal(TBody0, TBody)
        ;	TBody = TBody0
        ),
        (	TBody == true ->
        TClause = (THead:-Nop)
        ;	TClause = (THead:-Nop,TBody)
        ).

   //debug version of dynamic predicate rule
        Compile_predicateCalls(ddrule(THead,Nop,DHead,Body,Ctx), _, (THead:-Nop,DHead,DBody)) :-
        Compile_body(Body, _, DBody, Ctx).

   //goal
        Compile_predicateCalls(goal(Body,Ctx), Optimize, TBody) :-
        Compile_body(Body, TBody0, _, Ctx),
        (	Optimize == on ->
        Simplify_goal(TBody0, TBody)
        ;	TBody = TBody0
        ).

   //debug version of goal
        Compile_predicateCalls(dgoal(Body,Ctx), _, DBody) :-
        Compile_body(Body, _, DBody, Ctx).

   //predicate fact
        Compile_predicateCalls(fact(TFact), _, TFact).

   //debug version of a predicate fact
        Compile_predicateCalls(dfact(TFact,DHead), _, (TFact:-DHead)).

   //supported Prolog meta-directives (specified in the adapter files)
        Compile_predicateCalls(directive(Directive,Meta), _, TDirective) :-
        Directive =.. [Functor| Args],
        Meta =.. [Functor| MArgs],
        pp_entity_(_, Entity, Prefix, _, _),
   //MetaVars = [] as we're compiling a local call
        CompCtx(Ctx, _, _, Entity, Entity, Entity, Entity, Prefix, [], _, _, _, [], _),
        (	Compile_prolog_metaArguments(Args, MArgs, Ctx, TArgs, DArgs) ->
        (	CompilerFlag(debug, on) ->
        TDirective =.. [Functor| DArgs]
        ;	TDirective =.. [Functor| TArgs]
        )
        ;	% the meta-directive template is not usable, report it as an error
        throw(domain_error(metaDirective_template, Meta))
        ).



        AddCoinductive_predicateAuxClause(Head, TestHead, HeadExCtx, TCHead, BodyExCtx, THead, DHead) :-
        executionContext(HeadExCtx, Entity, Sender, This, Self, MetaCallCtx, HeadStack),
        executionContext(BodyExCtx, Entity, Sender, This, Self, MetaCallCtx, BodyStack),
        CoinductiveSuccess_hook(Head, Hypothesis, HeadExCtx, HeadStack, BodyStack, Hook),
        (	CompilerFlag(debug, on) ->
        Header = Debug(rule(Entity, DHead, 0, nil, 0), BodyExCtx),
        If = Debug(goal(checkCoinductiveSuccess(TestHead, HeadStack), CheckCoinductiveSuccess(TestHead, HeadStack, Hypothesis)), BodyExCtx),
        Then = Debug(goal(coinductiveSuccess_hook(Head, Hypothesis), Hook), BodyExCtx),
        Else = (
        Debug(goal(pushCoinductive_hypothesis(TestHead, HeadStack, BodyStack), BodyStack = [Head| HeadStack]), BodyExCtx),
        Debug(goal(Head, THead), BodyExCtx)
        )
        ;	Header = true,
        If = CheckCoinductiveSuccess(TestHead, HeadStack, Hypothesis),
        Then = Hook,
        Else = (BodyStack = [Head| HeadStack], THead)
        ),
        (	prolog_meta_predicate('*->(_, _), _, _) ->
   //backend Prolog compiler supports the soft-cut control construct
        assertz(pp_entityAuxClause_({(TCHead :- Header, ('*->(If, Then); Else))}))
        ;	prolog_meta_predicate(if(_, _, _), _, _) ->
   //backend Prolog compiler supports the if/3 soft-cut built-in meta-predicate
        assertz(pp_entityAuxClause_({(TCHead :- Header, if(If, Then, Else))}))
        ;	% the adapter file for the backend Prolog compiler declares that coinduction
   //is supported but it seems to be missing the necessary declaration for the
   //soft-cut control construct or meta-predicate
        throw(resource_error(softCutSupport))
        ).


        CoinductiveSuccess_hook(Head, Hypothesis, ExCtx, HeadStack, BodyStack, Hook) :-
   //ensure zero performance penalties when defining coinductive predicates without a definition
   //for the coinductive success hook predicates
        (	ppDefines_predicate_(coinductiveSuccess_hook(Head,Hypothesis), _, ExCtx, THead, _, _),
        \+ \+ (
        ppFinal_entity_term_(THead, _)
        ;	ppFinal_entity_term_((THead :- _), _)
        ) ->
   //... with at least one clause for this particular coinductive predicate head
        Hook = ((HeadStack = BodyStack), THead)
        ;	% we only consider coinductiveSuccess_hook/1 clauses if no coinductiveSuccess_hook/2 clause applies
        ppDefines_predicate_(coinductiveSuccess_hook(Head), _, ExCtx, THead, _, _),
        \+ \+ (
        ppFinal_entity_term_(THead, _)
        ;	ppFinal_entity_term_((THead :- _), _)
        ) ->
   //... with at least one clause for this particular coinductive predicate head
        Hook = ((HeadStack = BodyStack), THead)
        ;	% no hook predicates defined or defined but with no clauses for this particular coinductive predicate head
        Hook = (HeadStack = BodyStack)
        ).



   //reports missing predicate directives

        Report_missingDirectives(Type, Entity) :-
        (	CompilerFlag(missingDirectives, warning) ->
        Report_missingDirectives_(Type, Entity)
        ;	true
        ).


   //reports missing scope directives for multifile predicates

        Report_missingDirectives_(Type, Entity) :-
        pp_multifile_(Head, File, Lines),
   //declared multifile predicate
        functor(Head, Functor, Arity),
        \+ pp_public_(Functor, Arity),
        \+ pp_protected_(Functor, Arity),
        \+ pp_private_(Functor, Arity),
   //but missing corresponding scope directive
        IncrementCompiling_warningsCounter',
        print_message(warning(missingDirectives), core, missingScopeDirective(File, Lines, Type, Entity, Functor/Arity)),
        fail.

   //reports missing meta_predicate/1 directives for meta-predicates

        Report_missingDirectives_(Type, Entity) :-
        pp_missing_meta_predicateDirective_(Head, File, Lines),
        functor(Head, Functor, Arity),
        IncrementCompiling_warningsCounter',
        print_message(warning(missingDirectives), core, missing_predicateDirective(File, Lines, Type, Entity, (meta_predicate), Functor/Arity)),
        fail.

   //reports missing dynamic/1 directives

        Report_missingDirectives_(Type, Entity) :-
        pp_missingDynamicDirective_(Head, File, Lines),
        functor(Head, Functor, Arity),
        IncrementCompiling_warningsCounter',
        print_message(warning(missingDirectives), core, missing_predicateDirective(File, Lines, Type, Entity, (dynamic), Functor/Arity)),
        fail.

   //reports missing discontiguous/1 directives

        Report_missingDirectives_(Type, Entity) :-
        pp_missingDiscontiguousDirective_(Head, File, Lines),
        functor(Head, Functor, Arity),
        IncrementCompiling_warningsCounter',
        print_message(warning(missingDirectives), core, missing_predicateDirective(File, Lines, Type, Entity, (discontiguous), Functor/Arity)),
        fail.

   //reports missing scope directives for mode directives

        Report_missingDirectives_(Type, Entity) :-
        pp_mode_(Mode, _, File, Lines),
   //documented predicate or non-terminal
        functor(Mode, Functor, Arity),
        \+ pp_non_terminal_(Functor, Arity, _),
        \+ pp_public_(Functor, Arity),
        \+ pp_protected_(Functor, Arity),
        \+ pp_private_(Functor, Arity),
   //but missing scope directive
        IncrementCompiling_warningsCounter',
        print_message(warning(missingDirectives), core, missingScopeDirective(File, Lines, Type, Entity, Functor/Arity)),
        fail.

        Report_missingDirectives_(_, _).



   //Report_unknown_predicateCall(@compilation_mode, @callable)
        %
   //reports unknown predicates and non-terminals

        Report_unknown_predicateCall(runtime, _).

        Report_unknown_predicateCall(compile(_), Pred) :-
        CompilerFlag(unknown_predicates, Value),
        Report_unknown_predicateCallAux(Value, Pred).


        Report_unknown_predicateCallAux(silent, _).

        Report_unknown_predicateCallAux(error, Functor/Arity) :-
        Arity2 is Arity - 2,
        (	ppCalls_non_terminal_(Functor, Arity2, _) ->
        throw(existence_error(non_terminal, Functor//Arity2))
        ;	throw(existence_error(predicate, Functor/Arity))
        ).

        Report_unknown_predicateCallAux(warning, Functor/Arity) :-
        SourceFileContext(File, Lines, Type, Entity),
        Arity2 is Arity - 2,
        IncrementCompiling_warningsCounter',
        (	ppCalls_non_terminal_(Functor, Arity2, _) ->
        print_message(warning(unknown_predicates), core, unknown_non_terminalCalled_but_notDefined(File, Lines, Type, Entity, Functor//Arity2))
        ;	print_message(warning(unknown_predicates), core, unknown_predicateCalled_but_notDefined(File, Lines, Type, Entity, Functor/Arity))
        ).



   //Report_undefined_predicateCall(@compilation_mode, @callable)
        %
   //reports calls to declared, static, but undefined predicates and non-terminals

        Report_undefined_predicateCall(runtime, _).

        Report_undefined_predicateCall(compile(_), Pred) :-
        CompilerFlag(undefined_predicates, Value),
        Report_undefined_predicateCallAux(Value, Pred).


        Report_undefined_predicateCallAux(silent, _).

        Report_undefined_predicateCallAux(error, Functor/Arity) :-
        Arity2 is Arity - 2,
        (	ppCalls_non_terminal_(Functor, Arity2, _) ->
        throw(existence_error(procedure, Functor//Arity2))
        ;	throw(existence_error(procedure, Functor/Arity))
        ).

        Report_undefined_predicateCallAux(warning, Functor/Arity) :-
        SourceFileContext(File, Lines, Type, Entity),
        Arity2 is Arity - 2,
        IncrementCompiling_warningsCounter',
        (	ppCalls_non_terminal_(Functor, Arity2, _) ->
        print_message(warning(undefined_predicates), core, declaredStatic_non_terminalCalled_but_notDefined(File, Lines, Type, Entity, Functor//Arity2))
        ;	print_message(warning(undefined_predicates), core, declaredStatic_predicateCalled_but_notDefined(File, Lines, Type, Entity, Functor/Arity))
        ).



   //Report_non_portableCalls(@entity_type, @entityIdentifier)
        %
   //reports non-portable predicate and function calls in the body of object and category predicates

        Report_non_portableCalls(Type, Entity) :-
        (	CompilerFlag(portability, warning) ->
        Report_non_portableCalls_(Type, Entity)
        ;	true
        ).


        Report_non_portableCalls_(Type, Entity) :-
        pp_non_portable_predicate_(Head, File, Lines),
        functor(Head, Functor, Arity),
        IncrementCompiling_warningsCounter',
        print_message(warning(portability), core, nonStandard_predicateCall(File, Lines, Type, Entity, Functor/Arity)),
        fail.

        Report_non_portableCalls_(Type, Entity) :-
        pp_non_portableFunction_(Function, File, Lines),
        functor(Function, Functor, Arity),
        IncrementCompiling_warningsCounter',
        print_message(warning(portability), core, nonStandardArithmeticFunctionCall(File, Lines, Type, Entity, Functor/Arity)),
        fail.

        Report_non_portableCalls_(_, _).



   //write_encodingDirective(@stream, +atom)
        %
   //writes the encoding/1 directive (if supported in generated code);
   //it must be the first term in the file

        write_encodingDirective(Stream, Path) :-
        (	prologFeature(encodingDirective, full),
        ppFile_encoding_(_, Encoding, _) ->
        writeCompiled_term(Stream, (:- encoding(Encoding)), runtime, Path, 1)
        ;	true
        ).



   //write_entityDirectives(@stream, +atom)
        %
   //writes the compiled entity directives

        write_entityDirectives(Stream, Path) :-
        ppDirective_(Directive),
        writeCompiled_term(Stream, (:- Directive), runtime, Path, 1),
        fail.

        write_entityDirectives(_, _).



   //write_prolog_terms(@stream, atom)
        %
   //writes any Prolog clauses that appear before an entity opening directive

        write_prolog_terms(Stream, Path) :-
        pp_prolog_term_(Term, _, Line-_),
        writeCompiled_term(Stream, Term, user, Path, Line),
        fail.

        write_prolog_terms(_, _).



   //write_entityClauses(@stream, +atom, +atom)
        %
   //writes Logtalk entity clauses

        write_entityClauses(Stream, Path, _) :-
        ppDcl_(Clause),
        writeCompiled_term(Stream, Clause, runtime, Path, 1),
        fail.

        write_entityClauses(Stream, Path, _) :-
        ppDef_(Clause),
        writeCompiled_term(Stream, Clause, runtime, Path, 1),
        fail.

        write_entityClauses(Stream, Path, _) :-
        ppDdef_(Clause),
        writeCompiled_term(Stream, Clause, runtime, Path, 1),
        fail.

        write_entityClauses(Stream, Path, _) :-
        ppSuper_(Clause),
        writeCompiled_term(Stream, Clause, runtime, Path, 1),
        fail.

        write_entityClauses(Stream, Path, Rnm) :-
        pp_predicateAlias_(Entity, Pred, Alias, _, _, _),
        Clause =.. [Rnm, Entity, Pred, Alias],
        writeCompiled_term(Stream, Clause, runtime, Path, 1),
        fail.

        write_entityClauses(Stream, Path, Rnm) :-
        Catchall =.. [Rnm, _, Pred, Pred],
        writeCompiled_term(Stream, Catchall, runtime, Path, 1),
        fail.

        write_entityClauses(Stream, Path, _) :-
        ppFinal_entity_term_(Clause, Line-_),
        writeCompiled_term(Stream, Clause, user, Path, Line),
        fail.

        write_entityClauses(Stream, Path, _) :-
        ppFinal_entityAuxClause_(Clause),
        writeCompiled_term(Stream, Clause, aux, Path, 1),
        fail.

        write_entityClauses(_, _, _).



   //writeRuntimeClauses(@stream, +atom)
        %
   //writes the entity runtime multifile and dynamic directives and the entity
   //runtime clauses for all defined entities

        writeRuntimeClauses(Stream, Path) :-
        writeRuntimeClauses(Stream, Path, Current_protocol_'/5),
        writeRuntimeClauses(Stream, Path, CurrentCategory_'/6),
        writeRuntimeClauses(Stream, Path, Current_object_'/11),
        writeRuntimeClauses(Stream, Path, entity_property_'/2),
        writeRuntimeClauses(Stream, Path, predicate_property_'/3),
        writeRuntimeClauses(Stream, Path, Implements_protocol_'/3),
        writeRuntimeClauses(Stream, Path, ImportsCategory_'/3),
        writeRuntimeClauses(Stream, Path, InstantiatesClass_'/3),
        writeRuntimeClauses(Stream, Path, SpecializesClass_'/3),
        writeRuntimeClauses(Stream, Path, extendsCategory_'/3),
        writeRuntimeClauses(Stream, Path, extends_object_'/3),
        writeRuntimeClauses(Stream, Path, extends_protocol_'/3),
        writeRuntimeClauses(Stream, Path, LoadedFile_'/7),
        writeRuntimeClauses(Stream, Path, IncludedFile_'/4).


        writeRuntimeClauses(Stream, Path, Functor/Arity) :-
        functor(Clause, Functor, Arity),
        (	\+ ppRuntimeClause_(Clause) ->
        true
        ;	writeCompiled_term(Stream, (:- multifile(Functor/Arity)), runtime, Path, 1),
        writeCompiled_term(Stream, (:- dynamic(Functor/Arity)), runtime, Path, 1),
        (	ppRuntimeClause_(Clause),
        writeCompiled_term(Stream, Clause, runtime, Path, 1),
        fail
        ;	true
        )
        ).



   //writeInitializationDirective(@stream, +atom)
        %
   //writes the initialization directive for the compiled source file,
   //a conjunction of the initialization goals of the defined entities

        writeInitializationDirective(Stream, Path) :-
        Initialization_goal(Goal),
        (	Goal == true ->
        true
        ;	writeCompiled_term(Stream, (:- initialization(Goal)), runtime, Path, 1)
        ).



   //Initialization_goal(-callable)
        %
   //source file initialization goal constructed from object initialization
   //directives and from source file initialization/1 directives if present

        Initialization_goal(InitializationGoal) :-
        findall(
        Line-Goal,
        (	ppFile_objectInitialization_(_, Goal, Line-_)
        ;	ppFileInitialization_(Goal, Line-_)
        ),
        LineGoals
        ),
   //ensure source file textual order for the initialization goals
   //(this assumes that the backend Prolog system provides access to
   //read term position...)
        keysort(LineGoals, SortedLineGoals),
        findall(
        Goal,
        member(_-Goal, SortedLineGoals),
        Goals
        ),
        List_toConjunction(Goals, InitializationGoal).



   //converts a list of goals into a conjunction of goals

        List_toConjunction([], true).

        List_toConjunction([Goal| Goals], Conjunction) :-
        List_toConjunction(Goals, Goal, Conjunction).


        List_toConjunction([], Conjunction, Conjunction).

        List_toConjunction([Goal| Goals], Conjunction0, Conjunction) :-
        List_toConjunction(Goals, (Conjunction0, Goal), Conjunction).



   //converts a conjunction into a list of terms

        Conjunction_toList(Term, [Term]) :-
        var(Term),
        !.

        Conjunction_toList((Term, Conjunction), [Term| Terms]) :-
        !,
        Conjunction_toList(Conjunction, Terms).

        Conjunction_toList(Term, [Term]).



   //generates and asserts the initialization goal for the object being compiled

        generateFile_objectInitialization_goal' :-
        pp_entity_(_, Object, Prefix, _, _),
        (	prologFeature(threads, supported),
        setof(Mutex, Head^ppSynchronized_(Head, Mutex), Mutexes) ->
        Goal1 = Create_mutexes(Mutexes)
        ;	Goal1 = true
        ),
        (	pp_threaded_' ->
        Goal2 = Init_object_message_queue(Prefix)
        ;	Goal2 = true
        ),
   //an object may contain multiple initialization/1 directives
        (	bagof(ObjectInitGoal, Lines^ppFinal_objectInitialization_(ObjectInitGoal, Lines), ObjectInitGoals) ->
        List_toConjunction(ObjectInitGoals, Goal3),
        RemoveRedundantCalls((Goal1, Goal2, Goal3), Goal)
        ;	RemoveRedundantCalls((Goal1, Goal2), Goal)
        ),
        (	Goal == true ->
        true
        ;	ppReferenced_object_(Object, File, Lines),
        assertz(ppFile_objectInitialization_(Object, Goal, Lines))
        ).



   //AssertDynamic_entity(+atom)
        %
   //adds a dynamically created entity to memory

        AssertDynamic_entity(Kind) :-
        pp_entity_(_, _, _, _, Rnm),
        AssertDynamic_entity(Kind, Rnm),
        CallInitialization_goal'.


        AssertDynamic_entity(_, _) :-
        ppDirective_(dynamic(Functor/Arity)),
        functor(Pred, Functor, Arity),
        asserta(Pred),
        retract(Pred),
        fail.

        AssertDynamic_entity(_, _) :-
        ppDcl_(Clause),
        Assertz_entityClause(Clause, aux),
        fail.

        AssertDynamic_entity(_, _) :-
        ppDef_(Clause),
        Assertz_entityClause(Clause, aux),
        fail.

        AssertDynamic_entity(_, _) :-
        ppDdef_(Clause),
        Assertz_entityClause(Clause, aux),
        fail.

        AssertDynamic_entity(_, _) :-
        ppSuper_(Clause),
        Assertz_entityClause(Clause, aux),
        fail.

        AssertDynamic_entity(_, Rnm) :-
        pp_predicateAlias_(Entity, Pred, Alias, _, _, _),
        Clause =.. [Rnm, Entity, Pred, Alias],
        Assertz_entityClause(Clause, aux),
        fail.

        AssertDynamic_entity(_, Rnm) :-
        Catchall =.. [Rnm, _, Pred, Pred],
        Assertz_entityClause(Catchall, aux),
        fail.

        AssertDynamic_entity(_, _) :-
        ppFinal_entity_term_(Clause, _),
        Assertz_entityClause(Clause, user),
        fail.

        AssertDynamic_entity(_, _) :-
        ppFinal_entityAuxClause_(Clause),
        Assertz_entityClause(Clause, aux),
        fail.

        AssertDynamic_entity(Type, _) :-
        Save_entityRuntimeClause(Type),
        fail.

        AssertDynamic_entity(_, _) :-
        ppRuntimeClause_(Clause),
        Assertz_entityClause(Clause, aux),
        fail.

        AssertDynamic_entity(_, _).



   //CallInitialization_goal'
        %
   //calls any defined initialization goals for a dynamically created entity

        CallInitialization_goal' :-
        (	prologFeature(threads, supported),
        setof(Mutex, Head^ppSynchronized_(Head, Mutex), Mutexes) ->
        Create_mutexes(Mutexes)
        ;	true
        ),
        (	pp_object_(_, Prefix, _, _, _, _, _, _, _, _, _),
        pp_threaded_' ->
        Init_object_message_queue(Prefix)
        ;	true
        ),
   //an object may contain multiple initialization/1 directives
        (	bagof(Goal, ppFinal_objectInitialization_(Goal, _), GoalList) ->
        List_toConjunction(GoalList, Goals),
        once(Goals)
        ;	true
        ),
   //complementing categories add a file initialization goal
        (	ppFileInitialization_(InitializationGoal, _) ->
        once(InitializationGoal)
        ;	true
        ).



   //Construct_prototypeFunctors(+objectIdentifier, -atom, -atom, -atom, -atom, -atom, -atom, -atom, -atom, -atom)
        %
   //constructs functors used in the compiled code of an object playing the role of a prototype

        Construct_prototypeFunctors(Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm) :-
        (	Current_object_(Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, Flags),
        Flags /\ 1 =:= 1 ->
   //loaded, built-in object
        true
        ;	Construct_entity_prefix(Obj, Prefix),
        atomConcat(Prefix, 'Dcl', Dcl),
        atomConcat(Prefix, 'Def', Def),
        atomConcat(Prefix, 'Super', Super),
        IDcl = Dcl,
        IDef = Def,
        atomConcat(Prefix, 'Ddcl', DDcl),
        atomConcat(Prefix, 'Ddef', DDef),
        atomConcat(Prefix, 'Alias', Rnm)
        ).



   //ConstructIcFunctors(+objectIdentifier, -atom, -atom, -atom, -atom, -atom, -atom, -atom, -atom, -atom)
        %
   //constructs functors used in the compiled code of an object playing the role of a class or an instance

        ConstructIcFunctors(Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm) :-
        (	Current_object_(Obj, Prefix, Dcl, Def, Super, IDcl, IDef, DDcl, DDef, Rnm, Flags),
        Flags /\ 1 =:= 1 ->
   //loaded, built-in object
        true
        ;	Construct_entity_prefix(Obj, Prefix),
        atomConcat(Prefix, 'Dcl', Dcl),
        atomConcat(Prefix, 'Def', Def),
        atomConcat(Prefix, 'Super', Super),
        atomConcat(Prefix, 'Idcl', IDcl),
        atomConcat(Prefix, 'Idef', IDef),
        atomConcat(Prefix, 'Ddcl', DDcl),
        atomConcat(Prefix, 'Ddef', DDef),
        atomConcat(Prefix, 'Alias', Rnm)
        ).



   //Construct_protocolFunctors(+protocolIdentifier, -atom, -atom, -atom)
        %
   //constructs functors used in the compiled code of a protocol

        Construct_protocolFunctors(Ptc, Prefix, Dcl, Rnm) :-
        (	Current_protocol_(Ptc, Prefix, Dcl, Rnm, Flags),
        Flags /\ 1 =:= 1 ->
   //loaded, built-in protocol
        true
        ;	Construct_entity_prefix(Ptc, Prefix),
        atomConcat(Prefix, 'Dcl', Dcl),
        atomConcat(Prefix, 'Alias', Rnm)
        ).



   //ConstructCategoryFunctors(+categoryIdentifier, -atom, -atom, -atom, -atom)
        %
   //constructs functors used in the compiled code of a category

        ConstructCategoryFunctors(Ctg, Prefix, Dcl, Def, Rnm) :-
        (	CurrentCategory_(Ctg, Prefix, Dcl, Def, Rnm, Flags),
        Flags /\ 1 =:= 1 ->
   //loaded, built-in category
        true
        ;	Construct_entity_prefix(Ctg, Prefix),
        atomConcat(Prefix, 'Dcl', Dcl),
        atomConcat(Prefix, 'Def', Def),
        atomConcat(Prefix, 'Alias', Rnm)
        ).



   //entity_to_prefix(@entityIdentifier, -atom)
        %
   //converts an entity identifier into an entity prefix (used in the compiled code)
   //note that objects, categories, and protocols share the same namespace

        entity_to_prefix(Entity, Prefix) :-
        (	Current_object_(Entity, Prefix, _, _, _, _, _, _, _, _, _) ->
        true
        ;	Current_protocol_(Entity, Prefix, _, _, _) ->
        true
        ;	CurrentCategory_(Entity, Prefix, _, _, _, _) ->
        true
        ;	Construct_entity_prefix(Entity, Prefix)
        ).



   //prefix_to_entity(+atom, -entityIdentifier)
        %
   //reverses the entity prefix used in the compiled code
   //note that objects, categories, and protocols share the same namespace

        prefix_to_entity(Prefix, Entity) :-
        (	Current_object_(Entity, Prefix, _, _, _, _, _, _, _, _, _) ->
        true
        ;	Current_protocol_(Entity, Prefix, _, _, _) ->
        true
        ;	CurrentCategory_(Entity, Prefix, _, _, _, _) ->
        true
        ;	Deconstruct_entity_prefix(Prefix, Entity)
        ).



   //Construct_entity_prefix(@entityIdentifier, -atom)
        %
   //constructs the entity prefix used in the compiled code from the entity identifier
        %
   //prefix = code prefix + entity functor + "#" + entity arity + "."

        Construct_entity_prefix(Entity, Prefix) :-
        CompilerFlag(code_prefix, CodePrefix),
   //the functor code prefix can be used to hide internal predicates (by
   //defining it as '$' when using most backend Prolog compilers) and to
   //avoid conflicts with other predicates
        functor(Entity, Functor, Arity),
        atomConcat(CodePrefix, Functor, Prefix0),
        (	Arity_#atom.(Arity, ArityAtom) ->
        true
        ;	numberCodes(Arity, ArityCodes),
        atomCodes(ArityAtom0, ArityCodes),
        atomConcat('#', ArityAtom0, ArityAtom1),
        atomConcat(ArityAtom1, '.', ArityAtom)
        ),
        atomConcat(Prefix0, ArityAtom, Prefix).


   //avoid costly atom computations for the most common cases
        Arity_#atom.(0, '#0.').
        Arity_#atom.(1, '#1.').
        Arity_#atom.(2, '#2.').
        Arity_#atom.(3, '#3.').
        Arity_#atom.(4, '#4.').
        Arity_#atom.(5, '#5.').
        Arity_#atom.(6, '#6.').
        Arity_#atom.(7, '#7.').
        Arity_#atom.(8, '#8.').
        Arity_#atom.(9, '#9.').



   //Deconstruct_entity_prefix(+atom, -entityIdentifier)
        %
   //deconstructs the entity prefix used in the compiled code
   //returning the corresponding entity identifier

        Deconstruct_entity_prefix(Prefix, Entity) :-
   //valid values of the code_prefix flag are a single character atoms
        subAtom(Prefix, 1, _, 0, Entity0),
        atomConcat(Entity1, '.', Entity0),
   //locate the rightmost #
        subAtom(Entity1, Before, 1, After, '#'),
        Lines is Before + 1,
        subAtom(Entity1, Lines, _, 0, Rest),
        \+ subAtom(Rest, _, 1, _, '#'), !,
        subAtom(Entity1, 0, Before, _, Functor),
        subAtom(Entity1, _, After, 0, ArityAtom),
        atomCodes(ArityAtom, ArityCodes),
        numberCodes(Arity, ArityCodes),
        functor(Entity, Functor, Arity).



   //CompileAuxClauses(@list(clause))
        %
   //compiles a list of auxiliary predicate clauses;
   //used mainly in conjunction with goal_expansion/2 hooks

        CompileAuxClauses([Clause| Clauses]) :-
   //avoid making a predicate discontiguous by accident by using a
   //compilation mode that ensures that the auxiliary clauses will
   //be written after the user clauses
        CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, compile(aux), _, '-(0,0)),
        CompileClause(Clause, Ctx),
        CompileAuxClauses(Clauses).

        CompileAuxClauses([]).



   //entity_prefix(?entityIdentifier, ?atom)
        %
   //converts between entity identifiers and internal entity prefixes;
   //used mainly in hook objects for processing proprietary directives

        entity_prefix(Entity, Prefix) :-
        (	var(Entity), var(Prefix) ->
        pp_entity_(_, Entity, Prefix, _, _)
        ;	callable(Entity) ->
        entity_to_prefix(Entity, Prefix)
        ;	atom(Prefix),
        prefix_to_entity(Prefix, Entity)
        ).



   //Compile_predicate_heads(@list(callable), ?entityIdentifier, -list(callable), @compilationContext)
   //Compile_predicate_heads(@callable, ?entityIdentifier, -callable, @term)
        %
   //compiles a single predicate head, a conjunction of predicate heads, or a list of
   //predicate heads; used mainly in hook objects for processing proprietary directives
        %
   //the predicate heads are compiled in the context of the specified entity or in the context
   //of the entity being compiled when the entity argument is not instantiated

        Compile_predicate_heads(Heads, Entity, THeads, Ctx) :-
        Check(var_or_entityIdentifier, Entity),
        entity_prefix(Entity, Prefix),
        Compile_predicate_headsAux(Heads, Prefix, THeads, Ctx).


        Compile_predicate_headsAux(Heads, _, _, _) :-
        var(Heads),
        throw(instantiation_error).

        Compile_predicate_headsAux([], _, [], _) :-
        !.

        Compile_predicate_headsAux([Head| Heads], Prefix, [THead| THeads], Ctx) :-
        !,
        Compile_predicate_headsAux(Head, Prefix, THead, Ctx),
        Compile_predicate_headsAux(Heads, Prefix, THeads, Ctx).

        Compile_predicate_headsAux((Head, Heads), Prefix, (THead, THeads), Ctx) :-
        !,
        Compile_predicate_headsAux(Head, Prefix, THead, Ctx),
        Compile_predicate_headsAux(Heads, Prefix, THeads, Ctx).

        Compile_predicate_headsAux(Head, Prefix, THead, Ctx) :-
        Check(callable, Head),
        functor(Head, Functor, Arity),
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity),
        functor(THead, TFunctor, TArity),
        unify_head_theadArguments(Head, THead, Ctx).



   //Decompile_predicate_heads(+list(callable), ?entityIdentifier, ?atom, -list(callable))
   //Decompile_predicate_heads(+callable, ?entityIdentifier, ?atom, -callable)
        %
   //decompiles the predicate heads used for compiled predicates;
        %
   //all the compiled predicate heads must refer to the same entity
   //(which must be loaded) in order for this predicate to succeed

        Decompile_predicate_heads(THeads, Entity, Type, Heads) :-
        Check(var_or_entityIdentifier, Entity),
        Decompile_predicate_heads(THeads, Entity, Type, _, Heads).


        Decompile_predicate_heads(THeads, _, _, _, _) :-
        var(THeads),
        throw(instantiation_error).

        Decompile_predicate_heads([], _, _, _, []) :-
        !.

        Decompile_predicate_heads([THead| THeads], Entity, Type, Prefix, [Head| Heads]) :-
        !,
        Decompile_predicate_heads(THead, Entity, Type, Prefix, Head),
        Decompile_predicate_heads(THeads, Entity, Type, Prefix, Heads).

        Decompile_predicate_heads(':(Module,THead), Entity, Type, Prefix, Head) :-
        atom(Module),
        user_module_qualification(xx, ':(Module,xx)),
        !,
        Decompile_predicate_heads(THead, Entity, Type, Prefix, Head).

        Decompile_predicate_heads(THead, Entity, Type, Prefix, Head) :-
        callable(THead),
        functor(THead, TFunctor, TArity),
        (	var(Prefix) ->
        (	Current_object_(Entity, Prefix, _, _, _, _, _, _, _, _, _),
        Type = object
        ;	CurrentCategory_(Entity, Prefix, _, _, _, _),
        Type = category
        ;	Current_protocol_(Entity, Prefix, _, _, _),
        Type = protocol
        )
        ;	true
        ),
        Decompile_predicateIndicator(Prefix, TFunctor/TArity, Functor/Arity),
        functor(Head, Functor, Arity),
        unify_head_theadArguments(Head, THead, _),
        !.



   //Compile_predicateIndicators(+list(predicateIndicator), ?entityIdentifier, -list(predicateIndicator))
   //Compile_predicateIndicators(+predicateIndicator, ?entityIdentifier, -predicateIndicator)
        %
   //compiles a single predicate indicator, a conjunction of predicate indicators, or a list
   //of predicate indicators; used mainly in hook objects for processing proprietary directives
        %
   //the predicate indicators are compiled in the context of the specified entity or in the context
   //of the entity being compiled when the entity argument is not instantiated

        Compile_predicateIndicators(PIs, Entity, TPIs) :-
        Check(var_or_entityIdentifier, Entity),
        entity_prefix(Entity, Prefix),
        Compile_predicateIndicatorsAux(PIs, Prefix, TPIs).


        Compile_predicateIndicatorsAux(PIs, _, _) :-
        var(PIs),
        throw(instantiation_error).

        Compile_predicateIndicatorsAux([], _, []) :-
        !.

        Compile_predicateIndicatorsAux([PI| PIs], Prefix, [TPI| TPIs]) :-
        !,
        Compile_predicateIndicatorsAux(PI, Prefix, TPI),
        Compile_predicateIndicatorsAux(PIs, Prefix, TPIs).

        Compile_predicateIndicatorsAux((PI, PIs), Prefix, (TPI, TPIs)) :-
        !,
        Compile_predicateIndicatorsAux(PI, Prefix, TPI),
        Compile_predicateIndicatorsAux(PIs, Prefix, TPIs).

        Compile_predicateIndicatorsAux(PI, Prefix, TFunctor/TArity) :-
        (	valid_predicateIndicator(PI, Functor, Arity) ->
        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity)
        ;	valid_non_terminalIndicator(PI, Functor, _, ExtArity) ->
        Compile_predicateIndicator(Prefix, Functor/ExtArity, TFunctor/TArity)
        ;	throw(type_error(predicateIndicator, PI))
        ).



   //Compile_predicateIndicator(+atom, +predicateIndicator, -predicateIndicator)
        %
   //compiles the user predicate indicator using the encoding entity prefix + functor + # + arity

        Compile_predicateIndicator(Prefix, Functor/Arity, TFunctor/TArity) :-
        atomConcat(Prefix, Functor, TFunctor0),
        (	Arity_#atom(Arity, ArityAtom) ->
        true
        ;	numberCodes(Arity, ArityCodes),
        atomCodes(ArityAtom0, ArityCodes),
        atomConcat('#', ArityAtom0, ArityAtom)
        ),
        atomConcat(TFunctor0, ArityAtom, TFunctor),
   //add execution context argument
        TArity is Arity + 1.


   //avoid costly atom computations for the most common cases
        Arity_#atom(0, '#0').
        Arity_#atom(1, '#1').
        Arity_#atom(2, '#2').
        Arity_#atom(3, '#3').
        Arity_#atom(4, '#4').
        Arity_#atom(5, '#5').
        Arity_#atom(6, '#6').
        Arity_#atom(7, '#7').
        Arity_#atom(8, '#8').
        Arity_#atom(9, '#9').



   //Decompile_predicateIndicator(+atom, +predicateIndicator, -predicateIndicator)
        %
   //decompiles an internal predicate indicator used for a user predicate

        Decompile_predicateIndicator(Prefix, TFunctor/TArity, Functor/Arity) :-
        atomConcat(Prefix, Predicate, TFunctor),
   //locate the rightmost #
        subAtom(Predicate, Before, 1, _, '#'),
        Lines is Before + 1,
        subAtom(Predicate, Lines, _, 0, Rest),
        \+ subAtom(Rest, _, 1, _, '#'),
        subAtom(Predicate, 0, Before, _, Functor),
   //subtract execution context argument
        Arity is TArity - 1,
        Arity >= 0,
        !.



   //Decompile_predicateIndicators(+list(predicateIndicator), ?entityIdentifier, ?atom, -list(predicateIndicator))
   //Decompile_predicateIndicators(+predicateIndicator, ?entityIdentifier, ?atom, -predicateIndicator)
        %
   //reverses the predicate indicator used for a compiled predicate or a list of compiled predicates;
        %
   //all the compiled predicate indicators must refer to the same entity
   //(which must be loaded) in order for this predicate to succeed

        Decompile_predicateIndicators(TPIs, Entity, Type, PIs) :-
        Check(var_or_entityIdentifier, Entity),
        Decompile_predicateIndicators(TPIs, Entity, Type, _, PIs).


        Decompile_predicateIndicators(TPIs, _, _, _, _) :-
        var(TPIs),
        throw(instantiation_error).

        Decompile_predicateIndicators([], _, _, _, []) :-
        !.

        Decompile_predicateIndicators([TPI| TPIs], Entity, Type, Prefix, [PI| PIs]) :-
        !,
        Decompile_predicateIndicators(TPI, Entity, Type, Prefix, PI),
        Decompile_predicateIndicators(TPIs, Entity, Type, Prefix, PIs).

        Decompile_predicateIndicators(':(Module,TFunctor/TArity), Entity, Type, Prefix, Functor/Arity) :-
        atom(Module),
        user_module_qualification(xx, ':(Module,xx)),
        !,
        Decompile_predicateIndicators(TFunctor/TArity, Entity, Type, Prefix, Functor/Arity).

        Decompile_predicateIndicators(TFunctor/TArity, Entity, Type, Prefix, Functor/Arity) :-
        (	var(Prefix) ->
        (	Current_object_(Entity, Prefix, _, _, _, _, _, _, _, _, _),
        Type = object
        ;	CurrentCategory_(Entity, Prefix, _, _, _, _),
        Type = category
        ;	Current_protocol_(Entity, Prefix, _, _, _),
        Type = protocol
        )
        ;	true
        ),
        Decompile_predicateIndicator(Prefix, TFunctor/TArity, Functor/Arity),
        !.



   //Compile_hooks(+callable)
        %
   //compiles the user-defined default compiler hooks
   //(replacing any existing defined hooks)

        Compile_hooks(HookEntity) :-
        CompCtx(Ctx, _, _, user, user, user, HookEntity, _, [], [], ExCtx, runtime, [], _),
        executionContext(ExCtx, user, user, user, HookEntity, [], []),
        CurrentFlag_(events, Events),
        Compile_message_to_object(term_expansion(Term, ExpandedTerm), HookEntity, TermExpansionGoal, Events, Ctx),
        Compile_message_to_object(goal_expansion(Term, ExpandedTerm), HookEntity, GoalExpansionGoal, Events, Ctx),
        retractall(hook_term_expansion_(_, _)),
        assertz((
        hook_term_expansion_(Term, ExpandedTerm) :-
        catch(TermExpansionGoal, Error, term_expansion_error(HookEntity, Term, Error))
        )),
        retractall(hook_goal_expansion_(_, _)),
        assertz((
        hook_goal_expansion_(Term, ExpandedTerm) :-
        catch(GoalExpansionGoal, Error, goal_expansion_error(HookEntity, Term, Error))
        )).



   //builtIn_predicate(@callable)
        %
   //checks if the argument is either a Logtalk or a Prolog built-in predicate

        builtIn_predicate(Pred) :-
        Logtalk_builtIn_predicate(Pred, _),
        !.

        builtIn_predicate(Pred) :-
        predicate_property(Pred, builtIn),
        !.

        builtIn_predicate(Pred) :-
        Iso_predicate(Pred),
   //hack for missing ISO standard predicate defined in the used adapter file
        !.



   //prolog_builtIn_predicate(@callable)
        %
   //either host Prolog native built-ins or missing ISO built-ins
   //that we have defined in the correspondent adapter file

        prolog_builtIn_predicate(Pred) :-
        predicate_property(Pred, builtIn),
   //Logtalk built-in predicates may also have the property "builtIn"
   //depending on the used backend Prolog compiler
        \+ Logtalk_builtIn_predicate(Pred, _),
        !.

        prolog_builtIn_predicate(Pred) :-
   //ISO Prolog built-in predicate (defined in the adapter files)
        Iso_predicate(Pred).



   //prolog_builtInDatabase_predicate(@callable)
        %
   //ISO Prolog standard and proprietary database predicates

        prolog_builtInDatabase_predicate(Term) :-
        IsoDatabase_predicate(Term),
   //ISO Prolog standard database predicate
        !.

        prolog_builtInDatabase_predicate(Term) :-
        prologDatabase_predicate(Term),
   //proprietary database predicate (declared in the adapter files)
        !.



   //Logtalk built-in methods
        %
   //builtIn_method(@callable, ?scope, ?callable, ?integer)

        builtIn_method(Method, Scope, Meta, Flags) :-
        (	builtIn_methodSpec(Method, Scope, Meta, Flags) ->
        true
        ;	% check if call/2-N
        functor(Method, call, Arity),
        Arity > 1,
        Scope = p,
        functor(Meta, call, Arity),
        Closure is Arity - 1,
        arg(1, Meta, Closure),
        builtIn_methodCall_nArgs(Arity, Meta),
        Flags = 1
        ).


        builtIn_methodCall_nArgs(1, _) :-
        !.

        builtIn_methodCall_nArgs(N, Meta) :-
        arg(N, Meta, *),
        N2 is N - 1,
        builtIn_methodCall_nArgs(N2, Meta).


   //control constructs
        builtIn_methodSpec(_::_, p, '::(*, *), 1).
        builtIn_methodSpec(::_, p, '::(*), 1).
        builtIn_methodSpec([_], p, [*], 1).
        builtIn_methodSpec(^^_, p, '^^(*), 1).
        builtIn_methodSpec(_<<_, p, '<<(*, 0), 1).
        builtIn_methodSpec(_>>_, p, '>>(*, 0), 1).
        builtIn_methodSpec(':(_,_), p, ':(*, 0), 1) :-
        prologFeature(modules, supported).
        builtIn_methodSpec({_}, p(p(p)), '{}(0), 1).
        builtIn_methodSpec((_,_), p(p(p)), ',(0, 0), 1).
        builtIn_methodSpec((_;_), p(p(p)), ';(0, 0), 1).
        builtIn_methodSpec((_->_), p(p(p)), '->(0, 0), 1).
        builtIn_methodSpec('*->(_,_), p(p(p)), '*->(0, 0), 1) :-
        prolog_builtIn_predicate('*->(_, _)).
   //reflection methods
        builtIn_methodSpec(current_op(_,_,_), p(p(p)), current_op(*, *, (::)), 1).
        builtIn_methodSpec(current_predicate(_), p(p(p)), current_predicate((::)), 1).
        builtIn_methodSpec(predicate_property(_,_), p(p(p)), predicate_property((::), *), 1).
   //database methods
        builtIn_methodSpec(abolish(_), p(p(p)), abolish((::)), 1).
        builtIn_methodSpec(assert(_), p(p(p)), assert((::)), 1).	% just for compatibility with old code!
        builtIn_methodSpec(asserta(_), p(p(p)), asserta((::)), 1).
        builtIn_methodSpec(assertz(_), p(p(p)), assertz((::)), 1).
        builtIn_methodSpec(clause(_,_), p(p(p)), clause((::), *), 1).
        builtIn_methodSpec(retract(_), p(p(p)), retract((::)), 1).
        builtIn_methodSpec(retractall(_), p(p(p)), retractall((::)), 1).
   //term expansion methods
        builtIn_methodSpec(expand_term(_,_), p(p(p)), no, 1).
        builtIn_methodSpec(expand_goal(_,_), p(p(p)), no, 1).
   //DCGs methods
        builtIn_methodSpec(phrase(_,_,_), p, phrase(2, *, *), 1).
        builtIn_methodSpec(phrase(_,_), p, phrase(2, *), 1).
   //meta-calls plus logic and control methods
        builtIn_methodSpec(\+ _, p, \+ 0, 1).
        builtIn_methodSpec(call(_), p, call(0), 1).
        builtIn_methodSpec(once(_), p, once(0), 1).
        builtIn_methodSpec(ignore(_), p, ignore(0), 1).
        builtIn_methodSpec(!, p(p(p)), no, 1).
        builtIn_methodSpec(true, p(p(p)), no, 1).
        builtIn_methodSpec(fail, p(p(p)), no, 1).
        builtIn_methodSpec(false, p(p(p)), no, 1).
        builtIn_methodSpec(repeat, p(p(p)), no, 1).
   //exception handling methods
        builtIn_methodSpec(catch(_,_,_), p, catch(0, *, 0), 1).
        builtIn_methodSpec(throw(_), p, no, 1).
   //error predicates
        builtIn_methodSpec(instantiation_error, p, no, 1).
        builtIn_methodSpec(type_error(_,_), p, no, 1).
        builtIn_methodSpec(domain_error(_,_), p, no, 1).
        builtIn_methodSpec(existence_error(_,_), p, no, 1).
        builtIn_methodSpec(permission_error(_,_,_), p, no, 1).
        builtIn_methodSpec(representation_error(_), p, no, 1).
        builtIn_methodSpec(evaluation_error(_), p, no, 1).
        builtIn_methodSpec(resource_error(_), p, no, 1).
        builtIn_methodSpec(syntax_error(_), p, no, 1).
        builtIn_methodSpec(system_error, p, no, 1).
   //execution context methods
        builtIn_methodSpec(context(_), p, no, 1).
        builtIn_methodSpec(parameter(_,_), p, no, 1).
        builtIn_methodSpec(self(_), p, no, 1).
        builtIn_methodSpec(sender(_), p, no, 1).
        builtIn_methodSpec(this(_), p, no, 1).
   //all solutions methods
        builtIn_methodSpec(bagof(_,_,_), p, bagof(*, ^, *), 1).
        builtIn_methodSpec(findall(_,_,_), p, findall(*, 0, *), 1).
        builtIn_methodSpec(findall(_,_,_,_), p, findall(*, 0, *, *), 1).
        builtIn_methodSpec(forall(_,_,_), p, forall(0, 0), 1).
        builtIn_methodSpec(setof(_,_,_), p, setof(*, ^, *), 1).



   //Logtalk built-in meta-predicates
        %
   //Logtalk_meta_predicate(+callable, ?callable, ?atom)

        Logtalk_meta_predicate(Pred, Meta, predicate) :-
        builtIn_method(Pred, _, Meta, _),
        Meta \== no.



   //Reserved_predicate_protocol(?callable, ?atom)
        %
   //table of reserved predicate names and the built-in protocols
   //where they are declared

        Reserved_predicate_protocol(before(_, _, _), monitoring).
        Reserved_predicate_protocol(after(_, _, _), monitoring).
        Reserved_predicate_protocol(term_expansion(_, _), expanding).
        Reserved_predicate_protocol(goal_expansion(_, _), expanding).
        Reserved_predicate_protocol(forward(_), forwarding).



        %LogtalkDirective(@callable)
        %
   //valid Logtalk directives; a common subset of Prolog module directives are
   //also included as modules can be compiled as objects (but the specific case
   //of the use_module/1 directive is handled at the Prolog adapter file level)

        LogtalkDirective(Directive) :-
        Logtalk_openingDirective(Directive),
        !.

        LogtalkDirective(Directive) :-
        LogtalkClosingDirective(Directive),
        !.

        LogtalkDirective(Directive) :-
        Logtalk_entityDirective(Directive),
        !.

        LogtalkDirective(Directive) :-
        Logtalk_predicateDirective(Directive),
        !.


   //objects
        Logtalk_openingDirective(object(_)).
        Logtalk_openingDirective(object(_, _)).
        Logtalk_openingDirective(object(_, _, _)).
        Logtalk_openingDirective(object(_, _, _, _)).
        Logtalk_openingDirective(object(_, _, _, _, _)).
   //categories
        Logtalk_openingDirective(category(_)).
        Logtalk_openingDirective(category(_, _)).
        Logtalk_openingDirective(category(_, _, _)).
        Logtalk_openingDirective(category(_, _, _, _)).
   //protocols
        Logtalk_openingDirective(protocol(_)).
        Logtalk_openingDirective(protocol(_, _)).
   //Prolog module directives
        Logtalk_openingDirective(module(_)).
        Logtalk_openingDirective(module(_, _)).
   //module/3 directives are currently not supported but must
   //be recognized as entity opening directives
        Logtalk_openingDirective(module(_, _, _)).


        LogtalkClosingDirective(end_object).
        LogtalkClosingDirective(endCategory).
        LogtalkClosingDirective(end_protocol).


        Logtalk_entityDirective(builtIn).
        Logtalk_entityDirective(uses(_, _)).
        Logtalk_entityDirective(use_module(_, _)).
        Logtalk_entityDirective(include(_)).
        Logtalk_entityDirective(initialization(_)).
        Logtalk_entityDirective((dynamic)).
        Logtalk_entityDirective(op(_, _, _)).
        Logtalk_entityDirective(info(_)).
        Logtalk_entityDirective(threaded).
        Logtalk_entityDirective(setLogtalkFlag(_, _)).


        Logtalk_predicateDirective(synchronized(_)).
        Logtalk_predicateDirective(dynamic(_)).
        Logtalk_predicateDirective(meta_predicate(_)).
        Logtalk_predicateDirective(meta_non_terminal(_)).
        Logtalk_predicateDirective(discontiguous(_)).
        Logtalk_predicateDirective(public(_)).
        Logtalk_predicateDirective(protected(_)).
        Logtalk_predicateDirective(private(_)).
        Logtalk_predicateDirective(mode(_, _)).
        Logtalk_predicateDirective(info(_, _)).
        Logtalk_predicateDirective(alias(_, _)).
        Logtalk_predicateDirective(multifile(_)).
        Logtalk_predicateDirective(coinductive(_)).
   //Prolog module directives that are recognized when compiling modules as objects
        Logtalk_predicateDirective(export(_)).
        Logtalk_predicateDirective(reexport(_, _)).


   //conditional compilation directives
        ConditionalCompilationDirective(if(_)).
        ConditionalCompilationDirective(elif(_)).
        ConditionalCompilationDirective(else).
        ConditionalCompilationDirective(endif).


        IsConditionalCompilationDirective((:- Directive)) :-
        nonvar(Directive),
        ConditionalCompilationDirective(Directive).



   //FileDirective(@callable)
        %
   //standard file-level directives (used for portability checking)

        FileDirective(discontiguous(_)).
        FileDirective(dynamic(_)).
        FileDirective(multifile(_)).
        FileDirective(encoding(_)).
        FileDirective(include(_)).
        FileDirective(use_module(_)).
        FileDirective(use_module(_, _)).
        FileDirective(ensureLoaded(_)).
        FileDirective(set_prologFlag(_, _)).
        FileDirective(setLogtalkFlag(_, _)).
        FileDirective(initialization(_)).
        FileDirective(op(_, _, _)).



   //utility predicates used during compilation of Logtalk entities to store and
   //access compilation context information (represented by a compound term)

        CompCtx(ctx(_, _, _, _, _, _, _, _, _, _, _, _, _)).

        CompCtx(
        ctx(Head, HeadExCtx, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, Mode, Stack, Lines),
        Head, HeadExCtx, Entity, Sender, This, Self, Prefix, MetaVars, MetaCallCtx, ExCtx, Mode, Stack, Lines
        ).

   //head of the clause being compiled
        CompCtx_head(ctx(Head, _, _, _, _, _, _, _, _, _, _, _, _), Head).

   //head execution context of the clause being compiled
        CompCtx_head_execCtx(ctx(_, HeadExCtx, _, _, _, _, _, _, _, _, _, _, _), HeadExCtx).

   //entity containing the clause being compiled (either a category or an object)
        CompCtx_entity(ctx(_, _, Entity, _, _, _, _, _, _, _, _, _, _), Entity).

        CompCtxSender(ctx(_, _, _, Sender, _, _, _, _, _, _, _, _, _), Sender).

        CompCtx_this(ctx(_, _, _, _, This, _, _, _, _, _, _, _, _), This).

        CompCtxSelf(ctx(_, _, _, _, _, Self, _, _, _, _, _, _, _), Self).

   //entity prefix used to avoid predicate name conflicts
        CompCtx_prefix(ctx(_, _, _, _, _, _, Prefix, _, _, _, _, _, _), Prefix).

        CompCtx_meta_vars(ctx(_, _, _, _, _, _, _, MetaVars, _, _, _, _, _), MetaVars).

        CompCtx_metaCallCtx(ctx(_, _, _, _, _, _, _, _, MetaCallCtx, _, _, _, _), MetaCallCtx).

        CompCtx_execCtx(ctx(_, _, _, _, _, _, _, _, _, ExCtx, _, _, _), ExCtx).

   //compilation mode; possible values are "compile(user)", "compile(aux)", and "runtime"
        CompCtx_mode(ctx(_, _, _, _, _, _, _, _, _, _, Mode, _, _), Mode).

   //stack of coinductive hypothesis (ancestor goals)
        CompCtxStack(ctx(_, _, _, _, _, _, _, _, _, _, _, Stack, _), Stack).

   //begin line and end line (a pair of integers) of the term being compiled
        CompCtxLines(ctx(_, _, _, _, _, _, _, _, _, _, _, _, Lines), Lines).



   //utility predicates used to access execution context terms

        executionContext(c(This, Entity, r(Sender, Self, MetaCallContext, Stack)), Entity, Sender, This, Self, MetaCallContext, Stack).

   //inheritance only requires updating "this" and "entity"
        executionContext_update_this_entity(c(OldThis, OldEntity, Rest), OldThis, OldEntity, c(NewThis, NewEntity, Rest), NewThis, NewEntity).

        executionContext_this_entity(c(This, Entity, _), This, Entity).



   //term_template(@callable, -callable)
        %
   //constructs a template for a callable term

        term_template(Term, Template) :-
        functor(Term, Functor, Arity),
        functor(Template, Functor, Arity).



   //Flatten_toList(+term, -list)
        %
   //flattens an item, a list of items, or a conjunction of items into a list

        Flatten_toList([A| B], [A| B]) :-
        !.

        Flatten_toList([], []) :-
        !.

        Flatten_toList((A, B), [A| BB]) :-
        !,
        Flatten_toList(B, BB).

        Flatten_toList(A, [A]).



   //validScope(@nonvar).
        %
   //valid (user-level) scope

        validScope((private)).
        validScope(protected).
        validScope((public)).



   //valid_predicateIndicator(@term, -atom, -integer)
        %
   //valid predicate indicator

        valid_predicateIndicator(Functor/Arity, Functor, Arity) :-
        atom(Functor),
        integer(Arity),
        Arity >= 0.



   //valid_non_terminalIndicator(@term, -atom, -integer, -integer)
        %
   //valid grammar rule non-terminal indicator; the last argument is the
   //arity of the corresponding predicate

        valid_non_terminalIndicator(Functor//Arity, Functor, Arity, ExtArity) :-
        atom(Functor),
        integer(Arity),
        Arity >= 0,
        ExtArity is Arity + 2.



   //valid_predicate_or_non_terminalIndicator(@term, -atom, -integer)
        %
   //valid predicate indicator or grammar rule indicator

        valid_predicate_or_non_terminalIndicator(Functor/Arity, Functor, Arity) :-
        atom(Functor),
        integer(Arity),
        Arity >= 0.

        valid_predicate_or_non_terminalIndicator(Functor//Arity, Functor, Arity) :-
        atom(Functor),
        integer(Arity),
        Arity >= 0.



   //validInfo_key_value_pair(@term, -atom, -integer)
        %
   //valid info/1-2 key-value pair

        validInfo_key_value_pair(Key is Value, Key, Value) :-
        atom(Key),
        nonvar(Value).



   //Check_entityReference(+atom, @term, -atom, -entityIdentifier)

        Check_entityReference(object, Ref, Scope, Object) :-
        (	Ref = Scope::Object ->
        Check(scope, Scope),
        Check(objectIdentifier, Object)
        ;	Ref = Object,
        Scope = (public),
        Check(objectIdentifier, Object)
        ).

        Check_entityReference(protocol, Ref, Scope, Protocol) :-
        (	Ref = Scope::Protocol ->
        Check(scope, Scope),
        Check(protocolIdentifier, Protocol)
        ;	Ref = Protocol,
        Scope = (public),
        Check(protocolIdentifier, Protocol)
        ).

        Check_entityReference(category, Ref, Scope, Category) :-
        (	Ref = Scope::Category ->
        Check(scope, Scope),
        Check(categoryIdentifier, Category)
        ;	Ref = Category,
        Scope = (public),
        Check(categoryIdentifier, Category)
        ).



   //CheckClosure(@nonvar, @compilationContext)
        %
   //checks that a closure meta-argument is valid

        CheckClosure(Closure, _) :-
        var(Closure),
        !.

        CheckClosure(Free/Goal, Ctx) :-
        !,
        CheckLambda_expression(Free/Goal, Ctx),
        CheckClosure(Goal, Ctx).

        CheckClosure(Parameters>>Goal, Ctx) :-
        !,
        CheckLambda_expression(Parameters>>Goal, Ctx),
        CheckClosure(Goal, Ctx).

        CheckClosure({Closure}, _) :-
        !,
        Check(var_orCallable, Closure).

        CheckClosure(Object::Closure, _) :-
        !,
        Check(var_or_objectIdentifier, Object),
        Check(var_orCallable, Closure).

        CheckClosure(::Closure, _) :-
        !,
        Check(var_orCallable, Closure).

        CheckClosure(^^Closure, _) :-
        !,
        Check(var_orCallable, Closure).

        CheckClosure(Object<<Closure, _) :-
        !,
        Check(var_or_objectIdentifier, Object),
        Check(var_orCallable, Closure).

        CheckClosure(':(Module, Closure), _) :-
        !,
        Check(var_or_moduleIdentifier, Module),
        Check(var_orCallable, Closure).

        CheckClosure(Closure, _) :-
        \+ callable(Closure),
        throw(type_error(callable, Closure)).

        CheckClosure(_, _).



   //CheckLambda_expression(@nonvar, @compilationContext)
        %
   //checks that a lambda expression is valid

        CheckLambda_expression(Free/Parameters>>Goal, Ctx) :-
        !,
   //first, check for errors
        Check(var_orCurly_bracketed_term, Free),
        Check(list_or_partialList, Parameters),
        Check(var_orCallable, Goal),
   //second, check for likely errors if compiling a source file
        (	CompCtx_mode(Ctx, compile(_)),
        nonvar(Free),
        nonvar(Parameters),
        nonvar(Goal) ->
        CheckLambda_expression_unclassified_variables(Free/Parameters>>Goal, Ctx),
        CheckLambda_expression_mixed_up_variables(Free/Parameters>>Goal, Ctx)
        ;	true
        ).

        CheckLambda_expression(Free/Goal, _) :-
        Check(var_orCurly_bracketed_term, Free),
        Check(var_orCallable, Goal).

        CheckLambda_expression(Parameters>>Goal, Ctx) :-
   //first, check for errors
        Check(list_or_partialList, Parameters),
        Check(var_orCallable, Goal),
   //second, check for likely errors if compiling a source file
        (	CompCtx_mode(Ctx, compile(_)),
        nonvar(Parameters),
        nonvar(Goal) ->
        CheckLambda_expression_unclassified_variables(Parameters>>Goal, Ctx)
        ;	true
        ).



   //each lambda goal variable should be either a lambda free variable or a lambda parameter

        CheckLambda_expression_unclassified_variables(Parameters>>Goal, _) :-
   //take into account currying to avoid false positives
        CheckLambda_expression_goal_variables(Goal, GoalVars),
        term_variables(Parameters, ParameterVars),
        varSubtract(GoalVars, ParameterVars, UnqualifiedVars),
        (	UnqualifiedVars \== [],
        CompilerFlag(lambda_variables, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(lambda_variables), core, unclassified_variablesInLambda_expression(File, Lines, Type, Entity, UnqualifiedVars, Parameters>>Goal))
        ;	true
        ).


        CheckLambda_expression_goal_variables(Parameters>>Goal, UnqualifiedVars) :-
        !,
        CheckLambda_expression_goal_variables(Goal, GoalVars),
        term_variables(Parameters, ParameterVars),
        varSubtract(GoalVars, ParameterVars, UnqualifiedVars).

        CheckLambda_expression_goal_variables(Goal, UnqualifiedVars) :-
        term_variables(Goal, UnqualifiedVars).



   //no lambda goal variable should be both a lambda free variable and a lambda parameter

        CheckLambda_expression_mixed_up_variables(Free/Parameters>>Goal, _) :-
        term_variables(Free, FreeVars),
        term_variables(Parameters, ParameterVars),
        Intersection(FreeVars, ParameterVars, MixedUpVars),
        (	MixedUpVars \== [],
        CompilerFlag(lambda_variables, warning) ->
        IncrementCompiling_warningsCounter',
        SourceFileContext(File, Lines, Type, Entity),
        print_message(warning(lambda_variables), core, variables_withDualRoleInLambda_expression(File, Lines, Type, Entity, MixedUpVars, Free/Parameters>>Goal))
        ;	true
        ).



   //Same_operatorClass(+atom, +atom)
        %
   //this utility predicate is used when defining new operators using op/3
   //in order to know if there's an operator of the same class that should
   //be backed up

        Same_operatorClass(fx, fx).
        Same_operatorClass(fx, fy).

        Same_operatorClass(fy, fx).
        Same_operatorClass(fy, fy).

        Same_operatorClass(xf, xf).
        Same_operatorClass(xf, yf).

        Same_operatorClass(yf, xf).
        Same_operatorClass(yf, yf).

        Same_operatorClass(xfx, xfx).
        Same_operatorClass(xfx, xfy).
        Same_operatorClass(xfx, yfx).

        Same_operatorClass(xfy, xfx).
        Same_operatorClass(xfy, xfy).
        Same_operatorClass(xfy, yfx).

        Same_operatorClass(yfx, xfx).
        Same_operatorClass(yfx, xfy).
        Same_operatorClass(yfx, yfx).



   //valid_meta_predicate_template(@term)

        valid_meta_predicate_template(Pred) :-
        callable(Pred),
        Pred =.. [_| Args],
        valid_meta_predicate_templateArgs(Args).


        valid_meta_predicate_templateArgs([]).

        valid_meta_predicate_templateArgs([Arg| Args]) :-
        ground(Arg),
        valid_meta_predicate_templateArg(Arg),
        valid_meta_predicate_templateArgs(Args).


   //meta-argument but not called
        valid_meta_predicate_templateArg((::)) :- !.
   //non meta-argument
        valid_meta_predicate_templateArg(*) :- !.
   //goal with possible existential variables qualification
        valid_meta_predicate_templateArg(^) :- !.
   //goal or closure
        valid_meta_predicate_templateArg(Arg) :-
        integer(Arg), Arg >= 0.



   //valid_mode_template(@nonvar)

        valid_mode_template(Pred) :-
        Pred =.. [_| Args],
        valid_mode_templateArgs(Args).


        valid_mode_templateArgs([]).

        valid_mode_templateArgs([Arg| Args]) :-
        (	ground(Arg) ->
        valid_mode_templateArg(Arg)
        ;	throw(instantiation_error)
        ),
        valid_mode_templateArgs(Args).



   //valid_mode_templateArg(@nonvar)

   //unspecified argument, can be input, output, or both input and output
        valid_mode_templateArg((?)).
        valid_mode_templateArg('?(_)).
   //instantiated argument on predicate call, can be further instantiated by the predicate call
        valid_mode_templateArg((+)).
        valid_mode_templateArg('+(_)).
   //non-instantiated argument (i.e. a variable) on predicate call
        valid_mode_templateArg((-)).
        valid_mode_templateArg('-(_)).
   //not modified argument (i.e. not further instantiated) by the predicate call
        valid_mode_templateArg((@)).
        valid_mode_templateArg('@(_)).
   //ground argument
        valid_mode_templateArg((++)).
        valid_mode_templateArg('++(_)).
   //unbound argument
        valid_mode_templateArg((--)).
        valid_mode_templateArg('--(_)).



   //valid_number_of_proofs(@nonvar)

   //calling the predicate using the specified mode always fails
        valid_number_of_proofs(zero).
   //calling the predicate using the specified mode always succeeds once
        valid_number_of_proofs(one).
   //calling the predicate using the specified mode may succeed once or fail
        valid_number_of_proofs(zero_or_one).
   //calling the predicate using the specified mode may fail or succeed multiple times
        valid_number_of_proofs(zero_or_more).
   //calling the predicate using the specified mode always succeed at least once
        valid_number_of_proofs(one_or_more).
   //calling the predicate using the specified mode either succeeds once or throws an error
        valid_number_of_proofs(one_or_error).
   //calling the predicate using the specified mode throws an error
        valid_number_of_proofs(error).



   //valid_predicate_property(@nonvar)

   //predicate scope (public, protected, or private)
        valid_predicate_property(scope(_)).
   //public predicate
        valid_predicate_property((public)).
   //protected predicate
        valid_predicate_property(protected).
   //private predicate
        valid_predicate_property((private)).
   //dynamic predicate
        valid_predicate_property((dynamic)).
   //static predicate
        valid_predicate_property(static).
   //predicate is defined in Logtalk source code
        valid_predicate_property(logtalk).
   //predicate is defined in Prolog source code
        valid_predicate_property(prolog).
   //predicate is defined in foreign source code (e.g. C)
        valid_predicate_property(foreign).

   //entity containing the predicate scope directive
        valid_predicate_property(declaredIn(_)).
   //object or category containing the predicate definition
        valid_predicate_property(definedIn(_)).
   //object or category containing the inherited but overridden predicate definition
        valid_predicate_property(redefinedFrom(_)).
   //meta-predicate template
        valid_predicate_property(meta_predicate(_)).
   //coinductive predicate template
        valid_predicate_property(coinductive(_)).
   //built-in predicate
        valid_predicate_property(builtIn).
   //predicate is an alias of another predicate
        valid_predicate_property(alias_of(_)).
   //entity where the predicate alias is declared
        valid_predicate_property(aliasDeclaredIn(_)).
   //clauses for the predicate can be defined within multiple entities
        valid_predicate_property((multifile)).
   //predicate version of a non-terminal
        valid_predicate_property(non_terminal(_)).
   //calls to the predicate are synchronized
        valid_predicate_property(synchronized).

   //the remaining properties are available only when the entities are compiled with the "sourceData" flag turned on

   //mode/2 predicate information (predicates can have more than one mode)
        valid_predicate_property(mode(_, _)).
   //info/2 predicate information
        valid_predicate_property(info(_)).
   //number of predicate clauses
        valid_predicate_property(number_ofClauses(_)).
   //number of predicate rules
        valid_predicate_property(number_ofRules(_)).
   //entity containing the predicate scope directive plus declaration line
        valid_predicate_property(declaredIn(_, _)).
   //object or category containing the predicate definition plus definition line
        valid_predicate_property(definedIn(_, _)).
   //object or category containing the inherited but overridden predicate definition plus definition line
        valid_predicate_property(redefinedFrom(_, _)).
   //entity where the predicate alias is declared plus declaration line
        valid_predicate_property(aliasDeclaredIn(_, _)).
   //predicate is an auxiliary predicate
        valid_predicate_property(auxiliary).
   //predicate definition is inlined
        valid_predicate_property(inline).



   //valid_protocol_property(@nonvar)

   //built-in entity
        valid_protocol_property(builtIn).
   //dynamic entity (can be abolished at runtime)
        valid_protocol_property((dynamic)).
   //static entity
        valid_protocol_property(static).
   //entity compiled in debug mode
        valid_protocol_property(debugging).
   //list of predicate indicators of public predicates declared in the entity
        valid_protocol_property(public(_)).
   //list of predicate indicators of protected predicates declared in the entity
        valid_protocol_property(protected(_)).
   //list of predicate indicators of private predicates declared in the entity
        valid_protocol_property(private(_)).
   //list of declaration properties for a predicate declared in the entity
        valid_protocol_property(declares(_, _)).
   //list of properties for a predicate alias declared in the entity
        valid_protocol_property(alias(_, _)).
   //source data available for the entity
        valid_protocol_property(sourceData).

   //the remaining properties are available only when the entities are compiled with the "sourceData" flag turned on

   //list of pairs with user-defined protocol documentation
        valid_protocol_property(info(_)).
   //source file absolute path
        valid_protocol_property(file(_)).
   //source file basename and directory
        valid_protocol_property(file(_, _)).
   //start and end lines in a source file
        valid_protocol_property(lines(_, _)).



   //validCategory_property(@nonvar)

   //category properties include all protocol properties
        validCategory_property(Property) :-
        valid_protocol_property(Property), !.
   //messages sent from the object using the ::/2 control construct generate events
        validCategory_property(events).
   //list of definition properties for a predicate defined in the category
        validCategory_property(defines(_, _)).
   //list of definition properties for a multifile predicate defined in contributing entities
        validCategory_property(includes(_, _, _)).
   //list of definition properties for a multifile predicate defined for other entities
        validCategory_property(provides(_, _, _)).
   //list of calling properties for a predicate called in the entity
        validCategory_property(calls(_, _)).
   //list of updating properties for a dynamic predicate updated in the entity
        validCategory_property(updates(_, _)).
   //number of predicate clauses (including both user-defined and auxiliary clauses)
        validCategory_property(number_ofClauses(_)).
   //number of predicate rules (including both user-defined and auxiliary clauses)
        validCategory_property(number_ofRules(_)).
   //number of user-defined predicate clauses
        validCategory_property(number_of_userClauses(_)).
   //number of user-defined predicate rules
        validCategory_property(number_of_userRules(_)).



   //valid_object_property(@nonvar)

   //object properties include all category and protocol properties
        valid_object_property(Property) :-
        validCategory_property(Property), !.
   //object contains calls to the built-in multi-threading predicates
        valid_object_property(threaded).
   //object allows the use of the <</2 debugging control construct
        valid_object_property(contextSwitchingCalls).
   //object supports dynamic declaration of new predicates
        valid_object_property(dynamicDeclarations).
   //object can be complemented by categories (old, deprecated, Logtalk 2.x property)
        valid_object_property(complements).
   //object can be complemented by categories
        valid_object_property(complements(_)).
   //object resulted from the compilation of a Prolog module
        valid_object_property(module).



   //validFlag(@nonvar)
        %
   //true if the argument is a valid Logtalk flag name

   //lint compilation flags
        validFlag(unknown_entities).
        validFlag(singleton_variables).
        validFlag(unknown_predicates).
        validFlag(undefined_predicates).
        validFlag(underscore_variables).
        validFlag(portability).
        validFlag(redefined_builtIns).
        validFlag(missingDirectives).
        validFlag(duplicatedDirectives).
        validFlag(lambda_variables).
        validFlag(suspiciousCalls).
        validFlag(trivial_goalFails).
        validFlag(always_true_orFalse_goals).
        validFlag(deprecated).
   //optional features compilation flags
        validFlag(complements).
        validFlag(dynamicDeclarations).
        validFlag(events).
        validFlag(contextSwitchingCalls).
   //other compilation flags
        validFlag(scratchDirectory).
        validFlag(report).
        validFlag(hook).
        validFlag(code_prefix).
        validFlag(optimize).
        validFlag(debug).
        validFlag(clean).
        validFlag(sourceData).
        validFlag(reload).
        validFlag(relative_to).
        validFlag('$relative_to').
   //read-only compilation flags
        validFlag(versionData).
   //startup flags
        validFlag(settingsFile).
   //backend Prolog compiler information
        validFlag(prologDialect).
        validFlag(prolog_version).
        validFlag(prologCompatible_version).
        validFlag(prologConformance).
   //features requiring specific backend Prolog compiler support
        validFlag(unicode).
        validFlag(encodingDirective).
        validFlag(engines).
        validFlag(threads).
        validFlag(modules).
        validFlag(tabling).
        validFlag(coinduction).
   //backend Prolog compiler and loader options
        validFlag(prologCompiler).
        validFlag(prologLoader).



   //Read_onlyFlag(@nonvar)
        %
   //true if the argument is a read only Logtalk flag name

   //Logtalk version flags
        Read_onlyFlag(versionData).
   //startup flags
        Read_onlyFlag(settingsFile).
   //backend Prolog compiler features
        Read_onlyFlag(prologDialect).
        Read_onlyFlag(prolog_version).
        Read_onlyFlag(prologCompatible_version).
        Read_onlyFlag(prologConformance).
        Read_onlyFlag(unicode).
        Read_onlyFlag(encodingDirective).
        Read_onlyFlag(engines).
        Read_onlyFlag(threads).
        Read_onlyFlag(modules).
        Read_onlyFlag(tabling).
        Read_onlyFlag(coinduction).



   //validFlag_value(@atom, @nonvar)

        validFlag_value(unknown_entities, silent) :- !.
        validFlag_value(unknown_entities, warning) :- !.

        validFlag_value(singleton_variables, silent) :- !.
        validFlag_value(singleton_variables, warning) :- !.

        validFlag_value(unknown_predicates, silent) :- !.
        validFlag_value(unknown_predicates, warning) :- !.
        validFlag_value(unknown_predicates, error) :- !.

        validFlag_value(undefined_predicates, silent) :- !.
        validFlag_value(undefined_predicates, warning) :- !.
        validFlag_value(undefined_predicates, error) :- !.

        validFlag_value(portability, silent) :- !.
        validFlag_value(portability, warning) :- !.

        validFlag_value(redefined_builtIns, silent) :- !.
        validFlag_value(redefined_builtIns, warning) :- !.

        validFlag_value(missingDirectives, silent) :- !.
        validFlag_value(missingDirectives, warning) :- !.

        validFlag_value(duplicatedDirectives, silent) :- !.
        validFlag_value(duplicatedDirectives, warning) :- !.

        validFlag_value(lambda_variables, silent) :- !.
        validFlag_value(lambda_variables, warning) :- !.

        validFlag_value(suspiciousCalls, silent) :- !.
        validFlag_value(suspiciousCalls, warning) :- !.

        validFlag_value(trivial_goalFails, silent) :- !.
        validFlag_value(trivial_goalFails, warning) :- !.

        validFlag_value(always_true_orFalse_goals, silent) :- !.
        validFlag_value(always_true_orFalse_goals, warning) :- !.

        validFlag_value(deprecated, silent) :- !.
        validFlag_value(deprecated, warning) :- !.

        validFlag_value(report, on) :- !.
        validFlag_value(report, warnings) :- !.
        validFlag_value(report, off) :- !.

        validFlag_value(clean, on) :- !.
        validFlag_value(clean, off) :- !.

        validFlag_value(underscore_variables, dontCare) :- !.
        validFlag_value(underscore_variables, singletons) :- !.

        validFlag_value(code_prefix, Prefix) :-
        atom(Prefix),
        atomLength(Prefix, 1).

        validFlag_value(optimize, on) :- !.
        validFlag_value(optimize, off) :- !.

        validFlag_value(sourceData, on) :- !.
        validFlag_value(sourceData, off) :- !.

        validFlag_value(reload, always) :- !.
        validFlag_value(reload, changed) :- !.
        validFlag_value(reload, skip) :- !.

        validFlag_value(relative_to, Directory) :-
        atom(Directory).
        validFlag_value('$relative_to', Directory) :-
   //internal flag; just for documenting value entityKind
        atom(Directory).

        validFlag_value(debug, on) :- !.
        validFlag_value(debug, off) :- !.

        validFlag_value(complements, allow) :- !.
        validFlag_value(complements, restrict) :- !.
        validFlag_value(complements, deny) :- !.

        validFlag_value(dynamicDeclarations, allow) :- !.
        validFlag_value(dynamicDeclarations, deny) :- !.

        validFlag_value(contextSwitchingCalls, allow) :- !.
        validFlag_value(contextSwitchingCalls, deny) :- !.

        validFlag_value(events, allow) :- !.
        validFlag_value(events, deny) :- !.

        validFlag_value(hook, Obj) :-
        callable(Obj).

        validFlag_value(scratchDirectory, Directory) :-
        callable(Directory).

        validFlag_value(prologCompiler, Options) :-
        IsList(Options).
        validFlag_value(prologLoader, Options) :-
        IsList(Options).

        validFlag_value(versionData, Version) :-
        compound(Version),
        functor(Version, logtalk, 4).

        validFlag_value(settingsFile, allow) :- !.
        validFlag_value(settingsFile, restrict) :- !.
        validFlag_value(settingsFile, deny) :- !.

        validFlag_value(prologDialect, Dialect) :-
        atom(Dialect).
        validFlag_value(prolog_version, Version) :-
        compound(Version),
        functor(Version, v, 3).
        validFlag_value(prologCompatible_version, Version) :-
        compound(Version),
        functor(Version, v, 3).
        validFlag_value(prologConformance, strict) :- !.
        validFlag_value(prologConformance, lax) :- !.

        validFlag_value(unicode, full) :- !.
        validFlag_value(unicode, bmp) :- !.
        validFlag_value(unicode, unsupported) :- !.

        validFlag_value(encodingDirective, full) :- !.
        validFlag_value(encodingDirective, source) :- !.
        validFlag_value(encodingDirective, unsupported) :- !.

        validFlag_value(engines, supported) :- !.
        validFlag_value(engines, unsupported) :- !.

        validFlag_value(threads, supported) :- !.
        validFlag_value(threads, unsupported) :- !.

        validFlag_value(modules, supported) :- !.
        validFlag_value(modules, unsupported) :- !.

        validFlag_value(tabling, supported) :- !.
        validFlag_value(tabling, unsupported) :- !.

        validFlag_value(coinduction, supported) :- !.
        validFlag_value(coinduction, unsupported) :- !.



   //validRemark(@term)
        %
   //valid predicate remark documentation on info/1-2 directives

        validRemark(Topic - Text) :-
        atom(Topic),
        atom(Text).



   //valid_predicateAllocation(@nonvar)
        %
   //valid predicate allocation on info/2 directive

   //predicate defined in the object containing its scope directive
        valid_predicateAllocation(container).
   //predicate should be defined in the descendant objects
        valid_predicateAllocation(descendants).
   //predicate should be defined in the class instances
        valid_predicateAllocation(instances).
   //predicate should be defined in the class and its subclasses
        valid_predicateAllocation(classes).
   //predicate should be defined in the class subclasses
        valid_predicateAllocation(subclasses).
   //no restrictions on where the predicate should be defined
        valid_predicateAllocation(any).



   //valid_predicateRedefinition(@nonvar)
        %
   //valid predicate redefinition on info/2 directive

   //predicate should not be redefined
        valid_predicateRedefinition(never).
   //predicate can be freely redefined
        valid_predicateRedefinition(free).
   //predicate redefinition must call the inherited definition
        valid_predicateRedefinition(generalize).
   //predicate redefinition must call the inherited definition as the first body goal
        valid_predicateRedefinition(callSuperFirst).
   //predicate redefinition must call the inherited definition as the last body goal
        valid_predicateRedefinition(callSuperLast).



   //valid_predicate_exception(@term)
        %
   //valid predicate exception documentation on info/2 directive

        valid_predicate_exception(Description - Term) :-
        atom(Description),
        nonvar(Term).



   //valid_predicateCall_example(@term)
        %
   //valid predicate call example documentation on info/1 directive

        valid_predicateCall_example(Description - Call - {Bindings}) :-
        atom(Description),
        callable(Call),
        nonvar(Bindings),
        (	Bindings == no -> true
        ;	Bindings == yes -> true
        ;	valid_example_var_bindings(Bindings)
        ).



   //valid_predicateCall_example(@term, +atom, +integer)
        %
   //valid predicate call example documentation on info/2 directive

        valid_predicateCall_example((Description - Call - {Bindings}), Functor, Arity) :-
        atom(Description),
        nonvar(Call),
        functor(Pred, Functor, Arity),
        Call = Pred,
        nonvar(Bindings),
        (	Bindings == no -> true
        ;	Bindings == yes -> true
        ;	valid_example_var_bindings(Bindings)
        ).



        valid_example_var_bindings((Binding, Bindings)) :-
        !,
        valid_example_var_binding(Binding),
        valid_example_var_bindings(Bindings).

        valid_example_var_bindings(Binding) :-
        valid_example_var_binding(Binding).


        valid_example_var_binding(Binding) :-
        nonvar(Binding),
        Binding = (Var = _),
        var(Var).



   //Logtalk built-in predicates
        %
   //Logtalk_builtIn_predicate(?callable, ?callable)
        %
   //the second argument is either a meta-predicate template
   //(when aplicable) or the atom "no"


   //message sending and context switching control constructs
        Logtalk_builtIn_predicate(_ :: _, no).
        Logtalk_builtIn_predicate(_ << _, no).
   //compiling and loading predicates
        Logtalk_builtIn_predicate(logtalkCompile(_), no).
        Logtalk_builtIn_predicate(logtalkCompile(_, _), no).
        Logtalk_builtIn_predicate(logtalkLoad(_), no).
        Logtalk_builtIn_predicate(logtalkLoad(_, _), no).
        Logtalk_builtIn_predicate(logtalk_make, no).
        Logtalk_builtIn_predicate(logtalk_make(_), no).
        Logtalk_builtIn_predicate(logtalkLoadContext(_, _), no).
        Logtalk_builtIn_predicate(logtalkLibrary_path(_, _), no).
        Logtalk_builtIn_predicate(logtalk_make_targetAction(_), no).
   //entity properties
        Logtalk_builtIn_predicate(protocol_property(_, _), no).
        Logtalk_builtIn_predicate(category_property(_, _), no).
        Logtalk_builtIn_predicate(object_property(_, _), no).
   //entity enumeration
        Logtalk_builtIn_predicate(current_protocol(_), no).
        Logtalk_builtIn_predicate(currentCategory(_), no).
        Logtalk_builtIn_predicate(current_object(_), no).
   //entity creation predicates
        Logtalk_builtIn_predicate(create_object(_, _, _, _), no).
        Logtalk_builtIn_predicate(createCategory(_, _, _, _), no).
        Logtalk_builtIn_predicate(create_protocol(_, _, _), no).
   //entity abolishing predicates
        Logtalk_builtIn_predicate(abolish_object(_), no).
        Logtalk_builtIn_predicate(abolishCategory(_), no).
        Logtalk_builtIn_predicate(abolish_protocol(_), no).
   //entity relations
        Logtalk_builtIn_predicate(implements_protocol(_, _), no).
        Logtalk_builtIn_predicate(implements_protocol(_, _, _), no).
        Logtalk_builtIn_predicate(importsCategory(_, _), no).
        Logtalk_builtIn_predicate(importsCategory(_, _, _), no).
        Logtalk_builtIn_predicate(instantiatesClass(_, _), no).
        Logtalk_builtIn_predicate(instantiatesClass(_, _, _), no).
        Logtalk_builtIn_predicate(specializesClass(_, _), no).
        Logtalk_builtIn_predicate(specializesClass(_, _, _), no).
        Logtalk_builtIn_predicate(extends_protocol(_, _), no).
        Logtalk_builtIn_predicate(extends_protocol(_, _, _), no).
        Logtalk_builtIn_predicate(extends_object(_, _), no).
        Logtalk_builtIn_predicate(extends_object(_, _, _), no).
        Logtalk_builtIn_predicate(extendsCategory(_, _), no).
        Logtalk_builtIn_predicate(extendsCategory(_, _, _), no).
        Logtalk_builtIn_predicate(complements_object(_, _), no).
   //protocol conformance
        Logtalk_builtIn_predicate(conforms_to_protocol(_, _), no).
        Logtalk_builtIn_predicate(conforms_to_protocol(_, _, _), no).
   //events
        Logtalk_builtIn_predicate(abolish_events(_, _, _, _, _), no).
        Logtalk_builtIn_predicate(define_events(_, _, _, _, _), no).
        Logtalk_builtIn_predicate(current_event(_, _, _, _, _), no).
   //flags
        Logtalk_builtIn_predicate(currentLogtalkFlag(_, _), no).
        Logtalk_builtIn_predicate(setLogtalkFlag(_, _), no).
        Logtalk_builtIn_predicate(createLogtalkFlag(_, _, _), no).
   //multi-threading predicates
        Logtalk_builtIn_predicate(threaded(_), threaded(0)).
        Logtalk_builtIn_predicate(threadedCall(_, _), threadedCall(0, *)).
        Logtalk_builtIn_predicate(threadedCall(_), threadedCall(0)).
        Logtalk_builtIn_predicate(threaded_once(_, _), threaded_once(0, *)).
        Logtalk_builtIn_predicate(threaded_once(_), threaded_once(0)).
        Logtalk_builtIn_predicate(threadedIgnore(_), threadedIgnore(0)).
        Logtalk_builtIn_predicate(threaded_exit(_, _), threaded_exit((::), *)).
        Logtalk_builtIn_predicate(threaded_exit(_), threaded_exit((::))).
        Logtalk_builtIn_predicate(threaded_peek(_, _), threaded_peek((::), *)).
        Logtalk_builtIn_predicate(threaded_peek(_), threaded_peek((::))).
        Logtalk_builtIn_predicate(threaded_wait(_), no).
        Logtalk_builtIn_predicate(threaded_notify(_), no).
   //threaded engines predicates
        Logtalk_builtIn_predicate(threaded_engineCreate(_, _, _), threaded_engineCreate(*, 0, *)).
        Logtalk_builtIn_predicate(threaded_engineDestroy(_), threaded_engineDestroy(*)).
        Logtalk_builtIn_predicate(threaded_engineSelf(_), threaded_engineSelf(*)).
        Logtalk_builtIn_predicate(threaded_engine(_), threaded_engine(*)).
        Logtalk_builtIn_predicate(threaded_engine_next(_, _), threaded_engine_next(*, *)).
        Logtalk_builtIn_predicate(threaded_engine_nextReified(_, _), threaded_engine_nextReified(*, *)).
        Logtalk_builtIn_predicate(threaded_engine_yield(_), threaded_engine_yield(*)).
        Logtalk_builtIn_predicate(threaded_engine_post(_, _), threaded_engine_post(*, *)).
        Logtalk_builtIn_predicate(threaded_engineFetch(_), threaded_engineFetch(*)).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // DCG rule conversion
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //DcgRule(@grammarRule, -clause, @compilationContext)
        %
   //converts a grammar rule into a normal clause

        DcgRule((RHead --> _), _, _) :-
        var(RHead),
        throw(instantiation_error).

        DcgRule((RHead, _ --> _), _, _) :-
        var(RHead),
        throw(instantiation_error).

        DcgRule((Entity::NonTerminal, Terminals --> GRBody), (Entity::Head :- Body), Ctx) :-
        !,
        Check(objectIdentifier, Entity),
        DcgRule((NonTerminal, Terminals --> GRBody), (Head :- Body), Ctx).

        DcgRule((':(Module, NonTerminal), Terminals --> GRBody), (':(Module, Head) :- Body), Ctx) :-
        !,
        Check(moduleIdentifier, Module),
        DcgRule((NonTerminal, Terminals --> GRBody), (Head :- Body), Ctx).

        DcgRule((phrase(_), _ --> _), _, _) :-
        throw(permission_error(modify, builtIn_non_terminal, phrase//1)).

        DcgRule((NonTerminal, _ --> _), _, _) :-
        functor(NonTerminal, call, Arity),
        Arity >= 1,
        throw(permission_error(modify, builtIn_non_terminal, call//Arity)).

        DcgRule((NonTerminal, Terminals --> GRBody), (Head :- Body), Ctx) :-
        !,
        Dcg_non_terminal(NonTerminal, S0, S, Head),
        Dcg_body(GRBody, S0, S1, Goal1, Ctx),
        Dcg_terminals(Terminals, S, S1, Goal2),
        Body = (Goal1, Goal2),
        functor(NonTerminal, Functor, Arity),
        (	CompCtx_mode(Ctx, compile(_)),
        \+ ppDefines_non_terminal_(Functor, Arity) ->
        assertz(ppDefines_non_terminal_(Functor, Arity))
        ;	true
        ).

        DcgRule((Entity::NonTerminal --> GRBody), (Entity::Head :- Body), Ctx) :-
        !,
        Check(objectIdentifier, Entity),
        DcgRule((NonTerminal --> GRBody), (Head :- Body), Ctx).

        DcgRule((':(Module, NonTerminal) --> GRBody), (':(Module, Head) :- Body), Ctx) :-
        !,
        Check(moduleIdentifier, Module),
        DcgRule((NonTerminal --> GRBody), (Head :- Body), Ctx).

        DcgRule((phrase(_) --> _), _, _) :-
        throw(permission_error(modify, builtIn_non_terminal, phrase//1)).

        DcgRule((NonTerminal --> _), _, _) :-
        functor(NonTerminal, call, Arity),
        Arity >= 1,
        throw(permission_error(modify, builtIn_non_terminal, call//Arity)).

        DcgRule((eos --> _), _, _) :-
        throw(permission_error(modify, builtIn_non_terminal, eos//0)).

        DcgRule((NonTerminal --> GRBody), (Head :- Body), Ctx) :-
        !,
        Dcg_non_terminal(NonTerminal, S0, S, Head),
        Dcg_body(GRBody, S0, S, Body, Ctx),
        functor(NonTerminal, Functor, Arity),
        (	CompCtx_mode(Ctx, compile(_)),
        \+ ppDefines_non_terminal_(Functor, Arity) ->
        assertz(ppDefines_non_terminal_(Functor, Arity))
        ;	true
        ).

        DcgRule(Term, _, _) :-
        throw(type_error(grammarRule, Term)).



   //Dcg_non_terminal(+callable, @var, @var, -goal)
        %
   //translates a grammar goal non-terminal

        Dcg_non_terminal(NonTerminal, _, _, _) :-
        Check(callable, NonTerminal),
        pp_protocol_(_, _, _, _, _),
   //protocols cannot contain non-terminal definitions
        functor(NonTerminal, Functor, Arity),
        throw(permission_error(define, non_terminal, Functor//Arity)).

        Dcg_non_terminal(NonTerminal, S0, S, Goal) :-
        NonTerminal =.. NonTerminalUniv,
        Append(NonTerminalUniv, [S0, S], GoalUniv),
        Goal =.. GoalUniv.



   //Dcg_terminals(+list, @var, @var, -goal)
        %
   //translates a list of terminals

        Dcg_terminals(Terminals, S0, S, Goal) :-
        Check(nonvar, Terminals),
        (	IsList(Terminals) ->
        Append(Terminals, S, List),
        Goal = (S0 = List)
        ;	Check(list_or_partialList, Terminals),
        Goal = {Append(Terminals, S, S0)}
        ).



   //Dcg_msg(@dcgbody @objectIdentifier, @var, @var, -body)
        %
   //translates a grammar rule message to an object into a predicate message

        Dcg_msg(Var, Obj, S0, S, phrase(Obj::Var, S0, S)) :-
        var(Var),
        !.

        Dcg_msg('*->(GRIf, GRThen), Obj, S0, S, '*->(If, Then)) :-
        predicate_property('*->(_, _), builtIn),
        !,
        Dcg_msg(GRIf, Obj, S0, S1, If),
        Dcg_msg(GRThen, Obj, S1, S, Then).

        Dcg_msg((GRIf -> GRThen), Obj, S0, S, (If -> Then)) :-
        !,
        Dcg_msg(GRIf, Obj, S0, S1, If),
        Dcg_msg(GRThen, Obj, S1, S, Then).

        Dcg_msg((GREither; GROr), Obj, S0, S, (Either; Or)) :-
        !,
        Dcg_msg(GREither, Obj, S0, S, Either),
        Dcg_msg(GROr, Obj, S0, S, Or).

        Dcg_msg((GRFirst, GRSecond), Obj, S0, S, (First, Second)) :-
        !,
        Dcg_msg(GRFirst, Obj, S0, S1, First),
        Dcg_msg(GRSecond, Obj, S1, S, Second).

        Dcg_msg(!, _, S0, S, (!, (S0 = S))) :-
        !.

        Dcg_msg(NonTerminal, Obj, S0, S, Obj::Pred) :-
        Dcg_non_terminal(NonTerminal, S0, S, Pred).



   //DcgSelf_msg(@dcgbody, @var, @var, -body, -body)
        %
   //translates a grammar rule message to an object into a predicate message

        DcgSelf_msg(Var, S0, S, phrase(::Var, S0, S)) :-
        var(Var),
        !.

        DcgSelf_msg('*->(GRIf, GRThen), S0, S, '*->(If, Then)) :-
        predicate_property('*->(_, _), builtIn),
        !,
        DcgSelf_msg(GRIf, S0, S1, If),
        DcgSelf_msg(GRThen, S1, S, Then).

        DcgSelf_msg((GRIf -> GRThen), S0, S, (If -> Then)) :-
        !,
        DcgSelf_msg(GRIf, S0, S1, If),
        DcgSelf_msg(GRThen, S1, S, Then).

        DcgSelf_msg((GREither; GROr), S0, S, (Either; Or)) :-
        !,
        DcgSelf_msg(GREither, S0, S, Either),
        DcgSelf_msg(GROr, S0, S, Or).

        DcgSelf_msg((GRFirst, GRSecond), S0, S, (First, Second)) :-
        !,
        DcgSelf_msg(GRFirst, S0, S1, First),
        DcgSelf_msg(GRSecond, S1, S, Second).

        DcgSelf_msg(!, S0, S, (!, (S0 = S))) :-
        !.

        DcgSelf_msg(NonTerminal, S0, S, ::Pred) :-
        Dcg_non_terminal(NonTerminal, S0, S, Pred).



   //DcgSuperCall(@dcgbody, @var, @var, -body)
        %
   //translates a super call to a grammar rule in an ancestor entity

        DcgSuperCall(Var, S0, S, phrase(^^Var, S0, S)) :-
        var(Var),
        !.

        DcgSuperCall(NonTerminal, S0, S, ^^Pred) :-
        Dcg_non_terminal(NonTerminal, S0, S, Pred).



   //Dcg_body(@dcgbody, @var, @var, -body, @compilationContext)
        %
   //translates a grammar rule body into a Prolog clause body

        Dcg_body(Var, S0, S, phrase(Var, S0, S), _) :-
        var(Var),
        !.

        Dcg_body(Free/Parameters>>Lambda, S0, S, call(Free/Parameters>>Lambda, S0, S), Ctx) :-
        !,
        CheckLambda_expression(Free/Parameters>>Lambda, Ctx),
        (	\+ Parameters \= [_, _] ->
        true
        ;	throw(representation_error(lambda_parameters))
        ).

        Dcg_body(Parameters>>Lambda, S0, S, call(Parameters>>Lambda, S0, S), Ctx) :-
        !,
        CheckLambda_expression(Parameters>>Lambda, Ctx),
        (	\+ Parameters \= [_, _] ->
        true
        ;	throw(representation_error(lambda_parameters))
        ).

        Dcg_body(Free/Lambda, S0, S, call(Free/Lambda, S0, S), Ctx) :-
        !,
        CheckLambda_expression(Free/Lambda, Ctx).

        Dcg_body(Obj::RGoal, S0, S, CGoal, _) :-
        !,
        Dcg_msg(RGoal, Obj, S0, S, CGoal).

        Dcg_body(::RGoal, S0, S, CGoal, _) :-
        !,
        DcgSelf_msg(RGoal, S0, S, CGoal).

        Dcg_body(^^RGoal, S0, S, CGoal, _) :-
        !,
        DcgSuperCall(RGoal, S0, S, CGoal).

        Dcg_body(':(Module, RGoal), S0, S, CGoal, _) :-
        !,
        (	callable(RGoal) ->
        RGoal =.. RGoalUniv,
        Append(RGoalUniv, [S0, S], GoalUniv),
        Goal =.. GoalUniv,
        CGoal = ':(Module, Goal)
        ;	CGoal = call(':(Module,RGoal), S0, S)
        ).

        Dcg_body((GRIfThen; GRElse), S0, S, (If -> Then; Else), Ctx) :-
        nonvar(GRIfThen),
        GRIfThen = (GRIf -> GRThen),
        !,
        Dcg_body(GRIf, S0, S1, If, Ctx),
        Dcg_body(GRThen, S1, S, Then, Ctx),
        Dcg_body(GRElse, S0, S, Else, Ctx).

        Dcg_body((GRIfThen; GRElse), S0, S, ('*->(If, Then); Else), Ctx) :-
        nonvar(GRIfThen),
        GRIfThen = '*->(GRIf, GRThen),
        predicate_property('*->(_, _), builtIn),
        !,
        Dcg_body(GRIf, S0, S1, If, Ctx),
        Dcg_body(GRThen, S1, S, Then, Ctx),
        Dcg_body(GRElse, S0, S, Else, Ctx).

        Dcg_body((GREither; GROr), S0, S, (Either; Or), Ctx) :-
        !,
        Dcg_body(GREither, S0, S, Either0, Ctx),
        FixDisjunctionLeftSide(Either0, Either),
        Dcg_body(GROr, S0, S, Or, Ctx).

        Dcg_body('*->(GRIf, GRThen), S0, S, '*->(If, Then), Ctx) :-
        predicate_property('*->(_, _), builtIn),
        !,
        Dcg_body(GRIf, S0, S1, If, Ctx),
        Dcg_body(GRThen, S1, S, Then, Ctx).

        Dcg_body((GRIf -> GRThen), S0, S, (If -> Then), Ctx) :-
        !,
        Dcg_body(GRIf, S0, S1, If, Ctx),
        Dcg_body(GRThen, S1, S, Then, Ctx).

        Dcg_body((GRFirst, GRSecond), S0, S, (First, Second), Ctx) :-
        !,
        Dcg_body(GRFirst, S0, S1, First, Ctx),
        Dcg_body(GRSecond, S1, S, Second, Ctx).

        Dcg_body(!, S0, S, (!, (S0 = S)), _) :-
        !.

        Dcg_body('{}', S0, S, (S0 = S), _) :-
        !.

        Dcg_body({Goal}, S0, S, (call(Goal), (S0 = S)), _) :-
        var(Goal),
        !.

        Dcg_body({Goal}, S0, S, (Goal, (S0 = S)), _) :-
        !,
        Check(callable, Goal).

        Dcg_body(\+ GRBody, S0, S, (\+ Goal, (S0 = S)), Ctx) :-
        !,
        Dcg_body(GRBody, S0, _, Goal, Ctx).

        Dcg_body(phrase(GRBody), S0, S, phrase(GRBody, S0, S), _) :-
        !.

        Dcg_body(eos, S0, S, (S0 = [], S = []), _) :-
        !.

        Dcg_body(GRBody, S0, S, Goal, _) :-
        functor(GRBody, call, Arity),
        Arity >= 1,
        !,
        GRBody =.. [call, Closure| Args],
        Check(var_orCallable, Closure),
        Append(Args, [S0, S], FullArgs),
        Goal =.. [call, Closure| FullArgs].

        Dcg_body([], S0, S, (S0 = S), _) :-
        !.

        Dcg_body([T| Ts], S0, S, Goal, _) :-
        !,
        Dcg_terminals([T| Ts], S0, S, Goal).

        Dcg_body(String, S0, S, Goal, _) :-
        String(String),
        !,
        StringCodes(String, Codes),
        Dcg_terminals(Codes, S0, S, Goal).

        Dcg_body(Alias, S0, S, Goal, Ctx) :-
        pp_uses_non_terminal_(Obj, Original, Alias, Pred, PredAlias, _),
        !,
   //we must register here otherwise the non-terminal alias information would be lost
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, _, Mode, _, _),
        AddReferenced_object_message(Mode, Obj, Pred, PredAlias, Head),
        Dcg_body(Obj::Original, S0, S, Goal, Ctx).

        Dcg_body(Alias, S0, S, Goal, Ctx) :-
        pp_use_module_non_terminal_(Module, Original, Alias, Pred, PredAlias, _),
        !,
   //we must register here otherwise the non-terminal alias information would be lost
        CompCtx(Ctx, Head, _, _, _, _, _, _, _, _, _, Mode, _, _),
        AddReferenced_module_predicate(Mode, Module, Pred, PredAlias, Head),
        Dcg_body(':(Module, Original), S0, S, Goal, Ctx).

        Dcg_body(NonTerminal, S0, S, Goal, Ctx) :-
        Dcg_non_terminal(NonTerminal, S0, S, Goal),
        functor(NonTerminal, Functor, Arity),
        (	CompCtx(Ctx, _, _, _, _, _, _, _, _, _, _, compile(_), _, Lines),
        \+ ppCalls_non_terminal_(Functor, Arity, _) ->
        assertz(ppCalls_non_terminal_(Functor, Arity, Lines))
        ;	true
        ).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // table of ISO Prolog specified built-in predicates
        %
   // (used for portability checking)
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //IsoSpec_predicate(?callable)

   //control constructs
        IsoSpec_predicate(true).
        IsoSpec_predicate(fail).
        IsoSpec_predicate(false).
        IsoSpec_predicate(call(_)).
        IsoSpec_predicate(!).
        IsoSpec_predicate((Goal; _)) :-
        (	var(Goal) ->
        true
        ;	Goal \= '*->(_, _)
        ).
        IsoSpec_predicate((_, _)).
        IsoSpec_predicate((_ -> _)).
        IsoSpec_predicate(catch(_, _, _)).
        IsoSpec_predicate(throw(_)).
   //term unification
        IsoSpec_predicate((_ = _)).
        IsoSpec_predicate((_ \= _)).
        IsoSpec_predicate(unify_with_occursCheck(_, _)).
        IsoSpec_predicate(subsumes_term(_, _)).
   //term testing
        IsoSpec_predicate(var(_)).
        IsoSpec_predicate(nonvar(_)).
        IsoSpec_predicate(atom(_)).
        IsoSpec_predicate(atomic(_)).
        IsoSpec_predicate(number(_)).
        IsoSpec_predicate(integer(_)).
        IsoSpec_predicate(float(_)).
        IsoSpec_predicate(compound(_)).
        IsoSpec_predicate(acyclic_term(_)).
        IsoSpec_predicate(callable(_)).
        IsoSpec_predicate(ground(_)).
   //term comparison
        IsoSpec_predicate((_ @=< _)).
        IsoSpec_predicate((_ @< _)).
        IsoSpec_predicate((_ @>= _)).
        IsoSpec_predicate((_ @> _)).
        IsoSpec_predicate((_ == _)).
        IsoSpec_predicate((_ \== _)).
        IsoSpec_predicate(compare(_, _, _)).
   //term creation and decomposition
        IsoSpec_predicate(functor(_, _, _)).
        IsoSpec_predicate(arg(_, _, _)).
        IsoSpec_predicate(_ =.. _).
        IsoSpec_predicate(copy_term(_, _)).
        IsoSpec_predicate(term_variables(_, _)).
   //arithmetic evaluation
        IsoSpec_predicate(_ is _).
   //arithmetic comparison
        IsoSpec_predicate((_ =< _)).
        IsoSpec_predicate((_ < _)).
        IsoSpec_predicate((_ >= _)).
        IsoSpec_predicate((_ > _)).
        IsoSpec_predicate((_ =:= _)).
        IsoSpec_predicate((_ =\= _)).
   //database
        IsoSpec_predicate(clause(_, _)).
        IsoSpec_predicate(current_predicate(_)).
        IsoSpec_predicate(asserta(_)).
        IsoSpec_predicate(assertz(_)).
        IsoSpec_predicate(retract(_)).
        IsoSpec_predicate(retractall(_)).
        IsoSpec_predicate(abolish(_)).
   //all solutions
        IsoSpec_predicate(findall(_, _, _)).
        IsoSpec_predicate(bagof(_, _, _)).
        IsoSpec_predicate(setof(_, _, _)).
   //stream selection and control
        IsoSpec_predicate(currentInput(_)).
        IsoSpec_predicate(current_output(_)).
        IsoSpec_predicate(setInput(_)).
        IsoSpec_predicate(set_output(_)).
        IsoSpec_predicate(open(_, _, _, _)).
        IsoSpec_predicate(open(_, _, _)).
        IsoSpec_predicate(close(_, _)).
        IsoSpec_predicate(close(_)).
        IsoSpec_predicate(flush_output(_)).
        IsoSpec_predicate(flush_output).
        IsoSpec_predicate(stream_property(_, _)).
        IsoSpec_predicate(at_end_ofStream).
        IsoSpec_predicate(at_end_ofStream(_)).
        IsoSpec_predicate(setStream_position(_, _)).
   //character and byte input/output
        IsoSpec_predicate(getChar(_, _)).
        IsoSpec_predicate(getChar(_)).
        IsoSpec_predicate(getCode(_, _)).
        IsoSpec_predicate(getCode(_)).
        IsoSpec_predicate(peekChar(_, _)).
        IsoSpec_predicate(peekChar(_)).
        IsoSpec_predicate(peekCode(_, _)).
        IsoSpec_predicate(peekCode(_)).
        IsoSpec_predicate(putChar(_, _)).
        IsoSpec_predicate(putChar(_)).
        IsoSpec_predicate(putCode(_, _)).
        IsoSpec_predicate(putCode(_)).
        IsoSpec_predicate(nl).
        IsoSpec_predicate(nl(_)).
        IsoSpec_predicate(get_byte(_, _)).
        IsoSpec_predicate(get_byte(_)).
        IsoSpec_predicate(peek_byte(_, _)).
        IsoSpec_predicate(peek_byte(_)).
        IsoSpec_predicate(put_byte(_, _)).
        IsoSpec_predicate(put_byte(_)).
   //term input/output
        IsoSpec_predicate(read_term(_, _, _)).
        IsoSpec_predicate(read_term(_, _)).
        IsoSpec_predicate(read(_)).
        IsoSpec_predicate(read(_, _)).
        IsoSpec_predicate(write_term(_, _, _)).
        IsoSpec_predicate(write_term(_, _)).
        IsoSpec_predicate(write(_)).
        IsoSpec_predicate(write(_, _)).
        IsoSpec_predicate(writeq(_)).
        IsoSpec_predicate(writeq(_, _)).
        IsoSpec_predicate(writeCanonical(_)).
        IsoSpec_predicate(writeCanonical(_, _)).
        IsoSpec_predicate(op(_, _, _)).
        IsoSpec_predicate(current_op(_, _, _)).
        IsoSpec_predicate(charConversion(_, _)).
        IsoSpec_predicate(currentCharConversion(_, _)).
   //logic and control
        IsoSpec_predicate(\+ _).
        IsoSpec_predicate(once(_)).
        IsoSpec_predicate(repeat).
   //atomic term processing
        IsoSpec_predicate(atomLength(_, _)).
        IsoSpec_predicate(atomConcat(_, _, _)).
        IsoSpec_predicate(subAtom(_, _, _, _, _)).
        IsoSpec_predicate(atomChars(_, _)).
        IsoSpec_predicate(atomCodes(_, _)).
        IsoSpec_predicate(charCode(_, _)).
        IsoSpec_predicate(numberChars(_, _)).
        IsoSpec_predicate(numberCodes(_, _)).
   //implementation defined hooks functions
        IsoSpec_predicate(set_prologFlag(_, _)).
        IsoSpec_predicate(current_prologFlag(_, _)).
        IsoSpec_predicate(halt).
        IsoSpec_predicate(halt(_)).
   //sorting
        IsoSpec_predicate(keysort(_, _)).
        IsoSpec_predicate(sort(_, _)).

   //the following predicates are not part of the ISO/IEC 13211-1 Prolog standard
   //but can be found either on Core Revision standardization proposals or,
   //more important, these predicates are or are becoming de facto standards

   //term creation and decomposition
        IsoSpec_predicate(numbervars(_, _, _)).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // table of ISO Prolog specified arithmetic functions
        %
   // (used for portability checking)
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //IsoSpecFunction(?callable)

        IsoSpecFunction(pi).

        IsoSpecFunction('+(_)).
        IsoSpecFunction('-(_)).
        IsoSpecFunction('+(_, _)).
        IsoSpecFunction('-(_, _)).
        IsoSpecFunction('*(_, _)).
        IsoSpecFunction('/(_, _)).
        IsoSpecFunction('//(_, _)).
        IsoSpecFunction(rem(_, _)).
        IsoSpecFunction(mod(_, _)).
        IsoSpecFunction(div(_, _)).

        IsoSpecFunction('/\\(_, _)).
        IsoSpecFunction('\\/(_, _)).
        IsoSpecFunction('\\(_)).
        IsoSpecFunction('<<(_, _)).
        IsoSpecFunction('>>(_, _)).
        IsoSpecFunction(xor(_, _)).

        IsoSpecFunction('**(_, _)).
        IsoSpecFunction('^(_, _)).

        IsoSpecFunction(abs(_)).
        IsoSpecFunction(sign(_)).

        IsoSpecFunction(sqrt(_)).

        IsoSpecFunction(acos(_)).
        IsoSpecFunction(asin(_)).
        IsoSpecFunction(atan(_)).
        IsoSpecFunction(atan2(_, _)).
        IsoSpecFunction(cos(_)).
        IsoSpecFunction(sin(_)).
        IsoSpecFunction(tan(_)).

        IsoSpecFunction(exp(_)).
        IsoSpecFunction(log(_)).

        IsoSpecFunction(float(_)).
        IsoSpecFunction(ceiling(_)).
        IsoSpecFunction(floor(_)).
        IsoSpecFunction(round(_)).
        IsoSpecFunction(truncate(_)).
        IsoSpecFunction(floatFractional_part(_)).
        IsoSpecFunction(floatInteger_part(_)).

        IsoSpecFunction(max(_, _)).
        IsoSpecFunction(min(_, _)).

   //the following functions are not part of the ISO/IEC 13211-1 Prolog standard
   //but can be found either on Core Revision standardization proposals or,
   //more important, these functions are or are becoming de facto standards

        IsoSpecFunction(e).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // table of ISO Prolog specified flags
        %
   // (used for portability checking)
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //IsoSpecFlag(?atom)

        IsoSpecFlag(bounded).
        IsoSpecFlag(maxInteger).
        IsoSpecFlag(minInteger).
        IsoSpecFlag(integerRoundingFunction).
        IsoSpecFlag(maxArity).
        IsoSpecFlag(charConversion).
        IsoSpecFlag(debug).
        IsoSpecFlag(double_quotes).
        IsoSpecFlag(unknown).

   //the following flags are not part of the ISO/IEC 13211-1 Prolog standard
   //but can be found either on the Core Revision standardization proposal or,
   //more important, these flags are becoming de facto standards

        IsoSpecFlag(dialect).
        IsoSpecFlag(versionData).



   //IsoSpecFlag_value(+atom, @nonvar)

        IsoSpecFlag_value(bounded, true) :- !.
        IsoSpecFlag_value(bounded, false) :- !.

        IsoSpecFlag_value(maxInteger, Value) :-
        integer(Value).

        IsoSpecFlag_value(minInteger, Value) :-
        integer(Value).

        IsoSpecFlag_value(integerRoundingFunction, toward_zero) :- !.
        IsoSpecFlag_value(integerRoundingFunction, down) :- !.

        IsoSpecFlag_value(maxArity, Value) :-
        integer(Value).

        IsoSpecFlag_value(charConversion, on) :- !.
        IsoSpecFlag_value(charConversion, off) :- !.

        IsoSpecFlag_value(debug, on) :- !.
        IsoSpecFlag_value(debug, off) :- !.

        IsoSpecFlag_value(double_quotes, atom) :- !.
        IsoSpecFlag_value(double_quotes, chars) :- !.
        IsoSpecFlag_value(double_quotes, codes) :- !.

        IsoSpecFlag_value(unknown, error) :- !.
        IsoSpecFlag_value(unknown, warning) :- !.
        IsoSpecFlag_value(unknown, fail) :- !.

        IsoSpecFlag_value(dialect, Value) :-
        atom(Value).

        IsoSpecFlag_value(versionData, Value) :-
        compound(Value).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // table of ISO Prolog specified built-in database predicates
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //IsoDatabase_predicate(@callble)

        IsoDatabase_predicate(abolish(_)).
        IsoDatabase_predicate(asserta(_)).
        IsoDatabase_predicate(assertz(_)).
        IsoDatabase_predicate(clause(_, _)).
        IsoDatabase_predicate(retract(_)).
        IsoDatabase_predicate(retractall(_)).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // Multi-threading support
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //Init_object_message_queue(+atom)
        %
   //creates a message queue for an object given its prefix
   //(assume that any exception generated is due to the fact that the message
   //queue already exists, which may happen when reloading threaded objects;
   //there is no standard predicate for testing message queue existence)

        Init_object_message_queue(ObjPrefix) :-
        catch(message_queueCreate(_, [alias(ObjPrefix)]), _, true).



   //threaded_waitSynchCtg(+mutexIdentifier, @term, @objectIdentifier)
        %
   //calls to the threaded_wait/1 predicate from synchronized category predicates

        threaded_waitSynchCtg(Mutex, Msg, This) :-
        Current_object_(This, Prefix, _, _, _, _, _, _, _, _, _),
        mutex_unlock(Mutex),
        threaded_wait(Msg, Prefix),
        mutexLock(Mutex).



   //threaded_waitSynch(+mutexIdentifier, @term, +entity_prefix)
        %
   //calls to the threaded_wait/1 predicate from synchronized object predicates

        threaded_waitSynch(Mutex, Msg, Prefix) :-
        mutex_unlock(Mutex),
        threaded_wait(Msg, Prefix),
        mutexLock(Mutex).



   //threaded_waitCtg(@term, @objectIdentifier)

        threaded_waitCtg(Msg, This) :-
        Current_object_(This, Prefix, _, _, _, _, _, _, _, _, _),
        threaded_wait(Msg, Prefix).



   //threaded_wait(@term, +entity_prefix)

        threaded_wait(Msg, Prefix) :-
        var(Msg),
        !,
        thread_get_message(Prefix, notification(Msg)).

        threaded_wait([], _) :-
        !.

        threaded_wait([Msg| Msgs], Prefix) :-
        !,
        thread_get_message(Prefix, notification(Msg)),
        threaded_wait(Msgs, Prefix).

        threaded_wait(Msg, Prefix) :-
        thread_get_message(Prefix, notification(Msg)).



   //threaded_notifyCtg(@term, @objectIdentifier)

        threaded_notifyCtg(Msg, This) :-
        Current_object_(This, Prefix, _, _, _, _, _, _, _, _, _),
        threaded_notify(Msg, Prefix).



   //threaded_notify(@term, +entity_prefix)

        threaded_notify(Msg, Prefix) :-
        var(Msg),
        !,
        threadSend_message(Prefix, notification(Msg)).

        threaded_notify([], _) :-
        !.

        threaded_notify([Msg| Msgs], Prefix) :-
        !,
        threadSend_message(Prefix, notification(Msg)),
        threaded_notify(Msgs, Prefix).

        threaded_notify(Msg, Prefix) :-
        threadSend_message(Prefix, notification(Msg)).



   //threadedIgnore(@term, @callable, @executionContext)
        %
   //the thread is only created if the original goal is callable;
   //this prevents programming errors going unnoticed

        threadedIgnore(Goal, TGoal, ExCtx) :-
        Check(qualifiedCallable, Goal, logtalk(threadedIgnore(Goal), ExCtx)),
        threadCreate(catch(TGoal, _, true), _, [detached(true)]).



   //threadedCall(@term, @callable, @executionContext)
        %
   //the thread is only created if the original goal is callable; this prevents
   //programming errors going unnoticed until we try to retrieve the first answer

        threadedCall(Goal, TGoal, ExCtx) :-
        Check(qualifiedCallable, Goal, logtalk(threadedCall(Goal), ExCtx)),
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        threadCreate(mt_nonDet_goal(Queue, Goal, TGoal, This, Self, []), Id, []),
        threadSend_message(Queue, threadId(call, Goal, This, Self, [], Id)).



   //threaded_once(@term, @callable, @executionContext)
        %
   //the thread is only created if the original goal is callable; this prevents
   //programming errors going unnoticed until we try to retrieve the first answer

        threaded_once(Goal, TGoal, ExCtx) :-
        Check(qualifiedCallable, Goal, logtalk(threaded_once(Goal), ExCtx)),
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        threadCreate(mtDet_goal(Queue, Goal, TGoal, This, Self, []), Id, []),
        threadSend_message(Queue, threadId(once, Goal, This, Self, [], Id)).



   //threadedCall_tagged(@term, @callable, @executionContext, -nonvar)
        %
   //the thread is only created if the original goal is callable; this prevents
   //programming errors going unnoticed until we try to retrieve the first answer

        threadedCall_tagged(Goal, TGoal, ExCtx, Tag) :-
        Check(qualifiedCallable, Goal, logtalk(threadedCall(Goal, Tag), ExCtx)),
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        new_threaded_tag(Tag),
        threadCreate(mt_nonDet_goal(Queue, Goal, TGoal, This, Self, Tag), Id, []),
        threadSend_message(Queue, threadId(call, Goal, This, Self, Tag, Id)).



   //threaded_once_tagged(@term, @callable, @executionContext, -nonvar)
        %
   //the thread is only created if the original goal is callable; this prevents
   //programming errors going unnoticed until we try to retrieve the first answer

        threaded_once_tagged(Goal, TGoal, ExCtx, Tag) :-
        Check(qualifiedCallable, Goal, logtalk(threaded_once(Goal, Tag), ExCtx)),
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        new_threaded_tag(Tag),
        threadCreate(mtDet_goal(Queue, Goal, TGoal, This, Self, Tag), Id, []),
        threadSend_message(Queue, threadId(once, Goal, This, Self, Tag, Id)).



   //mtDet_goal(+message_queueIdentifier, +callable, +callable, +objectIdentifier, +objectIdentifier, @nonvar)
        %
   //processes a deterministic message received by an object's message queue

        mtDet_goal(Queue, Goal, TGoal, This, Self, Tag) :-
        threadSelf(Id),
        (	catch(TGoal, Error, true) ->
        (	var(Error) ->
        threadSend_message(Queue, Reply(Goal, This, Self, Tag, success, Id))
        ;	threadSend_message(Queue, Reply(Goal, This, Self, Tag, Error, Id))
        )
        ;	threadSend_message(Queue, Reply(Goal, This, Self, Tag, failure, Id))
        ).



   //mt_nonDet_goal(+atom, +callable, +callable, +objectIdentifier, +objectIdentifier, @nonvar)
        %
   //processes a non-deterministic message received by an object's message queue

        mt_nonDet_goal(Queue, Goal, TGoal, This, Self, Tag) :-
        threadSelf(Id),
        (	catch(TGoal, Error, true),
        (	var(Error) ->
        threadSend_message(Queue, Reply(Goal, This, Self, Tag, success, Id)),
        thread_get_message(Message),
        (	Message == next' ->
   //backtrack to the catch(Goal, ...) to try to find an alternative solution
        fail
        ;	% otherwise assume Message = exit' and terminate thread
        true
        )
        ;	threadSend_message(Queue, Reply(Goal, This, Self, Tag, Error, Id))
        )
        ;	% no (more) solutions
        threadSend_message(Queue, Reply(Goal, This, Self, Tag, failure, Id))
        ).



   //threaded_peek(+callable, @executionContext)

        threaded_peek(Goal, ExCtx) :-
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        thread_peek_message(Queue, Reply(Goal, This, Self, [], _, _)).



   //threaded_peek_tagged(+callable, @executionContext, @nonvar)

        threaded_peek_tagged(Goal, ExCtx, Tag) :-
        Check(nonvar, Tag, logtalk(threaded_peek(Goal, Tag), ExCtx)),
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        thread_peek_message(Queue, Reply(Goal, This, Self, Tag, _, _)).



   //threaded_exit(+callable, @executionContext)

        threaded_exit(Goal, ExCtx) :-
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        (	% first check if there is a thread running for proving the goal before proceeding
        thread_peek_message(Queue, threadId(Type, Goal, This, Self, [], Id)) ->
   //answering thread exists; go ahead and retrieve the solution(s)
        thread_get_message(Queue, threadId(Type, Goal, This, Self, [], Id)),
        (	Type == (once) ->
        setupCallCleanup(
        true,
        mtDetReply(Queue, Goal, This, Self, [], Id),
        thread_join(Id, _)
        )
        ;	setupCallCleanup(
        true,
        mt_nonDetReply(Queue, Goal, This, Self, [], Id),
        ((	thread_property(Id, status(running)) ->
   //thread still running, suspended waiting for a request to an alternative proof; tell it to exit
        catch(threadSend_message(Id, exit'), _, true)
        ;	true
        ),
        thread_join(Id, _))
        )
        )
        ;	% answering thread don't exist; generate an exception (failing is not an option as it could simply mean goal failure)
        throw(error(existence_error(thread, This), logtalk(threaded_exit(Goal), ExCtx)))
        ).



   //threaded_exit_tagged(+callable, @executionContext, @nonvar)

        threaded_exit_tagged(Goal, ExCtx, Tag) :-
        Check(nonvar, Tag, logtalk(threaded_exit(Goal, Tag), ExCtx)),
        executionContext(ExCtx, _, _, This, Self, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        (	% first check if there is a thread running for proving the goal before proceeding
        thread_peek_message(Queue, threadId(Type, Goal, This, Self, Tag, Id)) ->
   //answering thread exists; go ahead and retrieve the solution(s)
        thread_get_message(Queue, threadId(Type, Goal, This, Self, Tag, Id)),
        (	Type == (once) ->
        setupCallCleanup(
        true,
        mtDetReply(Queue, Goal, This, Self, Tag, Id),
        thread_join(Id, _)
        )
        ;	setupCallCleanup(
        true,
        mt_nonDetReply(Queue, Goal, This, Self, Tag, Id),
        ((	thread_property(Id, status(running)) ->
   //thread still running, suspended waiting for a request to an alternative proof; tell it to exit
        catch(threadSend_message(Id, exit'), _, true)
        ;	true
        ),
        thread_join(Id, _))
        )
        )
        ;	% answering thread don't exist; generate an exception (failing is not an option as it could simply mean goal failure)
        throw(error(existence_error(thread, This), logtalk(threaded_exit(Goal, Tag), ExCtx)))
        ).



   //return the solution found

        mtDetReply(Queue, Goal, This, Self, Tag, Id) :-
        thread_get_message(Queue, Reply(Reply, This, Self, Tag, Result, Id)),
        (	Result == success ->
        Goal = Reply
        ;	Result == failure ->
        fail
        ;	throw(Result)
        ).



   //return current solution; on backtracking, ask working thread for and get from it the next solution

        mt_nonDetReply(Queue, Goal, This, Self, Tag, Id) :-
        thread_get_message(Queue, Reply(Reply, This, Self, Tag, Result, Id)),
        (	Result == success ->
        Goal = Reply
        ;	Result == failure ->
        !,
        fail
        ;	throw(Result)
        ).

        mt_nonDetReply(Queue, Goal, This, Self, Tag, Id) :-
        catch(threadSend_message(Id, next'), _, fail),
        mt_nonDetReply(Queue, Goal, This, Self, Tag, Id).



   //threaded_engineCreate(@term, @term, @callable, +objectIdentifier, ?nonvar)
        %
   //the engine thread is only created if the original goal is callable; this prevents
   //programming errors going unnoticed until we try to retrieve the first answer

        threaded_engineCreate(AnswerTemplate, Goal, TGoal, ExCtx, Engine) :-
        Check(qualifiedCallable, Goal, logtalk(threaded_engineCreate(AnswerTemplate, Goal, Engine), ExCtx)),
        with_mutex(
        engines',
        threaded_engineCreate_protected(AnswerTemplate, Goal, TGoal, ExCtx, Engine)
        ).


        threaded_engineCreate_protected(AnswerTemplate, Goal, TGoal, ExCtx, Engine) :-
        executionContext(ExCtx, _, _, This, _, _, _),
        (	var(Engine) ->
        new_threaded_tag(Engine)
        ;	Current_engine_(This, Engine, _, _) ->
        throw(error(permission_error(create, engine, Engine), logtalk(threaded_engineCreate(AnswerTemplate, Goal, Engine), ExCtx)))
        ;	true
        ),
        Current_object_(This, ThisQueue, _, _, _, _, _, _, _, _, _),
        message_queueCreate(TermQueue),
        threadCreate(mt_engine_goal(ThisQueue, TermQueue, AnswerTemplate, TGoal, Engine), Id, []),
        assertz(Current_engine_(This, Engine, TermQueue, Id)).


   //compute a solution for the engine goal and return it; note that the thread
   //always terminates with a status of "true" when an exception occurs or there
   //aren't any more solutions for the engine goal
        %
   //we use the object queue to store a engine_term_queue'/3 term with the
   //engine name and the engine term queue to workaround random timing issues when
   //accessing the Current_engine_'/4 dynamic predicate that can result in
   //unexpected errors

        mt_engine_goal(ThisQueue, TermQueue, Answer, Goal, Engine) :-
        threadSelf(Id),
        threadSend_message(ThisQueue, engine_term_queue(Engine, TermQueue, Id)),
        (	setupCallCleanup(true, catch(Goal, Error, true), Deterministic = true),
        (	var(Error) ->
        (	var(Deterministic) ->
        threadSend_message(ThisQueue, Answer(Engine, Id, Answer, success)),
        thread_get_message(Message),
   //if Message = next', backtrack to try to find an alternative solution
        Message == Aborted'
        ;	% no (more) solutions after this one
        threadSend_message(ThisQueue, Answer(Engine, Id, Answer, final))
        )
        ;	Error == Aborted' ->
   //we are destrying the engine
        true
        ;	% engine goal error
        threadSend_message(ThisQueue, Answer(Engine, Id, _, error(Error)))
        )
        ;	% no (more) solutions
        threadSend_message(ThisQueue, Answer(Engine, Id, _, failure))
        ).



   //Current_engine(@objectIdentifier, ?nonvar)
        %
   //we cannot compile threaded_engine/1 calls into Current_engine_'/2 calls
   //as the last two arguments would cause problems with bagof/3 and setof/3 calls

        Current_engine(This, Engine) :-
        Current_engine_(This, Engine, _, _).



   //threaded_engine_next(@nonvar, ?term, @executionContext)
        %
   //blocks until an answer (either an engine goal solution or a solution
   //posted by a call to threaded_engine_yield/1) becomes available

        threaded_engine_next(Engine, Answer, ExCtx) :-
        (	var(Engine) ->
        throw(error(instantiation_error, logtalk(threaded_engine_next(Engine, Answer), ExCtx)))
        ;	executionContext(ExCtx, _, _, This, _, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
   //first check if the engine is running
        Current_engine_(This, Engine, _, Id) ->
   //engine exists; go ahead and retrieve an answer
        mt_engineReply(Queue, Answer, Engine, Id, ExCtx)
        ;	% engine does not exist
        throw(error(existence_error(engine, Engine), logtalk(threaded_engine_next(Engine, Answer), ExCtx)))
        ).


   //return current answer and start computing the next one
   //if the engine goal succeeded non-deterministically
        %
   //after all solutions are consumed, or in case of error,
   //ensure that the all next calls will fail

        mt_engineReply(Queue, Answer, Engine, Id, ExCtx) :-
        thread_get_message(Queue, Answer(Engine, Id, Reply, Result)),
        (	Result == success ->
        threadSend_message(Id, next'),
        Answer = Reply
        ;	Result == final ->
        threadSend_message(Queue, Answer(Engine, Id, Done(final), failure)),
        Answer = Reply
        ;	Result == failure ->
        threadSend_message(Queue, Answer(Engine, Id, Done(failure), failure)),
        fail
        ;	Result = error(Error),
        threadSend_message(Queue, Answer(Engine, Id, Done(error), failure)),
        throw(error(Error, logtalk(threaded_engine_next(Engine,Answer),ExCtx)))
        ).



   //threaded_engine_nextReified(@nonvar, ?term, @executionContext)
        %
   //blocks until an answer (either an engine goal solution or a solution
   //posted by a call to threaded_engine_yield/1) becomes available

        threaded_engine_nextReified(Engine, Answer, ExCtx) :-
        (	var(Engine) ->
        throw(error(instantiation_error, logtalk(threaded_engine_nextReified(Engine, Answer), ExCtx)))
        ;	executionContext(ExCtx, _, _, This, _, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
   //first check if the engine is running
        Current_engine_(This, Engine, _, Id) ->
   //engine exists; go ahead and retrieve an answer
        mt_engineReplyReified(Queue, Answer, Engine, Id)
        ;	% engine does not exist
        throw(error(existence_error(engine, Engine), logtalk(threaded_engine_nextReified(Engine, Answer), ExCtx)))
        ).


   //return current answer and start computing the next one
   //if the engine goal succeeded non-deterministically
        %
   //after all solutions are consumed, or in case of error,
   //ensure that the all next calls will fail

        mt_engineReplyReified(Queue, Answer, Engine, Id) :-
        thread_get_message(Queue, Answer(Engine, Id, Reply, Result)),
        (	Result == success ->
        threadSend_message(Id, next'),
        Answer = the(Reply)
        ;	Result == final ->
        threadSend_message(Queue, Answer(Engine, Id, Done(final), failure)),
        Answer = the(Reply)
        ;	Result == failure ->
        threadSend_message(Queue, Answer(Engine, Id, Done(failure), failure)),
        Answer = no
        ;	Result = error(Error),
        threadSend_message(Queue, Answer(Engine, Id, Done(error), failure)),
        Answer = exception(Error)
        ).



   //threaded_engineSelf(@objectIdentifier, ?nonvar)
        %
   //fails if not called from within an engine

        threaded_engineSelf(This, Engine) :-
        threadSelf(Id),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        thread_peek_message(Queue, engine_term_queue(Engine0, _, Id)),
        !,
        Engine = Engine0.



   //threaded_engine_yield(@term, @objectIdentifier)
        %
   //fails if not called from within an engine;
   //blocks until the returned answer is consumed

        threaded_engine_yield(Answer, This) :-
        threadSelf(Id),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        thread_peek_message(Queue, engine_term_queue(Engine, _, Id)),
        threadSend_message(Queue, Answer(Engine, Id, Answer, success)),
        thread_get_message(_).



   //threaded_engine_post(@nonvar, @term, @executionContext)

        threaded_engine_post(Engine, Term, ExCtx) :-
        executionContext(ExCtx, _, _, This, _, _, _),
        (	var(Engine) ->
        throw(error(instantiation_error, logtalk(threaded_engine_post(Engine, Term), ExCtx)))
        ;	% first check if the engine is running
        Current_engine_(This, Engine, TermQueue, _) ->
   //engine exists; go ahead and post the message in its mailbox
        threadSend_message(TermQueue, Term)
        ;	% engine does not exist
        throw(error(existence_error(engine, Engine), logtalk(threaded_engine_post(Engine, Term), ExCtx)))
        ).



   //threaded_engineFetch(?term, @objectIdentifier)
        %
   //fails if not called from within an engine or if we are
   //destroying a running engine

        threaded_engineFetch(Term, This) :-
        threadSelf(Id),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        (	% check if calling from within an engine
        thread_peek_message(Queue, engine_term_queue(_, TermQueue, Id)) ->
   //engine exists; go ahead and retrieve a message from its mailbox
        thread_get_message(TermQueue, Term),
        Term \== Aborted'
        ;	% engine does not exist
        fail
        ).



   //threaded_engineDestroy(@nonvar, @executionContext)
        %
   //when the engine thread is still running, we first put a throw/1 goal in the
   //thread signal queue and then send messages to both the thread queue and the
   //engine term queue to resume the engine goal if suspended waiting for either
   //a request for the next solution or a term to be processed

        threaded_engineDestroy(Engine, ExCtx) :-
        with_mutex(
        engines',
        threaded_engineDestroy_protected(Engine, ExCtx)
        ).


        threaded_engineDestroy_protected(Engine, ExCtx) :-
        (	var(Engine) ->
        throw(error(instantiation_error, logtalk(threaded_engineDestroy(Engine), ExCtx)))
        ;	executionContext(ExCtx, _, _, This, _, _, _),
        Current_object_(This, Queue, _, _, _, _, _, _, _, _, _),
        retract(Current_engine_(This, Engine, TermQueue, Id)) ->
        (	thread_property(Id, status(running)) ->
   //protect the call to threadSignal/2 as the thread may terminate
   //between checking its status and this call
        catch(threadSignal(Id, throw(Aborted')), _, true),
   //send a term to the engine term queue first as this queue is explicitly
   //created and destroyed and thus we can be sure it exists
        threadSend_message(TermQueue, Aborted'),
   //on the other hand, the engine thread and therefore its queue may no longer exist
        catch(threadSend_message(Id, Aborted'), _, true)
        ;	true
        ),
        thread_join(Id, _),
        message_queueDestroy(TermQueue),
   //remove any non-consumed answer
        (	thread_peek_message(Queue, Answer(Engine, Id, _, _)) ->
        thread_get_message(Queue, Answer(Engine, Id, _, _))
        ;	true
        ),
   //remove the answer that ensures threaded_engine_next/2 and threaded_engine_nextReified/2
   //return failures after consuming all solutions if present
        (	thread_peek_message(Queue, Answer(Engine, Id, _, failure)) ->
        thread_get_message(Queue, Answer(Engine, Id, _, failure))
        ;	true
        ),
   //remove the cache entry for the engine term queue handle
        (	thread_peek_message(Queue, engine_term_queue(Engine, _, Id)) ->
        thread_get_message(Queue, engine_term_queue(Engine, _, Id))
        ;	true
        )
        ;	% engine doesn't exist
        throw(error(existence_error(engine, Engine), logtalk(threaded_engineDestroy(Engine), ExCtx)))
        ).



   //threaded_or(-var, +callable, +list)
        %
   //implements the threaded/1 built-in predicate when the argument is a disjunction

        threaded_or(Queue, MTGoals, Results) :-
        threadSelf(Queue),
        catch((MTGoals, mt_threaded_or_exit(Results)), terminated', fail).



   //threadedAnd(-var, +callable, +list)
        %
   //implements the threaded/1 built-in predicate when the argument is a conjunction

        threadedAnd(Queue, MTGoals, Results) :-
        threadSelf(Queue),
        catch((MTGoals, mt_threadedAnd_exit(Results)), terminated', fail).



   //threaded_goal(+callable, -list(var), +message_queueIdentifier, -threadIdentifier)
        %
   //implements the call to an individual goal in the threaded/1 built-in predicate

        threaded_goal(TGoal, TVars, Queue, Id) :-
        term_variables(TGoal, TVars),
        threadCreate(mt_threadedCall(TGoal, TVars, Queue), Id, [at_exit(mt_exit_handler(Id, Queue))]).



   //mt_threadedCall(+callable, +list(var), +message_queueIdentifier)
        %
   //proves an individual goal from a threaded/1 predicate call and
   //sends the result back to the message queue associated to the call

        mt_threadedCall(TGoal, TVars, Queue) :-
        threadSelf(Id),
        (	call(TGoal) ->
        threadSend_message(Queue, Result(Id, true(TVars)))
        ;	threadSend_message(Queue, Result(Id, false))
        ).



   //mt_exit_handler(@nonvar, +message_queueIdentifier)
        %
   //error handler for threaded/1 individual thread calls; an error generated
   //by the threadSend_message/2 call is interpreted as meaning that the
   //master/parent thread queue no longer exists leading to the detaching of
   //the worker thread

        mt_exit_handler(Id, Queue) :-
        (	thread_property(Id, status(exception(Error))) ->
        catch(threadSend_message(Queue, Result(Id, exception(Error))), _, threadDetach(Id))
        ;	true
        ).



   //mt_threadedAnd_exit(+list)
        %
   //retrieves the result of proving a conjunction of goals using a threaded/1 predicate call
   //by collecting the individual thread results posted to the master thread message queue

        mt_threadedAnd_exit(Results) :-
        thread_get_message(Result(Id, Result)),
        mt_threadedAnd_exit(Result, Id, Results).


        mt_threadedAnd_exit(exception(Error), Id, Results) :-
        mt_threadedRecordResult(Results, Id, exception(Error)),
        (	Error == terminated' ->
   //messages can arrive out-of-order; if that's the case we need to keep looking
   //for the thread result that lead to the termination of the other threads
        mt_threadedAnd_exit(Results)
        ;	Error == Aborted' ->
        mt_threadedCallCancel(Results),
        throw(terminated')
        ;	mt_threadedCallCancel(Results),
        throw(Error)
        ).

        mt_threadedAnd_exit(true(TVars), Id, Results) :-
        (	mt_threadedAndAddResult(Results, Id, TVars, Continue) ->
        (	Continue == false ->
        mt_threadedCall_join(Results)
        ;	mt_threadedAnd_exit(Results)
        )
        ;	% adding a successful result can fail if the individual thread goals
   //are not independent (i.e. they share variables with the same or
   //partially the same role leading to unification failures)
        mt_threadedAnd_exit(false, Id, Results)
        ).

        mt_threadedAnd_exit(false, Id, Results) :-
        mt_threadedRecordResult(Results, Id, false),
        mt_threadedCallCancel(Results),
        fail.



   //mt_threadedAndAddResult(+list, +threadIdentifier, @callable, -atom)
        %
   //adds the result of proving a goal and checks if all other goals have succeeded

        mt_threadedAndAddResult([id(Id, TVars, true)| Results], Id, TVars, Continue) :-
        !,
        (	var(Continue) ->
   //we still don't know if there are any pending results
        mt_threadedContinue(Results, Continue)
        ;	true
        ).

        mt_threadedAndAddResult([id(_, _, Done)| Results], Id, TVars, Continue) :-
        (	var(Done) ->
   //we found a thread whose result is still pending
        Continue = true
        ;	% otherwise continue examining the remaining thread results
        true
        ),
        mt_threadedAndAddResult(Results, Id, TVars, Continue).



   //mt_threaded_or_exit(+message_queueIdentifier, +list)
        %
   //retrieves the result of proving a disjunction of goals using a threaded/1 predicate
   //call by collecting the individual thread results posted to the call message queue

        mt_threaded_or_exit(Results) :-
        thread_get_message(Result(Id, Result)),
        mt_threaded_or_exit(Result, Id, Results).


        mt_threaded_or_exit(exception(Error), Id, Results) :-
        mt_threadedRecordResult(Results, Id, exception(Error)),
        (	Error == terminated' ->
   //messages can arrive out-of-order; if that's the case we need to keep looking
   //for the thread result that lead to the termination of the other threads
        mt_threaded_or_exit(Results)
        ;	Error == Aborted' ->
        mt_threadedCallCancel(Results),
        throw(terminated')
        ;	mt_threadedCallCancel(Results),
        throw(Error)
        ).

        mt_threaded_or_exit(true(TVars), Id, Results) :-
        mt_threaded_or_exit_unify(Results, Id, TVars),
        mt_threadedCallCancel(Results).

        mt_threaded_or_exit(false, Id, Results) :-
        mt_threaded_orRecordFailure(Results, Id, Continue),
        (	Continue == true ->
        mt_threaded_or_exit(Results)
        ;	% all goals failed
        mt_threadedCall_join(Results),
        fail
        ).



   //unifies the successful thread goal result with the original call

        mt_threaded_or_exit_unify([id(Id, TVars, true)| _], Id, TVars) :-
        !.

        mt_threaded_or_exit_unify([_| Results], Id, TVars) :-
        mt_threaded_or_exit_unify(Results, Id, TVars).



   //mt_threaded_orRecordFailure(+list, +threadIdentifier, -atom)
        %
   //records a thread goal failure and checks if all other thread goals have failed

        mt_threaded_orRecordFailure([id(Id, _, false)| Results], Id, Continue) :-
        !,
        (	var(Continue) ->
   //we still don't know if there are any pending results
        mt_threadedContinue(Results, Continue)
        ;	true
        ).

        mt_threaded_orRecordFailure([id(_, _, Done)| Results], Id, Continue) :-
        (	var(Done) ->
   //we found a thread whose result is still pending
        Continue = true
        ;	% otherwise continue examining the remaining thread results
        true
        ),
        mt_threaded_orRecordFailure(Results, Id, Continue).



   //mt_threadedContinue(+list, -atom)
        %
   //checks if there are results still pending for a threaded/1 call

        mt_threadedContinue([], false).

        mt_threadedContinue([id(_, _, Done)| Results], Continue) :-
        (	var(Done) ->
   //we found a thread whose result is still pending
        Continue = true
        ;	% otherwise continue looking for a thread with a still pending result
        mt_threadedContinue(Results, Continue)
        ).



   //mt_threadedRecordResult(+list, +threadIdentifier, +callable)
        %
   //records a thread goal result

        mt_threadedRecordResult([id(Id, _, Result)| _], Id, Result) :-
        !.

        mt_threadedRecordResult([_| Results], Id, Result) :-
        mt_threadedRecordResult(Results, Id, Result).



   //mt_threadedCallCancel(+list)
        %
   //aborts a threaded call by aborting and joining all individual threads;
   //we must use catch/3 as some threads may already be terminated

        mt_threadedCallCancel(Results) :-
        mt_threadedCallAbort(Results),
        mt_threadedCall_join(Results).



   //mt_threadedCallAbort(+list)
        %
   //signals all individual threads to abort; we must use catch/3 as some threads may no longer exist

        mt_threadedCallAbort([]).

        mt_threadedCallAbort([id(Id, _, _)| Ids]) :-
        catch(threadSignal(Id, throw(Aborted')), _, true),
        mt_threadedCallAbort(Ids).



   //mt_threadedCall_join(+list)
        %
   //joins all individual threads; we must use catch/3 as some threads may no longer exist

        mt_threadedCall_join([]).

        mt_threadedCall_join([id(Id, _, Result)| Results]) :-
        (	var(Result) ->
   //don't leak thread results as threads may reuse identifiers
        thread_get_message(Result(Id, _))
        ;	true
        ),
        catch(thread_join(Id, _), _, true),
        mt_threadedCall_join(Results).



   //new_threaded_tag(-integer)
        %
   //generates a new multi-threading tag; used in the built-in asynchronous
   //multi-threading predicates

        new_threaded_tag(New) :-
        with_mutex(
        threaded_tag',
        (	retract(threaded_tagCounter_(Old)),
        New is Old + 1,
        asserta(threaded_tagCounter_(New))
        )
        ).



   //Create_mutexes(+list(mutexIdentifier))
        %
   //creates entity mutexes (called when loading an entity); we may
   //be reloading an entity and the mutex may be already created

        Create_mutexes([]).

        Create_mutexes([Mutex| Mutexes]) :-
        (	mutex_property(_, alias(Mutex)) ->
        true
        ;	mutexCreate(_, [alias(Mutex)])
        ),
        Create_mutexes(Mutexes).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // static binding supporting predicates
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //Send_to_objStatic_binding(@objectIdentifier, @callable, @objectIdentifier, -callable)
        %
   //static binding is only used for the (::)/2 control construct when the object receiving the
   //message is static and the support for complementing categories is disallowed (unfortunately,
   //allowing hot patching of an object would easily lead to inconsistencies as there isn't any
   //portable solution for updating in-place the definition of patched object predicates that
   //were already directly called due to the previous use of static binding)

        Send_to_objStatic_binding(Obj, Pred, Call, Ctx) :-
        CompCtx(Ctx, _, _, _, _, This, _, _, _, _, CallerExCtx, _, _, _),
        (	Send_to_objStatic_binding_(Obj, Pred, CallerExCtx, Call) ->
        true
        ;	Current_object_(Obj, _, Dcl, Def, _, _, _, _, _, _, ObjFlags),
        ObjFlags /\ 512 =\= 512,
   //object is not compiled in debug mode
        ObjFlags /\ 2 =:= 0,
   //object is static
        ObjFlags /\ 64 =\= 64,
   //complementing categories flag not set to "allow"
        term_template(Pred, GPred),
        call(Dcl, GPred, p(p(p)), Meta, PredFlags, _, DclCtn), !,
   //construct list of the meta-arguments that will be called in the "sender"
        goal_metaCallContext(Meta, GPred, GCallerExCtx, GThis, GMetaCallCtx),
        term_template(Obj, GObj),
        executionContext(GExCtx, _, GThis, GObj, GObj, GMetaCallCtx, []),
        call(Def, GPred, GExCtx, GCall, _, DefCtn), !,
        (	PredFlags /\ 2 =:= 0 ->
   //Type == static
        true
        ;	% Type == (dynamic)
        GObj = DclCtn ->
   //local declaration
        true
        ;	GObj = DefCtn
   //local definition
        ),
        (	GObj \= DefCtn ->
   //inherited definition; complementing categories
   //flag must also not be set to "restrict"
        ObjFlags /\ 32 =\= 32
        ;	% local definition
        true
        ),
   //predicate definition found; use it only if it's safe
        Static_bindingSafe_paths(GObj, DclCtn, DefCtn),
        (	Meta == no ->
   //cache only normal predicates
        assertz(Send_to_objStatic_binding_(GObj, GPred, GCallerExCtx, GCall)),
        Obj = GObj, Pred = GPred, This = GThis, CallerExCtx = GCallerExCtx, Call = GCall
        ;	% meta-predicates cannot be cached as they require translation of
   //the meta-arguments, which must succeed to allow static binding
   //(don't require the predicate and the meta-predicate template to
   //share the name as we may be using a predicate alias)
        Meta =.. [_| MArgs],
        Pred =.. [PredFunctor| Args],
        CompileStatic_binding_metaArguments(Args, MArgs, Ctx, TArgs),
        TPred =.. [PredFunctor| TArgs],
        Obj = GObj, TPred = GPred, This = GThis, CallerExCtx = GCallerExCtx, Call = GCall
        )
        ).


        CompileStatic_binding_metaArguments([], [], _, []).

        CompileStatic_binding_metaArguments([Arg| Args], [MArg| MArgs], Ctx, [TArg| TArgs]) :-
        CompileStatic_binding_metaArgument(MArg, Arg, Ctx, TArg),
        CompileStatic_binding_metaArguments(Args, MArgs, Ctx, TArgs).


        CompileStatic_binding_metaArgument((*), Arg, _, Arg) :-
        !.

        CompileStatic_binding_metaArgument(N, Closure, Ctx, {UserClosure}) :-
        integer(N),
   //goal or closure
        nonvar(Closure),
        (	Closure = Obj::UserClosure, Obj == user
        ;	Closure = {UserClosure}
        ;	CompCtx_entity(Ctx, Entity), Entity == user,
        \+ ControlConstruct(Closure),
        UserClosure = Closure
        ),
   //goal or closure called in "user"
        !,
        Check(var_orCallable, UserClosure).

        CompileStatic_binding_metaArgument(N, Closure, Ctx, TClosure) :-
        integer(N), N > 0,
   //closure
        !,
        Check(var_orCallable, Closure),
        Length(ExtArgs, 0, N),
        extendClosure(Closure, ExtArgs, Goal),
   //compiling the meta-argument allows predicate cross-referencing information
   //to be collected even if the compilation result cannot be used
        Compile_body(Goal, TGoal, _, Ctx),
        functor(TGoal, TFunctor, _),
        (	Goal == TGoal ->
        \+ ControlConstruct(TGoal),
   //either a built-in predicate or a predicate called in "user"
        TClosure = {Closure}
        ;	subAtom(TFunctor, 0, 5, _, ') ->
   //in some backend Prolog systems, internal Logtalk compiler/runtime
   //predicates may be marked as built-in predicates
        fail
        ;	builtIn_predicate(TGoal) ->
        \+ ControlConstruct(TGoal),
   //built-in predicates may result from goal-expansion during
   //compilation or from inlining of user predicate definitions
        builtIn_goal_toClosure(N, TGoal, TFunctor, TArgs),
        TClosure0 =.. [TFunctor| TArgs],
        TClosure = {TClosure0}
        ;	user_goal_toClosure(N, TGoal, TFunctor, TArgs, ExCtx) ->
        TClosure = Closure(TFunctor, TArgs, ExCtx)
        ;	% runtime resolved meta-call
        fail
        ).

        CompileStatic_binding_metaArgument(0, Goal, Ctx, {TGoal}) :-
   //the {}/1 construct signals a pre-compiled metacall
        Compile_body(Goal, TGoal, _, Ctx).


        builtIn_goal_toClosure(N, TGoal, TFunctor, TArgs) :-
        functor(TGoal, TFunctor, TArity),
        TGoal =.. [TFunctor| TAllArgs],
   //subtract the number of extra arguments
        Arity is TArity - N,
        TArity >= 0,
   //unify the compiled closure arguments from the compiled goal arguments
        Length(TArgs, 0, Arity),
        Append(TArgs, _, TAllArgs),
        !.


        user_goal_toClosure(N, TGoal, TFunctor, TArgs, ExCtx) :-
        functor(TGoal, TFunctor, TArity),
        TGoal =.. [TFunctor| TAllArgs],
   //subtract the number of extra arguments and the execution context argument
        Arity is TArity - N - 1,
        Arity >= 0,
   //unify the compiled closure arguments from the compiled goal arguments
        Length(TArgs, 0, Arity),
        Append(TArgs, _, TAllArgs),
   //unify the execution context argument using the compiled goal
        arg(TArity, TGoal, ExCtx),
        !.



   //objSuperCallStatic_binding(@objectIdentifier, @callable, @executionContext, -callable)
        %
   //static binding for the (^^)/1 control construct (used within objects)

        objSuperCallStatic_binding(Obj, Pred, ExCtx, Call) :-
        (	ppImportedCategory_(_, _, _, _, _, _),
        objSuperCallStatic_bindingCategory(Obj, Pred, ExCtx, Call) ->
        true
        ;	pp_extended_object_(_, _, _, _, _, _, _, _, _, _, _) ->
        objSuperCallStatic_binding_prototype(Obj, Pred, ExCtx, Call)
        ;	ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _),
        ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        objSuperCallStatic_bindingInstanceClass(Obj, Pred, ExCtx, Call)
        ;	ppInstantiatedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        objSuperCallStatic_bindingInstance(Obj, Pred, ExCtx, Call)
        ;	ppSpecializedClass_(_, _, _, _, _, _, _, _, _, _, _) ->
        objSuperCallStatic_bindingClass(Obj, Pred, ExCtx, Call)
        ;	fail
        ).


        objSuperCallStatic_bindingCategory(Obj, Alias, OExCtx, Call) :-
   //when working with parametric entities, we must connect the parameters
   //between related entities
        ppRuntimeClause_(ImportsCategory_(Obj, Ctg, _)),
        CurrentCategory_(Ctg, _, Dcl, Def, _, _),
   //we may be aliasing the predicate
        (	pp_predicateAlias_(Ctg, Pred, Alias, _, _, _) ->
        true
        ;	Pred = Alias
        ),
   //lookup predicate declaration
        call(Dcl, Pred, _, _, Flags, DclCtn), !,
   //the predicate must be static
        Flags /\ 2 =:= 0,
   //unify execution context arguments
        executionContext_update_this_entity(OExCtx, Obj, Obj, CExCtx, Obj, Ctg),
   //lookup predicate definition
        call(Def, Pred, CExCtx, Call, DefCtn), !,
   //predicate definition found; use it only if it's safe
        Static_bindingSafe_paths(Obj, DclCtn, DefCtn).


        objSuperCallStatic_binding_prototype(Obj, Alias, OExCtx, Call) :-
   //when working with parametric entities, we must connect the parameters
   //between related entities
        ppRuntimeClause_(extends_object_(Obj, Parent, RelationScope)),
        Current_object_(Parent, _, Dcl, Def, _, _, _, _, _, _, _),
   //we may be aliasing the predicate
        (	pp_predicateAlias_(Parent, Pred, Alias, _, _, _) ->
        true
        ;	Pred = Alias
        ),
   //lookup predicate declaration
        (	RelationScope == (public) ->
        call(Dcl, Pred, Scope, _, Flags, SCtn, TCtn)
        ;	RelationScope == protected ->
        call(Dcl, Pred, PredScope, _, Flags, SCtn, TCtn),
        FilterScope(PredScope, Scope)
        ;	Scope = p,
        call(Dcl, Pred, PredScope, _, Flags, SCtn0, TCtn),
        FilterScopeContainer(PredScope, SCtn0, Obj, SCtn)
        ), !,
   //check that the call is within scope (i.e. public or protected)
        (	Scope = p(_) ->
        true
        ;	Obj = SCtn
        ),
   //the predicate must be static
        Flags /\ 2 =:= 0,
   //unify execution context arguments
        executionContext_update_this_entity(OExCtx, Obj, Obj, PExCtx, Parent, Parent),
   //lookup predicate definition
        call(Def, Pred, PExCtx, Call, _, DefCtn), !,
   //predicate definition found; use it only if it's safe
        Static_bindingSafe_paths(Obj, TCtn, DefCtn).


        objSuperCallStatic_bindingInstance(Obj, Alias, OExCtx, Call) :-
   //when working with parametric entities, we must connect the parameters
   //between related entities
        ppRuntimeClause_(InstantiatesClass_(Obj, Class, RelationScope)),
        Current_object_(Class, _, _, _, _, IDcl, IDef, _, _, _, _),
   //we may be aliasing the predicate
        (	pp_predicateAlias_(Class, Pred, Alias, _, _, _) ->
        true
        ;	Pred = Alias
        ),
   //lookup predicate declaration
        (	RelationScope == (public) ->
        call(IDcl, Pred, Scope, _, Flags, SCtn, TCtn)
        ;	RelationScope == protected ->
        call(IDcl, Pred, PredScope, _, Flags, SCtn, TCtn),
        FilterScope(PredScope, Scope)
        ;	Scope = p,
        call(IDcl, Pred, PredScope, _, Flags, SCtn0, TCtn),
        FilterScopeContainer(PredScope, SCtn0, Obj, SCtn)
        ), !,
   //check that the call is within scope (i.e. public or protected)
        (	Scope = p(_) ->
        true
        ;	Obj = SCtn
        ),
   //the predicate must be static
        Flags /\ 2 =:= 0,
   //unify execution context arguments
        executionContext_update_this_entity(OExCtx, Obj, Obj, CExCtx, Class, Class),
   //lookup predicate definition
        call(IDef, Pred, CExCtx, Call, _, DefCtn), !,
   //predicate definition found; use it only if it's safe
        Static_bindingSafe_paths(Obj, TCtn, DefCtn).


        objSuperCallStatic_bindingClass(Obj, Alias, OExCtx, Call) :-
   //when working with parametric entities, we must connect the parameters
   //between related entities
        ppRuntimeClause_(SpecializesClass_(Obj, Superclass, RelationScope)),
        Current_object_(Superclass, _, _, _, _, IDcl, IDef, _, _, _, _),
   //we may be aliasing the predicate
        (	pp_predicateAlias_(Superclass, Pred, Alias, _, _, _) ->
        true
        ;	Pred = Alias
        ),
   //lookup predicate declaration
        (	RelationScope == (public) ->
        call(IDcl, Pred, Scope, _, Flags, SCtn, TCtn)
        ;	RelationScope == protected ->
        call(IDcl, Pred, PredScope, _, Flags, SCtn, TCtn),
        FilterScope(PredScope, Scope)
        ;	Scope = p,
        call(IDcl, Pred, PredScope, _, Flags, SCtn0, TCtn),
        FilterScopeContainer(PredScope, SCtn0, Obj, SCtn)
        ), !,
   //check that the call is within scope (i.e. public or protected)
        (	Scope = p(_) ->
        true
        ;	Obj = SCtn
        ),
   //the predicate must be static
        Flags /\ 2 =:= 0,
   //unify execution context arguments
        executionContext_update_this_entity(OExCtx, Obj, Obj, SExCtx, Superclass, Superclass),
   //lookup predicate definition
        call(IDef, Pred, SExCtx, Call, _, DefCtn), !,
   //predicate definition found; use it only if it's safe
        Static_bindingSafe_paths(Obj, TCtn, DefCtn).


        objSuperCallStatic_bindingInstanceClass(Obj, Pred, ExCtx, Call) :-
        (	objSuperCallStatic_bindingInstance(Obj, Pred, ExCtx, ICall),
        objSuperCallStatic_bindingClass(Obj, Pred, ExCtx, CCall) ->
        (	ICall == CCall ->
        Call = ICall
        ;	executionContext(ExCtx, _, _, _, Self, _, _),
        Call = (Obj = Self -> ICall; CCall)
        )
        ;	objSuperCallStatic_bindingInstance(Obj, Pred, ExCtx, Call) ->
        true
        ;	objSuperCallStatic_bindingClass(Obj, Pred, ExCtx, Call)
        ).



   //CtgSuperCallStatic_binding(@categoryIdentifier, @callable, @executionContext, -callable)
        %
   //static binding for the (^^)/1 control construct (used within categories)

        CtgSuperCallStatic_binding(Ctg, Alias, CExCtx, Call) :-
   //when working with parametric entities, we must connect the parameters
   //between related entities
        ppRuntimeClause_(extendsCategory_(Ctg, ExtCtg, RelationScope)),
        CurrentCategory_(ExtCtg, _, Dcl, Def, _, _),
   //we may be aliasing the predicate
        (	pp_predicateAlias_(ExtCtg, Pred, Alias, _, _, _) ->
        true
        ;	Pred = Alias
        ),
   //lookup predicate declaration
        (	RelationScope == (public) ->
        call(Dcl, Pred, Scope, _, Flags, DclCtn)
        ;	RelationScope == protected,
        call(Dcl, Pred, Scope0, _, Flags, DclCtn),
        FilterScope(Scope0, Scope)
        ), !,
   //check that the call is within scope
        Scope = p(_),
   //the predicate must be static
        Flags /\ 2 =:= 0,
   //unify execution context arguments
        executionContext_update_this_entity(CExCtx, This, Ctg, EExCtx, This, ExtCtg),
   //lookup predicate definition
        call(Def, Pred, EExCtx, Call, DefCtn), !,
   //predicate definition found; use it only if it's safe
        Static_bindingSafe_paths(Ctg, DclCtn, DefCtn).



   //Send_to_objDb_msgStatic_binding(@categoryIdentifier, @callable, -callable)
        %
   //static binding for selected database messages sent to an object

        Send_to_objDb_msgStatic_binding(Obj, Head, THead) :-
        Current_object_(Obj, _, Dcl, Def, _, _, _, _, _, _, ObjFlags),
   //check that the object is not compiled in debug mode
        ObjFlags /\ 512 =\= 512,
   //check that the object is static
        ObjFlags /\ 2 =:= 0,
        call(Dcl, Head, Scope, _, PredFlags, SCtn, DCtn), !,
   //check that the call is within scope
        Scope = p(p(_)),
   //check that the predicate is dynamic
        PredFlags /\ 2 =:= 2,
   //check that we're acting on the same entity that declares the predicate dynamic
        SCtn = Obj,
   //lookup local predicate definition
        call(Def, Head, _, THead), !,
   //predicate definition found; use it only if it's safe
        Static_binding_entity(DCtn).



   //Static_bindingSafe_paths(@entityIdentifier, @entityIdentifier, @entityIdentifier)
        %
   //all ancestor entities up to the starting point for both the declaration
   //container and the definition container must be static-binding entities

        Static_bindingSafe_paths(Entity, DclEntity, DefEntity) :-
        (	DclEntity \= Entity ->
        Static_binding_entity(DclEntity)
        ;	true
        ),
        (	DefEntity \= Entity ->
        Static_binding_entity(DefEntity)
        ;	true
        ),
        Static_bindingSafeDeclarationAncestors(Entity, DclEntity),
        Static_bindingSafeDefinitionAncestors(Entity, DefEntity).


        Static_binding_entity(Entity) :-
        (	Current_object_(Entity, _, _, _, _, _, _, _, _, _, Flags) ->
        Flags /\ 64 =\= 64,
        Flags /\ 32 =\= 32
   //support for complementing categories is disabled
        ;	Current_protocol_(Entity, _, _, _, Flags) ->
        true
        ;	CurrentCategory_(Entity, _, _, _, _, Flags)
        ),
        Flags /\ 512 =\= 512,
   //entity is not compiled in debug mode
        Flags /\ 2 =:= 0.
   //entity is static


        Static_binding_entity(object, Object) :-
        Current_object_(Object, _, _, _, _, _, _, _, _, _, Flags),
        Flags /\ 512 =\= 512,
   //object is not compiled in debug mode
        Flags /\ 2 =:= 0,
   //object is static
        Flags /\ 64 =\= 64,
        Flags /\ 32 =\= 32.
   //support for complementing categories is disallowed

        Static_binding_entity(protocol, Protocol) :-
        Current_protocol_(Protocol, _, _, _, Flags),
        Flags /\ 512 =\= 512,
   //protocol is not compiled in debug mode
        Flags /\ 2 =:= 0.
   //protocol is static

        Static_binding_entity(category, Category) :-
        CurrentCategory_(Category, _, _, _, _, Flags),
        Flags /\ 512 =\= 512,
   //category is not compiled in debug mode
        Flags /\ 2 =:= 0.
   //category is static


        Static_bindingSafeDeclarationAncestors(Entity, DclEntity) :-
        (	Entity = DclEntity ->
   //local predicate declaration
        true
        ;	% we add a third argument to properly handle class hierarchies if necessary
        Static_bindingSafeDeclarationAncestors(Entity, DclEntity, _)
        ).

        Static_bindingSafeDeclarationAncestors(Entity, DclEntity, Kind) :-
        entityAncestor(Entity, Type, Ancestor, Kind, NextKind),
        (	Static_binding_entity(Type, Ancestor) ->
        (	Ancestor = DclEntity ->
        true
        ;	% move up, implementing the same depth-first strategy used by the predicate
   //declaration lookup algorithm
        Static_bindingSafeDeclarationAncestors(Ancestor, DclEntity, NextKind)
        )
        ;	% ancestor can be later modified, rendering the static binding optimization invalid
        !,
        fail
        ).


        Static_bindingSafeDefinitionAncestors(Entity, DefEntity) :-
        (	Entity = DefEntity ->
   //local predicate definition
        true
        ;	% we add a third argument to properly handle class hierarchies if necessary
        Static_bindingSafeDefinitionAncestors(Entity, DefEntity, _)
        ).

        Static_bindingSafeDefinitionAncestors(Entity, DefEntity, Kind) :-
        entityAncestor(Entity, Type, Ancestor, Kind, NextKind),
   //protocols cannot contain predicate definitions
        Type \== protocol,
        (	Static_binding_entity(Type, Ancestor) ->
        (	Ancestor = DefEntity ->
        true
        ;	% move up, implementing the same depth-first strategy used by the predicate
   //definition lookup algorithm
        Static_bindingSafeDefinitionAncestors(Ancestor, DefEntity, NextKind)
        )
        ;	% ancestor can be later modified, rendering the static binding optimization invalid
        !,
        fail
        ).


   //entity ancestors are generated on backtracking in the same order
   //used by the predicate declaration and definition lookup algorithms

        entityAncestor(Entity, protocol, Protocol, Kind, Kind) :-
        Implements_protocol_(Entity, Protocol, _).

        entityAncestor(Entity, protocol, Protocol, Kind, Kind) :-
        ppRuntimeClause_(Implements_protocol_(Entity, Protocol, _)).

        entityAncestor(Entity, protocol, Protocol, protocol, protocol) :-
        extends_protocol_(Entity, Protocol, _).

        entityAncestor(Entity, protocol, Protocol, protocol, protocol) :-
        ppRuntimeClause_(extends_protocol_(Entity, Protocol, _)).

        entityAncestor(Entity, category, Category, category, category) :-
        extendsCategory_(Entity, Category, _).

        entityAncestor(Entity, category, Category, category, category) :-
        ppRuntimeClause_(extendsCategory_(Entity, Category, _)).

        entityAncestor(Entity, category, Category, Kind, Kind) :-
        ImportsCategory_(Entity, Category, _).

        entityAncestor(Entity, category, Category, Kind, Kind) :-
        ppRuntimeClause_(ImportsCategory_(Entity, Category, _)).

        entityAncestor(Entity, object, Parent, prototype, prototype) :-
        extends_object_(Entity, Parent, _).

        entityAncestor(Entity, object, Parent, prototype, prototype) :-
        ppRuntimeClause_(extends_object_(Entity, Parent, _)).

        entityAncestor(Entity, object, Class, instance, superclass) :-
        InstantiatesClass_(Entity, Class, _).

        entityAncestor(Entity, object, Class, instance, superclass) :-
        ppRuntimeClause_(InstantiatesClass_(Entity, Class, _)).

        entityAncestor(Entity, object, Superclass, superclass, superclass) :-
        SpecializesClass_(Entity, Superclass, _).

        entityAncestor(Entity, object, Superclass, superclass, superclass) :-
        ppRuntimeClause_(SpecializesClass_(Entity, Superclass, _)).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // utility predicates
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //Length(+list, +integer, -integer)
   //Length(-list, +integer, +integer)

        Length([], Length, Length) :-
        !.
        Length([_| Tail], Length0, Length) :-
        Length1 is Length0 + 1,
        Length(Tail, Length1, Length).


        Append([], List, List).
        Append([Head| Tail], List, [Head| Tail2]) :-
        Append(Tail, List, Tail2).


        member(Head, [Head| _]).
        member(Head, [_| Tail]) :-
        member(Head, Tail).


        member_var(V, [H| _]) :-
        V == H.
        member_var(V, [_| T]) :-
        member_var(V, T).


        memberchk_var(Element, [Head| Tail]) :-
        (	Element == Head ->
        true
        ;	memberchk_var(Element, Tail)
        ).


        IsList_or_partialList(Var) :-
        var(Var),
        !.
        IsList_or_partialList([]).
        IsList_or_partialList([_| Tail]) :-
        IsList_or_partialList(Tail).


        IsList((-)) :-
        !,
        fail.
        IsList([]).
        IsList([_| Tail]) :-
        IsList(Tail).


        Is_boolean((-)) :-
        !,
        fail.
        Is_boolean(true).
        Is_boolean(false).


        Intersection(_, [], []) :- !.
        Intersection([], _, []) :- !.
        Intersection([Head1| Tail1], List2, Intersection) :-
        (	memberchk_var(Head1, List2) ->
        Intersection = [Head1| IntersectionRest],
        Intersection(Tail1, List2, IntersectionRest)
        ;	Intersection(Tail1, List2, Intersection)
        ).


        varSubtract([], _, []).
        varSubtract([Head| Tail], List, Rest) :-
        (	memberchk_var(Head, List) ->
        varSubtract(Tail, List, Rest)
        ;	Rest = [Head| Tail2],
        varSubtract(Tail, List, Tail2)
        ).


        SumList(List, Sum) :-
        SumList(List, 0, Sum).

        SumList([], Sum, Sum).
        SumList([Value| Values], Sum0, Sum) :-
        Sum1 is Sum0 + Value,
        SumList(Values, Sum1, Sum).


        ReadFile_to_terms(Mode, File, Directory, SourceFile, Terms) :-
   //check file specification and expand library notation or environment
   //variable if used
        catch(
        CheckAnd_expandSourceFile(File, ExpandedFile),
        error(FileError, _),
        throw(FileError)
        ),
   //find the full file name as the extension may be missing
        (	SourceFile_name(ExpandedFile, [], Directory, _, _, SourceFile),
   //avoid a loading loop by checking that the file name is different
   //from the name of the file containing the include/1 directive
        \+ ppFile_pathsFlags_(_, _, SourceFile, _, _),
        File_exists(SourceFile) ->
        true
        ;	throw(existence_error(file, File))
        ),
        catch(
        open(SourceFile, read, Stream, []),
        error(OpenError, _),
        throw(OpenError)
        ),
        catch(
        ReadStream_to_terms(Mode, Stream, Terms),
        error(TermError, _),
        ReadFile_to_terms_error_handler(SourceFile, Stream, TermError)
        ),
        Close(Stream).


        ReadFile_to_terms_error_handler(SourceFile, Stream, Error) :-
        ppFile_pathsFlags_(_, _, _, ObjectFile, _),
        (	StreamCurrentLine_number(Stream, Line) ->
        true
        ;	Line = -1
        ),
        Close(Stream),
        Compiler_error_handler(SourceFile, ObjectFile, Line-Line, Error).


        ReadStream_to_terms(runtime, Stream, Terms) :-
        ReadStream_to_termsRuntime(Stream, Terms).
        ReadStream_to_terms(compile(_), Stream, Terms) :-
        ReadStream_to_termsCompile(Stream, Terms).


        ReadStream_to_termsRuntime(Stream, Terms) :-
        Read_term(Stream, Term, [], Lines, VariableNames),
        ReadStream_to_termsRuntime(Term, Lines, VariableNames, Stream, Terms).

        ReadStream_to_termsRuntime(Term, Lines, VariableNames, Stream, [Term-sd(Lines,VariableNames)| Terms]) :-
        var(Term),
   //delay the instantiation error
        !,
        Read_term(Stream, NextTerm, [], NextLines, NextVariableNames),
        ReadStream_to_termsRuntime(NextTerm, NextLines, NextVariableNames, Stream, Terms).
        ReadStream_to_termsRuntime(end_ofFile, _, _, _, []) :-
        !.
        ReadStream_to_termsRuntime((:- op(Priority, Specifier, Operators)), Lines, VariableNames, Stream, [(:- op(Priority, Specifier, Operators))-sd(Lines,VariableNames)| Terms]) :-
        !,
        Check(operatorSpecification, op(Priority, Specifier, Operators)),
        (	pp_entity_(_, _, _, _, _) ->
        Activate_entity_operators(Priority, Specifier, Operators, l)
        ;	ActivateFile_operators(Priority, Specifier, Operators)
        ),
        Read_term(Stream, NextTerm, [], NextLines, NextVariableNames),
        ReadStream_to_termsRuntime(NextTerm, NextLines, NextVariableNames, Stream, Terms).
        ReadStream_to_termsRuntime(Term, Lines, VariableNames, Stream, [Term-sd(Lines,VariableNames)| Terms]) :-
        Read_term(Stream, NextTerm, [], NextLines, NextVariableNames),
        ReadStream_to_termsRuntime(NextTerm, NextLines, NextVariableNames, Stream, Terms).


        ReadStream_to_termsCompile(Stream, Terms) :-
        Read_term(Stream, Term, [singletons(Singletons)], Lines, VariableNames),
        ReadStream_to_termsCompile(Term, Singletons, Lines, VariableNames, Stream, Terms).

        ReadStream_to_termsCompile(Term, _, Lines, VariableNames, Stream, [Term-sd(Lines,VariableNames)| Terms]) :-
        var(Term),
   //delay the instantiation error
        !,
        Read_term(Stream, NextTerm, [singletons(NextSingletons)], NextLines, NextVariableNames),
        ReadStream_to_termsCompile(NextTerm, NextSingletons, NextLines, NextVariableNames, Stream, Terms).
        ReadStream_to_termsCompile(end_ofFile, _, _, _, _, []) :-
        !.
        ReadStream_to_termsCompile((:- op(Priority, Specifier, Operators)), _, Lines, VariableNames, Stream, [(:- op(Priority, Specifier, Operators))-sd(Lines,VariableNames)| Terms]) :-
        !,
        Check(operatorSpecification, op(Priority, Specifier, Operators)),
        (	pp_entity_(_, _, _, _, _) ->
        Activate_entity_operators(Priority, Specifier, Operators, l)
        ;	ActivateFile_operators(Priority, Specifier, Operators)
        ),
        Read_term(Stream, NextTerm, [singletons(NextSingletons)], NextLines, NextVariableNames),
        ReadStream_to_termsCompile(NextTerm, NextSingletons, NextLines, NextVariableNames, Stream, Terms).
        ReadStream_to_termsCompile(Term, Singletons, Lines, VariableNames, Stream, [Term-sd(Lines,VariableNames)| Terms]) :-
        ReportSingleton_variables(Singletons, Term),
        Read_term(Stream, NextTerm, [singletons(NextSingletons)], NextLines, NextVariableNames),
        ReadStream_to_termsCompile(NextTerm, NextSingletons, NextLines, NextVariableNames, Stream, Terms).



   //Check(+atom, @term, @callable)
        %
   //entityKind-checking for built-in predicate arguments

        Check(var, Term, Context) :-
        (	var(Term) ->
        true
        ;	throw(error(type_error(variable, Term), Context))
        ).

        Check(nonvar, Term, Context) :-
        (	nonvar(Term) ->
        true
        ;	throw(error(instantiation_error, Context))
        ).

        Check(ground, Term, Context) :-
        (	ground(Term) ->
        true
        ;	throw(error(instantiation_error, Context))
        ).

        Check(atom, Term, Context) :-
        (	atom(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(atom, Term), Context))
        ).

        Check(var_orAtom, Term, Context) :-
        (	var(Term) ->
        true
        ;	atom(Term) ->
        true
        ;	throw(error(type_error(atom, Term), Context))
        ).

        Check(boolean, Term, Context) :-
        (	Term == true ->
        true
        ;	Term == false ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	atom(Term) ->
        throw(error(domain_error(boolean, Term), Context))
        ;	throw(error(type_error(atom, Term), Context))
        ).

        Check(var_or_boolean, Term, Context) :-
        (	var(Term) ->
        true
        ;	\+ atom(Term) ->
        throw(error(type_error(atom, Term), Context))
        ;	Term \== true,
        Term \== false,
        throw(error(domain_error(boolean, Term), Context))
        ).

        Check(atom_orString, Term, Context) :-
        (	atom(Term) ->
        true
        ;	String(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(atom_orString, Term), Context))
        ).

        Check(integer, Term, Context) :-
        (	integer(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(integer, Term), Context))
        ).

        Check(var_orInteger, Term, Context) :-
        (	var(Term) ->
        true
        ;	integer(Term) ->
        true
        ;	throw(error(type_error(integer, Term), Context))
        ).

        Check(non_negativeInteger, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	\+ integer(Term) ->
        throw(error(type_error(integer, Term), Context))
        ;	Term < 0 ->
        throw(error(domain_error(notLess_than_zero, Term), Context))
        ;	true
        ).

        Check(var_or_non_negativeInteger, Term, Context) :-
        (	var(Term) ->
        true
        ;	\+ integer(Term) ->
        throw(error(type_error(integer, Term), Context))
        ;	Term < 0 ->
        throw(error(domain_error(notLess_than_zero, Term), Context))
        ;	true
        ).

        Check(float, Term, Context) :-
        (	float(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(float, Term), Context))
        ).

        Check(atomic, Term, Context) :-
        (	atomic(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(atomic, Term), Context))
        ).

        Check(atomic_orString, Term, Context) :-
        (	atomic(Term) ->
        true
        ;	String(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(atomic_orString, Term), Context))
        ).

        Check(curly_bracketed_term, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	Term = {_} ->
        true
        ;	Term == '{}' ->
        true
        ;	throw(error(type_error(curly_bracketed_term, Term), Context))
        ).

        Check(var_orCurly_bracketed_term, Term, Context) :-
        (	var(Term) ->
        true
        ;	Term = {_} ->
        true
        ;	Term == '{}' ->
        true
        ;	throw(error(type_error(curly_bracketed_term, Term), Context))
        ).

        Check(callable, Term, Context) :-
        (	callable(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(callable, Term), Context))
        ).

        Check(var_orCallable, Term, Context) :-
        (	var(Term) ->
        true
        ;	callable(Term) ->
        true
        ;	throw(error(type_error(callable, Term), Context))
        ).

        Check(qualifiedCallable, Term, Context) :-
        (	prologFeature(modules, supported) ->
        Check(qualifiedCallable_, Term, Context)
        ;	Check(callable, Term, Context)
        ).

        Check(qualifiedCallable_, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	Term = ':(Module, Goal) ->
        Check(moduleIdentifier, Module, Context),
        Check(qualifiedCallable_, Goal, Context)
        ;	callable(Term) ->
        true
        ;	throw(error(type_error(callable, Term), Context))
        ).

        Check(clause, Term, Context) :-
        (	Term = (Head :- Body) ->
        Check(callable, Head, Context),
        Check(callable, Body, Context)
        ;	callable(Term) ->
        true
        ;	throw(error(type_error(callable, Term), Context))
        ).

        Check(clause_or_partialClause, Term, Context) :-
        (	Term = (Head :- Body) ->
        Check(callable, Head, Context),
        Check(var_orCallable, Body, Context)
        ;	callable(Term) ->
        true
        ;	throw(error(type_error(callable, Term), Context))
        ).

        Check(list, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	IsList(Term) ->
        true
        ;	throw(error(type_error(list, Term), Context))
        ).

        Check(list_or_partialList, Term, Context) :-
        (	var(Term) ->
        true
        ;	IsList_or_partialList(Term) ->
        true
        ;	throw(error(type_error(list, Term), Context))
        ).

        Check(list(Type), Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	IsList(Term) ->
        forall(member(Item, Term), Check(Type, Item, Context))
        ;	throw(error(type_error(list, Term), Context))
        ).

        Check(object, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	Current_object_(Term, _, _, _, _, _, _, _, _, _, _) ->
        true
        ;	callable(Term) ->
        throw(error(existence_error(object, Term), Context))
        ;	throw(error(type_error(objectIdentifier, Term), Context))
        ).

        Check(objectIdentifier, Term, Context) :-
        (	callable(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(objectIdentifier, Term), Context))
        ).

        Check(var_or_objectIdentifier, Term, Context) :-
        (	var(Term) ->
        true
        ;	callable(Term) ->
        true
        ;	throw(error(type_error(objectIdentifier, Term), Context))
        ).

        Check(protocol, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	Current_protocol_(Term, _, _, _, _) ->
        true
        ;	atom(Term) ->
        throw(error(existence_error(protocol, Term), Context))
        ;	throw(error(type_error(protocolIdentifier, Term), Context))
        ).

        Check(protocolIdentifier, Term, Context) :-
        (	atom(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(protocolIdentifier, Term), Context))
        ).

        Check(var_or_protocolIdentifier, Term, Context) :-
        (	var(Term) ->
        true
        ;	atom(Term) ->
        true
        ;	throw(error(type_error(protocolIdentifier, Term), Context))
        ).

        Check(category, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	CurrentCategory_(Term, _, _, _, _, _) ->
        true
        ;	callable(Term) ->
        throw(error(existence_error(category, Term), Context))
        ;	throw(error(type_error(categoryIdentifier, Term), Context))
        ).

        Check(categoryIdentifier, Term, Context) :-
        (	callable(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(categoryIdentifier, Term), Context))
        ).

        Check(var_orCategoryIdentifier, Term, Context) :-
        (	var(Term) ->
        true
        ;	callable(Term) ->
        true
        ;	throw(error(type_error(categoryIdentifier, Term), Context))
        ).

        Check(entityIdentifier, Term, Context) :-
        (	callable(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(entityIdentifier, Term), Context))
        ).

        Check(var_or_entityIdentifier, Term, Context) :-
        (	var(Term) ->
        true
        ;	callable(Term) ->
        true
        ;	throw(error(type_error(entityIdentifier, Term), Context))
        ).

        Check(moduleIdentifier, Term, Context) :-
        (	atom(Term) ->
        true
        ;	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	throw(error(type_error(moduleIdentifier, Term), Context))
        ).

        Check(var_or_moduleIdentifier, Term, Context) :-
        (	var(Term) ->
        true
        ;	atom(Term) ->
        true
        ;	throw(error(type_error(moduleIdentifier, Term), Context))
        ).

        Check(predicateIndicator, Term, Context) :-
        (	Term = Functor/Arity ->
        Check(atom, Functor, Context),
        Check(non_negativeInteger, Arity, Context)
        ;	throw(error(type_error(predicateIndicator, Term), Context))
        ).

        Check(var_or_predicateIndicator, Term, Context) :-
        (	var(Term) ->
        true
        ;	Term = Functor/Arity ->
        Check(var_orAtom, Functor, Context),
        Check(var_or_non_negativeInteger, Arity, Context)
        ;	throw(error(type_error(predicateIndicator, Term), Context))
        ).

        Check(predicate_or_non_terminalIndicator, Term, Context) :-
        (	Term = Functor/Arity ->
        Check(atom, Functor, Context),
        Check(non_negativeInteger, Arity, Context)
        ;	Term = Functor//Arity ->
        Check(atom, Functor, Context),
        Check(non_negativeInteger, Arity, Context)
        ;	throw(error(type_error(predicateIndicator, Term), Context))
        ).

        Check(scope, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	validScope(Term) ->
        true
        ;	atom(Term) ->
        throw(error(domain_error(scope, Term), Context))
        ;	throw(error(type_error(atom, Term), Context))
        ).

        Check(var_orScope, Term, Context) :-
        (	var(Term) ->
        true
        ;	validScope(Term) ->
        true
        ;	atom(Term) ->
        throw(error(domain_error(scope, Term), Context))
        ;	throw(error(type_error(atom, Term), Context))
        ).

        Check(var_or_event, Term, Context) :-
        (	var(Term) ->
        true
        ;	Term \== before,
        Term \== after ->
        throw(error(type_error(event, Term), Context))
        ;	true
        ).

        Check(operatorSpecification, Term, Context) :-
        (	Term = op(Priority, Specifier, Operators) ->
        Check(operator_priority, Priority, Context),
        Check(operatorSpecifier, Specifier, Context),
        Check(operator_names, Operators, Context)
        ;	throw(error(type_error(operatorSpecification, Term), Context))
        ).

        Check(operator_priority, Priority, Context) :-
        (	var(Priority) ->
        throw(error(instantiation_error, Context))
        ;	\+ integer(Priority),
        throw(error(type_error(integer, Priority), Context))
        ;	(Priority < 0; Priority > 1200) ->
        throw(error(domain_error(operator_priority, Priority), Context))
        ;	true
        ).

        Check(var_or_operator_priority, Priority, Context) :-
        (	var(Priority) ->
        true
        ;	Check(operator_priority, Priority, Context)
        ).

        Check(operatorSpecifier, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	\+ atom(Term) ->
        throw(error(type_error(atom, Term), Context))
        ;	member(Term, [fx, fy, xfx, xfy, yfx, xf, yf]) ->
        true
        ;	throw(error(domain_error(operatorSpecifier, Term), Context))
        ).

        Check(var_or_operatorSpecifier, Term, Context) :-
        (	var(Term) ->
        true
        ;	Check(operatorSpecifier, Term, Context)
        ).

        Check(operator_names, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	Term == (',') ->
        throw(error(permission_error(modify, operator, ','), Context))
        ;	atom(Term) ->
        true
        ;	\+ IsList(Term) ->
        throw(type_error(list, Term))
        ;	\+ (member(Operator, Term), \+ Check(operator_name, Operator, Context))
        ).

        Check(operator_name, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	Term == (',') ->
        throw(error(permission_error(modify, operator, ','), Context))
        ;	atom(Term) ->
        true
        ;	throw(error(type_error(atom, Term), Context))
        ).

        Check(var_or_object_property, Term, Context) :-
        (	var(Term) ->
        true
        ;	valid_object_property(Term) ->
        true
        ;	callable(Term) ->
        throw(error(domain_error(object_property, Term), Context))
        ;	throw(error(type_error(callable, Term), Context))
        ).

        Check(var_orCategory_property, Term, Context) :-
        (	var(Term) ->
        true
        ;	validCategory_property(Term) ->
        true
        ;	callable(Term) ->
        throw(error(domain_error(category_property, Term), Context))
        ;	throw(error(type_error(callable, Term), Context))
        ).

        Check(var_or_protocol_property, Term, Context) :-
        (	var(Term) ->
        true
        ;	valid_protocol_property(Term) ->
        true
        ;	callable(Term) ->
        throw(error(domain_error(protocol_property, Term), Context))
        ;	throw(error(type_error(callable, Term), Context))
        ).

        Check(flag, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	validFlag(Term) ->
        true
        ;	userDefinedFlag_(Term, _, _) ->
        true
        ;	atom(Term) ->
        throw(error(domain_error(flag, Term), Context))
        ;	throw(error(type_error(atom, Term), Context))
        ).

        Check(var_orFlag, Term, Context) :-
        (	var(Term) ->
        true
        ;	validFlag(Term) ->
        true
        ;	userDefinedFlag_(Term, _, _) ->
        true
        ;	atom(Term) ->
        throw(error(domain_error(flag, Term), Context))
        ;	throw(error(type_error(atom, Term), Context))
        ).

        Check(read_writeFlag, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	\+ atom(Term) ->
        throw(error(type_error(atom, Term), Context))
        ;	\+ validFlag(Term),
        \+ userDefinedFlag_(Term, _, _) ->
        throw(error(domain_error(flag, Term), Context))
        ;	Read_onlyFlag(Term) ->
        throw(error(permission_error(modify, flag, Term), Context))
        ;	userDefinedFlag_(Term, read_only, _) ->
        throw(error(permission_error(modify, flag, Term), Context))
        ;	true
        ).

        Check(var_orRead_writeFlag, Term, Context) :-
        (	var(Term) ->
        true
        ;	\+ atom(Term) ->
        throw(error(type_error(atom, Term), Context))
        ;	\+ validFlag(Term),
        \+ userDefinedFlag_(Term, _, _) ->
        throw(error(domain_error(flag, Term), Context))
        ;	Read_onlyFlag(Term) ->
        throw(error(permission_error(modify, flag, Term), Context))
        ;	userDefinedFlag_(Term, read_only, _) ->
        throw(error(permission_error(modify, flag, Term), Context))
        ;	true
        ).

        Check(flag_value, Term1+Term2, Context) :-
        (	var(Term2) ->
        throw(error(instantiation_error, Context))
        ;	validFlag_value(Term1, Term2) ->
        true
        ;	userDefinedFlag_(Term1, _, Type),
        call(Type, Term2) ->
        true
        ;	throw(error(domain_error(flag_value, Term1 + Term2), Context))
        ).

        Check(var_orFlag_value, Term1+Term2, Context) :-
        (	var(Term2) ->
        true
        ;	validFlag_value(Term1, Term2) ->
        true
        ;	userDefinedFlag_(Term1, _, Type),
        call(Type, Term2) ->
        true
        ;	throw(error(domain_error(flag_value, Term1 + Term2), Context))
        ).

        Check(predicate_property, Term, Context) :-
        (	var(Term) ->
        throw(error(instantiation_error, Context))
        ;	valid_predicate_property(Term) ->
        true
        ;	throw(error(domain_error(predicate_property, Term), Context))
        ).

        Check(var_or_predicate_property, Term, Context) :-
        (	var(Term) ->
        true
        ;	valid_predicate_property(Term) ->
        true
        ;	throw(error(domain_error(predicate_property, Term), Context))
        ).

        Check(key_valueInfo_pair, Term, Context) :-
        (	Term = (Key is Value) ->
        Check(atom, Key, Context),
        Check(nonvar, Value, Context)
        ;	throw(error(type_error(key_valueInfo_pair, Term), Context))
        ).



   //Check(+atom, @term)
        %
   //this simpler version of the predicate is mainly used when compiling source files

        Check(Type, Term) :-
        catch(Check(Type, Term, _), error(Error, _), throw(Error)).




        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        %
   // Logtalk startup initialization
        %
        %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%



   //dynamic entity counters initial definitions
        %
   //counters used when generating identifiers for dynamically created entities

        Dynamic_entityCounter_(object,   o, 1).
        Dynamic_entityCounter_(protocol, p, 1).
        Dynamic_entityCounter_(category, c, 1).



   //Load_builtIn_entities(-atom)
        %
   //loads all built-in entities if not already loaded (when embedding
   //Logtalk, the pre-compiled entities are loaded prior to this file)

        Load_builtIn_entities(ScratchDirectory) :-
        (	expandLibraryAlias(scratchDirectory, ScratchDirectory) ->
   //user override for the default scratch directory
        SetCompilerFlag(scratchDirectory, ScratchDirectory)
        ;	% use default scratch directory
        expandLibraryAlias(logtalk_user, LogtalkUserDirectory),
        atomConcat(LogtalkUserDirectory, 'scratch/', ScratchDirectory)
        ),
        loadBuiltInEntity(expanding, protocol, 'expanding', ScratchDirectory),
        loadBuiltInEntity(monitoring, protocol, 'monitoring', ScratchDirectory),
        loadBuiltInEntity(forwarding, protocol, 'forwarding', ScratchDirectory),
        loadBuiltInEntity(user, object, 'user', ScratchDirectory),
        loadBuiltInEntity(logtalk, object, 'logtalk', ScratchDirectory),
        loadBuiltInEntity(core_messages, category, 'core_messages', ScratchDirectory),
        assertz(builtIn_entitiesLoaded_').


        loadBuiltInEntity(Entity, Type, File, ScratchDirectory) :-
        (	Type == protocol,
        Current_protocol_(Entity, _, _, _, _) ->
        true
        ;	Type == category,
        CurrentCategory_(Entity, _, _, _, _, _) ->
        true
        ;	Type == object,
        Current_object_(Entity, _, _, _, _, _, _, _, _, _, _) ->
        true
        ;	% not an embedded entity; compile and load it
        logtalkLoad(
        core(File),
        [	% we need a fixed code prefix as some of the entity predicates may need
   //to be called directly by the compiler/runtime
        code_prefix('$'),
   //delete the generated intermediate files as they may be non-portable
   //between backend Prolog compilers
        clean(on),
   //use a scratch directory where we expect to have writing permission
        scratchDirectory(ScratchDirectory),
   //optimize entity code, allowing static binding to this entity resources
        optimize(on),
   //don't print any messages on the compilation and loading of these entities
        report(off),
   //prevent any attempts of logtalk_make(all) to reload this file
        reload(skip)
        ]
        )
        ).



   //LoadSettingsFile(+atom, -nonvar)
        %
   //loads any settings file defined by the user; settings files are compiled
   //and loaded silently, ignoring any errors;  the intermediate Prolog files
   //are deleted using the clean/1 compiler flag in order to prevent problems
   //when switching between backend Prolog compilers

        LoadSettingsFile(ScratchDirectory, Result) :-
        DefaultFlag(settingsFile, Value),
        Options = [
   //delete the generated intermediate file as it may be non-portable
   //between backend Prolog compilers
        clean(on),
   //use a scratch directory where we expect to have writing permission
        scratchDirectory(ScratchDirectory),
   //optimize any entity code present, allowing static binding to
   //entity resources, and preventing their redefinition
        optimize(on), reload(skip),
   //don't print any compilation or loading messages
        report(off)
        ],
        LoadSettingsFile(Value, Options, Result).


        LoadSettingsFile(deny, _, disabled).

        LoadSettingsFile(restrict, Options, Result) :-
   //lookup for a settings file restricted to the Logtalk user directory or home directory
        (	% first lookup for a settings file in the Logtalk user directory
        expandLibraryAlias(logtalk_user, User),
        LoadSettingsFileFromDirectory(User, Options, Result) ->
        true
        ;	% if not found, lookup for a settings file in the user home directory
        expandLibraryAlias(home, Home),
        LoadSettingsFileFromDirectory(Home, Options, Result) ->
        true
        ;	% no settings file found
        Result = none(restrict)
        ).

        LoadSettingsFile(allow, Options, Result) :-
        (	% first lookup for a settings file in the startup directory
        expandLibraryAlias(startup, Startup),
        LoadSettingsFileFromDirectory(Startup, Options, Result) ->
        true
        ;	% if not found, lookup for a settings file in the Logtalk user directory
        expandLibraryAlias(logtalk_user, User),
        LoadSettingsFileFromDirectory(User, Options, Result) ->
        true
        ;	% if still not found, lookup for a settings file in the user home directory
        expandLibraryAlias(home, Home),
        LoadSettingsFileFromDirectory(Home, Options, Result) ->
        true
        ;	% no settings file found
        Result = none(allow)
        ).


        LoadSettingsFileFromDirectory(Directory, Options, Result) :-
        (	File_extension(logtalk, Extension),
   //more than one possible extension may be listed in the used adapter file
        atomConcat(settings, Extension, SettingsFile),
   //construct full path to the possible settings file; directories resulting
   //from library alias expansion are guaranteed to end with a slash
        atomConcat(Directory, SettingsFile, SettingsPath),
        File_exists(SettingsPath) ->
   //settings file found; try to load it
        (	catch(logtalkLoad(SettingsPath, Options), _, fail) ->
        Result = loaded(Directory)
        ;	Result = error(Directory)
        )
        ;	% no settings file in this directory
        fail
        ).



   //ReportSettingsFile(@nonvar)
        %
   //reports result of the attempt to load a settings file defined by the user

        ReportSettingsFile(loaded(Path)) :-
        print_message(comment(settings), core, loadedSettingsFile(Path)).

        ReportSettingsFile(disabled) :-
        print_message(comment(settings), core, settingsFileDisabled).

        ReportSettingsFile(error(Path)) :-
        print_message(error, core, errorLoadingSettingsFile(Path)).

        ReportSettingsFile(none(Flag)) :-
        print_message(comment(settings), core, noSettingsFileFound(Flag)).



   //cache default and read-only compiler flags to improve the performance
   //of the compiler by reducing the potential number of flag levels that
   //need to be checked for finding the value of a flag in a given context
        %
   //although there should be no clauses for the CurrentFlag_'/2
   //predicate when this predicte is called at runtime initialization, a
   //wrong file order when embedding Logtalk or a Logtalk application can
   //falsify this assumption; therefore, we test for a flag definition
   //before caching its default value

        CacheCompilerFlags' :-
        DefaultFlag(Name, Value),
        \+ CurrentFlag_(Name, _),
        assertz(CurrentFlag_(Name, Value)),
        fail.

        CacheCompilerFlags' :-
        prologFeature(Name, Value),
        \+ CurrentFlag_(Name, _),
        assertz(CurrentFlag_(Name, Value)),
        fail.

        CacheCompilerFlags' :-
        versionData(VersionData),
        assertz(CurrentFlag_(versionData, VersionData)).



   //CompileDefault_hooks'
        %
   //compiles the default hooks specified on the backend Prolog compiler adapter
   //file or settings file for better performance when compiling source files

        CompileDefault_hooks' :-
        (	CompilerFlag(hook, Hook) ->
        Compile_hooks(Hook)
        ;	true
        ).



   //StartRuntime_threading'
        %
   //initializes the engines mutext plus the asynchronous threaded calls mutex
   //and tag counter support for compilers supporting multi-threading programming
   //(currently we use integers for the tag counter, which impose a limitation on
   //the maximum number of tags on backend Prolog compilers with bounded integers)

        StartRuntime_threading' :-
        (	prologFeature(engines, supported) ->
        mutexCreate(_, [alias(engines')])
        ;	true
        ),
        (	(	prologFeature(engines, supported)
        ;	prologFeature(threads, supported)
        ) ->
        mutexCreate(_, [alias(threaded_tag')]),
        (	current_prologFlag(bounded, true) ->
        current_prologFlag(minInteger, Min),
        assertz(threaded_tagCounter_(Min))
        ;	assertz(threaded_tagCounter_(0))
        )
        ;	true
        ).



   //Check_prolog_version'
        %
   //checks for a compatible backend Prolog compiler version
        %
   //note, however, that an old and incompatible backend Prolog version may
   //break Logtalk initialization before this checking predicate is called

        Check_prolog_version' :-
        prologFeature(prolog_version, Current),
        prologFeature(prologCompatible_version, Check),
        functor(Check, Operator, 1),
        arg(1, Check, Compatible),
        (	call(Operator, Current, Compatible) ->
        true
        ;	print_message(warning(compatibility), core, possiblyIncompatible_prolog_version(Current, Compatible))
        ).



   //Logtalk runtime initialization
        %
   //when embedding Logtalk in a saved state created by a backend Prolog
   //compiler, the runtime initialization may be triggered again when
   //running the saved state; we use a dynamic predicate as a flag to
   //prevent redoing this initialization

        RuntimeInitialization' :-
        RuntimeInitializationCompleted_',
        !.

        RuntimeInitialization' :-
        CacheCompilerFlags',
        Load_builtIn_entities(ScratchDirectory),
        LoadSettingsFile(ScratchDirectory, Result),
        print_message(banner, core, banner),
        print_message(comment(settings), core, defaultFlags),
        CompileDefault_hooks',
        StartRuntime_threading',
        ReportSettingsFile(Result),
        print_message(comment(help), core, help),
        Check_prolog_version',
        assertz(RuntimeInitializationC

 */