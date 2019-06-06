package com.dledmonds.slack;

import com.github.seratch.jslack.api.model.Message;

/**
 * @author dledmonds
 */
public interface MessageProcessor {

    void seenMessage(String channel, Message message);

}
