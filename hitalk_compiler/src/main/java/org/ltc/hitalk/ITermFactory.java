package org.ltc.hitalk;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.*;
import org.ltc.hitalk.term.ListTerm.Kind;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.nio.file.Path;

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

    IFunctor newFunctor(int value, ListTerm args);

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

    IFunctor newFunctor(int hilogApply, ITerm name, ListTerm args);

    IntTerm newAtomic(int i);

    FloatTerm newAtomic(double f);

    ListTerm newListTerm(Kind kind, ITerm... headTail);

    String createFlag(String scratch_directory, Path scratchDir);

    /**
     * @param functor
     * @return
     */
    IFunctor createMostGeneral(IFunctor functor) throws Exception;

    IFunctor newFunctor(String name, int arity);

    NumberTerm createNumber(String s);

    HtNonVar createNonvar(String value);
}