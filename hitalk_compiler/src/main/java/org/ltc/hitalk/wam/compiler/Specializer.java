package org.ltc.hitalk.wam.compiler;

import com.thesett.common.util.doublemaps.SymbolTable;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.core.utils.TermUtilities;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.entities.HtPredicateDefinition;
import org.ltc.hitalk.entities.HtPredicateIndicator;
import org.ltc.hitalk.entities.context.ExecutionContext;
import org.ltc.hitalk.entities.context.IMetrics;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.term.io.Environment;
import org.ltc.hitalk.wam.compiler.PiCalls.XPiCalls;
import org.ltc.hitalk.wam.transformers.ISpecializer;
import org.ltc.hitalk.wam.transformers.TransformInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static org.ltc.hitalk.core.algebra.Msg.msg;
import static org.ltc.hitalk.wam.compiler.BodyCall.BodyCalls;

/**
 *
 */
public
class Specializer<T extends ITerm, C extends BodyCalls <C>> implements ISpecializer <T> {

    protected final Logger logger = LoggerFactory.getLogger(getClass().getSimpleName());

    private final SymbolTable <Integer, String, Object> symbolTable;
    private final IVafInterner interner;
    private final List <HtPredicate> predicates;
    private final IResolver <HtPredicate, HtClause> resolver = Environment.instance().getResolver();
    private final List <PiCalls <?>> piCalls = new ArrayList <>();

    /**
     *
     */
    public Specializer ( SymbolTable <Integer, String, Object> symbolTable,
                         IVafInterner interner,
                         List <HtPredicate> predicates ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
        this.predicates = predicates;
    }

    /**
     * @param term
     * @return
     */
    @Override
    public List <T> specialize ( T term ) {
        List <T> spClauses = new ArrayList <>();
        List <PiCalls <C>> calls = new ArrayList <>();
        predicates.stream().map(this::collect).forEachOrdered(calls::addAll);
        final List <XPiCalls <C>> merged = getInterestingCalls(calls, predicates);

        return spClauses;
    }

    /**
     * @param calls
     * @param predicates
     * @return
     */
    private List <XPiCalls <C>> getInterestingCalls ( List <PiCalls <C>> calls, List <HtPredicate> predicates ) {
        PredicateTable <?> table = new PredicateTable();
        List <XPiCalls <C>> result = new ArrayList <>();
        for (PiCalls <C> piCalls : calls) {
            if (table.containsKey(piCalls.getName())) {
                final List <HtClause> clauses = table.get(piCalls.getName()).getBody();
//todo
            }
        }

        return result;
    }

    private List <XPiCalls <C>> getInterestingIndeedCalls ( List <PiCalls <C>> calls, List <HtPredicate> predicates ) {
        PredicateTable <?> table = new PredicateTable <>(predicates);
        List <XPiCalls <C>> result = new ArrayList <>();
        for (PiCalls <C> piCalls : calls) {
            if (table.containsKey(piCalls.getName())) {
                final List <HtClause> clauses = table.get(piCalls.getName()).getBody();
//todo
            }
        }

        return result;
    }

    protected List <HtClause> findSelectedClauses ( ListTerm args, List <HtClause> clauses ) {
        return clauses.stream().filter(clause -> unifiesMnl(args, clause.getHead().getArgsAsListTerm()))
                .collect(Collectors.toList());
    }

    private boolean unifiesMnl ( ListTerm args, ListTerm headsArgs ) {
        return TermUtilities.unify(args, headsArgs);
    }
//    proper_subset([], [_|_]).
//    proper_subset([_|SubTail], [_|SetTail]) :- proper_subset(SubTail, SetTail).

    //    find_msg(+Args, +SelectedClauses, -Msg)				*/
//    /*	Given the arguments of a body call and a list of clauses with	*/
//    /*	which the call is known to unify (modulo nonlinearity), this	*/
//    /*	predicate incrementally computes the arguments of the most	*/
//    /*	specific generalisation of these terms.  Variables in the Msg	*/
//    /*	are not numbered.						*/
//    /*----------------------------------------------------------------------*/

    //visit each pddef
    public List <XPiCalls <?>> collect ( HtPredicate predicate ) {
        PiCallsCollector pc = PiCallsCollector.toPiCallsCollector();
        final List <XPiCalls <?>> calls = pc.supplier().get();

        return piCalls;
    }

    public List <XPiCalls <C>> mergeCalls ( List <BodyCall <C>> calls ) {
        final List <XPiCalls <C>> mergedCalls = new ArrayList <>();
        for (final PiCalls <C> piCalls : calls) {

        }

        return mergedCalls;
    }

    /**
     * @param bodyCalls
     * @param clauses
     * @return
     */
    public static @NotNull ListTerm findMsg ( ListTerm bodyCalls, List <HtClause <? extends IFunctor>> clauses ) {
        final Map <HtVariable, Pair <ITerm, ITerm>> dict = new HashMap <>();
        for (final HtClause <? extends IFunctor> clause : clauses) {
            bodyCalls = (ListTerm) msg(bodyCalls, clause.getHead().getArgsAsListTerm(), dict);
        }

        return bodyCalls;
    }

//*======================================================================*/
//    /* merge_calls(+CallList, -/*	MergedList is a simplified version of CallList, where all calls	*/
//    /*	that select the same clauses are merged together.  This is done	*/
//    /*	since all these calls can use the same specialisation of the	*/
//    /*	predicate and the same representative.				*/
//    /*======================================================================*/
//
//  %:- mode 'merge calls'(+,+,-).
//
//'merge calls'(body_call(Args,SelClauses), Calls, [body_calls(CallMsg,SelClauses,[Args|SameSelect],_,_)|Merged]) :-
//	'find all delete'(Calls, SelClauses, SameSelect, Rest),
//	'find msg of calls'(Args, SameSelect, CallMsg),
//	( Rest == [] -> Merged = []
//	; Rest = [One|Rest1], 'merge calls'(One, Rest1, Merged)
//	).

    /**
     * @param calls
     * @return
     */
    protected List <BodyCalls <C>> mergeCalls0 ( List <BodyCall <C>> calls ) {
        final List <BodyCalls <C>> merged = new ArrayList <>();
        for (int i = 0; i < calls.size(); i++) {
            BodyCall <C> call = calls.get(i);
            List <ListTerm> sameSelect = new ArrayList <>();
            final List <BodyCall <C>> rest = findAllDelete(calls, call.selectedClauses, sameSelect);
            if (!rest.isEmpty()) {
                call = rest.get(0);
            }
            final ITerm msg = findMsgOfCalls(call.args, sameSelect);
            sameSelect.add((ListTerm) call.get(i));
            merged.add(new BodyCalls <>(msg, call.selectedClauses, sameSelect));
        }

        return merged;
    }

    /**
     * 'find all delete'([], _, [], []).
     * 'find all delete'([Call|Calls], SelectedClauses, SS, Other) :-
     * Call = body_call(Args,SC),
     * ( SelectedClauses == SC -> SS = [Args|SS1], Other = Other1
     * ;  SS = SS1, Other = [Call|Other1]
     * ),
     * 'find all delete'(Calls, SelectedClauses, SS1, Other1).
     *
     * @param calls
     * @param selectedClauses
     * @param ss
     * @return
     */
    private List <BodyCall <C>> findAllDelete ( List <BodyCall <C>> calls, List <HtClause> selectedClauses, List <ListTerm> ss ) {
        List <BodyCall <C>> other = new ArrayList <>();
        calls.forEach(call -> {
            final List <HtClause> sc = (List <HtClause>) call.get(1);
            if (selectedClauses == sc) {
                ss.add((ListTerm) call.get(0));
            } else {
                other.add(call);
            }
        });

        return other;
    }

    private ListTerm findMsgOfCalls ( ListTerm args, List <ListTerm> calls ) {
        final Map <HtVariable, Pair <ITerm, ITerm>> dict = new HashMap <>();
        for (final ListTerm call : calls) {
            args = (ListTerm) msg(args, call, dict);
        }

        return ListTerm.NIL;
    }

    /*----------------------------------------------------------------------*/
    /* 'give a second chance'(+CallArgs, +SelectedClauses)			*/
    /*	Gives a second chance to a partially instantiated call, (that	*/
    /*	does not select a subset of the predicate's clauses), by	*/
    /*	examining whether the call will benefit by the factoring of	*/
    /*	common subexpressions property of the specialisation algorithm.	*/
    /*	This is done by finding the most specific generalisation of	*/
    /*	the call and the heads of the selected clauses, and checking	*/
    /*	whether it contains any partially instantiated arguments.	*/
    /*----------------------------------------------------------------------*/

    public boolean isGivenTheSecondChance ( ListTerm callArgs, List <HtClause <? extends IFunctor>> selectedClauses ) {
        final @NotNull ListTerm msg = findMsg(callArgs, selectedClauses);
        return sthBound(msg);
    }

    /**
     * @param msg
     * @return
     */
    private boolean sthBound ( ListTerm msg ) {
        final ITerm[] heads = msg.getHeads();
        for (final ITerm head : heads) {
            if (!head.isVar()) {//chk value??
                return true;
            }
        }
        return false;
    }

    /**
     * interesting_calls([], _, []).
     * <p>
     * interesting_calls([pi_calls(Sym,C)|Cs], Preds, ICL) :-
     * Pred = pred(Sym,_Clauses,_),
     * (memberchk(Pred, Preds)
     * ->	'interesting calls'(C, Pred, IC),
     * ( IC == [] -> ICL = ICs ; ICL = [pi_calls(Sym,IC)|ICs] ),
     * interesting_calls(Cs, Preds, ICs)
     * ;	interesting_calls(Cs, Preds, ICL)
     * ).
     *
     * @param piCalls
     */
    private void interestingCalls ( final List <PiCalls <C>> piCalls,
                                    final PredicateTable <?> predicates ) {
        piCalls.forEach(piCall -> {

//            piCall.accept(pcc); // may not be there if dynamic
//            final int sym = piCall.getName();
//            final ITerm[] calls = piCall.getArguments();
//
            final HtPredicateDefinition def = predicates.lookup(piCall);
            if (!def.isBuiltIn()) {
                List <PiCalls> icalls = interestingCalls0(calls, predicates);
            }
        });
    }

    /**
     * @param calls
     * @param predicates
     * @return
     */
    private List <PiCalls <C>> interestingCalls0 ( ITerm[] calls, PredicateTable predicates ) {
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

    private List <HtClause <IFunctor>> specializePred ( PiCalls <?> piCall ) {
        List <HtClause <IFunctor>> spClauses = new ArrayList <>();
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

    public TransformInfo <? extends ITerm> getBestSoFarResult () {
        return null;
    }

    @Override
    public List <ITerm> transform ( ITerm t ) {
        return Collections.singletonList(t);
    }

    @Override
    public void cancel () {

    }

    @Override
    public void run () {

    }
}
