/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA. The text of license can be also found
 * at http://www.gnu.org/copyleft/lgpl.html
 */
package org.ltc.hitalk.gnu.prolog.test;

import org.ltc.hitalk.gnu.getopt.Getopt;
import org.ltc.hitalk.gnu.prolog.Version;
import org.ltc.hitalk.gnu.prolog.database.PrologTextLoaderError;
import org.ltc.hitalk.gnu.prolog.io.*;
import org.ltc.hitalk.gnu.prolog.term.AtomTerm;
import org.ltc.hitalk.gnu.prolog.term.Term;
import org.ltc.hitalk.gnu.prolog.vm.Environment;
import org.ltc.hitalk.gnu.prolog.vm.Interpreter;
import org.ltc.hitalk.gnu.prolog.vm.PrologCode.Result;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;

/**
 * Load a prolog file and run a goal.
 *
 * @see #USAGE
 * @see #EXAMPLE
 */
public class GoalRunner {
    private GoalRunner() {
    }

    public static final String USAGE = "usage: javaorg.ltc.hitalk.gnu.prolog.test.GoalRunner\n"
            + "                                      [-o|--once]\n"
            + "                                      [-t|--threads <threads>]\n"
            + "                                      [-i|--iterations <iterations>]\n"
            + "                                      <text to load> <goal to run>";
    public static final String EXAMPLE = "example: javaorg.ltc.hitalk.gnu.prolog.test.GoalRunner append.pro append([a,b],[c,d],R)";

    private static void usage() {
        System.out.println(USAGE);
        System.out.println(EXAMPLE);
        System.exit(-1);
    }

    public static void main(String args[]) {
        try {
            System.out.println("GNU Prolog for Java (" + Version.getVersion()
                    + ") Goal runner (c) Constantine Plotnikov, 1997-1999.");
            gnu.getopt.LongOpt[] longOptions = {new gnu.getopt.LongOpt("once", gnu.getopt.LongOpt.NO_ARGUMENT, null, 'o'),
                    new gnu.getopt.LongOpt("threads", gnu.getopt.LongOpt.REQUIRED_ARGUMENT, null, 't'),
                    new gnu.getopt.LongOpt("iterations", gnu.getopt.LongOpt.REQUIRED_ARGUMENT, null, 'i')};
            Getopt opts = new Getopt("GoalRunner", args, "ot:i:", longOptions);
            int c;
            int threads = 1, iterations = 1;
            boolean once = false;
            while ((c = opts.getopt()) != -1) {
                switch (c) {
                    case 'o':
                        once = true;
                        break;
                    case 't': {
                        try {
                            threads = Integer.parseInt(opts.getOptarg());
                        } catch (NumberFormatException e) {
                            System.err.println("-t|--threads takes an integer argument not" + opts.getOptarg());
                            usage();
                        }
                        break;
                    }
                    case 'i': {
                        try {
                            iterations = Integer.parseInt(opts.getOptarg());
                        } catch (NumberFormatException e) {
                            System.err.println("-i|--iterations takes an integer argument not" + opts.getOptarg());
                            usage();
                        }
                        break;
                    }
                    case '?':
                        System.err.println("The option '" + (char) opts.getOptopt() + "' is not valid");
                        usage();
                        break;
                    default:
                        System.err.println("getopt() returned " + (char) c);
                        usage();
                        break;
                }
            }
            int argumentsStartIndex = opts.getOptind();
            if ((args.length - argumentsStartIndex) != 2) {
                usage();
            }
            String textToLoad = args[argumentsStartIndex];
            String goalToRun = args[argumentsStartIndex + 1];

            Environment env = new Environment();
            env.ensureLoaded(AtomTerm.get(textToLoad));

            Runner[] runners = new Runner[threads];
            for (int j = 0; j < iterations; ++j) {
                for (int i = 0; i < threads; ++i) {
                    runners[i] = new Runner("it: " + j + " t:" + i, env, once, goalToRun);
                    runners[i].start();
                }
                for (int i = 0; i < threads; ++i) {
                    runners[i].join();
                    runners[i] = null;
                }
            }
            // runners.wait();// don't terminate TODO remove this.

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class Runner extends Thread {
        private Environment env;
        private boolean once;
        private String goalToRun;

        public Runner(String name, Environment environment, boolean once, String goalToRun) {
            super(name);
            env = environment;
            this.once = once;
            this.goalToRun = goalToRun;
        }

        @Override
        public void run() {
            Interpreter interpreter = env.createInterpreter();
            env.runInitialization(interpreter);
            for (PrologTextLoaderError element : env.getLoadingErrors()) {
                PrologTextLoaderError err = element;
                System.err.println(err);
                // err.printStackTrace();
            }
            LineNumberReader kin = new LineNumberReader(new InputStreamReader(System.in));
            StringReader rd = new StringReader(goalToRun);
            TermReader trd = new TermReader(rd, env);
            TermWriter out = new TermWriter(new OutputStreamWriter(System.out));
            ReadOptions rd_ops = new ReadOptions(env.getOperatorSet());
            try {
                Term goalTerm = trd.readTermEof(rd_ops);

                Interpreter.Goal goal = interpreter.prepareGoal(goalTerm);
                String response;
                do {
                    long startTime = System.currentTimeMillis();
                    Result rc = interpreter.execute(goal);
                    long stopTime = System.currentTimeMillis();
                    env.getUserOutput().flushOutput(null);
                    System.out.println("time = " + (stopTime - startTime) + "ms");
                    response = "n";
                    switch (rc) {
                        case SUCCESS: {
                            WriteOptions options = new WriteOptions(new OperatorSet());
                            for (String name : rd_ops.variableNames.keySet()) {
                                out.print(name + " = ");
                                out.print(options, (rd_ops.variableNames.get(name)).dereference());
                                out.print("; ");
                            }
                            out.println();
                            if (once) {
                                out.print("SUCCESS. redo suppressed by command line option \"-once\"");
                                return;
                            }
                            out.print("SUCCESS. redo (y/n/a)?");
                            out.flush();
                            response = kin.readLine();

                            if ("a".equals(response)) {
                                interpreter.stop(goal);
                                goal = interpreter.prepareGoal(goalTerm);
                            }

                            if ("n".equals(response)) {
                                return;
                            }
                            break;
                        }
                        case SUCCESS_LAST: {
                            WriteOptions options = new WriteOptions(new OperatorSet());
                            for (String name : rd_ops.variableNames.keySet()) {
                                out.print(name + " = ");
                                out.print(options, (rd_ops.variableNames.get(name)).dereference());
                                out.print("; ");
                            }
                            out.println();
                            out.println("SUCCESS LAST");
                            out.flush();
                            return;
                        }
                        case FAIL:
                            out.println("FAIL");
                            out.flush();
                            return;
                        case HALT:
                            env.closeStreams();
                            out.println("HALT");
                            out.flush();
                            System.exit(interpreter.getExitCode());
                            return;
                    }
                } while (true);
            } catch (Exception ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
        }
    }
}
