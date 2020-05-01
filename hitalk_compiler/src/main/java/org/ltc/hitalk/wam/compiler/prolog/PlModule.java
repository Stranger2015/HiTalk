package org.ltc.hitalk.wam.compiler.prolog;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.wam.compiler.HtFunctor;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledClause;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMCompiledPredicate;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PlModule extends HtFunctor implements IHitalkObject {

    List<HiTalkWAMCompiledClause> clauses = new ArrayList<>();

    List<HiTalkWAMCompiledPredicate> predicates = new ArrayList<>();

    /**
     * @param name
     */
    public PlModule(int name) {
        super(name);
    }
//    export/import

    /**
     * @param sb
     */
    public void toString0(StringBuilder sb) {

    }
}
