package org.ltc.hitalk.term.io;


import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.IOperatorTable;
import org.ltc.hitalk.compiler.bktables.TermFactory;
import org.ltc.hitalk.compiler.bktables.error.ExecutionError;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlDynamicOperatorParser;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlPrologParser;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.ltc.hitalk.compiler.bktables.error.ExecutionError.Kind.PERMISSION_ERROR;

/**
 *
 */
public
class TermIO {
    private static TermIO instance;

    static {
        try {
            instance = new TermIO();
        } catch (IOException e) {
            e.printStackTrace();
            throw new ExecutionError(PERMISSION_ERROR, null);
        }
    }

    private final VariableAndFunctorInterner interner;
    private final ITermFactory termFactory;
    private PlPrologParser parser;
    private final IOperatorTable optable;

    /**
     * @return
     */
    public static TermIO instance () {
        return instance;
    }


    protected final List <HiTalkStream> streams = new ArrayList <>(3);

    /**
     * @param i
     * @return
     */
    public HiTalkStream getStream ( int i ) {
        return streams.get(i);
    }

    /**
     * @return
     */
    public HiTalkStream currentInput () {
        return getStream(0);
    }

    /**
     * @return
     */
    public HiTalkStream currentOutput () {
        return getStream(1);
    }

    /**
     * @return
     */
    public HiTalkStream currentError () {
        return getStream(2);
    }

    public VariableAndFunctorInterner getInterner () {
        return interner;
    }

    public ITermFactory getTermFactory () {
        return termFactory;

    }

    public PlPrologParser getParser () {
        return parser;
    }

    public IOperatorTable getOptable () {
        return optable;
    }

    /**
     *
     */
    public TermIO () throws IOException {
//        FileInputStream in = new FileInputStream("current_input");
//        FileOutputStream out = new FileOutputStream("current_output");
//        FileOutputStream err = new FileOutputStream("current_error");

        streams.add(new HiTalkStream(FileDescriptor.in));
        streams.add(new HiTalkStream(FileDescriptor.out));
        streams.add(new HiTalkStream(FileDescriptor.err));

//        System.setIn(in);
//        System.setOut(new PrintStream(out));
//        System.setErr(new PrintStream(err, true));

        interner = new VariableAndFunctorInternerImpl(
                "HiTalk_Variable_Namespace",
                "HiTalk_Functor_Namespace");

        termFactory = new TermFactory(interner);
        optable = new PlDynamicOperatorParser();
        parser = new PlPrologParser(getStream(0), interner, termFactory, optable);


        initOptions("org/ltc/hitalk/wam/compiler/startup.pl");
    }

    private void initOptions ( String fn ) {

    }

    /**
     * @param parser
     */
    public void setParser ( PlPrologParser parser ) {
        TermIO.instance().setParser(parser);
    }

    /**
     * @param interner
     */
    public void setInterner ( VariableAndFunctorInterner interner ) {
        TermIO.instance().setInterner(interner);
    }
}