package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.FunctorTraverser;
import com.thesett.aima.logic.fol.compiler.PositionalContext;
import org.ltc.hitalk.compiler.HtClauseTraverser;

public
interface HtPositionalTermTraverser extends HtClauseTraverser, FunctorTraverser, PositionalContext {
}
