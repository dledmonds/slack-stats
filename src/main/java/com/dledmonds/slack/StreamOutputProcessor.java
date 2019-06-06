package com.dledmonds.slack;

import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.File;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Date;

/**
 * @author dledmonds
 */
public class StreamOutputProcessor implements ChannelProcessor, MessageProcessor, UserProcessor {

    protected PrintStream ps;

    protected StreamOutputProcessor(OutputStream os) {
        if (os instanceof PrintStream) {
            this.ps = (PrintStream)os;
        } else {
            this.ps = new PrintStream(os);
        }
    }

    @Override
    public void seenChannel(Conversation conversation) {
        ps.print("CHANNEL: ");
        if (conversation.isArchived()) {
            ps.println(conversation.getName() + " is archived");
            return;
        }

        ps.print(conversation.getId() + " - " + conversation.getName());
        if (conversation.getPreviousNames() != null) {
            for (String prevName : conversation.getPreviousNames()) {
                ps.print(", " + prevName);
            }
        }
        ps.println(" - Users: " + conversation.getNumOfMembers());
    }

    @Override
    public void seenMessage(String channel, Message message) {
        ps.print("MESSAGE: ");
        Date ts = Utils.convertTsToDate(message.getTs());
        ps.print(ts + " - " + message.getClientMsgId() + "/" + message.getUser() + " - " + message.getType() + " - " + message.getText());
        if (message.getFiles() != null) {
            for (File file : message.getFiles()) {
                ps.print("; File = " + file.getName() + " - " + file.getPermalink());
            }
        }
        ps.println();
    }

    @Override
    public void seenUser(User user) {
        ps.print("USER: ");

        if (user.isDeleted()) {
            ps.println(user.getName() + " is deleted");
            return;
        }

        if (user.isBot()) {
            ps.println(user.getName() + " is a bot");
            return;
        }

        ps.println(user.getId() + " - " + user.getName());
    }

}
