package server;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import util.Crawler;
import util.TParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;

public class IdCheckerHandler extends HttpServlet {
    static final String QUERY = "https://admin.remop.info/adminjk/dashboard/quick_view?query=";
    static final String PROFILE = "https://admin.remop.info/adminjk/users/{USER}/read_profile";
    static final String REFERRALS = "https://admin.remop.info/adminjk/users/{USER}/read_referrals";

    static JSONObject check(String user, String sessionId) {
        System.out.println("Checking: " + user);
        user = user.replaceAll("\\s", "");
        try {
            JSONObject o = new JSONObject();
            String content = Crawler.getContentFromUrl(QUERY + URLEncoder.encode(user, "UTF-8"), new HashMap<>() {{
                put("cookie", "_admin_session=" + sessionId);
            }}, "GET", null, null);
            String html = new JSONObject(content).getString("html");

            try {
                String id = TParser.getContent(html, "<user-status-control user-id='", "'");
                o.put("id", id == null ? "null" : id);
            } catch (Exception e) {
                o.put("id", "null");
            }

            String username = null;
            try {
                username = TParser.getContent(html, "<a href=\"/adminjk/users/", "\"");
                o.put("user", username == null ? "null" : username);
            } catch (Exception e) {
                o.put("user", "null");
            }

            try {
                Document doc = Jsoup.parse(html);
                String email = doc.select("tbody>tr>td>span").get(1).text();
                o.put("email", email == null ? "null" : email);
            } catch (Exception e) {
                o.put("email", "null");
            }

            if (username != null) {
                content = Crawler.getContentFromUrl(PROFILE.replace("{USER}", URLEncoder.encode(username, "UTF-8")), new HashMap<>() {{
                    put("cookie", "_admin_session=" + sessionId);
                }}, "GET", null, null);
                html = new JSONObject(content).getString("html");
                try {
                    Document doc = Jsoup.parse(html);
                    String mlmEnabled = doc.selectFirst(".row-mlm_enabled td inline-boolean-attr").attr("attr-value");
                    o.put("mlmEnabled", mlmEnabled == null ? "null" : mlmEnabled);
                    String mlmCertificated = doc.selectFirst(".row-mlm_certificated td span").text();
                    o.put("mlmCertificated", mlmCertificated == null ? "null" : mlmCertificated);
                } catch (Exception e) {
                    o.put("mlmEnabled", "null");
                    o.put("mlmCertificated", "null");
                }
            } else {
                o.put("mlmEnabled", "null");
                o.put("mlmCertificated", "null");
            }

            if (username != null) {

                content = Crawler.getContentFromUrl(REFERRALS.replace("{USER}", URLEncoder.encode(username, "UTF-8")), new HashMap<>() {{
                    put("cookie", "_admin_session=" + sessionId);
                }}, "GET", null, null);
                html = new JSONObject(content).getString("html");
                try {
                    String referrals = "0";
                    if (html.contains("downline referee")) {
                        referrals = TParser.getContent(html, "View ", " downline referee");
                    }
                    o.put("referees", referrals == null ? "null" : referrals);
                } catch (Exception e) {
                    o.put("referees", "null");
                }
            } else {
                o.put("referees", "null");
            }
            return o;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        System.out.println(check("ngocle6993@gmail.com", "3855623a66e0d7fac81bebb7529f34ac"));
    }


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        httpServletResponse.setCharacterEncoding("utf-8");
        String user = request.getParameter("user");
        String sid = request.getParameter("sid");
        JSONObject o = check(user, sid);
        httpServletResponse.getWriter().print(o.toString());
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
    }
}
