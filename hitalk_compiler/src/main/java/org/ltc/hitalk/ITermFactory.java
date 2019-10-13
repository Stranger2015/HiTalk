package org.ltc.hitalk;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.DottedPair;

/**
 *
 */
public
interface ITermFactory {

    /**
     * 整数値アトムを生成します。
     */
    Term newAtom ( int value );

    Term newAtom ( String value );

    /**
     * 実数値アトムを生成します。
     */
    Term newAtom ( double value );

    /**
     * ひとつ以上の引数を持つ関数子を作成します。
     */
    Term newFunctor ( String value, DottedPair args );

    Term newFunctor ( int value, DottedPair args );

    /**
     * 変数を作成します。
     */
    Term newVariable ( String value );

    /**
     * @return
     */
//    HiLogCompound createHiLogCompound ( Term term, ListTerm args );

    /**
     * @param s
     * @return
     */
    Functor createAtom ( String s );

    Functor createCompound ( String s, Term[] head, Term tail );

    HtProperty createFlag ( String flagName, String flagValue );

    /**
     * @param name
     * @param args
     * @param kind
     * @return
     */
    HtEntityIdentifier createIdentifier ( HtEntityKind kind, String name, Term... args );

    /**
     * @param name
     * @param args
     * @return
     */
    HtProperty createFlag ( String name, Term... args );

//    /**/Term newFunctor ( Term[] nameHeadTail );

    Term newFunctor ( int hilogApply, Term name, DottedPair args );

    Term newAtomic ( int i );

    Term newAtomic ( double f );


//    Term newFunctor ( Term name, Term[] args );

//    HtProperty createProperty ( String name, String value );
//    HtProperty createProperty ( String name, Term... args );

}
/*
    public static final String ANON_VAR = PrologConstants.UNDERSCORE_VAR_NAME;
    public static final Atom IMPLIES = createAtom(":-");
    public static final Atom END_OF_FILE = AtomConstants.END_OF_FILE;
    public static final Atom BEGIN_OF_FILE = AtomConstants.BEGIN_OF_FILE;
    public static final Term[] EMPTY_TERM_ARRAY = new Term[0];
    public static final Term NOP = null;
    public static final Atom ARROW = createAtom("->");
    public static final PredicateIndicator ARROW_2 = createPredicateIndicator(true, Term.ARROW, 2);

    */