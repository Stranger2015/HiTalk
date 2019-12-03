/*
 * Copyright The Sett Ltd, 2005 to 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ltc.hitalk.wam.compiler;

import com.thesett.aima.logic.fol.wam.optimizer.Matcher;
import com.thesett.aima.logic.fol.wam.optimizer.StateMachine;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.SizeableList;
import com.thesett.common.util.doublemaps.SymbolTable;
import org.ltc.hitalk.compiler.IVafInterner;

import java.util.List;


/**
 * WAMOptimizer performs peephole optimization on a list of WAM instructions.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Optimize a WAM instruction listing.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HiTalkWAMOptimizer implements IOptimizer {

    /**
     * The symbol table.
     */
    protected final SymbolTable <Integer, String, Object> symbolTable;

    /**
     * Holds the variable and functor name interner for the machine.
     */
    private final IVafInterner interner;

    /**
     * Builds a WAM instruction optimizer.
     *
     * @param symbolTable The symbol table to get instruction analysis information from.
     * @param interner    The variable and functor name interner for the machine.
     */
    public HiTalkWAMOptimizer ( SymbolTable <Integer, String, Object> symbolTable, IVafInterner interner ) {
        this.symbolTable = symbolTable;
        this.interner = interner;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <p/>Runs the instruction listing through an optimizer pass, and replaces the original instruction listing with
     * the optimized version.
     */
    @Override
    public <T extends IWAMOptimizeableListing> T apply ( T listing ) {
        SizeableList <HiTalkWAMInstruction> optListing = optimize(listing.getInstructions());
        listing.setOptimizedInstructions(optListing);

        return listing;
    }

    /**
     * Performs an optimization pass for specialized instructions.
     * <p>
     * <p/>The following instruction sequences can be optimized:
     *
     * <pre>
     * unify_var Xi, get_struc a/0, Xi -> unify_const a/0
     * get_struc a/0, Xi -> get_const a/0
     * put_struc a/0, Xi, set_var Xi -> set_const a/0
     * put_struc a/0, Xi -> put_const a/0
     * </pre>
     *
     * @param instructions The instructions to optimize.
     * @return An list of optimized instructions.
     */
    private SizeableList <HiTalkWAMInstruction> optimize ( List <HiTalkWAMInstruction> instructions ) {
        StateMachine <HiTalkWAMInstruction, HiTalkWAMInstruction> optimizeConstants =
                new HtOptimizeInstructions(symbolTable, interner);
        Iterable <HiTalkWAMInstruction> matcher =
                new Matcher <>(instructions.iterator(), optimizeConstants);

        SizeableList <HiTalkWAMInstruction> result = new SizeableLinkedList <>();

        for (HiTalkWAMInstruction instruction : matcher) {
            result.add(instruction);
        }

        return result;
    }

    public <P> P apply ( P result ) {
        return result;
    }
}
