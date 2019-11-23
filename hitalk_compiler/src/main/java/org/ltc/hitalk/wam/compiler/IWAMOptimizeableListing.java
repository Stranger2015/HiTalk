package org.ltc.hitalk.wam.compiler;

import com.thesett.common.util.SizeableList;

import java.util.List;


/**
 * WAMOptimizeableListing provides an instruction listing, and allows that instruction listing to be replaced with a
 * more optimized version.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> List the WAM instructions. </td></tr>
 * <tr><td> Allow the instructions to be replaced with a more optimized instruction listing. </td></tr>
 * <tr><td> List the original unoptimized instructions. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
interface IWAMOptimizeableListing {

    /**
     * Provides the compiled byte code instructions as an unmodifiable list.
     *
     * @return A list of the byte code instructions for this query.
     */
    List <HiTalkWAMInstruction> getInstructions ();

    /**
     * Replaces the instruction listing with a more optimized listing.
     *
     * @param instructions An optimized instruction listing.
     */
    void setOptimizedInstructions ( SizeableList <HiTalkWAMInstruction> instructions );

    /**
     * Provides the original unoptimized instruction listing, after the optimization replacement.
     *
     * @return The original unoptimized instruction listing.
     */
    List <HiTalkWAMInstruction> getUnoptimizedInstructions ();
}
