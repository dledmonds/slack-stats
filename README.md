# Slack Statistics
## Overview ##
Create some lighthearted stats using Slack data to find out which users post the most and make the most use of @channel/@here notifications.

## Usage
```
$ mvn clean package
```
At the same level as `pom.xml` create a `slack.properties` file with the following entries (substituting the <...> placeholders with your own data).
```
slack.token=<YOUR-TOKEN>
```
Run the report using the following command with optional CHANNEL-ID (default is all channels)
```
java -jar target/slack-statistics-1.0-SNAPSHOT-jar-with-dependencies.jar <CHANNEL-ID>
```

## TODO
- Handle message threads as separate messages
- Remove messages from bots connected to a user
- Stop treating join/leave channel notifications as messages
- Parse out use of :ICON: tags to generate statistics on
- Parse out @USER calls to generate statistics on
- UserMessageCountPerDayProcessor is considering your earliest message in any slack channel, might be better to make it
 earliest in a specific channel (assuming X days spent in channel)
