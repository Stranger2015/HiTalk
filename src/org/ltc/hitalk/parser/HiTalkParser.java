package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import static com.thesett.aima.logic.fol.OpSymbol.Associativity.*;
import static org.ltc.hitalk.core.HtConstants.*;

/**
 *
 */
public
class HiTalkParser extends HiLogParser {
    /**
     * Parses multiple sequential terms, and if more than one is encountered then the flat list of terms encountered
     * must contain operators in order to be valid Prolog syntax. In that case the flat list of terms is passed to the
     * {@link (Term[])} method for 'deferred decision parsing' of dynamic operators.
     *
     * @return A single first order logic term.
     * @throws SourceCodeException If the sequence of tokens does not form a valid syntactical construction as a first
     *                             order logic term.
     */

    public
    HiTalkParser ( VariableAndFunctorInterner interner ) {
        super(null, interner);
    }

    /**
     * Builds a
     * public
     * prolog parser on a token source to be parsed.
     *
     * @param source
     * @param interner The interner for variable and functor names.
     */
    public
    HiTalkParser ( HtTokenSource source, VariableAndFunctorInterner interner ) {
        super(source, interner);
    }

    //
//    /**
//     * Parses a single sentence in first order logic. A sentence consists of a term followed by a full stop.
//     *
//     * @return A sentence in first order logic.
//     * @throws SourceCodeException If the token sequence does not parse into a valid sentence.
//     */
//    @Override
//    public
//    HtClause clause () throws SourceCodeException {
//        variableContext.clear();
//        Term term = term();
//        if (term.isFunctor()) {
//            Functor functor = (Functor) term;
//            String name = interner.getFunctorName(functor);
//        }
//
//        return (HtClause) term;
//    }

    /**
     * Converts a term into a clause. The term must be a functor. If it is a functor corresponding to the ':-' symbol it
     * is a clause with a head and a body. If it is a functor corresponding to the '?-' symbol it is a query clause with
     * no head but must have a body. If it is neither but is a functor it is interpreted as a program clause ':-' with
     * no body, that is, a fact.
     *
     * @param term     The term to convert to a top-level clause.
     * @return A clause for the term, or <tt>null</tt> if it cannot be converted.
     * @throws SourceCodeException If the term to convert to a clause does not form a valid clause.
     */
    public//fixme
    HtClause convert ( Term term ) throws SourceCodeException {


        /**
     * Interns and inserts into the operator table all of the built in operators and functors in Prolog.
     */
    @Override
    protected
    void initializeBuiltIns () {
        super.initializeBuiltIns();
//Logtalk operators
        internOperator(COLON_COLON, 600, XFY);
        internOperator(COLON_COLON, 600, FY);// message sending to "self"
        internOperator(UP_UP, 600, FY);// "super" call (calls an inherited or imported method definition)
        // mode operator
        internOperator(PLUS, 200, FY);// input argument (instantiated); ISO Prolog standard operator
        internOperator(AT, 200, FY);// input argument (not modified by the call)
        internOperator(QUESTION, 200, FY);// input/output argument
        internOperator(MINUS, 200, FY);// output argument (not instantiated); ISO Prolog standard operator
        internOperator(PLUS_PLUS, 200, FY);// ground argument
        internOperator(MINUS_MINUS, 200, FY);// unbound argument (typically when returning an opaque term)
        internOperator(L_SHIFT, 400, YFX);// bitwise left-shift operator (used for context-switching calls)
        // some backend Prolog compilers don't declare this ISO Prolog standard operator!
        internOperator(R_SHIFT, 400, YFX);// bitwise right-shift operator (used for lambda expressions)
        // some backend Prolog compilers don't declare this ISO Prolog standard operator!
        internOperator(AS, 700, XFX);// predicate alias operator (alternative to the ::/2 or :/2 operators depending on the context)
// first introduced in SWI-Prolog and YAP also for defining aliases to module predicates

// HiTalk operator
        internOperator(PUBLIC, 1150, FX);
        internOperator(PROTECTED, 1150, FX);
        internOperator(PRIVATE, 1150, FX);
        internOperator(ENUMERATION, 1150, FX);
    }
}