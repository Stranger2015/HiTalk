package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import jline.ConsoleReader;
import org.ltc.hitalk.compiler.bktables.IConfig;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.term.io.TermIO;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;


/**
 * HiLogInterpreter implements an interactive Prolog like interpreter, built on top of a {@link HtResolutionEngine}.
 * It implements a top-level interpreter loop where queries or domain clauses may be entered. Queries are resolved
 * against the current domain using the resolver, after they have been compiled.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Parse text into first order logic clauses. <td> {@link com.thesett.aima.logic.fol.Parser}.
 * <tr><td> Compile clauses down to their compiled form. <td> {@link Compiler}.
 * <tr><td> Add facts to the current knowledge base. <td> {@link com.thesett.aima.logic.fol.Resolver}.
 * <tr><td> Resolve queries against the current knowledge base. <td> {@link com.thesett.aima.logic.fol.Resolver}.
 * <tr><td> Print the variable bindings resulting from resolution.
 *     <td> {@link com.thesett.aima.logic.fol.VariableAndFunctorInterner}.
 * </table></pre>
 *
 * @param <T> The compiled clause entityKind that the compiler produces.
 * @param <Q> The compiled query entityKind that the compiler produces.
 * @author Rupert Smith
 */
public
class HiLogInterpreter<S extends HtClause, T, Q> implements IInterpreter <S> {

    protected TermIO termIO;
    /* Used for debugging purposes. */
    /* private static final Logger log = Logger.getLogger(HiLogInterpreter.class.getName()); */

//    /**
//     * The prompt to use in query mode.
//     */
//    public static final String QUERY_PROMPT = "HiLog:>";
//
//    /**
//     * The line continuation prompt for query mode.
//     */
//    public static final String MULTI_LINE_QUERY_PROMPT = "   ";
//
//    /**
//     * The prompt to use in program mode.
//     */
//    public static final String PROGRAM_PROMPT = "";
//
//    /**
//     * The line continuation prompt for program mode.
//     */
//    public static final String MULTI_LINE_PROGRAM_PROMPT = "   ";
//
//    /**
//     * ASCII for a semicolon.
//     */
//    public static final int SEMICOLON = 59;
    /**
     * Holds the interactive parser that the interpreter loop runs on.
     */
    protected final InteractiveParser parser;
    /**
     * Holds the JLine console reader.
     */
    protected ConsoleReader consoleReader;
    /**
     * Holds the name of the predicate currently being parsed, clause by clause.
     */
    protected Integer currentPredicateName;
    /**
     * Holds the resolution engine that the interpreter loop runs on.
     */
    protected HtResolutionEngine <T, Q> engine;
    /**
     * Holds the current interaction mode.
     */
    protected Mode mode = Mode.Query;
    protected IConfig config;

    /**
     * Builds an interactive logical resolution interpreter from a parser, interner, compiler and resolver, encapsulated
     * as a resolution engine.
     *
     * @param engine The resolution engine. This must be using an {@link InteractiveParser}.
     */
    public
    HiLogInterpreter ( InteractiveParser parser, HtResolutionEngine <T, Q> engine ) {
        this.engine = engine;

//        if (!(parser instanceof InteractiveParser)) {
//            throw new IllegalArgumentException("'engine' must be built on an InteractiveParser.");
//        }

        this.parser = parser;
    }

    @Override
    public
    Mode getMode () {
        return mode;
    }

    /**
     * @return
     */
    @Override
    public
    HtPrologParser getParser () {
        return null;
    }

    /**
     * @param arg
     */
    @Override
    public
    void setFileName ( String arg ) {

    }

    @Override
    public
    void setTokenSource ( TokenSource tokenSource ) {

    }
//
//    @Override
//    public
//    HiLogParser getParser () {
//        return parser;
//    }

    /**
     * Implements the top-level interpreter loop. This will parse and evaluate clauses until it encounters an CTRL-D
     * in query mode, at which point the interpreter will terminate.
     *
     * @throws SourceCodeException If malformed code is encountered.
     * @throws IOException         If an IO error is encountered whilst reading the source code.
     */
    public
    void interpreterLoop () throws IOException {
        IInterpreter.super.interpreterLoop();
    }

    @Override
    public
    ConsoleReader getConsoleReader () {
        return consoleReader;
    }

    @Override
    public
    void setConsoleReader ( ConsoleReader reader ) {
        consoleReader = reader;
    }

    @Override
    public
    String getQueryPrompt () {
        return "HiLog:>";
    }

    @Override
    public
    String getProductName () {
        return "HiLog interpreter";
    }

    @Override
    public
    String getVersion () {
        return "0.0.1-preAlpha#1";
    }

    /**
     * Sets up the JLine console reader.
     *
     * @return A JLine console reader.
     * @throws IOException If an IO error is encountered while reading the input.
     */
    @Override
    public
    ConsoleReader initializeCommandLineReader () throws IOException {
        ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);

        return reader;
    }

    @Override
    public
    TermIO getTermIO () {
        return termIO;
    }

    /**
     * Evaluates a query against the resolver or adds a clause to the resolvers domain.
     *
     * @param clause The clause to run as a query or as a regular clause to add to the domain.
     */
    @Override
    public
    void evaluate ( HtClause clause ) throws SourceCodeException {
//        HtClause clause = sentence.getT();
        if (clause.isQuery()) {
            engine.endScope();
            engine.compile(clause);
            evaluateQuery(clause);
        }
        else {
            // Check if the program clause is new, or a continuation of the current predicate.
            int name = clause.getHead().getName();

            if ((currentPredicateName == null) || (currentPredicateName != name)) {
                engine.endScope();
                currentPredicateName = name;
            }

            addProgramClause((Sentence <S>) clause);
        }
    }

    /**
     * Evaluates a query. In the case of queries, the interner is used to recover textual names for the resulting
     * variable bindings. The user is queried through the parser to if more than one solution is required.
     * @param clause
     */
    private
    void evaluateQuery ( HtClause clause ) {
        /*log.fine("Read query from input.");*/

        // Create an iterator to generate all solutions on demand with. Iteration will stop if the request to
        // the parser for the more ';' token fails.
        Iterator <Set <Variable>> i = engine.iterator();

        if (!i.hasNext()) {
            System.out.println("false. ");

            return;
        }

        for (; i.hasNext(); ) {
            Set <Variable> solution = i.next();

            if (solution.isEmpty()) {
                System.out.print("true");
            }
            else {
                for (Iterator <Variable> j = solution.iterator(); j.hasNext(); ) {
                    Variable nextVar = j.next();

                    String varName = engine.getVariableName(nextVar.getName());

                    System.out.print(varName + " = " + nextVar.getValue().toString(engine, true, false));

                    if (j.hasNext()) {
                        System.out.println();
                    }
                }
            }

            // Finish automatically if there are no more solutions.
            if (!i.hasNext()) {
                System.out.println(".");

                break;
            }

            // Check if the user wants more solutions.
            try {
                int key = consoleReader.readVirtualKey();

                if (key == SEMICOLON) {
                    System.out.println(" ;");
                }
                else {
                    System.out.println();

                    break;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Adds a program clause to the domain. Multiple program clauses making up a predicate are compiled as a unit, and
     * not individually. For this reason, Prolog expects clauses for the same predicate to appear together in source
     * code. When a clause with a name and arity not seen before is encountered, a new compiler scope is entered into,
     * and this compiler scope is closed at the EOF of the current input stream, or when another clause with a different
     * name and arity is seen.
     *
     * @param clause The clause to add to the domain.
     */
    protected
    void addProgramClause ( Sentence <S> clause ) throws SourceCodeException {
        /*log.fine("Read program clause from input.");*/

        engine.compile((Sentence <HtClause>) clause);
    }

    /**
     * @return
     */
    @Override
    public
    IConfig getConfig () {
        return config;
    }

    /**
     *
     */
    @Override
    public
    void start () throws Exception {

    }

    /**
     * @return
     */
    @Override
    public
    int end () {
        return 0;
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStarted () {
        return false;
    }

    /**
     * @return
     */
    @Override
    public
    boolean isStopped () {
        return false;
    }

    /**
     * @return
     */
    @Override
    public
    VariableAndFunctorInterner getInterner () {
        return null;
    }

    @Override
    public
    void setParser ( HtPrologParser parser ) {

    }
}