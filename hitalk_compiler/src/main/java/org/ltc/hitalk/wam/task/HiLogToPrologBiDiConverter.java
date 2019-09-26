package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.*;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.term.ListTerm;

import java.util.HashMap;
import java.util.Map;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.TYPE_ERROR;

/**
 *
 */
public
class HiLogToPrologBiDiConverter {

    private final Map <Term, Term> hiLogTerms = new HashMap <>();

    /**
     * Functor representing hilog compound where args are just in usual way
     * arity = source arity + 1 when source arity >= 1
     */
    public int HILOG_APPLY;

    /**
     * Functor representing hilog compound where args are coded as a list
     * arity = 2 source arity >= 0 or partially unknown (open lists).
     */
    public int HILOG_APPLY2;

    private boolean unify;
    private int applyF;
    private TermTransformer transformer;
    private final TermTransformer hilog2Prolog =

            new TermTransformer() {
                /**
                 * @param hilogTerm
                 * @return
                 */
                @Override
                public
                Term transform ( Term hilogTerm ) {
                    if (hilogTerm.isVar()) {
                        Variable v = (Variable) hilogTerm;
                        if (v.isBound()) {
                            if (unify) {
                                return v.queryConversion();
                            }
                            else {
                                return v.getSubstitution();//fixme
                            }
                        }
                        else {
                            return v.getValue();
                        }
                    }
                    if (isList(hilogTerm)) {
                        return mapList(transformer, (ListTerm) hilogTerm);
                    }
                    if (isSpecialForm(hilogTerm)) {
                        return mapSpecialForm(transformer, hilogTerm);
                    }
                    if (!hilogTerm.isCompound()) {
                        throw new ExecutionError(TYPE_ERROR, null);
                    }
                    if (hasHiLogApplyFunctorName(hilogTerm, applyF)) {
                        return hilogTerm; //already converted
                    }
                    if (isFormula(hilogTerm)) {
                        return hilogTerm;
                    }
                    return null;
                }
            };
    private final TermTransformer prolog2Hilog = new TermTransformer() {
        /**
         * @param prologTerm
         * @return
         */
        @Override
        public
        Term transform ( Term prologTerm ) {
            if (prologTerm.isVar()) {
                Variable v = (Variable) prologTerm;
                if (v.isBound()) {
                    if (unify) {
                        return v.queryConversion();
                    }
                    else {
                        return v.getSubstitution();//fixme
                    }
                }
                else {
                    return v.getValue();
                }
            }
            else if (prologTerm.isConstant() || isProtected(prologTerm)) {
                return prologTerm;
            }
            if (isList(prologTerm)) {
                return mapList(transformer, (ListTerm) prologTerm);
            }
            if (isSpecialForm(prologTerm)) {
                return mapSpecialForm(transformer, prologTerm);
            }
            if (!prologTerm.isCompound()) {
                throw new ExecutionError(TYPE_ERROR, null);
            }
            if (hasHiLogApplyFunctorName(prologTerm, applyF)) {
                return prologTerm; //already converted
            }
            if (isFormula(prologTerm)) {
                return prologTerm;
            }

            return prologTerm;
        }
    };

    public
    HiLogToPrologBiDiConverter ( VariableAndFunctorInterner interner ) {
        transformer = this.hilog2Prolog;
//        transformer = this.prolog2Hilog;

        HILOG_APPLY = interner.internFunctorName("$hilog_apply", 2);
        HILOG_APPLY2 = interner.internFunctorName("$hilog_apply2", 2);
    }

    public
    boolean isUnify () {
        return unify;
    }

    public
    void setUnify ( boolean unify ) {
        this.unify = unify;
    }

    public
    int getApplyF () {
        return applyF;
    }

    public
    void setApplyF ( int applyF ) {
        this.applyF = applyF;
    }

    public
    TermTransformer getTransformer () {
        return transformer;
    }

    public
    void setTransformer ( TermTransformer transformer ) {
        this.transformer = transformer;
    }

    private
    boolean hasHiLogApplyFunctorName ( Term term, int applyF ) {
        return ((Functor) term).getName() == applyF;
    }

    private
    Term mapList ( TermTransformer transformer, ListTerm termList ) {
        boolean mustExit = false;
        while (!isNil(termList) || !mustExit) {

            Term listHead;
            Term listTail = termList;
            Term outList;
            Term outListHead;
            Term outListTail;///=outList;
            Term temp_term;
        }
        return null;
    }


    private
    boolean isNil ( ListTerm term ) {
        return false;
    }

    private
    boolean isFormula ( Term term ) {
        return false;
    }

    private
    boolean isList ( Term term ) {
        return false;
    }

    private
    boolean isProtected ( Term term ) {
        return false;
    }

    public
    Term convert ( Term term ) {
        return transformer.transform(term);
    }

    private
    Functor createFunctor ( int name, int arity ) {
//            final Functor functor = new Functor(name, arity);
        return null;
    }

    private
    Term mapSpecialForm ( TermTransformer transformer, Term term ) {

        return null;
    }

    private
    boolean isSpecialForm ( Term term ) {
        return false;
    }

    public
    Map <Term, Term> getHiLogTerms () {
        return hiLogTerms;
    }
}
