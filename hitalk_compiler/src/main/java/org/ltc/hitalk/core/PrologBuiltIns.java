package org.ltc.hitalk.core;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import java.util.Collections;
import java.util.function.Consumer;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

/**
 *
 */
public
enum PrologBuiltIns {

    PUBLIC("public", listTerm -> {

    }),

    PROTECTED("protected", listTerm -> {
    }),

    PRIVATE("private", listTerm -> {
    }),

    INCLUDE("include", listTerm -> {
    }),

    MULTIFILE("multifile", listTerm -> {
    }),
    DISCONTIGUOUS("discontiguous", listTerm -> {
    }),
    DYNAMIC("dynamic", listTerm -> {
    }),
    STATIC("static", listTerm -> {
    }),
    HILOG("hilog", listTerm -> {
    }),
    ENCODING("encoding", listTerm -> {
    }),
    TEXT("text", listTerm -> {
    }),
    ISO_LATIN_1("iso_latin_1", listTerm -> {
    }),
    UTF8("utf8", listTerm -> {
    }),
    EXPAND_TERM("expand_term", listTerm -> {
    }),
    EXPAND_GOAL("expand_goal", listTerm -> {
    }),
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
    CUT("!", listTerm -> {
    }),
    IMPLIES(":-", listTerm -> {
    }),
    DCG_IMPLIES("-->", listTerm -> {
    }),
    IF("->", listTerm -> {
    }),
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

    INITIALIZATION("initialization", listTerm -> {
    }),

    OP("op", listTerm -> {
    }),
    CURRENT_OP("current_op", listTerm -> {
    }),

    TRUE("true", listTerm -> {
    }),
    FAIL("fail", listTerm -> {
    }),
    FALSE("false", listTerm -> {
    }),
    NOT("\\+", listTerm -> {
    }),
    UNIFIES("=", listTerm -> {
        final IResolver<HtPredicate, HtClause> resolver = getAppContext().getResolverPre();//
        final IVafInterner interner = getAppContext().getInterner();
        final HtFunctor eqf = new HtFunctor(interner.internFunctorName("=", 2), new ListTerm(2));// fixme
        final HtClause query = new HtClause(null, new ListTerm(Collections.singletonList(eqf)));
        eqf.setArgument(0, listTerm.getHead(0));
        eqf.setArgument(1, listTerm.getHead(1));

        try {
            resolver.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            resolver.setQuery(query);
        } catch (LinkageException e) {
            e.printStackTrace();
        }
        final boolean result = resolver.resolve() != null;
    }),
    UNIV("=..", listTerm -> {
    }),
    IS("is", listTerm -> {
    }),
    NON_UNIFIES("\\=", listTerm -> {
    }),
    ASSIGN(":=", listTerm -> {
    }),
    CALL("call", listTerm -> {
    }),
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

    CURRENT_INPUT("current_input", listTerm -> {
    }),
    CURRENT_OUTPUT("current_output", listTerm -> {
    }),

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

    private static boolean booleanResult;
    private final String name;
    private final Consumer <ListTerm> builtInDef;

    public static boolean getBooleanResult () {
        return booleanResult;
    }


    /**
     * @param name
     */
    PrologBuiltIns ( String name, Consumer <ListTerm> impl ) {
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
    public Consumer <ListTerm> getBuiltInDef () {
        return builtInDef;
    }
}