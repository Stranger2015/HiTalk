options(read_term, [
'backquoted_string(Bool)',
'character_escapes(Bool)',
'comments(-Comments)',
'cycles(Bool)',
'dotlists(Bool)',
'double_quotes(Atom)',
'module(Module)',
'quasi_quotations(-List)',
'singletons(Vars)',
'syntax_errors(Atom)',
'subterm_positions(TermPos)'
]).

suboptions(read_term,'subterm_positions(TermPos)', [
'From-To',

]).
