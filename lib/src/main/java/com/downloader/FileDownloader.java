package com.downloader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by danger on 15/9/18.
 */
public class FileDownloader {
    int MAX_DOWNLOADING_TASKS = 3; // 最多同时进行的任务 默认3个
    int MAX_THREAD = 3; // 一个任务同时下载线程数

    private Map<String, DownloadTask> tasks; // = Collections.synchronizedList(new ArrayList<DownloadTask>());
    private Map<String, DownloadTaskInfo> taskInfos; // = Collections.synchronizedList(new ArrayList<DownloadTask>());
    private ExecutorService executor;

    private static FileDownloader instance;

    private FileDownloader() {
        tasks = Collections.synchronizedMap(new HashMap<String, DownloadTask>());
        taskInfos = Collections.synchronizedMap(new HashMap<String, DownloadTaskInfo>());

        executor = Executors.newFixedThreadPool((MAX_THREAD + 1) * MAX_DOWNLOADING_TASKS);// 限制线程池大小为7的线程池
    }

    public static FileDownloader getInstance() {
        if (instance == null) {
            synchronized (FileDownloader.class) {
                instance = new FileDownloader();
            }
        }
        return instance;
    }

    public int getMaxDownloadingTasks() {
        return MAX_DOWNLOADING_TASKS;
    }

    public void setMaxDownloadingTasks(int maxDownloadingTasks) {
        this.MAX_DOWNLOADING_TASKS = maxDownloadingTasks;
    }

    public int getMaxThread() {
        return MAX_THREAD;
    }

    public void setMaxThread(int maxThread) {
        this.MAX_THREAD = maxThread;
    }

    public boolean taskExist(String url) {
        return getTaskByUrl(url) != null;
    }

    public DownloadTask getTaskByUrl(String url) {
        synchronized (tasks) {
            for (DownloadTask task : tasks.values()) {
                if (task.getInfo().getFileUrl().equals(url)) {
                    return task;
                }
            }
        }

        return null;
    }

    public void addListener(DownloadListener listener) {
        for (DownloadTask task : tasks.values()) {
            task.addListener(listener);
        }
    }

    /**
     * 添加任务的监听器
     *
     * @param url
     * @param listener
     */
    public void addListener(String url, DownloadListener listener) {
        DownloadTask task = getTaskByUrl(url);
        if (task != null) {
            task.addListener(listener);
        }
    }

    /**
     * 删除任务的监听器
     *
     * @param listener
     */
    public void removeListener(DownloadListener listener) {
        for (DownloadTask task : tasks.values()) {
            task.removeListener(listener);
        }
    }

    /**
     * 删除任务的监听器
     *
     * @param url
     * @param listener
     */
    public void removeListener(String url, DownloadListener listener) {
        DownloadTask task = getTaskByUrl(url);
        if (task != null) {
            task.removeListener(listener);
        }
    }

    private boolean doAddTask(String url, String saveFileName, DownloadListener listener) {
        DownloadTaskInfo info = taskInfos.get(url);
        DownloadTask task = tasks.get(url);

        // 已经在下载当中了
        if (task != null && task.getInfo().getStatus() == DownloadTaskInfo.TaskStatus.DOWNLOADING) {
            addListener(url, listener);
            return true;
        }

        if (info == null) {
            info = new DownloadTaskInfo();
            info.setFileUrl(url);
            info.setFilePathName(saveFileName);
            taskInfos.put(url, info);
        }

        if (task == null) {
            task = new DownloadTask(info);
        }
        info.setStatus(DownloadTaskInfo.TaskStatus.PENDING);
        tasks.put(url, task);
        task.addListener(listener);
        return false;
    }


    /**
     * @param url          文件的下载地址
     * @param saveFileName 文件保存路径
     * @param listener     下载事件监听器
     */
    public void startDownload(String url, String saveFileName, DownloadListener listener) {
        if (doAddTask(url, saveFileName, listener)) return;
        schedule();
    }


    /**
     * 优先级非常 高的下载任务，不受调度器管理，立即下载，慎重使用
     *
     * @param url          文件的下载地址
     * @param saveFileName 文件保存路径
     * @param listener     下载事件监听器
     */
    public void startDownloadRightNow(String url, String saveFileName, DownloadListener listener) {

        if (doAddTask(url, saveFileName, listener)) return;
        // task.start();
        DownloadTask task = getTaskByUrl(url);
        if (task != null) {
            task.start();
        }
    }

    /**
     * 暂停下载
     *
     * @param url
     */
    public void pauseTask(String url) {
        DownloadTask task = getTaskByUrl(url);
        if (task != null) {
            task.pause();
        }
    }

    public void resumeTask(String url) {
        DownloadTask task = getTaskByUrl(url);
        DownloadTaskInfo taskInfo = taskInfos.containsKey(url) ? taskInfos.get(url) : null;
        if (task != null && taskInfo != null) {
            startDownload(url, taskInfo.getFilePathName(), null);
        }
    }

    /**
     * 进行一次调度
     */
    public void schedule() {
        int runningTaskCount = 0;
        for (DownloadTask task : tasks.values()) {
            if (task.getInfo().getStatus() == DownloadTaskInfo.TaskStatus.DOWNLOADING) {
                runningTaskCount++;
            }
            if (runningTaskCount >= MAX_DOWNLOADING_TASKS) {
                return;
            }
        }

        for (DownloadTask task : tasks.values()) {
            if (task.getInfo().getStatus() == DownloadTaskInfo.TaskStatus.PENDING) {
                task.start();
                return;
            }
        }
    }

    ExecutorService getExecutor() {
        return executor;
    }

    void removeTask(String fileUrl) {

        if (tasks.containsKey(fileUrl)) {
            tasks.remove(fileUrl);
        }
    }


}
