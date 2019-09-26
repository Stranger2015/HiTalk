list0([]).
list0([b|c]).
list0([|X]).

bypass :-
    {true}.

args().
args(|Args).
args(arg|args).