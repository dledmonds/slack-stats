package com.dledmonds.slack;

import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author dledmonds
 */
public class UserMessageCountProcessor implements ChannelProcessor, MessageProcessor, UserProcessor {

    private final static String ALL_CHANNELS = "@All-Channels@";

    protected String title = "User Messages Count";
    protected Map<String, Conversation> channelMap = new HashMap<>();
    protected Map<String, User> userMap = new HashMap<>();
    protected Map<String, UserMessageCount> userMessageCountMap = new HashMap<>();
    protected int limit;

    protected UserMessageCountProcessor(int limit) {
        this.limit = limit;
        userMessageCountMap.put(ALL_CHANNELS, new UserMessageCount(ALL_CHANNELS));
    }

    protected boolean filterMessage(Message message) {
        return false;
    }

    @Override
    public void seenChannel(Conversation conversation) {
        channelMap.put(conversation.getId(), conversation);
    }

    @Override
    public void seenUser(User user) {
        userMap.put(user.getId(), user);
    }

    @Override
    public void seenMessage(String channel, Message message) {
        User user = userMap.get(message.getUser());
        if (user == null) {
            System.err.println("Cannot find user " + message.getUser());
            return;
        }

        if (filterMessage(message)) return;

        // store details per channel
        UserMessageCount umc = userMessageCountMap.get(channel);
        if (umc == null) {
            umc = new UserMessageCount(channel);
            userMessageCountMap.put(channel, umc);
        }
        umc.seenMessageForUser(message.getUser());

        // store detail summary for all channels
        umc = userMessageCountMap.get(ALL_CHANNELS);
        umc.seenMessageForUser(message.getUser());
    }

    public void outputResults(OutputStream out) throws IOException {
        PrintStream ps = null;
        if (out instanceof PrintStream) {
            ps = (PrintStream)out;
        } else {
            ps = new PrintStream(out);
        }

        for (Entry<String, UserMessageCount> entry : userMessageCountMap.entrySet()) {
            entry.getValue().outputResults(ps);
        }
    }

    class UserMessageCount {
        private String channelId;
        private Map<String, Long> userMessagesMap = new HashMap<>();

        UserMessageCount(String channelId) {
            this.channelId = channelId;
        }

        void seenMessageForUser(String username) {
            Long count = userMessagesMap.get(username);
            if (count == null) count = new Long(0);
            userMessagesMap.put(username, ++count);
        }

        void outputResults(PrintStream ps) {
            boolean order = false; // for asc or desc
            List<Entry<String, Long>> list = new LinkedList<>(userMessagesMap.entrySet());
            list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                    ? o1.getKey().compareTo(o2.getKey())
                    : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                    ? o2.getKey().compareTo(o1.getKey())
                    : o2.getValue().compareTo(o1.getValue()));
            Map<String, Long> sortedMap = list.stream().collect(
                    Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));

            ps.println();
            ps.println(title + " - " +
                    (channelId.equals(ALL_CHANNELS) ? "All Channels" : channelMap.get(channelId).getName()));
            ps.println("----------------------------------------");
            int loop = 0;
            for (Entry<String, Long> entry : sortedMap.entrySet()) {
                ps.println(entry.getValue()
                        + "," + userMap.get(entry.getKey()).getName()
                        + "," + userMap.get(entry.getKey()).getRealName());
                if (++loop >= limit) return;
            }
            /*
            sortedMap.forEach((key, value) -> System.out.println(value
                    + "," + userMap.get(key).getName()
                    + "," + userMap.get(key).getRealName()));
             */
        }
    }
}
