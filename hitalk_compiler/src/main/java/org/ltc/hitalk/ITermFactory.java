package org.ltc.hitalk;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtEntityKind;
import org.ltc.hitalk.entities.HtProperty;
import org.ltc.hitalk.term.Atom;
import org.ltc.hitalk.term.FloatTerm;
import org.ltc.hitalk.term.IntTerm;
import org.ltc.hitalk.term.ListTerm;
import org.ltc.hitalk.term.ListTerm.Kind;
import org.ltc.hitalk.wam.compiler.IFunctor;

import java.nio.file.Path;

/**
 *
 */
public
interface ITermFactory {

    /**
     * 整数値アトムを生成します。
     */
    Atom newAtom ( int value );

    Atom newAtom ( String value );

    /**
     * 実数値アトムを生成します。
     * @return
     */
    FloatTerm newAtom ( double value );

    /**
     * ひとつ以上の引数を持つ関数子を作成します。
     */
    IFunctor newFunctor ( int hilogApply, String value, ListTerm args );

    IFunctor newFunctor ( int value, ListTerm args );

    /**
     * 変数を作成します。
     */
    Variable newVariable ( String value );

    /**
     * @param s
     * @return
     */
    IFunctor createAtom ( String s );

//    Functor createCompound ( String s, Term[] head, Term tail );

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

    IFunctor newFunctor ( int hilogApply, Term name, ListTerm args );

    IntTerm newAtomic ( int i );

    FloatTerm newAtomic ( double f );

    ListTerm newDottedPair ( Kind kind, Term[] terms );

    HtProperty createFlag ( String scratch_directory, Path scratchDir );

    /**
     * @param functor
     * @return
     */
    IFunctor createMostGeneral ( IFunctor functor );

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