package org.ltc.hitalk.parser;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.TermFactory;
import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.Language;

import java.util.HashSet;
import java.util.Set;

import static org.ltc.hitalk.core.BaseApp.getAppContext;
import static org.ltc.hitalk.parser.PlToken.TokenKind.TK_DOT;
import static org.ltc.hitalk.term.OpSymbolFunctor.Associativity.fx;
import static org.ltc.hitalk.wam.compiler.Language.HILOG;

/**
 *
 */
public class HiLogParser extends HtPrologParser {

    public static final String HILOG_APPLY_STRING = "$hilog_apply";

    public static final int HILOG_APPLY_INT = getAppContext().getInterner().internFunctorName(HILOG_APPLY_STRING, 1);

    public static final IFunctor HILOG_APPLY =
            getAppContext().getTermFactory().newHiLogFunctor(HILOG_APPLY_STRING, new ListTerm(1));

    /**
     * Builds a public prolog parser on a token source to be parsed.
     *
     * @param interner  the  interner for variable and functor names.
     */
    protected final Set<IFunctor> hilogFunctors = new HashSet<>();

    /**
     * @param stream
     * @param interner
     * @param termFactory
     * @param optable
     */
    public HiLogParser(HiTalkInputStream stream,
                       TermFactory termFactory,
                       IVafInterner interner,
                       IOperatorTable optable) throws Exception {
        super(stream, interner, termFactory, optable);
    }

    /**
     *
     */
//    public HiLogParser() throws Exception {
//        this(
//                getAppContext().getInputStream(),
//                getAppContext().getInterner(HILOG.getNameSpace("Variables", "Functors")),
//                getAppContext().getTermFactory(),
//                getAppContext().getOpTable()
//        );
//    }

    /**
     * @param inputStream
     * @param interner
     * @param factory
     * @param optable
     */
    public HiLogParser(HiTalkInputStream inputStream,
                       IVafInterner interner,
                       ITermFactory factory,
                       IOperatorTable optable) throws Exception {
        super(inputStream, interner, factory, optable);
    }

    /**
     * @return
     */
    public ITerm parse() throws Exception {
        return super.parse();
    }

    /**
     * @return
     */
    public ITerm next() throws Exception {
        return super.expr(TK_DOT);
    }

    /**
     * Parses a single terms, or atom (a name with arity zero), as a sentence in first order logic. The sentence will
     * be parsed in a fresh variable context, to ensure its variables are scoped to within the term only. The sentence
     * does not have to be terminated by a full stop. This method is not generally used by Prolog, but is provided as a
     * convenience to languages over terms, rather than clauses.
     *
     * @return A term parsed in a fresh variable context.
     */
    public ITerm termSentence() throws Exception {
        return super.termSentence();
    }

//    @Override
//    protected IFunctor compound(String name, ListTerm args) throws Exception {
//        // hilog p/_ q/_, pi_N/N, pi_1/1, piA1_A2/1-2.
//        final IFunctor result = hilogFunctors.contains(termFactory.newFunctor(name, args.size())) ?
//                termFactory.newHilogFunctor(name, args) :
//                super.compound(name, args);// :- hilog p, q, pi/N =>
//
//        return result;
//    }

    @Override
    public Language language() {
        return HILOG;
    }

    /**
     * Interns and inserts into  the  operator table all  of   the  built in operators and functors in Prolog.
     */
    public void initializeBuiltIns() {
        super.initializeBuiltIns();

        internOperator(PrologAtoms.HILOG, 1150, fx);
    }

//            final ITerm name = lastTerm;//visit(h);//endfTerm todo
//            lastTerm = termFactory.newHiLogFunctor(name, args);
//        }
//        return lastTerm;
}
//==========================

//        Algorithm specialize(Program)
//
//        1.Collecting Call (partially instantiated) calls to predicates that  are defined in Program;

//        2.For each ci2C nd and associate with cii. the  set Sel(ci) of Program clauses that are immediately selected
//        by ci, and ii. the most specific generalisation gi of  the  set Heads(Sel(ci))[fcig;

//        3.Remove from Call calls that do not benefit 2C^Sel(c)=Si^msg(Heads(Si)[fcg) is a variant of gig;
//        5.For each equivalence class  of calls CSido/*LetSel(CSi)be the  set  of immediately selected clauses  of  the
//        calls in CSi,pibe the ir predicate symbol, and p0i be an new predicate symbol.*/i.
//        If(Sel(CSi)6=;) the n.
//        Choose ap0i-representative Ri=(HiBi) of Sel(CSi) for the callsCSi;.Foreach clause
//        Clij=(HeadijBodyij)2Sel(CSi)do Insert in Program the clause Cl0ij=(Head0ijBodyij)where Head 0ijis
//        the p0i-difference of Headijfrom the head Hi of  the representative Ri;ii.For each call cij of
//        the  equivalence class CSi, ndit sp0-specialisation c0ij(and associate it with cij);

//        6.For each equivalence class  of  calls CSido Replace through out Program all occurrences  of  call cij2CS i
//        by it sp0-specialisationc 0ij;///Figure2:  the   call specialisation algorithm.
//        and some argument register (put*) WAMinstructions  of the original HiLog program become unnecessary.
//
//         the se instructions are eliminated from both    the   specialised calls as well as from the
//         specialised versions  of    the   predicates.
//        Algorithm Specialise begins by collecting all ( partially instantiated ) calls to predicates that  are defined
//        in  program P.
//        We allow for open HiLog programs,that  is HiLog  programs for which the definitions of some predicates are
//        missing or are imported from the modules.
//        the second step of  the algorithm nds and associates wi the ach call ci, the  set  of program clauses Sel(ci)
//        whose heads unify with ci.Each of  the se sets contains the program clauses that have the potential of being
//        selected for the execution of callci during run-time.As mentioned in the previous section,in the absence of any
//        information about the context conditions of ci,or the success conditions of each clause in Sel(ci), the Sel(ci)
//        set is the best safe approximation of  clauses that might be chosen for the execution of ci at runtime.
//        Not all collected calls, however,can benefit from specialisation ;calls that do not