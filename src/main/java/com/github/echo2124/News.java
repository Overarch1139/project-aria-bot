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
        } else if (newsType.equals("ExposureBuilding")) {
            try {
                System.out.println("[NEWS] Getting Exposure Building info");
                Document doc = Jsoup.connect(targetedExposureBuildingUrl).get();
                System.out.println(doc.title());
                fetchCovidExposureInfo(doc);
            } catch (Exception e) {
                System.out.println("[Exposure Site] ERROR: "+e.getMessage());
                activityLog.sendActivityMsg("[NEWS] Unable to get exposure info: "+e.getMessage(),3);
            }
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
                       activityLog.sendActivityMsg("[NEWS] Building covid update msg",1);
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
        FilterQuery filter = new FilterQuery();
        filter.follow(new long[] {43064490});
        ts.filter(filter);
    }

    public void initRSS(String feedURL, String category, Boolean checkLatest) {
        try {
            activityLog.sendActivityMsg("[NEWS] Initialising RSS Feed listener for category: "+category,1);
            parseRSS(feedURL);
            sendMsg(this.feed,category,checkLatest);
        } catch (Exception e) {
            activityLog.sendActivityMsg("[NEWS] Unable to initialise RSS Feed listener: "+e.getMessage(),3);
            throw new Error(e);
        }
    }

    public void parseRSS(String feedURL) throws MalformedURLException {
        activityLog.sendActivityMsg("[NEWS] Parsing received RSS Feed",1);
        URL newURL = new URL(feedURL);
        SyndFeed feed=null;
        try {
            feed = new SyndFeedInput().build(new XmlReader(newURL));
        } catch (Exception e) {
            activityLog.sendActivityMsg("[NEWS] Unable to parse RSS Feed: "+e.getMessage(),3);
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
        if (!checkState || !Boolean.parseBoolean(db.getDBEntry("NEWS_CHECK_LASTITLE",category+"##"+feed.getEntries().get(feedIndex).getTitle()))) {
            // check for guilds
            MessageChannel channel = Main.constants.jda.getTextChannelById(Main.constants.config.getChannelMonashNewsId());
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
            activityLog.sendActivityMsg("[NEWS] Sending Monash News update",1);
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("title", feed.getEntries().get(feedIndex).getTitle());
            db.modifyDB("NEWS", category,data);
        }
    }

    public void buildMsgFromTweet(Status status, String type) {
        System.out.println("Building MSG From tweet");
        MessageChannel channel =null;
        if (type.equals("covid_update")) {
            channel = Main.constants.jda.getTextChannelById(Main.constants.config.getChannelCovidUpdateId());
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

    public void fetchCovidExposureInfo(Document doc) {
        activityLog.sendActivityMsg("[NEWS] Fetching exposure info from remote",1);
        JSONObject jsonParentObject = new JSONObject();
        int numExposures = 0;
        try {
            Element table = doc.select("#covid-19_exposure_site__table").get(0);
            System.out.println("[NEWS] Parsing exposure site data");
            for (Element row : table.select("tr")) {
                JSONObject jsonObject = new JSONObject();
                Elements tds = row.select("td");
                if (!tds.isEmpty()) {
                    String campus = tds.get(0).text();
                    String building = tds.get(1).text();
                    String exposurePeriod = tds.get(2).text();
                    String cleaningStatus = tds.get(3).text();
                    String healthAdvice = tds.get(4).text();
                    jsonObject.put("Campus", campus);
                    jsonObject.put("Building", building);
                    jsonObject.put("ExposurePeriod", exposurePeriod);
                    jsonObject.put("CleaningStatus", cleaningStatus);
                    jsonObject.put("HealthAdvice", healthAdvice);
                    jsonParentObject.put(String.valueOf(numExposures), jsonObject);
                    numExposures++;
                }
            }
        } catch (Exception e) {
            System.out.println("[NEWS] ERROR: unable to parse exposure site table");
            activityLog.sendActivityMsg("[NEWS] ERROR: unable to parse exposure site table",3);
        }
        System.out.println("JSON:");
        System.out.println(jsonParentObject.toString());
        int retrivedIndex=Integer.parseInt(db.getDBEntry("CHECK_EXPOSURE_INDEX", "EXPOSURE_SITE"));
        if (retrivedIndex==0) {
            retrivedIndex=numExposures-4;
        }
          if (numExposures>retrivedIndex) {
            // do quick math here, find difference and reverse json object possibly
            HashMap<String, String> data = new HashMap<String, String>();
            data.put("col_name", "exposure_sites");
            data.put("size", String.valueOf(numExposures));
            db.modifyDB("EXPOSURE_SITE","", data);
            for (int i=0; i<(numExposures-retrivedIndex);i++) {
                buildMsgFromWebScrape(jsonParentObject.getJSONObject(String.valueOf(i)));
            }
          }
    }

    public void buildMsgFromWebScrape(JSONObject data) {
        for (String key : Main.constants.config.keySet()) {
            if (Main.constants.config.get(key).get
            activityLog.sendActivityMsg("[NEWS] Building exposure message", 1);
            MessageChannel channel = Main.constants.jda.getTextChannelById(Main.constants.config.getChannelExposureSiteId());
            EmbedBuilder embed = new EmbedBuilder();
            embed.setTitle("Exposure Sites Update");
            // will be the contents of above method **if** there is an update
            embed.addField("Campus: ", data.getString("Campus"), false);
            embed.addField("Building: ", data.getString("Building"), false);
            embed.addField("Exposure Period: ", data.getString("ExposurePeriod"), false);
            embed.addField("Cleaning Status: ", data.getString("CleaningStatus"), false);
            embed.addField("Health Advice: ", data.getString("HealthAdvice"), false);
            embed.setDescription("As always if you test positive to covid and have been on campus please report it to Monash University using the button below.");
            embed.setAuthor("Monash University");
            embed.setThumbnail("http://www.monash.edu/__data/assets/image/0008/492389/monash-logo.png");
            ArrayList<Button> btns = new ArrayList<Button>();
            btns.add(Button.link("https://www.monash.edu/news/coronavirus-updates", "Monash COVID Bulletin").withEmoji(Emoji.fromUnicode("U+2139")));
            btns.add(Button.link("https://forms.monash.edu/covid19-self-reporting", "Monash COVID Self-Report").withEmoji(Emoji.fromUnicode("U+1F4DD")));
            channel.sendMessageEmbeds(embed.build()).setActionRow(btns).queue();
        }
    }
}
