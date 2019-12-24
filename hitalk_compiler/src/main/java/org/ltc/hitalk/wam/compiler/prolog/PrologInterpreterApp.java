package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.IProduct;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.Language;
import org.ltc.hitalk.wam.compiler.Tools.Kind;

import java.io.IOException;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.wam.compiler.Language.PROLOG;
import static org.ltc.hitalk.wam.compiler.Tools.Kind.INTERPRETER;

/**
 *
 */
public class PrologInterpreterApp<T extends HtClause, P, Q, PC, QC> extends BaseApp <T, P, Q, PC, QC> {

    /**
     * @param fn
     */
    public PrologInterpreterApp ( String fn ) throws IOException, CloneNotSupportedException {
        if (fn.startsWith("meta:")) {
            fn = fn.substring(6);
            ///getParser().setTokenSource(PlTokenSource.getPlTokenSourceForString(fn));
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
     * @return
     */
    public Language getLanguage () {
        return PROLOG;
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

    public Kind tool () {
        return INTERPRETER;
    }

    @Override
    protected void initComponents () {

    }
}
