package scanner;


import com.downloader.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApkScanner {

    class ApkFile {
        List<String> imageFiles = new ArrayList<>();
        String iconFile = null;
        String description = null;
        String apkFile = null;
        String name = null;
        int id;

    }


    public static void main(String arg[]) throws IOException {
        if (arg == null || arg.length < 3) {
            usage();
            return;
        }

        String type = arg[1];
        String fromDir = arg[2];
        String toDir = arg[3];

        if (type.equals("test") || type.equals("run")) {
            usage();
            return;
        }


        ApkScanner scanner = new ApkScanner();
        HashMap<String, List<ApkFile>> apkFiles = scanner.scanFiles(arg[1]);

        if (type.equals("run")) {
            for (Map.Entry<String, List<ApkFile>> set : apkFiles.entrySet()) {

                StringBuilder sqlBuilder = new StringBuilder();


                for (ApkFile oneApk : set.getValue()) {
                    scanner.renameFiles(oneApk);

                    sqlBuilder.append(scanner.buildSql(oneApk));
                }

                File sqlFile = new File(set.getKey(), "sql.txt");
                FileUtil.saveToFile(sqlFile, sqlBuilder.toString());
            }
        }
    }

    private String buildSql(ApkFile oneApk) {

        File iconFile = new File(oneApk.iconFile);
        StringBuilder imagesBuilder = new StringBuilder();

        for (String img : oneApk.imageFiles) {
            File imgFile = new File(img);
            imagesBuilder.append("image/");
            imagesBuilder.append(imgFile.getName());
            imagesBuilder.append(",");
        }
        imagesBuilder.deleteCharAt(imagesBuilder.length() - 1);
        String s = String.format("insert into ttt values(id, name, desc,icon,images)values(%d, %s, %s, %s)\n",
                oneApk.id, // id
                oneApk.name, // 名字
                oneApk.description, // 描述
                iconFile.getName(), // 图标
                imagesBuilder.toString()// 图片

        );

        return s;
    }

    private void renameFiles(ApkFile oneApk) {
        File oldApkFile = new File(oneApk.apkFile); // rename apk file
        File destApkFile = new File(oldApkFile.getParentFile(), String.format("%d.apk", oneApk.id));
        if (oldApkFile.getAbsolutePath().equals(destApkFile.getAbsolutePath())) {
            oldApkFile.renameTo(destApkFile);
        }
        oneApk.apkFile = destApkFile.getAbsolutePath();


    }

    private static void usage() {
        System.out.println(" usage:");
        System.out.println(" [test/run] fromdir to dir");
    }

    public HashMap<String, List<ApkFile>> scanFiles(String dir) {
        File fileDir = new File(dir);
        return scanFiles(fileDir);
    }

    public HashMap<String, List<ApkFile>> scanFiles(File fileDir) {
        if (fileDir.exists()) {
            System.out.println(String.format("dir %s not exist", fileDir));
            return null;
        }

        File filesInDir[] = fileDir.listFiles();

        HashMap<String, List<ApkFile>> returnData = new HashMap<>();
        HashMap<Integer, ApkFile> infos = new HashMap<>();
        if (parseIds(fileDir.getAbsolutePath(), infos)) {

            List<ApkFile> apkFilesInDir = new ArrayList<>();
            for (File oneFile : filesInDir) {
                if (oneFile.isDirectory()) {
                    ApkFile oneApk = scanApkDir(oneFile, infos);
                    if (oneApk != null) {
                        apkFilesInDir.add(oneApk);
                    }
                }
            }
            if (apkFilesInDir.size() > 0) {
                returnData.put(fileDir.getAbsolutePath(), apkFilesInDir);
            }
        } else {
            for (File oneFile : filesInDir) {
                if (oneFile.isDirectory()) {
                    scanFiles(oneFile);
                }
            }
        }

        return returnData;
    }

    public boolean parseIds(String dir, HashMap<Integer, ApkFile> infos) {
        File infoFile = new File(dir, "ids.txt");
        if (!infoFile.exists()) {
            return false;
        }

        String idsString = FileUtil.loadFileToString(infoFile);

        String lines[] = idsString.split("\n");
        if (lines == null || lines.length == 0) {
            return false;
        }

        for (String onLine : lines) {
            String idAndName[] = onLine.split("\n");
            if (idAndName == null || idAndName.length < 2) {
                continue;
            }
            ApkFile apkType = new ApkFile();
            apkType.id = Integer.valueOf(idAndName[0]);
            apkType.name = idAndName[1];
            infos.put(apkType.id, apkType);
        }

        return infos.size() > 0;
    }

    public ApkFile scanApkDir(File apkDir, HashMap<Integer, ApkFile> infos) {
        ApkFile apkFile = null;
        for (ApkFile oneFile : infos.values()) {
            if (oneFile.name.trim().equals(apkDir.getName().trim())) {
                apkFile = oneFile;
                break;
            }
        }
        if (apkFile == null) {
            System.out.println(String.format("%s 未设置id", apkDir.getName()));
            return null;
        }

        File files[] = apkDir.listFiles(); // 所有的文件

        for (File oneFile : files) {
            if (oneFile.getName().endsWith(".apk")) { // 找到apk文件
                apkFile.apkFile = oneFile.getAbsolutePath();
            } else if (oneFile.getName().endsWith(".txt")) { // 说明文件
                apkFile.description = FileUtil.loadFileToString(oneFile);
            } else if (oneFile.getName().contains("icon") || oneFile.getName().contains("launcher")) {
                apkFile.iconFile = oneFile.getAbsolutePath();
            } else if (oneFile.getName().endsWith(".png") || oneFile.getName().endsWith(".jpg")) {
                apkFile.imageFiles.add(oneFile.getAbsolutePath());
            }
        }

        if (apkFile.apkFile == null) {
            System.out.println(String.format("%s apk 文件未找到", apkDir.getName()));
            return null;
        } else if (apkFile.iconFile == null) {
            System.out.println(String.format("%s 图标未找到", apkDir.getName()));
            return null;
        } else if (apkFile.description == null) {
            System.out.println(String.format("%s 描述未找到", apkDir.getName()));
            return null;
        } else if (apkFile.imageFiles.size() == 0) {
            System.out.println(String.format("%s 图片未找到", apkDir.getName()));
            return null;
        }

        return apkFile;
    }

}
