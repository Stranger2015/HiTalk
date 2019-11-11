package org.ltc.hitalk.wam.compiler;

import com.thesett.common.util.SizeableList;
import org.ltc.hitalk.entities.HtEntityIdentifier;
import org.ltc.hitalk.entities.HtPredicateDefinition.UserDefinition;
import org.ltc.hitalk.parser.HtClause;

import java.util.Arrays;

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

/**
 * HiTalkWAMCompiledClause is a clause, that belongs to an {@link HiTalkWAMCompiledPredicate}.
 * Compiled instructions added to the clause fall through onto the parent predicate, and are not held
 * on the compiled clause itself. The compiled clause has a label, which identifies its position within
 * the parent predicate.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Forward added instructions onto the parent predicate.
 * <tr><td> Hold a label as a position marker for the clause within its containing predicate.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public
class HiTalkWAMCompiledClause extends HtClause {
    /**
     * The parent predicate to which this compiled clause belongs.
     */
    private final HiTalkWAMCompiledPredicate parent;

    /**
     * The interned name of this clauses label.
     */
    int label;

    /**
     * Indicates that this has been added to a parent predicate already.
     */
    private boolean addedToParent;

//    /**?????
//     * Creates a clause within a parent predicate.
//     *
//     * @param parent The parent predicate.
//     */
//    public HiTalkWAMCompiledClause ( HiTalkWAMCompiledPredicate parent ) {
//        super(null, null, new Functor[0]);
//        this.parent = parent;
//    }

    /**
     * add dotted pair
     *
     * @param head
     * @param body
     * @param identifier
     * @param parent
     */
    public HiTalkWAMCompiledClause ( IFunctor head,
                                     IFunctor[] body,
                                     HtEntityIdentifier identifier,
                                     HiTalkWAMCompiledPredicate parent ) {
        super(identifier, head, body);
        this.parent = parent;
    }

    /**
     * Adds a conjunctive body functor, or head functor, to this clause, along with the instructions that implement it.
     *
     * @param body         A conjunctive body functor to add to this clause.
     * @param instructions A list of instructions to add to the body.
     */
    public void addInstructions ( HtFunctor body, SizeableList <HiTalkWAMInstruction> instructions ) {
        int oldLength;

        if (this.body == null) {
            oldLength = 0;
            this.body = new HtFunctor[1];
        } else {
            oldLength = this.body.length;
            this.body = Arrays.copyOf(this.body, oldLength + 1);
        }

        this.body[oldLength] = body;

        addInstructionsAndThisToParent(instructions);
    }

    /**
     * Adds some instructions sequentially, after any existing instructions, to the clause.
     *
     * @param instructions The instructions to add to the clause.
     */
    public void addInstructions ( SizeableList <HiTalkWAMInstruction> instructions ) {
        addInstructionsAndThisToParent(instructions);
    }

    /**
     * Adds some instructions to the parent predicate, and also adds this as a clause on the parent, if it has not
     * already been added.
     *
     * @param instructions The instructions to add.
     */
    private void addInstructionsAndThisToParent ( SizeableList <HiTalkWAMInstruction> instructions ) {
        if (!addedToParent) {
            parent.addInstructions(new UserDefinition <>(this), instructions);
            addedToParent = true;
        } else {
            parent.addInstructions(instructions);
        }
    }
}

