package org.ltc.hitalk.parser;

public
interface PrologAtoms {
    String BYPASS_NIL = "{}";//arity 0 => ATOM
//    String BYPASS_CONS = "{|}";//arity 0-2: {}, {| TERM_LIST OR VAR),{ERM1 | TERM_TAIL OR VAR}; depends on ctx:
// bypass/ dcg_escape/ other
//                       "{|}";//arity 0-2: {}, {| CALLABLE_LIST OR VAR),(CALLABLE1 | CALLABLE_TAIL OR VAR);
    String LBRACE = "{";
    String RBRACE = "}";
    String IMPLIES = ":-";
    String DCG_IMPLIES = "-->";
    String QUERY = "?-";
    String SEMICOLON = ";";
    String IF = "if";
    String IF_THEN = "->";
    String IF_STAR = "*-> ";
    String COMMA = ",";
    String NOT = "\\+";
    String UNIFIES = "=";
    String NON_UNIFIES = "\\=";
    String IDENTICAL = "==";
    String NON_IDENTICAL = "\\==";
    String AT_LESS = "@<";
    String AT_LESS_OR_EQUAL = "@=<";
    String AT_GREATER = "@>";
    String AT_GREATER_OR_EQUAL = "@>=";
    String UNIV = "=..";
    String IS = "is";
    String EQ_COLON_EQ = "=:=";
    String EQ_BSLASH_EQ = "=\\=";
    String LESS = "<";
    String LESS_OR_EQUAL = "=<";
    String GREATER = ">";
    String GREATER_OR_EQUAL = ">=";
    String SLASH_SLASH = "//";
    String SLASH = "/";
    String STAR = "*";
    String RSHIFT = ">>";
    String LSHIFT = "<<";
    String REM = "rem";
    String MOD = "mod";
    String MINUS = "-";
    String UP = "^";
    String STAR_STAR = "**";
    String COLON_COLON = "::";
    String UP_UP = "^^";
    String PLUS = "+";
    String AT = "@";
    String QUESTION = "?";
    String PLUS_PLUS = "++";
    String MINUS_MINUS = "--";
    //  String  LSHIFT = "<<";
//  String  RSHIFT = ">>";
    String AS = "as";

    String PUBLIC = "public";
    String PROTECTED = "protected";
    String PRIVATE = "private";
    String ENUMERATION = "enumeration";

    String SLASH_BSLASH = "/\\";
    String BSLASH_SLASH = "\\/";

    String NIL = "[]";//arity 0 => ATOM
//    String CONS = "[|]";//;

    String CONS = "|";//arity 1-2: (), (|LIST OR VAR),(EL1 | TAIL OR VAR);

    String TRUE = "true";
    String FAIL = "fail";
    String FALSE = "false";

    String ARGLIST_NIL = "()";//arity 0 => ATOM
    //    String ARGLIST_CONS = "(|)";//arity 0-2: (), (|ARGLIST OR VAR),(ARG1 | ARGTAIL OR VAR);
    String CUT = "!";

    String EXTENDS = "extends";
    String IMPLEMENTS = "implements";
    String IMPORTS = "imports";
    String COMPLEMENTS = "complements";
    String INSTANTIATES = "instantiates";
    String SPECIALIZES = "specializes";
    String ENCODING = "encoding";

    String HILOG = "hilog";
    String COLON = ":";
    String BSLASH = "\\";
    String DOT = ".";
    String ASSIGN = ":=";
    String DOLLAR = "$";
    String XOR = "xor";
    String DYNAMIC = "dynamic";
    String DISCONTIGUOUS = "discontiguous";
    String INITIALIZATION = "initialization";
    String META_PREDICATE = "meta_predicate";
    String MODULE_TRANSPARENT = "module_transparent";
    String MULTIFILE = "multifile";
    String THREAD_LOCAL = "thread_local";
    String THREAD_INITIALIZATION = "thread_initialization";
    String VOLATILE = "volatile";
    String RDIV = "rdiv";
    String DIV = "div";
    String COMMA_ = COMMA;
    String NAF = NOT;
}
