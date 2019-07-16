package org.ltc.hitalk.wam.compiler.builtins;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.common.util.SizeableLinkedList;
import org.ltc.hitalk.wam.compiler.HiTalkDefaultBuiltIn;
import org.ltc.hitalk.wam.compiler.HiTalkWAMInstruction;

public
class Bypass extends HiTalkCall {
    /**
     * Creates the base built in on the specified functor.
     *
     * @param functor        The functor to create a built-in for @code {}/1.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    public
    Bypass ( Functor functor, HiTalkDefaultBuiltIn defaultBuiltIn ) {
        super(functor, defaultBuiltIn);
    }

    /**
     * Call implements the Prolog 'call' operator. Call resolves its argument as a query. The argument to call may be a
     * variable, but must be bound to a callable functor or atom at the time the call is made in order to be valid.
     *
     * <pre><p/><table id="crc"><caption>CRC Card</caption>
     * <tr><th> Responsibilities <th> Collaborations
     * <tr><td> Call a callable functor or atom as a query for resolution.
     * </table></pre>
     *
     * @param expression
     * @param isFirstBody
     * @param clauseName
     * @param bodyNumber
     * @author Rupert Smith
     */
    @Override
    public
    SizeableLinkedList <HiTalkWAMInstruction> compileBodyArguments (
            Functor expression,
            boolean isFirstBody,
            FunctorName clauseName,
            int bodyNumber
    ) {
        return super.compileBodyArguments(expression, isFirstBody, clauseName, bodyNumber);
    }

    /**
     * {@inheritDoc}
     *
     * @param expression
     * @param isFirstBody
     * @param isLastBody
     * @param chainRule
     * @param permVarsRemaining
     */
    @Override
    public
    SizeableLinkedList <HiTalkWAMInstruction> compileBodyCall (
            Functor expression,
            boolean isFirstBody,
            boolean isLastBody,
            boolean chainRule,
            int permVarsRemaining ) {
        return super.compileBodyCall(expression, isFirstBody, isLastBody, chainRule, permVarsRemaining);
    }
}
