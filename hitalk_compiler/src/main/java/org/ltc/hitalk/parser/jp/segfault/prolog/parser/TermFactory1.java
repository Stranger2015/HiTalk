package org.ltc.hitalk.parser.jp.segfault.prolog.parser;


import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Nil;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.term.DottedPair;
import org.ltc.hitalk.term.DottedPair.Kind;

import static org.ltc.hitalk.term.Atom.EMPTY_TERM_ARRAY;

/**
 * Prologの項(term)を作成します。
 *
 * @author shun
 */
public abstract class TermFactory1<T extends Term> {

    private Kind kind;
    private Term[] headTail;

    /**
     * 文字列アトムを生成します。
     */
    public abstract T newAtom ( String value );

    /**
     * 整数値アトムを生成します。
     */
    public abstract T newAtom ( int value );

    /**
     * 実数値アトムを生成します。
     */
    public abstract T newAtom ( double value );

    /**
     * ひとつ以上の引数を持つ関数子を作成します。
     */
    public abstract T newFunctor ( String value, Term[] args );

    /**
     * 変数を作成します。
     */
    public abstract T newVariable ( String value );

    public Functor newAtom ( VariableAndFunctorInterner interner, PlToken.TokenKind ldelim, PlToken.TokenKind rdelim ) {
        String s = String.format("%s%s", ldelim.getImage(), rdelim.getImage());
        return new Functor(interner.internFunctorName(s, 0), new Term[0]);
    }

    /**
     * @param kind
     * @param headTail
     * @return
     */
    public T newDottedPair ( Kind kind, Term[] headTail ) {
        this.kind = kind;
        this.headTail = headTail;
        T t;
        if (headTail.length == 0) {
            t = (T) new Nil(0, EMPTY_TERM_ARRAY);
        } else //if (headTail.length ==1){ //[|VarOrList] []
            t = (T) new DottedPair(kind, headTail);
        return t;
    }
}
