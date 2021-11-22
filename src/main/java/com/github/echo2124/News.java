package com.github.echo2124;


import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLOutput;
import java.util.List;

// Based around grabbing RSS feeds
public class News {
    private String NEWS_CHANNEL="912355120723943424";
    private String cachedTitle="";
    private String feedOrg;
    // fallback if author is not available from rss feed
    private String[] defaultAuthors= {"Monash University", "ABC News"};
    private SyndFeed feed;
    private final int feedIndex =0;
    public News(String newsType) {
        switch (newsType) {
            case "Monash":
                feedOrg="Monash";
                initRSS("https://www.monash.edu/_webservices/news/rss?query=covid");
                break;
            case "Covid":
                feedOrg="ABC";
                getLatestTweet();
                break;
            default:
                System.out.println("[News] Invalid news type");
        }
    }

    public void getLatestTweet() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("tu7AA1josocRsfFp2sgLriVGA")
                .setOAuthConsumerSecret("qYKut6rMwPleyhjHF03NJU4YdUQe8vInmDFa3cCOJUXW055ru8")
                .setOAuthAccessToken("786025827280392192-OuNhuZIPYb5N4yS667I72HXZMyGSmNH")
                .setOAuthAccessTokenSecret("ujI55HDOu1ZLlKDdiO2TzhSEwZgsaGLsfb0ztAqSt2EFW");
        TwitterStream ts = new TwitterStreamFactory(cb.build()).getInstance();
        StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(Status status) {
                if (status.getUser().getId()==43064490 && status.getText().contains("#COVID19VicData")) {
                    buildMsgFromTweet(status);
                }
            }

            @Override
            public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

            }

            @Override
            public void onTrackLimitationNotice(int numberOfLimitedStatuses) {

            }

            @Override
            public void onScrubGeo(long userId, long upToStatusId) {

            }

            @Override
            public void onStallWarning(StallWarning warning) {

            }

            @Override
            public void onException(Exception ex) {

            }
        };
        ts.addListener(listener);
        ts.sample();
      /*  Twitter twitter = new TwitterFactory(cb.build()).getInstance();
        List<Status> statuses;
        try {
            statuses = twitter.getUserTimeline(43064490);
            Status newStatus = statuses.get(1);
            buildMsgFromTweet(newStatus);
        } catch (Exception e) {
        }*/



        FilterQuery filter = new FilterQuery();
        filter.follow(new long[] {43064490});
        ts.filter(filter);

    }



    public void initRSS(String feedURL) {
        try {
            parseRSS(feedURL);
           // buildMSG(this.feed);
            sendMsg(this.feed);
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    public void parseRSS(String feedURL) throws MalformedURLException {
        URL newURL = new URL(feedURL);
        SyndFeed feed=null;
        try {
            feed = new SyndFeedInput().build(new XmlReader(newURL));
        } catch (Exception e) {
        }
        this.feed=feed;
    }

    // compares prev posted msg to current to see if there's an update
    public void pushNewMsg(SyndFeed feed) {
        // compare this to cached prev feed. If different then push update
        System.out.println(feed.getEntries().get(feedIndex));
        if (cachedTitle == "") {
            cachedTitle = feed.getEntries().get(feedIndex).getTitle();
        } else {
            if (cachedTitle.equals(feed.getEntries().get(feedIndex).getTitle())) {
                System.out.println("[News] Up To Date!");
            } else {

            }
        }
    }

    public void sendMsg(SyndFeed feed) {
        MessageChannel channel= Main.constants.jda.getTextChannelById(NEWS_CHANNEL);
        EmbedBuilder newEmbed = new EmbedBuilder();
        if (feed.getEntries().get(feedIndex).getAuthor().equals("") || feed.getAuthor()==null) {
            if (feedOrg.equals("Monash")) {
                newEmbed.setAuthor(defaultAuthors[0]);
            }
        } else {
            newEmbed.setAuthor(feed.getEntries().get(feedIndex).getAuthor());
            }
        newEmbed.setTitle(feed.getEntries().get(feedIndex).getTitle(), feed.getEntries().get(feedIndex).getLink());
        newEmbed.setDescription(feed.getEntries().get(feedIndex).getDescription().getValue());
        if (!feed.getEntries().get(feedIndex).getEnclosures().isEmpty()) {
            newEmbed.setImage(feed.getEntries().get(feedIndex).getEnclosures().get(0).getUrl());
        }
       newEmbed.setThumbnail(feed.getImage().getUrl());
       newEmbed.setFooter(feed.getDescription());
       channel.sendMessage(newEmbed.build()).queue();
    }

    public void buildMsgFromTweet(Status status) {
        System.out.println("Building MSG From tweet");
        MessageChannel channel = Main.constants.jda.getTextChannelById(NEWS_CHANNEL);
        EmbedBuilder newEmbed = new EmbedBuilder();
        newEmbed.setTitle("Victoria Covid Update");
        newEmbed.setDescription(status.getText());
        newEmbed.setAuthor("Victorian Department of Health");
        newEmbed.setThumbnail(status.getUser().getProfileImageURL());
        MediaEntity[] media = status.getMediaEntities();
        if (media.length >0) {
            if (media[0].getMediaURL().contains("twimg")) {
                System.out.println("Media detected");
                newEmbed.setImage(media[0].getMediaURL());
            }
        }
        newEmbed.setFooter(status.getUser().getDescription());
        channel.sendMessage(newEmbed.build()).queue();
    }
    }
