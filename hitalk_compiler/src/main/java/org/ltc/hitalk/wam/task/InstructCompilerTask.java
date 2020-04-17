package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.compiler.IPendingClausesHolder;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 *
 */
public class InstructCompilerTask implements IPendingClausesHolder, IHitalkObject {
    private final Deque<HtClause> clauses = new ArrayDeque<>();

    /**
     * @return
     */
    public Deque<HtClause> getQueue() {
        return clauses;
    }

    /**
     * @return
     */
    public Deque<HtClause> getTaskQueue() {
        return null;
    }

    /**
     * @param item
     */
    public void push(HtClause item) {
        clauses.push(item);
    }

    /**
     * @return
     */
    public HtClause poll () {
        return clauses.poll();
    }

    public void toString0 ( StringBuilder sb ) {

    }
}
