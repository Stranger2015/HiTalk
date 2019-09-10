package org.ltc.hitalk.parser;

public
interface PrologAtoms {
    String BYPASS_NIL = "{}";//arity 0 => ATOM
    String BYPASS_CONS = "{|}";//arity 0-2: {}, {| TERM_LIST OR VAR),{ERM1 | TERM_TAIL OR VAR}; depends on ctx:
    // bypass/ dcg_escape/ other
//                       "{|}";//arity 0-2: {}, {| CALLABLE_LIST OR VAR),(CALLABLE1 | CALLABLE_TAIL OR VAR);
    String LBRACE = "{";
    String RBRACE = "}";
    String IMPLIES = ":-";
    String DCG_IMPLIES = "-->";
    String QUERY = "?";
    String SEMICOLON = ";";
    String IF = "->";
    String IF_STAR = "->*";
    String COMMA = ",";
    String NOT = "\\;";
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
    String UP_UP = "";
    String PLUS = "";
    String AT = "";
    String QUESTION = "?";
    //   String MINUS = "-";
    String PLUS_PLUS = "";
    //    String QUESTION = "";
    String MINUS_MINUS = "";
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
    String CONS = "[|]";//arity 0-2: (), (|LIST OR VAR),(EL1 | TAIL OR VAR);

    String TRUE = "true";
    String FAIL = "fail";
    String FALSE = "false";

    String ARGLIST_NIL = "()";//arity 0 => ATOM
    String ARGLIST_CONS = "(|)";//arity 0-2: (), (|ARGLIST OR VAR),(ARG1 | ARGTAIL OR VAR);
}
