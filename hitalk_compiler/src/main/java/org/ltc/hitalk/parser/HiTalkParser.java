package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.OpSymbol;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.PlOperatorTable;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.term.HlOperator;
import org.ltc.hitalk.term.io.HiTalkStream;

import static org.ltc.hitalk.parser.jp.segfault.prolog.parser.Operator.Associativity.*;
import static org.ltc.hitalk.term.HlOperator.Associativity.fy;
import static org.ltc.hitalk.term.HlOperator.Associativity.xfy;

/**
 *
 */
public class HiTalkParser implements IParser <Term> {
    private IParser <Term> parser;

    public void setParser ( IParser <Term> parser ) {
        setparser = parser;
    }

    @Override
    public HiTalkStream getStream () {
        return parser.getStream();
    }

    @Override
    public VariableAndFunctorInterner getInterner () {
        return parser.getInterner();
    }

    @Override
    public ITermFactory getFactory () {
        return parser.getFactory();
    }

    @Override
    public PlOperatorTable getOptable () {
        return null;
    }

    @Override
    public void setOperator ( HlOperator op ) {

    }

    /**
     * @return
     */
    public String language () {
        return "HiTalk";
    }

    /**
     *  public
     * prolog parser on a token source to be parsed.
     *
     * @param interner    The interner for variable and functor names.
     */
    public HiTalkParser ( HiTalkStream stream,
                          VariableAndFunctorInterner interner,
                          ITermFactory factory,
                          PlOperatorTable optable,
                          PlPrologParser parser ) {
        setStream(stream);
        setInterner(interner);
        setTermFactory(factory);
        setOptable(optable);
        setParser(parser);
    }

    public void setOptable ( PlOperatorTable optable ) {

    }

    public void setInterner ( VariableAndFunctorInterner interner ) {


    }

    public void setTermFactory ( ITermFactory factory ) {

    }

    public void setStream ( HiTalkStream stream ) {
        parser.set
    }

//    /**
//     * Converts a term into a clause. The term must be a functor. If it is a functor corresponding to the ':-' symbol it
//     * is a clause with a head and a body. If it is a functor corresponding to the '?-' symbol it is a query clause with
//     * no head but must have a body. If it is neither but is a functor it is interpreted as a program clause ':-' with
//     * no body, that is, a fact.
//     *
//     * @param term The term to convert to a top-level clause.
//     * @return A clause for the term, or <tt>null</tt> if it cannot be converted.
//     * @throws SourceCodeException If the term to convert to a clause does not form a valid clause.
//     */
//    public//fixme
//    HtClause convert ( Functor term ) throws SourceCodeException {
//        return super.convert(term);
//    }

    /**
     * Interns and inserts into the operator table all of the built in operators and functors in Prolog.
     */
    protected void initializeBuiltIns () {
        parser.initializeBuiltIns();
//Logtalk operators
        internOperator(PrologAtoms.COLON_COLON, 600, xfy);
        internOperator(PrologAtoms.COLON_COLON, 600, fy);// message sending to "self"
        internOperator(PrologAtoms.UP_UP, 600, fy);// "super" call (calls an inherited or imported method definition)
        // mode operator
        internOperator(PrologAtoms.PLUS, 200, fy);// input argument (instantiated); ISO Prolog standard operator
        internOperator(PrologAtoms.AT, 200, fy);// input argument (not modified by the call)
        internOperator(PrologAtoms.QUESTION, 200, fy);// input/output argument
        internOperator(PrologAtoms.MINUS, 200, fy);// output argument (not instantiated); ISO Prolog standard operator
        internOperator(PrologAtoms.PLUS_PLUS, 200, fy);// ground argument
        internOperator(PrologAtoms.MINUS_MINUS, 200, fy);// unbound argument (typically when returning an opaque term)
        internOperator(PrologAtoms.LSHIFT, 400, yfx);// bitwise left-shift operator (used for context-switching calls)
        // some backend Prolog compilers don't declare this ISO Prolog standard operator!
        internOperator(PrologAtoms.RSHIFT, 400, yfx);// bitwise right-shift operator (used for lambda expressions)
        // some backend Prolog compilers don't declare this ISO Prolog standard operator!
        internOperator(PrologAtoms.AS, 700, xfx);// predicate alias operator (alternative to the ::/2 or :/2 operators depending on the context)
// first introduced in SWI-Prolog and YAP also for defining aliases to module predicates

// HiTalk operator
        internOperator(PrologAtoms.PUBLIC, 1150, fx);
        internOperator(PrologAtoms.PROTECTED, 1150, fx);
        internOperator(PrologAtoms.PRIVATE, 1150, fx);
        internOperator(PrologAtoms.ENUMERATION, 1150, fx);
    }

    @Override
    public Sentence <Term> parse () throws SourceCodeException {
        return null;
    }

    @Override
    public void setTokenSource ( PlTokenSource source ) {

    }

    @Override
    public PlTokenSource getTokenSource () {
        return null;
    }

    @Override
    public void setOperator ( String operatorName, int priority, OpSymbol.Associativity associativity ) {

    }
}