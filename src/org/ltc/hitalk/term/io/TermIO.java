package org.ltc.hitalk.term.io;


import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.HiTalkFlag;
import org.ltc.hitalk.compiler.bktables.LogtalkFlag;
import org.ltc.hitalk.parser.HtPrologParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public
class TermIO {

    public
    TermIO ( ITermFactory tf, HtPrologParser pp ) {
        this.tf = tf;
        this.pp = pp;
        initOptionsMap();
        expansionHooks = null;
    }

    /**
     * read_term
     * <p>
     * If true, read `...` to a string object (see section 5.2). The default depends on the Prolog flag back_quotes.
     *
     * @code backquoted_string(Bool)
     * Defines how to read \ escape sequences in quoted atoms. See the Prolog flag character_escapes in
     * current_prolog_flag/2. (SWI-Prolog).
     * @code character_escapes(Bool)
     * Unify Comments with a list of Position-Comment, where Position is a stream position object
     * (see stream_position_data/3) indicating the start of a comment and Comment is a string object containing
     * the text including delimiters of a comment.
     * It returns all comments from where the read_term/2 call started up to the end of the term read.
     * @code "comments(-Comments)"
     */
    private
    void initOptionsMap () {

        map.put("read_term", new HiTalkFlag[]{
                tf.createFlag(tf.createFlag("backquoted_string(Bool)"),

                        tf.createFlag("character_escapes(Bool)"),

                        tf.createFlag("comments(-Comments)"),

                        tf.createFlag("cycles(Bool)"),
// If true (default false), re-instantiate templates as produced by the corresponding write_term/2 option. Note that the default is false to avoid misinterpretation of @(Template, Substutions), while the default of write_term/2 is true because emitting cyclic terms without using the template construct produces an infinitely large term (read: it will generate an error after producing a huge amount of output).
                        tf.createFlag("dotlists(Bool)"),
// If true (default false), read .(a,[]) as a list, even if lists are internally nor constructed using the dot as functor. This is primarily intended to read the output from write_canonical/1 from other Prolog systems. See section 5.1.
                        tf.createFlag("double_quotes(Atom)"),
// Defines how to readtf.createFlag(" ...tf.createFlag(" strings. See the Prolog flag double_quotes. (SWI-Prolog).
                        tf.createFlag("module(Module)"),
// Specify Module for operators, character_escapes flag and double_quotes flag. The value of the latter two is overruled if the corresponding read_term/3 option is provided. If no module is specified, the current `source module' is used. (SWI-Prolog).
                        tf.createFlag("quasi_quotations(-List)"),
//    If present, unify List with the quasi quotations (see section A.33) instead of evaluating quasi quotations.
//    Each quasi quotation is a term quasi_quotation(+Syntax, +Quotation, +VarDict, -Result),
//    where Syntax is the term in {|Syntax||..|},
//    Quotation is a list of character codes that represent the quotation,
//    VarDict is a list of Name=Variable and
//    Result is a variable that shares with the place where the quotation must be inserted.
//    This option is intended to support tools that manipulate Prolog source text.
                        tf.createFlag("singletons(Vars)"),
//    As variable_names, but only reports the variables occurring only once in the Term read (ISO).
//    If Vars is the constant warning, singleton variables are reported using print_message/2.
//    The variables appear in the order they have been read. The latter option provides backward compatibility and
//    is used to read terms from source files. Not all singleton variables are reported as a warning.
//    See section 2.16.1.9 for the rules that apply for warning about a singleton variable.
                        tf.createFlag("syntax_errors(Atom)"),
//    If error (default), throw an exception on a syntax error.
//    Other values are fail, which causes a message to be printed using print_message/2, after which the predicate
//    fails, quiet which causes the predicate to fail silently, and dec10 which causes syntax errors to be printed,
//    after which read_term/[2,3] continues reading the next term.
//    Using dec10, read_term/[2,3] never fails. (Quintus, SICStus).
                        tf.createFlag("subterm_positions(TermPos)", new HiTalkFlag[]{

                        }),
//    Describes the detailed layout of the term. The formats for the various types of terms are given below.
//    All positions are character positions. If the input is related to a normal stream, these positions are relative
//    to the start of the input; when reading from the terminal, they are relative to the start of the term.

                        tf.createFlag("From-To", new HiTalkFlag[]{
//        Used for primitive types (atoms, numbers, variables).
tf.createFlag("string_position(From, To)"),
//        Used to indicate the position of a string enclosed in double quotes (").
tf.createFlag("brace_term_position(From, To, Arg)"),
//        Term of the form {...}, as used in DCG rules. Arg describes the argument.
tf.createFlag("list_position(From, To, Elms, Tail)"),
//        A list. Elms describes the positions of the elements. If the list specifies the tail as |<TailTerm>, Tail is
//        unified with the term position of the tail, otherwise with the atom none.
tf.createFlag("term_position(From, To, FFrom, FTo, SubPos)"),
//        Used for a compound term not matching one of the above. FFrom and FTo describe the position of the functor.
//        SubPos is a list, each element of which describes the term position of the corresponding subterm.
tf.createFlag("dict_position(From, To, TagFrom, TagTo, KeyValuePosList)"),
//        Used for a dict (see section 5.4),. The position of the key-value pairs is described by KeyValuePosList,
//        which is a list of key_value_position/7 terms. The key_value_position/7 terms appear in the order of the input.
//        Because maps to not preserve ordering, the key is provided in the position description.
tf.createFlag("key_value_position(From, To, SepFrom, SepTo, Key, KeyPos, ValuePos)"),
//        Used for key-value pairs in a map (see section 5.4). It is similar to the term_position/5 that would be created,
//        except that the key and value positions do not need an intermediate list and the key is provided in Key to
//        enable synchronisation of the file position data with the data structure.
tf.createFlag("parentheses_term_position(From, To, ContentPos)"),
//        Used for terms between parentheses. This is an extension compared to the original Quintus specification that
//        was considered necessary for secure refactoring of terms.
tf.createFlag("quasi_quotation_position(From, To, SyntaxFrom, SyntaxTo, ContentPos)"),
//        Used for quasi quotations.
tf.createFlag("term_position(Pos)"),
//    Unifies Pos with the starting position of the term read. Pos is of the same format as used by stream_property/2.
tf.createFlag("var_prefix(Bool)"),
//    If true, demand variables to start with an underscore. See section 2.16.1.7.
tf.createFlag("variables(Vars)"),
//    Unify Vars with a list of variables in the term. The variables appear in the order they have been read. See also term_variables/2. (ISO)"),.
tf.createFlag("variable_names(Vars)")
//    Unify Vars with a list of `Name = Var', where Name is an atom describing the variable name and Var is a variable that shares with the corresponding variable in Term. (ISO)"),. The variables appear in the order they have been read.

                        }),
                map.put("logtalk_compile", new HiTalkFlag[]{

                }),
                map.put("read_clause", new HiTalkFlag[]

                        {

                        }),
        map.put("load_files", new HiTalkFlag[]{
                "autoload(Bool)",        //  If true (default false), indicate that this load is a demand load. This implies that, depending on the setting of the Prolog flag verbose_autoload, the load action is printed at level informational or silent. See also print_message/2 and current_prolog_flag/2.
                "check_script(Bool)",     //If false (default true), do not check the first character to be # and skip the first line when found.
                "derived_from(File)",//      Indicate that the loaded file is derived from File. Used by make/0 to time-check and load the original file rather than the derived file.
                "dialect(+Dialect)",       //Load Files with enhanced compatibility with the target Prolog system identified by Dialect. See expects_dialect/1 and section C for details.
                "encoding(Encoding)",//      Specify the way characters are encoded in the file. Default is taken from the Prolog flag encoding. See section 2.19.1 for details.
                "expand(Bool)",//            If true, run the filenames through expand_file_name/2 and load the returned files. Default is false, except for consult/1 which is intended for interactive use. Flexible location of files is defined by file_search_path/2.
                "format(+Format)",     //    Used to specify the file format if data is loaded from a stream using the stream(Stream) option. Default is source, loading Prolog source text. If qlf, load QLF data (see qcompile/1).
                "if(Condition)",         // Load the file only if the specified condition is satisfied. The value true loads the file unconditionally, changed loads the file if it was not loaded before or has been modified since it was loaded the last time, and not_loaded loads the file if it was not loaded before.
                "imports(Import)",        // Specify what to import from the loaded module. The default for use_module/1 is all. Import is passed from the second argument of use_module/2. Traditionally it is a list of predicate indicators to import. As part of the SWI-Prolog/YAP integration, we also support Pred as Name to import a predicate under another name. Finally, Import can be the term except(Exceptions),
//                where Exceptions is a list of predicate indicators that specify predicates that are not imported or Pred as Name terms to denote renamed predicates.
//                See also reexport/2 and use_module/2.bug
//        If Import equals all, all operators are imported as well. Otherwise, operators are not imported. Operators can be imported selectively by adding terms op(Pri,Assoc,Name) to the Import list. If such a term is encountered, all exported operators that unify with this term are imported. Typically, this construct will be used with all arguments unbound to import all operators or with only Name bound to import a particular operator.
                "modified(TimeStamp)",
//                Claim that the source was loaded at TimeStamp without checking the source. This option is intended to be used together with the stream(Input) option, for example after extracting the time from an HTTP server or database.
                "module(+Module)",      //   Load the indicated file into the given module, overruling the module name specified in the :- module(Name, ...) directive. This currently serves two purposes: (1) allow loading two module files that specify the same module into the same process and force and (2): force loading source code in a specific module, even if the code provides its own module name. Experimental.
                "must_be_module(Bool)",   // If true, raise an error if the file is not a module file. Used by use_module/[1,2]."qcompile(Atom)         // How to deal with quick-load-file compilation by qcompile/1. Values are:

                "never",
//        Default. Do not use qcompile unless called explicitly.
                "auto",
//        Use qcompile for all writeable files. See comment below.
                "large",
//        Use qcompile if the file is `large'. Currently, files larger than 100 Kbytes are considered large.
                "part",

//                            If load_files/2 appears in a directive of a file that is compiled into Quick Load Format
//                            using qcompile/1, the contents of the argument files are included in the .qlf file instead
//                            of the loading directive.
//                            If this option is not present, it uses the value of the Prolog flag qcompile as default.
                "optimise(+Boolean)",
//                Explicitly set the optimization for compiling this module. See optimise.
                "redefine_module(+Action)",
//                Defines what to do if a file is loaded that provides a module that is already loaded from
//                            another file. Action is one of false (default), which prints an error and refuses to load
//                            the file, or true, which uses unload_file/1 on the old file and then proceeds loading
//                            the new file.
//                            Finally, there is ask, which starts interaction with the user. ask is only provided if
//                            the stream user_input is associated with a terminal.
//    reexport(Bool)          If true re-export the imported predicate. Used by reexport/1 and reexport/2.
//    register(Bool)          If false, do not register the load location and options. This option is used by make/0
//                            and load_hotfixes/1 to avoid polluting the load-context database. See source_file_property/2.
//    sandboxed(Bool)         Load the file in sandboxed mode. This option controls the flag sandboxed_load. The only
//                            meaningful value for Bool is true. Using false while the Prolog flag is set to true raises
//                            a permission error.
//    scope_settings(Bool)
//        Scope style_check/1 and expects_dialect/1 to the file and files loaded from the file after
//                            the directive. Default is true. The system and user initialization files (see -f and -F)
//                            are loading with scope_settings(false).
//    silent(Bool)            If true, load the file without printing a message. The specified value is the default for
//                            all files loaded as a result of loading the specified files. This option writes the Prolog
//                            flag verbose_load with the negation of Bool.
//    stream(Input)           This SWI-Prolog extension compiles the data from the stream Input. If this option is used,
//                            Files must be a single atom which is used to identify the source location of the loaded
//                            clauses as well as to remove all clauses if the data is reconsulted.
//                            This option is added to allow compiling from non-file locations such as databases, the web,
//                            the user (see consult/1) or other servers. It can be combined with format(qlf) to load
//                            QLF data from a stream.
//                            The load_files/2 predicate can be hooked to load other data or data from objects other than
//                            files. See prolog_load_file/2 for a description and library(http/http_load) for an example.
//                            All hooks for load_files/2 are documented in section B.8.

        });

        map.put("logtalk_load", new HiTalkFlag[]

                {

                });
        map.put("read_term_from_string", new HiTalkFlag[]

                {

                });
        map.put("read_term_1", new HiTalkFlag[]

                {

                });
        map.put("read_term+2", new HiTalkFlag[]

                {

                });
        map.put("read_term_3", new HiTalkFlag[]{
//                Read options: Options is a list of read options.
//                If this list contains contradictory options, the rightmost option is the one which applies.
//                Possible options are:
tf.createFlag("variables(VL)"),
//                VL is unified with the list of all variables of the input term, in left-to-right traversal order.
//                Anonymous variables are included in the list VL.
        });
        map.put("read_term_4", new HiTalkFlag[]

                {

                });
        map.put("read_term+5", new HiTalkFlag[]

                {

                });
        map.put("read_term=5", new HiTalkFlag[]

                {

                });
        map.put("read_term--0", new HiTalkFlag[]

                {

                });

    }

    private
    ITermFactory tf;
    private final HtPrologParser pp;
    private final List <LogtalkFlag.Hook> expansionHooks;
    private final Map <String, HiTalkFlag[]> map = new HashMap <>();

    //    private TermStack termStack;
    // hook1 standard reading hooks// begin_of_file/edd_of_file
    //hilog
    // hook3 binarization
    //userefinedhooks
//    IHiTalkParserWrapper parser = new HandWrittenParserWrapper(in, termStack);
//    Term t = readTerm(in, null);


    /**
     * Creates a term parser over an interner.
     *
     * @param tokenSource
     * @param interner    The interner to use to intern all functor and variable names.
     */

    public
    TermIO ( TokenSource tokenSource, VariableAndFunctorInterner interner ) {

        pp = new HtPrologParser(tokenSource, interner);
//       tf.createFlag("Prolog_Variable_Namespace", "Prolog_Functor_Namespace"

        expansionHooks = new ArrayList <>();
    }

//    @Nullable
//    public Term readTerm (List <RwOption> options ) {
//
//        Term t = readRawTerm(options);
//        List <LogtalkFlag.Hook> hook;
//        //  for (hook = expansionHooks; expansionHooks.nextToken; )
//        //  TermExpander expander = getTermExpander();
//        //t = expander.execute(t);
//
//        return t;
//    }
//        if(t.getName()==PrologConstants.IMPLIES)


    /**
     * @param options
     * @return
     */
    public
    Term readRawTerm ( HiTalkFlag[] options ) {
        try {

            Term rawTerm = pp.term();

            return applyOptions(rawTerm, options);
        } catch (SourceCodeException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    private
    Term applyOptions ( Term rawTerm, HiTalkFlag[] options ) {

    }

//    private ListOptions getReadTermDefaultOtions(){
//        return ;
//    }


//    public Term readTermFromString ( List <RwOption> options ) {
//
//        return null;
//    }
//
//    public Term readTermFromString ( String s, List <RwOption> options ) {
//        TokenSource  tokenSource = TokenSource.getTokenSourceForString(s);
//       return readRawTerm(options);
//    }

    public
    Clause readClause () {
        try {
            return pp.clause();
        } catch (SourceCodeException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }
}
