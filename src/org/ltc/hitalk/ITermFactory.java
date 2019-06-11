package org.ltc.hitalk;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.term.Atom;
import org.ltc.hitalk.term.HiLogCompound;
import org.ltc.hitalk.term.ListTerm;

/**
 *
 */
public
interface ITermFactory {

    /**
     * @return
     */
    HiLogCompound createHiLogCompound ( Term term, ListTerm args );

    /**
     * @param s
     * @return
     */
    Atom createAtom ( String s );

    Functor createCompound ( String s, Term[] head, Term tail );
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