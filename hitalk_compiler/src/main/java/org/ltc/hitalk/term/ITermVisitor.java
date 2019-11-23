package org.ltc.hitalk.term;

/**
 * TermVisitor provides a visitor pattern over terms.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Visit a term.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface ITermVisitor {

    /**
     * Visits a term.
     *
     * @param term The term to visit.
     */
    default void visit ( ITerm term ) {

    }

//    void visit ( ITerm term1, ITerm term2 );
}

