package org.ltc.hitalk.wam.transformers;

/**
 *
 */
public
interface IMetrics extends Comparable <IMetrics> {
    /**
     * @return
     */
    IMetrics initialMetrics ();

    /**
     * @param currentMetrics
     * @return
     */
    IMetrics subtract ( IMetrics currentMetrics );
}
