package com.beangenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Dengjun on 2015/10/23.
 */
public class BeanGenerator {
    public static final String NL = "\r\n";

    public static final int SERIAL_Json = 3;
    public static final int SERIAL_Json1 = 1;
    public static final int SERIAL_Json2 = 2;

    public static final int SERIAL_Parcelable = 4;

    public static final int SERIAL_Xml = 24;
    public static final int SERIAL_Xml1 = 8;
    public static final int SERIAL_Xml2 = 16;

    public static final int SERIAL_URL = 96;
    public static final int SERIAL_URL1 = 32;
    public static final int SERIAL_URL2 = 64;

    public int serialType = SERIAL_Json1 | SERIAL_Json2 | SERIAL_URL1;
    public int indent = 0;


    public BeanGenerator(int serialType) {
        this.serialType = serialType;
    }

    public void generate(String dir, String packageName, List<DataBean> list) throws IOException {
        String realDir = dir + "/" + packageName.replace(".", "/");

        File dirFile = new File(realDir);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        for (DataBean dataBean : list) {
            StringBuilder sb = new StringBuilder();
            File clsFile = new File(realDir, dataBean.clsName + ".java");
            sb.append("package ").append(packageName).append(";").append(NL).append(NL);

            if ((dataBean.getSerialType() & SERIAL_Json) != 0) {
                sb.append("import org.json.JSONException;" + NL);
                sb.append("import org.json.JSONObject;" + NL);
                sb.append(NL);
            }

            if ((dataBean.getSerialType() & SERIAL_URL) != 0) {
                sb.append("import java.io.UnsupportedEncodingException;" + NL);
                sb.append("import java.net.URLEncoder;" + NL);
            }
            sb.append(NL);

            genOneDataBean(dataBean, sb);
            System.out.print(sb);
            FileOutputStream fos = new FileOutputStream(clsFile);
            fos.write(sb.toString().getBytes("utf-8"));
        }
    }

    private void genOneDataBean(DataBean dataBean, Appendable appendable) throws IOException {
        appendable.append(String.format("public class %s {" + NL, dataBean.getClsName()));

        indentRight();
        // 属性定义
        for (Property property : dataBean.getProperties()) {
            String typeName = property.getType() == PropertyType.Custom ?
                    property.getDataBean().clsName : property.getType().toString();

            appendSpaces(appendable);
            appendable.append(String.format("private %s %s;%s", typeName, property.getName(), NL));
        }
        indentLeft();

        appendable.append(NL);
        // getter and setter
        for (Property property : dataBean.getProperties()) {
            String typeName = property.getType() == PropertyType.Custom ?
                    property.getDataBean().clsName : property.getType().toString();

            appendable.append(String.format("    public void set%s(%s %s) {" + NL
                            + "        this.%s = %s;" + NL
                            + "    }" + NL + NL
                    , CapitalFirst(property.getName())
                    , typeName
                    , property.getName()
                    , property.getName()
                    , property.getName()
            ));

            appendable.append(String.format("    public %s get%s() {" + NL
                            + "        return %s;" + NL
                            + "    }" + NL + NL
                    , typeName
                    , CapitalFirst(property.getName())
                    , property.getName()
            ));
        }
        writeJsonSerial(appendable, dataBean);
        writeUrlSerial(appendable, dataBean);


        appendable.append("}").append(NL); // end of class
    }


    private void writeJsonSerial(Appendable appendable, DataBean dataBean) throws IOException {
        // json serialize
        if ((dataBean.getSerialType() & SERIAL_Json) == 0) { // 生成 json 序列化代码
            return;
        }

        if ((dataBean.getSerialType() & SERIAL_Json1) != 0) {
            appendable.append("    public JSONObject toJson() throws JSONException {" + NL);

            appendable.append("        JSONObject jobj = new JSONObject();" + NL);
            for (Property property : dataBean.getProperties()) {
                if ((property.getWriteMode() & DataBean.WRITE_NULL) != 0) { // write null
                    if (property.getType() == PropertyType.String) {
                        appendable.append(String.format("        jobj.put(\"%s\", %s); // put not matter empty or not" + NL
                                , property.getName()
                                , property.getName()
                        ));
                    } else {
                        appendable.append(String.format("        if (%s == null) { // put not matter empty or not" + NL
                                , property.getName()
                        ));
                        if (property.getType() != PropertyType.String
                                && property.getType() != PropertyType.Custom) {
                            appendable.append(String.format("            jobj.put(\"%s\", 0); // put not matter empty or not" + NL
                                    , property.getName()
                            ));
                        } else {
                            appendable.append(String.format("            jobj.put(\"%s\", new JSONObject()); // put not matter empty or not" + NL
                                    , property.getName()
                            ));
                        }

                        appendable.append("        } else {" + NL);
                        if (property.getType() == PropertyType.Custom) {
                            appendable.append(String.format("            jobj.put(\"%s\", %s.toJson());" + NL
                                    , property.getName()
                                    , property.getName()
                            ));
                        } else {
                            appendable.append(String.format("            jobj.put(\"%s\", %s);" + NL
                                    , property.getName()
                                    , property.getName()
                            ));
                        }
                        appendable.append("        }" + NL);
                    }
                } else {
                    appendable.append(String.format("        if (%s != null) {" + NL
                            , property.getName()
                    ));

                    if (property.getType() == PropertyType.Custom) {
                        appendable.append(String.format("            jobj.put(\"%s\", %s.toJson());" + NL
                                , property.getName()
                                , property.getName()
                        ));
                    } else {
                        appendable.append(String.format("            jobj.put(\"%s\", %s);" + NL
                                , property.getName()
                                , property.getName()
                        ));
                    }
                    appendable.append("        }" + NL);
                }

            }
            appendable.append("        return jobj;" + NL);
            appendable.append("    }" + NL + NL); // end of getJon Method
        }


        if ((dataBean.getSerialType() & SERIAL_Json2) != 0) {
            // 生成 fromJson
            appendable.append(String.format("    public static %s fromJson(String jsonString) throws JSONException {" + NL
                    , dataBean.getClsName()
            ));
            appendable.append("        JSONObject jobj = new JSONObject(jsonString);" + NL);
            appendable.append("        return fromJson(jobj);" + NL);
            appendable.append("    }" + NL + NL);

            appendable.append(String.format("    public static %s fromJson(JSONObject jobj) throws JSONException {" + NL
                    , dataBean.getClsName()
            ));

            appendable.append(String.format("        %s obj = new %s();" + NL
                    , dataBean.getClsName()
                    , dataBean.getClsName()
            ));


            for (Property property : dataBean.getProperties()) {
                appendable.append(String.format("        if (jobj.has(\"%s\")) {" + NL
                        , property.getName()
                ));
                if (property.getType() == PropertyType.Custom) {
                    appendable.append(String.format("            obj.%s = %s.fromJson(jobj.getJSONObject(\"%s\"));" + NL
                            , property.getName()
                            , property.getDataBean().clsName
                            , property.getName()
                    ));
                } else if (property.getType() == PropertyType.Integer) {
                    appendable.append(String.format("            obj.%s = jobj.get%s(\"%s\");" + NL
                            , property.getName()
                            , "Int"
                            , property.getName()
                    ));
                } else if (property.getType() == PropertyType.Float) {
                    appendable.append(String.format("            obj.%s = (float) jobj.get%s(\"%s\");" + NL
                            , property.getName()
                            , "Double"
                            , property.getName()
                    ));
                } else {
                    appendable.append(String.format("            obj.%s = jobj.get%s(\"%s\");" + NL
                            , property.getName()
                            , property.getType().toString()
                            , property.getName()
                    ));
                }

                appendable.append("        }" + NL); //
            }
            appendable.append("        return obj;" + NL);
            appendable.append("    }" + NL + NL); // end of getJon Method
        }
    }


    private void writeUrlSerial(Appendable appendable, DataBean dataBean) throws IOException {
        if ((dataBean.getSerialType() & SERIAL_URL1) != 0) { // to url

            appendable.append("    public String toUrl() throws UnsupportedEncodingException {" + NL);
            appendable.append("        StringBuilder sb = new StringBuilder();" + NL);

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < dataBean.getProperties().size(); i++) {
                Property property = dataBean.getProperties().get(i);
                if ((property.getWriteMode() & DataBean.WRITE_NULL) == 0) { // 不写空串
                    builder.append(String.format("        if (%s != null) {" + NL
                            , property.getName()
                    ));
                    builder.append(String.format("            sb.append(\"%s=\");" + NL
                            , property.getName()
                            , property.getName()
                    ));
                    if (property.getType() == PropertyType.String) {
                        builder.append(String.format("            sb.append(URLEncoder.encode(%s, \"utf-8\"));" + NL
                                , property.getName()
                        ));
                    } else {
                        builder.append(String.format("            sb.append(%s);" + NL
                                , property.getName()
                        ));
                    }
                    builder.append("            sb.append(\"&\");" + NL);
                    builder.append("        }" + NL);
                } else {

                    builder.append(String.format("        sb.append(\"%s=\");" + NL
                            , property.getName()
                            , property.getName()
                    ));

                    builder.append(String.format("        if (%s != null) {" + NL
                            , property.getName()
                    ));

                    if (property.getType() == PropertyType.String) {
                        builder.append(String.format("            sb.append(URLEncoder.encode(%s, \"utf-8\"));" + NL
                                , property.getName()
                        ));
                    } else {
                        builder.append(String.format("            sb.append(%s);" + NL
                                , property.getName()
                        ));
                    }
                    builder.append("        }" + NL);
                    builder.append("        sb.append(\"&\");" + NL);
                    //  appendable.append("            StringBuilder sb = new StringBuilder();" + NL);
                }
            }

            appendable.append(builder.toString() + NL);
            appendable.append("        if (sb.length() > 0) { // delete the last & " + NL);
            appendable.append("            sb.deleteCharAt(sb.length() - 1);" + NL);
            appendable.append("        }" + NL);

            appendable.append("        return sb.toString();" + NL);
            appendable.append("    }" + NL + NL);
        }

    }

    private void indentRight() {
        indent++;
    }

    private void indentLeft() {
        indent--;
    }

    private void appendSpaces(Appendable appendable) throws IOException {
        for (int i = 0; i < indent; i++) {
            appendable.append("    ");
        }
    }

    private String CapitalFirst(String s) {
        char c = s.charAt(0);
        if (c >= 'a' && c <= 'z') {
            c = (char) (c - 'a' + 'A');
        }
        return c + s.substring(1);
    }

}
