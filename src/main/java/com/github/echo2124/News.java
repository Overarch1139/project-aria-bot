package com.github.echo2124;


import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import twitter4j.*;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLOutput;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.github.echo2124.Main.constants.activityLog;
import static com.github.echo2124.Main.constants.db;

public class News {

    private String cachedTitle="";
    private String feedOrg;
    // fallback if author is not available from rss feed
    private String[] defaultAuthors= {"Monash University", "ABC News"};
    private String[] monashCategories={"Technology Related News", "COVID-19 Related News", "General University News"};
    private SyndFeed feed;
    private final int feedIndex =0;
    private final String targetedExposureBuildingUrl="https://www.monash.edu/news/coronavirus-updates/exposure-sites";
   // COVID WEB SCRAPER CONFIG
    private final String VIC_COVID_DATA_URL="https://www.coronavirus.vic.gov.au/victorian-coronavirus-covid-19-data";
    private final String COVID_UPDATE_ISSUED_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(2) > div > div > h2";
    private final String COVID_CASES_PAST_WEEK_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(3) > div > div:nth-child(1) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_ACTIVE_CASES_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(3) > div > div:nth-child(2) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_HOSPITAL_CASES_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(5) > div > div:nth-child(1) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_HOSPITAL_ICU_CASES_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(5) > div > div:nth-child(2) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_PCR_TESTS_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(7) > div > div:nth-child(1) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_RAT_TESTS_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(7) > div > div:nth-child(2) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_TOTAL_CASES_PCR_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(7) > div > div:nth-child(3) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_LIVES_LOST_PER_DAY_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(9) > div > div:nth-child(1) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_TOTAL_LIVES_LOST_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(9) > div > div:nth-child(2) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private final String COVID_CASES_RECOVERED_SELECTOR="#rpl-main-content > div > div > div > div > div:nth-child(9) > div > div:nth-child(3) > div > div.rpl-markup.tide-wysiwyg.app-wysiwyg.rpl-statistics-grid__block-heading > div";
    private Database db;
    // if category not exist, push regardless, if category check for title. Match against feed title trying to be pushed
    public News(String newsType, Database db) {
        this.db=db;
        if (newsType.equals("Covid")) {
            feedOrg = "ABC";
            if (!Boolean.parseBoolean(System.getenv("IS_DEV"))) {
                getLatestTweet();
            }
        } else if (newsType.equals("Monash")) {
            feedOrg = "Monash";
            if (Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_CATEGORY", "technology"))) {
                System.out.println("[News] Technology Category Found!");
                initRSS("https://www.monash.edu/_webservices/news/rss?category=engineering+%26+technology", "technology", true);
            } else {
                initRSS("https://www.monash.edu/_webservices/news/rss?category=engineering+%26+technology", "technology", false);
            }
            if (Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_CATEGORY", "covid"))) {
                System.out.println("[News] COVID Category Found!");
                initRSS("https://www.monash.edu/_webservices/news/rss?query=covid", "covid", true);
            } else {
                initRSS("https://www.monash.edu/_webservices/news/rss?query=covid", "covid", false);
            }
            if (Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_CATEGORY", "news"))) {
                System.out.println("[News] News Category Found!");
                initRSS("https://www.monash.edu/_webservices/news/rss?category=university+%26+news", "news", true);
            } else {
                initRSS("https://www.monash.edu/_webservices/news/rss?category=university+%26+news", "news", false);
            }
            setInterval();
        }
    }


    // checks  every 4 hrs for RSS feed updates
    public void setInterval() {
        TimerTask updateMonashNews = new TimerTask() {
            public void run() {
                new News("Monash", Main.constants.db);
                new News("ExposureBuilding",Main.constants.db);
            }
        };
        Timer timer = new Timer("Timer");
        long delay=(int) 2.16e7;
        timer.schedule(updateMonashNews, delay);
    }


    public void getLatestTweet() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(System.getenv("TWITTER_CONSUMER_KEY"))
                .setOAuthConsumerSecret(System.getenv("TWITTER_CONSUMER_SECRET"))
                .setOAuthAccessToken(System.getenv("TWITTER_ACCESS_TOKEN"))
                .setOAuthAccessTokenSecret(System.getenv("TWITTER_ACCESS_SECRET"));

        TwitterStream ts = new TwitterStreamFactory(cb.build()).getInstance();
        StatusListener listener = new StatusListener() {
            @Override
            public void onStatus(Status status) {
                if (status.getUser().getId()==43064490){
                   if (status.getText().contains("#COVID19VicData") || status.getText().contains("More data soon")) {
                       activityLog.sendActivityMsg("[NEWS] Building covid update msg",1, null);
                       try {
                           Document doc = Jsoup.connect(VIC_COVID_DATA_URL).get();
                           buildMsgFromWebScrape(doc);

                       } catch (Exception e) {
                           activityLog.sendActivityMsg("[NEWS] Unable to get covid data from scrape: "+e.getMessage(),3,null);
                       }
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
        FilterQuery filter = new FilterQuery();
        filter.follow(new long[] {43064490});
        ts.filter(filter);
    }

    public void initRSS(String feedURL, String category, Boolean checkLatest) {
        try {
            activityLog.sendActivityMsg("[NEWS] Initialising RSS Feed listener for category: "+category,1, null);
            parseRSS(feedURL);
            sendMsg(this.feed,category,checkLatest);
        } catch (Exception e) {
            activityLog.sendActivityMsg("[NEWS] Unable to initialise RSS Feed listener: "+e.getMessage(),3, null);
            throw new Error(e);
        }
    }

    public void parseRSS(String feedURL) throws MalformedURLException {
        activityLog.sendActivityMsg("[NEWS] Parsing received RSS Feed",1, null);
        URL newURL = new URL(feedURL);
        SyndFeed feed=null;
        try {
            feed = new SyndFeedInput().build(new XmlReader(newURL));
        } catch (Exception e) {
            activityLog.sendActivityMsg("[NEWS] Unable to parse RSS Feed: "+e.getMessage(),3, null);
        }
        this.feed=feed;
    }


    public void sendMsg(SyndFeed feed, String category, Boolean checkState) {
        if (!checkState || !Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_LASTITLE",category+"##"+feed.getEntries().get(feedIndex).getTitle()))) {
            // check for guilds
            for (String key : Main.constants.config.keySet()) {
                if (Main.constants.config.get(key).getNewsModuleEnabled()) {
                    MessageChannel channel = Main.constants.jda.getTextChannelById(Main.constants.config.get(key).getChannelMonashNewsId());
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
                    switch (category) {
                        case "technology":
                            newEmbed.setFooter(monashCategories[0]);
                            break;
                        case "covid":
                            newEmbed.setFooter(monashCategories[1]);
                            break;
                        case "news":
                            newEmbed.setFooter(monashCategories[2]);
                            break;
                        default:
                            newEmbed.setFooter(feed.getDescription());
                            break;
                    }
                    channel.sendMessageEmbeds(newEmbed.build()).queue();
                    activityLog.sendActivityMsg("[NEWS] Sending Monash News update", 1, null);
                    HashMap<String, String> data = new HashMap<String, String>();
                    data.put("title", feed.getEntries().get(feedIndex).getTitle());
                    db.modifyDB("NEWS", category, data);
                }
            }
        }
    }

    public void buildMsgFromTweet(Status status, String type) {
        for (String key : Main.constants.config.keySet()) {
            System.out.println("Building MSG From tweet");
            MessageChannel channel =null;
            if (type.equals("covid_update")) {
                    if (Main.constants.config.get(key).getNewsModuleEnabled()) {
                        channel = Main.constants.jda.getTextChannelById(Main.constants.config.get(key).getChannelCovidUpdateId());
                    }
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
            channel.sendMessageEmbeds(newEmbed.build()).queue();
        }
    }


    public void buildMsgFromWebScrape(Document doc) {
        try {
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("update_issued",doc.select(COVID_UPDATE_ISSUED_SELECTOR).get(0).text());
            data.put("cases_past", doc.select(COVID_CASES_PAST_WEEK_SELECTOR).get(0).text());
        } catch (Exception e ) {
            activityLog.sendActivityMsg("[NEWS] Unable to parse covid website data", 3, null);
        }
    }
}
