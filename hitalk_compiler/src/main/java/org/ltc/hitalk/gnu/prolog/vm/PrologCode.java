/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA. The text of license can be also found
 * at http://www.gnu.org/copyleft/lgpl.html
 */
package org.ltc.hitalk.gnu.prolog.vm;

import org.ltc.hitalk.gnu.prolog.term.Term;
import org.ltc.hitalk.gnu.prolog.vm.builtins.imphooks.Predicate_halt;

/**
 * Implementing classes can be executed and return a return code of
 *
 * {@link Predicate_halt Predicate_halt} can
 */
public interface PrologCode extends Installable
{
	/**
	 * predicate was returned with success, backtrack info was created, and
	 * re-execute is possible.
	 */
	enum Result {
		SUCCESS(0),
		/** predicate was returned with success, backtrack info was not created */
		SUCCESS_LAST(1),
		/** predicate failed */
		FAIL(-1),
		/**
		 * returned by the interpreter when it was halted, should never be returned by
		 * prolog code
		 */
		HALT(-2);

		private final int result;

		Result(int result) {
			this.result = result;
		}

		public int getResult() {
			return result;
		}
	}

	/**
	 * this method is used for execution of code
	 * 
	 * @param interpreter
	 *          interpreter in which context code is executed
	 * @param backtrackMode
	 *          true if predicate is called on backtracking and false otherwise
	 * @param args
	 *          arguments of code
	 * @return either SUCCESS, SUCCESS_LAST, or FAIL.
	 * @throws PrologException
	 */
	Result execute(Interpreter interpreter, boolean backtrackMode, Term... args)
            throws Exception;

}
