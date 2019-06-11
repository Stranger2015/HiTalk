package org.ltc.hitalk.wam.interpreter;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.isoprologparser.PrologParser;
import com.thesett.aima.logic.fol.isoprologparser.PrologParserConstants;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.parsing.SourceCodePosition;
import com.thesett.common.util.Source;
import jline.ConsoleReader;

import java.io.IOException;
import java.util.ArrayList;

/**
 *
 */
public
interface IInterpreter {
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
    Parser getParser ();

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
        TokenBuffer tokenBuffer = new TokenBuffer();

        // Used to hold the currently buffered lines of input, for the purpose of presenting this back to the user
        // in the event of a syntax or other error in the input.
        ArrayList <String> inputLines = new ArrayList <>();

        // Used to count the number of lines entered.
        int lineNo = 0;

        while (true) {
            String line = null;

            try {
                line = getConsoleReader().readLine(getQueryPrompt());
                inputLines.add(line);

                // JLine returns null if CTRL-D is pressed. Exit program getMode() back to query getMode(), or exit the
                // interpreter completely from query getMode().
                if ((line == null) && ((getMode() == Mode.Query) || (getMode() == getMode().QueryMultiLine))) {
                    /*log.fine("CTRL-D in query getMode(), exiting.");*/

                    System.out.println();

                    break;
                }
                else if ((line == null) && (getMode() == getMode().Program || (getMode() == getMode().ProgramMultiLine))) {
                    /*log.fine("CTRL-D in program getMode(), returning to query getMode().");*/

                    System.out.println();
                    getMode() = getMode().Query;

                    continue;
                }

                // Check the input to see if a system directive was input. This is only allowed in query getMode(), and is
                // handled differently to normal queries.
                if (getMode() == getMode().Query) {
                    Source <Token> tokenSource = new OffsettingTokenSource(TokenSource.getTokenSourceForString(line), lineNo);
                    parser.setTokenSource(tokenSource);

                    PrologParser.Directive directive = parser.peekAndConsumeDirective();

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
                                getMode() = getMode().Program;
                                break;
                        }

                        inputLines.clear();

                        continue;
                    }
                }

                // For normal queries, the query functor '?-' begins every statement, this is not passed back from
                // JLine even though it is used as the command prompt.
                if (getgetMode() () == getMode().Query){
                    line = getQueryPrompt() + line;
                    inputLines.set(inputLines.size() - 1, line);
                }

                // Buffer input tokens until EOL is reached, of the input is terminated with a PERIOD.
                Source <Token> tokenSource = new OffsettingTokenSource(TokenSource.getTokenSourceForString(line), lineNo);
                Token nextToken;

                while (true) {
                    nextToken = tokenSource.poll();

                    if (nextToken == null) {
                        break;
                    }

                    if (nextToken.kind == PrologParserConstants.PERIOD) {
                        /*log.fine("Token was PERIOD.");*/
                        getMode() = (getMode() == getMode().QueryMultiLine) ? getMode().Query : getMode();
                        getMode() = (getMode() == getMode().ProgramMultiLine) ? getMode().Program : getMode();

                        tokenBuffer.offer(nextToken);

                        break;
                    }
                    else if (nextToken.kind == PrologParserConstants.EOF) {
                        /*log.fine("Token was EOF.");*/
                        getMode() = (getMode() == getMode().Query) ? getMode().QueryMultiLine : getMode();
                        getMode() = (getMode() == getMode().Program) ? getMode().ProgramMultiLine : getMode();

                        lineNo++;

                        break;
                    }

                    tokenBuffer.offer(nextToken);
                }

                // Evaluate the current token buffer, whenever the input is terminated with a PERIOD.
                if ((nextToken != null) && (nextToken.kind == PrologParserConstants.PERIOD)) {
                    getParser().setTokenSource(tokenBuffer);

                    // Parse the next clause.
                    Sentence <Clause <? extends com.thesett.aima.logic.fol.Functor>> nextParsing = parser.parse();

                    /*log.fine(nextParsing.toString());*/
                    evaluate(nextParsing);

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

    ConsoleReader getConsoleReader ();

    String getQueryPrompt ();

    void setConsoleReader ( ConsoleReader reader );

    ConsoleReader initializeCommandLineReader () throws IOException;

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
