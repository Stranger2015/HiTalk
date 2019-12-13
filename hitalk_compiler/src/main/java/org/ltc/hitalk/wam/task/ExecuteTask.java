package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.core.IPreCompiler;
import org.ltc.hitalk.parser.Directive;
import org.ltc.hitalk.term.ITerm;

import java.util.List;
import java.util.function.Function;

public class ExecuteTask extends CompilerTask {

    public ExecuteTask ( IPreCompiler preCompiler,
                         Function <ITerm, List <ITerm>> action,
                         Directive.DirectiveKind kind ) {
        super(preCompiler, action, kind);
    }
}

