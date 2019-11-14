package org.ltc.hitalk.wam.printer;

import com.thesett.aima.logic.fol.LinkageException;
import org.ltc.hitalk.term.ListTerm;

public interface IListTermVisitor {
    void visit ( ListTerm listTerm ) throws LinkageException;
}
