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
package org.ltc.hitalk.compiler.bktables;

import org.ltc.hitalk.term.HlOperator;

import java.util.EnumMap;

import static org.ltc.hitalk.term.HlOperator.Fixity;

/**
 * OperatorTable maintains a table of dynamically defined operators. Implementations of this table may provide their own
 * local rules, as to the range of priorities, associativities and degree of operator overloading that is permitted. Due
 * to the possibility of operator overloading, when a query against an operator name is made, multiple possible
 * operators may be returned.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Add new operators to the table.
 * <tr><td> Find all operators matching a given name.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface PlOperatorTable {
    /**
     * Sets the priority and associativity of a named operator in this table. This method may be used to remove
     * operators by some implementations, through a special setting of the priority value.
     *
     * @param operator         The interned name of the operator as a functor.
     */
    void setOperator ( HlOperator operator );

    /**
     * Checks the operator table for all possible operators matching a given name.
     *
     * @param name The name of the operator to find.
     * @return An array of matching operators, or <tt>null</tt> if none can be found.
     */
    EnumMap <Fixity, HlOperator> getOperatorsMatchingNameByFixity ( String name );

    HlOperator convert ( String name );
}
