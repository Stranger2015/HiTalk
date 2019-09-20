package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.HtType;

/**
 *
 */
public
class Flag /*extends HtProperty */ {

    private final HtType type;
    private final FunctorName name;
    private final Term value;

    /**
     * @param type
     * @param name
     * @param value
     */
    public
    Flag ( HtType type, FunctorName name, Term value ) {
        this.type = type;
        this.name = name;
        this.value = value;
    }

    public
    class Hook {
    }

//    public
//    Flag ( int n, ListTerm args ) {
//        super(n, args);
//    }
//
//    public
//    Flag ( int ffn, Functor functor ) {
//        super(ffn, functor);
//    }
}