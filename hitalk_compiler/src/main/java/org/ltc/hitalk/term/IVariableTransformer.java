package org.ltc.hitalk.term;

import java.util.List;

/**
 *
 */
public interface IVariableTransformer extends ITermTransformer {
    /**
     * @param variable
     * @return
     */
    List <ITerm> transform ( HtVariable variable );
}
