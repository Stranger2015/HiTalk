package org.ltc.hitalk.parser;

public class HtLinkageException extends HtSourceCodeException {
    /**
     * Builds an exception with a message and a user message and a message key. To create an exception with no key and
     * just a user message or with no user message at all pass in null arguments for the key or user message.
     *
     * @param message     The exception message.
     * @param cause       The wrapped exception underlying this one.
     * @param key         A key to look up user readable messages with.
     * @param userMessage The user readable message or data string.
     * @param position    The position of the error.
     */
    public HtLinkageException ( String message,
                                Throwable cause,
                                String key,
                                String userMessage,
                                ISourceCodePosition position ) {
        super(message, cause, key, userMessage, position);
    }
}
