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
    package org.ltc.hitalk.wam.compiler.prolog;

    import com.thesett.common.util.logic.UnaryPredicate;
    import org.ltc.hitalk.term.ITerm;

    /**
     * FunctorTermPredicate is a unary predicate that picks out all terms that are functors.
     *
     * <pre><p/><table id="crc"><caption>CRC Card</caption>
     * <tr><th> Responsibilities <th> Collaborations
     * <tr><td> Match terms that are functors. <td> {@link ITerm}.
     * </table></pre>
     *
     * @author Rupert Smith
     */

    public class HtFunctorTermPredicate implements UnaryPredicate <ITerm> {

        /**
         * Determine whether a term is a functor.
         *
         * @param term The term to examine.
         * @return <tt>true</tt> if the term is a functor, <tt>false</tt> if it is not.
         */
        public boolean evaluate ( ITerm term ) {
            return term.isFunctor();
        }
    }
