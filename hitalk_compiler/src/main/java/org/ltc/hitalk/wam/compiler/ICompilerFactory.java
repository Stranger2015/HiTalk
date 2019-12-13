package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledClause;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public interface ICompilerFactory<T extends HtClause, P, Q, PC, QC> extends IHitalkObject {
    /**
     * @param language
     * @return
     */
    PrologWAMCompiler <HtClause, HtPredicate, HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledClause> createWAMCompiler ( Language language );

    /**
     * @param language
     * @return
     */
    ICompiler <T, P, Q> createPreCompiler ( Language language );

    /**
     * @param language
     * @return
     */
    ICompiler <T, PC, QC> createInstrCompiler ( Language language );

    /**
     * @param language
     * @return
     */
    IParser createParser ( Language language );
}
