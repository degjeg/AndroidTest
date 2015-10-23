package a.b.c;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UInfo {
    private Float lat;
    private Float lon;
    private String nick;

    public void setLat(Float lat) {
        this.lat = lat;
    }

    public Float getLat() {
        return lat;
    }

    public void setLon(Float lon) {
        this.lon = lon;
    }

    public Float getLon() {
        return lon;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getNick() {
        return nick;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jobj = new JSONObject();
        if (lat != null) {
            jobj.put("lat", lat);
        }
        if (lon != null) {
            jobj.put("lon", lon);
        }
        if (nick != null) {
            jobj.put("nick", nick);
        }
        return jobj;
    }

    public static UInfo fromJson(String jsonString) throws JSONException {
        JSONObject jobj = new JSONObject(jsonString);
        return fromJson(jobj);
    }

    public static UInfo fromJson(JSONObject jobj) throws JSONException {
        UInfo obj = new UInfo();
        if (jobj.has("lat")) {
            obj.lat = (float) jobj.getDouble("lat");
        }
        if (jobj.has("lon")) {
            obj.lon = (float) jobj.getDouble("lon");
        }
        if (jobj.has("nick")) {
            obj.nick = jobj.getString("nick");
        }
        return obj;
    }

    public String toUrl() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        if (lat != null) {
            sb.append("lat=");
            sb.append(lat);
            sb.append("&");
        }
        if (lon != null) {
            sb.append("lon=");
            sb.append(lon);
            sb.append("&");
        }
        if (nick != null) {
            sb.append("nick=");
            sb.append(URLEncoder.encode(nick, "utf-8"));
            sb.append("&");
        }

        if (sb.length() > 0) { // delete the last & 
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

}
