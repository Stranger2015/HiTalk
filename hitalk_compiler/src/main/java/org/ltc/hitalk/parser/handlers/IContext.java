package org.ltc.hitalk.parser.handlers;

public interface IContext<T extends ParserStateHandler> {

    /**
     * @return
     */
    IContext<T> getParentContext();
}
