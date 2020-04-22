package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IHitalkObject;
import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive.DirectiveKind;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.parser.PlLexer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;

/**
 *
 */
public class InstructCompilerTask<T extends HtClause> extends PreCompilerTask<T> implements IHitalkObject {
    private final Deque<T> clauses = new ArrayDeque<>();

    /**
     * @param preCompiler
     * @param tokenSource
     * @param kind
     */
    public InstructCompilerTask(IPreCompiler<T, PreCompilerTask<T>, ?, ?, ?, ?> preCompiler, PlLexer tokenSource, EnumSet<
            DirectiveKind> kind) {
        super(preCompiler, tokenSource, kind);
    }

    /**
     * @return
     */
    public Deque<T> getClausesQueue() {
        return clauses;
    }

    /**
     * @param item
     */
    public void push(T item) {
        clauses.push(item);
    }


    public void toString0 ( StringBuilder sb ) {

    }
}
