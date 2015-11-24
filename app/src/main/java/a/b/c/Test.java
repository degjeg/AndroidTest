package a.b.c;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class Test {
    private String uname;
    private String pw;
    private Integer type;
    private Long date;
    private Boolean allow;
    private Float x;
    private Float y;
    private UInfo info1;
    private UInfo info2;

    public void setUname(String uname) {
        this.uname = uname;
    }

    public String getUname() {
        return uname;
    }

    public void setPw(String pw) {
        this.pw = pw;
    }

    public String getPw() {
        return pw;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Integer getType() {
        return type;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public Long getDate() {
        return date;
    }

    public void setAllow(Boolean allow) {
        this.allow = allow;
    }

    public Boolean getAllow() {
        return allow;
    }

    public void setX(Float x) {
        this.x = x;
    }

    public Float getX() {
        return x;
    }

    public void setY(Float y) {
        this.y = y;
    }

    public Float getY() {
        return y;
    }

    public void setInfo1(UInfo info1) {
        this.info1 = info1;
    }

    public UInfo getInfo1() {
        return info1;
    }

    public void setInfo2(UInfo info2) {
        this.info2 = info2;
    }

    public UInfo getInfo2() {
        return info2;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject jobj = new JSONObject();
        jobj.put("uname", uname); // put not matter empty or not
        jobj.put("pw", pw); // put not matter empty or not
        if (type == null) { // put not matter empty or not
            jobj.put("type", 0); // put not matter empty or not
        } else {
            jobj.put("type", type);
        }
        if (date != null) {
            jobj.put("date", date);
        }
        if (allow == null) { // put not matter empty or not
            jobj.put("allow", 0); // put not matter empty or not
        } else {
            jobj.put("allow", allow);
        }
        if (x != null) {
            jobj.put("x", x);
        }
        if (y == null) { // put not matter empty or not
            jobj.put("y", 0); // put not matter empty or not
        } else {
            jobj.put("y", y);
        }
        if (info1 != null) {
            jobj.put("info1", info1.toJson());
        }
        if (info2 == null) { // put not matter empty or not
            jobj.put("info2", new JSONObject()); // put not matter empty or not
        } else {
            jobj.put("info2", info2.toJson());
        }
        return jobj;
    }

    public static Test fromJson(String jsonString) throws JSONException {
        JSONObject jobj = new JSONObject(jsonString);
        return fromJson(jobj);
    }

    public static Test fromJson(JSONObject jobj) throws JSONException {
        Test obj = new Test();
        if (jobj.has("uname")) {
            obj.uname = jobj.getString("uname");
        }
        if (jobj.has("pw")) {
            obj.pw = jobj.getString("pw");
        }
        if (jobj.has("type")) {
            obj.type = jobj.getInt("type");
        }
        if (jobj.has("date")) {
            obj.date = jobj.getLong("date");
        }
        if (jobj.has("allow")) {
            obj.allow = jobj.getBoolean("allow");
        }
        if (jobj.has("x")) {
            obj.x = (float) jobj.getDouble("x");
        }
        if (jobj.has("y")) {
            obj.y = (float) jobj.getDouble("y");
        }
        if (jobj.has("info1")) {
            obj.info1 = UInfo.fromJson(jobj.getJSONObject("info1"));
        }
        if (jobj.has("info2")) {
            obj.info2 = UInfo.fromJson(jobj.getJSONObject("info2"));
        }
        return obj;
    }

    public String toUrl() throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder();
        sb.append("uname=");
        if (uname != null) {
            sb.append(URLEncoder.encode(uname, "utf-8"));
        }
        sb.append("&");
        sb.append("pw=");
        if (pw != null) {
            sb.append(URLEncoder.encode(pw, "utf-8"));
        }
        sb.append("&");
        sb.append("type=");
        if (type != null) {
            sb.append(type);
        }
        sb.append("&");
        if (date != null) {
            sb.append("date=");
            sb.append(date);
            sb.append("&");
        }
        sb.append("allow=");
        if (allow != null) {
            sb.append(allow);
        }
        sb.append("&");
        if (x != null) {
            sb.append("x=");
            sb.append(x);
            sb.append("&");
        }
        sb.append("y=");
        if (y != null) {
            sb.append(y);
        }
        sb.append("&");
        if (info1 != null) {
            sb.append("info1=");
            sb.append(info1);
            sb.append("&");
        }
        sb.append("info2=");
        if (info2 != null) {
            sb.append(info2);
        }
        sb.append("&");

        if (sb.length() > 0) { // delete the last & 
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

}
