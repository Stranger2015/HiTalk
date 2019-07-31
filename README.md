_HiTalk_
========
The project is still under development

## _Objectives._
Standalone compiler + IDE.
  
## Ideas.
1. Enum;

2. HiLog extensions;
 
## _Examples._
```
:- object( type,
            extends( enum )).

    :- enumeration([
        hitalk_type, % :- true (by default)
        prolog_type, % :- true (by default)
        user_type    % :- true (by default)
    ]).

:- end_object.
src
%=======================================================
gen_src
% Generated because has the enum type
:- object( hitalk_type )).
    
    :- builtin.

    :- enumeration([
        enhkiry , % :- true (by default)
        prolog_type, % :- true (by default)
        user_type    % :- true (by default)
    ]).
    

:- end_object.
    
 object( prolog_type )).
    :- builtin.

    :- enumeration([
        term, % :- true (by default)
    ]).
   
:- end_object.

object( term )).
    :- builtin.

    :- enumeration([
        var, % :- true (by default)
        nonvar :- \+ var    
    ]).

:- end_object.