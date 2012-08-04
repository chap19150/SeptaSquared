package com.chapslife.septasquare.push;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.chapslife.septasquare.push.models.LinkedUser;
import com.chapslife.septasquare.push.models.SeptaStop;
import com.chapslife.septasquare.push.util.Common;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.users.User;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;

/**
 * A servlet to dynamically create the HTML for the homepage. Also enforces that
 * the user is both logged in to Google and connected to Foursquare.
 */
@SuppressWarnings("serial")
public class IndexServlet extends HttpServlet {
	private static final Logger log = Logger.getLogger(GetHereNow.class.getName());
	private String code = "d";
	private String reqs = "f";
	private LinkedUser u;
	private PersistenceManager pm;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

		pm = Common.getPM();
		/*// 69
		SeptaStop ss = SeptaStop.loadOrCreate(pm, "4a920d35f964a520b41c20e3");
		ss.save(pm);
		// millbourne
		ss = SeptaStop.loadOrCreate(pm, "4bbb838b51b89c74444d862a");
		ss.save(pm);
		// 63
		ss = SeptaStop.loadOrCreate(pm, "4b9c156af964a520484836e3");
		ss.save(pm);
		// 60
		ss = SeptaStop.loadOrCreate(pm, "4ba0b60ff964a520d47837e3");
		ss.save(pm);
		// 56
		ss = SeptaStop.loadOrCreate(pm, "4ca6a91c14c33704d54bc23b");
		ss.save(pm);
		// 56
		ss = SeptaStop.loadOrCreate(pm, "4bbb84b751b89c74054f862a");
		ss.save(pm);
		// 52
		ss = SeptaStop.loadOrCreate(pm, "4b9e1909f964a5208aca36e3");
		ss.save(pm);
		// 46
		ss = SeptaStop.loadOrCreate(pm, "4b38db92f964a520375125e3");
		ss.save(pm);
		// 40
		ss = SeptaStop.loadOrCreate(pm, "4b749d6cf964a520e7e72de3");
		ss.save(pm);
		// 34
		ss = SeptaStop.loadOrCreate(pm, "4b1d1910f964a520d80b24e3");
		ss.save(pm);
		// 30
		ss = SeptaStop.loadOrCreate(pm, "4dc1379f45dd472e9b71a891");
		ss.save(pm);
		// 15
		ss = SeptaStop.loadOrCreate(pm, "4a689388f964a52096ca1fe3");
		ss.save(pm);
		// 13
		ss = SeptaStop.loadOrCreate(pm, "4b7b50b1f964a520e95d2fe3");
		ss.save(pm);
		// 11
		ss = SeptaStop.loadOrCreate(pm, "4b984885f964a5206b3935e3");
		ss.save(pm);
		// 8
		ss = SeptaStop.loadOrCreate(pm, "4b0c961bf964a520b23f23e3");
		ss.save(pm);
		// 5
		ss = SeptaStop.loadOrCreate(pm, "4a6894a7f964a52099ca1fe3");
		ss.save(pm);
		// 2
		ss = SeptaStop.loadOrCreate(pm, "4c0ed0352466a59389b17921");
		ss.save(pm);
		// spring
		ss = SeptaStop.loadOrCreate(pm, "4a2fa975f964a520cc981fe3");
		ss.save(pm);
		// girard
		ss = SeptaStop.loadOrCreate(pm, "4b281899f964a520f08e24e3");
		ss.save(pm);
		// berks
		ss = SeptaStop.loadOrCreate(pm, "4b47ba7ff964a5202f3c26e3");
		ss.save(pm);
		// y-d
		ss = SeptaStop.loadOrCreate(pm, "4a6764b1f964a52056c91fe3");
		ss.save(pm);
		// huntingdon
		ss = SeptaStop.loadOrCreate(pm, "4b47bb38f964a5204d3c26e3");
		ss.save(pm);
		// somerset
		ss = SeptaStop.loadOrCreate(pm, "4b47bbddf964a520663c26e3");
		ss.save(pm);
		// allegheny
		ss = SeptaStop.loadOrCreate(pm, "4b4c017af964a520ecac26e3");
		ss.save(pm);
		// tioga
		ss = SeptaStop.loadOrCreate(pm, "4b4c0233f964a520f3ac26e3");
		ss.save(pm);
		// erie-torr
		ss = SeptaStop.loadOrCreate(pm, "4b54873ef964a520aebe27e3");
		ss.save(pm);
		// church
		ss = SeptaStop.loadOrCreate(pm, "4b54884af964a520d5be27e3");
		ss.save(pm);

		// margaret
		ss = SeptaStop.loadOrCreate(pm, "4b54895df964a520fcbe27e3");
		ss.save(pm);
		// frankford
		ss = SeptaStop.loadOrCreate(pm, "4b9460dff964a520f67634e3");
		ss.save(pm);*/
		try {
			User googler = Common.getGoogleUser();

			if (googler == null)
				resp.sendRedirect(Common.getGoogleLoginUrl());
			else {
				resp.setContentType("text/html");
				PrintWriter out = resp.getWriter();
				u = LinkedUser.loadOrCreate(pm, Common.getGoogleUser().getUserId());
				FoursquareApi api = null;
				if (u.foursquareAuth() != null) {
					api = Common.getApi(u.foursquareAuth());
				} else {
					code = req.getParameter("code");
					log.info(code);
					if (code != null) {
						api = Common.getApi();
						try {
							api.authenticateCode(code);
							if (api.getOAuthToken() != null) {
								u = LinkedUser.loadOrCreate(pm, Common.getGoogleUser().getUserId());
								u.foursquareAuth = api.getOAuthToken();
								getUserID(u.foursquareAuth);
								u.save(pm);
							} else
								api = null;
						} catch (FoursquareApiException e) {
							log.warning(e.toString());
							api = null;
						}
					}

				}

				if (api == null || api.getOAuthToken() == null || api.getOAuthToken().length() <= 0) {
					writeHead(out, "Connect to foursquare");
					writeConnectPrompt(out);
					writeFoot(out, null, null);
				} else {
					writeHead(out, "Welcome to " + Common.TARGET_VENUE_NAME);
					writePage(out, Common.TARGET_VENUE_NAME);
					writeFoot(out, Common.TARGET_VENUE,
							Common.createChannelToken(Common.TARGET_VENUE));
				}
			}
		} finally {
			pm.close();
		}
	}

	private void getUserID(String auth) {
		StringBuilder sb = new StringBuilder();
		try {
			URL url = new URL("https://api.foursquare.com/v2/users/self?oauth_token=" + auth
					+ "&v=20120704");
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			String line;

			while ((line = reader.readLine()) != null) {
				sb.append(" " + line);
			}
			reader.close();
			JSONObject object = new JSONObject(sb.toString());
			JSONObject response = object.getJSONObject("response");
			JSONObject user = response.getJSONObject("user");
			String userId = user.getString("id");
			LinkedUser u3 = LinkedUser.loadOrCreate(pm, userId);
			u3.foursquareAuth = auth;
			u3.save(pm);
		} catch (MalformedURLException e) {
			// ...
		} catch (IOException e) {
			// ...
		} catch (JSONException e) {
			// TODO Auto-generated catch block
		}
	}

	private void writeHead(PrintWriter out, String title) {
		out.println("<!doctype html><html>\n<head>\n<title>");
		out.println(title);
		out.println("</title>\n<link rel=\"stylesheet\" href=\"http://twitter.github.com/bootstrap/1.4.0/bootstrap.min.css\"/>");
		out.println("<link type=\"text/css\" rel=\"stylesheet\" media=\"all\" href=\"/static/style.css\"/>");
		out.println("<script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.7.0/jquery.min.js\"></script>");
		out.println("<script type=\"text/javascript\" src=\"/_ah/channel/jsapi\"></script>");
		out.println("<script type=\"text/javascript\" src=\"/static/script.js\"></script>");
		out.println("</head><body>\n<div id=\"wrapper\" class=\"container-fluid\">");
	}

	private void writePage(PrintWriter out, String placeName) {
		out.println("<div id=\"lastCheckin\"><div id=\"picWrapper\"><img id=\"userPic\" src=\"\"/><img id=\"userCrown\" src=\"/static/crown.png\"/></div>");
		out.println("<h1 id=\"message\"></h1></div>\n<div id=\"herenow\">");
		out.println("<div id=\"people\"></div><h2><span id=\"count\">0</span> here @ " + placeName
				+ "</h2>\n</div>");
	}

	private void writeConnectPrompt(PrintWriter out) {
		out.println("<div id=\"connect\">\nTo use this widget, you need to connect to foursquare "
				+ "d" + " .<br />");
		out.print("<a href=\"" + Common.getFoursquareLoginUrl());
		out.println("\"><img src=\"/static/connect-white.png\" /></a>\n</div>");
	}

	// If producing a working page, include the channel token right away to get
	// going immediately.
	private void writeFoot(PrintWriter out, String vid, String token) {
		out.println("<script type=\"text/javascript\">");

		if (token != null) {
			out.println("channel = new goog.appengine.Channel('" + token + "');");
			out.println("socket = channel.open();\nsocket.onopen = onOpened;\nsocket.onmessage = onMessage;");
			out.println("socket.onerror = onError;\nsocket.onclose = onClose;");
		}
		if (vid != null) {
			out.println("var vid = '" + vid + "';");
		}

		out.println("</script>");

		out.print("</div></body></html>");
	}
}
