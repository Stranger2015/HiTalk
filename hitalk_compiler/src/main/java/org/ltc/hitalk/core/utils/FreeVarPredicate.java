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
package org.ltc.hitalk.core.utils;

import com.thesett.common.util.logic.UnaryPredicate;
import org.ltc.hitalk.term.HtVariable;
import org.ltc.hitalk.term.ITerm;

/**
 * FunctorTermPredicate is a unary predicate over logical terms that picks out all terms that are free variables.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Determines whether a term is a free variable or not.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class FreeVarPredicate implements UnaryPredicate <ITerm> {

    /**
     * Determine whether a term is a free variable.
     *
     * @param term The object to test for predicate membership.
     * @return <tt>true</tt> if the term is a free variable, <tt>false</tt> otherwise.
     */
    public boolean evaluate ( ITerm term ) {
        if (term.isVar() && (term instanceof HtVariable)) {
            HtVariable var = (HtVariable) term;

            return !var.isBound();
        }

        return false;
    }
}
