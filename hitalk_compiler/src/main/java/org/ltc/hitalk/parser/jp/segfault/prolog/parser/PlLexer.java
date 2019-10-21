package org.ltc.hitalk.parser.jp.segfault.prolog.parser;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind;
import org.ltc.hitalk.term.io.HiTalkStream;

import java.io.EOFException;
import java.io.IOException;
import java.util.Optional;

import static java.lang.Character.*;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlToken.TokenKind.*;

/**
 * 入力ストリームをPrologテキストとみなし、トークン列に分解します。
 *
 * @author shun
 */
public class PlLexer {

    private final HiTalkStream stream;
    private PlToken token;

    public static final String PUNCTUATION = "#&*+-./\\:;?@^$<=>";
    public static final String PARENTHESIS = "(){}[],!|";

    public PlLexer ( HiTalkStream stream ) {
        this.stream = stream;
    }

    public static boolean isMergeable ( String l, String r ) {
        return isTokenBoundary(l.charAt(l.length() - 1), r.charAt(0));
    }

    /**
     * 指定した二文字の間がトークンの境界になりうるかどうかを調べます。
     */
    public static boolean isTokenBoundary ( char l, char r ) {
        return isJavaIdentifierPart(l) != isJavaIdentifierPart(r) || isTokenBoundary(l) || isTokenBoundary(r);
    }

    public static boolean isTokenBoundary ( char c ) {
        return isWhitespace(c) || PARENTHESIS.indexOf(c) != -1;
    }

    /**
     * 次のトークンを返します。
     */
    public PlToken next ( boolean value ) {
        try {
            return (token = getToken(value));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }

    /**
     * 前に解析したトークンを返します。
     */
    public PlToken peek () {
        return token;
    }

    private TokenKind calcTokenKind ( int c ) {
        for (int i = 0; i < values().length; i++) {
            TokenKind value = values()[i];
            if (value.getChar() == c) {
                return value;
            }
        }
        return null;
    }

    private PlToken getToken ( boolean valued ) throws IOException, ParseException {
        int chr = stream.read();
        if (chr == -1) {
            return null;
        }
        if (valued) {
            // 括弧など
            if ("([{".indexOf(chr) != -1) {
                return new PlToken(calcTokenKind(chr));
            }
            // 整数値アトム
            if (chr == '-') {
                int c = stream.read();
                if (isDigit(c)) {
                    return getNumber(c, "-");
                }
                ungetc(c);
            } else if (isDigit(chr)) {
                return getNumber(chr, "");
            }
        }

        if ("}])".indexOf(chr) != -1) {
            return new PlToken(calcTokenKind(chr), String.valueOf((char) chr));
        }
        PlToken token = getAtom(chr);
        if (token == null) {//不正な文字
            throw new ParseException(":Bad char `" + (char) chr + ":0x" + Integer.toHexString(chr) + "'.");
        }
        if (valued && token.kind == ATOM) {
            if ((chr = stream.read()) == '(') {
                return new PlToken(FUNCTOR_BEGIN);
            }
            ungetc(chr);
        }

        return token;
    }

    private PlToken getAtom ( int chr ) throws IOException, ParseException {
        StringBuilder val = new StringBuilder();
        // 単体でアトムを構成
        if (";,!|".indexOf(chr) != -1) {
            return new PlToken(ATOM, String.valueOf((char) chr));
        }
        // アルファベットのみで構成されるアトムか変数
        if (isJavaIdentifierStart(chr)) {
            do {
                val.append((char) chr);
            } while (isJavaIdentifierPart(chr = stream.read()));
            ungetc(chr);
            return new PlToken(isUpperCase(val.charAt(0)) || val.charAt(0) == '_' ? VAR : ATOM, val.toString());
        }
        // 'アトム'
        if (chr == '\'') {
            while ((chr = readFully()) != '\'') {
                val.append((char) chr);
                if (chr == '\\') {
                    val.append(readFully());
                }
            }
            return new PlToken(ATOM, Quotemeta.decode(val.toString()))/*, true)*/;
        }
        // アトム
        ungetc(chr);
        val = Optional.of(repeat(PUNCTUATION)).map(StringBuilder::new).orElse(null);
        if (!val.toString().isEmpty()) {
            return new PlToken(ATOM, val.toString());
        }
        return null;
    }

    private PlToken getNumber ( int chr, String prefix ) throws IOException, ParseException {
        String number = prefix;
        if (chr == '0') {
            chr = stream.read();
            if (chr == 'x') {
                return new PlToken(INTEGER_LITERAL, number + "0x" + repeat1("0123456789abcdefABCDEF"));
            }
            ungetc(chr);
            if (isDigit(chr)) {
                number += repeat("01234567");
                if (!isDigit(chr = stream.read())) {
                    ungetc(chr);
                    return new PlToken(INTEGER_LITERAL, "0" + number);
                }
                number += (char) chr;
                number += repeat("0123456789");
            } else {
                number += "0";
            }
        } else {
            number += (char) chr + repeat("0123456789");
        }
        TokenKind kind = INTEGER_LITERAL;
        chr = stream.read();
        if (chr == '.') {
            if (!isDigit(chr = stream.read())) {
                ungetc(chr);
                ungetc('.');
                return new PlToken(INTEGER_LITERAL, number);
            }
            kind = FLOATING_POINT_LITERAL;
            number += "." + (char) chr + repeat("0123456789");
            chr = stream.read();
        }
        if (chr == 'e' || chr == 'E') {
            String sign = "";
            chr = stream.read();
            if (chr == '+' || chr == '-') {
                sign = String.valueOf((char) chr);
            } else {
                ungetc(chr);
            }
            kind = FLOATING_POINT_LITERAL;
            number += "e" + sign + repeat1("0123456789");
        } else {
            ungetc(chr);
        }
        return new PlToken(kind, number);
    }

    private String repeat1 ( String chars ) throws IOException, ParseException {
        String result = repeat(chars);
        if (result.isEmpty()) {
            throw new ParseException("文字がありません。chars=\"" + chars + "\"");
        }
        return result;
    }

    private String repeat ( String chars ) throws IOException {
        StringBuilder result = new StringBuilder();
        for (; ; ) {
            int c = stream.read();
            if (chars.indexOf(c) == -1) {
                ungetc(c);
                break;
            }
            result.append((char) c);
        }
        return result.toString();
    }

    private char readFully () throws IOException {
        int c = stream.read();
        if (c == -1) {
            throw new EOFException();
        }
        return (char) c;
    }

    private void skipWhitespaces () throws IOException {
        for (; ; ) {
            int chr = stream.read();
            if (!isWhitespace(chr)) {
                if (chr == '%') {
                    stream.readLine();
                    continue;
                }
                if (chr == '/') {
                    int c = stream.read();
                    if (c == '*') {
                        while (true) {
                            if (readFully() == '*' && readFully() == '/') break;
                        }
                        continue;
                    }
                    ungetc(c);
                }
                ungetc(chr);
                break;
            }
        }
    }

    private void ungetc ( int c ) throws IOException {
        if (c != -1) {
            stream.unread(c);
        }
    }

    public PlToken getNextToken () throws IOException, ParseException {
        return next(true);
    }
}

//hilog read
//%   File   : READ.PL
//        %   Author : D.H.D.Warren + Richard O'Keefe
//        %   Updated: 5 July 1984
//        %   Purpose: Read Prolog terms in Dec-10 syntax.
///*
//    Modified by Alan Mycroft to regularise the functor modes.
//    This is both easier to understand (there are no more '?'s),
//    and also fixes bugs concerning the curious interaction of cut with
//    the state of parameter instantiation.
//
//    Since this file doesn't provide "metaread", it is considerably
//    simplified.  The token list format has been changed somewhat, see
//    the comments in the RDTOK file.
//
//(modified for HiLog changes, dsw)
//    I have added the rule X(...) -> apply(X,[...]) for Alan Mycroft.
//*/
//
///*
//    Modified by Saumya Debray, SUNY @ Stony Brook, to cut away DEC-10 syntax
//    that isn't used by C-Prolog 1.5 : "public" and "mode" declarations have
//    been deleted, and the builtins ttynl/0 and ttyput/1 replaced by nl/0
//    and put/1.	(April 2, 1985)
//*/
//
///* hacked by D.S.Warren to try to parse HiLog syntax, 11/3/89.
//Changed to be Quintus Prolog compatible
//*/
//
//        % :- use_module(library(basics)).
//
//        %   h_read(?Answer).
//
//        h_read(Answer) :- h_read(Answer,_).
//
//        %   h_read(?Answer, ?Variables)
//        %   reads a term from the current input stream and unifies it with
//        %   Answer.  Variables is bound to a list of [Atom=Variable] pairs.
//
//        h_read(Answer, Variables) :-
//        repeat,
//        h_read_tokens(Tokens, Variables),
//        (   h_read(Tokens, 1200, Term, LeftOver), h_read_all(LeftOver)
//        ),
//        !,
//        Answer = Term.
//
//
//        %   h_read_all(+Tokens)
//        %   checks that there are no unparsed tokens left over.
//
//        h_read_all([]) :- !.
//        h_read_all(S) :-
//        h_read_syntax_error(['operator expected after expression'], S).
//
//
//        %   h_read_expect(Token, TokensIn, TokensOut)
//        %   reads the next token, checking that it is the one expected, and
//        %   giving an error message if it is not.  It is used to look for
//        %   right brackets of various sorts, as they're all we can be sure of.
//
//        h_read_expect(Token, [Token|Rest], Rest) :- !.
//        h_read_expect(Token, S0, _) :-
//        h_read_syntax_error([Token,'or operator expected'], S0).
//
//
//        %   I want to experiment with having the operator information held as
//        %   ordinary Prolog facts.  For the moment the following predicates
//        %   remain as interfaces to curr_op.
//        %   h_read_prefixop(O -> Self, Rarg)
//        %   h_read_postfixop(O -> Larg, Self)
//        %   h_read_infixop(O -> Larg, Self, Rarg)
//
//
//        h_read_prefixop(Op, Prec, Prec) :-
//        h_read_curr_op(Prec, fy, Op), !.
//        h_read_prefixop(Op, Prec, Less) :-
//        h_read_curr_op(Prec, fx, Op), !,
//        Less is Prec-1.
//
//
//        h_read_postfixop(Op, Prec, Prec) :-
//        h_read_curr_op(Prec, yf, Op), !.
//        h_read_postfixop(Op, Less, Prec) :-
//        h_read_curr_op(Prec, xf, Op), !, Less is Prec-1.
//
//
//        h_read_infixop(Op, Less, Prec, Less) :-
//        h_read_curr_op(Prec, xfx, Op), !, Less is Prec-1.
//        h_read_infixop(Op, Less, Prec, Prec) :-
//        h_read_curr_op(Prec, xfy, Op), !, Less is Prec-1.
//        h_read_infixop(Op, Prec, Prec, Less) :-
//        h_read_curr_op(Prec, yfx, Op), !, Less is Prec-1.
//
//
//        h_read_ambigop(F, L1, O1, R1, L2, O2) :-
//        h_read_postfixop(F, L2, O2),
//        h_read_infixop(F, L1, O1, R1), !.
//
//
//        %   h_read(+TokenList, +Precedence, -Term, -LeftOver)
//        %   parses a Token List in a context of given Precedence,
//        %   returning a Term and the unread Left Over tokens.
//
//        h_read([Token|RestTokens], Precedence, Term, LeftOver) :-
//        h_read(Token, RestTokens, Precedence, Term, LeftOver).
//        h_read([], _, _, _) :-
//        h_read_syntax_error(['expression expected'], []).
//
//
//        %   h_read(+Token, +RestTokens, +Precedence, -Term, -LeftOver)
//
//        % changed for HiLog
//        h_read(var(Variable,_), ['('|S1], Precedence, Answer, S) :- !,
//        h_read(S1, 999, Arg1, S2),
//        h_read_args(S2, RestArgs, S3), !,
//        =..(Term,[apply,Variable,Arg1|RestArgs]),
//        h_read_exprtl0(S3,Term,Precedence,Answer,S).
//
//        h_read(var(Variable,_), S0, Precedence, Answer, S) :- !,
//        h_read_exprtl0(S0, Variable, Precedence, Answer, S).
//
//        h_read(atom(-), [integer(Integer)|S1], Precedence, Answer, S) :-
//        Negative is -Integer, !,
//        h_read_exprtl0(S1, Negative, Precedence, Answer, S).
//
//        % changed for HiLog
//        h_read(atom(Functor), ['('|S1], Precedence, Answer, S) :- !,
//        h_read(S1, 999, Arg1, S2),
//        h_read_args(S2, RestArgs, S3),
//        =..(Term,[apply,Functor,Arg1|RestArgs]), !,
//        h_read_exprtl0(S3, Term, Precedence, Answer, S).
//
//        h_read(atom(Functor), S0, Precedence, Answer, S) :-
//        h_read_prefixop(Functor, Prec, Right), !,
//        h_read_aft_pref_op(Functor, Prec, Right, S0, Precedence, Answer, S).
//
//        h_read(atom(Atom), S0, Precedence, Answer, S) :- !,
//        h_read_exprtl0(S0, Atom, Precedence, Answer, S).
//
//        % added for HiLog
//        h_read(integer(Integer), ['('|S1], Precedence, Answer, S) :- !,
//        h_read(S1, 999, Arg1, S2),
//        h_read_args(S2, RestArgs, S3),
//        =..(Term,[apply,Integer,Arg1|RestArgs]), !,
//        h_read_exprtl0(S3, Term, Precedence, Answer, S).
//
//        h_read(integer(Integer), S0, Precedence, Answer, S) :- !,
//        h_read_exprtl0(S0, Integer, Precedence, Answer, S).
//
//        h_read('[', [']'|S1], Precedence, Answer, S) :- !,
//        h_read_exprtl0(S1, [], Precedence, Answer, S).
//
//        % HiLog list
//        h_read('[', S1, Precedence, Answer, S) :- !,
//        h_read(S1, 999, Arg1, S2),
//        h_read_list(S2, RestArgs, S3), !,
//        h_read_exprtl0(S3, apply('.',Arg1,RestArgs), Precedence, Answer, S).
//
//        h_read('(', S1, Precedence, Answer, S) :- !,
//        h_read(S1, 1200, Term, S2),
//        h_read_expect(')', S2, S3), !,
//        h_read_exprtl0(S3, Term, Precedence, Answer, S).
//
//        h_read(' (', S1, Precedence, Answer, S) :- !,
//        h_read(S1, 1200, Term, S2),
//        h_read_expect(')', S2, S3), !,
//        h_read_exprtl0(S3, Term, Precedence, Answer, S).
//
//        h_read('{', ['}'|S1], Precedence, Answer, S) :- !,
//        h_read_exprtl0(S1, '{}', Precedence, Answer, S).
//
//        h_read('{', S1, Precedence, Answer, S) :- !,
//        h_read(S1, 1200, Term, S2),
//        h_read_expect('}', S2, S3), !,
//        h_read_exprtl0(S3, '{}'(Term), Precedence, Answer, S).
//
//        h_read(string(List), S0, Precedence, Answer, S) :- !,
//        h_read_exprtl0(S0, List, Precedence, Answer, S).
//
//        h_read(Token, S0, _, _, _) :-
//        h_read_syntax_error([Token,'cannot start an expression'], S0).
//
//
//        %   h_read_args(+Tokens, -TermList, -LeftOver)
//        %   parses {',' expr(999)} ')' and returns a list of terms.
//
//        h_read_args([Tok|S1], Term, S) :- h_read_args1(Tok,Term,S,S1), !.
//        h_read_args(S, _, _) :-
//        h_read_syntax_error([', or ) expected in arguments'], S).
//
//
//        h_read_args1(',',[Term|Rest],S,S1) :-
//        h_read(S1, 999, Term, S2), !,
//        h_read_args(S2, Rest, S).
//        h_read_args1(')',[],S,S).
//
//
//
//        %   h_read_list(+Tokens, -TermList, -LeftOver)
//        %   parses {',' expr(999)} ['|' expr(999)] ']' and returns a list of terms.
//
//        h_read_list([Tok|S1],Term,S) :- h_read_list1(Tok,Term,S,S1), !.
//        h_read_list(S, _, _) :-
//        h_read_syntax_error([', | or ] expected in list'], S).
//
//
//        %HiLog
//        h_read_list1(',',apply('.',Term,Rest),S,S1) :-
//        h_read(S1, 999, Term, S2), !,
//        h_read_list(S2, Rest, S).
//        h_read_list1('|',Rest,S,S1) :-
//        h_read(S1, 999, Rest, S2), !,
//        h_read_expect(']', S2, S).
//        h_read_list1(']',[],S,S).
//
//
//        %   h_read_aft_pref_op(+Op, +Prec, +ArgPrec, +Rest, +Precedence, -Ans, -LeftOver)
//
//        h_read_aft_pref_op(Op, Oprec, _Aprec, S0, Precedence, _, _) :-
//        Precedence < Oprec, !,
//        h_read_syntax_error(['prefix operator',Op,'in context with precedence '
//        ,Precedence], S0).
//
//        h_read_aft_pref_op(Op, Oprec, _Aprec, S0, Precedence, Answer, S) :-
//        h_read_peepop(S0, S1),
//        h_read_prefix_is_atom(S1, Oprec), % can't cut but would like to
//        h_read_exprtl(S1, Oprec, Op, Precedence, Answer, S).
//
//        % changed for HiLog
//        h_read_aft_pref_op(Op, Oprec, Aprec, S1, Precedence, Answer, S) :-
//        h_read(S1, Aprec, Arg, S2),
//        =..(Term,[apply,Op,Arg]), !,
//        h_read_exprtl(S2, Oprec, Term, Precedence, Answer, S).
//
//
//        %   The next clause fixes a bug concerning "mop dop(1,2)" where
//        %   mop is monadic and dop dyadic with higher Prolog priority.
//
//        h_read_peepop([atom(F),'('|S1], [atom(F),'('|S1]) :- !.
//        h_read_peepop([atom(F)|S1], [infixop(F,L,P,R)|S1]) :-
//        h_read_infixop(F, L, P, R).
//        h_read_peepop([atom(F)|S1], [postfixop(F,L,P)|S1]) :-
//        h_read_postfixop(F, L, P).
//        h_read_peepop(S0, S0).
//
//
//        %   h_read_prefix_is_atom(+TokenList, +Precedence)
//        %   is true when the right context TokenList of a prefix operator
//        %   of result precedence Precedence forces it to be treated as an
//        %   atom, e.g. (- = X), p(-), [+], and so on.
//
//        h_read_prefix_is_atom([Token|_], Precedence) :-
//        h_read_prefix_is_atom(Token, Precedence).
//
//        h_read_prefix_is_atom(infixop(_,L,_,_), P) :- L >= P.
//        h_read_prefix_is_atom(postfixop(_,L,_), P) :- L >= P.
//        h_read_prefix_is_atom(')', _).
//        h_read_prefix_is_atom(']', _).
//        h_read_prefix_is_atom('}', _).
//        h_read_prefix_is_atom('|', P) :- 1100 >= P.
//        h_read_prefix_is_atom(',', P) :- 1000 >= P.
//        h_read_prefix_is_atom([],  _).
//
//
//        %   h_read_exprtl0(+Tokens, +Term, +Prec, -Answer, -LeftOver)
//        %   is called by read/4 after it has read a primary (the Term).
//        %   It checks for following postfix or infix operators.
//
//        h_read_exprtl0([Tok|S1], Term, Precedence, Answer, S) :-
//        h_read_exprtl01(Tok,Term,Precedence,Answer,S,S1), !.
//        h_read_exprtl0(S, Term, _, Term, S).
//
//
//        h_read_exprtl01(atom(F), Term, Precedence, Answer,S,S1) :-
//        h_read_ambigop(F, L1, O1, R1, L2, O2), !,
//        ( h_read_exprtl([infixop(F,L1,O1,R1)|S1],0,Term,Precedence,Answer,S)
//        ; h_read_exprtl([postfixop(F,L2,O2) |S1],0,Term,Precedence,Answer,S)
//        ).
//        h_read_exprtl01(atom(F), Term, Precedence, Answer, S,S1) :-
//        h_read_infixop(F, L1, O1, R1), !,
//        h_read_exprtl([infixop(F,L1,O1,R1)|S1],0,Term,Precedence,Answer,S).
//        h_read_exprtl01(atom(F),Term,Precedence,Answer,S,S1) :-
//        h_read_postfixop(F, L2, O2), !,
//        h_read_exprtl([postfixop(F,L2,O2) |S1],0,Term,Precedence,Answer,S).
//        % HiLog and
//        h_read_exprtl01(',', Term, Precedence, Answer, S,S1) :-
//        Precedence >= 1000, !,
//        h_read(S1, 1000, Next, S2), !,
//        h_read_exprtl(S2, 1000, apply(',',Term,Next), Precedence, Answer, S).
//        % HiLog or
//        h_read_exprtl01('|', Term, Precedence, Answer, S,S1) :-
//        Precedence >= 1100, !,
//        h_read(S1, 1100, Next, S2), !,
//        h_read_exprtl(S2, 1100, apply(';',Term,Next), Precedence, Answer, S).
//        % for HiLog
//        h_read_exprtl01('(', Term, Precedence, Answer, S,S1) :-
//        !,
//        h_read(S1, 999, Arg1, S2),
//        h_read_args(S2, RestArgs, S3),
//        =..(HiLogTerm,[apply,Term,Arg1|RestArgs]),
//        h_read_exprtl0(S3, HiLogTerm, Precedence, Answer, S).
//        h_read_exprtl01(Thing, _, _, _, _,S1) :-
//        h_read_cfexpr(Thing, Culprit), !,
//        h_read_syntax_error([Culprit,'follows expression'], [Thing|S1]).
//
//
//        h_read_cfexpr(atom(_),       atom).
//        h_read_cfexpr(var(_,_),      variable).
//        h_read_cfexpr(integer(_),    integer).
//        h_read_cfexpr(string(_),     string).
//        h_read_cfexpr(' (',          bracket).
//        h_read_cfexpr('(',           bracket).
//        h_read_cfexpr('[',           bracket).
//        h_read_cfexpr('{',           bracket).
//
//
//
//        h_read_exprtl([Tok|S1], C, Term, Precedence, Answer, S) :-
//        h_read_exprtl1(Tok,C,Term,Precedence,Answer,S,S1), !.
//        h_read_exprtl(S, _, Term, _, Term, S).
//
//        % changed for HiLog
//        h_read_exprtl1(infixop(F,L,O,R), C, Term, Precedence, Answer, S, S1) :-
//        Precedence >= O, C =< L, !,
//        h_read(S1, R, Other, S2),
//        =..(Expr,[apply,F,Term,Other]), /*!,*/
//        h_read_exprtl(S2, O, Expr, Precedence, Answer, S).
//        h_read_exprtl1(postfixop(F,L,O), C, Term, Precedence, Answer, S, S1) :-
//        Precedence >= O, C =< L, !,
//        =..(Expr,[apply,F,Term]),
//        h_read_peepop(S1, S2),
//        h_read_exprtl(S2, O, Expr, Precedence, Answer, S).
//        % HiLog and
//        h_read_exprtl1(',', C, Term, Precedence, Answer, S, S1) :-
//        Precedence >= 1000, C < 1000, !,
//        h_read(S1, 1000, Next, S2), /*!,*/
//        h_read_exprtl(S2, 1000, apply(',',Term,Next), Precedence, Answer, S).
//        % HiLog or
//        h_read_exprtl1('|', C, Term, Precedence, Answer, S, S1) :-
//        Precedence >= 1100, C < 1100, !,
//        h_read(S1, 1100, Next, S2), /*!,*/
//        h_read_exprtl(S2, 1100, apply(';',Term,Next), Precedence, Answer, S).
//
//
//        %   This business of syntax errors is tricky.  When an error is detected,
//        %   we have to write out a message.  We also have to note how far it was
//        %   to the end of the input, and for this we are obliged to use the data-
//        %   base.  Then we fail all the way back to h_read(), and that prints the
//        %   input list with a marker where the error was noticed.  If subgoal_of
//        %   were available in compiled code we could use that to find the input
//        %   list without hacking the data base.  The really hairy thing is that
//        %   the original code noted a possible error and backtracked on, so that
//        %   what looked at first sight like an error sometimes turned out to be
//        %   a wrong decision by the parser.  This version of the parser makes
//        %   fewer wrong decisions, and my goal was to get it to do no backtracking
//        %   at all.  This goal has not yet been met, and it will still occasionally
//        %   report an error message and then decide that it is happy with the input
//        %   after all.  Sorry about that.
//
///*  Modified by D.S. Warren, Sep 10 1985, to print remainder of
//    token list only, and not use record/recorded */
//
//        h_read_syntax_error(Message, List) :-
//        nl, print('**'),
//        print_list(Message),nl,
//        print('** Tokens skipped:'),
//        print_list(List),
//        fail.
//
//        print_list([]) :- nl.
//        print_list([Head|Tail]) :-
//        tab(1),
//        print_token(Head),
//        print_list(Tail).
//
//        print_token(atom(X))    :- !, print(X).
//        print_token(var(_V,X))   :- !, print(X).
//        print_token(integer(X)) :- !, print(X).
//        print_token(string(X))  :- !, print(X).
//        print_token(X)          :-    print(X).
//
//        /*  	An attempt at defining the "curr_op" predicate for read.	   */
//
///* could add the clause:
//	op(Prec,Assoc,Op) :- assert_fact(h_read_curr_op(Prec,Assoc,Op)).
//   to implement op */
//
//        h_read_curr_op(1200,xfx,(':-')).
//        h_read_curr_op(1200,xfx,('-->')).
//        h_read_curr_op(1200,fx,(':-')).
//        h_read_curr_op(1198,xfx,('::-')).
//        h_read_curr_op(1100,xfy,';').
//        h_read_curr_op(1050,xfy,'->').
//        h_read_curr_op(1000,xfy,',').
//        h_read_curr_op(900,fy,not).
//        h_read_curr_op(900,fy,'\+').
//        h_read_curr_op(900,fy,spy).
//        h_read_curr_op(900,fy,nospy).
//        h_read_curr_op(700,xfx,'=').
//        h_read_curr_op(700,xfx,is).
//        h_read_curr_op(700,xfx,'=..').
//        h_read_curr_op(700,xfx,'==').
//        h_read_curr_op(700,xfx,'\==').
//        h_read_curr_op(700,xfx,'@<').
//        h_read_curr_op(700,xfx,'@>').
//        h_read_curr_op(700,xfx,'@=<').
//        h_read_curr_op(700,xfx,'@>=').
//        h_read_curr_op(700,xfx,'=:=').
//        h_read_curr_op(700,xfx,'=\=').
//        h_read_curr_op(700,xfx,'<').
//        h_read_curr_op(700,xfx,'>').
//        h_read_curr_op(700,xfx,'=<').
//        h_read_curr_op(700,xfx,'>=').
//        h_read_curr_op(661,xfy,'.').	/* !! */
//        h_read_curr_op(500,yfx,'+').
//        h_read_curr_op(500,yfx,'-').
//        h_read_curr_op(500,yfx,'/\').
//        h_read_curr_op(500,yfx,'\/').
//        h_read_curr_op(500,fx,'+').
//        h_read_curr_op(500,fx,'-').
//        h_read_curr_op(400,yfx,'*').
//        h_read_curr_op(400,yfx,'/').
//        h_read_curr_op(400,yfx,'//').
//        h_read_curr_op(400,yfx,'<<').
//        h_read_curr_op(400,yfx,'>>').
//        h_read_curr_op(300,xfx,mod).
//        h_read_curr_op(200,xfy,'^').
//
//
///*
//%   File   : RDTOK.PL
//%   Author : R.A.O'Keefe
//%   Updated: 5 July 1984
//%   Purpose: Tokeniser in reasonably standard Prolog.
//*/
///*  This tokeniser is meant to complement the library READ routine.
//    It recognises Dec-10 Prolog with the following exceptions:
//
//        %( is not accepted as an alternative to {
//
//        %) is not accepted as an alternative to )
//
//        NOLC convention is not supported (read_name could be made to do it)
//
//        ,.. is not accepted as an alternative to | (hooray!)
//
//        large integers are not read in as xwd(Top18Bits,Bottom18Bits)
//
//        After a comma, "(" is read as ' (' rather than '('.  This does the
//        parser no harm at all, and the Dec-10 tokeniser's behaviour here
//        doesn't actually buy you anything.  This tokeniser guarantees never
//        to return '(' except immediately after an atom, yielding ' (' every
//        other where.
//
//    In particular, radix notation is EXACTLY as in Dec-10 Prolog version 3.53.
//    Some times might be of interest.  Applied to an earlier version of this file:
//        this code took                  1.66 seconds
//        the Dec-10 tokeniser took       1.28 seconds
//        A Pascal version took           0.96 seconds
//    The Dec-10 tokeniser was called via the old RDTOK interface, with
//    which this file is compatible.  One reason for the difference in
//    speed is the way variables are looked up: this code uses a linear
//    list, while the Dec-10 tokeniser uses some sort of tree.  The Pascal
//    version is the program WLIST which lists "words" and their frequencies.
//    It uses a hash table.  Another difference is the way characters are
//    classified: the Dec-10 tokeniser and WLIST have a table which maps
//    ASCII codes to character classes, and don't do all this comparison
//    and and memberchking.  We could do that without leaving standard Prolog,
//    but what do you want from one evening's work?
//*/
//
///*  Modified by Saumya Debray to be compatible with C-Prolog syntax.  This
//    involved (i) deleting "public" and "mode" declarations, and (ii)
//    replacing ttynl/0 by nl/0, ttyput/1 by put/1.  (Apr 6, 1985)	*/
//
///*
//%   h_read_tokens(TokenList, Dictionary)
//%   returns a list of tokens.  It is needed to "prime" read_tokens/2
//%   with the initial blank, and to check for end of file.  The
//%   Dictionary is a list of AtomName=Variable pairs in no particular order.
//%   The way end of file is handled is that everything else FAILS when it
//%   hits character "26", sometimes printing a warning.  It might have been
//%   an idea to return the atom 'end_of_file' instead of the same token list
//%   that you'd have got from reading "end_of_file. ", but (1) this file is
//%   for compatibility, and (b) there are good practical reasons for wanting
//%   this behaviour. */
//
//        h_read_tokens(TokenList, Dictionary) :-
//        h_read_tokens(32, Dict, ListOfTokens),
//        append(Dict, [], Dict), !, /*  fill in the "hole" at the end */
//        Dictionary = Dict,              /*  unify explicitly so we read and */
//        TokenList = ListOfTokens.       /*  then check even with filled in */
//        /*  arguments */
//        h_read_tokens([atom(end_of_file)], []).   /*  only thing that can go wrong */
//
///*  read_tokens/3 modified by Saumya Debray : June 18, 1985 : to consist
//     of a single clause with a search-tree structure over it that permits
//     more efficient compiled code to be generated.  The tree is skewed, so
//     that those characters expected to be encountered more often are
//     closer to the top of the tree (the assumption here is that lower
//     case letters are the most frequent, followed by upper case letters
//     and numbers).  The old code follows the new code, but with each
//     occurrence of "read_tokens" replaced by "old_read_tokens".		*/
//
//        h_read_tokens(Ch,Dict,Tokens) :-
//        ((Ch >= 97,
//        ((Ch =< 122, Tokens = [atom(A)|TokRest],
//        h_read_name(Ch,S,NextCh), name(A,S),
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ) ;
//        (Ch > 122,
//        ((Ch =:= 124, Tokens = ['|'|TokRest],
//        get0(NextCh), h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 124,
//        ((Ch =:= 123, Tokens = ['{'|TokRest], get0(NextCh),
//        h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 123,
//        ((Ch =:= 125, Tokens = ['}'|TokRest], get0(NextCh),
//        h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 125, Tokens = [atom(A)|TokRest], get0(AnotherCh),
//        h_read_symbol(AnotherCh,Chars,NextCh), name(A,[Ch|Chars]),
//        h_read_aft_atom(NextCh,Dict,Tokens)
//        ))))))))) ;
//        (Ch < 97,
//        ((Ch < 65,
//        ((Ch < 48,
//        ((Ch =< 39,
//        ((Ch =< 34,
//        ((Ch =< 32,
//        ((Ch >= 0,
//        ((Ch =:= 26, fail) ;
//        (Ch =\= 26,
//        get0(NextCh), h_read_tokens(NextCh,Dict,Tokens)
//        )
//        )
//        ) ;
//        (Ch < 0, fail)
//        )
//        ) ;
//        (Ch > 32,
//        ((Ch =:= 33, Tokens = [atom('!')|TokRest],
//        get0(NextCh), h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 33, Tokens = [string(S)|TokRest],
//        h_read_string(S,34,NextCh),
//        h_read_tokens(NextCh,Dict,TokRest)
//        ))))
//        ) ;
//        (Ch > 34,
//        ((Ch =< 37,
//        ((Ch =:= 37, h_read_skip_comment,
//        get0(NextCh), h_read_tokens(NextCh,Dict,Tokens)
//        ) ;
//        (Ch =\= 37,
//        Tokens = [atom(A)|TokRest],
//        h_read_name(Ch,S,NextCh), name(A,S),
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        )
//        )
//        ) ;
//        (Ch > 37, Tokens = [atom(A)|TokRest],
//        ((Ch =:= 39,
//        h_read_string(S,39,NextCh), name(A,S),
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 39,
//        get0(AnotherCh), h_read_symbol(AnotherCh,Chars,NextCh),
//        name(A,[Ch|Chars]),
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ))))))
//        ) ;
//        (Ch > 39,
//        ((Ch =< 42,
//        ((Ch =:= 40, Tokens = [' ('|TokRest],
//        get0(NextCh), h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 40,
//        ((Ch =:= 41, Tokens = [')'|TokRest],
//        % HiLog		      get0(NextCh), h_read_tokens(NextCh,Dict,TokRest)
//        get0(NextCh), h_read_aft_atom(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 41, Tokens = [atom(A)|TokRest],
//        get0(AnotherCh), h_read_symbol(AnotherCh,Chars,NextCh),
//        name(A,[Ch|Chars]),
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ))))
//        ) ;
//        (Ch > 42,
//        ((Ch =:= 44, Tokens = [','|TokRest],
//        get0(NextCh), h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 44,
//        ((Ch =:= 46, get0(NextCh),
//        h_read_fullstop(NextCh,Dict,Tokens)
//        ) ;
//        (Ch =\= 46,
//        ((Ch =:= 47,
//        get0(NextCh), h_read_solidus(NextCh,Dict,Tokens)
//        ) ;
//        (Ch =\= 47, Tokens = [atom(A)|TokRest],
//        get0(AnotherCh), h_read_symbol(AnotherCh,Chars,NextCh),
//        name(A,[Ch|Chars]),
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ))))))))))
//        ) ;
//        (Ch >= 48,
//        ((Ch =< 57, Tokens = [integer(I)|TokRest],
//        h_read_integer(Ch,I,NextCh),
//        %HiLog		h_read_tokens(NextCh,Dict,TokRest)
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ) ;
//        (Ch > 57,
//        ((Ch =:= 59, Tokens = [atom((';'))|TokRest],
//        get0(NextCh), h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 59, Tokens = [atom(A)|TokRest],
//        get0(AnotherCh), h_read_symbol(AnotherCh,Chars,NextCh),
//        name(A,[Ch|Chars]), h_read_aft_atom(NextCh,Dict,TokRest)
//        ))))))
//        ) ;
//        (Ch >= 65,
//        ((Ch =< 90, Tokens = [var(Var,Name)|TokRest],
//        h_read_name(Ch,S,NextCh), name(Name,S),
//        h_read_lookup(Dict, Name=Var),
//        %HiLog	      h_read_tokens(NextCh,Dict,TokRest)
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ) ;
//        (Ch > 90,
//        ((Ch =< 93,
//        ((Ch =:= 91, Tokens = ['['|TokRest],
//        get0(NextCh), h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 91,
//        ((Ch =\= 92, Tokens = [']'|TokRest],
//        get0(NextCh),
//        %HiLog		    h_read_tokens(NextCh,Dict,TokRest)
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =:= 92, Tokens = [atom(A)|TokRest],
//        get0(AnotherCh),
//        h_read_symbol(AnotherCh,Chars,NextCh),
//        name(A,[Ch|Chars]),
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ))))
//        ) ;
//        (Ch > 93,
//        ((Ch =:= 95, Tokens = [var(Var,Name)|TokRest],
//        h_read_name(Ch,S,NextCh),
//        ((S = "_", Name = '_') ;
//        (name(Name,S), h_read_lookup(Dict, Name=Var))
//        ),
//        %HiLog		  h_read_tokens(NextCh,Dict,TokRest)
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 95, Tokens = [atom(A)|TokRest],
//        get0(AnotherCh), h_read_symbol(AnotherCh,Chars,NextCh),
//        name(A,[Ch|Chars]),
//        h_read_aft_atom(NextCh,Dict,TokRest)
//        )))))))))
//        ).
//
//        h_read_skip_comment :-
//        repeat,
//        get0(Ch),
//        (Ch = 10 ; Ch < 0 ; Ch = 31 ; Ch = 26),
//        !,
//        Ch =\= 26,
//        Ch > 0.		/*  fail on EOF */
//
//
//
///*
//%   The only difference between h_read_aft_atom(Ch, Dict, Tokens) and
//%   read_tokens/3 is what they do when Ch is "(".  rd_aft_atom
//%   finds the token to be '(', while read_tokens finds the token to be
//%   ' ('.  This is how the parser can tell whether <atom> <paren> must
//%   be an operator application or an ordinary function symbol application.
//%   See the library file READ.PL for details. */
//
///*  Modified by Saumya Debray : June 18, 1985 : to use the conditional
//    to avoid both the cut and the laying down of the choice point.	*/
//
///* **********************************************************************
//h_read_aft_atom(40, Dict, ['('|Tokens]) :- !,
//        get0(NextCh),
//        h_read_tokens(NextCh, Dict, Tokens).
//h_read_aft_atom(Ch, Dict, Tokens) :-
//        h_read_tokens(Ch, Dict, Tokens).
//********************************************************************** */
//
//        h_read_aft_atom(Ch,Dict,Tokens) :-
//        ((Ch =:= 40, Tokens = ['('|TokRest], get0(NextCh),
//        h_read_tokens(NextCh,Dict,TokRest)
//        ) ;
//        (Ch =\= 40, h_read_tokens(Ch,Dict,Tokens))
//        ).
//
///*
//%   h_read_string(Chars, Quote, NextCh)
//%   reads the body of a string delimited by Quote characters.
//%   The result is a list of ASCII codes.  There are two complications.
//%   If we hit the end of the file inside the string this predicate FAILS.
//%   It does not return any special structure.  That is the only reason
//%   it can ever fail.  The other complication is that when we find a Quote
//%   we have to look ahead one character in case it is doubled.  Note that
//%   if we find an end-of-file after the quote we *don't* fail, we return
//%   a normal string and the end of file character is returned as NextCh.
//%   If we were going to accept C-like escape characters, as I think we
//%   should, this would need changing (as would the code for 0'x).  But
//%   the purpose of this module is not to present my ideal syntax but to
//%   present something which will read present-day Prolog programs. */
//
//        h_read_string(Chars, Quote, NextCh) :-
//        get0(Ch),
//        h_read_string(Ch, Chars, Quote, NextCh).
//
//
//        h_read_string(Eofsym, _, Quote, Eofsym) :-
//        (Eofsym is 26; Eofsym is -1),  /* new */
//        print('! end of line or file in '), put(Quote),
//        print(token), put(Quote), nl,
//        !, fail.
//        h_read_string(Quote, Chars, Quote, NextCh) :- !,
//        get0(Ch),                               /* closing or doubled quote */
//        h_read_more_string(Ch, Quote, Chars, NextCh).
//        h_read_string(Char, [Char|Chars], Quote, NextCh) :-
//        h_read_string(Chars, Quote, NextCh).      /* ordinary character */
//
//
//        h_read_more_string(Quote, Quote, [Quote|Chars], NextCh) :- !,
//        h_read_string(Chars, Quote, NextCh).      /* doubled quote */
//        h_read_more_string(NextCh, _, [], NextCh).             /* end */
//
//
///*
//%   h_read_solidus(Ch, Dict, Tokens)
//%   checks to see whether /Ch is a /* comment or a symbol.  If the
//%   former, it skips the comment.  If the latter it just calls read_symbol.
//%   We have to take great care with /* comments to handle end of file
//%   inside a comment, which is why read_solidus/2 passes back an end of
//%   file character or a (forged) blank that we can give to read_tokens.
//*/
//
//        h_read_solidus(42, Dict, Tokens) :- !,
//        get0(Ch),
//        h_read_solidus(Ch, NextCh),
//        h_read_tokens(NextCh, Dict, Tokens).
//        h_read_solidus(Ch, Dict, [atom(A)|Tokens]) :-
//        h_read_symbol(Ch, Chars, NextCh),         /* might read 0 chars */
//        name(A, [47|Chars]),
//        h_read_tokens(NextCh, Dict, Tokens).
//        h_read_solidus(Ch, LastCh) :-
//        Ch =:= -1,print('! end of file in /*comment'), nl;
//        Ch =\= -1,
//        (Ch =:= 26,print('! end of file in /*comment'), nl;
//        Ch =\= 26,get0(NextCh),
//        (Ch =:= 42,
//        (NextCh =\= 47, h_read_solidus(NextCh,LastCh);
//        NextCh =:= 47, LastCh=32)
//        ;
//        Ch =\= 42, h_read_solidus(NextCh,LastCh)
//        )
//        ).
//
///*
//%   h_read_name(Char, String, LastCh)
//%   reads a sequence of letters, digits, and underscores, and returns
//%   them as String.  The first character which cannot join this sequence
//%   is returned as LastCh. */
//
//        /* modified by Saumya Debray : June 18, 1985 : to use search tree structure */
//
//        h_read_name(Ch,ChList,LastCh) :-
//        ((Ch >= 65,
//        ((Ch =< 90, ChList = [Ch | Chars],
//        get0(NextCh), h_read_name(NextCh, Chars, LastCh)
//        ) ;
//        (Ch > 90,
//        ((Ch =:= 95, ChList = [Ch | Chars],
//        get0(NextCh), h_read_name(NextCh, Chars, LastCh)
//        ) ;
//        (Ch =\= 95,
//        ((Ch >= 97,
//        ((Ch =< 122, ChList = [Ch | Chars],
//        get0(NextCh), h_read_name(NextCh, Chars, LastCh)
//        ) ;
//        (Ch > 122, ChList = [], LastCh = Ch)
//        )) ;
//        (Ch < 97, ChList = [], LastCh = Ch)
//        )))))) ;
//        (Ch < 65,
//        ((Ch >= 48,
//        ((Ch =< 57, ChList = [Ch | Chars], get0(NextCh),
//        h_read_name(NextCh,Chars,LastCh)
//        ) ;
//        (Ch > 57, ChList = [], LastCh = Ch)
//        )) ;
//        (Ch < 48,
//        ((Ch =:= 36, ChList = [Ch | Chars], get0(NextCh),
//        h_read_name(NextCh,Chars,LastCh)
//        ) ;
//        (Ch =\= 36,
//        ChList = [], LastCh = Ch)
//        )
//        )
//        ))).
//
///* **********************************************************************
//h_read_name(Char, [Char|Chars], LastCh) :-
//        ( Char >= 97, Char =< 122       % a..z
//        ; Char >= 65, Char =< 90        % A..Z
//        ; Char >= 48, Char =< 57        % 0..9
//        ; Char = 95                     %  _
//        ), !,
//        get0(NextCh),
//        h_read_name(NextCh, Chars, LastCh).
//h_read_name(LastCh, [], LastCh).
//********************************************************************** */
///*
//%   h_read_symbol(Ch, String, NextCh)
//%   reads the other kind of atom which needs no quoting: one which is
//%   a string of "symbol" characters.  Note that it may accept 0
//%   characters, this happens when called from read_fullstop. */
//
//        h_read_symbol(Char, [Char|Chars], LastCh) :-
//        /*        memberchk(Char, "#$&*+-./:<=>?@\^`~"), */
//        h_read_chkspec(Char),
//        !,
//        get0(NextCh),
//        h_read_symbol(NextCh, Chars, LastCh).
//        h_read_symbol(LastCh, [], LastCh).
//
//        h_read_chkspec(0'#).	% '#' 35
//        h_read_chkspec(0'$).	% '$' 36
//        h_read_chkspec(0'&).	% '&' 38
//        h_read_chkspec(0'*).	% '*' 42
//        h_read_chkspec(0'+).	% '+' 43
//        h_read_chkspec(0'-).	% '-' 45
//        h_read_chkspec(0'.).	% '.' 46
//        h_read_chkspec(0'/).	% '/' 47
//        h_read_chkspec(0':).	% ':' 58
//        h_read_chkspec(0'<).	% '<' 60
//        h_read_chkspec(0'=).	% '=' 61
//        h_read_chkspec(0'>).	% '>' 62
//        h_read_chkspec(0'?).	% '?' 63
//        h_read_chkspec(0'@).	% '@' 64
//        h_read_chkspec(0'\).	% '\' 92
//        h_read_chkspec(0'^).	% '^' 94
//        h_read_chkspec(0'`).	% '`' 96
//h_read_chkspec(0'~).	% '~' 126
//
///*
//%   h_read_fullstop(Char, Dict, Tokens)
//%   looks at the next character after a full stop.  There are
//%   three cases:
//%       (a) the next character is an end of file.  We treat this
//%           as an unexpected end of file.  The reason for this is
//%           that we HAVE to handle end of file characters in this
//%           module or they are gone forever; if we failed to check
//%           for end of file here and just accepted .<EOF> like .<NL>
//%           the caller would have no way of detecting an end of file
//%           and the next call would abort.
//%       (b) the next character is a layout character.  This is a
//%           clause terminator.
//%       (c) the next character is anything else.  This is just an
//%           ordinary symbol and we call read_symbol to process it.
//*/
//
//h_read_fullstop(26, _, _) :- !,
//        print('! end of file just after full stop'), nl,
//        fail.
//h_read_fullstop(Ch, _, []) :-
//        Ch =< 32, !.            /* END OF CLAUSE */
//h_read_fullstop(Ch, Dict, [atom(A)|Tokens]) :-
//        h_read_symbol(Ch, S, NextCh),
//        name(A, [46|S]),
//        h_read_tokens(NextCh, Dict, Tokens).
//
//
///*
//%   read_integer is complicated by having to understand radix notation.
//%   There are three forms of integer:
//%       0 ' <any character>     - the ASCII code for that character
//%       <digit> ' <digits>      - the digits, read in that base
//%       <digits>                - the digits, read in base 10.
//%   Note that radix 16 is not understood, because 16 is two digits,
//%   and that all the decimal digits are accepted in each base (this
//%   is also true of C).  So 2'89 = 25.  I can't say I care for this,
//%   but it does no great harm, and that's what Dec-10 Prolog does.
//%   The X =\= 26 tests are to make sure we don't miss an end of file
//%   character.  The tokeniser really should be in C, not least to
//%   make handling end of file characters bearable.  If we hit an end
//%   of file inside an integer, read_integer will fail.
//*/
//
//h_read_integer(BaseChar, IntVal, NextCh) :-
//        Base is BaseChar - 48,
//        get0(Ch),
//        Ch =\= 26,
//        (   Ch =\= 39, h_read_digits(Ch, Base, 10, IntVal, NextCh)
//        ;   Base >= 1, h_read_digits(0, Base, IntVal, NextCh)
//        ;   get0(IntVal), IntVal =\= 26, get0(NextCh)
//        ),  !.
//
//h_read_digits(SoFar, Base, Value, NextCh) :-
//        get0(Ch),
//        Ch =\= 26,
//        h_read_digits(Ch, SoFar, Base, Value, NextCh).
//
//h_read_digits(Digit, SoFar, Base, Value, NextCh) :-
//        Digit >= 48, Digit =< 57,
//        !,
//        Next is SoFar*Base-48+Digit,
//        h_read_digits(Next, Base, Value, NextCh).
//h_read_digits(LastCh, Value, _, Value, LastCh).
//
//
///*
//%   read_lookup is identical to memberchk except for argument order and
//%   mode declaration.
//*/
//
//h_read_lookup([X|_], X) :- !.
//h_read_lookup([_|T], X) :-
//        h_read_lookup(T, X).
//
//
