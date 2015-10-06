package com.downloader;

import android.os.SystemClock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by danger on 15/9/18.
 */
public class DownloadTaskInfo {
    private static final int TIME_FOR_CALCULATE_SEED = 10; // 速度为N秒内的平均值

    private static final String TAG = "DownloadTaskInfo";
    private String fileUrl; // 文件的地址
    private String filePathName; // 文件保存位置
    private int fileLength = 0; // 文件大小
    private int downloaded = 0; // 已下载的字节数
    private int taskName; // 任务名称

    private Object exInfo; // 用户设置的任务扩展信息
    private List<BlockInfo> blockInfoList = Collections.synchronizedList(new ArrayList<BlockInfo>());


    /**
     * @see TaskStatus
     */
    private int status = 0; // 状态
    private int speed = 0; // 下载速度 单位（字节/秒）
    private List<Map.Entry<Integer, Integer>> speedInfo = new ArrayList<>();// new int[TIME_FOR_CALCULATE_SEED]; // N秒内每一秒所下载的字节数

    /**
     * @see ErrorCode
     */
    private int errorCode;
    private int httpCode;




    public static class BlockInfo { // 多线程同时下载时，单个线程的数据

        public BlockInfo() {
        }

        public BlockInfo(int startPos, int endPosition) {
            this.startPos = startPos;
            this.position = startPos;
            this.endPosition = endPosition;
        }

        public int startPos; // 开始位置
        public int position; // 当前位置
        public int endPosition; // 结束位置
    }

    public interface TaskStatus {
        int NONE = 0;
        int PENDING = 1; // 排队中
        int PAUSED = 2;
        int FAILED = 3;
        int FINISHED = 4;
        int DOWNLOADING = 5;
    }

    public interface ErrorCode {
        int NET = 1; // 网络连接错误
        int TIMEOUT = 2; // 超时
        int HTTP = 3; // HTTP错误，未获得200等返回码
        int HTTP_CONTENT_LENGTH = 4; // 未获得ContentLength
        int CREATE_DIR = 5; // 创建文件夹错误
        int CREATE_FILE = 6; // 创建文件错误
        int WRITE_FILE = 7; // 创建文件错误
        int DELETE_OLDER_FILE = 8;
        int DELETE_OLDER_TEMP_FILE = 9;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getFilePathName() {
        return filePathName;
    }

    public void setFilePathName(String filePathName) {
        this.filePathName = filePathName;
    }

    public int getFileLength() {
        return fileLength;
    }

    public void setFileLength(int fileLength) {
        this.fileLength = fileLength;
    }

    public int getDownloaded() {
        return downloaded;
    }

    public void setDownloaded(int downloaded) {
        this.downloaded = downloaded;
    }

    public int getTaskName() {
        return taskName;
    }

    public void setTaskName(int taskName) {
        this.taskName = taskName;
    }

    public Object getExInfo() {
        return exInfo;
    }

    public void setExInfo(Object exInfo) {
        this.exInfo = exInfo;
    }

    public List<BlockInfo> getBlockInfoList() {
        return blockInfoList;
    }

    public void setBlockInfoList(List<BlockInfo> blockInfoList) {
        this.blockInfoList = blockInfoList;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public void setHttpCode(int httpCode) {
        this.httpCode = httpCode;
    }


    /**
     * 更新速度信息
     */
    public void updateSpeedInfo() {
        Integer currentTimeInSeconds = (int) (SystemClock.elapsedRealtime() / 1000);

        int downloadedOld = downloaded;

        downloaded = 0;
        for (BlockInfo blockInfo : blockInfoList) {
            downloaded += blockInfo.position - blockInfo.startPos;
        }
        downloaded = Math.min(downloaded, fileLength);
        int downloadedThis = downloaded - downloadedOld;

        int speedTmp = 0;

        Map.Entry<Integer, Integer> lastEntry = null;
        if (speedInfo.size() > 0) { // 填补0
            lastEntry = speedInfo.get(speedInfo.size() - 1);

            int last = lastEntry.getKey();

            for (int i = Math.max(last + 1, currentTimeInSeconds - TIME_FOR_CALCULATE_SEED + 1); i < currentTimeInSeconds; i++) {
                speedInfo.add(new AbstractMap.SimpleEntry<Integer, Integer>(i, 0));
            }
        }

        if (lastEntry == null) {
            speedInfo.add(new AbstractMap.SimpleEntry<Integer, Integer>(currentTimeInSeconds, downloadedThis));
        } else {
            lastEntry.setValue(lastEntry.getValue() + downloadedThis);
        }

        while (speedInfo.size() > TIME_FOR_CALCULATE_SEED) {
            speedInfo.remove(0);
        }

        Iterator<Map.Entry<Integer, Integer>> iterator = speedInfo.iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Integer> entry = iterator.next();
            speedTmp += entry.getValue();
        }

        int start = speedInfo.get(0).getKey();
        int end = speedInfo.get(speedInfo.size() - 1).getKey();
        int diff = Math.max(1, end - start);

        speed = speedTmp / diff;
    }


    public void clearSpeed() {
        speedInfo.clear();
    }

    public String toJsonString() {
        JSONObject jsonObject = new JSONObject();
        JSONArray blocks = new JSONArray();

        for (BlockInfo blockInfo : blockInfoList) {
            JSONArray block = new JSONArray();
            block.put(blockInfo.startPos);
            block.put(blockInfo.position);
            block.put(blockInfo.endPosition);

            blocks.put(block);
        }
        try {
            jsonObject.put("blocks", blocks);
            jsonObject.put("len", fileLength);
        } catch (JSONException e) {
            // should not happen e.printStackTrace();
        }

        return jsonObject.toString();
    }

    public void parse(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            fileLength = jsonObject.getInt("len");
            JSONArray blocks = jsonObject.getJSONArray("blocks");
            blockInfoList.clear();
            downloaded = 0;

            for (int i = 0; i < blocks.length(); i++) {
                JSONArray block = blocks.getJSONArray(i);
                BlockInfo blockInfo = new BlockInfo();
                blockInfo.startPos = block.getInt(0);
                blockInfo.position = block.getInt(1);
                blockInfo.endPosition = block.getInt(2);
                blockInfoList.add(blockInfo);

                downloaded += blockInfo.position - blockInfo.startPos;
            }
        } catch (JSONException e) {
            // Logcat.e(TAG, "", e); // should not happen
        }
    }
}
