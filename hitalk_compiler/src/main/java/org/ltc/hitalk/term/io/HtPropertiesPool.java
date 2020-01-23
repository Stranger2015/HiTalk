package org.ltc.hitalk.term.io;

import org.ltc.hitalk.entities.HtProperty;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class HtPropertiesPool {
//    io props
    /**
     * TERM READING AND WRITING
     * <p>
     * This section describes the basic term reading and writing predicates.
     * The predicates format/[1,2] and writef/2 provide formatted output.
     * Writing to Prolog data structures such as atoms or code-lists is supported by with_output_to/2 and format/3.
     * <p>
     * Reading is sensitive to the Prolog flag character_escapes, which controls the interpretation of the \ character in
     * quoted atoms and strings.
     * <p>
     * [ISO]write_term(+Term, +Options)
     * The predicate write_term/2 is the generic form of all Prolog term-write predicates. Valid options are:
     * <p>
     * attributes(Atom)
     * Define how attributed variables (see section 8.1) are written. The default is determined by the Prolog flag write_attributes. Defined values are ignore (ignore the attribute), dots (write the attributes as {...}), write (simply hand the attributes recursively to write_term/2) and portray (hand the attributes to attr_portray_hook/2).
     * <p>
     * back_quotes(Atom)
     * Fulfills the same role as the back_quotes prolog flag. Notably, the value string causes string objects to be printed between back quotes and symbol_char causes the backquote to be printed unquoted. In all other cases the backquote is printed as a quoted atom.
     * <p>
     * brace_terms(Bool)
     * If true (default), write {}(X) as {X}. See also dotlists and ignore_ops.
     * <p>
     * blobs(Atom)
     * Define how non-text blobs are handled. By default, this is left to the write handler specified with the blob type.
     * Using portray, portray/1 is called for each blob encountered. See section 12.4.8. character_escapes(Bool)
     * If true and quoted(true) is active, special characters in quoted atoms and strings are emitted as ISO escape sequences.
     * Default is taken from the reference module (see below).
     * <p>
     * cycles(Bool)
     * If true (default), cyclic terms are written as @(Template, Substitutions), where Substitutions is a list Var = Value.
     * If cycles is false, max_depth is not given, and Term is cyclic, write_term/2 raises a domain_error.
     * See also the cycles option in read_term/2.
     * <p>
     * dotlists(Bool)
     * <p>
     * If true (default false), write lists using the dotted term notation rather than the list notation.
     * Note that as of version 7, the list constructor is '[|]'.
     * Using dotlists(true), write_term/2 writes a list using `.' as constructor.
     * This is intended for communication with programs such as other Prolog systems, that rely on this notation. See also the option no_lists(true) to use the actual SWI-Prolog list functor.
     * <p>
     * fullstop(Bool)
     * <p>
     * If true (default false), add a fullstop token to the output. The dot is preceeded by a space if needed and followed by a space (default) or newline if the nl(true) option is also given.99
     * <p>
     * ignore_ops(Bool)
     * <p>
     * If true, the generic term representation (<functor>(<args> ... )) will be used for all terms.
     * Otherwise (default), operators will be used where appropriate.
     * <p>
     * max_depth(Integer)
     * <p>
     * If the term is nested deeper than Integer, print the remainder as ellipses ( ... ).
     * A 0 (zero) value (default) imposes no depth limit. This option also delimits the number of printed items in a list.
     * Example:
     * <p>
     * ?- write_term(a(s(s(s(s(0)))), [a,b,c,d,e,f]),
     * [max_depth(3)]).
     * a(s(s(...)), [a, b|...])
     * true.
     * <p>
     * Used by the top level and debugger to limit screen output. See also the Prolog flags answer_write_options and debugger_write_options.
     * <p>
     * module(Module)
     * <p>
     * Define the reference module (default user).
     * This defines the default value for the character_escapes option as well as the operator definitions to use.
     * See also op/3.
     * <p>
     * nl(Bool)
     * <p>
     * Add a newline to the output. See also the fullstop option.
     * <p>
     * no_lists(Bool)
     * Do not use list notation. This is similar to dotlists(true), but uses the SWI-Prolog list functor, which is by
     * default '[|]' instead of the ISO Prolog '.'. Used by display/1.
     * <p>
     * numbervars(Bool)
     * <p>
     * If true, terms of the format $VAR(N), where N is a non-negative integer, will be written as a variable name.
     * If N is an atom it is written without quotes. This extension allows for writing variables with user-provided names.
     * The default is false. See also numbervars/3 and the option variable_names.
     * <p>
     * partial(Bool)
     * <p>
     * If true (default false), do not reset the logic that inserts extra spaces that separate tokens where needed.
     * This is intended to solve the problems with the code below. Calling write_value(.) writes .., which cannot
     * be read. By adding partial(true) to the option list, it correctly emits . ..
     * Similar problems appear when emitting operators using multiple calls to write_term/3.
     * <p>
     * write_value(Value) :-
     * write_term(Value, [partial(true)]),
     * write('.'), nl.
     * <p>
     * portray(Bool)
     * Same as portrayed(Bool). Deprecated.
     * portray_goal(:Goal)
     * Implies portray(true), but calls Goal rather than the predefined hook portray/1. Goal is called through call/3, where the first argument is Goal, the second is the term to be printed and the 3rd argument is the current write option list. The write option list is copied from the write_term call, but the list is guaranteed to hold an option priority that reflects the current priority.
     * <p>
     * portrayed(Bool)
     * <p>
     * If true, the hook portray/1 is called before printing a term that is not a variable. If portray/1 succeeds,
     * the term is considered printed. See also print/1. The default is false. This option is an extension to the ISO write_term options.
     * <p>
     * priority(Integer)
     * <p>
     * An integer between 0 and 1200 representing the `context priority'. Default is 1200. Can be used to write partial
     * terms appearing as the argument to an operator. For example:
     * <p>
     * format('~w = ', [VarName]),
     * write_term(Value, [quoted(true), priority(699)])
     * <p>
     * <p>
     * quoted(Bool)
     * <p>
     * If true, atoms and functors that need quotes will be quoted. The default is false.
     * <p>
     * spacing(+Spacing)
     * <p>
     * Determines whether and where extra white space is added to enhance readability. The default is standard, adding only space where needed for proper tokenization by read_term/3. Currently, the only other value is next_argument, adding a space after a comma used to separate arguments in a term or list.
     * <p>
     * variable_names(+List)
     * <p>
     * Assign names to variables in Term. List is a list of terms Name = Var, where Name is an atom that represents a valid Prolog variable name. Terms where Var is bound or is a variable that does not appear in Term are ignored. Raises an error if List is not a list, one of the members is not a term Name = Var, Name is not an atom or Name does not represent a valid Prolog variable name.
     * <p>
     * The implementation binds the variables from List to a term '$VAR'(Name).
     * Like write_canonical/1, terms that where already bound to '$VAR'(X) before write_term/2 are printed normally,
     * unless the option numbervars(true) is also provided. If the option numbervars(true) is used, the user is
     * responsible for avoiding collisions between assigned names and numbered names.
     * See also the variable_names option of read_term/2.
     * <p>
     * Possible variable attributes (see section 8.1) are ignored. In most cases one should use copy_term/3 to obtain a copy that is free of attributed variables and handle the associated constraints as appropriate for the use-case.
     * <p>
     * ============================================================================
     * [ISO]write_term(+Stream, +Term, +Options)
     * <p>
     * As write_term/2, but output is sent to Stream rather than the current output.
     * <p>
     * [semidet]write_length(+Term, -Length, +Options)
     * <p>
     * True when Length is the number of characters emitted for write_termTerm,
     * Options . In addition to valid options for write_term/2, it processes the option:
     * <p>
     * max_length(+MaxLength)
     * <p>
     * If provided, fail if Length would be larger than MaxLength. The implementation ensures that the runtime is limited when computing the length of a huge term with a bounded maximum.
     * <p>
     * [ISO]write_canonical(+Term)
     * <p>
     * Write Term on the current output stream using com.sun.tools.doclets.formats.html.resources.standard parenthesised prefix notation (i.e., ignoring operator declarations). Atoms that need quotes are quoted. Terms written with this predicate can always be read back, regardless of current operator declarations. Equivalent to write_term/2 using the options ignore_ops, quoted and numbervars after numbervars/4 using the singletons option.
     * <p>
     * Note that due to the use of numbervars/4, non-ground terms must be written using a single write_canonical/1 call.
     * This used to be the case anyhow, as garbage collection between multiple calls to one of the write predicates can change the _G<NNN> identity of the variables.
     * <p>
     * [ISO]write_canonical(+Stream, +Term)
     * Write Term in canonical form on Stream.
     * <p>
     * [ISO]write(+Term)
     * Write Term to the current output, using brackets and operators where appropriate.
     * <p>
     * [ISO]write(+Stream, +Term)
     * Write Term to Stream.
     * <p>
     * [ISO]writeq(+Term)
     * Write Term to the current output, using brackets and operators where appropriate. Atoms that need quotes are quoted. Terms written with this predicate can be read back with read/1 provided the currently active operator declarations are identical.
     * <p>
     * [ISO]writeq(+Stream, +Term)
     * Write Term to Stream, inserting quotes.
     * <p>
     * writeln(+Term)
     * Equivalent to write(Term), nl.. The output stream is locked, which implies no output from other threads can
     * appear between the term and newline.
     * <p>
     * writeln(+Stream, +Term)
     * Equivalent to write(Stream, Term), nl(Stream).. The output stream is locked, which implies no output from other threads can appear between the term and newline.
     * print(+Term)
     * Print a term for debugging purposes. The predicate print/1 acts as if defined as below.
     * <p>
     * print(Term) :-
     * current_prolog_flag(print_write_options, Options), !,
     * write_term(Term, Options).
     * print(Term) :-
     * write_term(Term, [ portray(true),
     * numbervars(true),
     * quoted(true)
     * ]).
     * <p>
     * The print/1 predicate is used primarily through the ~p escape sequence of format/2, which is commonly used in the recipies used by print_message/2 to emit messages.
     * <p>
     * The classical definition of this predicate is equivalent to the ISO predicate write_term/2 using the options portray(true) and numbervars(true). The portray(true) option allows the user to implement application-specific printing of terms printed during debugging to facilitate easy understanding of the output. See also portray/1 and library(portray_text). SWI-Prolog adds quoted(true) to (1) facilitate the copying/pasting of terms that are not affected by portray/1 and to (2) allow numbers, atoms and strings to be more easily distinguished, e.g., 42, '42' and "42".
     * print(+Stream, +Term)
     * Print Term to Stream.
     * portray(+Term)
     * A dynamic predicate, which can be defined by the user to change the behaviour of print/1 on (sub)terms.
     * For each subterm encountered that is not a variable print/1 first calls portray/1 using the term as argument.
     * For lists, only the list as a whole is given to portray/1. If portray/1 succeeds print/1 assumes the term has
     * been written.
     * ===============================================================================================================
     * <p>
     * [ISO]read(-Term)
     * Read the next Prolog term from the current input stream and unify it with Term. On reaching end-of-file Term is unified with the atom end_of_file. This is the same as read_term/2 using an empty option list.
     * <p>
     * [NOTE] You might have found this while looking for a predicate to read input from a file or the user. Quite likely this is not what you need in this case. This predicate is for reading a Prolog term which may span multiple lines and must end in a full stop (dot character followed by a layout character). The predicates for reading and writing Prololg terms are particularly useful for storing Prolog data in a file or transferring them over a network communication channel (socket) to another Prolog process. The libraries provide a wealth of predicates to read data in other formats. See e.g., library(readutil), library(pure_input) or libraries from the extension packages to read XML, JSON, YAML, etc.
     * <p>
     * [ISO]read(+Stream, -Term)
     * Read the next Prolog term from Stream. See read/1 and read_term/2 for details.
     * <p>
     * read_clause(+Stream, -Term, +Options)
     * Equivalent to read_term/3, but sets options according to the current compilation context and optionally processes
     * comments. Defined options:
     * <p>
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
     * <p>
     * The singletons option of read_term/3 is initialised from the active style-checking mode. The module option is initialised to the current compilation module (see prolog_load_context/2).
     * <p>
     * [ISO]read_term(-Term, +Options)
     * Read a term from the current input stream and unify the term with Term. The reading is controlled by options from the list of Options. If this list is empty, the behaviour is the same as for read/1. The options are upward compatible with Quintus Prolog. The argument order is according to the ISO standard. Syntax errors are always reported using exception-handling (see catch/3). Options:
     * <p>
     * backquoted_string(Bool)
     * If true, read `...` to a string object (see section 5.2). The default depends on the Prolog flag back_quotes.
     * character_escapes(Bool)
     * Defines how to read \ escape sequences in quoted atoms. See the Prolog flag character_escapes in current_prolog_flag/2. (SWI-Prolog).
     * comments(-Comments)
     * Unify Comments with a list of Position-Comment, where Position is a stream position object (see stream_position_data/3) indicating the start of a comment and Comment is a string object containing the text including delimiters of a comment. It returns all comments from where the read_term/2 call started up to the end of the term read.
     * cycles(Bool)
     * If true (default false), re-instantiate templates as produced by the corresponding write_term/2 option. Note that the default is false to avoid misinterpretation of @(Template, Substutions), while the default of write_term/2 is true because emitting cyclic terms without using the template construct produces an infinitely large term (read: it will generate an error after producing a huge amount of output).
     * dotlists(Bool)
     * If true (default false), read .(a,[]) as a list, even if lists are internally nor constructed using the dot as functor. This is primarily intended to read the output from write_canonical/1 from other Prolog systems. See section 5.1.
     * double_quotes(Atom)
     * Defines how to read " ... " strings. See the Prolog flag double_quotes. (SWI-Prolog).
     * module(Module)
     * Specify Module for operators, character_escapes flag and double_quotes flag. The value of the latter two is overruled if the corresponding read_term/3 option is provided. If no module is specified, the current `source module' is used. (SWI-Prolog).
     * quasi_quotations(-List)
     * If present, unify List with the quasi quotations (see section A.34) instead of evaluating quasi quotations. Each quasi quotation is a term quasi_quotation(+Syntax, +Quotation, +VarDict, -Result), where Syntax is the term in {|Syntax||..|}, Quotation is a list of character codes that represent the quotation, VarDict is a list of Name=Variable and Result is a variable that shares with the place where the quotation must be inserted. This option is intended to support tools that manipulate Prolog source text.
     * singletons(Vars)
     * As variable_names, but only reports the variables occurring only once in the Term read (ISO). If Vars is the constant warning, singleton variables are reported using print_message/2. The variables appear in the order they have been read. The latter option provides backward compatibility and is used to read terms from source files. Not all singleton variables are reported as a warning. See section 2.17.1.9 for the rules that apply for warning about a singleton variable.101
     * syntax_errors(Atom)
     * If error (default), throw an exception on a syntax error. Other values are fail, which causes a message to be printed using print_message/2, after which the predicate fails, quiet which causes the predicate to fail silently, and dec10 which causes syntax errors to be printed, after which read_term/[2,3] continues reading the next term. Using dec10, read_term/[2,3] never fails. (Quintus, SICStus).
     * subterm_positions(TermPos)
     * Describes the detailed layout of the term. The formats for the various types of terms are given below. All positions are character positions. If the input is related to a normal stream, these positions are relative to the start of the input; when reading from the terminal, they are relative to the start of the term.
     * <p>
     * From-To
     * Used for primitive types (atoms, numbers, variables).
     * string_position(From, To)
     * Used to indicate the position of a string enclosed in double quotes (").
     * brace_term_position(From, To, Arg)
     * Term of the form {...}, as used in DCG rules. Arg describes the argument.
     * list_position(From, To, Elms, Tail)
     * A list. Elms describes the positions of the elements. If the list specifies the tail as |<TailTerm>, Tail is unified with the term position of the tail, otherwise with the atom none.
     * term_position(From, To, FFrom, FTo, SubPos)
     * Used for a compound term not matching one of the above. FFrom and FTo describe the position of the functor. SubPos is a list, each element of which describes the term position of the corresponding subterm.
     * dict_position(From, To, TagFrom, TagTo, KeyValuePosList)
     * Used for a dict (see section 5.4). The position of the key-value pairs is described by KeyValuePosList, which is a list of key_value_position/7 terms. The key_value_position/7 terms appear in the order of the input. Because maps to not preserve ordering, the key is provided in the position description.
     * key_value_position(From, To, SepFrom, SepTo, Key, KeyPos, ValuePos)
     * Used for key-value pairs in a map (see section 5.4). It is similar to the term_position/5 that would be created, except that the key and value positions do not need an intermediate list and the key is provided in Key to enable synchronisation of the file position data with the data structure.
     * parentheses_term_position(From, To, ContentPos)
     * Used for terms between parentheses. This is an extension compared to the original Quintus specification that was considered necessary for secure refactoring of terms.
     * quasi_quotation_position(From, To, SyntaxFrom, SyntaxTo, ContentPos)
     * Used for quasi quotations.
     * <p>
     * term_position(javafx.geometry.Pos)
     * Unifies Pos with the starting position of the term read. Pos is of the same format as used by stream_property/2.
     * var_prefix(Bool)
     * If true, demand variables to start with an underscore. See section 2.17.1.7.
     * variables(Vars)
     * Unify Vars with a list of variables in the term. The variables appear in the order they have been read. See also term_variables/2. (ISO).
     * variable_names(Vars)
     * Unify Vars with a list of `Name = Var', where Name is an atom describing the variable name and Var is a variable that shares with the corresponding variable in Term. (ISO). The variables appear in the order they have been read.
     * <p>
     * <p>
     * [ISO]read_term(+Stream, -Term, +Options)
     * Read term with options from Stream. See read_term/2.
     * <p>
     * read_term_from_atom(+Atom, -Term, +Options)
     * <p>
     * Use read_term/3 to read the next term from Atom. Atom is either an atom or a string object (see section 5.2).
     * It is not required for Atom to end with a full-stop. This predicate supersedes atom_to_term/3.
     * read_history(+Show, +Help, +Special, +Prompt, -Term, -Bindings)
     * Similar to read_term/2 using the option variable_names, but allows for history substitutions. read_history/6 is
     * used by the top level to read the user's actions.
     * Show is the command the user should type to show the saved events. Help is the command to get an overview of the
     * capabilities. Special is a list of commands that are not saved in the history. Prompt is the first prompt given.
     * Continuation prompts for more lines are determined by prompt/2. A %w in the prompt is substituted by the event
     * number. See section 2.8 for available substitutions.
     * <p>
     * SWI-Prolog calls read_history/6 as follows:
     * <p>
     * read_history(h, '!h', [trace], '%w ?- ', Goal, Bindings)
     * <p>
     * prompt(-Old, +New)
     * Set prompt associated with read/1 and its derivatives. Old is first unified with the current prompt.
     * On success the prompt will be set to New if this is an atom. Otherwise an error message is displayed.
     * A prompt is printed if one of the read predicates is called and the cursor is at the left margin.
     * It is also printed whenever a newline is given and the term has not been terminated.
     * Prompts are only printed when the current input stream is user.
     * <p>
     * prompt1(+Prompt)
     * Sets the prompt for the next line to be read. Continuation lines will be read using the prompt defined by prompt/2.
     */
    private Set<HtProperty> set = new HashSet<>();
    private Map<Class<?>, HtProperty> map1 = new HashMap<>();

    public void add(HtProperty property) {
        set.add(property);
    }

    public void lookup(HtProperty property) {
//        if (!set.contains(property)) {
        add(property);
//        }
    }

    public void addClassMapping(Class<?> clazz, HtProperty property) {
        map1.put(clazz, property);
    }

    public boolean ownsProperty(Class<?> clazz, HtProperty property) {
        return map1.containsKey(clazz) && map1.containsValue(property);
    }
}