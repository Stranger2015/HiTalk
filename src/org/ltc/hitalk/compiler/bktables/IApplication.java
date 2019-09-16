package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import org.ltc.hitalk.parser.HtPrologParser;

/**
 *
 */
public
interface IApplication {

    /**
     * @return
     */
    IConfig getConfig ();

    void init ();

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

    /**
     * @param parser
     */
    void setParser ( HtPrologParser parser );

    /**
     * @return
     */
    HtPrologParser getParser ();

    /**
     * @param fileName
     */
    void setFileName ( String fileName );

    void setTokenSource ( TokenSource tokenSource );
}
