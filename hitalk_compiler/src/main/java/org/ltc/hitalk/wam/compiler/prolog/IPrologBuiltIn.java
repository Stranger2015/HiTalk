package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.common.util.SizeableLinkedList;
import org.ltc.hitalk.wam.compiler.HtFunctorName;
import org.ltc.hitalk.wam.compiler.IFunctor;
import org.ltc.hitalk.wam.compiler.hitalk.HiTalkWAMInstruction;


/**
 * HiTalkBuiltIn defines the behaviour of Prolog built-in predicates.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Generate instructions to set up the arguments to a call to a built-in functor.</td></tr>
 * <tr><td> Generate instructions to call to a built-in functor.</td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
interface IPrologBuiltIn {

    /**
     * Compiles the arguments to a call to a body of a clause into an instruction listing in WAM.
     * <p>
     * <p/>The name of the clause containing the body, and the position of the body within this clause are passed as
     * arguments, mainly so that these coordinates can be used to help make any labels generated within the generated
     * code unique.
     *
     * @param expression  The clause body to compile.
     * @param isFirstBody <tt>true</tt> iff this is the first body of a program clause.
     * @param clauseName  The name of the clause within which this body appears.
     * @param bodyNumber  The body position within the containing clause.
     * @return A listing of the instructions for the clause body in the WAM instruction set.
     */
    SizeableLinkedList<HiTalkWAMInstruction> compileBodyArguments(IFunctor expression,
                                                                  boolean isFirstBody,
                                                                  HtFunctorName clauseName,
                                                                  int bodyNumber) throws Exception;

    /**
     * Compiles a call to a body of a clause into an instruction listing in WAM.
     *
     * @param expression        The body functor to call.
     * @param isFirstBody       Iff this is the first body in a clause.
     * @param isLastBody        Iff this is the last body in a clause.
     * @param chainRule         Iff the clause is a chain rule, so has no BaseApp frame.
     * @param permVarsRemaining The number of permanent variables remaining at this point in the calling clause. Used
     *                          for BaseApp trimming.
     * @return A list of instructions for the body call.
     */
    SizeableLinkedList<HiTalkWAMInstruction> compileBodyCall(IFunctor expression,
                                                             boolean isFirstBody,
                                                             boolean isLastBody,
                                                             boolean chainRule,
                                                             int permVarsRemaining) throws Exception;
}
