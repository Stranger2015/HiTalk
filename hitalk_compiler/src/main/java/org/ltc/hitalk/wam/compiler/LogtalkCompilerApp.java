package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;
import static org.ltc.hitalk.wam.compiler.Language.LOGTALK;

public class LogtalkCompilerApp<T extends HtMethod, P, Q> extends PrologCompilerApp <T, P, Q> {
    private final static Language language = LOGTALK;

    /**
     * @param fn
     */
    public LogtalkCompilerApp ( String fn ) {
        super(fn);
    }

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
