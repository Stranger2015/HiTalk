package org.ltc.hitalk.wam.machine;

import com.thesett.aima.logic.fol.*;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.isoprologparser.SentenceParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledPredicate;
import org.ltc.hitalk.wam.compiler.HiTalkWAMCompiledQuery;

import java.io.InputStream;

import static com.thesett.aima.logic.fol.isoprologparser.TokenSource.getTokenSourceForInputStream;

public
class HiTalkWAMEngine extends ResolutionEngine <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> {

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
    private static final String BUILT_IN_LIB = "wam_builtins.hlgt";
    protected final HtPrologParser parser;
    protected final VariableAndFunctorInterner interner;

    public
    void setCompiler ( LogicCompiler <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler ) {
        this.compiler = compiler;
    }

    protected LogicCompiler <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler;

    /**
     * Builds an logical resolution engine from a parser, interner, compiler and resolver.
     *
     * @param parser   The parser.
     * @param interner The interner.
     */
    public
    HiTalkWAMEngine ( HtPrologParser parser,
                      //
                      VariableAndFunctorInterner interner,
                      //
                      LogicCompiler <Clause, HiTalkWAMCompiledPredicate, HiTalkWAMCompiledQuery> compiler ) {
        this.parser = parser;
        this.interner = interner;
        this.compiler = compiler;
    }

    /**,
     nheritDoc}
     * <p>
     * <p/>Loads the built-in library resource specified by {@link #BUILT_IN_LIB}.
     */
    public
    void reset () {
        // Reset the resolver to completely clear out its domain.
        resolver.reset();

        // Create a token source to load the model rules from.
        InputStream input = getClass().getClassLoader().getResourceAsStream(BUILT_IN_LIB);
        TokenSource tokenSource = (TokenSource) getTokenSourceForInputStream(input);

        // Set up a parser on the token source.
        Parser <Clause, Token> libParser = new SentenceParser(interner);
        libParser.setTokenSource(tokenSource);

        // Load the built-ins into the domain.
        try {
            while (true) {
                Sentence <Clause> sentence = libParser.parse();

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

