package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.INameable;

/**
 * @param <NT>
 */
public
class HtEvent<NT> implements INameable <NT> {
    private final NT name;
    private final Term obj;
    private final Term sender;
    private final Term message;
    private final HtEntity monitor;
    private final Term call;

    /**
     * @param name
     * @param obj
     * @param sender
     * @param message
     * @param monitor
     * @param call
     */
    public
    HtEvent ( NT name, Term obj, Term sender, Term message, HtEntity monitor, Term call ) {
        this.name = name;
        this.obj = obj;
        this.sender = sender;
        this.message = message;
        this.monitor = monitor;
        this.call = call;
    }

    /**
     * @return
     */
    @Override
    public
    NT getName () {
        return name;
    }

    /**
     * @return
     */
    public
    Term getObj () {
        return obj;
    }

    /**
     * @return
     */
    public
    Term getSender () {
        return sender;
    }

    /**
     * @return
     */
    public
    Term getMessage () {
        return message;
    }

    /**
     * @return
     */
    public
    HtEntity getMonitor () {
        return monitor;
    }

    /**
     * @return
     */
    public
    Term getCall () {
        return call;
    }
}
