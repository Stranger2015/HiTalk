package org.ltc.hitalk.wam.printer;


import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermTraverser;
import com.thesett.aima.search.Operator;
import org.ltc.hitalk.parser.HtClause;

import java.util.Iterator;


/**
 * HtClauseTraverser provides a traversal pattern over clauses.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide all reachable child terms of a clause.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
interface HtClauseTraverser extends TermTraverser {

    /**
     * Visits a clause.
     *
     * @param clause  The clause to visit.
     * @param reverse <tt>true</tt> if the child operators should be presented in reverse order to what is deemed to be
     *                a natural, left-to-right ordering.
     * @return An iterator over operators producing the traversed elements of the clause.
     */
    Iterator <Operator <Term>> traverse ( HtClause clause, boolean reverse );
}
