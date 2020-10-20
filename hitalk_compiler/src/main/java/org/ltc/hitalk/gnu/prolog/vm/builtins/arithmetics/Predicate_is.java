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
package org.ltc.hitalk.gnu.prolog.vm.builtins.arithmetics;

import org.ltc.hitalk.gnu.prolog.term.Term;
import org.ltc.hitalk.gnu.prolog.vm.Evaluate;
import org.ltc.hitalk.gnu.prolog.vm.ExecuteOnlyCode;
import org.ltc.hitalk.gnu.prolog.vm.Interpreter;
import org.ltc.hitalk.gnu.prolog.vm.PrologException;

/**
 * prolog code
 */
public class Predicate_is extends ExecuteOnlyCode
{

	@Override
	public Result execute(Interpreter interpreter, boolean backtrackMode, Term[] args)
			throws PrologException
	{
		return interpreter.unify(args[0], Evaluate.evaluate(args[1]));
	}
}
