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
package org.ltc.hitalk.wam.compiler.hitalk;

import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.common.parsing.SourceCodeException;
import org.ltc.hitalk.core.IHitalkObject;

/**
 * A compiler over any language that has a one-to-one mapping from some input form, to some output form. Compiles from
 * the source representation, S, to the target compiled representation, T.
 * <p>
 * <p/>The source form may not have the same scope as the target form. For example, the source form might be single
 * lines of code, but the target form might be complete functions. The {@link #endScope()} method is used to signal the
 * completion of some input scope, that triggers the completion of the compilation of its contents. A
 * {@link LogicCompilerObserver} must be set on the compiler, and will be notified upon the compilation of any target
 * forms.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Transform one language representation into another (binary) representation.
 * <tr><td> Accept notification of end of scope to complete compilation.
 * <tr><td> Accept observers to notify of all compiled representation generated.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface ILogicCompiler<T, P, Q> extends IHitalkObject {
    /**
     * Compiles a sentence into a (presumably binary) form, that provides a Java interface into the compiled structure.
     *
     * @param sentence The sentence to compile.
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    void compile ( Sentence <T> sentence ) throws SourceCodeException;

    /**
     * Establishes an observer on the compiled forms that the compiler outputs.
     *
     * @param observer The compiler output observer.
     */
    void setCompilerObserver ( LogicCompilerObserver <P, Q> observer );

    /**
     * Signal the end of a compilation scope, to trigger completion of the compilation of its contents.
     *
     * @throws SourceCodeException If there is an error in the source to be compiled that prevents its compilation.
     */
    void endScope () throws SourceCodeException;
}
