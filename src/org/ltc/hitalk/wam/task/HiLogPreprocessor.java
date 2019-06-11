package org.ltc.hitalk.wam.task;


import com.thesett.aima.logic.fol.*;
import org.ltc.hitalk.wam.transformers.ISpecializer;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public
class HiLogPreprocessor extends StandardPreprocessor <Term> implements ISpecializer {

    private final HiLogToPrologBiDiConverter converter;

    public
    HiLogPreprocessor ( ITransformer <Term> transformer, VariableAndFunctorInterner interner ) {
        super(null, transformer);
        converter = new HiLogToPrologBiDiConverter(interner);
    }


    public
    HiLogToPrologBiDiConverter getConverter () {
        return converter;
    }


    @Override
    public
    Clause specialize ( Clause clause ) {
        return clause;
    }


    class Specializer implements ISpecializer {

        @Override
        public
        Clause specialize ( Clause clause ) {

            return clause;
        }

        @Override
        public
        Term transform ( Term term ) {
            return term;
        }
    }

    /**
     * Collects all partially instantiated calls to ( HiLog ) predicates
     * defined within the module under compilation.
     * The algorithm for collecting the calls is quadratic in the number of predicates,
     * but "only" O(nlogn) in the number of partially instantiated calls to each predicate.
     */
    class PiCallsCollector {

        List <Functor> collect () {
            List <Functor> piCalls = new ArrayList <>();
            PredicateVisitor visitor = new PredicateVisitor() {
                @Override
                public
                void visit ( Predicate predicate ) {
                    Clause[] clauses = predicate.getBody();
                    for (Clause clause : clauses) {
                        if (clause.isQuery()) {
                            continue;
                        }
                        Functor[] body = clause.getBody();
//                        bod.TODO
                    }
                }

                @Override
                public
                void visit ( Term term ) {

                }
            };

            return piCalls;
        }
    }

}

///* File:      spec.P
// ** Author(s): Kostis F. Sagonas
// ** Contact:   xsb-contact@cs.sunysb.edu
// **
// ** Copyright (C) The Research Foundation of SUNY, 1986, 1993-1998
// **
// ** XSB is free software; you can redistribute it and/or modify it under the
// ** terms of the GNU Library General Public License as published by the Free
// ** Software Foundation; either version 2 of the License, or (at your option)
// ** any later version.
// **
// ** XSB is distributed in the hope that it will be useful, but WITHOUT ANY
// ** WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// ** FOR A PARTICULAR PURPOSE.  See the GNU Library General Public License for
// ** more details.
// **
// ** You should have received a copy of the GNU Library General Public License
// ** along with XSB; if not, write to the Free Software Foundation,
// ** Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
// **
// ** $Id: spec.P,v 1.11 2012-05-21 20:44:44 dwarren Exp $
// **
// 
//
//:- compiler_options([xpp_on,optimize,sysmod]).
//        #include "standard.h"
//
//
//        /*======================================================================
//        /*  This module of the XSB compiler specialises (partially evaluates)	
//        /*  partially instantiated calls to predicates of a source module.  It	
//        /*  essentially has the behaviour of a source-to-source transformation,	
//        /*  but the transformation is actually done in the internal source	
//        /*  format of the XSB compiler (a list of "pred(Sym,Clauses,Pragma)"	
//        /*  structures), which is a subpart of a "Module" data structure.	
//        /*======================================================================
//
//
//        /*======================================================================
//        /* spec(+ModuleIn, -ModuleOut)						
//        /*	ModuleOut is ModuleIn partially evaluated for p.i. calls to	
//        /*	predicates that are defined in ModuleIn.  Currently, modules	
//        /*	that do not contain partially instantiated calls to predicates	
//        /*	of the module are not affected.  Also, the QueryList (QR), and	
//        /*	the DeclarationList (DL) are not yet taken into account.	
//        /*======================================================================
//
//        spec(module(SymTab,DL,CL,QR,Pars), module(SymTab,DL,CL1,QR,Pars)) :-
//        initialise,
//        collect_pi_calls(CL, DL, PI_Calls),
//        interesting_calls(PI_Calls, CL, InterestingCalls),
//        merge_calls(InterestingCalls, MergedCalls),
//        %	telling(F), tell(userout), pp(MergedCalls), told, tell(F),
//        specialise(MergedCalls, SymTab, CLmid, CL),
//        new_clause_list(MergedCalls, CLmid, CL1),
//        generate_table_decl(DL, MergedCalls).
//
//        initialise :- conset('singleton call #', 0).
//
//
//        //======================================================================//
//        // collect_pi_calls(+Preds, +DL, -PI_Calls)				//
//        //	Collects all partially instantiated calls to (HiLog) predicates	//
//        //	defined within the module under compilation.  The algorithm for	//
//        //	collecting the calls is quadratic in the number of predicates,	//
//        //	but "only" O(nlogn) in the number of partially instantiated	//
//        /*	calls to each predicate.					
//        /*======================================================================
//
//        collect_pi_calls(Preds, DL, PI_Calls) :-
//        'collect pi calls'(Preds, Open_PI_Calls),
//        'well, add some more'(DL, Open_PI_Calls),
//        close_pi_calls(Open_PI_Calls, PI_Calls).
//
//        :- mode 'collect pi calls'(+,?).
//
//        'collect pi calls'([], _).
//        'collect pi calls'([pred(_,Clauses,_)|Preds], HB) :-
//        'collect from pred'(Clauses, HB),
//        'collect pi calls'(Preds, HB).
//
//        :- mode 'collect from pred'(+,?).
//
//        'collect from pred'([], _).
//        'collect from pred'([clause(_,Body,_)|CLs], HB) :-
//        chb(Body, HB),
//        'collect from pred'(CLs, HB).
//
//        :- mode chb(+,?).
//
//        chb(and(G1,G2), HB) :- chb(G1, HB), chb(G2, HB).
//        chb(or(G1,G2), HB) :- chb(G1, HB), chb(G2, HB).
//        chb(if(G1,G2), HB) :- chb(G1, HB), chb(G2, HB).
//        chb(not(G), HB) :- chb(G, HB).
//        chb(goal(Sym,Args), HB) :-
//        ( filter_goal(Sym, Args) ->	% partially instantiated (HiLog) call
//        memberchk(pi_calls(Sym,Calls), HB),	% memberchk
//        add_new_clause(body_call(Args), Calls)
//        ; true
//        ).
//        chb(inlinegoal(_,_,_), _).
//
//
//        :- mode filter_goal(+,+).
//
//        filter_goal(Sym, Args) :- sym_prop(defined, Sym), sth_bound(Args).
//
//        /*----------------------------------------------------------------------
//        /* 'well, add some more'(+DeclarationList, #Calls)			
//        /*	Adds some more calls to the list of p.i. calls that might	
//        /*	be specialised.  These calls are the user-supplied table	
//        /*	specialisation declarations.					
//        /*----------------------------------------------------------------------
//
//        'well, add some more'([], _).
//        'well, add some more'([Goal|Goals], Calls) :-
//        chb(Goal, Calls),
//        'well, add some more'(Goals, Calls).
//
//
//        /*======================================================================
//        /* interesting_calls(+CallList, +Preds, -InterestingCallList)		
//        /*	Finds interesting calls by filtering out all calls that either	
//        /*	cannot be specialised, or that will not benefit by the		
//        /*	specialisation algorithm.					
//        /*	The first case has to do with ensuring the soundness of clause	
//        /*	replacement.  The constraint that is enforced is CONSECUTIVE	
//        /*	CLAUSE SELECTION, but it is checked only when the "spec_repr"	
//        /*	option is on.							
//        /*	Calls that benefit from the specialisation algorithm are:	
//        /*	  i. calls that select a SUBSET of the clauses of a predicate.	
//        /*	 ii. calls that contain COMMON SUBTERMS	with the heads of the	
//        /*	     selected clauses.						
//        /*----------------------------------------------------------------------
//        /*  NOTE: The consecutive clauses constraint is necessary in general,	
//        /*	  for specialisation with clause replacement to be sound.	
//        /*	  However, one fine day, it can be lifted for those predicates	
//        /*	  that either the order of their clauses does not matter, or 	
//        /*	  in the presence of mode information about the predicate.	
//        /*======================================================================
//
//        interesting_calls([], _, []).
//        interesting_calls([pi_calls(Sym,C)|Cs], Preds, ICL) :-
//        Pred = pred(Sym,_Clauses,_),
//        (memberchk(Pred, Preds)  % may not be there if dynamic
//        ->	'interesting calls'(C, Pred, IC),
//        ( IC == [] -> ICL = ICs ; ICL = [pi_calls(Sym,IC)|ICs] ),
//        interesting_calls(Cs, Preds, ICs)
//        ;	interesting_calls(Cs, Preds, ICL)
//        ).
//
//        :- mode 'interesting calls'(+,+,-).
//
//        'interesting calls'([], _, []).
//        'interesting calls'([C|Cs], Pred, ICL) :-
//        ( interesting_indeed(C, Pred, NC) -> ICL = [NC|ICs] ; ICL = ICs ),
//        'interesting calls'(Cs, Pred, ICs).
//
//        :- mode interesting_indeed(+,+,-).
//
//        interesting_indeed(body_call(Args), pred(Sym,Clauses,_),
//        body_call(Args,SelectedClauses)) :-
//        find_selected_clauses(Args, Clauses, SelectedClauses),
//        ( SelectedClauses \== [] ->
//        ( option(spec_repr) ->
//        consecutive(Clauses, Front, SelectedClauses, Back),
//        check_for_cut(SelectedClauses, Back),
//        ( ( Front=[_|_] -> true ; Back=[_|_] )	% proper subset
//        ; 'give a second chance'(Args, SelectedClauses)
//        )
//        ; ( proper_subset(SelectedClauses, Clauses)
//        ; 'give a second chance'(Args, SelectedClauses)
//        )
//        )
//        ; sym_name(Sym, N, A),
//        warning(('A partially instantiated call to ',N,'/',A,' will fail!')),
//        fail
//        ).
//
//        :- mode  find_selected_clauses(+,+,-).
//        :- index find_selected_clauses/3-2.
//
//        find_selected_clauses(_, [], []).
//        find_selected_clauses(CallArgs, [Clause|Clauses], SCL) :-
//        Clause = clause(HeadArgs,_,_),
//        ( unifies_mnl(CallArgs, HeadArgs) -> SCL = [Clause|SCs] ; SCL = SCs ),
//        find_selected_clauses(CallArgs, Clauses, SCs).
//
//        /*----------------------------------------------------------------------
//        /* unifies_mnl(+CallArgs, +ClauseArgs)					
//        /*	Succeeds if the arguments of the call unify modulo nonlinearity	
//        /*	with the arguments of the clause.				
//        /*----------------------------------------------------------------------
//
//        unifies_mnl([], _).
//        unifies_mnl([Arg|Args], [HeadArg|HeadArgs]) :-
//        umnl_chk(Arg, HeadArg),
//        unifies_mnl(Args, HeadArgs).
//
//        :- mode umnl_chk(+,+).
//
//        umnl_chk(varocc(_), _).
//        umnl_chk(integer(Int), HeadArg) :-
//        ( HeadArg = varocc(_) -> true ; HeadArg = integer(Int) ).
//        umnl_chk(real(Real), HeadArg) :-
//        ( HeadArg = varocc(_) -> true ; HeadArg = real(Real) ).
//        umnl_chk(constant(Sym), HeadArg) :-
//        ( HeadArg = varocc(_) -> true ; HeadArg = constant(Sym) ).
//        umnl_chk(structure(Sym,Args), HeadArg) :-
//        ( HeadArg = varocc(_) -> true
//        ; HeadArg = structure(Sym,HeadArgs), unifies_mnl(Args, HeadArgs)
//        ).
//
//        /*----------------------------------------------------------------------
//        /* 'give a second chance'(+CallArgs, +SelectedClauses)			
//        /*	Gives a second chance to a partially instantiated call, (that	
//        /*	does not select a subset of the predicate's clauses), by	
//        /*	examining whether the call will benefit by the factoring of	
//        /*	common subexpressions property of the specialisation algorithm.	
//        /*	This is done by finding the most specific generalisation of	
//        /*	the call and the heads of the selected clauses, and checking	
//        /*	whether it contains any partially instantiated arguments.	
//        /*----------------------------------------------------------------------
//
//        'give a second chance'(CallArgs, SelectedClauses) :-
//        find_msg(CallArgs, SelectedClauses, Msg),
//        sth_bound(Msg).
//
//
//        /*======================================================================
//        /* merge_calls(+CallList, -/*	MergedList is a simplified version of CallList, where all calls	
//        /*	that select the same clauses are merged together.  This is done	
//        /*	since all these calls can use the same specialisation of the	
//        /*	predicate and the same representative.				
//        /*======================================================================
//
//        merge_calls([], []).
//        merge_calls([pi_calls(Sym,Calls)|Cs],
//        [pi_calls(Sym,MergedCalls,_Representable)|MCs]) :-
//        Calls = [One|Rest],	% known to be non-empty
//        'merge calls'(One, Rest, MergedCalls),
//        merge_calls(Cs, MCs).
//
//        :- mode 'merge calls'(+,+,-).
//
//        'merge calls'(body_call(Args,SelClauses), Calls,
//        [body_calls(CallMsg,SelClauses,[Args|SameSelect],_,_)|Merged]) :-
//        'find all delete'(Calls, SelClauses, SameSelect, Rest),
//        'find msg of calls'(Args, SameSelect, CallMsg),
//        ( Rest == [] -> Merged = []
//        ; Rest = [One|Rest1], 'merge calls'(One, Rest1, Merged)
//        ).
//
//        :- mode 'find all delete'(+,+,-,-).
//
//        'find all delete'([], _, [], []).
//        'find all delete'([Call|Calls], SelectedClauses, SS, Other) :-
//        Call = body_call(Args,SC),
//        ( SelectedClauses == SC -> SS = [Args|SS1], Other = Other1
//        ;  SS = SS1, Other = [Call|Other1]
//        ),
//        'find all delete'(Calls, SelectedClauses, SS1, Other1).
//
//        :- mode  'find msg of calls'(+,+,-).
//        :- index 'find msg of calls'/3-2.
//
//        'find msg of calls'(Args, [], Args).
//        'find msg of calls'(Args, [CallArgs|Calls], Msg) :-
//        msg(Args, CallArgs, M),
//        'find msg of calls'(M, Calls, Msg).
//
//
//        /*======================================================================
//        /* specialise(+InterestingMergedCalls, #SymTab, +CLIn,-CLOut)		
//        /*	Performs the actual specialisation of all call equivalence	
//        /*	classes that are worth specialising.				
//        /*======================================================================
//
//        specialise([], _, CL, CL).
//        specialise([pi_calls(Sym,Calls,Representable)|PICs], SymTab, CLin, CLout) :-
//        specialise_pred(Sym, Calls, Representable, SymTab, CLin, CLmid),
//        specialise(PICs, SymTab, CLmid, CLout).
//
//        /*----------------------------------------------------------------------
//        /* specialise_pred(+Sym, +Calls, +Representable, #SymTab, +CLin,-CLout)	
//        /*	Specialises calls to one predicate.  Specialisation without	
//        /*	clause replacement is always allowed, but specialisation with	
//        /*	clause replacement is only allowed when all equivalence classes	
//        /*	of calls have non-overlapping clause selection.			
//        /*----------------------------------------------------------------------
//
//        specialise_pred(Sym, Calls, Representable, SymTab, CLin, CLout) :-
//        sym_name(Sym, N, A),
//        ( sym_type(Sym,tabled(_,_)) -> Tabled = 1 ; Tabled = 0 ),
//        ( ( \+sym_type(Sym,aggregation(_)),
//        ( \+ option(spec_repr), Representable = 0
//        ; non_overlapping_selection(Calls), Representable = 1
//        )
//        ) ->
//        message(('% Specialising partially instantiated calls to ',
//        N, '/', A)),
//        'specialise pred'(Calls, N, A, Tabled, SymTab, CLin, CLout)
//        ; CLout = CLin, Representable = 0,
//        (sym_type(Sym,aggregation(_))
//        ->	  message(('% specialization of aggregate predicate not performed: ',N,'/',A))
//        ;	  warning(('Calls select overlapping clause blocks of predicate ',
//        N, '/', A)),
//        message(('           ',
//        '(specialisation with representation cannot be performed!)'))
//        )
//        ).
//
//        :- mode 'specialise pred'(+,+,+,+,?,?,?).
//
//        'specialise pred'([], _, _, _, _, CL, CL).
//        'specialise pred'([BC|BCs], N, A, Tabled, SymTab, CLin, CLout) :-
//        BC = body_calls(CallsMsg,SelClauses,Cs,NCs,Rep),
//        specialise_calls(CallsMsg, SelClauses, Cs, N, A, Tabled,
//        SymTab, NCs, Rep, NewPred),
//        ( var(NewPred) -> CLin = CLmid ; CLin = [NewPred|CLmid] ),
//        'specialise pred'(BCs, N, A, Tabled, SymTab, CLmid, CLout).
//
//        /*----------------------------------------------------------------------
//        /* specialise_calls(+CallsMsg, +SelClauses, +Calls, +N, +A, +Tabled,	
//        /*		    #SymTab, -NewCalls, -Representative, -NewPred)	
//        /*	Specialises a list of Calls that select the same set of clauses	
//        /*	of a predicate N/A.  In general, the specialisation involves	
//        /*	creating the representative of the selected clauses, a special	
//        /*	predicate, and specialised versions of the calls (NewCalls).	
//        /*----------------------------------------------------------------------
//
//        specialise_calls(CallsMsg, SelClauses, Calls, N, A, Tabled,
//        SymTab, NewCalls, Representative, NewPred) :-
//        ( singleton_sets_opt(SelClauses, Tabled, HeadArgs, Body) ->
//        singleton_special_calls(Calls, HeadArgs, Body, NewCalls)
//        ; find_msg(CallsMsg, SelClauses, Msg),
//        %	  typeout(('# Msg = ', Msg)),
//        represent(Msg, N, A, Tabled, SymTab, NewSym, Representative),
//        make_special_pred(SelClauses, Msg, NewSym, NewPred),
//        %	  typeout(('# NewPred = ', NewPred)),
//        make_special_calls(Calls, Msg, NewSym, NewCalls)
//        %	  typeout(('# NewCalls = ', NewCalls))
//        ).
//
//        /*----------------------------------------------------------------------
//        /* singleton_sets_opt(+SelectedClauses, +Tabled, -HeadArgs, -Body)	
//        /*	Checks if the singleton clause selection optimisation is	
//        /*	applicable and if so, returns the arguments of head and the	
//        /*	goal of the body of the selected clause.  The optimisation is	
//        /*	applicable iff the body contains just a single literal provided	
//        /*	that the termination characteristics of the program are not	
//        /*	changed by bypassing calls to tabled predicates.		
//        /*----------------------------------------------------------------------
//
//        singleton_sets_opt([clause(HeadArgs,Body,_)], Tabled, HeadArgs, Body) :-
//        Tabled =:= 0,	%% do not bypass a call to a tabled predicate
//        fail_if(option(unfold_off)),
//        ( Body = inlinegoal(_,_,_) -> true ; Body = goal(_,_) ).
//
//
//        /*----------------------------------------------------------------------
//        /* find_msg(+Args, +SelectedClauses, -Msg)				
//        /*	Given the arguments of a body call and a list of clauses with	
//        /*	which the call is known to unify (modulo nonlinearity), this	
//        /*	predicate incrementally computes the arguments of the most	
//        /*	specific generalisation of these terms.  Variables in the Msg	
//        /*	are not numbered.						
//        /*----------------------------------------------------------------------
//
//        find_msg(Args, [Clause|Clauses], Msg) :-
//        Clause = clause(HeadArgs,_,_),
//        msg(Args, HeadArgs, M),
//        ( Clauses == [] -> Msg = M ; find_msg(M, Clauses, Msg) ).
//
//        :- mode msg(+,+,-).
//
//        msg([], [], []).
//        msg([Arg1|Args1], [Arg2|Args2], [MsgArg|MsgArgs]) :-
//        msg_arg(Arg1, Arg2, MsgArg),
//        msg(Args1, Args2, MsgArgs).
//
//        :- mode msg_arg(+,+,-).
//
//        msg_arg(structure(Sym,Args1), structure(Sym,Args2), structure(Sym,MsgArgs)) :-
//        !,
//        msg(Args1, Args2, MsgArgs).
//        msg_arg(X, X, X) :-
//        fail_if(X = varocc(_)),		% forget numbers of variables.
//        !.
//        msg_arg(_, _, varocc(_)).
//
//
//        /*----------------------------------------------------------------------
//        /* represent(+Msg, +PredName, +PredArity, +Tabled,			
//        /*	     #SymTab, -NewSym, -Representative)				
//        /*	Creates a new predicate symbol and the representative of a set	
//        /*	of selected clauses.  Msg is the most specific generalisation	
//        /*	of the heads of these clauses and the calls that selected them,	
//        /*	and PredName is the predicate name of the clauses.		
//        /*----------------------------------------------------------------------
//
//        represent(Msg, Name, Arity, Tabled, SymTab, NewSym, Representative) :-
//        gensym_pred(Name, NewName),
//        msg_copy(Msg, MsgCopy, NewArgs, [], 0, NewArity),
//        Properties = [pred,defined,used|Prop],
//        ( Tabled =:= 0 -> Prop = []
//        ; Prop = [tabled],
//        warning(('The specialisation of ', Name, '/', Arity,
//        ' will cause double tabling !')),
//        ttywritenl('           (possible source of inefficiency)',STDWARN)
//        ),
//        sym_insert(NewName, NewArity, Properties, SymTab, NewSym),
//        Representative = clause(MsgCopy,goal(NewSym,NewArgs),_NewPrag).
//        %	typeout(('# Representative = ', Representative)).
//
//
//        /*----------------------------------------------------------------------
//        /* msg_copy(+Msg, -MsgCopy, +VarListIn,-VarListOut, +VidIn,-VidOut)	
//        /*	Makes a copy of the most specific generalisation, while at the	
//        /*	same time returns the list of variables appearing in Msg.  The	
//        /*	occurrences of variables in the outputs are numbered starting	
//        /*	from 1, and on return, VidOut contains the length of VarList.	
//        /*----------------------------------------------------------------------
//
//        msg_copy([], [], VL, VL, Vid, Vid).
//        msg_copy([Arg|Args], [ArgCopy|ArgCopies], VLin, VLout, VidIn, VidOut) :-
//        ( Arg = varocc(_) ->
//        VidMid is VidIn + 1,
//        ArgCopy = varocc(VidMid),
//        VLin = [ArgCopy|VLmid]
//        ; Arg = structure(Sym,StructArgs) ->
//        ArgCopy = structure(Sym,StructArgCopies),
//        msg_copy(StructArgs, StructArgCopies, VLin,VLmid, VidIn,VidMid)
//        ; Arg = ArgCopy, VLmid = VLin, VidMid = VidIn
//        ),
//        msg_copy(Args, ArgCopies, VLmid, VLout, VidMid, VidOut).
//
//
//        /*----------------------------------------------------------------------
//        /* make_special_pred(+SelClauses, +Msg, +NewSym, -SpecialPred)		
//        /*	Given a list of selected clauses and the Msg of the heads of	
//        /*	these clauses, creates a new list of clauses for the NewSym	
//        /*	predicate symbol.  These clauses are the ones that will replace	
//        /*	the selected ones.  A naive attempt to index these new clauses	
//        /*	at an (appropriate) argument is also made.			
//        /*----------------------------------------------------------------------
//
//        make_special_pred(SelClauses, Msg, NewSym, pred(NewSym,SpecialClauses,_)) :-
//        make_special_clauses(SelClauses, Msg, SpecialClauses, Heads),
//        ( index_euristic(Heads, Pos) -> sym_propin(index(Pos), NewSym) ; true ).
//
//        :- mode make_special_clauses(+,+,-,-).
//
//        make_special_clauses([], _, [], []).
//        make_special_clauses([clause(HeadArgs,Body,Prag)|Cls], Msg,
//        [clause(NewHeadArgs,Body,Prag)|SpecialCls],
//        [NewHeadArgs|NHAs]) :-
//        make_new_args(HeadArgs, Msg, NewHeadArgs, []),
//        make_special_clauses(Cls, Msg, SpecialCls, NHAs).
//
//        :- mode make_new_args(+,+,?,?).
//
//        make_new_args([], [], NHA, NHA).
//        make_new_args([Arg|Args], [MsgArg|MsgArgs], NHAin, NHAout) :-
//        make_new_arg(Arg, MsgArg, NHAin, NHAmid),
//        make_new_args(Args, MsgArgs, NHAmid, NHAout).
//
//        :- mode make_new_arg(+,+,?,?).
//
//        make_new_arg(structure(Sym,HeadArgs), structure(Sym,MsgArgs), NHAin, NHAout) :-
//        !,
//        make_new_args(HeadArgs, MsgArgs, NHAin, NHAout).
//        make_new_arg(Arg, Arg, NHA, NHA) :-
//        fail_if(Arg = varocc(_)),
//        !.
//        make_new_arg(Arg, _, [Arg|NHAmid], NHAmid).
//
//        /*----------------------------------------------------------------------
//        /* make_special_calls(+Calls, +Msg, +NewSym, -SpecialCalls)		
//        /*	Creates specialised versions of partially instantiated calls	
//        /*	by finding their arguments w.r.t. the most specific		
//        /*	generalisation of the calls and the heads of the selected	
//        /*	clauses.							
//        /*----------------------------------------------------------------------
//
//        make_special_calls([], _, _, []).
//        make_special_calls([Call|Calls], Msg, NewSym, [SpecCall|SpecCalls]) :-
//        make_special_call(Call, Msg, NewSym, SpecCall),
//        make_special_calls(Calls, Msg, NewSym, SpecCalls).
//
//        :- mode make_special_call(+,+,+,-).
//
//        make_special_call(CallArgs, Msg, NewSym, goal(NewSym,NewCallArgs)) :-
//        make_new_args(CallArgs, Msg, NewCallArgs, []).
//
//        /*----------------------------------------------------------------------
//        /* index_euristic(+HeadList, -Pos)					
//        /*	Makes a naive attempt to index the new predicate into some	
//        /*	appropriate position based on the arguments of the heads.  If	
//        /*	indexing has a chance of being useful, it succeeds, returning	
//        /*	the leftmost argument position in which there is some		
//        /*	discrimination.							
//        /*	A predicate does NOT need to be indexed if it has only one	
//        /*	clause or if its heads do not have arguments.			
//        /*----------------------------------------------------------------------
//        /*	Yes, I know the following is hairy but it does the job, and	
//        /*	it (usually) does it pretty well too!!  Can you see the only	
//        /*	case in which the following euristic will be fooled?		
//        /*	Hint: Transformational indexing will take care of it!		
//        /*----------------------------------------------------------------------
//
//        index_euristic(HeadList, Pos) :-
//        HeadList = [[Arg1|_],_|_],		% check indexing necessity
//        'index euristic'(Arg1, HeadList, 1, Pos).
//
//        :- mode 'index euristic'(+,+,+,-).
//
//        'index euristic'(Arg1, HeadList, PosIn, PosOut) :-
//        ( discrimination(Arg1, HeadList) -> PosOut = PosIn
//        ; shift_left(HeadList, ShiftedLeft),
//        ShiftedLeft = [[NewArg1|_]|_], PosMid is PosIn + 1,
//        'index euristic'(NewArg1, ShiftedLeft, PosMid, PosOut)
//        ).
//
//        :- mode discrimination(+,+).
//
//        discrimination(Arg, [[H|_]|R]) :-
//        fail_if((Arg=varocc(_) ; Arg=H)) ; discrimination(Arg, R).
//
//        :- mode shift_left(+,-).
//
//        shift_left([], []).
//        shift_left([[_|L]|T], [L|ST]) :- shift_left(T, ST).
//
//
//        /*----------------------------------------------------------------------
//        /* singleton_special_calls(+Calls, +HeadArgs, +Body, -SpecialCalls)	
//        /*	Creates specialised versions of partially instantiated calls	
//        /*	that select single clauses of certain types.  The new calls	
//        /*	are mostly conjunctions of =/2 goals (for the arguments of the	
//        /*	head), and a modified version of the literal in the body.	
//        /*	The new calls may contain a small number of unnecessary calls	
//        /*	to true/0, but since these calls get eliminated in later	
//        /*	phases of the compiler anyway, I see no reason of complicating	
//        /*	my life.							
//        /*----------------------------------------------------------------------
//
//        singleton_special_calls([], _, _, []).
//        singleton_special_calls([Call|Calls], HeadArgs, Body, [SpecCall|SpecCalls]) :-
//        singleton_special_call(Call, HeadArgs, Body, SpecCall),
//        singleton_special_calls(Calls, HeadArgs, Body, SpecCalls).
//
//        :- mode singleton_special_call(+,+,+,-).
//
//        singleton_special_call([], _, Body, NewBody) :-
//        'change vars in body'(Body, NewBody).
//        singleton_special_call([CArg|CArgs], [HArg|HArgs], Body, SpecCall) :-
//        'arg unification'(CArg, HArg, SpecCall, RestSpecCall),
//        singleton_special_call(CArgs, HArgs, Body, RestSpecCall).
//
//        :- mode 'arg unification'(+,+,?,?).
//
//        'arg unification'(CallArg, HeadArg, SpecCall, RestSpecCall) :-
//        ( HeadArg = varocc(Id) ->
//        name(Id, L), NL = [0'_, 0'v|L], atom_codes(NewId, NL),
//        SpecCall = and(inlinegoal(=,2,[CallArg,varocc(NewId)]),
//        RestSpecCall)
//        ; ( CallArg = varocc(_) ->
//        'change vars in arg'(HeadArg, NewArg),
//        SpecCall = and(inlinegoal(=,2,[CallArg,NewArg]),RestSpecCall)
//        ; CallArg = structure(_,CallArgs) ->
//        HeadArg = structure(_,HeadArgs),
//        singleton_special_call(CallArgs, HeadArgs,
//        inlinegoal(true,0,[]), StructCall),
//        SpecCall = and(StructCall,RestSpecCall)
//        ; SpecCall = RestSpecCall
//        )
//        ).
//
//        :- mode 'change vars in body'(+,-).
//
//        'change vars in body'(inlinegoal(P,A,Args), inlinegoal(NP,A,NewArgs)) :-
//        ( P == '!' -> NP = true ; NP = P ),
//        'change vars in args'(Args, NewArgs).
//        'change vars in body'(goal(Sym,Args), goal(Sym,NewArgs)) :-
//        'change vars in args'(Args, NewArgs).
//
//        :- mode 'change vars in args'(+,-).
//
//        'change vars in args'([], []).
//        'change vars in args'([Arg|Args], [NewArg|NewArgs]) :-
//        'change vars in arg'(Arg, NewArg),
//        'change vars in args'(Args, NewArgs).
//
//        :- mode 'change vars in arg'(+,-).
//
//        'change vars in arg'(varocc(Id), varocc(NewId)) :-
//        !,
//        name(Id, L), NL = [0'_, 0'v|L], name(NewId, NL).
//        'change vars in arg'(structure(Sym,Args), structure(Sym,NewArgs)) :-
//        !,
//        'change vars in args'(Args, NewArgs).
//        'change vars in arg'(X, X).
//
//
//        /*======================================================================
//        /* new_clause_list(+MergedCalls, +CLIn, -CLOut)				
//        /*======================================================================
//
//        new_clause_list([], CL, CL).
//        new_clause_list([pi_calls(Sym,Calls,Representable)|PICs], CLin, CLout) :-
//        ( \+sym_type(Sym,aggregation(_)), ( \+ option(spec_repr) ; Representable =:= 1 ) ->
//        'new clause list'(Calls, Sym, Representable, CLin, CLmid)
//        ; CLmid = CLin
//        ),
//        new_clause_list(PICs, CLmid, CLout).
//
//        :- mode 'new clause list'(+,+,+,?,?).
//
//        'new clause list'([], _, _, CL, CL).
//        'new clause list'([BC|BCs], Sym, Representable, CLin, CLout) :-
//        BC = body_calls(_,SelClauses,Cs,NCs,Rep),
//        substitute(CLin, Sym, Cs, Representable, SelClauses, NCs, Rep, CLmid),
//        'new clause list'(BCs, Sym, Representable, CLmid, CLout).
//
//        /*----------------------------------------------------------------------
//        /* substitute(+CLin, +Sym, +Calls, +Representable,			
//        /*	      +SelClauses, +NCalls, +Rep, -CLout)			
//        /*----------------------------------------------------------------------
//
//        substitute([], _, _, _, _, _, _, []).
//        substitute([Pred|Preds], Sym, Calls, Representable,
//        SelClauses, NCalls, Rep, [NP|NPs]) :-
//        Pred = pred(PredSym,CLin,Prag),
//        NP = pred(PredSym,CLout,Prag),
//        ( nonvar(Rep), Representable =:= 1, PredSym == Sym ->
//        subst_rep(CLin, SelClauses, Rep, CLmid)
//        ; CLmid = CLin
//        ),
//        subst_calls(CLmid, Sym, Calls, NCalls, CLout),
//        substitute(Preds,Sym,Calls,Representable,SelClauses,NCalls,Rep,NPs).
//
//        /*----------------------------------------------------------------------
//        /* subst_rep(+CLin, +SelClauses,+Representative, -CLout)		
//        /*	CLout is CLin where the selected clauses have been substituted	
//        /*	by their representative.  The selected clauses are known to be	
//        /*	consecutive (clause order is preserved).			
//        /*----------------------------------------------------------------------
//
//        subst_rep(CLin, SelClauses, Representative, CLout) :-
//        consecutive(CLin, Front, SelClauses, Back),
//        append(Front, [Representative|Back], CLout).
//
//        /*----------------------------------------------------------------------
//        /* subst_calls(+CLin, +Sym, +Calls, +NewCalls, -CLout)			
//        /*----------------------------------------------------------------------
//
//        subst_calls([], _, _, _, []).
//        subst_calls([Cl|Cls], Sym, Calls, NewCalls, [NewCl|NewCls]) :-
//        Cl = clause(Head,Body,Prag),
//        needs_new_body(Body, Sym, Calls, NewCalls, NewBody, Needed),
//        ( nonvar(Needed) -> NewCl = clause(Head,NewBody,Prag) ; NewCl = Cl ),
//        subst_calls(Cls, Sym, Calls, NewCalls, NewCls).
//
//        :- mode needs_new_body(+,+,+,+,-,-).
//
//        needs_new_body(and(G1,G2), CallSym, Calls, NewCalls, and(NG1,NG2), Needed) :-
//        needs_new_body(G1, CallSym, Calls, NewCalls, NG1, Needed),
//        needs_new_body(G2, CallSym, Calls, NewCalls, NG2, Needed).
//        needs_new_body(or(G1,G2), CallSym, Calls, NewCalls, or(NG1,NG2), Needed) :-
//        needs_new_body(G1, CallSym, Calls, NewCalls, NG1, Needed),
//        needs_new_body(G2, CallSym, Calls, NewCalls, NG2, Needed).
//        needs_new_body(if(G1,G2), CallSym, Calls, NewCalls, if(NG1,NG2), Needed) :-
//        needs_new_body(G1, CallSym, Calls, NewCalls, NG1, Needed),
//        needs_new_body(G2, CallSym, Calls, NewCalls, NG2, Needed).
//        needs_new_body(not(G), CallSym, Calls, NewCalls, not(NG), Needed) :-
//        needs_new_body(G, CallSym, Calls, NewCalls, NG, Needed).
//        needs_new_body(goal(Sym,Args), CallSym, Calls, NewCalls, NG, Needed) :-
//        ( Sym == CallSym, occurs_in(Args, Calls, NewCalls, NewCall) ->
//        ( NewCall = and(_,_) -> name_vars_uniquely(NewCall, Sym, NG)
//        ; NG = NewCall
//        ),
//        Needed = 1	/* just bind Needed 
//        ; NG = goal(Sym,Args)
//        ).
//        needs_new_body(inlinegoal(N,A,ArgList), _, _, _, inlinegoal(N,A,ArgList), _).
//
//        :- mode occurs_in(+,+,+,-).
//
//        occurs_in(A, [A|_], [B|_], B).
//        occurs_in(A, [_|B], [_|C], D) :- occurs_in(A, B, C, D).
//
//        /*----------------------------------------------------------------------
//        /* name_vars_uniquely(+Goal, +Sym, -NewGoal)				
//        /*	Renames all variables that were introduced as a result of	
//        /*	performing unfolding (due to singleton sets optimisations)	
//        /*	to unique variables, not appearing anywhere else in the clause.	
//        /*	This step is necessary for unfolding of multiple calls within	
//        /*	the same clause to be correct.					
//        /*----------------------------------------------------------------------
//
//        name_vars_uniquely(G, Sym, NG) :-
//        sym_name(Sym, N, A),
//        atom_codes(N, NL), name(A, AL), append([0'_|NL], [0'_|AL], PredL),
//        conget('singleton call #', C), NewC is C+1, name(NewC, CL),
//        conset('singleton call #', NewC), append(PredL, [0'_|CL], IdL),
//        'name vars uniquely'(G, IdL, NG).
//
//        :- mode 'name vars uniquely'(+,+,-).
//
//        'name vars uniquely'(and(G1,G2), IdL, and(NG1,NG2)) :-
//        'name vars uniquely'(G1, IdL, NG1),
//        'name vars uniquely'(G2, IdL, NG2).
//        'name vars uniquely'(inlinegoal(G,A,Args), IdL, inlinegoal(G,A,NewArgs)) :-
//        'name arg vars uniquely'(Args, IdL, NewArgs).
//        'name vars uniquely'(goal(Sym,Args), IdL, goal(Sym,NewArgs)) :-
//        'name arg vars uniquely'(Args, IdL, NewArgs).
//
//        :- mode 'name arg vars uniquely'(+,+,-).
//
//        'name arg vars uniquely'([], _, []).
//        'name arg vars uniquely'([Arg|Args], IdL, [NewArg|NewArgs]) :-
//        make_arg_vars_unique(Arg, IdL, NewArg),
//        'name arg vars uniquely'(Args, IdL, NewArgs).
//
//        :- mode make_arg_vars_unique(+,+,-).
//
//        make_arg_vars_unique(varocc(Id), IdL, varocc(NewId)) :-
//        !,
//        name(Id, IdName),
//        ( IdName = [0'_,0'v|_] ->
//        append(IdName, IdL, NewIdL), atom_codes(NewId, NewIdL)
//        ; NewId = Id
//        ).
//        make_arg_vars_unique(structure(Sym,Args), IdL, structure(Sym,NewArgs)) :-
//        !,
//        'name arg vars uniquely'(Args, IdL, NewArgs).
//        make_arg_vars_unique(X, _, X).
//
//
//        /*======================================================================
//        /* generate_table_decl(+DL, +MergedCalls)				
//        /*	Generates table declarations for some of the predicates that	
//        /*	were created by the specialisation.  The following code has	
//        /*	only side-effects.						
//        /*======================================================================
//
//        generate_table_decl([], _).
//        generate_table_decl([goal(Sym,Args)|Goals], PICs) :-
//        'generate table decl'(Sym, PICs, Args),
//        generate_table_decl(Goals, PICs).
//
//        :- mode 'generate table decl'(+,+,+).
//
//        'generate table decl'(Sym, PICs, Args) :-
//        ( memberchk(pi_calls(Sym,CallList,_Representable), PICs) ->
//        'search for call'(CallList, Args)
//        ; sym_name(Sym, N, A),
//        warning(('Table specialisation declaration for ', N, '/', A,
//        ' could not be ')),
//        ttywritenl(('           processed as intended... ',
//        '(tabling the whole predicate instead)'),STDWARN),
//        sym_propin(tabled, Sym)
//        ).
//
//        :- mode 'search for call'(+,+).
//
//        'search for call'([], _).
//        'search for call'([C|CL], Args) :-
//        C = body_calls(_,_,Cs,NCs,Rep),
//        ( memberchk(Args,Cs) ->
//        ( nonvar(Rep) ->
//        NCs = [goal(NewSym,_)|_],
//        sym_propin(tabled, NewSym)
//        ; error(('Singleton optimisations clash with table declaration')),
//        ttywritenl('         (please use compiler option "unfold_off")',
//        STDERR)
//        )
//        ; 'search for call'(CL, Args)
//        ).
//
//
//        /*======================================================================
//        /*  Auxilliary Predicates.						
//        /*======================================================================
//
//        /*----------------------------------------------------------------------
//        /* sth_bound(+ArgList)							
//        /*	Succeeds if there is a non-variable argument in ArgList.	
//        /*----------------------------------------------------------------------
//
//        sth_bound([Arg|_]) :- fail_if(Arg = varocc(_)), !.
//        sth_bound([_|Args]) :- sth_bound(Args).
//
//        /*----------------------------------------------------------------------
//        /* close_pi_calls(+Open_PI_Calls, -Closed_PI_Calls)			
//        /*	Closes an open ended-list of partially instantiated calls.	
//        /*----------------------------------------------------------------------
//
//        close_pi_calls([], []) :- !.	% HB is a open-ended list; close it!
//        close_pi_calls([pi_calls(Sym,Calls)|T], [pi_calls(Sym,CallList)|NT]) :-
//        clause_listify(Calls, CallList),
//        close_pi_calls(T, NT).
//
//        /*----------------------------------------------------------------------
//        /* proper_subset(+SubSet, +Set)						
//        /*	Succeeds iff Subset is a proper subset of Set.			
//        /*----------------------------------------------------------------------
//
//        proper_subset([], [_|_]).
//        proper_subset([_|SubTail], [_|SetTail]) :- proper_subset(SubTail, SetTail).
//
//        /*----------------------------------------------------------------------
//        /* consecutive(+Sequence, -Begin, +SubSequence, -End)			
//        /*	Succeeds if Sequence is of the `form' [Begin|SubSequence|End].	
//        /*----------------------------------------------------------------------
//        /* NOTES: 1. It is known that possible duplicates in Sequence will	
//        /*	     appear as duplicates in SubSequence.			
//        /*	  2. Only the heads of the clauses are tested for equality;	
//        /*	     this is essential for this predicate to be also used in	
//        /*	     predicate subst_rep/4.					
//        /*	  3. A perfect example where specialisation with representation	
//        /*	     helps!							
//        /*	  4. Predicate consecutive/3 is very similar to append/3 but	
//        /*	     does not perform unification; only head equality check.	
//        /*----------------------------------------------------------------------
//
//        consecutive([H1|T1], Front, [H2|T2], Back) :-
//        H1 = clause(HeadArgs1,_,_), H2 = clause(HeadArgs2,_,_),
//        ( HeadArgs1 == HeadArgs2 -> Front = [], consecutive(T2, Back, T1)
//        ; Front = [H1|NewFront], consecutive(T1, NewFront, [H2|T2], Back)
//        ).
//
//        :- mode consecutive(+,-,+).
//
//        consecutive([], L, L).
//        consecutive([H1|L1], L2, [H3|L3]) :-
//        H1 = clause(HeadArgs1,_,_), H3 = clause(HeadArgs3,_,_),
//        HeadArgs1 == HeadArgs3, consecutive(L1, L2, L3).
//
//        /*----------------------------------------------------------------------
//        /* check_for_cut(+SelectedClauses, +AfterSelectedClauses)		
//        /*	Succeeds iff the Selected Clauses are the last clauses of the	
//        /*	predicate, or iff they do not contain a hard cut.		
//        /*----------------------------------------------------------------------
//
//        check_for_cut(SelectedClauses, Back) :-
//        ( Back == [] -> true ; have_hardcut(SelectedClauses, 0) ).
//
//        /*----------------------------------------------------------------------
//        /* non_overlapping_selection(+Calls)					
//        /*	Succeeds iff the sets of selected clauses of the Calls are	
//        /*	non-overlapping.						
//        /*----------------------------------------------------------------------
//
//        :- mode non_overlapping_selection(+).
//
//        non_overlapping_selection([]).
//        non_overlapping_selection([BC|BCs]) :-
//        BC = body_calls(_,SelClauses,_,_,_),
//        'non overlapping selection'(BCs, SelClauses),
//        non_overlapping_selection(BCs).
//
//        :- mode 'non overlapping selection'(+,+).
//
//        'non overlapping selection'([], _).
//        'non overlapping selection'([BC|BCs], SelClauses) :-
//        BC = body_calls(_,OtherSelClauses,_,_,_),
//        disjoint(SelClauses, OtherSelClauses),
//        'non overlapping selection'(BCs, SelClauses).
//
//        :- mode disjoint(+,+).
//
//        disjoint([], _).
//        disjoint([Cl|Cls], SCL) :- fail_if(memberchk(Cl, SCL)), disjoint(Cls, SCL).
//
//        end_of_file.
//
//        /*======================================================================
//        /*  Debugging Predicates.						
//        /*======================================================================
//
//        :- import telling/1, tell/1, told/0, put/1, tab/1, ttywrite/1, write/1, nl/0
//        from standard.
//
//        pp([]).
//        pp([pi_calls(Sym,Calls)|T]) :-
//        write('# pi_calls('),
//        sym_name(Sym, P, A), write(P), put(0'/), write(A), put(0',), nl,
//        pp_calls(Calls), tab(10), put(0')), nl, pp(T).
//        pp([pi_calls(Sym,MergedCalls,Representable)|T]) :-
//        write('# pi_calls('),
//        sym_name(Sym, P, A), write(P), put(0'/), write(A), put(0',), nl,
//        pp_calls(MergedCalls), put(0',), nl,
//        ( Representable =:= 0 -> write('Non-') ; true ),
//        write('Representable') tab(10), put(0')), nl, pp(T).
//
//        pp_calls([]).
//        pp_calls([body_call(Args)|Calls]) :-
//        tab(4), write(Args), nl, pp_calls(Calls).
//        pp_calls([body_call(Args,SelectedClauses)|Calls]) :-
//        tab(4), write(Args), nl,
//        tab(4), write(SelectedClauses), nl,
//        pp_calls(Calls).
//        pp_calls([body_calls(CallMsg,SelectedClauses,Cs,_NCs,_Rep)|Calls]) :-
//        tab(4), write(CallMsg), nl,
//        tab(4), write(SelectedClauses), nl,
//        tab(4), write(Cs), nl,
//        pp_calls(Calls).
//
//        typeout(M) :- telling(X), tell(userout), ttywrite(M), nl, told, tell(X).
//
//
//----------------------- end of file spec.P */