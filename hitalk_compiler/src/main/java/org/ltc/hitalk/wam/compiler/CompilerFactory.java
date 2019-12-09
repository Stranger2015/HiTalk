package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.wam.compiler.hitalk.PrologInstructionCompiler;
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
    public PrologWAMCompiler <HtClause, HtPredicate, HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledClause>
    createWAMCompiler ( Language language ) {
        try {
            return (PrologWAMCompiler <HtClause,
                    HtPredicate,
                    HtClause,
                    HiTalkWAMCompiledPredicate,
                    HiTalkWAMCompiledClause>) language.getWamCompilerClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new ExecutionError(EXISTENCE_ERROR, null);
        }
    }

    /**
     * @param language
     * @return
     */
    public ICompiler <T, P, Q> createPreCompiler ( Language language ) {

//        return new PrologPreCompiler <T, P, Q>(
//                getAppContext().getSymbolTable(),
//                getAppContext().getInterner(),
//                getAppContext().getDefaultBuiltIn(),
//                getAppContext().getBuiltInTransform(),
//                getAppContext().getResolverPre(),
//                getAppContext().getParser());
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
    public ICompiler <T, PC, QC> createInstrCompiler ( Language language ) {
//        return new PrologInstructionCompiler <>(
//                getAppContext().getSymbolTable(),
//                getAppContext().getInterner(),
//                getAppContext().getDefaultBuiltIn(),
//                getAppContext().getObserverIC(),
//                getAppContext().getParser());
        try {
            return (PrologInstructionCompiler <T, PC, QC>) language.getInstrCompilerClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            throw new ExecutionError(EXISTENCE_ERROR, null);
        }
    }
}
