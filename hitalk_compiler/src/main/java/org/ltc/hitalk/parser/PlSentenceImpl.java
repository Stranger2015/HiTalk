package org.ltc.hitalk.parser;

import com.thesett.aima.logic.fol.Sentence;
import org.ltc.hitalk.parser.jp.segfault.prolog.parser.ISentence;
import org.ltc.hitalk.term.ITerm;

/**
 * PlSentenceImpl provides a simple implementation of {@link Sentence} for packaging logic terms as sentences in some
 * logical language.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Capture some sub-type of Term as a sentence. <td> {@link ITerm}
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class PlSentenceImpl implements ISentence <ITerm> {

    /**
     * The term to present as a sentence.
     */
    ITerm term;

    /**
     * Creates a new sentence from a term.
     *
     * @param term The term to capture as a sentence.
     */
    public PlSentenceImpl ( ITerm term ) {
        this.term = term;
    }

    /**
     * Provides the term captured by this sentence.
     *
     * @return The term captured by this sentence.
     */
    public ITerm getT () {
        return term;
    }
}
