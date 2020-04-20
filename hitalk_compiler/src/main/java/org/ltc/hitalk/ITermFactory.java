package org.ltc.hitalk;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.util.List;

/**
 *
 */
public
interface ITermFactory extends IHitalkObject {

    /**
     * 整数値アトムを生成します。
     */
    IFunctor newAtom(int value);

    /**
     * @param value
     * @return
     */
    IFunctor newAtom(String value);

    /**
     * ひとつ以上の引数を持つ関数子を作成します。
     */
    IFunctor newFunctor(int hilogApply, String value, ListTerm args);

    /**
     * @param name
     * @param listTerm
     * @return
     */
    IFunctor newHiLogFunctor(String name, ListTerm listTerm);

    /**
     * @param term
     * @param args
     * @return
     */
    IFunctor newFunctor(IFunctor term, ListTerm args) throws Exception;

    /**
     * 変数を作成します。
     */
    HtVariable newVariable(String value);

    /**
     * @param s
     * @return
     */
    IFunctor createAtom(String s);

    /**
     * @param flagName
     * @param flagValue
     * @return
     */
    HtProperty createFlag(String flagName, String flagValue);

    /**
     * @param name
     * @param args
     * @param kind
     * @return
     */
    HtEntityIdentifier createIdentifier(HtEntityKind kind, String name, ITerm... args);

    /**
     * @param name
     * @param args
     * @return
     */
    HtProperty createFlag(String name, ITerm... args);

    IntTerm newIntTerm(int i);

    FloatTerm newFloatTerm(double f);

    ListTerm newListTerm(ListTerm.Kind kind, List<ITerm> headTail);

    /**
     * @param functor
     * @return
     */
    IFunctor createMostGeneral(HtFunctorName functor) throws Exception;

    NumberTerm createNumber(String s);

    HtNonVar createNonvar(String value);

    IFunctor newFunctor(String name, ListTerm args);

    IFunctor newHiLogFunctor(ITerm name, ListTerm args);

    /**
     * @param namesHeads
     * @return
     */
    IFunctor newHiLogFunctor(List<ITerm> namesHeads);

    IFunctor newHiLogFunctor(IFunctor name, ListTerm args);

    /**
     * @param prefix
     * @param image
     * @return
     */
    IntTerm newIntTerm(String prefix, String image);

    /**
     * @param prefix
     * @param image
     * @return
     */
    FloatTerm newFloatTerm(String prefix, String image);
}