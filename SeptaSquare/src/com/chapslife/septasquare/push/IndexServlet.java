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
		//getBuses();
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

	private void getMFL() {
		// 69
		SeptaStop ss = SeptaStop.loadOrCreate(pm, "4a920d35f964a520b41c20e3", 416);
		ss.save(pm);
		// millbourne
		ss = SeptaStop.loadOrCreate(pm, "4bbb838b51b89c74444d862a", 2446);
		ss.save(pm);
		// 63
		ss = SeptaStop.loadOrCreate(pm, "4b9c156af964a520484836e3", 2447);
		ss.save(pm);
		// 60
		ss = SeptaStop.loadOrCreate(pm, "4ba0b60ff964a520d47837e3", 2448);
		ss.save(pm);
		// 56
		ss = SeptaStop.loadOrCreate(pm, "4ca6a91c14c33704d54bc23b", 2449);
		ss.save(pm);
		// 56
		ss = SeptaStop.loadOrCreate(pm, "4bbb84b751b89c74054f862a", 2449);
		ss.save(pm);
		// 52
		ss = SeptaStop.loadOrCreate(pm, "4b9e1909f964a5208aca36e3", 2450);
		ss.save(pm);
		// 46
		ss = SeptaStop.loadOrCreate(pm, "4b38db92f964a520375125e3", 2451);
		ss.save(pm);
		// 40
		ss = SeptaStop.loadOrCreate(pm, "4b749d6cf964a520e7e72de3", 2452);
		ss.save(pm);
		// 34
		ss = SeptaStop.loadOrCreate(pm, "4b1d1910f964a520d80b24e3", 2453);
		ss.save(pm);
		// 30
		ss = SeptaStop.loadOrCreate(pm, "4dc1379f45dd472e9b71a891", 21532);
		ss.save(pm);
		// 15
		ss = SeptaStop.loadOrCreate(pm, "4a689388f964a52096ca1fe3", 1392);
		ss.save(pm);
		// 13
		ss = SeptaStop.loadOrCreate(pm, "4b7b50b1f964a520e95d2fe3", 2455);
		ss.save(pm);
		// 11
		ss = SeptaStop.loadOrCreate(pm, "4b984885f964a5206b3935e3", 2456);
		ss.save(pm);
		// 8
		ss = SeptaStop.loadOrCreate(pm, "4b0c961bf964a520b23f23e3", 2457);
		ss.save(pm);
		// 5
		ss = SeptaStop.loadOrCreate(pm, "4a6894a7f964a52099ca1fe3", 2458);
		ss.save(pm);
		// 2
		ss = SeptaStop.loadOrCreate(pm, "4c0ed0352466a59389b17921", 428);
		ss.save(pm);
		// spring
		ss = SeptaStop.loadOrCreate(pm, "4a2fa975f964a520cc981fe3", 2459);
		ss.save(pm);
		// girard
		ss = SeptaStop.loadOrCreate(pm, "4b281899f964a520f08e24e3", 353);
		ss.save(pm);
		// berks
		ss = SeptaStop.loadOrCreate(pm, "4b47ba7ff964a5202f3c26e3", 2460);
		ss.save(pm);
		// y-d
		ss = SeptaStop.loadOrCreate(pm, "4a6764b1f964a52056c91fe3", 2461);
		ss.save(pm);
		// huntingdon
		ss = SeptaStop.loadOrCreate(pm, "4b47bb38f964a5204d3c26e3", 2462);
		ss.save(pm);
		// somerset
		ss = SeptaStop.loadOrCreate(pm, "4b47bbddf964a520663c26e3", 797);
		ss.save(pm);
		// allegheny
		ss = SeptaStop.loadOrCreate(pm, "4b4c017af964a520ecac26e3", 60);
		ss.save(pm);
		// tioga
		ss = SeptaStop.loadOrCreate(pm, "4b4c0233f964a520f3ac26e3", 2463);
		ss.save(pm);
		// erie-torr
		ss = SeptaStop.loadOrCreate(pm, "4b54873ef964a520aebe27e3", 838);
		ss.save(pm);
		// church
		ss = SeptaStop.loadOrCreate(pm, "4b54884af964a520d5be27e3", 2464);
		ss.save(pm);

		// margaret
		ss = SeptaStop.loadOrCreate(pm, "4b54895df964a520fcbe27e3", 217);
		ss.save(pm);
		// frankford
		ss = SeptaStop.loadOrCreate(pm, "4b9460dff964a520f67634e3", 61);
		ss.save(pm);
	}

	private void getBSL() {
		// ATT
		SeptaStop ss = SeptaStop.loadOrCreate(pm, "4b15e59ef964a52067b523e3", 152);
		ss.save(pm);
		// Oregon
		ss = SeptaStop.loadOrCreate(pm, "4b15e5d1f964a5206bb523e3", 20967);
		ss.save(pm);
		// Snyder
		ss = SeptaStop.loadOrCreate(pm, "4b15e608f964a5206eb523e3", 1286);
		ss.save(pm);
		// Tasker
		ss = SeptaStop.loadOrCreate(pm, "4b15e62cf964a52070b523e3", 1285);
		ss.save(pm);
		// Ellsworth
		ss = SeptaStop.loadOrCreate(pm, "4b15e674f964a52074b523e3", 1284);
		ss.save(pm);
		// Lombard
		ss = SeptaStop.loadOrCreate(pm, "4b15e6a4f964a52075b523e3", 1283);
		ss.save(pm);
		// Walnut
		ss = SeptaStop.loadOrCreate(pm, "4b3bd1c2f964a520d67b25e3", 1282);
		ss.save(pm);
		// City
		ss = SeptaStop.loadOrCreate(pm, "4b15e8fff964a5208db523e3", 1281);
		ss.save(pm);
		// Race
		ss = SeptaStop.loadOrCreate(pm, "4b15e8c4f964a5208cb523e3", 1280);
		ss.save(pm);
		// Spring
		ss = SeptaStop.loadOrCreate(pm, "4b2c4178f964a520b3c424e3", 1279);
		ss.save(pm);
		// 8th St And Market St
		ss = SeptaStop.loadOrCreate(pm, "4b0c961bf964a520b23f23e3", 21531);
		ss.save(pm);
		// Chinatown
		ss = SeptaStop.loadOrCreate(pm, "4b15ea6df964a5209db523e3", 2440);
		ss.save(pm);
		// Fairmount
		ss = SeptaStop.loadOrCreate(pm, "4b15ea29f964a5209ab523e3", 1278);
		ss.save(pm);
		// Girard
		ss = SeptaStop.loadOrCreate(pm, "4b15eb15f964a520a1b523e3", 20966);
		ss.save(pm);
		// Cecil
		ss = SeptaStop.loadOrCreate(pm, "4aeb4ed9f964a520bcc021e3", 1277);
		ss.save(pm);
		// Susquehanna
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c2cf964a52054c424e3", 1276);
		ss.save(pm);
		// North
		ss = SeptaStop.loadOrCreate(pm, "4b15ede3f964a520c1b523e3", 2439);
		ss.save(pm);
		// Allegheny
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c68f964a52056c424e3", 142);
		ss.save(pm);
		// Erie
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c96f964a52059c424e3", 140);
		ss.save(pm);
		// Hunting
		ss = SeptaStop.loadOrCreate(pm, "4b2c3cb5f964a5205cc424e3", 1274);
		ss.save(pm);
		// Wyoming
		ss = SeptaStop.loadOrCreate(pm, "4b2c3ce0f964a52061c424e3", 1273);
		ss.save(pm);
		// Logan
		ss = SeptaStop.loadOrCreate(pm, "4b2c3d04f964a52067c424e3", 1272);
		ss.save(pm);
		// Olney
		ss = SeptaStop.loadOrCreate(pm, "4d5432468c39cbffd9785b80", 82);
		ss.save(pm);
		// Fern
		ss = SeptaStop.loadOrCreate(pm, "4ac259c8f964a520c89820e3", 20965);
		ss.save(pm);

	}

	public void getTrentonLine() {
		// // Trenton
		// SeptaStop ss = SeptaStop.loadOrCreate(pm,
		// "4d51d00b4015b1f7cb1074cf",90701);
		// ss.save(pm);
		// //Stenton
		// ss = SeptaStop.loadOrCreate(pm, "4c92774e418ea1cdea36a585",90715);
		// ss.save(pm);
		// Glenside
		// SeptaStop ss = SeptaStop.loadOrCreate(pm,
		// "4a8f172bf964a520e31320e3",90411);
		// ss.save(pm);
		// //Washington Lane
		// ss = SeptaStop.loadOrCreate(pm, "4cb071a2c5e6a1cdc4f5d4f6",90714);
		// ss.save(pm);

		// Levittown
		SeptaStop ss = SeptaStop.loadOrCreate(pm, "4b15ee32f964a520c4b523e3", 90702);
		ss.save(pm);
		// Bristol
		ss = SeptaStop.loadOrCreate(pm, "4c0f9e5f75f99c7428afecc4", 90703);
		ss.save(pm);
		// Croydon
		ss = SeptaStop.loadOrCreate(pm, "4de96af2fa76cc1b8aca6d77", 90704);
		ss.save(pm);
		// Eddington
		ss = SeptaStop.loadOrCreate(pm, "4b15ec89f964a520adb523e3", 90705);
		ss.save(pm);
		// Cornwells
		ss = SeptaStop.loadOrCreate(pm, "4b15ec41f964a520abb523e3", 90706);
		ss.save(pm);
		// Torresdale
		ss = SeptaStop.loadOrCreate(pm, "4b15ecbbf964a520b0b523e3", 90707);
		ss.save(pm);
		// Holmesburg
		ss = SeptaStop.loadOrCreate(pm, "4b15ed10f964a520b5b523e3", 90708);
		ss.save(pm);
		// Tacony
		ss = SeptaStop.loadOrCreate(pm, "4b15ed4af964a520b7b523e3", 90709);
		ss.save(pm);
		// Bridesburg
		//ss = SeptaStop.loadOrCreate(pm, "4b2c4178f964a520b3c424e3", 90710);
		//ss.save(pm);
		// West Trenton
		//ss = SeptaStop.loadOrCreate(pm, "4b0c961bf964a520b23f23e3", 90327);
		//ss.save(pm);
		// Yardley
		ss = SeptaStop.loadOrCreate(pm, "4b0c469bf964a520483a23e3", 90326);
		ss.save(pm);
		// Woodbourne
		ss = SeptaStop.loadOrCreate(pm, "4b99a2e8f964a520f38835e3", 90325);
		ss.save(pm);
		// Langhorne
		//ss = SeptaStop.loadOrCreate(pm, "4b15eb15f964a520a1b523e3", 90324);
		//ss.save(pm);
		// Trevose
		ss = SeptaStop.loadOrCreate(pm, "4bbf074998f4952178fbd163", 90322);
		ss.save(pm);
		// Somerton
		ss = SeptaStop.loadOrCreate(pm, "4bbf08aa30c99c7467845411", 90321);
		ss.save(pm);
		// Forest Hills
		ss = SeptaStop.loadOrCreate(pm, "4bc5a0196a3e9c74e30df648", 90320);
		ss.save(pm);
		// Philmont
		//ss = SeptaStop.loadOrCreate(pm, "4b2c3c68f964a52056c424e3", 90319);
		//ss.save(pm);
		// Bethayres
		ss = SeptaStop.loadOrCreate(pm, "4bbf09a5006dc9b683ecfb3f", 90318);
		ss.save(pm);
		// Meadowbrook
		ss = SeptaStop.loadOrCreate(pm, "4be32e51d27a20a10a75915b", 90317);
		ss.save(pm);
		// Rydal
		ss = SeptaStop.loadOrCreate(pm, "4ba8b6a7f964a5200ee939e3", 90316);
		ss.save(pm);
		// Noble
		ss = SeptaStop.loadOrCreate(pm, "4ba2a0e9f964a520ae0a38e3", 90315);
		ss.save(pm);
		// Doylestown
		ss = SeptaStop.loadOrCreate(pm, "4ae766b2f964a520fbaa21e3", 90538);
		ss.save(pm);
		// Delaware Valley College
		//ss = SeptaStop.loadOrCreate(pm, "4ac259c8f964a520c89820e3", 90537);
		//ss.save(pm);
		// New Britain
		ss = SeptaStop.loadOrCreate(pm, "4dcbbe83d16478749fa8c6d1", 90536);
		ss.save(pm);
		/*// Link Belt
		ss = SeptaStop.loadOrCreate(pm, "4b15e608f964a5206eb523e3", 90534);
		ss.save(pm);
		// Fortuna
		ss = SeptaStop.loadOrCreate(pm, "4b15e62cf964a52070b523e3", 90532);
		ss.save(pm);
		// Lansdale
		ss = SeptaStop.loadOrCreate(pm, "4b15e674f964a52074b523e3", 90531);
		ss.save(pm);
		// Chalfont
		ss = SeptaStop.loadOrCreate(pm, "4b15e6a4f964a52075b523e3", 90535);
		ss.save(pm);
		// Colmar
		ss = SeptaStop.loadOrCreate(pm, "4b3bd1c2f964a520d67b25e3", 90533);
		ss.save(pm);
		// Pennbrook
		ss = SeptaStop.loadOrCreate(pm, "4b15e8fff964a5208db523e3", 90530);
		ss.save(pm);
		// North Wales
		ss = SeptaStop.loadOrCreate(pm, "b15e8c4f964a5208cb523e3", 90529);
		ss.save(pm);*/
		// Penllyn
		ss = SeptaStop.loadOrCreate(pm, "4cb6efb6e262b60cb64c72e0", 90527);
		ss.save(pm);
		// Ambler
		//ss = SeptaStop.loadOrCreate(pm, "4b0c961bf964a520b23f23e3", 90526);
		//ss.save(pm);
		// Fort Washington
		ss = SeptaStop.loadOrCreate(pm, "4b1fa500f964a520a72724e3", 90525);
		ss.save(pm);
//		// Oreland
//		ss = SeptaStop.loadOrCreate(pm, "4b15ea29f964a5209ab523e3", 90524);
//		ss.save(pm);
//		// North Hills
//		ss = SeptaStop.loadOrCreate(pm, "4b15eb15f964a520a1b523e3", 90523);
//		ss.save(pm);
//		// Warminster
//		ss = SeptaStop.loadOrCreate(pm, "4aeb4ed9f964a520bcc021e3", 90417);
//		ss.save(pm);
		// Hatboro
		ss = SeptaStop.loadOrCreate(pm, "4dc673781f6ef43b8a2eeeb1", 90416);
		ss.save(pm);
		// Crestmont
		ss = SeptaStop.loadOrCreate(pm, "4c003726f61ea593b386eb13", 90414);
		ss.save(pm);
		// Roslyn
		//ss = SeptaStop.loadOrCreate(pm, "4b2c3c68f964a52056c424e3", 90413);
		//ss.save(pm);
		// Ardsley
		ss = SeptaStop.loadOrCreate(pm, "4ac09f45f964a520549420e3", 90412);
		ss.save(pm);
		// Elkins Park
		ss = SeptaStop.loadOrCreate(pm, "4f8d62cfe4b09ac7a7ddd46f", 90409);
		ss.save(pm);
		// Melrose Park
		//ss = SeptaStop.loadOrCreate(pm, "4b2c3ce0f964a52061c424e3", 90408);
		//ss.save(pm);
		// Fern Rock T C
		//ss = SeptaStop.loadOrCreate(pm, "4b2c3d04f964a52067c424e3", 90407);
		//ss.save(pm);
		// Wayne Junction
		ss = SeptaStop.loadOrCreate(pm, "4b15ef8af964a520d5b523e3", 90009);
		ss.save(pm);
		// Fox Chase
		ss = SeptaStop.loadOrCreate(pm, "4b15e289f964a5204ab523e3", 90815);
		ss.save(pm);
		// Ryers
		ss = SeptaStop.loadOrCreate(pm, "4afc9a10f964a5204d2422e3", 90814);
		ss.save(pm);
		// Cheltenham
		ss = SeptaStop.loadOrCreate(pm, "4b15e1aaf964a52046b523e3", 90813);
		ss.save(pm);
		// Lawndale
		ss = SeptaStop.loadOrCreate(pm, "4b15e366f964a5204fb523e3", 90812);
		ss.save(pm);
		// Olney
		ss = SeptaStop.loadOrCreate(pm, "4fa152a6e4b0ee815cf40c75", 90811);
		ss.save(pm);
		// Chestnut Hill East
		//ss = SeptaStop.loadOrCreate(pm, "4b15e6a4f964a52075b523e3", 90720);
		//ss.save(pm);
		// Gravers
		ss = SeptaStop.loadOrCreate(pm, "4bd0b3cb9854d13ad242f84d", 90719);
		ss.save(pm);
		// Wyndmoor
		ss = SeptaStop.loadOrCreate(pm, "4beaf3b5a9900f47732b1740", 90718);
		ss.save(pm);
		// Mount Airy
		//ss = SeptaStop.loadOrCreate(pm, "b15e8c4f964a5208cb523e3", 90717);
		//ss.save(pm);
		// Sedgwick
		//ss = SeptaStop.loadOrCreate(pm, "4b2c4178f964a520b3c424e3", 90716);
		//ss.save(pm);
		// Germantown
		ss = SeptaStop.loadOrCreate(pm, "4cb75f4e52edb1f7378b75fe", 90713);
		ss.save(pm);
		// Wister
		//ss = SeptaStop.loadOrCreate(pm, "4b15eb15f964a520a1b523e3", 90712);
		//ss.save(pm);
		// Chestnut Hill West
		//ss = SeptaStop.loadOrCreate(pm, "4aeb4ed9f964a520bcc021e3", 90801);
		//ss.save(pm);
		// Highland
		ss = SeptaStop.loadOrCreate(pm, "4bfd92c3f61dc9b625399fde", 90802);
		ss.save(pm);
		// St. Martins
		ss = SeptaStop.loadOrCreate(pm, "4b4e456af964a5209de726e3", 90803);
		ss.save(pm);
		// Allen Lane
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c68f964a52056c424e3", 90804);
		ss.save(pm);
		// Carpenter
		ss = SeptaStop.loadOrCreate(pm, "4c3aed5a5810a5936cc5b93c", 90805);
		ss.save(pm);
		// Upsal
		//ss = SeptaStop.loadOrCreate(pm, "4b2c3cb5f964a5205cc424e3", 90806);
		//ss.save(pm);
		// Tulpehocken
		ss = SeptaStop.loadOrCreate(pm, "4c06ace08b4520a19bc68597", 90807);
		ss.save(pm);
		// Chelten Avenue
		ss = SeptaStop.loadOrCreate(pm, "4c0645a3761ac9b6dda42074", 90808);
		ss.save(pm);
		// Queen Lane
		ss = SeptaStop.loadOrCreate(pm, "4d5432468c39cbffd9785b80", 90809);
		ss.save(pm);
		// Norristown
		//ss = SeptaStop.loadOrCreate(pm, "4ac259c8f964a520c89820e3", 90228);
		//ss.save(pm);
		// Main Street
		ss = SeptaStop.loadOrCreate(pm, "4fbbeaeee4b0d636a5de76b7", 90227);
		ss.save(pm);
		// Norristown T.C
		ss = SeptaStop.loadOrCreate(pm, "4b101806f964a520ed6823e3", 90226);
		ss.save(pm);
		// Conshohocken
		//ss = SeptaStop.loadOrCreate(pm, "4b15e62cf964a52070b523e3", 90225);
		//ss.save(pm);
		// Spring Mill
		ss = SeptaStop.loadOrCreate(pm, "4bb3b4492397b713b98038b3", 90224);
		ss.save(pm);
		// Miquon
		ss = SeptaStop.loadOrCreate(pm, "4b9e831ef964a52074ea36e3", 90223);
		ss.save(pm);
		// Ivy Ridge
		//ss = SeptaStop.loadOrCreate(pm, "4b3bd1c2f964a520d67b25e3", 90222);
		//ss.save(pm);
		// Manayunk
		ss = SeptaStop.loadOrCreate(pm, "4ec110b20aaf6450bb3b36d9", 90221);
		ss.save(pm);
		// Wissahickon
		//ss = SeptaStop.loadOrCreate(pm, "b15e8c4f964a5208cb523e3", 90220);
		//ss.save(pm);
		// East Falls
		ss = SeptaStop.loadOrCreate(pm, "4ba0d675f964a5202c8137e3", 90219);
		ss.save(pm);
		// Allegheny
		ss = SeptaStop.loadOrCreate(pm, "4c00552d369476b08b488f1f", 90218);
		ss.save(pm);
		// Downingtown
		//ss = SeptaStop.loadOrCreate(pm, "4b15ea6df964a5209db523e3", 90502);
		//ss.save(pm);
		// Whitford
		ss = SeptaStop.loadOrCreate(pm, "4baa0241f964a52068433ae3", 90503);
		ss.save(pm);
		// Exton
		//ss = SeptaStop.loadOrCreate(pm, "4b15eb15f964a520a1b523e3", 90504);
		//ss.save(pm);
		// Malvern
		ss = SeptaStop.loadOrCreate(pm, "4a7a0876f964a5203ee81fe3", 90505);
		ss.save(pm);
		// Paoli
		//ss = SeptaStop.loadOrCreate(pm, "4b2c3c2cf964a52054c424e3", 90506);
		//ss.save(pm);
		// Daylesford
		ss = SeptaStop.loadOrCreate(pm, "4b9bd729f964a520ca2b36e3", 90507);
		ss.save(pm);
		// Berwyn
		ss = SeptaStop.loadOrCreate(pm, "4ac494bef964a520be9e20e3", 90508);
		ss.save(pm);
		// Devon
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c96f964a52059c424e3", 90509);
		ss.save(pm);
		// Strafford
		//ss = SeptaStop.loadOrCreate(pm, "4b2c3cb5f964a5205cc424e3", 90510);
		//ss.save(pm);
		// Wayne
		//ss = SeptaStop.loadOrCreate(pm, "4b2c3ce0f964a52061c424e3", 90511);
		//ss.save(pm);
		// St. Davids
		ss = SeptaStop.loadOrCreate(pm, "4d4088eca799ba7a77ded160", 90512);
		ss.save(pm);
		// Radnor
		ss = SeptaStop.loadOrCreate(pm, "4aea0d68f964a520dbb821e3", 90513);
		ss.save(pm);
		// Villanova
		ss = SeptaStop.loadOrCreate(pm, "4ba7d6c4f964a520ddb739e3", 90514);
		ss.save(pm);
		// Rosemont
		ss = SeptaStop.loadOrCreate(pm, "4b15e5d1f964a5206bb523e3", 90515);
		ss.save(pm);
		// Bryn Mawr
		ss = SeptaStop.loadOrCreate(pm, "4bb744911344b71354819e04", 90516);
		ss.save(pm);
		// Haverford
		//ss = SeptaStop.loadOrCreate(pm, "4b15e62cf964a52070b523e3", 90517);
		//ss.save(pm);
		// Ardmore
		//ss = SeptaStop.loadOrCreate(pm, "4b15e674f964a52074b523e3", 90518);
		//ss.save(pm);
		// Wynnewood
		ss = SeptaStop.loadOrCreate(pm, "4bc3069774a9a593faeed3f6", 90519);
		ss.save(pm);
		// Narberth
		ss = SeptaStop.loadOrCreate(pm, "4aa8df7bf964a520c05120e3", 90520);
		ss.save(pm);
		// Merion
		ss = SeptaStop.loadOrCreate(pm, "4bf9b7035efe2d7fd2b86c34", 90521);
		ss.save(pm);
		// Overbrook
		ss = SeptaStop.loadOrCreate(pm, "4ed378e56c2510ace47f776f", 90522);
		ss.save(pm);
		// 30th Street Station,
		ss = SeptaStop.loadOrCreate(pm, "4d0e98a43d45b1f76a1ea0f2", 90004);
		ss.save(pm);
		// Suburban Station
		ss = SeptaStop.loadOrCreate(pm, "4a6889d0f964a52085ca1fe3", 90005);
		ss.save(pm);
		// Market East
		ss = SeptaStop.loadOrCreate(pm, "4a4e167cf964a52019ae1fe3", 90006);
		ss.save(pm);
		// Temple
		ss = SeptaStop.loadOrCreate(pm, "4b05d44af964a52013e422e3", 90007);
		ss.save(pm);
		/*// North Broad
		ss = SeptaStop.loadOrCreate(pm, "4b15eb15f964a520a1b523e3", 90008);
		ss.save(pm);
		// Cynwyd
		ss = SeptaStop.loadOrCreate(pm, "4aeb4ed9f964a520bcc021e3", 90001);
		ss.save(pm);
		// Bala
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c2cf964a52054c424e3", 90002);
		ss.save(pm);
		// Wynnefield Avenue
		ss = SeptaStop.loadOrCreate(pm, "4b15ede3f964a520c1b523e3", 90003);
		ss.save(pm);
		// University City
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c68f964a52056c424e3", 90406);
		ss.save(pm);
		// 49th Street
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c96f964a52059c424e3", 90314);
		ss.save(pm);
		// Angora
		ss = SeptaStop.loadOrCreate(pm, "4b2c3cb5f964a5205cc424e3", 90313);
		ss.save(pm);
		// Fernwood-Yeadon
		ss = SeptaStop.loadOrCreate(pm, "4b2c3ce0f964a52061c424e3", 90312);
		ss.save(pm);
		// Lansdowne
		ss = SeptaStop.loadOrCreate(pm, "4b2c3d04f964a52067c424e3", 90311);
		ss.save(pm);
		// Gladstone
		ss = SeptaStop.loadOrCreate(pm, "4d5432468c39cbffd9785b80", 90310);
		ss.save(pm);
		// Clifton-Aldan
		ss = SeptaStop.loadOrCreate(pm, "4ac259c8f964a520c89820e3", 90309);
		ss.save(pm);
		// Primos
		ss = SeptaStop.loadOrCreate(pm, "4b15e5d1f964a5206bb523e3", 90308);
		ss.save(pm);
		// Secane
		ss = SeptaStop.loadOrCreate(pm, "4b15e608f964a5206eb523e3", 90307);
		ss.save(pm);
		// Morton-Rutledge
		ss = SeptaStop.loadOrCreate(pm, "4b15e62cf964a52070b523e3", 90306);
		ss.save(pm);
		// Swarthmore
		ss = SeptaStop.loadOrCreate(pm, "4b15e674f964a52074b523e3", 90305);
		ss.save(pm);
		// Wallingford
		ss = SeptaStop.loadOrCreate(pm, "4b15e6a4f964a52075b523e3", 90304);
		ss.save(pm);
		// Moylan-Rose Valley
		ss = SeptaStop.loadOrCreate(pm, "4b3bd1c2f964a520d67b25e3", 90303);
		ss.save(pm);
		// Media
		ss = SeptaStop.loadOrCreate(pm, "4b15e8fff964a5208db523e3", 90302);
		ss.save(pm);
		// Elwyn
		ss = SeptaStop.loadOrCreate(pm, "b15e8c4f964a5208cb523e3", 90301);
		ss.save(pm);
		// Darby
		ss = SeptaStop.loadOrCreate(pm, "4b2c4178f964a520b3c424e3", 90217);
		ss.save(pm);
		// Curtis Park
		ss = SeptaStop.loadOrCreate(pm, "4b0c961bf964a520b23f23e3", 90216);
		ss.save(pm);
		// Sharon Hill
		ss = SeptaStop.loadOrCreate(pm, "4b15ea6df964a5209db523e3", 90215);
		ss.save(pm);
		// Folcroft
		ss = SeptaStop.loadOrCreate(pm, "4b15ea29f964a5209ab523e3", 90214);
		ss.save(pm);
		// Glenolden
		ss = SeptaStop.loadOrCreate(pm, "4b15eb15f964a520a1b523e3", 90213);
		ss.save(pm);
		// Norwood
		ss = SeptaStop.loadOrCreate(pm, "4aeb4ed9f964a520bcc021e3", 90212);
		ss.save(pm);
		// Prospect Park - Moore
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c2cf964a52054c424e3", 90211);
		ss.save(pm);
		// Ridley Park
		ss = SeptaStop.loadOrCreate(pm, "4b15ede3f964a520c1b523e3", 90210);
		ss.save(pm);
		// Crum Lynne
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c68f964a52056c424e3", 90209);
		ss.save(pm);
		// Eddystone
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c96f964a52059c424e3", 90208);
		ss.save(pm);
		// Chester
		ss = SeptaStop.loadOrCreate(pm, "4b2c3cb5f964a5205cc424e3", 90207);
		ss.save(pm);
		// Highland Avenue
		ss = SeptaStop.loadOrCreate(pm, "4b2c3ce0f964a52061c424e3", 90206);
		ss.save(pm);
		// Marcus Hook
		ss = SeptaStop.loadOrCreate(pm, "4b2c3d04f964a52067c424e3", 90205);
		ss.save(pm);
		// Claymont
		ss = SeptaStop.loadOrCreate(pm, "4d5432468c39cbffd9785b80", 90204);
		ss.save(pm);
		// Wilmington
		ss = SeptaStop.loadOrCreate(pm, "4ac259c8f964a520c89820e3", 90203);
		ss.save(pm);
		// Newark
		ss = SeptaStop.loadOrCreate(pm, "4b15e5d1f964a5206bb523e3", 90201);
		ss.save(pm);
		// Eastwick
		ss = SeptaStop.loadOrCreate(pm, "4b15e608f964a5206eb523e3", 90405);
		ss.save(pm);
		// Airport Terminal A,
		ss = SeptaStop.loadOrCreate(pm, "4b15e62cf964a52070b523e3", 90404);
		ss.save(pm);
		// Airport Terminal B
		ss = SeptaStop.loadOrCreate(pm, "4b15e674f964a52074b523e3", 90403);
		ss.save(pm);
		// Airport Terminal CD
		ss = SeptaStop.loadOrCreate(pm, "4b15e6a4f964a52075b523e3", 90402);
		ss.save(pm);
		// Airport Terminal ef
		ss = SeptaStop.loadOrCreate(pm, "4b3bd1c2f964a520d67b25e3", 90401);
		ss.save(pm);
		// Churchman's Crossing
		ss = SeptaStop.loadOrCreate(pm, "b15e8c4f964a5208cb523e3", 90202);
		ss.save(pm);
		// Neshaminy Falls
		ss = SeptaStop.loadOrCreate(pm, "4b2c4178f964a520b3c424e3", 90323);
		ss.save(pm);
		// Willow Grove
		ss = SeptaStop.loadOrCreate(pm, "4b0c961bf964a520b23f23e3", 90415);
		ss.save(pm);
		// Gwynedd Valley
		ss = SeptaStop.loadOrCreate(pm, "4b15ea6df964a5209db523e3", 90528);
		ss.save(pm);
		// North Philadelphia
		ss = SeptaStop.loadOrCreate(pm, "4b15ea29f964a5209ab523e3", 90810);
		ss.save(pm);
		// North Philadelphia Amtrak
		ss = SeptaStop.loadOrCreate(pm, "4b15eb15f964a520a1b523e3", 90711);
		ss.save(pm);
		// Thorndale
		ss = SeptaStop.loadOrCreate(pm, "4aeb4ed9f964a520bcc021e3", 90501);
		ss.save(pm);
		// Jenkintown Wyncote
		ss = SeptaStop.loadOrCreate(pm, "4b2c3c2cf964a52054c424e3", 90410);
		ss.save(pm);*/

	}
	public void getBuses() {
		// 57
		SeptaStop ss = SeptaStop.loadOrCreate(pm, "4bf542df94af2d7f5b793b72", 57);
		ss.save(pm);
		// 5
		ss = SeptaStop.loadOrCreate(pm, "4e1e4aa4fa76920758acf9f4", 5);
		ss.save(pm);
	}
}
