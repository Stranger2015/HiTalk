package org.ltc.hitalk.wam.compiler.prolog;

import com.thesett.common.parsing.SourceCodeException;

public interface ICompilerObserver<T, Q> {
    /**
     * Accepts notification of the completion of the compilation of a sentence into a (binary) form.
     *
     * @param sentence The compiled form of the sentence.
     * @throws SourceCodeException If there is an error in the compiled code that prevents its further processing.
     */
    void onCompilation ( T sentence ) throws SourceCodeException;

    /**
     * Accepts notification of the completion of the compilation of a query into binary form.
     *
     * @param sentence The compiled query.
     * @throws SourceCodeException If there is an error in the compiled code that prevents its further processing.
     */
    void onQueryCompilation ( Q sentence ) throws SourceCodeException;
}
