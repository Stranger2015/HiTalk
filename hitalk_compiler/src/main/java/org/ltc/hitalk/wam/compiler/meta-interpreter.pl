
%solve(_,true):- !. %C1

solve(Ctx,(G1,G2)):-
    !, solve(Ctx,G1),
       solve(Ctx,G2). %C2

solve(Ctx,U>>G):- !,
    solve([U|Ctx],G). %C3

solve(Ctx,G):-
    member(U,Ctx), %C4
    method(U,G,Body),
    solve(Ctx,Body).

solve(_, G):-
    predicate_property(G, built_in),
    call(G).

method(U, Head, Body) :-
%    current_entity(U),
    clause( U::Head, Body ).