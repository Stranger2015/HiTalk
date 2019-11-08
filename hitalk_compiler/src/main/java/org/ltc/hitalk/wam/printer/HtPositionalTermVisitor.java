package org.ltc.hitalk.wam.printer;

/**
 *
 */
public
interface HtPositionalTermVisitor extends HtAllTermsVisitor {
    /**
     * @param positionalTraverser
     */
    void setPositionalTraverser ( HtPositionalTermTraverser positionalTraverser );
}
