package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.IParser;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.prolog.PrologWAMCompiler;

/**
 * @param <T>
 * @param <P>
 * @param <Q>
 */
public interface ICompilerFactory<T extends HtClause, P, Q, PC extends HiTalkWAMCompiledPredicate, QC extends HiTalkWAMCompiledQuery> extends IHitalkObject {

    /**
     * @param language
     * @return
     */
    PrologWAMCompiler<T, P, Q, PC, QC> createWAMCompiler(Language language);

    /**
     * @param language
     * @return
     */
    IPreCompiler createPreCompiler ( Language language );

    /**
     * @param language
     * @return
     */
    BaseInstructionCompiler<T, P, Q, PC, QC> createInstrCompiler(Language language);

    /**
     * @param language
     * @return
     */
    IParser createParser ( Language language );
}
