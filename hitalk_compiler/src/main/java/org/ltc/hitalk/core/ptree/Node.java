package org.ltc.hitalk.core.ptree;

/**
 *
 */
public
class Node {
    private State state;

    public
    void setState ( State state ) {
        this.state = state;
    }

    public
    State getState () {
        return state;
    }


    public
    enum State {
        INACTIVE,
        ACTIVE,
        COMPLETE,
        PROCESSED,
        ABRUPTED,
        REMOVED,
        STATE,
    }

}
