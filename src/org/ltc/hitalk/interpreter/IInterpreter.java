package org.ltc.hitalk.interpreter;

import com.thesett.aima.logic.fol.isoprologparser.PrologParserConstants;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.parsing.SourceCodePosition;
import jline.ConsoleReader;
import org.ltc.hitalk.compiler.bktables.IApplication;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.HtPrologParser;
import org.ltc.hitalk.term.io.TermIO;
import org.ltc.hitalk.wam.compiler.HtTokenSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public
interface IInterpreter<S extends HtClause> extends IApplication {
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
    HtPrologParser getParser ();

    /**
     * @throws IOException
     */
    default
    void interpreterLoop () throws IOException {
        // Display the welcome message.
        banner();

        // Initialize the JLine console.
        setConsoleReader(initializeCommandLineReader());

        // Used to buffer input, and only feed it to the parser when a PERIOD is encountered.
        TokenBuffer tokenBuffer = new TokenBuffer(getTermIO());

        // Used to hold the currently buffered lines of input, for the purpose of presenting this back to the user
        // in the event of a syntax or other error in the input.
        List <String> inputLines = new ArrayList <>();

        // Used to count the number of lines entered.
        int lineNo = 0;
        Mode mode = Mode.Query;

        while (true) {
            String line = null;

            try {
                line = getConsoleReader().readLine(getQueryPrompt());
                inputLines.add(line);
                // JLine returns null if CTRL-D is pressed. Exit program getMode() back to query getMode(), or exit the
                // interpreter completely from query getMode().
                if ((line == null) && ((getMode() == Mode.Query) || (getMode() == Mode.QueryMultiLine))) {
                    /*log.fine("CTRL-D in query getMode(), exiting.");*/

                    System.out.println();
                    break;
                }
                else if ((line == null) && (getMode() == Mode.Program || (getMode() == Mode.ProgramMultiLine))) {
                    /*log.fine("CTRL-D in program getMode(), returning to query getMode().");*/

                    System.out.println();
                    mode = Mode.Query; /////fixme
                    continue;
                }

                // Check the input to see if a system directive was input. This is only allowed in query getMode(), and is
                // handled differently to normal queries.
                if (getMode() == Mode.Query) {
                    HtTokenSource tokenSource = HtTokenSource.getTokenSourceForString(line, lineNo);
                    getParser().setTokenSource(tokenSource);

                    HtPrologParser.Directive directive = getParser().peekAndConsumeDirective();

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
                HtTokenSource tokenSource = HtTokenSource.getTokenSourceForString(line, lineNo);//todo
                Token nextToken;

                while (true) {
                    nextToken = tokenSource.poll();

                    if (nextToken == null) {
                        break;
                    }

                    if (nextToken.kind == PrologParserConstants.PERIOD) {
                        /*log.fine("Token was PERIOD.");*/
                        mode = (getMode() == Mode.QueryMultiLine) ? Mode.Query : getMode();
                        mode = (getMode() == Mode.ProgramMultiLine) ? Mode.Program : getMode();

                        tokenBuffer.offer(nextToken);
                        break;
                    }
                    else if (nextToken.kind == PrologParserConstants.EOF) {
                        /*log.fine("Token was EOF.");*/
                        mode = (getMode() == Mode.Query) ? Mode.QueryMultiLine : getMode();
                        mode = (getMode() == Mode.Program) ? Mode.ProgramMultiLine : getMode();

                        lineNo++;
                        break;
                    }

                    tokenBuffer.offer(nextToken);
                }

                // Evaluate the current token buffer, whenever the input is terminated with a PERIOD.
                if ((nextToken != null) && (nextToken.kind == PrologParserConstants.PERIOD)) {
                    getParser().setTokenSource(tokenBuffer);

                    // Parse the next clause.
                    S nextClause = (S) getParser().sentence();

                    /*log.fine(nextParsing.toString());*/
                    evaluate(nextClause);

                    inputLines.clear();
                }
            } catch (SourceCodeException e) {
                SourceCodePosition sourceCodePosition = e.getSourceCodePosition().asZeroOffsetPosition();
                int startLine = sourceCodePosition.getStartLine();
                int endLine = sourceCodePosition.getEndLine();
                int startColumn = sourceCodePosition.getStartColumn();
                int endColumn = sourceCodePosition.getEndColumn();

                System.out.println("[(" + startLine + ", " + startColumn + "), (" + endLine + ", " + endColumn + ")]");

                for (int i = 0; i < inputLines.size(); i++) {
                    String errorLine = inputLines.get(i);
                    System.out.println(errorLine);

                    // Check if the line has the error somewhere in it, and mark the part of it that contains the error.
                    int pos = 0;

                    if (i == startLine) {
                        for (; pos < startColumn; pos++) {
                            System.out.print(" ");
                        }
                    }

                    if (i == endLine) {
                        for (; pos <= endColumn; pos++) {
                            System.out.print("^");
                        }

                        System.out.println();
                    }

                    if ((i > startLine) && (i < endLine)) {
                        for (; pos < errorLine.length(); pos++) {
                            System.out.print("^");
                        }

                        System.out.println();
                    }
                }

                System.out.println();
                System.out.println(e.getMessage());
                System.out.println();

                inputLines.clear();
                tokenBuffer.clear();
            }
        }
    }

    /**
     * @param clause
     * @throws SourceCodeException
     */
    void evaluate ( HtClause clause ) throws SourceCodeException;

    /**
     * @return
     */
    ConsoleReader getConsoleReader ();

    /**
     * @return
     */
    String getQueryPrompt ();

    /**
     * @param reader
     */
    void setConsoleReader ( ConsoleReader reader );

    /**
     * @return
     * @throws IOException
     */
    ConsoleReader initializeCommandLineReader () throws IOException;

    /**
     * @return
     */
    TermIO getTermIO ();

    /**
     *
     */
    default
    void banner () {
        System.out.printf("\n| %s %s %s\n| %s\n", getProductName(), getVersion(), getCopyright(), getLicense());
    }

    /**
     * @return
     */
    String getProductName ();

    /**
     * @return
     */
    String getVersion ();

    /**
     * @return
     */
    default
    String getCopyright () {
        return "Copyright (c) 2019, Anton Danilov, All rights reserved.";
    }

    /**
     * @return
     */
    default
    String getLicense () {
        return "Licensed under the Apache License, Version 2.0.\n" + "| https//www.apache.org/licenses/LICENSE-2.0";
    }
}
