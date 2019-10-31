package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.TermVisitor;

/**
 *
 */
public
interface HtPositionalTermVisitor extends TermVisitor {
    /**
     * @param positionalTraverser
     */
    void setPositionalTraverser ( HtPositionalTermTraverser positionalTraverser );
}
