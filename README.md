_HiTalk_
========
The project is still under development

## _Objectives._
Standalone compiler + IDE.
  
## _Ideas._
1. Enum pseudo entity;

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
%srcuu
%=======================================================
%gen_src
% Generated because has the enum type
:- object( hitalk_type )).
    
    :- built_in.

    :- enumeration([
        object , % :- true (by default)
        category, % :- true (by default)
        protocol    % :- true (by default)
    ]).
    

:- end_object.
    
:- object( prolog_type )).
    :- built_in.

    :- enumeration([
        term, % :- true (by default)
    ]).
   
:- end_object.

:- object( term )).
    :- built_in.

    :- enumeration([
        var, % :- true (by default)
        nonvar :- \+ var    
    ]).

:- end_object.

:- object( nonvar )).
    :- built_in.

    :- enumeration([
        atomic, % /*scalar*/ :- true (by default)
        list,
        compound    
    ]).

:- end_object.
````

## *Misc*

#Issues 
List syntax changes:
````
<foo>()                     arguments' nil
<foo>( Arg_1, ..., Arg_n )
<foo>( Arg_1 | RestArgs )
<foo>(| Args_Itself )
[| List_Itself]

[Head |] is just [ Head | _ ]//???????
````
Predicate indicator - **P/N**

BOF

Encoding



````
