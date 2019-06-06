package com.dledmonds.slack;

import com.github.seratch.jslack.api.model.Conversation;
import com.github.seratch.jslack.api.model.Message;
import com.github.seratch.jslack.api.model.User;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author dledmonds
 */
public class UserMessageCountPerDayProcessor implements ChannelProcessor, MessageProcessor, UserProcessor {

    private final static String ALL_CHANNELS = "@All-Channels@";

    protected String title = "User Messages Per Day Count";
    protected Map<String, Conversation> channelMap = new HashMap<>();
    protected Map<String, User> userMap = new HashMap<>();
    protected Map<String, Date> userEarliestMessageMap = new HashMap<>();
    protected Map<String, UserMessageCountPerDay> userMessagePerDayMap = new HashMap<>();
    protected int limit;

    protected UserMessageCountPerDayProcessor(int limit) {
        this.limit = limit;
        userMessagePerDayMap.put(ALL_CHANNELS, new UserMessageCountPerDay(ALL_CHANNELS));
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

        // track earliest message across all channels
        Date earliestMsg = userEarliestMessageMap.get(message.getUser());
        if (earliestMsg == null) {
            earliestMsg = Utils.convertTsToDate(message.getTs());
        } else {
            Date currentMsg = Utils.convertTsToDate(message.getTs());
            if (currentMsg.getTime() < earliestMsg.getTime()) {
                earliestMsg = currentMsg;
            }
        }
        userEarliestMessageMap.put(message.getUser(), earliestMsg);

        // store details per channel
        UserMessageCountPerDay umc = userMessagePerDayMap.get(channel);
        if (umc == null) {
            umc = new UserMessageCountPerDay(channel);
            userMessagePerDayMap.put(channel, umc);
        }
        umc.seenMessageForUser(message.getUser());

        // store detail summary for all channels
        umc = userMessagePerDayMap.get(ALL_CHANNELS);
        umc.seenMessageForUser(message.getUser());
    }

    public void outputResults(OutputStream out) throws IOException {
        PrintStream ps = null;
        if (out instanceof PrintStream) {
            ps = (PrintStream)out;
        } else {
            ps = new PrintStream(out);
        }

        for (Entry<String, UserMessageCountPerDay> entry : userMessagePerDayMap.entrySet()) {
            entry.getValue().outputResults(ps);
        }
    }

    class UserMessageCountPerDay {
        private String channelId;
        private Map<String, Double> userMessagesMap = new HashMap<>();

        UserMessageCountPerDay(String channelId) {
            this.channelId = channelId;
        }

        void seenMessageForUser(String username) {
            Double count = userMessagesMap.get(username);
            if (count == null) count = new Double(0);
            userMessagesMap.put(username, ++count);
        }

        void outputResults(PrintStream ps) {
            // post process values to create a value per day concept
            long now = System.currentTimeMillis();
            for (Entry<String, Double> entry : userMessagesMap.entrySet()) {
                Date earliestMsg = userEarliestMessageMap.get(entry.getKey());
                long daysBetween = TimeUnit.DAYS.convert(now - earliestMsg.getTime(), TimeUnit.MILLISECONDS);
                entry.setValue(daysBetween < 1 ? 0 : entry.getValue() / daysBetween);
            }

            boolean order = false; // for asc or desc
            List<Entry<String, Double>> list = new LinkedList<>(userMessagesMap.entrySet());
            list.sort((o1, o2) -> order ? o1.getValue().compareTo(o2.getValue()) == 0
                    ? o1.getKey().compareTo(o2.getKey())
                    : o1.getValue().compareTo(o2.getValue()) : o2.getValue().compareTo(o1.getValue()) == 0
                    ? o2.getKey().compareTo(o1.getKey())
                    : o2.getValue().compareTo(o1.getValue()));
            Map<String, Double> sortedMap = list.stream().collect(Collectors.toMap(Entry::getKey, Entry::getValue, (a, b) -> b, LinkedHashMap::new));

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            ps.println();
            ps.println(title + " - " +
                    (channelId.equals(ALL_CHANNELS) ? "All Channels" : channelMap.get(channelId).getName()));
            ps.println("----------------------------------------");
            int loop = 0;
            for (Entry<String, Double> entry : sortedMap.entrySet()) {
                ps.println(String.format("%.2f", entry.getValue())
                        + "," + userMap.get(entry.getKey()).getName()
                        + "," + userMap.get(entry.getKey()).getRealName()
                        + "," + df.format(userEarliestMessageMap.get(entry.getKey())));
                if (++loop >= limit) return;
            }
        }
    }

}
