package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import org.ltc.hitalk.ITermFactory;
import org.ltc.hitalk.compiler.bktables.PlOperatorTable;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.PlTokenSource;
import org.ltc.hitalk.term.HlOperator;
import org.ltc.hitalk.term.HlOperator.Associativity;
import org.ltc.hitalk.term.io.HiTalkStream;

public interface IParser<T extends Term> {
    HiTalkStream getStream ();

    VariableAndFunctorInterner getInterner ();

    ITermFactory getFactory ();

    PlOperatorTable getOptable ();

    void setOperator ( HlOperator op );

    String language ();

    T parse ();

    void setTokenSource ( PlTokenSource source );

    PlTokenSource getTokenSource ();

    void internOperator ( String colonColon, int i, Associativity xfy );

    void initializeBuiltIns ();
}
