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
package org.ltc.hitalk.gnu.prolog.io;

import org.ltc.hitalk.gnu.prolog.io.parser.gen.TermParser;
import org.ltc.hitalk.gnu.prolog.io.parser.gen.TokenMgrError;
import org.ltc.hitalk.gnu.prolog.term.Term;
import org.ltc.hitalk.gnu.prolog.vm.Environment;

import java.io.FilterReader;
import java.io.Reader;
import java.io.StringReader;

/**
 * Reads {@link Term Terms} from strings and {@link Reader Readers}.
 * 
 */
public class TermReader extends FilterReader
{
	protected static final OperatorSet defaultOperatorSet = new OperatorSet();

	TermParser parser;

	public TermReader(Reader r, int line, int col, Environment environment)
	{
		super(r);
		parser = new TermParser(r, line, col, environment);
	}

	public TermReader(Reader r, Environment environment)
	{
		this(r, 1, 1, environment);
	}

	public Term readTerm(ReadOptions options) throws Exception {
		try
		{
			return parser.readTerm(options);
		} catch (TokenMgrError ex)
		{
			throw new ParseException(ex);
		}
	}

	public static Term stringToTerm(ReadOptions options, String str, Environment environment) throws Exception {
		StringReader srd = new StringReader(str);
		TermReader trd = new TermReader(srd, environment);
		return trd.readTermEof(options);
	}

	public static Term stringToTerm(String str, Environment environment) throws Exception
	{
		StringReader srd = new StringReader(str);
		TermReader trd = new TermReader(srd, environment);
		return trd.readTermEof();
	}

	public Term readTermEof(ReadOptions options) throws Exception {
		try
		{
			return parser.readTermEof(options);
		} catch (TokenMgrError ex)
		{
			throw new ParseException(ex);
		}
	}

	public Term readTerm(OperatorSet set) throws Exception {
		ReadOptions options = new ReadOptions(set);
		return readTerm(options);
	}

	public Term readTermEof(OperatorSet set) throws Exception {
		ReadOptions options = new ReadOptions(set);
		return readTermEof(options);
	}

	public Term readTerm() throws Exception {
		return readTerm(defaultOperatorSet);
	}

	public Term readTermEof() throws Exception {
		return readTermEof(defaultOperatorSet);
	}

	public int getCurrentLine()
	{
		return parser.getCurrentLine();
	}

	public int getCurrentColumn()
	{
		return parser.getCurrentColumn();
	}

}
