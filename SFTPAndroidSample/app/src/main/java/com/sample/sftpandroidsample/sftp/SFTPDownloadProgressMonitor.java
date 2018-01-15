package com.sample.sftpandroidsample.sftp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.jcraft.jsch.SftpProgressMonitor;

import static com.sample.sftpandroidsample.MainActivity.MESSAGE_COUNT;
import static com.sample.sftpandroidsample.MainActivity.MESSAGE_END;
import static com.sample.sftpandroidsample.MainActivity.MESSAGE_INIT;
import static com.sample.sftpandroidsample.MainActivity.TAG;

public class SFTPDownloadProgressMonitor implements SftpProgressMonitor {

    private SFTPDownloadProgressMonitor() {
    }

    public static SFTPDownloadProgressMonitor create(Handler handler, String fileName) {
        SFTPDownloadProgressMonitor v = new SFTPDownloadProgressMonitor();
        v.handler = handler;
        v.fileName = fileName;
        return v;
    }

    private Handler handler;
    private String fileName, dest;
    private long max = 0, count = 0, percent = 0;

    @Override
    public void init(int op, String src, String dest, long max) {
        this.dest = dest;
        this.max = max;
        Log.i(TAG, "==================================================================================");
        Log.d(TAG, "Downloading...");
        Log.i(TAG, "Source Path = " + src);
        Log.i(TAG, "Destination Path = " + dest);
        Log.i(TAG, "File Size = " + bytesToMB(this.max));
        Log.i(TAG, "==================================================================================");
        if (handler != null) {
            Message msg = handler.obtainMessage(MESSAGE_INIT);
            Bundle bundle = new Bundle();
            bundle.putString("fileName", fileName);
            bundle.putString("src", src);
            bundle.putString("dest", dest);
            bundle.putString("max", bytesToMB(this.max));
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    @Override
    public boolean count(long bytes) {
        this.count += bytes;
        long percentNow = this.count * 100 / max;
        if (percentNow > this.percent) {
            this.percent = percentNow;
            Log.d(TAG, "Download Percentage = " + this.percent + "%");
            Log.d(TAG, "Downloaded File Size = " + bytesToMB(this.count));
        }
        if (handler != null) {
            handler.removeMessages(MESSAGE_INIT);
            Message msg = handler.obtainMessage(MESSAGE_COUNT);
            Bundle bundle = new Bundle();
            bundle.putString("percentage", String.valueOf(this.percent));
            bundle.putString("fileSize", String.valueOf(bytesToMB(this.count)));
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
        return true;
    }

    @Override
    public void end() {
        Log.i(TAG, "==================================================================================");
        Log.i(TAG, "Successfully Downloaded File...");
        Log.i(TAG, "Download Percentage = " + this.percent + "%");
        Log.i(TAG, "Total Downloaded File Size = " + bytesToMB(this.count));
        Log.i(TAG, "==================================================================================");
        if (handler != null) {
            handler.removeMessages(MESSAGE_COUNT);
            Message msg = handler.obtainMessage(MESSAGE_END);
            Bundle bundle = new Bundle();
            bundle.putString("dest", this.dest);
            bundle.putString("percentage", String.valueOf(this.percent));
            bundle.putString("fileSize", String.valueOf(bytesToMB(this.count)));
            msg.setData(bundle);
            handler.sendMessage(msg);
        }
    }

    private String bytesToMB(long bytes) {
        return String.valueOf((bytes / 1024.0) / 1024.0).substring(0, 5) + "MB";
    }
}
