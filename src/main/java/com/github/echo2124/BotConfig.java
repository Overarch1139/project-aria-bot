package com.github.echo2124;

public class BotConfig {
    private final String configName;
    private final String serverId;
    private final String activityState;
    private final String roleVerifiedId;
    private final String roleOnCampusId;
    private final String roleDevTeamId;
    private final String channelVerifyId;
    private final String channelAdminId;
    private final String channelMonashNewsId;
    private final String channelCovidUpdateId;
    private final String channelOnCampusId;
    private final String channelActivityLogId;
    private final Boolean verifiedModuleEnabled;
    private final Boolean newsModuleEnabled;
    private final Boolean onCampusModuleEnabled;

    public BotConfig(String configName, String serverId, String activityState, String roleVerifiedId, String roleOncampusId, String roleDevTeamId, String channelVerifyId, String channelAdminId, String channelMonashNewsId, String channelCovidUpdateId, String channelActivityLogId, String channelOnCampusId, Boolean verifiedModuleEnabled, Boolean newsModuleEnabled, Boolean onCampusModuleEnabled) {
        this.configName = configName;
        this.serverId = serverId;
        this.activityState = activityState;
        this.roleVerifiedId = roleVerifiedId;
        this.roleOnCampusId = roleOncampusId;
        this.roleDevTeamId = roleDevTeamId;
        this.channelVerifyId = channelVerifyId;
        this.channelAdminId = channelAdminId;
        this.channelMonashNewsId = channelMonashNewsId;
        this.channelCovidUpdateId = channelCovidUpdateId;
        this.channelOnCampusId = channelOnCampusId;
        this.channelActivityLogId = channelActivityLogId;
        this.verifiedModuleEnabled = verifiedModuleEnabled;
        this.newsModuleEnabled = newsModuleEnabled;
        this.onCampusModuleEnabled = onCampusModuleEnabled;
    }

    public String getConfigName() {
        return configName;
    }

    public String getServerId() {
        return serverId;
    }

    public String getActivityState() {
        return activityState;
    }

    public String getRoleVerifiedId() {
        return roleVerifiedId;
    }

    public String getRoleOnCampusId() {
        return roleOnCampusId;
    }

    public String getRoleDevTeamId() {
        return roleDevTeamId;
    }

    public String getChannelVerifyId() {
        return channelVerifyId;
    }

    public String getChannelAdminId() {
        return channelAdminId;
    }

    public String getChannelMonashNewsId() {
        return channelMonashNewsId;
    }

    public String getChannelCovidUpdateId() {
        return channelCovidUpdateId;
    }

    public String getChannelOnCampusId() {
        return channelOnCampusId;
    }

    public String getChannelActivityLogId() {
        return channelActivityLogId;
    }
    public Boolean getVerifiedModuleEnabled() {return verifiedModuleEnabled;}

    public Boolean getNewsModuleEnabled() {return newsModuleEnabled;}

    public Boolean getOnCampusModuleEnabled() {return onCampusModuleEnabled;}

    @Override
    public String toString() {
        return "Config{" +
                "configName='" + configName + '\'' +
                ", serverId='" + serverId + '\'' +
                ", activityState='" + activityState + '\'' +
                ", roleVerifiedId='" + roleVerifiedId + '\'' +
                ", roleOnCampusId='" + roleOnCampusId + '\'' +
                ", roleDevTeamId='" + roleDevTeamId + '\'' +
                ", channelVerifyId='" + channelVerifyId + '\'' +
                ", channelAdminId='" + channelAdminId + '\'' +
                ", channelMonashNewsId='" + channelMonashNewsId + '\'' +
                ", channelCovidUpdateId='" + channelCovidUpdateId + '\'' +
                ", channelOnCampusId='" + channelOnCampusId + '\'' +
                ", channelActivityLogId='" + channelActivityLogId + '\'' +
                ", verifiedModuleEnabled='" + verifiedModuleEnabled + '\'' +
                ", newsModuleEnabled='" + newsModuleEnabled + '\'' +
                ", onCampusModuleEnabled='" + onCampusModuleEnabled + '\'' +
                '}';
    }
}
