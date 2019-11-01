package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.Tools;

/**
 *
 */
public class PrologInterpreterApp extends BaseApplication <HtClause, HtPredicate, HtClause> {

    public static void main ( String[] args ) {

    }

    /**
     * @param varOrFunctor
     * @return
     */
    public String namespace ( String varOrFunctor ) {
        return null;
    }

    /**
     *
     */
    public void undoInit () {

    }

    public void shutdown () {

    }

    public void doStart () throws Exception {

    }

    public IProduct product () {
        return product;
    }

    public Language language () {
        return getParser().language();
    }

    public Tools tool () {
        return Tools.INTERPRETER;
    }
}
