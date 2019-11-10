package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.term.PackedDottedPair;

public interface PackedDottedPairVisitor {
    void visit ( PackedDottedPair dottedPair ) throws LinkageException;
}
