package com.chapslife.septasquare.push.util;

import java.util.Date;
import java.util.List;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;

import com.chapslife.septasquare.push.models.LinkedUser;
import com.google.appengine.api.channel.ChannelMessage;
import com.google.appengine.api.channel.ChannelService;
import com.google.appengine.api.channel.ChannelServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserServiceFactory;

import fi.foyt.foursquare.api.FoursquareApi;

public class Common {
  public final static String CLIENT_ID = "RDHHGP2EJKC5SIWVKTKB5S4QNQA3IJ5WJ3MXMZCZLD1MCEZY";
  public final static String CLIENT_SECRET = "2NARXDMP2VCBUNBQGXAKETWCYF43X51RUJY0UZJO1FZZPAOT";
  private final static String CALLBACK = "http://septasquare.appspot.com/";
  public final static String PUSH_SECRET = "X11ZPECMHL5EK425WSPLWIMXZYZMZTMDWWHBCQRFWN2TJA55";
  public final static String TARGET_VENUE = "4f3a5573e4b0b6d3bba463fe";
  public final static String TARGET_VENUE_NAME = "Applico 5th Floor";
  
  public static FoursquareApi getApi() { return getApi(null); }
  public static FoursquareApi getApi(String token) {
    FoursquareApi foursquareApi = new FoursquareApi(CLIENT_ID, CLIENT_SECRET, CALLBACK);
    if (token != null) foursquareApi.setoAuthToken(token);
    return foursquareApi;
  }
  public static FoursquareApi getCurrentApi(PersistenceManager pm) {
    User googler = getGoogleUser();
    if (googler != null) {
      LinkedUser luser = LinkedUser.loadOrCreate(pm, googler.getUserId());
      if (luser.foursquareAuth() != null) {
        return getApi(luser.foursquareAuth());
      }
    }
    
    return null;
  }
  
  // Convert the core aspects of a checkin to json
  public static String checkinToJson(String name, String photo, boolean isMayor) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\"name\":\"");
    sb.append(name);
    sb.append("\",\"photo\":\"");
    sb.append(photo);
    sb.append("\",\"isMayor\":");
    sb.append(isMayor);
    sb.append("}");
    return sb.toString();
  }
  
  /* Methods for getting at aspects of persistence */
  private static final PersistenceManagerFactory pmfInstance =
      JDOHelper.getPersistenceManagerFactory("transactions-optional");

  private static PersistenceManagerFactory getPMF() {
    return pmfInstance;
  }
  
  public static PersistenceManager getPM() {
    return getPMF().getPersistenceManager();
  }
  
  /* Methods for user management tidbits */
  public static User getGoogleUser() {
    return UserServiceFactory.getUserService().getCurrentUser();
  }
  
  public static String getGoogleLoginUrl() {
    return UserServiceFactory.getUserService().createLoginURL("/#retryAuth");
  }
  
  public static String getFoursquareLoginUrl() {
    return "https://foursquare.com/oauth2/authenticate?client_id=" + CLIENT_ID +
           "&response_type=code&redirect_uri=" + CALLBACK;
  }
  
  /* Methods for handling Channel Client ID creation and understanding */
  public static String createChannelToken(String vid) {
    ChannelService cService = ChannelServiceFactory.getChannelService();
    return cService.createChannel(getClientId(vid));
  }
  
  private static String getClientId(String vid) {
    return vid + "-" + (new Date()).getTime();
  }
  
  public static String parseClientId(String clientId) {
    int ind = clientId.indexOf('-');
    if (ind > 0) {
      return clientId.substring(0, ind);
    } else return null;
  }
  
  // Actually handle sending out messages to a given list of clients
  public static void sendUpdate(List<String> clients, String message) {
    ChannelService cService = ChannelServiceFactory.getChannelService();
    
    for (String client : clients) {
      cService.sendMessage(new ChannelMessage(client, message));
    }
  }
}
