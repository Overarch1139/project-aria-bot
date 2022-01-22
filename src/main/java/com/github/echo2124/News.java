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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class News {

    private String cachedTitle="";
    private String feedOrg;
    // fallback if author is not available from rss feed
    private String[] defaultAuthors= {"Monash University", "ABC News"};
    private SyndFeed feed;
    private final int feedIndex =0;
    private Database db;
    // if category not exist, push regardless, if category check for title. Match against feed title trying to be pushed
    // TODO: resolve inf loop issue. Probably something to do with setInterval method
    public News(String newsType, Database db) {
        this.db=db;
        if (newsType.equals("Covid")) {
            feedOrg = "ABC";
            getLatestTweet();
        } else if (newsType.equals("Monash")) {
            feedOrg="Monash";
                if (Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_CATEGORY", "technology"))) {
                    initRSS("https://www.monash.edu/_webservices/news/rss?category=engineering+%26+technology","technology", true);
                } else {
                    initRSS("https://www.monash.edu/_webservices/news/rss?category=engineering+%26+technology","technology", false);
                }
                if (Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_CATEGORY", "covid"))) {
                    initRSS("https://www.monash.edu/_webservices/news/rss?query=covid", "covid", true);
                } else {
                    initRSS("https://www.monash.edu/_webservices/news/rss?query=covid", "covid", false);
                }
                if (Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_CATEGORY", "news"))) {
                    initRSS("https://www.monash.edu/_webservices/news/rss?category=university+%26+news", "news", true);
                } else {
                    initRSS("https://www.monash.edu/_webservices/news/rss?category=university+%26+news", "news", false);
                }
             //   setInterval();

        }
    }


    // checks everyday at 1am for updates. RSS Feed is scheduled to update at 12am, hence why its not worth running it constantly.
    public void setInterval() {
        Runnable task = () -> {
            new News("Monash", Main.constants.db);
        };
        long delay = ChronoUnit.MILLIS.between(LocalTime.now(), LocalTime.of(01, 00, 00));
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(task, delay, TimeUnit.MILLISECONDS);
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
                if (status.getUser().getId()==43064490){
                   if (status.getText().contains("#COVID19VicData") || status.getText().contains("More data soon")) {
                       buildMsgFromTweet(status, "covid_update");
                   }
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



    public void initRSS(String feedURL, String category, Boolean checkLatest) {
        try {
            parseRSS(feedURL);
           // buildMSG(this.feed);
            sendMsg(this.feed,category,checkLatest);
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

    public void sendMsg(SyndFeed feed, String category, Boolean checkState) {
        if (!checkState || !Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_LASTITLE",category+"|"+feed.getEntries().get(feedIndex).getTitle()))) {
            MessageChannel channel = Main.constants.jda.getTextChannelById(Main.constants.NEWS_CHANNEL);
            EmbedBuilder newEmbed = new EmbedBuilder();
            if (feed.getEntries().get(feedIndex).getAuthor().equals("") || feed.getAuthor() == null) {
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
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("title", feed.getEntries().get(feedIndex).getTitle());
            db.modifyDB("NEWS", category,data);
        }
    }


    public void buildMsgFromTweet(Status status, String type) {
        System.out.println("Building MSG From tweet");
        MessageChannel channel =null;
        if (type.equals("covid_update")) {
            channel = Main.constants.jda.getTextChannelById(Main.constants.COVID_UPDATE_CHANNEL);
        } else {
           // channel = Main.constants.jda.getTextChannelById(Main.constants.NEWS_CHANNEL);
        }
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
