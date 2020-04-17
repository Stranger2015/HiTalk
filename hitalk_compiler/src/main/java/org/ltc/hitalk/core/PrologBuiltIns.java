package org.ltc.hitalk.core;

import org.ltc.hitalk.term.ListTerm;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 *
 */
public
enum PrologBuiltIns {

    PUBLIC("public", PrologBuiltIns::_public),

    PROTECTED("protected", PrologBuiltIns::_protected),

    PRIVATE("private", PrologBuiltIns::_private),

    INCLUDE("include", PrologBuiltIns::include),

    MULTIFILE("multifile", PrologBuiltIns::multifile),
    DISCONTIGUOUS("discontiguous", PrologBuiltIns::discontiguous),
    DYNAMIC("dynamic", PrologBuiltIns::dynamic),
    STATIC("static", PrologBuiltIns::_static),
    HILOG("hilog", PrologBuiltIns::hilog),
    ENCODING("encoding", PrologBuiltIns::encoding),
    TEXT("text", Collections::singletonList),
    ISO_LATIN_1("iso_latin_1", Collections::singletonList),
    UTF8("utf8", Collections::singletonList),
    EXPAND_TERM("expand_term", PrologBuiltIns::expand_term),
    EXPAND_GOAL("expand_goal", PrologBuiltIns::expand_goal),
    DCG_EXPAND("dcg_expand", PrologBuiltIns::dcg_expand),

    LOGTALK_LIBRARY_PATH("logtalk_library_path", Collections::singletonList),

    OBJECT_PROPERTY("object_property", Collections::singletonList),
    CATEGORY_PROPERTY("category_property", Collections::singletonList),
    PROTOCOL_PROPERTY("protocol_property", Collections::singletonList),

    COLON_COLON("::", Collections::singletonList),
    UP_UP("^^", Collections::singletonList),
    COMMA(",", Collections::singletonList),
    COLON(":", Collections::singletonList),
    SEMICOLON(";", Collections::singletonList),
    CUT("!", PrologBuiltIns::cut),
    IMPLIES(":-", PrologBuiltIns::implies),
    DCG_IMPLIES("-->", PrologBuiltIns::dcg_imnplies),
    IF("->", PrologBuiltIns::if_then),
    IF_STAR("*->", Collections::singletonList),
    PLUS("+", Collections::singletonList),
    MINUS("-", Collections::singletonList),
    MULT("*", Collections::singletonList),
    DIV("/", Collections::singletonList),
    AT("@", Collections::singletonList),
    PLUS_PLUS("++", Collections::singletonList),
    MINUS_MINUS("--", Collections::singletonList),
    QUESTION("?", Collections::singletonList),
    QUERY("?-", Collections::singletonList),
    L_SHIFT("<<", Collections::singletonList),
    R_SHIFT(">>", Collections::singletonList),
    AS("as", Collections::singletonList),

//    BYPASS("{}", Collections::singletonList),

    INITIALIZATION("initialization", PrologBuiltIns::initialization),

    OP("op", PrologBuiltIns::op),
    CURRENT_OP("current_op", PrologBuiltIns::current_op),

    TRUE("true", PrologBuiltIns::_true),
    FAIL("fail", PrologBuiltIns::fail),
    FALSE("false", PrologBuiltIns::fail),
    NOT("\\+", PrologBuiltIns::not),
    UNIFIES("=", PrologBuiltIns::unify),
    UNIV("=..", PrologBuiltIns::univ),
    IS("is", PrologBuiltIns::is),
    NON_UNIFIES("\\=", PrologBuiltIns::not_unify),
    ASSIGN(":=", PrologBuiltIns::assign),
    CALL("call", PrologBuiltIns::call),
    OBJECT("object", Collections::singletonList),
    NIL("nil", Collections::singletonList),
    CONS("cons", Collections::singletonList),
    PROTOCOL("protocol", Collections::singletonList),
    CATEGORY("category", Collections::singletonList),
    ENUMERATION("enumeration", Collections::singletonList),
    IDENTICAL("==", Collections::singletonList),
    NON_IDENTICAL("\\==", Collections::singletonList),
    CLASS("class", Collections::singletonList),

    END_OBJECT("end_object", Collections::singletonList),
    END_PROTOCOL("end_protocol", Collections::singletonList),
    END_CATEGORY("end_category", Collections::singletonList),

    CURRENT_OBJECT("create_object", Collections::singletonList),
    CURRENT_CATEGORY("create_category", Collections::singletonList),
    CURRENT_PROTOCOL("create_protocol", Collections::singletonList),

    CREATE_OBJECT("create_object", Collections::singletonList),
    CREATE_CATEGORY("create_category", Collections::singletonList),
    CREATE_PROTOCOL("create_protocol", Collections::singletonList),

    ABOLISH_OBJECT("abolish_object", Collections::singletonList),
    ABOLISH_CATEGORY("abolish_category", Collections::singletonList),
    ABOLISH_PROTOCOL("abolish_protocol", Collections::singletonList),

    IMPLEMENTS_PROTOCOL("implements_protocol", Collections::singletonList),
    IMPORTS_CATEGORY("imports_category", Collections::singletonList),
    INSTANTIATES_CLASS("instantiates_class", Collections::singletonList),
    SPECIALIZES_CLASS("specializes_class", Collections::singletonList),
    EXTENDS_PROTOCOL("extends_protocol", Collections::singletonList),
    EXTENDS_OBJECT("extends_object", Collections::singletonList),
    EXTENDS_CATEGORY("extends_category", Collections::singletonList),

    COMPLEMENTS_OBJECT("complements_object", Collections::singletonList),

    CONFORMS_TO_PROTOCOL("conforms_to_protocol", Collections::singletonList),

    ABOLISH_EVENTS("abolish_events", Collections::singletonList),
    DEFINE_EVENTS("define_events", Collections::singletonList),
    CURRENT_EVENT("current_event", Collections::singletonList),

    CURRENT_LOGTALK_FLAG("current_logtalk_flag", Collections::singletonList),
    SET_LOGTALK_FLAG("set_logtalk_flag", Collections::singletonList),
    CREATE_LOGTALK_FLAG("create_logtalk_flag", Collections::singletonList),

    READ("read", Collections::singletonList),

    CURRENT_INPUT("current_input", PrologBuiltIns::current_input), CURRENT_OUTPUT("current_output", PrologBuiltIns::current_output),

    USER_INPUT("user_input", Collections::singletonList),
    USER_OUTPUT("user_output", Collections::singletonList),
//
//        Input and output
//
//    Edinburgh-style I/O

    TELL("tell", Collections::singletonList),
    TELLING("telling", Collections::singletonList),
    TOLD("told", Collections::singletonList),

    SEE("see", Collections::singletonList),
    SEEING("seeing", Collections::singletonList),
    SEEN("seen", Collections::singletonList),
    APPEND("append", Collections::singletonList),

    NL("nl", Collections::singletonList),
    TTYFLUSH("ttyflush", Collections::singletonList),
    FUNCTOR("functor", Collections::singletonList),
    NAME("name", Collections::singletonList),
    SUB_ATOM_ICASECHK("sub_atom_icasechk", Collections::singletonList),
    SUB_ATOM("sub_atom", Collections::singletonList),

    ATOM_PREFIX("atom_prefix", Collections::singletonList),
    ATOM_LENGTH("atom_length", Collections::singletonList),
    ATOMIC_LIST_CONCAT("atomic_list_concat", Collections::singletonList),
    TERM_TO_ATOM("term_to_atom", Collections::singletonList),
    ATOM_CONCAT("atom_concat", Collections::singletonList),
    ATOMIC_CONCAT("atomic_concat", Collections::singletonList),
    ATOM_TO_TERM("atom_to_term", Collections::singletonList),
    ATOM_NUMBER("atom_number", Collections::singletonList),
    TERM_EXPANSION("term_expansion", Collections::singletonList);

    private static List<ListTerm> dcg_expand(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> call(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

//    private static List<ListTerm> callN( ListTerm listTerm) {
//        return null;
//    }

    private static List<ListTerm> assign(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> not_unify(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> is(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> univ(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> unify(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> not(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> expand_goal(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> expand_term(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> include(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> _private(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> _protected(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> _public(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> fail(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> _true(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> current_op(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> op(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> initialization(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> current_output(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> current_input(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private final String name;
    private final Function<ListTerm, List<ListTerm>> builtInDef;

    /**
     * @param name
     */
    PrologBuiltIns(String name, Function<ListTerm, List<ListTerm>> impl) {
        this.name = name;
        this.builtInDef = impl;
    }

    private static List<ListTerm> multifile(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> discontiguous(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> dynamic(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> _static(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> hilog(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> encoding(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> cut(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> implies(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> dcg_imnplies(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    private static List<ListTerm> if_then(ListTerm listTerm) {
        return Collections.singletonList(listTerm);
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public Function<ListTerm, List<ListTerm>> getBuiltInDef() {
        return builtInDef;
    }
}