package com.github.echo2124;

public class Config {
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
    private final String channelExposureSiteId;
    private final String channelActivityLogId;
    private final Boolean verifiedModuleEnabled;
    private final Boolean newsModuleEnabled;
    private final Boolean onCampusModuleEnabled;
    private final Boolean sheetParserModuleEnabled;
    private final String[] sheetParserParentColumns;



    public Config(String configName, String serverId, String activityState, String roleVerifiedId, String roleOncampusId, String roleDevTeamId, String channelVerifyId, String channelAdminId, String channelMonashNewsId, String channelCovidUpdateId, String channelExposureSiteId, String channelActivityLogId, String channelOnCampusId, Boolean verifiedModuleEnabled, Boolean newsModuleEnabled, Boolean onCampusModuleEnabled, Boolean sheetParserModuleEnabled, String[] sheetParserParentColumns, Boolean sheetParserModuleEnabled1, String[] sheetParserParentColumns1) {
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
        this.channelExposureSiteId = channelExposureSiteId;
        this.channelActivityLogId = channelActivityLogId;
        this.verifiedModuleEnabled = verifiedModuleEnabled;
        this.newsModuleEnabled = newsModuleEnabled;
        this.onCampusModuleEnabled = onCampusModuleEnabled;
        this.sheetParserModuleEnabled = sheetParserModuleEnabled;
        this.sheetParserParentColumns = sheetParserParentColumns;
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

    public String getChannelExposureSiteId() {
        return channelExposureSiteId;
    }

    public String getChannelActivityLogId() {
        return channelActivityLogId;
    }
    public Boolean getVerifiedModuleEnabled() {return verifiedModuleEnabled;}

    public Boolean getNewsModuleEnabled() {return newsModuleEnabled;}

    public Boolean getOnCampusModuleEnabled() {return onCampusModuleEnabled;}

    public Boolean getSheetParserModuleEnabled() {return sheetParserModuleEnabled;}

    public String[] getSheetParserParentColumns() {return sheetParserParentColumns;}

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
                ", channelExposureSiteId='" + channelExposureSiteId + '\'' +
                ", channelActivityLogId='" + channelActivityLogId + '\'' +
                ", verifiedModuleEnabled='" + verifiedModuleEnabled + '\'' +
                ", newsModuleEnabled='" + newsModuleEnabled + '\'' +
                ", onCampusModuleEnabled='" + onCampusModuleEnabled + '\'' +
                '}';
    }
}
