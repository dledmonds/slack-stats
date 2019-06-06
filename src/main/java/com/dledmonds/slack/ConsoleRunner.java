package com.dledmonds.slack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author dledmonds
 */
public class ConsoleRunner {



    public static void main(String[] args) {
        File configFile = new File("slack.properties");
        if (!configFile.exists()) {
            System.err.println("No slack.properties file exists in current directory");
            System.exit(1);
        }
        Properties p = new Properties();
        try (InputStream in = new FileInputStream(configFile)) {
            p.load(in);
        } catch (IOException ioe) {
            System.err.println("Unable to read slack.properties");
            System.exit(1);
        }
        String token = p.getProperty("slack.token");
        if (token == null || token.isEmpty()) {
            System.err.println("slack.token must be set");
            System.exit(1);
        }

        try {
            SlackEngine st = new SlackEngine(token, args);
            //StreamOutputProcessor p1 = new StreamOutputProcessor(System.out);
            StreamOutputProcessor p1 = new StreamOutputProcessor(new FileOutputStream(System.currentTimeMillis() + ".log"));
            st.addChannelProcessor(p1);
            st.addUserProcessor(p1);
            st.addMessageProcessor(p1);

            UserMessageCountProcessor p2 = new UserMessageCountProcessor(10);
            st.addChannelProcessor(p2);
            st.addUserProcessor(p2);
            st.addMessageProcessor(p2);

            UserHereOrChannelMessageCountProcessor p3 = new UserHereOrChannelMessageCountProcessor(10);
            st.addChannelProcessor(p3);
            st.addUserProcessor(p3);
            st.addMessageProcessor(p3);

            UserMessageCountPerDayProcessor p4 = new UserMessageCountPerDayProcessor(10);
            st.addChannelProcessor(p4);
            st.addUserProcessor(p4);
            st.addMessageProcessor(p4);

            st.run(); // single threaded

            System.out.println();
            p2.outputResults(System.out);

            System.out.println();
            p4.outputResults(System.out);

            System.out.println();
            p3.outputResults(System.out);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
