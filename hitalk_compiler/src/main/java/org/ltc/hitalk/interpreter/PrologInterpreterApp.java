package org.ltc.hitalk.interpreter;

import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApplication;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.Tools;

import java.io.IOException;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public class PrologInterpreterApp<T extends HtClause, P, Q> extends BaseApplication <T, P, Q> {
    /**
     * @param fn
     */
    public PrologInterpreterApp ( String fn ) throws IOException, CloneNotSupportedException {
        if (fn.startsWith("meta:")) {
            fn = fn.substring(6);
            getParser().setTokenSource(PlTokenSource.getPlTokenSourceForString(fn));
        } else {
            setFileName(fn);
        }
    }

    /**
     * @param args
     */
    public static void main ( String[] args ) {
        try {
            IApplication application = new PrologInterpreterApp <>(args[0]);
            application.init();
            application.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
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
