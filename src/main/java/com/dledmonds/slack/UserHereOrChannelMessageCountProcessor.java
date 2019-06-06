package com.dledmonds.slack;

import com.github.seratch.jslack.api.model.Message;

public class UserHereOrChannelMessageCountProcessor extends UserMessageCountProcessor {

    protected UserHereOrChannelMessageCountProcessor(int limit) {
        super(limit);
        title = "User @here/@channel Messages Count";
    }

    @Override
    protected boolean filterMessage(Message message) {
        boolean matchKeywords = message.getText().contains("<!here>") || message.getText().contains("<!channel>");
        return !matchKeywords;
    }

}
