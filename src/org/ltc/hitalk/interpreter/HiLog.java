package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.HiLogAstCompiler;

/**
 * Algorithm Specialise (Program)
 * <p>
 * l. Collect in C all (partially instantiated) calls to predicates that are defined in Program;
 * 2. For each ci € C find and associate with ci
 * i.  the set Sel(ci) of Program clauses that are immediately selected by ci, and
 * ii. the most specific generalization gi of the set Heads(Sel(ci)) U {c'i};
 * 3. Remove from C all calls that do not benefit from specialization;
 * let C' be the result;
 * 4. Partition C' into Cs1, Cs2,,..., Csk
 * where Csi = {c|c € C /\ Scl(c) = S'i /\ msg(H cads(S^i ) U {c} ) is a variant of gi};
 * 5. For each equivalence class of calls Csi do /x Let S^cl(Csi) be the set of immediately selected clauses of
 * the calls in Csi, pi be their predicate symbol, and be a new predicate symbol. x/
 * i. If(Sel(Csi)0/) then
 * a. Choose a p'-representative Ri = (Hi :- Bi) of Sel(Csi) for the calls Csi;
 * b. For each clause Clij = (Headij :- Bodyij) € Sel(Csi)
 * do Insert in Program the clause Clij = (Head'ij:-Bodyij) where Head'ij is the p'i difference of Headij
 * from the head Hi of the representative Ri;
 * ii. For each call cij of the equivalence class Csi, find its p' specialization c'ij (and associate it with cij);
 * 6. For each equivalence class of calls Csi do Replace throughout Program all occurrences of call cij € Csij
 * by its p'-specialization c'ij;
 * <p>
 * Appendix A
 * Standard Predicates and Functions
 * A.1 List of Standard Predicates
 * abolish(Name/Arity)
 * abolish(Name, Arity)
 * abolish all tables
 * abolish table call(Term)
 * abolish table pred(Pred)
 * abort
 * analyze table(Pred)
 * arg(Index, Term, Arg)
 * arg0(Index, Term, Arg)
 * assert(Clause)
 * asserta(Clause)
 * assertz(Clause)
 * atom(Term)
 * atomic(Term)
 * atom chars(Atom, CharList)
 * bagof(Elem, Goal, Bag)
 * break
 * ’C’(List1, Token, List2)
 * call(Term)
 * callable(Term)
 * cd(Dir)
 * clause(Head, Body)
 * close(FileName)
 * compare(Res, Term1, Term2)
 * <p>
 * APPENDIX A. STANDARD PREDICATES AND FUNCTIONS
 * <p>
 * compound(Term)
 * compile(Module)
 * compile(Module, Options)
 * consult(Module)
 * consult(Module, Options)
 * copy term(Term, Copy)
 * cputime(Time)
 * current atom(Atom)
 * current functor(Functor)
 * current functor(Functor, Term)
 * current input(File)
 * current module(Module)
 * current module(Module, File)
 * current op(Precedence, Type, Name)
 * current output(File)
 * current predicate(Predicate)
 * current predicate(Predicate, Term)
 * debug
 * debugging
 * edit(File)
 * erase(Reference)
 * expand term(Term, Expanded Term)
 * fail
 * fail if(Goal)
 * file exists(File)
 * findall(Elem, Goal, List)
 * float(Term)
 * functor(Term, Functor, Arity)
 * get(Char)
 * get0(Char)
 * get calls for table(Pred, Call, Empty)
 * get returns for call(Call, Return)
 * halt
 * <p>
 * <p>
 * <p>
 * hilog(Symbol)
 * hilog_arg(Index, Term, Arg)
 * hilog_functor(Term, Functor, Arity)
 * hilog_op(Precedence, Type, Name)
 * hilog_symbol(Symbol)
 */
public
class HiLog<T,Q> extends TopLevel<T,Q> {

    /**
     * @param engine
     * @param interpreter
     * @param compiler
     */
    public
    HiLog ( HtResolutionEngine<T, Q> engine, HiLogInterpreter <HtClause, T,Q> interpreter, HiLogAstCompiler compiler ) {
        super(engine, interpreter, compiler);
    }


}


