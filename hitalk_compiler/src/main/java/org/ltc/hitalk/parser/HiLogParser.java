package org.ltc.hitalk.parser;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.TermFactory;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.ParseException;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.term.io.HiTalkStream;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.ltc.hitalk.term.HlOpSymbol.Associativity.fx;
import static org.ltc.hitalk.wam.compiler.Language.HILOG;

/**
 *
 */
public class HiLogParser extends PlPrologParser {

    public static final String HILOG_APPLY = "$hilog_apply";
    public static int hilogApply = -2;

    /**
     * Builds a
     * public
     * prolog parser on a token source to be parsed.
     *
     * @param interner  the  interner for variable and functor names.
     */
    protected Set <HtFunctor> hilogFunctors = new HashSet <>();

    /**
     * @param stream
     * @param interner
     * @param factory
     * @param optable
     */
    public HiLogParser ( HiTalkStream stream,
                         IVafInterner interner,
                         TermFactory factory,
                         IOperatorTable optable ) {
        super(stream, interner, factory, optable);

        hilogApply = interner.internFunctorName(HILOG_APPLY, 0);
    }

    //    @Override
    protected IFunctor compound ( String name, ListTerm args ) throws IOException, ParseException {
        IFunctor result;
        if (hilogFunctors.contains(name)) {
            result = factory.newFunctor(hilogApply, name, args);// :- hilog p, q, pi/N =>
// hilog p/_ q/_, pi_N/N, pi_1/1, piA1_A2/1-2.
        } else {
            result = super.compound(name, args);
        }

        return result;
    }

    @Override
    public Language language () {
        return HILOG;
    }

    /**
     * Interns and inserts into  the  operator table all  of   the  built in operators and functors in Prolog.
     */
    public void initializeBuiltIns () {
        super.initializeBuiltIns();

        internOperator(PrologAtoms.HILOG, 1150, fx);
    }
}
//%% Copyright (C) 1990 SUNY at Stony Brook
//
//        hilog :-
//        repeat,
//        write('HiLog> '),
//        h_read(A, Vs),
//        (
//        A == end_ of _file
//        ;
//        A == halt
//        ;
//        transBody(A, R),
//        call(R),
//        hilog_report(Vs) ->
//        nl, write('yes'), nl, fail
//        ;
//        nl, write('no'),  nl, fail
//        ),
//        !.
//
//        hilog_report([]).
//        hilog_report([V = T | Us]) :-
//        nl, write(V), write(' = '), h_write(T),
//
//        member(U = S, Us),
//        write(','),
//        nl, write(U), write(' = '), h_write(S),
//        fail
//        ;
//        write(' '),
//        get0(Char),
//        (
//        Char == 10
//        ;
//        skip(10),
//        Char \== 59
//        ).
//
//        %%
//        %% htest
//        %%
//        %% Use this instead  of  hilog to test only  the  translation from HiLog to
//        %% Prolog.
//        %%
//
//        htest :-
//        repeat,
//        write('> '),
//        h_read(A),
//        (
//        A == end_ of _file
//        ;
//        A == halt
//        ;
//        transClause(A, R, Links),
//        nl,
//        write('input:  '), write(A), nl, nl,
//        write('output: '), write(R), nl, nl ->
//        member(Link, Links),
//        write('link:   '), write(Link),
//        nl,
//        fail
//        ;
//        nl,
//        fail
//        ),
//        !.
//
//        %%
//        %% transClause
//        %%
//        %% Translate a HiLog clause to a Prolog clause plus  the  list  of 
//        %% additional linking  clauses  we need to get to  the  Prolog clause from
//        %% o the r translated clause bodies.
//        %%
//
//        transClause(HiLogClause, PrologClause, Links) :-
//        hilogHeadBody(HiLogClause, HiLogHead, HiLogBody),
//        transPredicate(HiLogHead, PrologHead),
//        makeLinks(HiLogHead, PrologHead, Links),
//        (
//        HiLogBody == true ->
//        PrologClause = PrologHead
//        ;
//        transBody(HiLogBody, PrologBody),
//        PrologClause = (PrologHead :- PrologBody)
//        ).
//
//        %%
//        %% transBody(+HiLogFormula, -PrologFormula).
//        %%
//        %% Translate  the  body  of  a HiLog clause:
//        %%
//        %% (1) add guard conditions to possibly non-rigid calls,
//        %% (2) translate meta-predicates to Prolog meta-predicates, and
//        %% (3) translate normal predicates.
//        %%
//
//        transBody(A, R) :-
//        var(A) ->
//        girdNonRigid(c(A), A, R)
//        ;
//        nonrigid(A, Var) ->
//        transPredicate(A, B),
//        girdNonRigid(B, Var, R)
//        ;
//        transMeta(A, R) ->
//        true
//        ;
//        transPredicate(A, R).
//
//        %% Currently, we only look for calls that  are non-rigid at compile
//        %% time.  For example, in A(X)(Y), we check at run-time that  A is
//        %% bound, but we won't check to see whe the r it is bound to something
//        %% rigid.  This catches obvious programming errors with little cost.
//        %% To make  the  test more rigorous, but slower, just replace nonvar(Var)
//        %% below with nonrigid(Var) (I think).
//
//        girdNonRigid(A, Var, (nonvar(Var) -> A; failNonRigid(A))).
//
//        failNonRigid(A) :-
//        %% Use current_predicate/2 to test for $h_nonrigid to keep
//        %% SB-Prolog quiet.
//        current_predicate('$h_nonrigid', '$h_nonrigid') ->
//        call(A)
//        ;
//        nl, write('Failing non-rigid call.'), nl,
//        fail.
//
//        %%
//        %% transMeta(+HiLogFormula, -PrologFormula).
//        %%
//
//        %% Handle unary and binary meta-predicates.
//
//        transMeta(apply(\+, A), \+ R) :-
//        transBody(A, R).
//        transMeta(apply(not, A), not(R)) :-
//        transBody(A, R).
//
//        transMeta(apply(',', A, B), (R, S)) :-
//        transBody(A,R),
//        transBody(B,S).
//        transMeta(apply(';', A, B), (R; S)) :-
//        transBody(A, R),
//        transBody(B, S).
//        transMeta(apply('->', A, B), (R -> S)) :-
//        transBody(A, R),
//        transBody(B, S).
//
//        transMeta(apply('^', A, B), A ^ R) :-
//        transBody(B, R).
//        transMeta(apply(bag of , A, B, C), bag of (A, R, C)) :-
//        transBody(B, R).
//        transMeta(apply(set of , A, B, C), set of (A, R, C)) :-
//        transBody(B, R).
//        transMeta(apply(findall, A, B, C), findall(A, R, C)) :-
//        transBody(B, R).
//
//        %%
//        %% transPredicate(+HiLogPredicate, -PrologPredicate).
//        %%
//        %% Construct  the  "flattened" Prolog term corresponding to a HiLog term.
//        %%
//
//        transPredicate(A, R) :-
//        var(A) ->
//        R = c(A)
//        ;
//        predicateName(A, Name, Args),
//        name(F, Name),
//        B =.. [F | Args],
//        transTerm(B, C),
//        (transProlog(C, R) -> true ; R = C).
//
//        %%
//        %% predicateName(+HiLogTerm, -Name, -Args).
//        %%
//        %% For  the  given HiLog term, construct  the  name  of   the  functor  of   the 
//        %% corresponding "flattened" Prolog term and its arguments.
//        %%
//
//        predicateName(A, "apply", [A]) :-
//        var(A).
//        predicateName(A, Name, []) :-
//        atom(A),
//        name(A, Name).
//        predicateName(A, FullName, FullArgs) :-
//        functor(A, apply, N),
//        A =.. [apply, F | Args],
//        predicateName(F, BaseName, BaseArgs),
//        Arity is N - 1,
//        name(Arity, Digits),
//        append(BaseName, [0'_ | Digits], FullName),
//        append(BaseArgs, Args, FullArgs).
//
//        %%
//        %% transTerm(+HiLogTerm, -PrologTerm).
//        %%
//        %% As a run-time efficiency gesture, convert all terms  of   the  form
//        %% apply('.', X, Xs) to  the  form [X | Xs].
//        %%
//
//        transTerm(A, R) :-
//        A = apply(F, X, Xs),
//        F == '.' ->
//        transTerm(X, Y),
//        transTerm(Xs, Ys),
//        R = [Y | Ys]
//        ;
//        ( var(A); atomic(A) ) ->
//        R = A
//        ;
//        fresh(A, R),
//        transArgs(1, A, R).
//
//        transArgs(I, A, R) :-
//        arg(I, A, X),
//        arg(I, R, Y) ->
//        transTerm(X, Y),
//        J is I + 1,
//        transArgs(J, A, R)
//        ;
//        true.
//
//        %%
//        %% makeLinks(+HiLogTerm, +PrologTerm, -Links)
//        %%
//        %% Compute  the  list  of  additional  clauses  we need to get to a
//        %% translated clause from o the r translated clause bodies.
//        %%
//
//        makeLinks(H, _, []) :-
//        var(H).
//        makeLinks(H, P, [(c(HiLog) :- Prolog) | Links]) :-
//        nonvar(H),
//
//        %% Make fresh templates for  the  HiLog and Prolog terms.
//        h_fresh(H, HiLog),
//        fresh(P, Prolog),
//
//        %% Convert  the  HiLog template to a series  of  terms which will
//        %% be  the  heads  of   the  link  clauses .  At  the  same time, get  the 
//        %% innermost functor and all  the  variables  of   the  template.
//        makeLinkHeads(HiLog, Heads, F, Args),
//
//        %% Unify  the  variables in  the  HiLog template and  the  Prolog
//        %% template and compose  the  link heads with  the  Prolog template
//        %% to make  the  link  clauses .   the re are two cases to consider:
//        %%
//        %% (1)  the  predicate is rigid.   the n  the  innermost functor is
//        %% not a variable, and  the  Prolog template should unify only
//        %% with  the  HiLog template arguments.
//        %%
//        %% (2)  the  predicate is non-rigid.   the n  the  innermost functor
//        %% is a variable, and  the  Prolog template should unify with  the 
//        %% functor and  the  arguments.  Fur the rmore,  the  first link head
//        %% returned by makeLinkHeads should be ignored, as it will have
//        %%  the  same form as  the  Prolog template.
//        (
//        nonvar(F) ->
//        Prolog =.. [_ | Args],
//        makeLink clauses (Heads, Prolog, Links)
//        ;
//        Prolog =.. [_, F | Args],
//        Heads = [_ | Heads1],
//        makeLink clauses (Heads1, Prolog, Links)
//        ).
//
//        makeLink clauses ([], _, []).
//        makeLink clauses ([Head | Heads], Body, [(Head :- Body) | Links]) :-
//        makeLink clauses (Heads, Body, Links).
//
//        %%
//        %% makeLinkHeads(+HiLogTerm, -LinkHeads, -Functor, -Args)
//        %%
//        %% Make  the  list  of  heads for link  clauses  by peeling one layer  of 
//        %% apply/N at a time from HiLogTerm.  In addition, determine  the 
//        %% innermost functor and collect all  the  arguments  of  HiLogTerm.
//        %%
//        makeLinkHeads(A, Heads, F, Args) :-
//        makeLinkHeads(A, [], [], Heads, F, Args).
//
//        makeLinkHeads(A, Args, _, [], A, Args) :-
//        var(A);
//        atom(A).
//
//        makeLinkHeads(A, Args, Suffix, [Head | Heads], X, Xs) :-
//        functor(A, apply, N),
//
//        %% A is  the  peeled HiLog template, and Args is  the  list  of 
//        %% arguments we have already peeled from  the  original template.
//        %%
//        %% Peel one more layer from A, and add  the  new arguments to  the 
//        %% argument list.
//        A =.. [apply, F | NewArgs],
//        append(NewArgs, Args, HeadArgs),
//
//        %% Put  the  arity  of  this layer into  the  link name suffix and
//        %% generate a new link name.
//        Arity is N - 1,
//        name(Arity, Digits),
//        append([0'_ | Digits], Suffix, HeadSuffix),
//        append("apply", HeadSuffix, HeadName),
//        name(H, HeadName),
//
//        %% Generate  the  link head.
//        Head =.. [H, F | HeadArgs],
//
//        %% Continue with  the  next layer.
//        makeLinkHeads(F, HeadArgs, HeadSuffix, Heads, X, Xs).
//
//        %%
//        %% fresh
//        %%
//        %% Make a new Prolog term with fresh variables for all  the  arguments.
//        %%
//
//        fresh(A, B) :-
//        functor(A, F, N),
//        functor(B, F, N).
//
//        %%
//        %% h_fresh
//        %%
//        %% Make a new HiLog term with fresh variables for all  the  arguments.
//        %%
//
//        h_fresh(A, B) :-
//        var(A) ->
//        true
//        ;
//        functor(A, apply, N),
//        arg(1, A, F) ->
//        functor(B, apply, N),
//        arg(1, B, G),
//        h_fresh(F, G)
//        ;
//        B = A.
//
//        %%
//        %% hilogHeadBody(+Clause, ?Head, ?Body)
//        %%
//        %% Unify Head and Body with  the  head and body  of   the  clause in Clause.
//        %% If Clause is a unit clause,  the n  the  Body is true.
//        %%
//
//        hilogHeadBody(A, Head, Body) :-
//        A = apply(F, Head, Body),
//        F == (:-) ->
//        true
//        ;
//        Head = A,
//        Body = true.
//
//        %%
//        %% nonrigid(HiLogPredicate).
//        %% nonrigid(HiLogPredicate, Variable).
//        %%
//        %% Satisfied if  the  predicate is "non-rigid" meaning that   the  innermost
//        %% functor is a variable.  nonrigid/2 also gives  the  innermost functor.
//        %%
//
//        nonrigid(A) :-
//        nonrigid(A, _).
//
//        nonrigid(A, Var) :-
//        var(A) ->
//        Var = A
//        ;
//        functor(A, apply, _),
//        arg(1, A, F),
//        nonrigid(F, Var).
//
//        %%
//        %% Simple builtins.
//        %%
//
//        transProlog( '=_2'(X, Y) 		, X =   Y ).
//        transProlog( '==_2'(X, Y)		, X ==  Y ).
//        transProlog( '\==_2'(X, Y)		, X \== Y ).
//        transProlog( '@<_2'(X, Y)		, X @<  Y ).
//        transProlog( '@>_2'(X, Y)		, X @>  Y ).
//        transProlog( '@=<_2'(X, Y)		, X @=< Y ).
//        transProlog( '@>=_2'(X, Y)		, X @>= Y ).
//        transProlog( '=:=_2'(X, Y)		, (is_2(A, X), is_2(B, Y), A =:= B) ).
//        transProlog( '=\=_2'(X, Y)		, (is_2(A, X), is_2(B, Y), A =\= B) ).
//        transProlog( '<_2'(X, Y)		, (is_2(A, X), is_2(B, Y), A <   B) ).
//        transProlog( '>_2'(X, Y)		, (is_2(A, X), is_2(B, Y), A >   B) ).
//        transProlog( '=<_2'(X, Y)		, (is_2(A, X), is_2(B, Y), A =<  B) ).
//        transProlog( '>=_2'(X, Y)		, (is_2(A, X), is_2(B, Y), A >=  B) ).
//        transProlog( append_3(X, Y, Z)		, append(X, Y, Z) ).
//        transProlog( assert_1(X) 		, h_assertz(X) ).
//        transProlog( asserta_1(X)		, h_asserta(X) ).
//        transProlog( assertz_1(X)		, h_assertz(X) ).
//        transProlog( atom_1(X)			, atom(X) ).
//        transProlog( atomic_1(X)		, atomic(X) ).
//        transProlog( clause_2(X, Y)		, h_clause(X, Y) ).
//        transProlog( compare_3(X, Y, Z)		, compare(X, Y, Z) ).
//        transProlog( compile_1(X)		, h_compile(X) ).
//        transProlog( consult_1(X)		, h_reconsult(X) ).
//        transProlog( consult_dynamic_1(X)	, h_reconsult_dynamic(X) ).
//        transProlog( copy_term_2(X, Y)		, copy_term(X, Y) ).
//        transProlog( current_atom_1(X)		, current_atom(X) ).
//        transProlog( current_op_3(X, Y, Z)	, current_op(X, Y, Z) ).
//        transProlog( current_predicate_2(X, Y)	, current_predicate(X, Y) ).
//        transProlog( display_1(X)		, h_writeq(X) ).
//        transProlog( erase_1(X)			, erase(X) ).
//        transProlog( expand_term_2(X, Y)	, expand_term(X, Y) ).
//        transProlog( float_1(X)			, float(X) ).
//        transProlog( get_1(X)			, get(X) ).
//        transProlog( get0_1(X)			, get0(X) ).
//        transProlog( instance_2(X, Y)		, instance(X, Y) ).
//        transProlog( integer_1(X)		, integer(X) ).
//        transProlog( length_2(X, Y)		, length(X, Y) ).
//        transProlog( name_2(X, Ys)		, name(X, Ys) ).
//        transProlog( nonvar_1(X)		, nonvar(X) ).
//        transProlog( nospy_1(X)			, nospy(X) ).
//        transProlog( number_1(X)		, number(X) ).
//        transProlog( numbervars_3(X, Y, Z)	, numbervars(X, Y, Z) ).
//        transProlog( op_3(X, Y, Z)		, op(X, Y, Z) ).
//        transProlog( phrase_2(X, Y)		, phrase(X, Y) ).
//        transProlog( phrase_3(X, Y, Z)		, phrase(X, Y, Z) ).
//        transProlog( portray_clause_1(X)	, portray_clause(X) ).
//        transProlog( print_1(X)			, print(X) ).
//        transProlog( put_1(X)			, put(X) ).
//        transProlog( reconsult_1(X)		, h_reconsult(X) ).
//        transProlog( reconsult_dynamic_1(X)	, h_reconsult_dynamic(X) ).
//        transProlog( recorda_3(X, Y, Z)		, recorda(X, Y, Z) ).
//        transProlog( recorded_3(X, Y, Z)	, recorded(X, Y, Z) ).
//        transProlog( recordz_3(X, Y, Z)		, recordz(X, Y, Z) ).
//        transProlog( retract_1(X)		, h_retract(X) ).
//        transProlog( retractall_1(X)		, h_retractall(X) ).
//        transProlog( see_1(X)			, see(X) ).
//        transProlog( seeing_1(X)		, seeing(X) ).
//        transProlog( skip_1(X)			, skip(X) ).
//        transProlog( sort_2(X, Y)		, sort(X, Y) ).
//        transProlog( spy_1(X)			, spy(X) ).
//        transProlog( statistics_2(X, Y)		, statistics(X, Y) ).
//        transProlog( tell_1(X)			, tell(X) ).
//        transProlog( telling_1(X)		, telling(X) ).
//        transProlog( tab_1(X)			, tab(X) ).
//        transProlog( var_1(X)			, var(X) ).
//        transProlog( write_1(X)			, h_write(X) ).
//        transProlog( writeq_1(X)		, h_writeq(X) ).
//        transProlog( write_canonical_1(X)	, h_writeq(X) ).
//
//        %%
//        %% Complex builtins.
//        %%
//
//        '._2'(X, Xs) :-
//        member(Name, [X | Xs]),
//        h_reconsult(Name),
//        fail
//        ;
//        true.
//
//        '=.._2'(Term, Xs) :-
//        Xs = [Term],
//        atomic(Term) ->
//        true
//        ;
//        Term =.. [apply | Xs],
//        Term \== apply('.', _, _).
//
//        arg_3(I, Term, Arg) :-
//        I > 0,
//        functor(Term, apply, _),
//        J is I + 1,
//        arg(J, Term, Arg).
//
//        functor_3(Term, Name, Arity) :-
//        atomic(Term) ->
//        Name = Term,
//        Arity = 0
//        ;
//        nonvar(Term) ->
//        arg(1, Term, Name),
//        functor(Term, apply, N),
//        Arity is N - 1
//        ;
//        Arity == 0 ->
//        atomic(Name),
//        Term = Name
//        ;
//        N is Arity + 1,
//        functor(Term, apply, N),
//        arg(1, Term, Name),
//        Term \== apply('.', _, _).
//
//        is_2(X, X)			:- number(X).
//        is_2(C, apply(-, X))		:- C is (-X).
//        is_2(C, apply(integer, X))	:- C is integer(X).
//        is_2(C, apply(float, X))	:- C is float(X).
//        is_2(C, apply(\, X))		:- C is \(X).
//        is_2(C, apply(+, X, Y))		:- is_2(A, X), is_2(B, Y), C is A + B.
//        is_2(C, apply(-, X, Y))		:- is_2(A, X), is_2(B, Y), C is A - B.
//        is_2(C, apply(*, X, Y))		:- is_2(A, X), is_2(B, Y), C is A * B.
//        is_2(C, apply(/, X, Y))		:- is_2(A, X), is_2(B, Y), C is A / B.
//        is_2(C, apply(//, X, Y))	:- is_2(A, X), is_2(B, Y), C is A // B.
//        is_2(C, apply(mod, X, Y))	:- is_2(A, X), is_2(B, Y), C is A mod B.
//        is_2(C, apply(/\, X, Y))	:- is_2(A, X), is_2(B, Y), C is A /\ B.
//        is_2(C, apply(\/, X, Y))	:- is_2(A, X), is_2(B, Y), C is A \/ B.
//        is_2(C, apply(<<, X, Y))	:- is_2(A, X), is_2(B, Y), C is A << B.
//        is_2(C, apply(>>, X, Y))	:- is_2(A, X), is_2(B, Y), C is A >> B.
//
//        %%
//        %% Translate a file  of  HiLog  clauses  to Prolog.
//        %%
//
//        h_compile(Name) :-
//        h_translate(Name, PFile),
//        compile_and_load(PFile).
//
//        h_reconsult(Name) :-
//        h_translate(Name, PFile),
//        reconsult(PFile).
//
//        h_translate(Name, PFile) :-
//        %% Open HiLog and Prolog files.
//        h_translate_name(Name, HFile, PFile),
//        seeing(OldSee),
//        telling(OldTell),
//        see(HFile),
//        tell(PFile),
//
//        %% Translate HiLog code and put it in  the  Prolog file.  Keep
//        %% track  of  which link  clauses  we will need.
//        repeat,
//        h_read(A),
//        (
//        A == end_ of _file
//        ;
//        transClause(A, B, Links),
//        portray_clause(B),
//        assertLinks(Links) ->
//        fail
//        ) ->
//        seen,
//        told,
//        see(OldSee),
//        tell(OldTell),
//
//        %% Compile link  clauses .
//        updateLinks.
//
//        h_translate_name(Name, HFile, PFile) :-
//        %% If  the  user is  the  input, put  the  Prolog code into user.ho.
//        Name == user ->
//        HFile = user,
//        PFile = 'user.ho'
//        ;
//        %% O the rwise, a file is  the  input.  Append ".hl" to  the  name
//        %% unless it's already  the re.   the  same name with ".ho" instead
//        %%  of  ".hl" is  the  name  of   the  intermediate Prolog file.
//        name(Name, Cs),
//        (
//        %% If  the  name ends in ".hl", strip it  of f to get  the  root.
//        append(Root, ".hl", Cs) ->
//        true
//        ;
//        %% O the rwise, use  the  whole name for  the  root.
//        Root = Cs
//        ),
//        append(Root, ".hl", Ds),
//        name(HFile, Ds),
//        append(Root, ".ho", Es),
//        name(PFile, Es).
//
//        %%
//        %% h_reconsult_dynamic
//        %%
//
//        h_reconsult_dynamic(X) :-
//        h_translate_name(X, File, _),
//        seeing(Old),
//        see(File),
//
//        retractall('$h_reconsult'(_, File)),
//        repeat,
//        h_read(A),
//        (
//        A == end_ of _file
//        ;
//        h_reconsult_clause(A, File) ->
//        fail
//        ) ->
//        seen,
//        see(Old),
//
//        %% Compile link  clauses .
//        updateLinks.
//
//        h_reconsult_clause(A, File) :-
//        hilogHeadBody(A, Head, _),
//        (
//        %% Clear away previous instances  of  this predicate.  Use
//        %% negation here to avoid instantiating anything in  the 
//        %% head (in case we have a non-rigid head).
//        \+ '$h_reconsult'(Head, File) ->
//        h_fresh(Head, Template),
//        h_retractall(Template),
//        asserta('$h_reconsult'(Template, File))
//        ;
//        true
//        ),
//
//        %% Now assert  the  clause.
//        h_assertz(A, Links),
//        assertLinks(Links).
//
//        %%
//        %% Database predicates.
//        %%
//        %% When a term is asserted, it becomes a clause visible to calls in  the 
//        %% program.   the refore, we translate HiLog  clauses  to Prolog  clauses 
//        %% for assertion.  We maintain a database '$h_clause' that  maps  the 
//        %% HiLog  clauses  to Prolog  clauses , so that  h_clause and h_retract can
//        %% operate properly.
//        %%
//        %%  the  order  of  assertions in h_asserta and h_assertz is important.
//        %%  the re are times when asserting  the  Prolog code will fail (at least
//        %% under Quintus Prolog).  We want to do this first, so we are't left
//        %% with dangling references in  the  clause map.  We also have to update
//        %%  the  links immediately.
//        %%
//
//        h_asserta(A) :-
//        transClause(A, B, Links),
//        mapClause(A, B, Map),
//        asserta(B),
//        asserta(Map),
//        assertLinks(Links),
//        updateLinks(Links).
//
//        h_assertz(A) :-
//        h_assertz(A, Links),
//        assertLinks(Links),
//        updateLinks(Links).
//
//        h_assertz(A, Links) :-
//        transClause(A, B, Links),
//        mapClause(A, B, Map),
//        assertz(B),
//        assertz(Map).
//
//        h_clause(Head, Body) :-
//        '$h_clause'((Head :- Body), _).
//
//        h_retract(A) :-
//        mapClause(A, B, Map),
//        retract(Map),
//        (retract(B) -> true).
//
//        h_retractall(X) :-
//        h_retract(X),
//        fail
//        ;
//        true.
//
//        mapClause(HiLogClause, PrologClause, Map) :-
//        hilogHeadBody(HiLogClause, Head, Body),
//        Map = '$h_clause'((Head :- Body), PrologClause).
//
//        %%
//        %% Support routines for links.
//        %%
//        %% Our preprocessor translates HiLog predicates to special Prolog
//        %% predicates.  This creates a problem since non-rigid HiLog calls will
//        %% not be able to find  the  Prolog predicates.   the  solution is to
//        %% create a series  of   clauses  for each translated predicate that  maps
//        %% non-rigid calls to  the  actual Prolog predicates.   the  link  clauses 
//        %% which we need are in  the  $h_link database.   the  link  clauses  which
//        %% we have compiled is in  the  $h_link_compiled database.
//        %%
//
//        %% Assert new links into  the  $h_link database.
//
//        assertLinks(Links) :-
//        member(Link, Links),
//        \+ '$h_link'(Link),
//        assertz('$h_link'(Link)),
//        fail
//        ;
//        true.
//
//        %% Compile and load links.
//
//        compileLinks :-
//        telling(Old),
//
//        %% Put all  the  links into  the  _links file.
//        (
//        '$h_link'(Link),
//        portray_clause(Link),
//        portray_clause('$h_link_compiled'(Link)),
//        fail
//        ;
//        true
//        tell('_links'),
//        ),
//        told,
//        tell(Old),
//
//        %% Compile and load  the  links.
//        compile_and_load('_links').
//
//        %% See if links need to be recompiled.
//
//        updateLinks :-
//        bag of (Link, '$h_link'(Link), Links),
//        updateLinks(Links).
//
//        updateLinks(Links) :-
//        member(Link, Links),
//        \+ '$h_link_compiled'(Link) ->
//        compileLinks
//        ;
//        true.
//==========================

//        Algorithm specialize(Program)
//
//        1.Collecting Call (partially instantiated) calls to predicates that  are defined in Program;

//        2.For each ci2C nd and associate with cii. the  set Sel(ci) of Program clauses that are immediately selected
//        by ci, and ii. the most specific generalisation gi of  the  set Heads(Sel(ci))[fcig;

//        3.Remove from Call calls that do not benefit 2C^Sel(c)=Si^msg(Heads(Si)[fcg) is a variant of gig;
//        5.For each equivalence class  of calls CSido/*LetSel(CSi)be the  set  of immediately selected clauses  of  the
//        calls in CSi,pibe the ir predicate symbol, and p0i be an new predicate symbol.*/i.
//        If(Sel(CSi)6=;) the n.
//        Choose ap0i-representative Ri=(HiBi) of Sel(CSi) for the callsCSi;.Foreach clause
//        Clij=(HeadijBodyij)2Sel(CSi)do Insert in Program the clause Cl0ij=(Head0ijBodyij)where Head 0ijis
//        the p0i-difference of Headijfrom the head Hi of  the representative Ri;ii.For each call cij of
//        the  equivalence class CSi, ndit sp0-specialisation c0ij(and associate it with cij);

//        6.For each equivalence class  of  calls CSido Replace through out Program all occurrences  of  call cij2CS i
//        by it sp0-specialisationc 0ij;///Figure2:  the   call specialisation algorithm.
//        and some argument register (put*) WAMinstructions  of the original HiLog program become unnecessary.
//
//         the se instructions are eliminated from both    the   specialised calls as well as from the
//         specialised versions  of    the   predicates.
//        Algorithm Specialise begins by collecting all ( partially instantiated ) calls to predicates that  are defined
//        in  program P.
//        We allow for open HiLog programs,that  is HiLog  programs for which the definitions of some predicates are
//        missing or are imported from the modules.
//        the second step of  the algorithm nds and associates wi the ach call ci, the  set  of program clauses Sel(ci)
//        whose heads unify with ci.Each of  the se sets contains the program clauses that have the potential of being
//        selected for the execution of callci during run-time.As mentioned in the previous section,in the absence of any
//        information about the context conditions of ci,or the success conditions of each clause in Sel(ci), the Sel(ci)
//        set is the best safe approximation of  clauses that might be chosen for the execution of ci at runtime.
//        Not all collected calls, however,can benefit from specialisation ;calls that do not