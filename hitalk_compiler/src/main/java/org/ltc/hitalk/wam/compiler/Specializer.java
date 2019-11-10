package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.HtPredicateIndicator;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.transformers.ISpecializer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 *
 */
public
class Specializer implements ISpecializer <HtClause, Term> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final SymbolTable <Integer, String, Object> symbolTable;
    private final VariableAndFunctorInterner interner;
    private final PredicateTable predicateTable;
    private final List <PiCalls> piCalls;

    /**
     *
     */
    public Specializer ( SymbolTable <Integer, String, Object> symbolTable,
                         VariableAndFunctorInterner interner,
                         PredicateTable predicateTable,
                         List <PiCalls> piCalls ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
        this.predicateTable = predicateTable;
        this.piCalls = piCalls;
    }

    /**
     * @param clause
     * @return
     */
    @Override
    public List <HtClause> specialize ( HtClause clause ) {
        List <HtClause> spClauses = new ArrayList <>();
        List <PiCalls> piCalls = new ArrayList <>();

        return spClauses;
    }

    public List <PiCalls> collect ( HtPredicateDefinition def ) {
        final PiCallsCollector collector = PiCallsCollector.toPiCallsCollector();
        final Supplier <List <PiCalls>> supp = collector.supplier();
        final List <PiCalls> pc = supp.get();

        return piCalls;
    }

    public List <PiCalls> mergeCalls () {
        List <PiCalls> l = new ArrayList <>();
        return l;
    }

    /**
     * interesting_calls([], _, []).
     * <p>
     * interesting_calls([pi_calls(Sym,C)|Cs], Preds, ICL) :-
     * Pred = pred(Sym,_Clauses,_),
     * (memberchk(Pred, Preds)  % may not be there if dynamic
     * ->	'interesting calls'(C, Pred, IC),
     * ( IC == [] -> ICL = ICs ; ICL = [pi_calls(Sym,IC)|ICs] ),
     * interesting_calls(Cs, Preds, ICs)
     * ;	interesting_calls(Cs, Preds, ICL)
     * ).
     *
     * @param piCalls
     */
    private void interestingCalls ( final List <PiCalls> piCalls, final PredicateTable predicates ) {

        piCalls.forEach(piCall -> {
            final int sym = piCall.getName();
            final Term[] calls = piCall.getArguments();
            if (predicates.lookup(piCall)) {
                List <PiCalls> icalls = interestingCalls0(calls, predicates);
            }
        });
    }

    /**
     * @param calls
     * @param predicates
     * @return
     */
    private List <PiCalls> interestingCalls0 ( Term[] calls, PredicateTable predicates ) {
        return piCalls;
    }

// specialise_pred(Sym, Calls, Representable, SymTab, CLin, CLout) :-
//    sym_name(Sym, N, A),
//	( sym_type(Sym,tabled(_,_)) -> Tabled = 1 ; Tabled = 0 ),
//            ( ( \+sym_type(Sym,aggregation(_)),
//            ( \+ option(spec_repr), Representable = 0
//    ; non_overlapping_selection(Calls), Representable = 1
//            )
//            ) ->
//   message(('% Specialising partially instantiated calls to ',
//            N, '/', A)),
//            'specialise pred'(Calls, N, A, Tabled, SymTab, CLin, CLout)
//    ; CLout = CLin, Representable = 0,
//            (sym_type(Sym,aggregation(_))
//            ->	  message(('% specialization of aggregate predicate not performed: ',N,'/',A))
//    ;	  warning(('Calls select overlapping clause blocks of predicate ',
//                   N, '/', A)),
//    message(('           ',
//                    '(specialisation with representation cannot be performed!)'))
//            )
//            ).

    /*
'specialise pred'([], _, _, _, _, CL, CL).
'specialise pred'([BC|BCs], N, A, Tabled, SymTab, CLin, CLout) :-
	BC = body_calls(CallsMsg,SelClauses,Cs,NCs,Rep),
	specialise_calls(CallsMsg, SelClauses, Cs, N, A, Tabled,
			 SymTab, NCs, Rep, NewPred),
	( var(NewPred) -> CLin = CLmid ; CLin = [NewPred|CLmid] ),
	'specialise pred'(BCs, N, A, Tabled, SymTab, CLmid, CLout).
*/

    private List <HtClause> specializePred ( PiCalls piCall ) {
        List <HtClause> spClauses = new ArrayList <>();
        HtPredicateIndicator symbol = new HtPredicateIndicator(piCall);
//noinspection PlaceholderCountMatchesArgumentCount
        logger.info("Specializing partially instantiated calls to %s... ", symbol);

        return spClauses;
    }

    /*
    spec(module(SymTab,DL,CL,QR,Pars), module(SymTab,DL,CL1,QR,Pars)) :-
    initialise,
    collect_pi_calls(CL, DL, PI_Calls),
    interesting_calls(PI_Calls, CL, InterestingCalls),
    merge_calls(InterestingCalls, MergedCalls),
    specialise(MergedCalls, SymTab, CLmid, CL),
    new_clause_list(MergedCalls, CLmid, CL1),
    generate_table_decl(DL, MergedCalls).
*/

    /*
    collect_pi_calls(Preds, DL, PI_Calls) :-
	'collect pi calls'(Preds, Open_PI_Calls),
	'well, add some more'(DL, Open_PI_Calls),
	close_pi_calls(Open_PI_Calls, PI_Calls).

:- mode 'collect pi calls'(+,?).

'collect pi calls'([], _).
'collect pi calls'([pred(_,Clauses,_)|Preds], HB) :-
	'collect from pred'(Clauses, HB),
	'collect pi calls'(Preds, HB).

:- mode 'collect from pred'(+,?).

'collect from pred'([], _).
'collect from pred'([clause(_,Body,_)|CLs], HB) :-
	chb(Body, HB),
	'collect from pred'(CLs, HB).

:- mode chb(+,?).

chb(and(G1,G2), HB) :- chb(G1, HB), chb(G2, HB).
chb(or(G1,G2), HB) :- chb(G1, HB), chb(G2, HB).
chb(if(G1,G2), HB) :- chb(G1, HB), chb(G2, HB).
chb(not(G), HB) :- chb(G, HB).
chb(goal(Sym,Args), HB) :-
	( filter_goal(Sym, Args) ->	% partially instantiated (HiLog) call
		memberchk(pi_calls(Sym,Calls), HB),	% memberchk
		add_new_clause(body_call(Args), Calls)
	; true
	).
chb(inlinegoal(_,_,_), _).


   add_new_clause(New,[X|L]) :-
       var(X)
        -> X=New   % first element
        ;  add_new_clause1(New,L,0).

   add_new_clause1(New,[_|L],N) :-
       L = [Y|_], nonvar(Y), !,
       N1 is N+1, add_new_clause1(New,L,N1).

   add_new_clause1(New,[X|L1],N) :-    % X is last bound on list
       (add_tree(New,X,N)
        -> true
        ;  L1=[Y|_],
           N1 is N+1,
           add_tree(New,Y,N1)
       ).

   add_tree(New,[X|Y],N) :-
       (N =:= 0
        -> (var(X)
            -> X=New
            ;  var(Y), Y=New
           )
        ;  N1 is N-1,
           (var(Y)
            -> (add_tree(New,X,N1)
                -> true
                ;  add_tree(New,Y,N1)
               )
            ;  add_tree(New,Y,N1)
           )
       ).


:- mode filter_goal(+,+).

filter_goal(Sym, Args) :- sym_prop(defined, Sym), sth_bound(Args).

///*----------------------------------------------------------------------*/
//    /* 'well, add some more'(+DeclarationList, #Calls)			*/
//    /*	Adds some more calls to the list of p.i. calls that might	*/
//    /*	be specialised.  These calls are the user-supplied table	*/
//    /*	specialisation declarations.					*/
//    /*----------------------------------------------------------------------*/
//
//'well, add some more'([], _).
//            'well, add some more'([Goal|Goals], Calls) :-
//    chb(Goal, Calls),
//	'well, add some more'(Goals, Calls).
//
/*
    interesting_indeed(body_call(Args), pred(Sym,Clauses,_),body_call(Args,SelectedClauses)) :-
    find_selected_clauses(Args, Clauses, SelectedClauses),
	( SelectedClauses \== [] ->
            ( option(spec_repr) ->
    consecutive(Clauses, Front, SelectedClauses, Back),
    check_for_cut(SelectedClauses, Back),
			( ( Front=[_|_] -> true ; Back=[_|_] )	% proper subset
            ; 'give a second chance'(Args, SelectedClauses)
            )
    ; ( proper_subset(SelectedClauses, Clauses)
            ; 'give a second chance'(Args, SelectedClauses)
            )
            )
    ; sym_name(Sym, N, A),
    warning(('A partially instantiated call to ',N,'/',A,' will fail!')),
    fail
	).
*/
/*
/*======================================================================*/
    /* specialise(+InterestingMergedCalls, #SymTab, +CLIn,-CLOut)		*/
    /*	Performs the actual specialisation of all call equivalence	*/
    /*	classes that are worth specialising.				*/
    /*======================================================================*/

//    specialise([], _, CL, CL).
//    specialise([pi_calls(Sym,Calls,Representable)|PICs], SymTab, CLin, CLout) :-
//    specialise_pred(Sym, Calls, Representable, SymTab, CLin, CLmid),
//    specialise(PICs, SymTab, CLmid, CLout).

    /*----------------------------------------------------------------------*/
    /* specialise_pred(+Sym, +Calls, +Representable, #SymTab, +CLin,-CLout)	*/
    /*	Specialises calls to one predicate.  Specialisation without	*/
    /*	clause replacement is always allowed, but specialisation with	*/
    /*	clause replacement is only allowed when all equivalence classes	*/
    /*	of calls have non-overlapping clause selection.			*/
    /*----------------------------------------------------------------------*/

/*    /*
    find_selected_clauses(_, [], []).
    find_selected_clauses( CallArgs, [Clause|Clauses], SCL ) :-
    Clause = clause(HeadArgs,_,_),
	( unifies_mnl(CallArgs, HeadArgs) -> SCL = [Clause|SCs] ; SCL = SCs ),
    find_selected_clauses(CallArgs, Clauses, SCs).
*/

    /**
     *
     */
    @Override
    public void reset () {

    }

    @Override
    public ExecutionContext getContext () {
        return null;
    }

    @Override
    public void setContext ( ExecutionContext context ) {

    }

    @Override
    public boolean isAcceptable ( IMetrics max ) {
        return false;
    }

    @Override
    public TransformInfo getBestSoFarResult () {
        return null;
    }

    @Override
    public Term transform ( Term t ) {
        return null;
    }

    @Override
    public void cancel () {

    }

    @Override
    public void run () {

    }
}
