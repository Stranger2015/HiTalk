package org.ltc.hitalk.entities.context;

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
