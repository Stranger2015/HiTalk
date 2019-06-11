package org.ltc.hitalk.wam.interpreter;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.interpreter.InteractiveParser;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;
import jline.ConsoleReader;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;


/**
 * HiTalkInterpreter implements an interactive Prolog like interpreter, built on top of a {@link ResolutionEngine}.
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
 * @param <T> The compiled clause type that the compiler produces.
 * @param <Q> The compiled query type that the compiler produces.
 * @author Rupert Smith
 */
public
class HiTalkInterpreter<T, Q> implements IInterpreter {
    public
    HiTalkInterpreter ( Mode program ) {

    }

    /* Used for debugging purposes. */
    /* private static final Logger log = Logger.getLogger(HiTalkInterpreter.class.getName()); */

    @Override
    public
    String getQueryPrompt () {
        return queryPrompt;
    }

    @Override
    public
    void setConsoleReader ( ConsoleReader reader ) {

    }

    /**
     * The prompt to use in query mode.
     */
    public final String queryPrompt = "[|= ]";

    public
    String getMultiLineQueryPrompt () {
        return multiLineQueryPrompt;
    }

    /**
     * The line continuation prompt for query mode.
     */
    public final String multiLineQueryPrompt = "   ";

    public
    String getProgramPrompt () {
        return programPrompt;
    }

    /**
     * The prompt to use in program mode.
     */
    public final String programPrompt = "";

    public
    String getMultiLineProgramPrompt () {
        return multiLineProgramPrompt;
    }

    /**
     * The line continuation prompt for program mode.
     */
    public final String multiLineProgramPrompt = "   ";

    /**
     * ASCII for a semicolon.
     */
    public static final int SEMICOLON = 59;

    /**
     * @return
     */
    public
    String getInterpreter () {
        return interpreter;
    }

    protected final String interpreter = "HiTalk interpreter";

    /**
     * Holds the interactive parser that the interpreter loop runs on.
     */
    protected final InteractiveParser parser;

    /**
     * Holds the resolution engine that the interpreter loop runs on.
     */
    protected final ResolutionEngine <Clause, T, Q> engine;

    @Override
    public
    ConsoleReader getConsoleReader () {
        return consoleReader;
    }

    /**
     * Holds the JLine console reader.
     */
    private ConsoleReader consoleReader;

    /**
     * Holds the name of the predicate currently being parsed, clause by clause.
     */
    private Integer currentPredicateName;

    /**
     * Holds the current interaction mode.
     */
    private Mode mode = Mode.Query;

    /**
     * Builds an interactive logical resolution interpreter from a parser, interner, compiler and resolver, encapsulated
     * as a resolution engine.
     *
     * @param engine The resolution engine. This must be using an {@link InteractiveParser}.
     */
    public
    HiTalkInterpreter ( ResolutionEngine <Clause, T, Q> engine ) {
        this.engine = engine;
        Parser <Clause, Token> parser = engine.getParser();

        if (!(parser instanceof InteractiveParser)) {
            throw new IllegalArgumentException("'engine' must be built on an InteractiveParser.");
        }

        this.parser = (InteractiveParser) parser;
    }

    /**
     * @return
     */
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
    Parser getParser () {
        return parser;
    }

    /**
     * Implements the top-level interpreter loop. This will parse and evaluate sentences until it encounters an CTRL-D
     * in query mode, at which point the interpreter will terminate.
     *
     * @throws SourceCodeException If malformed code is encountered.
     * @throws IOException         If an IO error is encountered whilst reading the source code.
     */
    public final
    void interpreterLoop () throws IOException {
        IInterpreter.super.interpreterLoop();
    }

    /**
     * Sets up the JLine console reader.
     *
     * @return A JLine console reader.
     * @throws IOException If an IO error is encountered while reading the input.
     */
    public
    ConsoleReader initializeCommandLineReader () throws IOException {
        ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);

        return reader;
    }

    @Override
    public
    String getProductName () {
        return "HiTalk interpreter";
    }

    @Override
    public
    String getVersion () {
        return "";
    }

    @Override
    public
    String getCopyright () {
        return "";
    }

    /**
     * Evaluates a query against the resolver or adds a clause to the resolvers domain.
     *
     * @param sentence The clausal sentence to run as a query or as a clause to add to the domain.
     * @throws SourceCodeException If the query or domain clause fails to compile or link into the resolver.
     */
    protected
    void evaluate ( Sentence <Clause> sentence ) throws SourceCodeException {
        Clause clause = sentence.getT();

        if (clause.isQuery()) {
            engine.endScope();
            engine.compile(sentence);
            evaluateQuery();
        }
        else {
            // Check if the program clause is new, or a continuation of the current predicate.
            int name = clause.getHead().getName();

            if ((currentPredicateName == null) || (currentPredicateName != name)) {
                engine.endScope();
                currentPredicateName = name;
            }

            addProgramClause(sentence);
        }
    }

    /**
     * Evaluates a query. In the case of queries, the interner is used to recover textual names for the resulting
     * variable bindings. The user is queried through the parser to if more than one solution is required.
     */
    private
    void evaluateQuery () {
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
     * @param sentence The clause to add to the domain.
     */
    private
    void addProgramClause ( Sentence <Clause> sentence ) throws SourceCodeException {
        /*log.fine("Read program clause from input.");*/

        engine.compile(sentence);
    }
}