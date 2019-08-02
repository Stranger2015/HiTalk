package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.compiler.bktables.BkLoadedEntities;
import org.ltc.hitalk.compiler.bktables.BkTableKind;
import org.ltc.hitalk.compiler.bktables.BookKeepingTables;
import org.ltc.hitalk.compiler.bktables.IRegistry;
import org.ltc.hitalk.entities.HtEntityIdentifier;

import java.util.List;

import static com.thesett.aima.logic.fol.OpSymbol.Associativity.*;
import static com.thesett.aima.logic.fol.TermUtils.flattenTerm;

/**
 *
 */
public
class HiTalkParser extends HiLogParser {

    /**
     * Builds a
     * public
     * prolog parser on a token source to be parsed.
     *
     * @param source
     * @param interner The interner for variable and functor names.
     */
    public
    HiTalkParser ( TokenSource source, VariableAndFunctorInterner interner ) {
        super(source, interner);
    }

    /**
     * Parses a single horn clause as a sentence in first order logic. A sentence consists of a clause followed by a
     * full stop.
     *
     * @return A horn clause sentence in first order logic.
     * @throws SourceCodeException If the token sequence does not parse into a valid sentence.
     */
    @Override
    public
    Clause sentence () throws SourceCodeException {
        return super.sentence();
    }

    /**
     * Parses a single sentence in first order logic. A sentence consists of a term followed by a full stop.
     *
     * @return A sentence in first order logic.
     * @throws SourceCodeException If the token sequence does not parse into a valid sentence.
     */
    @Override
    public
    HtClause clause () throws SourceCodeException {
        variableContext.clear();
        Term term = term();
        if (term.isFunctor()) {
            Functor functor = (Functor) term;
            FunctorName functorName = interner.getDeinternedFunctorName(functor.getName());
        }

        return (HtClause) term;
    }

    /**
     * Converts a term into a clause. The term must be a functor. If it is a functor corresponding to the ':-' symbol it
     * is a clause with a head and a body. If it is a functor corresponding to the '?-' symbol it is a query clause with
     * no head but must have a body. If it is neither but is a functor it is interpreted as a program clause ':-' with
     * no body, that is, a fact.
     *
     * @param term     The term to convert to a top-level clause.
     * @param interner The functor and variable name interner for the namespace the term to convert is in.
     * @return A clause for the term, or <tt>null</tt> if it cannot be converted.
     * @throws SourceCodeException If the term to convert to a clause does not form a valid clause.
     */
    public static
    HtClause convertToClause ( Term term, VariableAndFunctorInterner interner ) throws SourceCodeException {
        // Check if the top level term is a query, an implication or neither and reduce the term into a clause
        // accordingly.
        if (term instanceof OpSymbol) {
            OpSymbol symbol = (OpSymbol) term;
            IRegistry <BkLoadedEntities> registry = new BookKeepingTables <>();
            if (":-".equals(symbol.getTextName())) {
                List <Functor> flattenedArgs = flattenTerm(symbol.getArgument(1), Functor.class,
                        ",", interner);
                Functor head = (Functor) symbol.getArgument(0);
                FunctorName fname = interner.getDeinternedFunctorName(head.getName());
                Functor identifier = null;
                HtEntityIdentifier entity = null;
                Functor[] args = flattenedArgs.toArray(new Functor[flattenedArgs.size()]);
                if ("::".equals(fname.getName()) && fname.getArity() == 2) {
                    identifier = (Functor) head.getArgument(0);
                    head = (Functor) head.getArgument(1);
                    BkLoadedEntities record = registry.selectOne(BkTableKind.LOADED_ENTITIES,
                            new BkLoadedEntities(new HtEntityIdentifier(identifier, null)));
                    entity = record.getEntity1();
                    return new HtClause(entity, head, args);
                }
                else if (":".equals(fname.getName()) && fname.getArity() == 2) {
                    return null;//todo
                }
                else {
                    return null;
                }
            }
            else {
                return null;
            }
        }
        return null;
    }

    /**
     * Interns and inserts into the operator table all of the built in operators and functors in Prolog.
     */
    @Override
    protected
    void initializeBuiltIns () {
        super.initializeBuiltIns();
//Logtalk operators
        internOperator("::", 600, XFY);
        internOperator("::", 600, FY);// message sending to "self"
        internOperator("^^", 600, FY);// "super" call (calls an inherited or imported method definition)
        // mode operator
        internOperator("+", 200, FY);// input argument (instantiated); ISO Prolog standard operator
        internOperator("@", 200, FY);// input argument (not modified by the call)
        internOperator("?", 200, FY);// input/output argument
        internOperator("-", 200, FY);// output argument (not instantiated); ISO Prolog standard operator
        internOperator("++", 200, FY);// ground argument
        internOperator("--", 200, FY);// unbound argument (typically when returning an opaque term)
        internOperator("<<", 400, YFX);// bitwise left-shift operator (used for context-switching calls)
        // some backend Prolog compilers don't declare this ISO Prolog standard operator!
        internOperator(">>", 400, YFX);// bitwise right-shift operator (used for lambda expressions)
        // some backend Prolog compilers don't declare this ISO Prolog standard operator!
        internOperator("as", 700, XFX);// predicate alias operator (alternative to the ::/2 or :/2 operators depending on the context)
// first introduced in SWI-Prolog and YAP also for defining aliases to module predicates

// HiTalk operator
        internOperator("public", 1150, FX);
        internOperator("protected", 1150, FX);
        internOperator("private", 1150, FX);
        internOperator("enumeration", 1150, FX);
    }
}