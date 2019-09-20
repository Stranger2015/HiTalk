package org.ltc.hitalk.entities;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import org.ltc.hitalk.compiler.bktables.INameable;

/**
 * @param
 */
public
class HtEvent implements INameable <Functor> {
    private final Functor name;
    private final Term obj;
    private final Term sender;
    private final Term message;
    private final HtEntityIdentifier monitor;
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
    HtEvent ( Functor name, Term obj, Term sender, Term message, HtEntityIdentifier monitor, Term call ) {
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
    Functor getName () {
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
    HtEntityIdentifier getMonitor () {
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
