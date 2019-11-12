package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.term.PackedDottedPair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BodyCall extends PiCalls {
    List <Term> args = new ArrayList <>();
    List <HtClause> selectedClauses = new ArrayList <>();

    /**
     * @param sym
     * @param dottedPair
     */
    public BodyCall ( IFunctor sym, PackedDottedPair dottedPair ) {
        super(sym, dottedPair);
    }

    public String toStringArguments () {
        return super.toStringArguments();//fixme
    }

    public void forEach ( Consumer <? super Term> action ) {

    }
}
