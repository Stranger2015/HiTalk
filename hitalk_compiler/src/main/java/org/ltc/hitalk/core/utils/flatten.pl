flatten(clause(Args,B0,P), clause(NArgs,B1,P)) :-
        flt_args_head(Args, NArgs, B2, B1, 1),     % different
        flt_body(B0, B2).

/* old code
flatten(clause(Args,B0,P), clause(NArgs,B1,P)) :-
	flt_args(Args, NArgs, B2, B1, 1, 0),
	flt_body(B0, B2).
*/

/*======================================================================*/
/* flt_args(+OldArgs, -NewArgs, +OldGoal, -NewGoal, +Level, +InIs)      */
/*      Flattens the arguments in the head of a clause; it is written   */
/*      this way so that arguments appear in a left-to-right order in   */
/*      the beginning of the body.                                      */
/*======================================================================*/

flt_args_head([], [], OldGoal, OldGoal, _).
flt_args_head([Arg|Args], [NArg|NArgs], OldGoal, NewGoal, Level) :-
        flt_args_head(Args, NArgs, OldGoal, MedGoal, Level),
        flt_arg_head(Arg, NArg, MedGoal, NewGoal, Level).

flt_arg_head(varocc(Var), varocc(Var), OldGoal, OldGoal, _).
%%flt_arg_head(integer(Int), integer(Int), OldGoal, OldGoal, _).
flt_arg_head(integer(Int), New, OldGoal, NewGoal, Level) :-
	level_allowed(Allowed),
	(Level =< Allowed
	 ->	New = integer(Int), NewGoal = OldGoal
	 ;	functor(IntBox,'$BOX$',3),
		(Int @= IntBox
		 ->	gennum(Vid), New = varocc(Vid),
			NewGoal = and(inlinegoal('=',2,[New,integer(Int)]),OldGoal)
		 ;	New = integer(Int), NewGoal = OldGoal
		)
	).
flt_arg_head(real(Real), New, OldGoal, NewGoal, Level) :-
	level_allowed(Allowed),
	(Level =< Allowed
	 ->	New = real(Real), NewGoal = OldGoal
	 ;	gennum(Vid), New = varocc(Vid),
		NewGoal = and(inlinegoal('=',2,[New,real(Real)]),OldGoal)
	 ).
flt_arg_head(constant(Sym), constant(Sym), OldGoal, OldGoal, _).
flt_arg_head(structure(Sym,Args), New, OldGoal, NewGoal, Level) :-
        level_allowed(Allowed),
	(is_small_int_list(Sym,Args,String,[])
	 ->	(Level =< Allowed
		 ->	New = string(String), NewGoal = OldGoal
		 ;	gennum(Vid), New = varocc(Vid),
			NewGoal = and(inlinegoal('=',2,[New,string(String)]),OldGoal)
		)
	 ;	NStr = structure(Sym,NArgs),
		Level1 is Level + 1,
		flt_args_head(Args, NArgs, OldGoal, MedGoal, Level1),
		(Level =< Allowed
		 ->	New = NStr, NewGoal = MedGoal
		 ;	gennum(Vid), New = varocc(Vid),
			NewGoal = and(inlinegoal('=',2,[New,NStr]),MedGoal)
		)
	).
/*======================================================================*/
/* is_small_int_list(+Sym,+Args,-IntList0,-IntList)			*/
/*======================================================================*/

/* is "- string at least 2 symbols long */
is_small_int_list(sym('.',2,_,_),[integer(N),structure(Sym,Args)],[N|IL0],IL) :-
	is_small_int_list0(Sym,Args,IL0,IL).

is_small_int_list0(sym('.',2,_,_),[integer(N),Symbol],[N|IL0],IL) :-
	N >= 0, N =< 16'ffff',
	(Symbol = constant(sym([],0,_,_))
	 ->	IL = IL0
	 ;	Symbol = structure(Sym,Args),
		is_small_int_list(Sym,Args,IL0,IL)
	).

/*======================================================================*/
/* flt_body(+Body, -FlattenedBody)					*/
/*======================================================================*/

flt_body(and(and(A,B),C),Gs) :- !, flt_body(and(A,and(B,C)),Gs).
flt_body(and(G1,G2), Gs) :-
	flt_body(G1, G3),
	(last_and_goal(G3,inlinegoal(fail,0,[]))
	 ->	Gs = G1
	 ;	flt_body(G2, G4),
		(G3 == inlinegoal(true,0,[])
		 ->	Gs = G4
		 ;	G4 == inlinegoal(true,0,[])
		 ->	Gs = G3
		 ;	append_and(G3,G4,Gs)  %%Gs = and(G3,G4)
		)
	).
%%flt_body(and(G1,G2), and(G3,G4)) :-
%%	flt_body(G1, G3), flt_body(G2, G4).
flt_body(or(G1,G2), or(G3,G4)) :-
	flt_body(G1, G3), flt_body(G2, G4).
flt_body(if_then_else(TG,G1,G2), if_then_else(FTG,G3,G4)) :-
	flt_body(TG,FTG), flt_body(G1, G3), flt_body(G2, G4).
flt_body(goal(Sym,Args), NewGoal) :-
	flt_args(Args, NArgs, goal(Sym,NArgs), NewGoal, 1, 0).
flt_body(inlinegoal(InlP,Arity,Args), NewGoal) :-
	flt_inline(InlP,Arity,Args,NewGoal).

last_and_goal(and(_,G),LG) :- !, last_and_goal(G,LG).
last_and_goal(G,G).

append_and(and(G1,G2),G3,and(G1,G4)) :- !, append_and(G2,G3,G4).
append_and(G1,G2,and(G1,G2)).

flt_inline(=,2,Args,NewGoal) :-
	Args = [Arg1,Arg2],
	(Arg1 == Arg2
	 ->	NewGoal = inlinegoal(true,0,[])
	 ; is_not_var(Arg1), is_not_var(Arg2)
	 ->	(Arg1 = structure(Sym1,Args1), Arg2 = structure(Sym2,Args2),
		 Sym1 = Sym2, length(Args1,Len), length(Args2,Len)
		 ->	equate_args(Args1,Args2,NewGoal)
		 ;	NewGoal = inlinegoal(fail,0,[])
		)
	 ;	flt_args(Args, NArgs, inlinegoal(=,2,NArgs), NewGoal, 1, 0)
	).
flt_inline(is,2,Args, NewGoal) :- !,
	flt_args(Args, NArgs, inlinegoal(is,2,NArgs), NewGoal, 1, 1).
flt_inline(P,2,Args, NewGoal) :-
	arithrelop(P),
	!,
	flt_args(Args, NArgs, inlinegoal(P,2,NArgs), NewGoal, 1, 1).
flt_inline(P,A,Args, NewGoal) :-
	flt_args(Args, NArgs, inlinegoal(P,A,NArgs), NewGoal, 1, 0).

equate_args([Arg1],[Arg2],NewGoal) :- !,
	flt_inline(=,2,[Arg1,Arg2],NewGoal).
%%equate_args([Arg1|Args1],[Arg2|Args2],and(FirstNewGoal,NewGoal)) :-
equate_args([Arg1|Args1],[Arg2|Args2],NewGoals) :-
	flt_inline(=,2,[Arg1,Arg2],FirstNewGoal),
	equate_args(Args1,Args2,NewGoal),
	append_and(FirstNewGoal,NewGoal,NewGoals).

is_not_var(A) :- \+ A = varocc(_).

/*======================================================================*/
/* flt_arg(+OldArgs, -NewArgs, +OldGoal, -NewGoal, +Level, +InIs)	*/
/*======================================================================*/

flt_args([], [], OldGoal, OldGoal, _, _InIs).
flt_args([Arg|Args], [NArg|NArgs], OldGoal, NewGoal, Level, InIs) :-
	flt_arg(Arg, NArg, OldGoal, MedGoal, Level, InIs),
	flt_args(Args, NArgs, MedGoal, NewGoal, Level, InIs).

/*======================================================================*/
/* flt_arg(+OldArg, -NewArg, +OldGoal, -NewGoal, +Level, +InIs)		*/
/*									*/
/*	InIs| Level>allowd   Level<=allowd				*/
/*	-----------------------------------				*/
/*	0   |  no flatten   flatten to '='				*/
/*	1   |  no flatten   flatten to 'is'				*/
/*									*/
/*	When function translation is allowed, all functions are		*/
/*	flattened except in the case when a function occurs within	*/
/*	is/2 or an arithmetic comparison operator (=:=/2, </2, etc)	*/
/*	at the allowed level.						*/
/*======================================================================*/

flt_arg(varocc(Var), varocc(Var), OldGoal, OldGoal, _, _InIs).
%%flt_arg(integer(Int), integer(Int), OldGoal, OldGoal, _Level, _InIs).
flt_arg(integer(Int), New, OldGoal, NewGoal, Level, InIs) :-
	level_allowed(Allowed),
	(Level =< Allowed
	 ->	New = integer(Int), NewGoal = OldGoal
	 ; InIs =:= 1
	 ->	New = integer(Int), NewGoal = OldGoal
	 ;	functor(IntBox,'$BOX$',3),
		(Int @= IntBox
		 ->	gennum(Vid), New = varocc(Vid),
			NewGoal = and(inlinegoal('=',2,[New,integer(Int)]),OldGoal)
		 ;	New = integer(Int), NewGoal = OldGoal
		)
	).
flt_arg(real(Real), New, OldGoal, NewGoal, Level, InIs) :-
	level_allowed(Allowed),
	(Level =< Allowed
	 ->	New = real(Real), NewGoal = OldGoal
	 ; InIs =:= 1
	 ->	New = real(Real), NewGoal = OldGoal
	 ;	gennum(Vid), New = varocc(Vid),
		NewGoal = and(inlinegoal('=',2,[New,real(Real)]),OldGoal)
	).
flt_arg(constant(Sym), constant(Sym), OldGoal, OldGoal, _, _InIs).
flt_arg(structure(Sym,Args), New, OldGoal, NewGoal, Level, InIs) :-
	level_allowed(Allowed),
	(is_small_int_list(Sym,Args,String,[])
	 ->	(Level =< Allowed
		 ->	New = string(String), NewGoal = OldGoal
		 ;	gennum(Vid), New = varocc(Vid),
			NewGoal = and(inlinegoal('=',2,[New,string(String)]),OldGoal)
		)
	 ;	NStr = structure(Sym, NArgs),
		Level1 is Level + 1,
		flt_args(Args, NArgs, MedGoal, NewGoal, Level1, InIs),
		( Level =< Allowed, New = NStr, MedGoal = OldGoal
		 ;	Level > Allowed, gennum(Vid), New = varocc(Vid),
			( InIs =:= 1,
			 MedGoal = and(inlinegoal(is, 2, [New,NStr]), OldGoal)
			 ;	InIs =\= 1,
				MedGoal = and(inlinegoal('=', 2, [New,NStr]), OldGoal)
			)
		)
	).

