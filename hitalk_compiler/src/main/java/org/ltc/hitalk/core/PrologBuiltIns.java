package org.ltc.hitalk.core;

import org.ltc.hitalk.wam.compiler.HtFunctor;

import java.util.function.Consumer;

/**
 *
 */
public
enum PrologBuiltIns {
    PUBLIC("public"),
    PROTECTED("protected"),
    PRIVATE("private"),

    INCLUDE("include"),

    MULTIFILE("multifile"),
    DISCONTIGUOUS("discontiguous"),
    DYNAMIC("dynamic"),
    STATIC("static"),
    HILOG("hilog"),
    ENCODING("encoding"),
    TEXT("text"),
    ISO_LATIN_1("iso_latin_1"),
    UTF8("utf8"),
    EXPAND_TERM("expand_term"),
    EXPAND_GOAL("expand_goal"),
    LOGTALK_LIBRARY_PATH("logtalk_library_path"),

    OBJECT_PROPERTY("object_property"),
    CATEGORY_PROPERTY("category_property"),
    PROTOCOL_PROPERTY("protocol_property"),

    COLON_COLON("::"),
    UP_UP("^^"),
    COMMA(","),
    COLON(":"),
    SEMICOLON("),"),
    CUT("!"),
    IMPLIES(":-"),
    DCG_IMPLIES("-->"),
    IF("->"),
    IF_STAR("*->"),
    PLUS("+"),
    MINUS("-"),
    MULT("*"),
    DIV("/"),
    AT("@"),
    PLUS_PLUS("++"),
    MINUS_MINUS("--"),
    QUESTION("?"),
    QUERY("?-"),
    L_SHIFT("<<"),
    R_SHIFT(">>"),
    AS("as"),


    BYPASS("{}"),

    INITIALIZATION("initialization"),

    OP("op"),
    CURRENT_OP("current_op"),

    TRUE("true"),
    FAIL("fail"),
    FALSE("false"),
    NOT("\\+"),
    UNIFIES("="),
    UNIV("=.."),
    IS("is"),
    NON_UNIFIES("\\="),
    ASSIGN(":="),
    CALL("call"),
    OBJECT("object"),
    NIL("nil"),
    CONS("cons"),
    PROTOCOL("protocol"),
    CATEGORY("category"),
    ENUMERATION("enumeration"),
    IDENTICAL("=="),
    NON_IDENTICAL("\\=="),
    CLASS("class"),

    END_OBJECT("end_object"),
    END_PROTOCOL("end_protocol"),
    END_CATEGORY("end_category"),

    CURRENT_OBJECT("create_object"),
    CURRENT_CATEGORY("create_category"),
    CURRENT_PROTOCOL("create_protocol"),

    CREATE_OBJECT("create_object"),
    CREATE_CATEGORY("create_category"),
    CREATE_PROTOCOL("create_protocol"),

    ABOLISH_OBJECT("abolish_object"),
    ABOLISH_CATEGORY("abolish_category"),
    ABOLISH_PROTOCOL("abolish_protocol"),

    IMPLEMENTS_PROTOCOL("implements_protocol"),
    IMPORTS_CATEGORY("imports_category"),
    INSTANTIATES_CLASS("instantiates_class"),
    SPECIALIZES_CLASS("specializes_class"),
    EXTENDS_PROTOCOL("extends_protocol"),
    EXTENDS_OBJECT("extends_object"),
    EXTENDS_CATEGORY("extends_category"),

    COMPLEMENTS_OBJECT("complements_object"),

    CONFORMS_TO_PROTOCOL("conforms_to_protocol"),

    ABOLISH_EVENTS("abolish_events"),
    DEFINE_EVENTS("define_events"),
    CURRENT_EVENT("current_event"),

    CURRENT_LOGTALK_FLAG("current_logtalk_flag"),
    SET_LOGTALK_FLAG("set_logtalk_flag"),
    CREATE_LOGTALK_FLAG("create_logtalk_flag"),

    READ("read"),

    CURRENT_INPUT("current_input"),
    CURRENT_OUTPUT("current_output"),

    USER_INPUT("user_input"),
    USER_OUTPUT("user_output"),

    //    Input and output

//    Edinburgh-style I/O

    TELL("tell"),
    TELLING("telling"),
    TOLD("told"),

    SEE("see"),
    SEEING("seeing"),
    SEEN("seen"),
    APPEND("append"),

    NL("nl"),
    TTYFLUSH("ttyflush"),
    FUNCTOR("functor"),
    NAME("name"),
    SUB_ATOM_ICASECHK("sub_atom_icasechk"),
    SUB_ATOM("sub_atom"),

    ATOM_PREFIX("atom_prefix"),
    ATOM_LENGTH("atom_length"),
    ATOMIC_LIST_CONCAT("atomic_list_concat"),
    TERM_TO_ATOM("term_to_atom"),
    ATOM_CONCAT("atom_concat"),
    ATOMIC_CONCAT("atomic_concat"),
    ATOM_TO_TERM("atom_to_term"),
    ATOM_NUMBER("atom_number"),
    ;


    private final String name;
    private final Consumer <HtFunctor> impl;

    /**
     * @param name
     */
    PrologBuiltIns ( String name, Consumer <HtFunctor> impl ) {
        this.name = name;
        this.impl = impl;
    }

    /**
     * @return
     */
    public
    String getName () {
        return name;
    }
}