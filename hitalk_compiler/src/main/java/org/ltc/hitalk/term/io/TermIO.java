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

    private VariableAndFunctorInterner interner;
    private ITermFactory termFactory;
    private PlPrologParser parser;
    private IOperatorTable optable;

    /**
     * @return
     */
    public static TermIO instance () {
        return instance;
    }


    protected final List <HiTalkStream> streams = new ArrayList <>();

    /**
     * @param i
     * @return
     */
    public HiTalkStream getStream ( int i ) {
        return streams.get(i);
    }

    /**
     * @param termFactory
     */
    public void setTermFactory ( ITermFactory termFactory ) {
        this.termFactory = termFactory;
    }

    /**
     * @param optable
     */
    public void setOptable ( IOperatorTable optable ) {
        this.optable = optable;
    }

    /**
     * @param stream
     * @return
     */
    public HiTalkStream addStream ( HiTalkStream stream ) {
        streams.add(stream);
        return stream;
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

    /**
     * @return
     */
    public VariableAndFunctorInterner getInterner () {
        return interner;
    }

    /**
     * @return
     */
    public ITermFactory getTermFactory () {
        return termFactory;
    }

    /**
     * @return
     */
    public PlPrologParser getParser () {
        return parser;
    }

    /**
     * @return
     */
    public IOperatorTable getOptable () {
        return optable;
    }

    /**
     *
     */
    private TermIO () throws IOException {
        streams.add(new HiTalkStream(FileDescriptor.in, true));//  "current_input"
        streams.add(new HiTalkStream(FileDescriptor.out, false));// "current_output"
        streams.add(new HiTalkStream(FileDescriptor.err, false));// "current_error"

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
        this.parser = parser;
    }

    /**
     * @param interner
     */
    public void setInterner ( VariableAndFunctorInterner interner ) {
        this.interner = interner;
    }
}