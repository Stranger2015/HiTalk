package org.ltc.hitalk.term.io;


import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.PrologParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.Source;
import org.jetbrains.annotations.Nullable;
import org.ltc.hitalk.compiler.bktables.LogtalkFlag;

import java.util.ArrayList;
import java.util.List;

/* ===========================================================
 *
 */
@Deprecated
public
class TermReader {

    private final PrologParser pp;
    private final List <LogtalkFlag.Hook> expansionHooks;

    //    private TermStack termStack;
    // hook1 standard reading hooks// begin_of_file/edd_of_file
    //hilog
    // hook3 binarization
    //userefinedhooks
//    IHiTalkParserWrapper parser = new HandWrittenParserWrapper(in, termStack);
//    Term t = readTerm(in, null);


    /**
     * Creates a term parser over an interner.
     *
     * @param tokenSource
     * @param interner    The interner to use to intern all functor and variable names.
     */

    public
    TermReader ( Source <Token> tokenSource, VariableAndFunctorInterner interner ) {

        pp = new PrologParser(tokenSource, interner);
//        "Prolog_Variable_Namespace", "Prolog_Functor_Namespace"

        expansionHooks = new ArrayList <>();
    }

//    @Nullable
//    public Term readTerm (List <RwOption> options ) {
//
//        Term t = readRawTerm(options);
//        List <LogtalkFlag.Hook> hook;
//        //  for (hook = expansionHooks; expansionHooks.nextToken; )
//        //  TermExpander expander = getTermExpander();
//        //t = expander.execute(t);
//
//        return t;
//    }
//        if(t.getName()==PrologConstants.IMPLIES)


    @Nullable
    public
    Term readRawTerm ( List <RwOption> options ) {
        //boolean t = initOptions(options);
        try {
            return pp.term();
        } catch (SourceCodeException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

//    private ListOptions getReadTermDefaultOtions(){
//        return ;
//    }


//    public Term readTermFromString ( List <RwOption> options ) {
//
//        return null;
//    }
//
//    public Term readTermFromString ( String s, List <RwOption> options ) {
//        TokenSource  tokenSource = TokenSource.getTokenSourceForString(s);
//       return readRawTerm(options);
//    }

    public
    Clause readClause () {
        try {
            return pp.clause();
        } catch (SourceCodeException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }
}
