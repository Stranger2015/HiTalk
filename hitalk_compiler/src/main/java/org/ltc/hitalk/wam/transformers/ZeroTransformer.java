package org.ltc.hitalk.wam.transformers;


import org.ltc.hitalk.entities.context.IMetrics;

/**
 * Doing-nothing transformer.
 * it is needed to support transformation subsystem when no optimizing transformers are set.
 * <p>
 * Created by Anthony on 28.06.2015.
 */
public
class ZeroTransformer extends DefaultTransformer {
//       private static final Logger logger = LoggerFactory.getLogger( ZeroTransformer.class.getName() );

    /**
     *
     */
    public ZeroTransformer () {
        super();
    }
//
//    /**
//     *
//     */
//    @Override
//    public
//    void reset () {
//    }

//    /**
//     *
//     */
//    @Override
//    public
//    void message () {
////        logger.info("Default transformation");
//    }

    /**
     * Always returns true for this transformer.
     *
     * @param max
     * @return
     */
    @Override
    public boolean isAcceptable ( IMetrics max ) {
        return true;
    }

////    @Override
//    public
//    TransformInfo getBestSoFarResult () {
//        return new TransformInfo(/*ExecutionContext context,
//                           ExecutionInfo executionInfo,
//                           IMetrics delta,
//                           ITerm input,
//                           List <ITerm> output*/);
//    }
}
