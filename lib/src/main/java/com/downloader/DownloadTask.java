package com.downloader;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by danger on 15/9/18.
 */
public class DownloadTask implements Runnable {
    private static final String TAG = "FileDownloader";
    private DownloadTaskInfo info;
    private List<WeakReference<DownloadListener>> listeners;
    private List<BlockDownloadTask> blockDownloadTasks = Collections.synchronizedList(new ArrayList<BlockDownloadTask>());
    private File destFile;
    private File tempFile;
    private File infoFile;
    HttpURLConnection urlConnection;
    private boolean needNotify = false;
    final private AtomicBoolean keepRunning = new AtomicBoolean(true);
    final private Handler handler;
    final Semaphore semaphore = new Semaphore(1);
    long startTime;// 开始时间，秒

    private String taskId;

    // private Handler mainThreadHandler;

    public DownloadTask(DownloadTaskInfo info) {
        this.info = info;
        handler = new Handler(Looper.getMainLooper());

        destFile = new File(info.getFilePathName());
        tempFile = new File(info.getFilePathName() + ".tmp");
        infoFile = new File(info.getFilePathName() + ".info");

        taskId = String.format("%x", info.getFileUrl().hashCode());
    }

    public DownloadTaskInfo getInfo() {
        return info;
    }

    public void setInfo(DownloadTaskInfo info) {
        this.info = info;
    }

    public void addListener(DownloadListener listener) {
        if (listener == null) {
            return;
        }
        if (listeners == null) {
            listeners = Collections.synchronizedList(new ArrayList<WeakReference<DownloadListener>>());
        }
        Iterator<WeakReference<DownloadListener>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<DownloadListener> oneListener = iterator.next();
            if (oneListener.get() == null) {
                listeners.remove(oneListener);
            }
            if (oneListener.get() == listener) {
                return;
            }
        }
        listeners.add(new WeakReference<DownloadListener>(listener));
    }


    public void removeListener(DownloadListener listener) {
        Iterator<WeakReference<DownloadListener>> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<DownloadListener> oneListener = iterator.next();
            if (oneListener.get() == listener) {
                listeners.remove(oneListener);
            }
        }
    }


    public void start() {
        info.setStatus(DownloadTaskInfo.TaskStatus.DOWNLOADING);

        FileDownloader.getInstance().getExecutor().submit(this);
        startTime = SystemClock.elapsedRealtime();
    }

    public void notifyListener() {
        if (listeners == null || listeners.size() == 0) {
            semaphore.release();
            return;
        }

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }

        // Logcat.w(TAG, String.format("[%s] notify", taskId));
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    Iterator<WeakReference<DownloadListener>> iterator = listeners.iterator();
                    while (iterator.hasNext()) {
                        WeakReference<DownloadListener> oneListener = iterator.next();
                        if (oneListener.get() == null) {
                            listeners.remove(oneListener);
                        } else {
                            oneListener.get().onDownloadEvent(DownloadTask.this);
                        }
                    }
                } finally {
                    semaphore.release();
                }

            }
        });
    }

    private void clear() {

        Logcat.w(TAG, String.format("[%s] clear", taskId));
        synchronized (keepRunning) {
            keepRunning.set(false);
            keepRunning.notifyAll();
        }

        if (urlConnection != null) {
            urlConnection.disconnect();
            urlConnection = null;
        }

        synchronized (blockDownloadTasks) {
            Iterator<BlockDownloadTask> iterator = blockDownloadTasks.iterator();
            while (iterator.hasNext()) {
                BlockDownloadTask task = iterator.next();
                iterator.remove();
                task.cancel();
            }

            blockDownloadTasks.clear();
        }

        info.clearSpeed();
        FileDownloader.getInstance().removeTask(info.getFileUrl());
        FileDownloader.getInstance().schedule();
    }

    /**
     * 暂停任务
     */
    public void pause() {
        setTaskStatus(DownloadTaskInfo.TaskStatus.PAUSED);
        clear();
    }

    /**
     * 删除任务(会删除已下载的临时文件）
     */
    public void cancel() {
        setTaskStatus(DownloadTaskInfo.TaskStatus.NONE);
        clear();

        if (!destFile.delete()) {
            Logcat.w(TAG, String.format("[%s] unable to delete:%s", taskId, destFile));
        }

        if (!infoFile.delete()) {
            Logcat.w(TAG, String.format("[%s] unable to delete:", taskId, infoFile));
        }

    }

    void setTaskStatus(int status) {
        info.setStatus(status);
    }

    @Override
    public void run() {
        try {
            doDownload();
        } catch (Exception e) {
            Logcat.e(TAG, String.format("task:[%s]", taskId), e);
        } finally {
            clear();
        }
    }

    private void doDownload() {
        int length = 0;
        length = getFileLength();

        if (!keepRunning.get()) {
            return;
        }
        Logcat.d(TAG, String.format("[%s]len=%d ->%s", taskId, length, info.getFileUrl()));
        if (length <= 0) { // 无法获得远程文件大小，停止下载过程
            notifyListener();
            return;
        }

        if (!keepRunning.get()) {
            return;
        }
        loadInfo();
        if (info.getFileLength() > 0
                && (info.getFileLength() != length
                || info.getFileLength() != tempFile.length())) {
            if (removeOldFile()) { // 无法删除旧的文件
                notifyListener();
                return;
            }
        }
        info.setFileLength(length); // 无法创建临时文件

        if (!keepRunning.get()) {
            return;
        }

        if (!createTempFile()) {
            notifyListener();
            return;
        }

        int threadCount = FileDownloader.getInstance().MAX_THREAD;
        if (info.getBlockInfoList().size() > 0) {
            threadCount = info.getBlockInfoList().size();
        } else {
            int blockSize = length / threadCount;
            int i = 0;
            for (; i < threadCount - 1; i++) {
                int startPos = blockSize * i;
                info.getBlockInfoList().add(new DownloadTaskInfo.BlockInfo(startPos, startPos + blockSize));
            }
            info.getBlockInfoList().add(new DownloadTaskInfo.BlockInfo(blockSize * i, length));
        }


        if (!keepRunning.get()) {
            return;
        }

        for (int i = 0; i < info.getBlockInfoList().size(); i++) { // TODO
            DownloadTaskInfo.BlockInfo blockInfo = info.getBlockInfoList().get(i);

            Logcat.d(TAG, String.format("[%s]block start[%d-%d-%d]", taskId,
                    blockInfo.startPos, blockInfo.position, blockInfo.endPosition));

            if (blockInfo.position >= blockInfo.endPosition) { // 该块已经下载完成
                continue;
            }

            BlockDownloadTask blockDownloadTask = new BlockDownloadTask(blockInfo);
            blockDownloadTasks.add(blockDownloadTask);
            FileDownloader.getInstance().getExecutor().submit(blockDownloadTask);
        }
        try {
            while (haveKeepRunningBlockTask()) {

                // Logcat.d(TAG, String.format("[%s]need:%s", taskId, needNotify ? "Y" : "N"));
                long used = SystemClock.elapsedRealtime() - startTime; // 目前已经使用的时间

                if (used / 1000 % 2 == 0) {
                    info.updateSpeedInfo();
                }


                if (needNotify) {
                    info.updateSpeedInfo();
                    notifyListener();

                    String jsonString = info.toJsonString();
                    FileUtil.saveToFile(infoFile, jsonString);

                    needNotify = false;
                }

                synchronized (keepRunning) {
                    if (keepRunning.get()) {
                        keepRunning.wait(200);
                    }
                }
            }

            if (isFinished()) {
                info.updateSpeedInfo();
                tempFile.renameTo(destFile);
                infoFile.delete();
                setTaskStatus(DownloadTaskInfo.TaskStatus.FINISHED);
                notifyListener();
            }

        } catch (InterruptedException e) {
            // e.printStackTrace();
        } catch (Exception e) {
            // e.printStackTrace();
        }
    }

    private boolean isFinished() {
        for (DownloadTaskInfo.BlockInfo blockInfo : info.getBlockInfoList()) {
            if (blockInfo.position < blockInfo.endPosition) {
                return false;
            }
        }

        return true;
    }


    private boolean removeOldFile() {
        Logcat.w(TAG, String.format("[%s] remove older file", taskId));
        if (destFile.exists()) {
            if (!destFile.delete()) {
                Logcat.w(TAG, String.format("[%s] remove old file fail", taskId));

                info.setStatus(DownloadTaskInfo.TaskStatus.FAILED);
                info.setErrorCode(DownloadTaskInfo.ErrorCode.DELETE_OLDER_FILE);
                return true;
            }
        }

        if (tempFile.exists()) {
            if (!tempFile.delete()) {
                Logcat.w(TAG, String.format("[%s] remove old temp file fail", taskId));
                info.setStatus(DownloadTaskInfo.TaskStatus.FAILED);
                info.setErrorCode(DownloadTaskInfo.ErrorCode.DELETE_OLDER_TEMP_FILE);
                return true;
            }
        }
        return false;
    }

    private boolean createTempFile() {
        if (!tempFile.exists()) {
            try {
                File dir = tempFile.getParentFile();
                if (!dir.exists() && !dir.mkdirs()) {
                    info.setStatus(DownloadTaskInfo.TaskStatus.FAILED);
                    info.setErrorCode(DownloadTaskInfo.ErrorCode.CREATE_DIR);
                    return false;
                }
                if (!tempFile.createNewFile()) {
                    info.setStatus(DownloadTaskInfo.TaskStatus.FAILED);
                    info.setErrorCode(DownloadTaskInfo.ErrorCode.CREATE_FILE);
                    return false;
                }
                RandomAccessFile randomAccessFile = new RandomAccessFile(tempFile, "rw");
                randomAccessFile.setLength(info.getFileLength());
                randomAccessFile.close();
                return true;
            } catch (FileNotFoundException e) {
                // e.printStackTrace();
                Logcat.e(TAG, String.format("[%s ] create file %s", taskId, info.getFilePathName()), e);
                info.setStatus(DownloadTaskInfo.TaskStatus.FAILED);
                info.setErrorCode(DownloadTaskInfo.ErrorCode.CREATE_FILE);
            } catch (IOException e) {
                Logcat.e(TAG, String.format("[%s ] create temp %s", taskId, info.getFilePathName()), e);
                info.setStatus(DownloadTaskInfo.TaskStatus.FAILED);
                info.setErrorCode(DownloadTaskInfo.ErrorCode.CREATE_FILE);
            }
            return false;
        }
        return true;
    }

    public int getFileLength() {
        int length = -1000;
        int httpCode = 0;
        try {
            URL url = new URL(info.getFileUrl());
            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.setRequestProperty("Accept-Encoding", "urlConnection");
            urlConnection.setDoInput(false);
            urlConnection.setDoOutput(false);
            urlConnection.connect();

            length = urlConnection.getContentLength();

            httpCode = urlConnection.getResponseCode();

            info.setHttpCode(httpCode);
            if (httpCode >= 400) {
                length = -1002;
            }

        } catch (IOException e) {
            Logcat.e(TAG, String.format("[%s] connect", taskId), e);

        } catch (NullPointerException e) {
            length = -1003;
        } catch (Exception e) {
            Logcat.e(TAG, String.format("[%s] connect", taskId), e);

        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
                urlConnection = null;
            }
        }

        if (length <= 0 && length != -1003) {
            info.setErrorCode(DownloadTaskInfo.ErrorCode.NET);
            info.setStatus(DownloadTaskInfo.TaskStatus.FAILED);
            info.setHttpCode(httpCode);
        }
        info.setFileLength(length);
        return length;
    }


    class BlockDownloadTask implements Runnable {
        boolean isRunning = true;
        final AtomicBoolean keepRunning = new AtomicBoolean(true);

        DownloadTaskInfo.BlockInfo blockInfo;

        RandomAccessFile randomAccessFile;
        HttpURLConnection blockUrlConnection;
        InputStream inputStream;


        public BlockDownloadTask(DownloadTaskInfo.BlockInfo blockInfo) {
            this.blockInfo = blockInfo;
        }

        private void doDownload() {
            try {
                URL url = new URL(info.getFileUrl());

                blockUrlConnection = (HttpURLConnection) url.openConnection();
                blockUrlConnection.addRequestProperty("RANGE", String.format("bytes=%d-%d", blockInfo.position, blockInfo.endPosition));

                blockUrlConnection.connect();

                int httpCode = blockUrlConnection.getResponseCode();
                Logcat.d(TAG, String.format("[%s][%d] http%d", taskId,
                        blockInfo.startPos, httpCode));
                if (httpCode >= 400) {
                    keepRunning.set(false);
                    return;
                }

                isRunning = true;
                inputStream = blockUrlConnection.getInputStream();
                byte data[] = new byte[20 * 1024]; // 20K
                int readLength = 1;

                while (keepRunning.get() && readLength > 0) {
                    readLength = inputStream.read(data);
                    if (readLength > 0) {
                        isRunning = true;
                        needNotify = true;
                        randomAccessFile.write(data, 0, readLength);
                        blockInfo.position += readLength;
                    }

                    Logcat.d(TAG, String.format("[%s][%d] %d-%d-%d", taskId, blockInfo.startPos,
                            readLength, blockInfo.position, blockInfo.endPosition));
                }

                if (blockInfo.position >= blockInfo.endPosition) { // block download finished
                    keepRunning.set(false);
                    return;
                }
            } catch (MalformedURLException e) {
                // e.printStackTrace();
                Logcat.e(TAG, String.format("[%s][%s]", taskId, blockInfo.startPos), e);
            } catch (SocketException e) { // canceled
                // e.printStackTrace();
                keepRunning.set(false);
                Logcat.e(TAG, String.format("[%s][%s]", taskId, blockInfo.startPos), e);
            } catch (IOException e) {
                // e.printStackTrace();
                Logcat.e(TAG, String.format("[%s][%s]", taskId, blockInfo.startPos), e);
            } catch (NullPointerException e) { // canceled
                // e.printStackTrace();
                Logcat.e(TAG, String.format("[%s][%s]", taskId, blockInfo.startPos), e);
                keepRunning.set(false);
            } catch (Exception e) {
                // e.printStackTrace();
                Logcat.e(TAG, String.format("[%s][%s]", taskId, blockInfo.startPos), e);
            } finally {
                Logcat.d(TAG, String.format("[%s][%d]finish %d-%d", taskId, blockInfo.startPos,
                        blockInfo.position, blockInfo.endPosition));

                isRunning = false;
            }
        }

        @Override
        public void run() {
            try {
                randomAccessFile = new RandomAccessFile(tempFile, "rw");
            } catch (FileNotFoundException e) {
                Logcat.e(TAG, String.format("[%s] Error Open File:%s", taskId, getInfo().getFilePathName()), e);
                isRunning = false;
                keepRunning.set(false);
                cancel();
                needNotify = true;
                return;
                // e.printStackTrace();
            } catch (Exception e) {
                Logcat.e(TAG, String.format("[%s] Error Open File:%s", taskId, getInfo().getFilePathName()), e);
                isRunning = false;
                keepRunning.set(false);
                cancel();
                needNotify = true;
                return;
            }

            try {
                randomAccessFile.seek(blockInfo.position);
            } catch (IOException e) {
                Logcat.e(TAG, String.format("[%s] Error seek:%s", taskId, getInfo().getFilePathName()), e);
                isRunning = false;
                keepRunning.set(false);
                cancel();
                return;
            } catch (Exception e) {
                Logcat.e(TAG, String.format("[%s] Error seek:%s", taskId, getInfo().getFilePathName()), e);
                isRunning = false;
                keepRunning.set(false);
                cancel();
                return;
            }

            long triedTimes = 0;

            try {
                while (keepRunning.get()
                        && (haveRunningBlockTask() && triedTimes < 10000)) {
                    doDownload();

                    try {
                        synchronized (keepRunning) {
                            if (keepRunning.get()) {
                                keepRunning.wait(2000);
                            }
                        }
                        triedTimes += 2000;
                    } catch (InterruptedException e) {
                        // e.printStackTrace();
                        return;
                    }
                }
            } finally {
                cancel();
            }
        }

        public void cancel() {

            Logcat.e(TAG, String.format("[%s][%d] cancel", taskId, blockInfo.startPos));

            isRunning = false;

            synchronized (keepRunning) {
                keepRunning.set(false);
                keepRunning.notifyAll();
            }

            if (blockUrlConnection != null) {
                blockUrlConnection.disconnect();
                blockUrlConnection = null;
            }

            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // e.printStackTrace(); should not happen
                }
                inputStream = null;
            }

            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    // e.printStackTrace(); should not happen
                }
                randomAccessFile = null;
            }

            synchronized (blockDownloadTasks) {
                if (blockDownloadTasks.contains(this)) {
                    blockDownloadTasks.remove(this);
                }
            }
        }

    }

    private boolean haveRunningBlockTask() {
        for (BlockDownloadTask task : blockDownloadTasks) {
            if (task.isRunning) {
                return true;
            }
        }

        return false;
    }

    private boolean haveKeepRunningBlockTask() {
        for (BlockDownloadTask task : blockDownloadTasks) {
            if (task.keepRunning.get()) {
                return true;
            }
        }

        return false;
    }

    private void saveInfo() {
        String str = info.toJsonString();
        if (str != null) {
            FileUtil.saveToFile(infoFile, str);
        }
    }

    private void loadInfo() {
        String str = FileUtil.loadFileToString(infoFile);
        if (str != null) {
            info.parse(str);
        }
    }

    @Override
    public String toString() {
        return String.format("[%s]sts=%d code=%d %d/%d speed:%dB/S",
                taskId, info.getStatus(), info.getErrorCode(),
                info.getDownloaded(), info.getFileLength(), info.getSpeed());
    }
}
