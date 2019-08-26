package org.ltc.hitalk.database;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Functor;
import org.ltc.hitalk.entities.HtPredicateIndicator;

/**
 *
 */
public
class Database {
    /**
     * @param head
     * @return
     */
    public
    Functor clause ( Functor head ) {
        return clause(head, -1);
    }

    /**
     * @param head
     * @return
     */
    public
    Functor clause ( Functor head, int dbRef ) {
        return null;
    }

    /**
     * @param filename
     */
    public
    void consult ( Functor filename ) {

    }

    /**
     * @param filename
     */
    public
    void reconsult ( Functor filename ) {

    }

    /**
     * @param clause
     */
    public
    void assertA ( Clause clause ) {

    }

    /**
     * @param clause
     */
    public
    void assertZ ( Clause clause ) {

    }

    /**
     *
     */
    public
    void retract () {

    }

    /**
     *
     */
    public
    void retractall () {

    }

    /**
     *
     */
    public
    void abolish ( HtPredicateIndicator indicator ) {

    }

    /**
     *
     */
    public
    void recordA () {

    }

    /**
     *
     */
    public
    void recordZ () {

    }

    /**
     * @return
     */
    public
    boolean erased () {
        return false;
    }

    /**
     * @return
     */
    public
    boolean recorded () {
        return false;
    }

    /**
     *
     */
    public
    void erase () {
    }

    /**
     *
     */
    public
    void listing () {

    }
}