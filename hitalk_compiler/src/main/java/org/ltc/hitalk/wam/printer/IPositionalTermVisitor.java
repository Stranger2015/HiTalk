package org.ltc.hitalk.wam.printer;

/**
 *
 */
public
interface IPositionalTermVisitor extends IAllTermsVisitor {
    /**
     * @param positionalTraverser
     */
    void setPositionalTraverser ( IPositionalTermTraverser positionalTraverser );
}
