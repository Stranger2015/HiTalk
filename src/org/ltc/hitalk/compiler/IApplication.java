package org.ltc.hitalk.compiler;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;

/**
 *
 */
public
interface IApplication {
    /**
     * @return
     */
    IConfig getConfig ();

    /**
     *
     */
    void start () throws Exception;

    /**
     * @return
     */
    int end ();

    /**
     * @return
     */
    boolean isStarted ();

    /**
     * @return
     */
    boolean isStopped ();

    /**
     * @return
     */
    VariableAndFunctorInterner getInterner ();

    /**
     *
     */
    void banner ();
}
