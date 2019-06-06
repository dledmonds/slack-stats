package com.dledmonds.slack;

import com.github.seratch.jslack.api.model.Conversation;

/**
 * @author dledmonds
 */
public interface ChannelProcessor {

    void seenChannel(Conversation conversation);

}
