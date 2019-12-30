package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.prolog.PrologInstructionCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologPreCompiler;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.EXISTENCE_ERROR;

public class CompilerFactory<T extends HtClause, P, Q, PC, QC> implements ICompilerFactory <T, P, Q, PC, QC> {

    /**
     * @param language
     * @return
     */
    @Override
    public PlPrologParser createParser ( Language language ) {
        try {
            return (PlPrologParser) language.getParserClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new ExecutionError(EXISTENCE_ERROR, null);
        }
    }

    /**
     * @param language
     * @return
     */
    @Override
    public PrologWAMCompiler <T, P, Q, PC, QC> createWAMCompiler ( Language language ) {
        try {
            return (PrologWAMCompiler <T, P, Q, PC, QC>) language.getWamCompilerClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new ExecutionError(EXISTENCE_ERROR, null);
        }
    }

    /**
     * @param language
     * @return
     */
    public IPreCompiler createPreCompiler ( Language language ) {
        try {
            return (PrologPreCompiler <T, P, Q>) language.getPreCompilerClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new ExecutionError(EXISTENCE_ERROR, null);
        }
    }

    /**
     * @param language
     * @return
     */
    public BaseInstructionCompiler <T, PC, QC> createInstrCompiler ( Language language ) {
        try {
            return (PrologInstructionCompiler <T, PC, QC>) language.getInstrCompilerClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new ExecutionError(EXISTENCE_ERROR, null);
        }
    }

    public void toString0 ( StringBuilder sb ) {

    }
}
