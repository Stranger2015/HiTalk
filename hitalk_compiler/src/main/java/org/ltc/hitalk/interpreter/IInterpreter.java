package org.ltc.hitalk.interpreter;


import jline.ConsoleReader;
import org.ltc.hitalk.compiler.PredicateTable;
import org.ltc.hitalk.core.BaseApp;
import org.ltc.hitalk.core.IConfigurable;
import org.ltc.hitalk.core.IResolver;
import org.ltc.hitalk.entities.HtPredicate;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtSourceCodeException;
import org.ltc.hitalk.parser.PlPrologParser;
import org.ltc.hitalk.parser.PlToken;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.wam.compiler.HtFunctor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 */
public interface IInterpreter<T extends HtClause, P, Q> extends IConfigurable/*, ICompiler <HtClause,P, Q>*/,
        IResolver <P, Q> {
    /**
     *
     */
    int SEMICOLON = 59;

    /**
     * @return
     */
    Mode getMode ();

    /**
     * @return
     */
    PlPrologParser getParser ();

    /**
     * @throws IOException
     */
    default void interpreterLoop () throws IOException {
        // Display the welcome message.
//        banner();

        // Initialize the JLine console.
        setConsoleReader(initializeCommandLineReader());

        // Used to buffer input, and only feed it to the parser when a PERIOD is encountered.
//        TokenBuffer tokenBuffer = (TokenBuffer) TokenBuffer.getTokenSourceForInputStream(System.in, "stdin");
//
        // Used to hold the currently buffered lines of input, for the purpose of presenting this back to the user
        // in the event of a syntax or other error in the input.
        List <String> inputLines = new ArrayList <>();

        // Used to count the number of lines entered.
        int lineNo = 0;
        Mode mode = Mode.Query;

        while (true) {
            String line = null;

            line = getConsoleReader().readLine(getQueryPrompt());
            inputLines.add(line);
            // JLine returns null if CTRL-D is pressed. Exit program getMode() back to query getMode(), or exit the
            // interpreter completely from query getMode().
            if ((line == null) && ((getMode() == Mode.Query) || (getMode() == Mode.QueryMultiLine))) {
                /*log.fine("CTRL-D in query getMode(), exiting.");*/

                System.out.println();
                break;
            } else if ((line == null) && (getMode() == Mode.Program || (getMode() == Mode.ProgramMultiLine))) {
                /*log.fine("CTRL-D in program getMode(), returning to query getMode().");*/

                System.out.println();
                mode = Mode.Query; //fixme
                continue;
            }

            // Check the input to see if a system directive was input. This is only allowed in query getMode(), and is
            // handled differently to normal queries.
            if (getMode() == Mode.Query) {
//                    PlTokenSource tokenSource = PlTokenSource.getPlTokenSourceForString(line, lineNo);
//                    getParser().setTokenSource(tokenSource);

                PlPrologParser.Directive directive = getParser().peekAndConsumeDirective();

                if (directive != null) {
                    switch (directive) {
                        case Trace:
                            /*log.fine("Got trace directive.");*/
                            break;
                        case Info:
                            /*log.fine("Got info directive.");*/
                            break;
                        case User:
                            /*log.fine("Got user directive, entering program getMode().");*/
                            mode = Mode.Program;
                            break;
                    }
                    inputLines.clear();
                    continue;
                }
            }

            // For normal queries, the query functor '?-' begins every statement, this is not passed back from
            // JLine even though it is used as the command prompt.
            if (getMode() == Mode.Query) {
                line = getQueryPrompt() + line;
                inputLines.set(inputLines.size() - 1, line);
            }

            // Buffer input tokens until EOL is reached, of the input is terminated with a PERIOD.
//                PlTokenSource tokenSource = PlTokenSource.getPlTokenSourceForString(line, lineNo);//todo
            PlToken nextToken;

//                while (true) {
////                    nextToken = tokenSource.poll();
////
//                    if (nextToken == null) {
//                        break;
//                    }
//
//                    if (nextToken.kind == PlToken.TokenKind.DOT) {
//                        /*log.fine("Token was PERIOD.");*/
//                        mode = (getMode() == Mode.QueryMultiLine) ? Mode.Query : getMode();
//                        mode = (getMode() == Mode.ProgramMultiLine) ? Mode.Program : getMode();
//
//                        tokenBuffer.offer(nextToken);
//                        break;
//                    } else if (nextToken.kind == EOF) {
//                        /*log.fine("Token was EOF.");*/
//                        mode = (getMode() == Mode.Query) ? Mode.QueryMultiLine : getMode();
//                        mode = (getMode() == Mode.Program) ? Mode.ProgramMultiLine : getMode();
//
//                        lineNo++;
//                        break;
//                    }
//
//                    tokenBuffer.offer(nextToken);
//                }
//
//                 Evaluate the current token buffer, whenever the input is terminated with a PERIOD.
//                if ((nextToken != null) && (nextToken.kind == DOT)) {
//                    getParser().setTokenSource(tokenBuffer);
//
//                     Parse the next clause.
//                    HtClause nextClause = getParser().sentence();
//
//                    /*log.fine(nextParsing.toString());*/
//                    evaluate(nextClause);
//
//                    inputLines.clear();
//                }
        }
    }

    /**
     * @param clause
     * @throws HtSourceCodeException
     */
    void evaluate ( T clause ) throws HtSourceCodeException, HtSourceCodeException;

    /**
     * @return
     */
    ConsoleReader getConsoleReader ();

    /**
     * @param reader
     */
    void setConsoleReader ( ConsoleReader reader );

    /**
     * @return
     */
    String getQueryPrompt ();

    /**
     * @return
     * @throws IOException
     */
    ConsoleReader initializeCommandLineReader () throws IOException;

    default Set <HtVariable> solve ( HtFunctor goal, HtClause clause ) {
        PredicateTable <HtPredicate> predicateTable = BaseApp.getAppContext().getPredicateTable();
        HtClause clause1 = predicateTable.lookup(goal, clause);

        return null;
    }
}
