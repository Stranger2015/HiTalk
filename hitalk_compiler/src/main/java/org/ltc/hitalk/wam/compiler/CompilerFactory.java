package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.wam.compiler.hitalk.PrologInstructionCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

import static org.ltc.hitalk.core.BaseApp.getAppContext;

public class CompilerFactory<T extends HtClause, P, Q, PC, QC> implements ICompilerFactory <T, P, Q, PC, QC> {
    /**
     * @param language
     * @return
     */
    public PrologWAMCompiler <T, P, Q, PC, QC> createWAMCompiler ( Language language ) {
        return (PrologWAMCompiler <T, P, Q, PC, QC>) language.getCompiler();
    }

    /**
     * @param language
     * @return
     */
    public PrologPreCompiler <T, P, Q> createPreCompiler ( Language language ) {

        return new PrologPreCompiler <T, P, Q>(
                getAppContext().getSymbolTable(),
                getAppContext().getInterner(),
                getAppContext().getDefaultBuiltIn(),
                getAppContext().getBuiltInTransform(),
                getAppContext().getResolverPre(),
                getAppContext().getParser());
    }

    /**
     * @param language
     * @return
     */
    public PrologInstructionCompiler <T, PC, QC> createInstrCompiler ( Language language ) {
        return new PrologInstructionCompiler <>(
                getAppContext().getSymbolTable(),
                getAppContext().getInterner(),
                getAppContext().getDefaultBuiltIn(),
                getAppContext().getObserverIC(),
                getAppContext().getParser());
    }

    /**
     * @param language
     * @return
     */
    public IParser createParser ( Language language ) {
        return language.getParser();
    }
}
