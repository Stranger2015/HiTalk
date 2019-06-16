package org.ltc.hitalk.compiler.bktables;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.entities.HtEntity;

public
class HtEvent<NT> implements INameable <NT> {
    NT name;
    Term obj;
    Term sender;
    Term message;
    HtEntity monitor;
    Term call;

    @Override
    public
    NT getName () {
        return name;
    }
}
