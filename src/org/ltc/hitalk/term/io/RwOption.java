package org.ltc.hitalk.term.io;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.LogtalkFlag;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public
class RwOption extends LogtalkFlag {

    package org.ltc.pscc.core.compiler;


import org.ltc.pscc.core.parser.ParseException;
import org.ltc.pscc.core.term.Term;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

    /**
     * read_term options
     * <p>
     * backquoted_string(Bool)
     * If true, read `...` to a string object (see section 5.2). The default depends on the Prolog flag back_quotes.
     * <p>
     * character_escapes(Bool)
     * Defines how to read \ escape sequences in quoted atoms. See the Prolog flag character_escapes in current_prolog_flag/2. (SWI-Prolog).
     * <p>
     * comments(-Comments)
     * Unify Comments with a list of Position-Comment, where Position is a stream position object (see stream_position_data/3) indicating the start of a comment and Comment is a string object containing the text including delimiters of a comment. It returns all comments from where the read_term/2 call started up to the end of the target read.
     * <p>
     * cycles(Bool)
     * If true (default false), re-instantiate templates as produced by the corresponding write_term/2 option. Note that the default is false to avoid misinterpretation of @(Template, Substutions), while the default of write_term/2 is true because emitting cyclic terms without using the template construct produces an infinitely large target (read: it will generate an error after producing a huge amount of output).
     * <p>
     * dotlists(Bool)
     * If true (default false), read .(a,[]) as a list, even if lists are internally nor constructed using the dot as functor. This is primarily intended to read the output from write_canonical/1 from other Prolog systems. See section 5.1.
     * <p>
     * double_quotes(Atom)
     * Defines how to read " ... " strings. See the Prolog flag double_quotes. (SWI-Prolog).
     * <p>
     * module(Module)
     * Specify Module for operators, character_escapes flag and double_quotes flag.
     * The value of the latter two is overruled if the corresponding read_term/3 option is provided.
     * If no module is specified, the current `source module' is used. (SWI-Prolog).
     * <p>
     * quasi_quotations(-List)
     * If present, unify List with the quasi quotations (see section A.33) instead of evaluating quasi quotations. Each quasi quotation is a target quasi_quotation(+Syntax, +Quotation, +VarDict, -Result), where Syntax is the target in {|Syntax||..|}, Quotation is a list of character codes that represent the quotation, VarDict is a list of Name=Variable and Result is a variable that shares with the place where the quotation must be inserted.
     * This option is intended to support tools that manipulate Prolog source text.
     * <p>
     * singletons(Vars)
     * As variable_names, but only reports the variables occurring only once in the Term read (ISO).
     * If Vars is the constant warning, singleton variables are reported using print_message/2.
     * The variables appear in the order they have been read.
     * The latter option provides backward compatibility and is used to read terms from source files.
     * Not all singleton variables are reported as a warning.
     * See section 2.16.1.9 for the rules that apply for warning about a singleton variable.
     * <p>
     * syntax_errors(Atom)
     * If error (default), throw an exception on a syntax error. Other values are fail, which causes a message to be printed using print_message/2, after which the predicate fails, quiet which causes the predicate to fail silently, and dec10 which causes syntax errors to be printed, after which read_term/[2,3] continues reading the next target. Using dec10, read_term/[2,3] never fails. (Quintus, SICStus).
     * <p>
     * subterm_positions(TermPos)
     * Describes the detailed layout of the target. The formats for the various types of terms are given below. All positions are character positions. If the input is related to a normal stream, these positions are relative to the start of the input; when reading from the terminal, they are relative to the start of the target.
     * From-To
     * Used for primitive types (atoms, numbers, variables).
     * <p>
     * string_position(From, To)
     * Used to indicate the position of a string enclosed in double quotes (").
     * <p>
     * brace_term_position(From, To, Arg)
     * Term of the form {...}, as used in DCG rules. Arg describes the argument.
     * <p>
     * list_position(From, To, Elms, Tail)
     * A list. Elms describes the positions of the elements. If the list specifies the tail as |<TailTerm>, Tail is unified with the target position of the tail, otherwise with the atom none.
     * <p>
     * term_position(From, To, FFrom, FTo, SubPos)
     * Used for a compound target not matching one of the above. FFrom and FTo describe the position of the functor. SubPos is a list, each element of which describes the target position of the corresponding subterm.
     * <p>
     * dict_position(From, To, TagFrom, TagTo, KeyValuePosList)
     * Used for a dict (see section 5.4). The position of the key-value pairs is described by KeyValuePosList, which is a list of key_value_position/7 terms. The key_value_position/7 terms appear in the order of the input. Because maps to not preserve ordering, the key is provided in the position description.
     * <p>
     * key_value_position(From, To, SepFrom, SepTo, Key, KeyPos, ValuePos)
     * Used for key-value pairs in a map (see section 5.4). It is similar to the term_position/5 that would be created, except that the key and value positions do not need an intermediate list and the key is provided in Key to enable synchronisation of the file position data with the data structure.
     * <p>
     * parentheses_term_position(From, To, ContentPos)
     * Used for terms between parentheses. This is an extension compared to the original Quintus specification that was considered necessary for secure refactoring of terms.
     * quasi_quotation_position(From, To, SyntaxFrom, SyntaxTo, ContentPos)
     * Used for quasi quotations.
     * <p>
     * term_position(Pos)
     * Unifies Pos with the starting position of the target read. Pos is of the same format as used by stream_property/2.
     * <p>
     * var_prefix(Bool)
     * If true, demand variables to start with an underscore. See section 2.16.1.7.
     * variables(Vars)
     * Unify Vars with a list of variables in the target. The variables appear in the order they have been read. See also term_variables/2. (ISO).
     * <p>
     * variable_names(Vars)
     * Unify Vars with a list of `Name = Var', where Name is an atom describing the variable name and Var is a variable that shares with the corresponding variable in Term. (ISO). The variables appear in the order they have been read.
     * <p>
     * end_of_term(dot/eof): specifies the end-of-target delimiter: --------------------> GNU PROLOG
     * dot is the classical full-stop delimiter (a dot followed with a layout character),
     * eof is the end-of-file delimiter.
     * This option is useful for predicates like read_term_from_atom/3 (section 8.15.1)
     * to avoid to add a terminal dot at the end of the atom.
     * The default value is dot.
     * <p>
     * write_term options
     * <p>
     * attributes(Atom)
     * Define how attributed variables (see section 8.1) are written.
     * The default is determined by the Prolog flag write_attributes.
     * Defined values are
     * ignore (ignore the attribute),
     * dots (write the attributes as {...}),
     * write (simply hand the attributes recursively to write_term/2) and
     * portray (hand the attributes to attr_portray_hook/2).
     * <p>
     * back_quotes(Atom)
     * Fulfills the same role as the back_quotes prolog flag.
     * Notably, the value string causes string objects to be printed between back quotes and symbol_char causes the backquote to be printed unquoted.
     * In all other cases the backquote is printed as a quoted atom.
     * <p>
     * brace_terms(Bool)
     * If true (default), write {}(X) as {X}. See also dotlists and ignore_ops.
     * <p>
     * blobs(Atom)
     * Define how non-text blobs are handled. By default, this is left to the write handler specified with the blob type. Using portray, portray/1 is called for each blob encountered. See section 12.4.8.
     * <p>
     * character_escapes(Bool)
     * If true and quoted(true) is active, special characters in quoted atoms and strings are emitted as ISO escape sequences. Default is taken from the reference module (see below).
     * <p>
     * cycles(Bool)
     * If true (default), cyclic terms are written as @(Template, Substitutions), where Substitutions is a list Var = Value. If cycles is false, max_depth is not given, and Term is cyclic, write_term/2 raises a domain_error.94 See also the cycles option in read_term/2.
     * <p>
     * dotlists(Bool)
     * If true (default false), write lists using the dotted target notation rather than the list notation.95 Note that as of version 7, the list constructor is '[|]'. Using dotlists(true), write_term/2 writes a list using `.' as constructor. This is intended for communication with programs such as other Prolog systems, that rely on this notation.
     * <p>
     * fullstop(Bool)
     * If true (default false), add a fullstop token to the output. The dot is preceeded by a space if needed and followed by a space (default) or newline if the nl(true) option is also given.96
     * <p>
     * ignore_ops(Bool)
     * If true, the generic target representation (<functor>(<args> ... )) will be used for all terms. Otherwise (default), operators will be used where appropriate.97.
     * <p>
     * max_depth(Integer)
     * If the target is nested deeper than Integer, print the remainder as ellipses ( ... ). A 0 (zero) value (default) imposes no depth limit. This option also delimits the number of printed items in a list. Example:
     * ?- write_term(a(s(s(s(s(0)))), [a,b,c,d,e,f]),
     * [max_depth(3)]).
     * a(s(s(...)), [a, b|...])
     * true.
     * Used by the top level and debugger to limit screen output. See also the Prolog flags answer_write_options and debugger_write_options.
     * <p>
     * module(Module)
     * Define the reference module (default user). This defines the default value for the character_escapes option as well as the operator definitions to use. See also op/3.
     * <p>
     * nl(Bool)
     * Add a newline to the output. See also the fullstop option.
     * <p>
     * numbervars(Bool)
     * If true, terms of the format $VAR(N), where N is a non-negative integer, will be written as a variable name. If N is an atom it is written without quotes. This extension allows for writing variables with user-provided names. The default is false. See also numbervars/3 and the option variable_names.
     * partial(Bool)
     * If true (default false), do not reset the logic that inserts extra spaces that separate tokens where needed. This is intended to solve the problems with the code below. Calling write_value(.) writes .., which cannot be read. By adding partial(true) to the option list, it correctly emits . .. Similar problems appear when emitting operators using multiple calls to write_term/3.
     * write_value(Value) :-
     * write_term(Value, [partial(true)]),
     * write('.'), nl.
     * <p>
     * portray(Bool)
     * Same as portrayed(Bool). Deprecated.
     * portray_goal(:Goal)
     * Implies portray(true), but calls Goal rather than the predefined hook portray/1. Goal is called through call/3, where the first argument is Goal, the second is the target to be printed and the 3rd argument is the current write option list. The write option list is copied from the write_term call, but the list is guaranteed to hold an option priority that reflects the current priority.
     * <p>
     * portrayed(Bool)
     * If true, the hook portray/1 is called before printing a target that is not a variable. If portray/1 succeeds, the target is considered printed. See also print/1. The default is false. This option is an extension to the ISO write_term options.
     * priority(Integer)
     * An integer between 0 and 1200 representing the `context priority'. Default is 1200. Can be used to write partial terms appearing as the argument to an operator. For example:
     * format('~w = ', [VarName]),
     * write_term(Value, [quoted(true), priority(699)])
     * <p>
     * quoted(Bool)
     * If true, atoms and functors that need quotes wil3l be quoted. The default is false.
     * <p>
     * spacing(+Spacing)
     * Determines whether and where extra white space is added to enhance readability. The default is standard, adding only space where needed for proper tokenization by read_term/3. Currently, the only other value is next_argument, adding a space after a comma used to separate arguments in a target or list.
     * variabermes(+List)
     * Assign names to variables in Term. List is a list of terms Name = Var, where Name is an atom that represents a valid Prolog variable name. Terms where Var is bound or is a variable that does not appear in Term are ignored. Raises an error if List is not a list, one of the members is not a target Name = Var, Name is not an atom or Name does not represent a valid Prolog variable name.
     * The implementation binds the variables from List to a target '$VAR'(Name).
     * Like write_canonical/1, terms that where already bound to '$VAR'(X) before write_term/2 are printed normally, unless the option numbervars(true) is also provided. If the option numbervars(true) is used,
     * the user is responsible for avoiding collisions between assigned names and numbered names.
     * See also the variable_names option of read_term/2.
     * <p>
     * Possible variable attributes (see section 8.1) are ignored. In most cases one should use copy_term/3 to obtain a copy that is free of attributed variables and handle the associated constraints as appropriate for the use-case.
     */

    enum Options {
        ;

        /**
         * Equivalent to read_term/3,
         * but sets options according to the current compilation context and optionally processes comments.
         * Defined options:
         * syntax_errors(+Atom)
         * See read_term/3, but the default is dec10 (report and restart).
         * term_position(-TermPos)
         * Same as for read_term/3.
         * subterm_positions(-TermPos)
         * Same as for read_term/3.
         * variable_names(-Bindings)
         * Same as for read_term/3.
         * process_comment(+Boolean)
         * If true (default), call prolog:comment_hook(Comments, TermPos, Term) if this multifile hook is defined (see prolog:comment_hook/3). This is used to drive PlDoc.
         * comments(-Comments)
         * If provided, unify Comments with the comments encountered while reading Term. This option implies process_comment(false).
         * The singletons option of read_term/3 is initialised from the active style-checking mode. The module option is initialised to the current compilation module (see prolog_load_context/2).
         */
//        READ_CLAUSE_OPTIONS(options -> {
//            return getReadClauseOptions();
//        }),
//        READ_TERM_OPTIONS(options -> {
//            return getReadTermOptions();
//        }),
//        WRITE_TERM_OPTIONS(options -> {
//            return getWriteTermOptions();
//        });

//
//        private static List <RwOption> writeTermOptions =
//
//        }

        private static List <RwOption> readTermOptions;
        private static List <RwOption> readClauseOptions;
        private static List <RwOption> writeTermOptions;

        public static
        List <RwOption> getReadClauseOptions () {
            return readClauseOptions;
        }

        public static
        List <RwOption> getReadTermOptions () {
            return readTermOptions;
        }

        public static
        List <RwOption> getWriteTermOptions () {
            return writeTermOptions;
        }

        public
        List <RwOption> readOption ( TermReader reader, String s ) throws IOException, ParseException {
            Term t = reader.readTermFromString(s, Collections.emptyList());
            Term name = t.getName();
            Term args = t.getArgs();

            return null;
        }

        private
        Term readTermFromString ( String s ) {
            return null;
        }
    }

    /**
     * @param flag
     * @param flagValue
     */
    public
    RwOption ( Term flag, Term flagValue ) {
        this.flag = flag;
        this.flagValue = flagValue;
    }

    public
    Term getFlag () {
        return flag;
    }

    public
    Term getFlagValue () {
        return flagValue;
    }

    private final Term flag;
    private final Term flagValue;
}


}
