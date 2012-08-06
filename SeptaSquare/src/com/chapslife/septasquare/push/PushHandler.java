package com.chapslife.septasquare.push;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chapslife.septasquare.push.models.ChannelRecord;
import com.chapslife.septasquare.push.models.LinkedUser;
import com.chapslife.septasquare.push.models.SeptaStop;
import com.chapslife.septasquare.push.util.Common;
import com.google.gson.stream.JsonReader;

/**
 * The landing point for pushes from foursquare. Ensures that the push secret
 * value is correct, then parses the JSON and pushes the results out to the
 * approrpiate clients.
 */
@SuppressWarnings("serial")
public class PushHandler extends HttpServlet {
	private static final Logger log = Logger.getLogger(PushHandler.class.getName());
	static Date date = new Date();
	private double userLat = 0.0;
	private double userLng = 0.0;
	int currentStopID = 0;
	int closestStopID = 0;
	double curDistance = 0;
	double prevLng = 0.0;
	double prevLat = 0.0;
	double curLng = 0.0;
	double curLat = 0.0;
	private String stopname;
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String pushSecret = req.getParameter("secret");
		String pushBody = req.getParameter("checkin");
		log.warning("Push received: " + pushBody);
		if (pushSecret != null && pushBody != null && pushSecret.equals(Common.PUSH_SECRET)) {
			try {
				handlePush(pushBody);
			} catch (JSONException e) {
				log.warning("Had a terrible run-in with invalid JSON!\n" + e.getMessage() + "\n"
						+ pushBody);
			}
		}
	}

	// Parse the json and send the messages.
	private void handlePush(String pushBody) throws JSONException {
		JSONObject pushJson = new JSONObject(pushBody);

		// Read out the isMayor flag. This is so complicated since it could be
		// not present if false.
		// If it isn't present, a JSON exception is thrown. So, we catch that
		// and make it false.
		boolean isMayor = false;
		try {
			Boolean mayorness = pushJson.getBoolean("isMayor");
			isMayor = (mayorness != null && mayorness);
		} catch (JSONException e) {
			isMayor = false;
		}

		// Load in the required information. These will throw an exception if
		// missing, which is OK
		// since we couldn't continue if any of them aren't there.
		String vid = pushJson.getJSONObject("venue").getString("id");
		JSONObject user = pushJson.getJSONObject("user");
		String name = user.getString("firstName");
		String userId = user.getString("id");
		String photo = user.getString("photo");
		userLat = pushJson.getJSONObject("venue").getJSONObject("location").getDouble("lat");
		userLng = pushJson.getJSONObject("venue").getJSONObject("location").getDouble("lng");
		photo.replace("_thumbs", "");
		String CHECKIN_ID = pushJson.getString("id");
		String endpoint = "https://api.foursquare.com/v2/checkins/" + CHECKIN_ID + "/reply";
		if (vid != null && name != null) {
			PersistenceManager pm = Common.getPM();
			try {
				List<String> targetClients = ChannelRecord.loadOrCreate(pm, vid).clientIds();
				Common.sendUpdate(targetClients, Common.checkinToJson(name, photo, isMayor));
			} finally {
				pm.close();
			}
		}
		getSeptaData(endpoint, CHECKIN_ID, userId, vid);

	}

	private void getSeptaData(String endpoint, String CHECKIN_ID, String userId, String venueId) {
		PersistenceManager pm = Common.getPM();
		LinkedUser u3 = LinkedUser.loadOrCreate(pm, userId);
		String line = null;
		StringBuilder sb = new StringBuilder();
		StringBuilder build = new StringBuilder();
		String time = "";
		String direction = "";
		
		String param5 = "http://septasquare.appspot.com/bsl_alerts";
		if (u3.foursquareAuth() != null) {
			SeptaStop stop = SeptaStop.loadOrCreate(pm, venueId);
			if (stop.routeName().equals("BUS")) {
				try {
					URL url = new URL("http://www3.septa.org/hackathon/Stops/" + stop.stopId());
					BufferedReader reader = new BufferedReader(new InputStreamReader(
							url.openStream()));
					JsonReader readerd = new JsonReader(reader);
					readerd.beginArray();
					while (readerd.hasNext()) {
						readerd.beginObject();
						while (readerd.hasNext()) {
							String name = readerd.nextName();
							if (name.equals("lng")) {
								curLng = readerd.nextDouble();
							} else if (name.equals("lat")) {
								curLat = readerd.nextDouble();
							} else if (name.equals("stopid")) {
								currentStopID = readerd.nextInt();
							}else if (name.equals("stopname")) {
								stopname = readerd.nextString();
							}else {
								readerd.skipValue(); // avoid some unhandle events
							}
							if(closestStopID > 0){
								double thisDistance = distance(curLat, curLng, userLat, userLng);
								if(thisDistance < curDistance){
									closestStopID = currentStopID;
									curDistance = thisDistance;
								}
							}else{
								curDistance = distance(curLat, curLng, userLat, userLng);
								closestStopID = currentStopID;
							}
							log.warning(stopname + " " + String.valueOf(curDistance));
							
						}
						readerd.endObject();
					}
					log.warning("lat" + " " + String.valueOf(userLat));
					readerd.endArray();
					reader.close();
					if(currentStopID > 0){
						url = new URL("http://www3.septa.org/sms/" + String.valueOf(currentStopID) + "/" + String.valueOf(stop.stopId()));
						BufferedReader nreader = new BufferedReader(new InputStreamReader(
								url.openStream()));
						build.append("Next Stop times @\n");
						while ((line = nreader.readLine()) != null) {
							build.append(line + "\n");
							log.warning(line);
						}
					nreader.close();
					}
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else
			if (stop.routeName().equals("Rail")) {
				try {
					URL url = new URL(
							"http://www.empoweringsinglemoms.com/kennyphp/stoptimes.php?stopid="
									+ stop.stopId() + "&serv_id=" + getSeptaServiceID()
									+ "&dep_time_lo=" + DateUtils.now() + "&dep_time_hi="
									+ DateUtils.later() + "&format=json");
					BufferedReader reader = new BufferedReader(new InputStreamReader(
							url.openStream()));

					log.warning("URL Path " + url.getPath());
					while ((line = reader.readLine()) != null) {
						sb.append(" " + line);
						log.warning(line);
					}
					reader.close();
				} catch (MalformedURLException e) {
				} catch (IOException e) {
				}
				try {
					JSONObject pushJson = new JSONObject(sb.toString());
					JSONArray posts = pushJson.getJSONArray("posts");
					for (int index = 0; index < posts.length(); index++) {
						JSONObject object = posts.getJSONObject(index);

						JSONObject post = object.getJSONObject("post");

						String dest = post.getString("trip_headsign");

						String departTime = (post.getString("departure_time"));
						log.warning(dest + " " + departTime);
						
						if (index > 3) {
							break;
						}else{
							build.append("Train to " + dest + " @ " + departTime + ".\n");
						}
						
					}
					
				} catch (JSONException e1) {
					e1.printStackTrace();

				}
				
			} else if (stop.stopId() != null) {
				// TODO Auto-generated method stub
				if (stop.routeName().equals("MFL")) {
					param5 = "http://septasquare.appspot.com/mfl_alerts";
				}
				try {
					URL url = new URL("http://www3.septa.org/hackathon/BusSchedules/?req1="
							+ stop.stopId() + "&req2=" + stop.routeName() + "&req6=5");
					BufferedReader reader = new BufferedReader(new InputStreamReader(
							url.openStream()));

					log.warning("URL Path " + url.getPath());
					while ((line = reader.readLine()) != null) {
						sb.append(" " + line);
						log.warning(line);
					}
					reader.close();

				} catch (MalformedURLException e) {
				} catch (IOException e) {
				}
				try {
					JSONObject pushJson = new JSONObject(sb.toString());
					JSONArray schedules = pushJson.getJSONArray(stop.routeName());
					JSONObject schedule0 = null;
					JSONObject schedule1 = null;
					for (int index = 0; index < schedules.length(); index++) {
						JSONObject schedule = schedules.getJSONObject(index);
						log.warning("Direction " + schedule.getString("Direction"));
						if (schedule.getString("Direction").equals("0") && schedule0 == null) {
							schedule0 = schedules.getJSONObject(index);
						} else if (schedule.getString("Direction").equals("1") && schedule1 == null) {
							schedule1 = schedules.getJSONObject(index);
						} else if (schedule0 != null && schedule1 != null) {
							break;
						}
					}
					if (schedule0 != null) {
						build.append(" Next train " + stop.direction1() + " @ "
								+ schedule0.getString("date") + ". ");
					}
					if (schedule1 != null) {
						build.append(" Next train " + stop.direction2() + " @ "
								+ schedule1.getString("date") + ". ");
					}
				} catch (JSONException e1) {
					e1.printStackTrace();

				}

			}
			try {
				log.warning(build.toString());
				String charset = "UTF-8";
				String param1 = CHECKIN_ID;
				String param2 = build.toString();
				String param3 = "20120704";
				String param4 = u3.foursquareAuth();
				
				String query = String.format("CHECKIN_ID=%s&text=%s&v=%s&oauth_token=%s&url=%s",
						URLEncoder.encode(param1, charset), URLEncoder.encode(param2, charset),
						URLEncoder.encode(param3, charset), URLEncoder.encode(param4, charset),
						URLEncoder.encode(param5, charset));

				URL url = new URL(endpoint);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type",
						"application/x-www-form-urlencoded;charset=" + charset);
				OutputStream output = null;
				output = connection.getOutputStream();
				output.write(query.getBytes(charset));

				InputStream response = connection.getInputStream();

				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					// OK
				} else {
					BufferedReader in = new BufferedReader(new InputStreamReader(response));
					line = null;
					while ((line = in.readLine()) != null) {
						log.warning(line);
					}
				}
			} catch (Exception e) {
				log.warning(e.toString());
			}
		}

	}

	private double distance(double lat1, double lon1, double lat2, double lon2) {
		  double theta = lon1 - lon2;
		  double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		  dist = Math.acos(dist);
		  dist = rad2deg(dist);
		  dist = dist * 60 * 1.1515;
		  
		  return (dist);
		}

	private double deg2rad(double deg) {
		  return (deg * Math.PI / 180.0);
		}

		/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
		/*::  This function converts radians to decimal degrees             :*/
		/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
		private double rad2deg(double rad) {
		  return (rad * 180.0 / Math.PI);
		}
	public static String getSeptaServiceID() {
		String servid = null;
		String strDateFormat = "EEEE";
		SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
		sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		String weekday = sdf.format(date);
		Format formatter;
		String s;
		// TODO check cal date for exceptions
		Boolean reverse;
		String myHours;
		formatter = new SimpleDateFormat("hh:mm:ss");
		s = formatter.format(date);

		String[] tokens = s.split(":");
		// Making sure that the right service day comes up
		myHours = tokens[0];
		if (myHours.equals("24") || myHours.equals("25") || myHours.equals("26")) {
			reverse = true;
		} else {
			reverse = false;
		}
		weekday = weekday.trim();
		if (weekday.equals("Sunday")) {
			if (reverse == false) {
				servid = "S3";
			} else {
				servid = "S2";
			}
		} else if (weekday.equalsIgnoreCase("Saturday")) {
			if (reverse == false) {
				servid = "S2";
			} else {
				servid = "S1";
				// Log.d("NOOOO",weekday + "-" + reverse.toString() + "-" +
				// myHours.toString());
			}
		} else if (weekday.equals("Monday")) {
			if (reverse == false) {
				servid = "S1";
			} else {
				servid = "S3";
			}
		} else {
			servid = "S1";
			// Log.d("NOOOO",weekday + "-" + reverse.toString());
		}

		return servid;
	}

	public static class DateUtils {
		public static String date() {
			final String DATE_FORMAT_NOW = "yyyy-MM-dd";
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
			sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
			return sdf.format(cal.getTime());
		}

		public static String now() {
			final String DATE_FORMAT_NOW = "HH:mm:ss";
			String laterTime = null;
			String myHours = null;
			String myMins = null;
			String mySecs = null;
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
			sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
			cal.add(Calendar.MINUTE, -5);
			String ti = sdf.format(cal.getTime());
			String[] tokens = ti.split(":");
			myHours = tokens[0];
			myMins = tokens[1];
			mySecs = tokens[2];
			if (myHours.equals("00")) {
				myHours = "24";
			} else if (myHours.equals("01")) {
				myHours = "25";
			}
			laterTime = myHours + ":" + myMins + ":" + mySecs;
			return laterTime;

		}

		public static String later() {
			final String DATE_FORMAT_NOW = "HH:mm:ss";
			String laterTime = null;
			String myHours = null;
			String myMins = null;
			String mySecs = null;
			Calendar cal = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
			sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
			cal.add(Calendar.HOUR, 1);

			String ti = sdf.format(cal.getTime());
			String[] tokens = ti.split(":");
			myHours = tokens[0];
			myMins = tokens[1];
			mySecs = tokens[2];
			if (myHours.equals("00")) {
				myHours = "24";
			} else if (myHours.equals("01")) {
				myHours = "25";
			} else if (myHours.equals("02")) {
				myHours = "26";
			} else if (myHours.equals("03")) {
				myHours = "26";
			} else if (myHours.equals("04")) {
				myHours = "26";
			} else if (myHours.equals("05")) {
				myHours = "26";
			}
			laterTime = myHours + ":" + myMins + ":" + mySecs;
			return laterTime;
		}
	}
}
