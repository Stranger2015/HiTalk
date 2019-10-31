package org.ltc.hitalk.wam.task;

import com.thesett.aima.logic.fol.*;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public
class HiLogToPrologBiDiConverter {

//    private final Map <Term, Term> hiLogTerms = new HashMap <>();

    /**
     * Functor representing hilog compound where args are just in usual way
     * arity = source arity + 1 when source arity >= 1
     */
    public int HILOG_APPLY;

    /**
     * Functor representing hilog compound where args are coded as a list
     * arity = 2 source arity >= 0 or partially unknown (open lists).
     */
//    public int HILOG_APPLY2;

    private boolean unify;
    private int applyF;

    SymbolTable <Integer, String, Object> symbolTable = new SymbolTableImpl <>();
    VariableAndFunctorInterner interner = new VariableAndFunctorInternerImpl("", "");

    private final HiLogDecoder hilog2Prolog = new HiLogDecoder(symbolTable, interner);
    private final HiLogEncoder prolog2Hilog = new HiLogEncoder(symbolTable, interner);

//    private final HiLogEncoder transformer;
//        = new TermTransformer() {
//        /**
//         * @param prologTerm
//         * @return
//         */
//        @Override
//        public
//        Term transform ( Term prologTerm ) {
//            if (prologTerm.isVar()) {
//                Variable v = (Variable) prologTerm;
//                if (v.isBound()) {
//                    if (unify) {
//                        return v.queryConversion();
//                    }
//                    else {
//                        return v.getSubstitution();//fixme
//                    }
//                }
//                else {
//                    return v.getValue();
//                }
//            }
//            else if (prologTerm.isConstant() || isProtected(prologTerm)) {
//                return prologTerm;
//            }
//            if (isList(prologTerm)) {
//                return mapList(transformer, (ListTerm) prologTerm);
//            }
//            if (isSpecialForm(prologTerm)) {
//                return mapSpecialForm(transformer, prologTerm);
//            }
//            if (!prologTerm.isCompound()) {
//                throw new ExecutionError(TYPE_ERROR, null);
//            }
//            if (hasHiLogApplyFunctorName(prologTerm, applyF)) {
//                return prologTerm; //already converted
//            }
//            if (isFormula(prologTerm)) {
//                return prologTerm;
//            }
//
//            return prologTerm;
//        }
//    };

    public HiLogToPrologBiDiConverter ( VariableAndFunctorInterner interner ) {
//        transformer = this.hilog2Prolog;
//        transformer = this.prolog2Hilog;

        HILOG_APPLY = interner.internFunctorName("$hilog_apply", 2);
//        HILOG_APPLY2 = interner.internFunctorName("$hilog_apply2", 2);
    }

    public boolean isUnify () {
        return unify;
    }

    public void setUnify ( boolean unify ) {
        this.unify = unify;
    }

    public int getApplyF () {
        return applyF;
    }

    public void setApplyF ( int applyF ) {
        this.applyF = applyF;
    }

//    public TermTransformer getTransformer () {
//        return                      transformer;
//    }
//
//    public void setTransformer ( TermTransformer transformer ) {
//        this.transformer = transformer;
//    }

    private boolean hasHiLogApplyFunctorName ( Term term, int applyF ) {
        return ((Functor) term).getName() == applyF;
    }

    private Term mapList ( TermTransformer transformer, ListTerm termList ) {
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


    private boolean isNil ( ListTerm term ) {
        return false;
    }

    private boolean isFormula ( Term term ) {
        return false;
    }

    private boolean isList ( Term term ) {
        return false;
    }

    private boolean isProtected ( Term term ) {
        return false;
    }

    public Term convert ( Term term ) {
        return null;// transformer.transform(term);
    }

    private Functor createFunctor ( int name, int arity ) {
//            final Functor functor = new Functor(name, arity);
        return null;
    }

    private Term mapSpecialForm ( TermTransformer transformer, Term term ) {

        return null;
    }

    private boolean isSpecialForm ( Term term ) {
        return false;
    }

}
