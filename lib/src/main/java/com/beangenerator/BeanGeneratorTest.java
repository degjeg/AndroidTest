package com.beangenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Dengjun on 2015/10/23.
 */
public class BeanGeneratorTest {

    public static void main(String arg[]) {

        try {
            int serialType = BeanGenerator.SERIAL_Json | BeanGenerator.SERIAL_URL;
            BeanGenerator beanGenerator = new BeanGenerator(serialType);
            DataBean dataBean = new DataBean("Test", 1);
            dataBean.setSerialType(serialType);
            dataBean.addProperty(PropertyType.String, "uname", 1);
            dataBean.addProperty(PropertyType.String, "pw", 1);
            dataBean.addProperty(PropertyType.Integer, "type", 1);
            dataBean.addProperty(PropertyType.Long, "date", 0);
            dataBean.addProperty(PropertyType.Boolean, "allow", 1);
            dataBean.addProperty(PropertyType.Float, "x", 0);
            dataBean.addProperty(PropertyType.Float, "y");

            DataBean infoBean = new DataBean("UInfo", 0);
            infoBean.setSerialType(serialType);
            infoBean.addProperty(PropertyType.Float, "lat");
            infoBean.addProperty(PropertyType.Float, "lon");
            infoBean.addProperty(PropertyType.String, "nick");

            dataBean.addProperty(infoBean, "info1", 0);
            dataBean.addProperty(infoBean, "info2", 1);

            List<DataBean> list = new ArrayList<>();
            list.add(dataBean);
            list.add(infoBean);

            beanGenerator.generate("app/src/main/java", "a.b.c", list);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
