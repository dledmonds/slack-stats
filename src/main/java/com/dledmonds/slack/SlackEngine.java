package com.dledmonds.slack;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.methods.SlackApiException;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsInfoRequest;
import com.github.seratch.jslack.api.methods.request.conversations.ConversationsListRequest;
import com.github.seratch.jslack.api.methods.request.users.UsersListRequest;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsInfoResponse;
import com.github.seratch.jslack.api.methods.response.conversations.ConversationsListResponse;
import com.github.seratch.jslack.api.methods.response.users.UsersListResponse;
import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.ConversationType;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author dledmonds
 */
public class SlackEngine implements Runnable {

    private Slack slack;
    private String token;
    private List<String> allowedChannels;
    private List<ChannelProcessor> channelProcessors;
    private List<MessageProcessor> messageProcessors;
    private List<UserProcessor> userProcessors;

    SlackEngine(String token, String ... channelIds) {
        this.token = token;
        this.allowedChannels = Arrays.asList(channelIds);
        this.slack = Slack.getInstance();
        this.channelProcessors = new ArrayList<>();
        this.messageProcessors = new ArrayList<>();
        this.userProcessors = new ArrayList<>();
    }

    void addChannelProcessor(ChannelProcessor channelProcessor) {
        channelProcessors.add(channelProcessor);
    }

    void addMessageProcessor(MessageProcessor messageProcessor) {
        messageProcessors.add(messageProcessor);
    }

    void addUserProcessor(UserProcessor userProcessor) {
        userProcessors.add(userProcessor);
    }

    public void run() {
        try {
            getUsers();
            getChannels(); // calls getMessages as it loops
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    private void getChannels() throws IOException, SlackApiException {
        boolean keepGoing = true;
        String nextCursor = null;

        List<Conversation> allChannels = new ArrayList<>();
        if (!allowedChannels.isEmpty()) {
            for (String channelId : allowedChannels) {
                ConversationsInfoRequest ciReq = ConversationsInfoRequest.builder().token(token).channel(channelId).build();
                ConversationsInfoResponse ciResp = slack.methods().conversationsInfo(ciReq);
                allChannels.add(ciResp.getChannel());
            }
        } else {
            ConversationsListRequest clReq = ConversationsListRequest.builder().token(token).types(Arrays.asList(ConversationType.PUBLIC_CHANNEL)).build();
            while (keepGoing) {
                clReq.setCursor(nextCursor);
                ConversationsListResponse clResp = slack.methods().conversationsList(clReq);
                System.out.println("Adding " + clResp.getChannels().size() + " channels");
                allChannels.addAll(clResp.getChannels());

                nextCursor = clResp.getResponseMetadata().getNextCursor();
                if (nextCursor == null || nextCursor.isEmpty()) keepGoing = false;
            }
        }
        System.out.println("Got " + allChannels.size() + " channels");
        // sort by name
        allChannels.sort((Conversation c1, Conversation c2) -> c1.getName().compareTo(c2.getName()));

        for (Conversation conversation : allChannels) {
            if (!conversation.isChannel() && !allowedChannels.contains(conversation.getId())) continue; // only process channels
            for (ChannelProcessor cp : channelProcessors) {
                cp.seenChannel(conversation);
            }

            getMessages(conversation.getId());
        }
    }

    private void getUsers() throws IOException, SlackApiException {
        boolean keepGoing = true;
        String nextCursor = null;

        List<User> allUsers = new ArrayList<>();
        UsersListRequest uReq = UsersListRequest.builder().token(token).build();
        while (keepGoing) {
            uReq.setCursor(nextCursor);
            UsersListResponse uResp = slack.methods().usersList(uReq);

            System.out.println("Adding " + uResp.getMembers().size() + " users");
            allUsers.addAll(uResp.getMembers());

            nextCursor = uResp.getResponseMetadata() == null ? null : uResp.getResponseMetadata().getNextCursor();
            if (nextCursor == null || nextCursor.isEmpty()) keepGoing = false;
        }
        System.out.println("Got " + allUsers.size() + " users");
        // alphabetical sort
        allUsers.sort((User u1, User u2) -> u1.getName().compareTo(u2.getName()));

        for (User user : allUsers) {
            for (UserProcessor up : userProcessors) {
                up.seenUser(user);
            }
        }
    }

    private void getMessages(String channel) throws IOException, SlackApiException {
        boolean keepGoing = true;
        String nextCursor = null;
        long loopCount = 0;

        List<Message> allMessages = new ArrayList<>();
        ConversationsHistoryRequest chReq = ConversationsHistoryRequest.builder().token(token).channel(channel).build();
        while (keepGoing) {
            chReq.setCursor(nextCursor);
            ConversationsHistoryResponse chResp = slack.methods().conversationsHistory(chReq);

            System.out.println("Adding "+ (++loopCount) + "-" + chResp.getMessages().size() + " messages");
            allMessages.addAll(chResp.getMessages());

            nextCursor = chResp.getResponseMetadata() == null ? null : chResp.getResponseMetadata().getNextCursor();
            if (nextCursor == null || nextCursor.isEmpty()) keepGoing = false;

            //if (allMessages.size() >= 500) keepGoing = false;
        }
        //if (allMessages.size() < 500) return;

        System.out.println("Got " + allMessages.size() + " messages for " + channel);
        // sort by timestamp
        allMessages.sort((Message m1, Message m2) -> m1.getTs().compareTo(m2.getTs()));

        for (Message message : allMessages) {
            if (message.getUser() == null) continue; // only process messages sent by users/bots
            for (MessageProcessor mp : messageProcessors) {
                mp.seenMessage(channel, message);
            }
        }
    }

    private void pauseFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            // ignore this
        }
    }

}
