package org.ltc.hitalk.wam.machine;

import org.ltc.hitalk.compiler.IVafInterner;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.core.ICompiler;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.interpreter.HtResolutionEngine;
import org.ltc.hitalk.parser.*;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.HtMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind;
import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.RESOURCE_ERROR;

/**
 *
 */
public
class HiTalkWAMEngine<T extends HtMethod, P, Q, PC, QC> extends HtResolutionEngine<T, P, Q, PC, QC> {

    protected final Logger log = LoggerFactory.getLogger(getClass().getSimpleName());

    /**
     * HiTalkWAMEngine implements a {@link HtResolutionEngine} for an WAM-based Prolog with built-ins. This engine loads its
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
    private static final String BUILT_IN_LIB = "wam_builtins.pl";
    private static final String BUILT_IN_LIB_CORE = "core.lgt";
    private final IResolver<P, Q> resolver;

    /**
     * Creates a prolog parser using the specified interner.
     *
     * @param parser
     * @param interner The functor and variable name interner.
     * @param compiler
     */
    public HiTalkWAMEngine(PlPrologParser parser,
                           IVafInterner interner,
                           ICompiler<T, P, Q> compiler,
                           IResolver<P, Q> resolver) {
        super(parser, interner, compiler);
        this.resolver = resolver;
    }

    public void setCompiler(ICompiler<T, P, Q> compiler) {
        this.compiler = compiler;
    }

    protected ICompiler<T, P, Q> compiler;

    /**
     * {InheritDoc}
     * <p>
     * Loads the built-in library resource specified by {@link #BUILT_IN_LIB}.
     */
    public void reset() throws Exception {
        // Reset the resolver to completely clear out its domain.
        cleanupDomain();

        // Create a token source to load the model rules from.
        InputStream input = getClass().getClassLoader().getResourceAsStream(BUILT_IN_LIB);
        if (input == null) {
            throw new ExecutionError(RESOURCE_ERROR, new HtFunctorName(BUILT_IN_LIB, 0));
        } else {
            /**
             * @param fileName
             * @return
             * @throws FileNotFoundException
             */
            PlLexer tokenSource = PlLexer.getTokenSourceForInputStream(input, BUILT_IN_LIB);
//            HiTalkInputStream stream= appContext.createHiTalkInputStream(Paths.get(BUILT_IN_LIB));
//            PlLexer tokenSource = new PlLexer(stream);
//            stream.setTokenSource(tokenSource);
            IParser libParser = new HiTalkParser();
            libParser.setTokenSource(tokenSource);       // Set up a parser on the token source.
            // Load the built-ins into the domain.
            try {
                while (true) {
                    HtClause sentence = libParser.sentence();
                    if (sentence == null) {
                        break;
                    }

                    compiler.compile(sentence);
                }

                compiler.endScope();
            } catch (HtSourceCodeException e) {
                // There should not be any errors in the built in library, if there are then the prolog engine just
                // isn't going to work, so report this as a bug.
                throw new ExecutionError(Kind.PERMISSION_ERROR,
                        "Got an exception whilst loading the built-in library.",
                        e);
            }
        }
    }

    /**
     *
     */
    protected void cleanupDomain() {

    }

    /**
     * @return
     */
    public IResolver<P, Q> getResolver() {
        return resolver;
    }

    /**
     * Interns a functor name to an integer id. A functor is uniquely identified by a name and its arity. Two functors
     * with the same name but different arity are actually different functors.
     *
     * @param name The name and arity of the functor to intern.
     * @return An interned id for the functor.
     */
    public int internFunctorName(HtFunctorName name) {
        return internFunctorName(name);
    }

    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param sentence The sentence to compile.
     * @throws HtSourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    public void compile(T sentence) throws Exception {

    }
}