package org.ltc.hitalk.wam.compiler.hilog;


import com.thesett.aima.logic.fol.TermTransformer;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;
import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.VafInterner;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IFunctor;

/**
 *
 */
public
class HiLogToPrologBiDiConverter<T extends HtClause, P, Q, PC, QC> {

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

    SymbolTable<Integer, String, Object> symbolTable = new SymbolTableImpl<>();
    IVafInterner interner = new VafInterner("", "");


    private final HiLogDecoder <T, P, Q, PC, QC> hilog2Prolog;//= new HiLogDecoder<>(symbolTable, interner,);
//    private final HiLogEncoder prolog2Hilog = new HiLogEncoder(symbolTable, interner);

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
//        }if (prologTerm.isVar()) {
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
//        }if (prologTerm.isVar()) {
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
//        }if (prologTerm.isVar()) {
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
//        }if (prologTerm.isVar()) {
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
//        }if (prologTerm.isVar()) {
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

    public HiLogToPrologBiDiConverter ( IVafInterner interner,
                                        HiLogDecoder <T, P, Q, PC, QC> hilog2Prolog ) {
//        transformer = this.hilog2Prolog;
//        transformer = this.prolog2Hilog;

        HILOG_APPLY = interner.internFunctorName("$hilog_apply", 2);
//        HILOG_APPLY2 = interner.internFunctorName("$hilog_apply2", 2);
        this.hilog2Prolog = hilog2Prolog;
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

    private boolean hasHiLogApplyFunctorName(ITerm term, int applyF) throws Exception {
        return ((IFunctor) term).getName() == applyF;
    }

    private ITerm mapList (TermTransformer transformer, ListTerm termList ) {
        boolean mustExit = false;
        while (true) {

            ITerm listHead;
            ITerm listTail = termList;
            ITerm outList;
            ITerm outListHead;
            ITerm outListTail;///=outList;
            ITerm temp_term;
        }

//        final ITerm list=termList;//fixme
    }


    private boolean isNil ( ListTerm term ) {
        return false;
    }

    private boolean isFormula ( ITerm term ) {
        return false;
    }

    private boolean isList ( ITerm term ) {
        return false;
    }

    private boolean isProtected ( ITerm term ) {
        return false;
    }

    public ITerm convert ( ITerm term ) {
        return null;// transformer.transform(term);
    }

    private IFunctor createFunctor ( int name, int arity ) {
//            final Functor functor = new Functor(name, arity);
        return null;
    }

    private ITerm mapSpecialForm ( TermTransformer transformer, ITerm term ) {

        return null;
    }

    private boolean isSpecialForm ( ITerm term ) {
        return false;
    }

}
