package com.chapslife.septasquare.push;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chapslife.septasquare.push.models.ChannelRecord;
import com.chapslife.septasquare.push.models.LinkedUser;
import com.chapslife.septasquare.push.models.SeptaStop;
import com.chapslife.septasquare.push.util.Common;

/**
 * The landing point for pushes from foursquare. Ensures that the push secret
 * value is correct, then parses the JSON and pushes the results out to the
 * approrpiate clients.
 */
@SuppressWarnings("serial")
public class PushHandler extends HttpServlet {
	private static final Logger log = Logger.getLogger(PushHandler.class.getName());

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
		if (u3.foursquareAuth() != null) {
			SeptaStop stop = SeptaStop.loadOrCreate(pm, venueId);
			if (stop.stopId() != null) {
				// TODO Auto-generated method stub
				String line = null;
				StringBuilder sb = new StringBuilder();
				StringBuilder build = new StringBuilder();
				String time = "";
				String direction = "";
				try {
					URL url = new URL("http://www3.septa.org/hackathon/BusSchedules/?req1="
							+ stop.stopId() + "&req2=" + "MFL" + "&req6=5");
					BufferedReader reader = new BufferedReader(new InputStreamReader(
							url.openStream()));

					log.warning("URL Path " + url.getPath());
					while ((line = reader.readLine()) != null) {
						sb.append(" " + line);
						log.info(line);
					}
					reader.close();

				} catch (MalformedURLException e) {
				} catch (IOException e) {
				}
				try {
					JSONObject pushJson = new JSONObject(sb.toString());
					JSONArray schedules = pushJson.getJSONArray("MFL");
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
				try {
					String charset = "UTF-8";
					String param1 = CHECKIN_ID;
					String param2 = build.toString();
					String param3 = "20120704";
					String param4 = u3.foursquareAuth();

					String query = String.format("CHECKIN_ID=%s&text=%s&v=%s&oauth_token=%s",
							URLEncoder.encode(param1, charset), URLEncoder.encode(param2, charset),
							URLEncoder.encode(param3, charset), URLEncoder.encode(param4, charset));

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

	}
}
