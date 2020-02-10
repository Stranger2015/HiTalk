package org.ltc.hitalk.core;

import org.ltc.hitalk.term.ListTerm;

import java.util.function.Consumer;

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
    TEXT("text", listTerm -> {
    }),
    ISO_LATIN_1("iso_latin_1", listTerm -> {
    }),
    UTF8("utf8", listTerm -> {
    }),
    EXPAND_TERM("expand_term", PrologBuiltIns::expand_term),
    EXPAND_GOAL("expand_goal", PrologBuiltIns::expand_goal),
    LOGTALK_LIBRARY_PATH("logtalk_library_path", listTerm -> {
    }),

    OBJECT_PROPERTY("object_property", listTerm -> {
    }),
    CATEGORY_PROPERTY("category_property", listTerm -> {
    }),
    PROTOCOL_PROPERTY("protocol_property", listTerm -> {
    }),

    COLON_COLON("::", listTerm -> {
    }),
    UP_UP("^^", listTerm -> {
    }),
    COMMA(",", listTerm -> {
    }),
    COLON(":", listTerm -> {
    }),
    SEMICOLON(";", listTerm -> {
    }),
    CUT("!", PrologBuiltIns::cut),
    IMPLIES(":-", PrologBuiltIns::implies),
    DCG_IMPLIES("-->", PrologBuiltIns::dcg_imnplies),
    IF("->", PrologBuiltIns::if_then),
    IF_STAR("*->", listTerm -> {
    }),
    PLUS("+", listTerm -> {
    }),
    MINUS("-", listTerm -> {
    }),
    MULT("*", listTerm -> {
    }),
    DIV("/", listTerm -> {
    }),
    AT("@", listTerm -> {
    }),
    PLUS_PLUS("++", listTerm -> {
    }),
    MINUS_MINUS("--", listTerm -> {
    }),
    QUESTION("?", listTerm -> {
    }),
    QUERY("?-", listTerm -> {
    }),
    L_SHIFT("<<", listTerm -> {
    }),
    R_SHIFT(">>", listTerm -> {
    }),
    AS("as", listTerm -> {
    }),


    BYPASS("{}", listTerm -> {
    }),

    INITIALIZATION("initialization", PrologBuiltIns::initialization),

    OP("op", PrologBuiltIns::op),
    CURRENT_OP("current_op", PrologBuiltIns::current_op),

    TRUE("true", PrologBuiltIns::_true),
    FAIL("fail", PrologBuiltIns::fail),
    FALSE("false", PrologBuiltIns::_false),
    NOT("\\+", PrologBuiltIns::not),
    UNIFIES("=", PrologBuiltIns::unify),
    UNIV("=..", PrologBuiltIns::univ),
    IS("is", PrologBuiltIns::is),
    NON_UNIFIES("\\=", PrologBuiltIns::not_unify),
    ASSIGN(":=", PrologBuiltIns::assign),
    CALL("call", PrologBuiltIns::call),
    OBJECT("object", listTerm -> {
    }),
    NIL("nil", listTerm -> {
    }),
    CONS("cons", listTerm -> {
    }),
    PROTOCOL("protocol", listTerm -> {
    }),
    CATEGORY("category", listTerm -> {
    }),
    ENUMERATION("enumeration", listTerm -> {
    }),
    IDENTICAL("==", listTerm -> {
    }),
    NON_IDENTICAL("\\==", listTerm -> {
    }),
    CLASS("class", listTerm -> {
    }),

    END_OBJECT("end_object", listTerm -> {
    }),
    END_PROTOCOL("end_protocol", listTerm -> {
    }),
    END_CATEGORY("end_category", listTerm -> {
    }),

    CURRENT_OBJECT("create_object", listTerm -> {
    }),
    CURRENT_CATEGORY("create_category", listTerm -> {
    }),
    CURRENT_PROTOCOL("create_protocol", listTerm -> {
    }),

    CREATE_OBJECT("create_object", listTerm -> {
    }),
    CREATE_CATEGORY("create_category", listTerm -> {
    }),
    CREATE_PROTOCOL("create_protocol", listTerm -> {
    }),

    ABOLISH_OBJECT("abolish_object", listTerm -> {
    }),
    ABOLISH_CATEGORY("abolish_category", listTerm -> {
    }),
    ABOLISH_PROTOCOL("abolish_protocol", listTerm -> {
    }),

    IMPLEMENTS_PROTOCOL("implements_protocol", listTerm -> {
    }),
    IMPORTS_CATEGORY("imports_category", listTerm -> {
    }),
    INSTANTIATES_CLASS("instantiates_class", listTerm -> {
    }),
    SPECIALIZES_CLASS("specializes_class", listTerm -> {
    }),
    EXTENDS_PROTOCOL("extends_protocol", listTerm -> {
    }),
    EXTENDS_OBJECT("extends_object", listTerm -> {
    }),
    EXTENDS_CATEGORY("extends_category", listTerm -> {
    }),

    COMPLEMENTS_OBJECT("complements_object", listTerm -> {
    }),

    CONFORMS_TO_PROTOCOL("conforms_to_protocol", listTerm -> {
    }),

    ABOLISH_EVENTS("abolish_events", listTerm -> {
    }),
    DEFINE_EVENTS("define_events", listTerm -> {
    }),
    CURRENT_EVENT("current_event", listTerm -> {
    }),

    CURRENT_LOGTALK_FLAG("current_logtalk_flag", listTerm -> {
    }),
    SET_LOGTALK_FLAG("set_logtalk_flag", listTerm -> {
    }),
    CREATE_LOGTALK_FLAG("create_logtalk_flag", listTerm -> {
    }),

    READ("read", listTerm -> {
    }),

    CURRENT_INPUT("current_input", PrologBuiltIns::current_input),
    CURRENT_OUTPUT("current_output", PrologBuiltIns::current_output),

    USER_INPUT("user_input", listTerm -> {
    }),
    USER_OUTPUT("user_output", listTerm -> {
    }),
//
//        Input and output
//
//    Edinburgh-style I/O

    TELL("tell", listTerm -> {
    }),
    TELLING("telling", listTerm -> {
    }),
    TOLD("told", listTerm -> {
    }),

    SEE("see", listTerm -> {
    }),
    SEEING("seeing", listTerm -> {
    }),
    SEEN("seen", listTerm -> {
    }),
    APPEND("append", listTerm -> {
    }),

    NL("nl", listTerm -> {
    }),
    TTYFLUSH("ttyflush", listTerm -> {
    }),
    FUNCTOR("functor", listTerm -> {
    }),
    NAME("name", listTerm -> {
    }),
    SUB_ATOM_ICASECHK("sub_atom_icasechk", listTerm -> {
    }),
    SUB_ATOM("sub_atom", listTerm -> {
    }),

    ATOM_PREFIX("atom_prefix", listTerm -> {
    }),
    ATOM_LENGTH("atom_length", listTerm -> {
    }),
    ATOMIC_LIST_CONCAT("atomic_list_concat", listTerm -> {
    }),
    TERM_TO_ATOM("term_to_atom", listTerm -> {
    }),
    ATOM_CONCAT("atom_concat", listTerm -> {
    }),
    ATOMIC_CONCAT("atomic_concat", listTerm -> {
    }),
    ATOM_TO_TERM("atom_to_term", listTerm -> {
    }),
    ATOM_NUMBER("atom_number", listTerm -> {
    }),
    TERM_EXPANSION("term_expansion", listTerm -> {

    });

    private static void call(ListTerm listTerm) {
    }

    private static void assign(ListTerm listTerm) {

    }

    private static void not_unify(ListTerm listTerm) {

    }

    private static void is(ListTerm listTerm) {

    }

    private static void univ(ListTerm listTerm) {

    }

    private static void unify(ListTerm listTerm) {

    }

    private static void not(ListTerm listTerm) {

    }

    private static void _false(ListTerm listTerm) {

    }

    private static void expand_goal(ListTerm listTerm) {

    }

    private static void expand_term(ListTerm listTerm) {


    }

    private static void include(ListTerm listTerm) {

    }

    private static void _private(ListTerm listTerm) {

    }

    private static void _protected(ListTerm listTerm) {

    }

    private static void _public(ListTerm listTerm) {
    }

    private static void fail(ListTerm listTerm) {

    }

    private static void _true(ListTerm listTerm) {
    }

    private static void current_op(ListTerm listTerm) {


    }

    private static void op(ListTerm listTerm) {

    }

    private static void initialization(ListTerm listTerm) {

    }

    private static void current_output(ListTerm listTerm) {

    }

    private static void current_input(ListTerm listTerm) {


    }

    private static boolean booleanResult;
    private final String name;
    private final Consumer<ListTerm> builtInDef;

    public static boolean getBooleanResult() {
        return booleanResult;
    }


    /**
     * @param name
     */
    PrologBuiltIns(String name, Consumer<ListTerm> impl) {
        this.name = name;
        this.builtInDef = impl;
    }

    private static void multifile(ListTerm listTerm) {
    }

    private static void discontiguous(ListTerm listTerm) {
    }

    private static void dynamic(ListTerm listTerm) {
    }

    private static void _static(ListTerm listTerm) {
    }

    private static void hilog(ListTerm listTerm) {
    }

    private static void encoding(ListTerm listTerm) {
    }

    private static void cut(ListTerm listTerm) {
    }

    private static void implies(ListTerm listTerm) {
    }

    private static void dcg_imnplies(ListTerm listTerm) {
    }

    private static void if_then(ListTerm listTerm) {
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
    public Consumer <ListTerm> getBuiltInDef () {
        return builtInDef;
    }
}