package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.impl.DefaultFileSystemManager;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.HiTalkBuiltInTransform;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.*;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.core.utils.ISymbolTable;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.entities.context.Context;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.LoadContext;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.*;
import org.ltc.hitalk.wam.compiler.hilog.HiLogCompilerApp;
import org.ltc.hitalk.wam.compiler.prolog.ICompilerObserver;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;
import org.ltc.hitalk.wam.task.PreCompilerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.in;
import static org.ltc.hitalk.compiler.bktables.BkTableKind.LOADED_ENTITIES;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.RESOURCE_ERROR;
import static org.ltc.hitalk.wam.compiler.Language.HITALK;

/**
 * Reloading files, active code and threads
 * <p>
 * Traditionally, Prolog BaseApps allow for reloading files holding currently active code.
 * In particular, the following sequence is a valid use of the development BaseApp:
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
 * <p>
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
 *
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
 *
 * <p>
 * Note that this schema does not prevent deadlocks under all situations. Consider two mutually dependent (see section
 * 4.3.2.1) module files A and B, where thread 1 starts loading A and thread 2 starts loading B at the same time.
 * Both threads will deadlock when trying to load the used module.
 * <p>
 * The current implementation does not detect such cases and the involved threads will freeze.
 * This problem can be avoided if a mutually dependent collection of files is always loaded from the same start file.
 */
public class HiTalkCompilerApp<T extends HtMethod, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery>
        extends HiLogCompilerApp<T, P, Q, PC, QC> {

    private final static Language language = HITALK;

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
    public final HtProperty[] DEFAULT_PROPS = new HtProperty[0];
    //library entity names
    public HtEntityIdentifier EXPANDING;
    public HtEntityIdentifier MONITORING;
    public HtEntityIdentifier FORWARDING;
    public HtEntityIdentifier USER;

    /**
     * @return
     */
    @Override
    public CompilerConfig getConfig() {
        return (CompilerConfig) super.getConfig();
    }

    /**
     * @param config
     */
    @Override
    public void setConfig(IConfig config) {
        this.config = config;
    }

    /**
     *
     */
    public void init() throws LinkageException, IOException {

    }

    /**
     * @param symbolTable
     * @param interner
     * @param observer
     * @param parser
     * @return
     */
    public PrologWAMCompiler<T, P, Q, PC, QC>
    createWAMCompiler(ISymbolTable<Integer, String, Object> symbolTable,
                      IVafInterner interner,
                      ICompilerObserver<P, Q> observer,
                      HtPrologParser parser) {
        return new HiTalkWAMCompiler<>(symbolTable, interner, parser, observer);
    }

    public IParser createParser(HiTalkInputStream stream,
                                IVafInterner interner,
                                ITermFactory factory,
                                IOperatorTable opTable) throws Exception {
        return new HiTalkParser(stream, interner, factory, opTable);
    }

    public HtEntityIdentifier LOGTALK;
    public HtEntityIdentifier CORE_MESSAGES;
    //
    public IFunctor OBJECT;
    public IFunctor PROTOCOL;
    public IFunctor CATEGORY;

    public IFunctor END_OBJECT;
    public IFunctor END_CATEGORY;
    public IFunctor END_PROTOCOL;

    /**
     * Used for logging to the console.
     */
    protected final Logger console = LoggerFactory.getLogger(getClass().getSimpleName());
    protected boolean compiling;
    protected boolean compilingEntity;
    /**
     * True if the system is compiling source files with the -c option or qcompile/1
     * into an intermediate code file.
     * Can be used to perform conditional code optimizations in term_expansion/2 (see also the -O option)
     * or to omit execution of directives during compilation.
     */

    protected DefaultFileSystemManager fsManager;
    private static final String vfsPath = "C:/Users/Anthony_2/VFS";
    private static InputStreamReader is;
    private static BufferedReader br;

    /**
     * Holds the instruction generating compiler.
     */
    private LogicCompilerObserver<T, Q> observer;

    /**
     * @return
     */
    public Language getLanguage() {
        return language;
    }

    /**
     * @return
     */
    public BookKeepingTables getBkt() {
        return bkt;
    }

    protected BookKeepingTables bkt = new BookKeepingTables();

    /**
     * Holds the pre-compiler, for analyzing and transforming terms prior to compilation proper.
     */

    protected HiTalkBuiltInTransform<T, P, Q, PC, QC> builtInTransform;

    /**
     * @param fileName
     */
    public HiTalkCompilerApp(String fileName) throws Exception {
        super(fileName);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            IApplication application = new HiTalkCompilerApp<>(args[0]);
            application.init();
            application.start();
        } catch (Exception e) {
            e.printStackTrace();
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
     * // HtProperty[] flags;
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
    protected HtProperty[] createFlags(LoadContext loadContext, String scratchDirectory) {
        return new HtProperty[]{///todo flags
//                getTermFactory().createFlag("basename", fileName),//FIXME PARSE
//                getTermFactory().createFlag("directory", fileName),//FIXME PARSE
//                getTermFactory().createFlag("entity_identifier", new HtFunctor(-1, NIL)),//FIXME PARSE
//                getTermFactory().createFlag("file", fileName),//FIXME PARSE
//                getTermFactory().createFlag("basename", fileName),//FIXME PARSE
        };
    }

    /**
     *
     */
    public void initialize() throws Exception {
        initBookKeepingTables();
        initDirectives();
        cacheCompilerFlags();
        Path scratchDirectory = loadBuiltInEntities();
        startRuntimeThreading();
        Object result = loadSettingsFile(scratchDirectory);
//        printMessage(BANNER, CORE, BANNER);
//        printMessage(comment(settings), CORE, DEFAULT_FLAGS);
//        expandGoals()
//        ;
//        compileHooks();
        reportSettingsFile(result);
    }

    /**
     * @param result
     */
    protected void reportSettingsFile(Object result) {

    }

    public InputStream getResource(String s) throws FileSystemException {
//    fsManager.addOperationProvider();
//       URL url = new URL("");

        FileObject fileObject = fsManager.resolveFile("" + s);
//        fsManager.createFileSystem("res",)
        if (fileObject != null && fileObject.exists()) {
            return fileObject.getContent().getInputStream();

        }
        return in;
    }


    protected Object loadSettingsFile(Path scratchDirectory) throws Exception {

        return super.loadSettingsFile(scratchDirectory);
    }

//    /**
//     * @param input
//     * @throws IOException
//     */
//    private void logtalkCompile ( InputStream input ) throws IOException, HtSourceCodeException {
//        ITokenSource tokenSource = getTokenSourceForInputStream(input, "");
//        setTokenSource(tokenSource);
//        compiler.compile(tokenSource);
//    }

    /**
     * @param identifier
     * @param fileName
     * @param scratchDir
     * @throws IOException
     * @throws HtSourceCodeException
     */
    protected void loadBuiltInEntity(HtEntityIdentifier identifier, String fileName, Path scratchDir) throws Exception {
        List rs = bkt.select(LOADED_ENTITIES);
        if (rs.isEmpty()) {
            loadContext.setProps(createProps(scratchDir));
//            compileLoad(fileName, loadContext);
            bkt.add(new BkLoadedEntities(identifier));
        }
    }

    protected HtProperty[] createProps(Path scratchDir) {
        return new HtProperty[0];
    }

    protected HtProperty[] createFlags(Path scratchDir) {
        return new HtProperty[]{
                //we need a fixed code prefix as some of the entity predicates may need
//                    to be called directly by the compiler/runtime
                getTermFactory().createFlag("code_prefix", "$"),
                //delete the generated intermediate files as they may be non-portable
                //between backend Prolog compilers
                getTermFactory().createFlag("clean", "on"),
                //use a scratch directory where we expect to have writing permission
//                getTermFactory().createFlag("scratch_directory", scratchDir),
                //optimize entity code, allowing static binding to this entity resource
                getTermFactory().createFlag("optimize", "on"),
                //don't print any messages on the compilation and loading of these entities
                getTermFactory().createFlag("report", "off"),
                //prevent any attempts of logtalk_make(all) to reload this file
                getTermFactory().createFlag("reload", "skip")};
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
    protected void initBookKeepingTables() {

    }

    /**
     * compiles the user-defined default compiler hooks
     * (replacing any existing defined hooks)
     */
    protected void compileHooks(HtEntityIdentifier hookEntity) {
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


    protected void initDirectives() {
        initRuntimeDirectives();
        initCompilerDirectives();
    }

    protected void initCompilerDirectives() {

    }

    protected void initRuntimeDirectives() {

    }

    //Compile_message_to_object(@term, @objectIdentifier, -callable, +atom, +compilationContext)

    //compiles a message sending call

    //messages to the pseudo-object "user"

    protected void compileMessageToObject(Term pred, HtEntityIdentifier obj, ICallable call, IFunctor atom, Context ctx) {
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
//        //predicate_property_(Entity, IFunctor/Arity, Property)
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
//        //userDefinedFlag_(HtProperty, Access, Type)
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
//        //pp_public_(IFunctor, Arity)
//        :- dynamic(pp_public_'/2).
//        //pp_protected_(IFunctor, Arity)
//        :- dynamic(pp_protected_'/2).
//        //pp_private_(IFunctor, Arity)
//        :- dynamic(pp_private_'/2).
//        //pp_meta_predicate_(PredTemplate, MetaTemplate)
//        :- dynamic(pp_meta_predicate_'/2).
//        //pp_predicateAlias_(Entity, Pred, Alias, NonTerminalFlag, File, Lines)
//        :- dynamic(pp_predicateAlias_'/6).
//        //pp_non_terminal_(IFunctor, Arity, ExtArity)
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
//        //pp_number_ofClausesRules_(IFunctor, Arity, NumberOfClauses, NumberOfRules)
//        :- dynamic(pp_number_ofClausesRules_'/4).
//        //pp_number_ofClausesRules_(Other, IFunctor, Arity, NumberOfClauses, NumberOfRules)
//        :- dynamic(pp_number_ofClausesRules_'/5).
//
//        //pp_predicateDeclarationLocation_(IFunctor, Arity, File, Line)
//        :- dynamic(pp_predicateDeclarationLocation_'/4).
//        //pp_predicateDefinitionLocation_(IFunctor, Arity, File, Line)
//        :- dynamic(pp_predicateDefinitionLocation_'/4).
//        //ppDefines_predicate_(Head, IFunctor/Arity, ExCtx, THead, Mode, Origin)
//        :- dynamic(ppDefines_predicate_'/6).
//        //ppInline_predicate_(IFunctor/Arity)
//        :- dynamic(ppInline_predicate_'/1).
//
//        //pp_predicateDefinitionLocation_(Other, IFunctor, Arity, File, Line)
//        :- dynamic(pp_predicateDefinitionLocation_'/5).
//
//        //ppCalls_predicate_(IFunctor/Arity, TFunctor/TArity, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(ppCalls_predicate_'/5).
//        //ppCallsSelf_predicate_(IFunctor/Arity, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(ppCallsSelf_predicate_'/4).
//        //ppCallsSuper_predicate_(IFunctor/Arity, HeadFunctor/HeadArity, File, Lines)
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
//        //ppDefines_non_terminal_(IFunctor, Arity)
//        :- dynamic(ppDefines_non_terminal_'/2).
//        //ppCalls_non_terminal_(IFunctor, Arity, Lines)
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
//        //ppReferenced_object_message_(Object, IFunctor/Arity, AliasFunctor/Arity, HeadFunctor/HeadArity, File, Lines)
//        :- dynamic(ppReferenced_object_message_'/6).
//        //ppReferenced_module_predicate_(Module, IFunctor/Arity, AliasFunctor/Arity, HeadFunctor/HeadArity, File, Lines)
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


    protected void cacheCompilerFlags() {

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
    public void logtalkCompile(List<String> fileName) {

    }

    /**
     * @param fileName
     */
    public void logtalkCompile(String fileName) throws Exception {
        wamCompiler.compileFile(fileName, executionContext.getFlags());
//         user, user, user, user, [], []
    }

    public void compile(String fileName, HtProperty[] flags) throws Exception {
        wamCompiler.compile(PlLexer.getTokenSourceForIoFileName(fileName), flags);

    }
    //logtalkCompile(@list(sourceFile_name))

    //compiles to disk a source file or list of sogurce files using default flags

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
//        protected void logtalkCompile (List < String > files, List < HtProperty > flags){
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
    protected void resetWarningsCounters() {
//            retractall(pp_warnings_top_goal_(_)),
//            retractall(ppCompiling_warningsCounter_(_)),
//            retractall(ppLoading_warningsCounter_(_)).
    }

    //
//
//
    protected void initWarningsCounter(Term goal) {
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
    void incrementCompilingWarningsCounter() {
//        ' :- retract(ppCompiling_warningsCounter_(Old)), New is Old + 1, assertz(ppCompiling_warningsCounter_(New)).
//
    }

    void incrementLoading_warningsCounter() {
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
//        AssertCompilerFlags([HtProperty | Flags]) :- HtProperty =.. [Name, Value],
//        retractall(ppFileCompilerFlag_(Name, _)), assertz(ppFileCompilerFlag_(Name, Value)), AssertCompilerFlags(Flags).


    protected void startRuntimeThreading() {


    }

    protected void compileDefaultHooks() {

    }

    //=================================================================================

    /**
     * loads all built-in entities if not already loaded (when embedding
     * Logtalk, the pre-compiled entities are loaded prior to this file)
     *
     * @return
     */
    protected Path loadBuiltInEntities() throws Exception {
        getLogger().info("Loading built-in entities... ");

        Path scratchDir = getScratchDirectory();
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
     * //assume the  component of the library path
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
    protected Path expandLibraryAlias(String library) {
        Path location = logtalkLibraryPath(library);
        return location;
    }

    protected Path logtalkLibraryPath(String library) {

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
     *
     * @return
     */
//    protected
//    void printMessage ( IFunctor kind, Atom component, Atom message ){
//if (getCompilerFlag(FlagKey.REPORTS) != FlagValue.OFF) {
////
////
////        }
//        }
    @Override
    public Path getScratchDirectory() {
        if (scratchDirectory == null) {
            scratchDirectory = Paths.get(DEFAULT_SCRATCH_DIRECTORY).toAbsolutePath();
        }
        return scratchDirectory;
    }

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
//    @Override
    public void setCompilerObserver(LogicCompilerObserver<T, Q> observer) {
        this.observer = observer;
    }

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     */
//    @Override
    public void endScope() {

    }

    /**
     *
     */
    @Override
    public void doInit() throws Exception {
        super.doInit();

        EXPANDING = getTermFactory().createIdentifier(HtEntityKind.PROTOCOL, "expanding");
        MONITORING = getTermFactory().createIdentifier(HtEntityKind.PROTOCOL, "monitoring");
        FORWARDING = getTermFactory().createIdentifier(HtEntityKind.PROTOCOL, "forwarding");
        USER = getTermFactory().createIdentifier(HtEntityKind.OBJECT, "user");
        LOGTALK = getTermFactory().createIdentifier(HtEntityKind.OBJECT, "logtalk");
        CORE_MESSAGES = getTermFactory().createIdentifier(HtEntityKind.CATEGORY, "core_messages");
        OBJECT = getTermFactory().createAtom("object");
        PROTOCOL = getTermFactory().createAtom("protocol");
        CATEGORY = getTermFactory().createAtom("category");
        END_OBJECT = getTermFactory().createAtom("end_object");
//        END_ENUM = getTermFactory().createAtom("end_enum");
        END_PROTOCOL = getTermFactory().createAtom("end_protocol");
        END_CATEGORY = getTermFactory().createAtom("end_category");
//=============================
    }

    public Language language() {
        return HITALK;
    }

    /**
     * @return
     */
    protected List<HtEntityIdentifier> hooksPipeline() {
        List<HtEntityIdentifier> l = new ArrayList<>();

        return l;
    }

    /**
     * @return
     */
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

    public void setBuiltInTransform(HiTalkBuiltInTransform<T, P, Q, PC, QC> builtInTransform) {
        this.builtInTransform = builtInTransform;
    }

    public HiTalkBuiltInTransform<T, P, Q, PC, QC> getBuiltInTransform() {
        return builtInTransform;
    }

    /**
     *
     */
    public static class ClauseChainObserver<T extends HtMethod, P, Q,
            PC extends HiTalkWAMCompiledPredicate,
            QC extends HiTalkWAMCompiledQuery> implements LogicCompilerObserver<T, Q> {
        protected IPreCompiler<T, PreCompilerTask<T>, P, Q, PC, QC> preCompiler;
        protected BaseInstructionCompiler<T, P, Q, PC, QC> instructionCompiler;

        /**
         * @param preCompiler
         * @param instructionCompiler
         */
        public ClauseChainObserver(IPreCompiler<T, PreCompilerTask<T>, P, Q, PC, QC> preCompiler,
                                   BaseInstructionCompiler<T, P, Q, PC, QC> instructionCompiler) {
            this.preCompiler = preCompiler;
            this.instructionCompiler = instructionCompiler;
        }

        /**
         * Accepts notification of the completion of the compilation of a sentence into a (binary) form.
         *
         * @param sentence The compiled form of the sentence.
         */
        @Override
        public void onCompilation(Sentence<T> sentence) {
            try {
                instructionCompiler.compile(sentence.getT());
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(RESOURCE_ERROR, null);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void onQueryCompilation(Sentence<Q> sentence) {
            try {
                instructionCompiler.compile((T) sentence.getT());
            } catch (Exception e) {
                e.printStackTrace();
                throw new ExecutionError(RESOURCE_ERROR, null);
            }
        }
    }
}