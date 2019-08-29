package org.ltc.hitalk.wam.machine;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.HiTalkParser;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import java.io.InputStream;
import java.util.Objects;

import static com.thesett.aima.logic.fol.isoprologparser.TokenSource.getTokenSourceForInputStream;

/**
 *
 */
public
class HiTalkWAMEngine extends HtResolutionEngine <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

    /**
     * HiTalkWAMEngine implements a {@link ResolutionEngine} for an WAM-based Prolog with built-ins. This engine loads its
     * standard library of built-ins from a resource on the classpath.
     *
     * <pre><p/><table id="crc"><caption>CRC Card</caption>
     * <tr><th> Responsibilities <th> Collaborations
     * <tr><td> Load the Prolog built-in library from a resource on the classpath when the engine is reset.
     * </table></pre>
     *
     * @author Rupert Smith
     */

//Holds the name of the resource on the classpath that contains the built-in library.
    private static final String BUILT_IN_LIB = "wam_builtins.lgt";
    private static final String BUILT_IN_LIB_CORE = "core.lgt";
    //    private static final String BUILT_IN_LIB = "wam_builtins.hlgt";
//    private static final String BUILT_IN_LIB = "wam_builtins.hlgt";

    /**
     * Creates a prolog parser using the specified interner.
     *
     * @param parser
     * @param interner The functor and variable name interner.
     * @param compiler
     * @param resolver
     */
    public
    HiTalkWAMEngine ( HtPrologParser parser,
                      VariableAndFunctorInterner interner,
                      LogicCompiler <HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler,
                      Resolver <HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> resolver ) {
        super(parser, interner, compiler, resolver);
    }

    public
    void setCompiler ( LogicCompiler <HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler ) {
        this.compiler = compiler;
    }

    protected LogicCompiler <HtClause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler;

    /**
     * {InheritDoc}
     *
     * Loads the built-in library resource specified by {@link #BUILT_IN_LIB}.
     */
    public
    void reset () {
        // Reset the resolver to completely clear out its domain.
        resolver.reset();

        // Create a token source to load the model rules from.
        InputStream input = getClass().getClassLoader().getResourceAsStream(BUILT_IN_LIB);
        HtTokenSource tokenSource = (HtTokenSource) getTokenSourceForInputStream(Objects.requireNonNull(input));

        // Set up a parser on the token source.
        Parser <HtClause, Token> libParser = new HiTalkParser(tokenSource, interner);
        libParser.setTokenSource(tokenSource);

        // Load the built-ins into the domain.
        try {
            while (true) {
                Sentence <HtClause> sentence = libParser.parse();

                if (sentence == null) {
                    break;
                }

                compiler.compile(sentence);
            }

            compiler.endScope();
        } catch (SourceCodeException e) {
            // There should not be any errors in the built in library, if there are then the prolog engine just
            // isn't going to work, so report this as a bug.
            throw new IllegalStateException("Got an exception whilst loading the built-in library.", e);
        }
    }
}

