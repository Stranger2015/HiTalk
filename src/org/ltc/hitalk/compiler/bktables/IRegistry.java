package org.ltc.hitalk.compiler.bktables;

/**
 *
 */
public
interface IRegistry {
    /**
     * @param clazz
     * @return
     */
    boolean isRegistered ( Class <? extends IIdentifiable> clazz );

    /**
     * @param iIdentifiable
     * @return
     */
    IIdentifiable register ( IIdentifiable iIdentifiable );

    /**
     * @param iIdentifiable
     * @return
     */
    default
    IIdentifiable lookup ( IIdentifiable iIdentifiable ) {
        if (!isRegistered(iIdentifiable.getClass())) {
            return register(iIdentifiable);
        }
        return iIdentifiable.newInstance();
    }

    /**
     * @param id
     * @return
     */
    IIdentifiable getById ( int id );
}