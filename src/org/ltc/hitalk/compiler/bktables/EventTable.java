package org.ltc.hitalk.compiler.bktables;

/**
 * @param <T>
 */
public
class EventTable<T extends HtEvent> extends BookKeepingTable <T> {
    final BookKeepingTable before = new BookKeepingTable();
    final BookKeepingTable after = new BookKeepingTable();

    /**
     *
     */
    public
    EventTable () {
    }

    /**
     * @param event
     */
    public
    void addBefore ( HtEvent event ) {

    }


}
