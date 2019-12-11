package org.ltc.hitalk.wam.task;

import org.ltc.hitalk.term.ITerm;
import org.ltc.hitalk.wam.transformers.ITransformer;

import java.util.List;
import java.util.function.Function;

/**
 * @param <T>
 */
public class HitalkStandardPreprocessor<T extends ITerm> extends PrologStandardPreprocessor <T> {
    /**
     * @param action
     * @param target
     * @param transformer
     */
    public HitalkStandardPreprocessor ( Function <T, List <T>> action, List <T> target, ITransformer <T> transformer ) {
        super(action, target, transformer);
    }
}
