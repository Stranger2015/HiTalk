package org.ltc.hitalk.wam.transformers;


import com.thesett.aima.logic.fol.Term;

/**
 * Doing-no-transforming transformer.
 * it is needed to support transformation subsystem when no optimizing transformers are set.
 * <p>
 * Created by Anthony on 28.06.2015.
 */
public
class ZeroTransformer<T extends Term> extends DefaultTransformer <T> {
//       private static final Logger logger = LoggerFactory.getLogger( ZeroTransformer.class.getName() );

    /**
     *
     */
    public
    ZeroTransformer () {
        super(null);
    }

    /**
     *
     */
    @Override
    public
    void reset () {
    }

    @Override
    public
    ITransformer <T> getTransformer () {
        return this;
    }

    /**
     *
     */
    @Override
    public
    void message () {
//        logger.info("Default transformation");
    }

//    @Override
//    protected
//    List <Term> execute0 () {
//        return null;
//    }

    /**
     * Always returns true for this transformer.
     *
     * @param max
     * @return
     */
    @Override
    public
    boolean isAcceptable ( IMetrics max ) {
        return true;
    }

    @Override
    public
    TransformInfo getBestSoFarResult () {
        return null;
    }

}
