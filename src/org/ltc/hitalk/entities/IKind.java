package org.ltc.hitalk.entities;

/**
 * @param <E>
 */
public
interface IKind<E extends Enum <E>> {
    /**
     * @return
     */
    boolean isAbstract ();

    /**
     * @return
     */
    E getParent ();
}
