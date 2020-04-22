package org.ltc.hitalk.parser;

import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.term.io.HiTalkInputStream;
import org.ltc.hitalk.wam.compiler.Language;

import static org.ltc.hitalk.parser.PrologAtoms.*;
import static org.ltc.hitalk.term.OpSymbolFunctor.Associativity.*;
import static org.ltc.hitalk.wam.compiler.Language.HITALK;

/**
 *
 */
public class HiTalkParser extends HiLogParser {

    public HiTalkParser() {

    }

    /**
     * @return
     */
    @Override
    public Language language() {
        return HITALK;
    }

    /**
     * public
     * prolog parser on a token source to be parsed.
     *
     * @param interner The interner for variable and functor names.
     */
    public HiTalkParser(HiTalkInputStream stream,
                        IVafInterner interner,
                        ITermFactory factory,
                        IOperatorTable optable) throws Exception {
        super(stream, interner, factory, optable);
    }

    /**
     * Interns and inserts into the operator table all of the built in operators and functors in Prolog.
     */
    public void initializeBuiltIns () {
        super.initializeBuiltIns();
//Logtalk operators
        internOperator(COLON_COLON, 600, xfy);
        internOperator(COLON_COLON, 600, fy);// message sending to "self"
        internOperator(UP_UP, 600, fy);// "super" call (calls an inherited or imported method definition)
        // mode operator
        internOperator(PLUS, 200, fy);// input argument (instantiated); ISO Prolog standard operator
        internOperator(AT, 200, fy);// input argument (not modified by the call)
        internOperator(QUESTION, 200, fy);// input/output argument
        internOperator(MINUS, 200, fy);// output argument (not instantiated); ISO Prolog standard operator
        internOperator(PLUS_PLUS, 200, fy);// ground argument
        internOperator(MINUS_MINUS, 200, fy);// unbound argument (typically when returning an opaque term)
        internOperator(LSHIFT, 400, yfx);// bitwise left-shift operator (used for context-switching calls)
        // some backend Prolog compilers don't declare this ISO Prolog standard operator!
        internOperator(RSHIFT, 400, yfx);// bitwise right-shift operator (used for lambda expressions)
        // some backend Prolog compilers don't declare this ISO Prolog standard operator!
        internOperator(AS, 700, xfx);// predicate alias operator (alternative to the ::/2 or :/2 operators depending on the context)
// first introduced in SWI-Prolog and YAP also for defining aliases to module predicates

// HiTalk operator
        internOperator(PUBLIC, 1150, fx);
        internOperator(PROTECTED, 1150, fx);
        internOperator(PRIVATE, 1150, fx);
        internOperator(ENUMERATION, 1150, fx);
    }
}