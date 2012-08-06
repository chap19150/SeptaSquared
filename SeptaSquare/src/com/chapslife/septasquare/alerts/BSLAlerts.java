package com.chapslife.septasquare.alerts;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.chapslife.septasquare.push.models.LinkedUser;
import com.chapslife.septasquare.push.util.Common;
import com.google.gson.stream.JsonReader;

@SuppressWarnings("serial")
public class BSLAlerts extends HttpServlet {

	private static final Logger log = Logger.getLogger(BSLAlerts.class.getName());
	private String code = "d";
	private String reqs = "f";
	private LinkedUser u;
	private PersistenceManager pm;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();

		String line = null;
		StringBuilder sb = new StringBuilder();
		StringBuilder alertDiv = new StringBuilder();
		String advisory = "";
		try {
			URL url = new URL("http://www3.septa.org/hackathon/Alerts/get_alert_data.php?req1=rr_route_bsl");
			BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
			JsonReader readerd = new JsonReader(reader);
			readerd.beginArray();
			while (readerd.hasNext()) {
				readerd.beginObject();
				while (readerd.hasNext()) {
					String name = readerd.nextName();
					if (name.equals("route_name")) {
						log.warning(readerd.nextString());
					} else if (name.equals("advisory_message")) {
						advisory = readerd.nextString();
						log.warning(advisory);
					} else {
						readerd.skipValue(); // avoid some unhandle events
					}
				}
				readerd.endObject();
			}
			readerd.endArray();
		} catch (MalformedURLException e) {

		} catch (IOException e) {
		}

		//writeHead(out, "BSL Alerts");
		//writeConnectPrompt(out);
		if(advisory.length() > 1){
			writeAdvisory(out, advisory);
		}
		// log.warning(alertDiv.toString());
		//writeFoot(out, null, null);
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

	private void writeAdvisory(PrintWriter out, String advisory) {
		out.println("<!doctype html><html>\n<head>\n<title>");
		out.println("BSL Alerts");
		out.println("</title>\n<link rel=\"stylesheet\" href=\"http://twitter.github.com/bootstrap/1.4.0/bootstrap.min.css\"/>");
		out.println("<link type=\"text/css\" rel=\"stylesheet\" media=\"all\" href=\"/static/style.css\"/>");
		out.println("<script type=\"text/javascript\" src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.7.0/jquery.min.js\"></script>");
		out.println("<script type=\"text/javascript\" src=\"/_ah/channel/jsapi\"></script>");
		out.println("<script type=\"text/javascript\" src=\"/static/script.js\"></script>");
		out.println("</head><body>\n<div id=\"wrapper\" class=\"container-fluid\">");
		out.println(advisory);
		out.print("</div></body></html>");
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
