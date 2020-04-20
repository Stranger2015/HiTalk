package org.ltc.hitalk.wam.compiler;

import org.ltc.hitalk.core.IQueueHolder;
import org.ltc.hitalk.parser.HtClause;
import org.ltc.hitalk.wam.task.PreCompilerTask;

public interface IPendingTasks extends IQueueHolder<PreCompilerTask<HtClause>> {
}
