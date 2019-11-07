package org.ltc.hitalk.core;

import java.util.function.Consumer;

/**
 *
 */
public
enum PrologBuiltIns {

    PUBLIC("public", o -> {
    }),

    PROTECTED("protected", o -> {
    }),
    PRIVATE("private", o -> {
    }),

    INCLUDE("include", o -> {
    }),

    MULTIFILE("multifile", o -> {
    }),
    DISCONTIGUOUS("discontiguous", o -> {
    }),
    DYNAMIC("dynamic", o -> {
    }),
    STATIC("static", o -> {
    }),
    HILOG("hilog", o -> {
    }),
    ENCODING("encoding", o -> {
    }),
    TEXT("text", o -> {
    }),
    ISO_LATIN_1("iso_latin_1", o -> {
    }),
    UTF8("utf8", o -> {
    }),
    EXPAND_TERM("expand_term", o -> {
    }),
    EXPAND_GOAL("expand_goal", o -> {
    }),
    LOGTALK_LIBRARY_PATH("logtalk_library_path", o -> {
    }),

    OBJECT_PROPERTY("object_property", o -> {
    }),
    CATEGORY_PROPERTY("category_property", o -> {
    }),
    PROTOCOL_PROPERTY("protocol_property", o -> {
    }),

    COLON_COLON("::", o -> {
    }),
    UP_UP("^^", o -> {
    }),
    COMMA(",", o -> {
    }),
    COLON(":", o -> {
    }),
    SEMICOLON(";", o -> {
    }),
    CUT("!", o -> {
    }),
    IMPLIES(":-", o -> {
    }),
    DCG_IMPLIES("-->", o -> {
    }),
    IF("->", o -> {
    }),
    IF_STAR("*->", o -> {
    }),
    PLUS("+", o -> {
    }),
    MINUS("-", o -> {
    }),
    MULT("*", o -> {
    }),
    DIV("/", o -> {
    }),
    AT("@", o -> {
    }),
    PLUS_PLUS("++", o -> {
    }),
    MINUS_MINUS("--", o -> {
    }),
    QUESTION("?", o -> {
    }),
    QUERY("?-", o -> {
    }),
    L_SHIFT("<<", o -> {
    }),
    R_SHIFT(">>", o -> {
    }),
    AS("as", o -> {
    }),


    BYPASS("{}", o -> {
    }),

    INITIALIZATION("initialization", o -> {
    }),

    OP("op", o -> {
    }),
    CURRENT_OP("current_op", o -> {
    }),

    TRUE("true", o -> {
    }),
    FAIL("fail", o -> {
    }),
    FALSE("false", o -> {
    }),
    NOT("\\+", o -> {
    }),
    UNIFIES("=", o -> {
    }),
    UNIV("=..", o -> {
    }),
    IS("is", o -> {
    }),
    NON_UNIFIES("\\=", o -> {
    }),
    ASSIGN(":=", o -> {
    }),
    CALL("call", o -> {
    }),
    OBJECT("object", o -> {
    }),
    NIL("nil", o -> {
    }),
    CONS("cons", o -> {
    }),
    PROTOCOL("protocol", o -> {
    }),
    CATEGORY("category", o -> {
    }),
    ENUMERATION("enumeration", o -> {
    }),
    IDENTICAL("==", o -> {
    }),
    NON_IDENTICAL("\\==", o -> {
    }),
    CLASS("class", o -> {
    }),

    END_OBJECT("end_object", o -> {
    }),
    END_PROTOCOL("end_protocol", o -> {
    }),
    END_CATEGORY("end_category", o -> {
    }),

    CURRENT_OBJECT("create_object", o -> {
    }),
    CURRENT_CATEGORY("create_category", o -> {
    }),
    CURRENT_PROTOCOL("create_protocol", o -> {
    }),

    CREATE_OBJECT("create_object", o -> {
    }),
    CREATE_CATEGORY("create_category", o -> {
    }),
    CREATE_PROTOCOL("create_protocol", o -> {
    }),

    ABOLISH_OBJECT("abolish_object", o -> {
    }),
    ABOLISH_CATEGORY("abolish_category", o -> {
    }),
    ABOLISH_PROTOCOL("abolish_protocol", o -> {
    }),

    IMPLEMENTS_PROTOCOL("implements_protocol", o -> {
    }),
    IMPORTS_CATEGORY("imports_category", o -> {
    }),
    INSTANTIATES_CLASS("instantiates_class", o -> {
    }),
    SPECIALIZES_CLASS("specializes_class", o -> {
    }),
    EXTENDS_PROTOCOL("extends_protocol", o -> {
    }),
    EXTENDS_OBJECT("extends_object", o -> {
    }),
    EXTENDS_CATEGORY("extends_category", o -> {
    }),

    COMPLEMENTS_OBJECT("complements_object", o -> {
    }),

    CONFORMS_TO_PROTOCOL("conforms_to_protocol", o -> {
    }),

    ABOLISH_EVENTS("abolish_events", o -> {
    }),
    DEFINE_EVENTS("define_events", o -> {
    }),
    CURRENT_EVENT("current_event", o -> {
    }),

    CURRENT_LOGTALK_FLAG("current_logtalk_flag", o -> {
    }),
    SET_LOGTALK_FLAG("set_logtalk_flag", o -> {
    }),
    CREATE_LOGTALK_FLAG("create_logtalk_flag", o -> {
    }),

    READ("read", o -> {
    }),

    CURRENT_INPUT("current_input", o -> {
    }),
    CURRENT_OUTPUT("current_output", o -> {
    }),

    USER_INPUT("user_input", o -> {
    }),
    USER_OUTPUT("user_output", o -> {
    }),
//
//        Input and output
//
//    Edinburgh-style I/O

    TELL("tell", o -> {
    }),
    TELLING("telling", o -> {
    }),
    TOLD("told", o -> {
    }),

    SEE("see", o -> {
    }),
    SEEING("seeing", o -> {
    }),
    SEEN("seen", o -> {
    }),
    APPEND("append", o -> {
    }),

    NL("nl", o -> {
    }),
    TTYFLUSH("ttyflush", o -> {
    }),
    FUNCTOR("functor", o -> {
    }),
    NAME("name", o -> {
    }),
    SUB_ATOM_ICASECHK("sub_atom_icasechk", o -> {
    }),
    SUB_ATOM("sub_atom", o -> {
    }),

    ATOM_PREFIX("atom_prefix", o -> {
    }),
    ATOM_LENGTH("atom_length", o -> {
    }),
    ATOMIC_LIST_CONCAT("atomic_list_concat", o -> {
    }),
    TERM_TO_ATOM("term_to_atom", o -> {
    }),
    ATOM_CONCAT("atom_concat", o -> {
    }),
    ATOMIC_CONCAT("atomic_concat", o -> {
    }),
    ATOM_TO_TERM("atom_to_term", o -> {
    }),
    ATOM_NUMBER("atom_number", o -> {
    });

    //    static {
//        VariableAndFunctorInterner interner =
//                new VariableAndFunctorInternerImpl("prolog vars", "prolog builtin foos");}
//
    private final String name;
    private final Consumer builtInDef;

    /**
     * @param name
     */
    PrologBuiltIns ( String name, Consumer impl ) {
        this.name = name;
        this.builtInDef = impl;
    }

    /**
     * @return
     */
    public String getName () {
        return name;
    }

    /**
     * @return
     */
    public Consumer getBuiltInDef () {
        return builtInDef;
    }
}