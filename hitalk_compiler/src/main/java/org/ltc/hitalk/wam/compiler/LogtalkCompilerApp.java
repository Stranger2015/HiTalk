package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.PrologCompilerApp;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

public class LogtalkCompilerApp<T extends HtMethod, P, Q, PC extends HiTalkWAMCompiledPredicate,
        QC extends HiTalkWAMCompiledQuery> extends PrologCompilerApp<T, P, Q, PC, QC> {

    /**
     * @param fn
     */
    public LogtalkCompilerApp(String fn) throws Exception {
        super(fn);
    }

    /**
     * @return
     */
    @Override
    public Language getLanguage () {
        return language;
    }

    /**
     * @param args
     */
    public static void main ( String[] args ) {
        try {
            IApplication application = new LogtalkCompilerApp <>(args[0]);
            application.init();
            application.start();
        } catch (Exception e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }
}
