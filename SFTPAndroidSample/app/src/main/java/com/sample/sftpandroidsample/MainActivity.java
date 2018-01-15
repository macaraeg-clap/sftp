package com.sample.sftpandroidsample;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.view.ViewManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.sample.sftpandroidsample.sftp.SFTPConnectAsyncTask;
import com.sample.sftpandroidsample.sftp.SFTPCredentials;
import com.sample.sftpandroidsample.sftp.SFTPDownloadProgressMonitor;
import com.sample.sftpandroidsample.sftp.SFTPPath;
import com.sample.sftpandroidsample.sftp.SFTPSavedFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public static class MainActivityController {

        private MainActivityController() {
        }

        static MainActivityController create(MainActivity mainActivity) {
            MainActivityController v = new MainActivityController();
            v.mainActivity = mainActivity;
            return v;
        }

        private MainActivity mainActivity;

        public void updateConnectButtonLabel(String label) {
            if (mainActivity != null)
                mainActivity.updateButtonLabel(R.id.btn_connect, label);
        }

        public void enableConnectButton(boolean state) {
            if (mainActivity != null)
                mainActivity.enableButton(R.id.btn_connect, state);
        }

        public void enableSFTPCredentialsFields(boolean state) {
            if (mainActivity != null)
                mainActivity.enableSFTPCredentials(state);
        }

        public void showConnectionLoading(int state) {
            if (mainActivity != null)
                mainActivity.displayProgressLoading(state);
        }

        public void removeConnectionLoading() {
            if (mainActivity != null)
                mainActivity.removeProgressLoading();
        }

        public void showDownloadApkFields(int state) {
            if (mainActivity != null)
                mainActivity.showContainerFields(R.id.download_container, state);
        }

        public void setSFTPChannel(ChannelSftp channel) {
            if (mainActivity != null)
                mainActivity.channelSftp = channel;
        }

        public void setSession(Session session) {
            if (mainActivity != null)
                mainActivity.session = session;
        }
    }

    public Handler handler = new Handler() {

        private String message;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_INIT:
                    setDestinationAPKFile(msg.getData().getString("fileName"));
                    updateButtonLabel(R.id.btn_download, "Downloading...");
                    if (progressDoalog != null)
                        progressDoalog.setMessage(message = "Total File Size:\n\t" + msg.getData().getString("max"));
                    break;
                case MESSAGE_COUNT:
                    if (progressDoalog != null) {
                        progressDoalog.setTitle("Downloading... " + msg.getData().getString("percentage") + "%");
                        progressDoalog.setMessage(message + "\n\nDownloaded File Size:\n\t" + msg.getData().getString("fileSize"));
                    }
                    break;
                case MESSAGE_END:
                    sftpSavedFile = SFTPSavedFile.instance();
                    sftpSavedFile.setFile(msg.getData().getString("dest"));
                    sftpSavedFile.setFileSize(msg.getData().getString("fileSize"));
                    updateButtonLabel(R.id.btn_download, "Successfully Downloaded");
                    enableButton(R.id.btn_download, false);
                    if (progressDoalog != null)
                        progressDoalog.dismiss();
                    closeConnection();
                    displaySavedFileDetails();
                    showContainerFields(R.id.saved_container, View.VISIBLE);
                    break;
            }
        }
    };

    public MainActivity() {
        context = this;
    }


    public final static int MESSAGE_INIT = 1, MESSAGE_COUNT= 2, MESSAGE_END = 3;
    public static final String TAG = "SFTPConnection";
    private static Context context;
    private SFTPCredentials sftpCredentials;
    private SFTPPath sftpPath;
    private SFTPSavedFile sftpSavedFile;
    private ChannelSftp channelSftp;
    private Session session;
    private ProgressDialog progressDoalog;
    private String sourceAPKFile = "Mrcos.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        forceHideKeyboard();
        initializeSFTPCredentials();
        initializeSFTPPath();
        displaySFTPCredentials();
        {
            final MainActivity mainActivity = this;
            Button v = findViewById(R.id.btn_connect);
            if (v != null)
                v.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        SFTPConnectAsyncTask.create(MainActivityController.create(mainActivity), sftpCredentials).execute();
                    }
                });
        }
        {
            Button v = findViewById(R.id.btn_download);
            if (v != null)
                v.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        sftpDownloadAPKFile();
                    }
                });
        }
        {
            Button v = findViewById(R.id.btn_install);
            if (v != null)
                v.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        installDownloadedAPK(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath(), getDownloadFileName());
                    }
                });
        }
        displaySFTPPath();
    }

    private String getDownloadFileName() {
        TextView v = findViewById(R.id.txt_dst_file);
        if (v != null)
            return v.getText().toString();
        return "";
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //closeConnection();
    }

    private void closeConnection() {
        if (channelSftp != null)
            channelSftp.disconnect();
        if (session != null)
            session.disconnect();
    }

    private void sftpDownloadAPKFile() {
        if (sftpPath != null && channelSftp != null) {
            progressDoalog = new ProgressDialog(MainActivity.this);
            progressDoalog.setTitle("Downloading...");
            progressDoalog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDoalog.setCanceledOnTouchOutside(false);
            progressDoalog.show();
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        String dst_file_name = "MRCOS_" + DateFormat.format("hhmmss_a_MMddyyyy", new Date()).toString().toUpperCase(), ext_name = ".apk";
                        channelSftp.get(sftpPath.getSourcePath() + sourceAPKFile, sftpPath.getDestinationPath() + dst_file_name + ext_name,
                                SFTPDownloadProgressMonitor.create(handler, dst_file_name + ext_name));
                        closeConnection();
                    } catch (SftpException | StringIndexOutOfBoundsException e) {
                        Log.e(TAG, e.getMessage() + "...", e);
                        progressDoalog.dismiss();
                    }
                }
            }).start();
        }
    }

    private void installDownloadedAPK(String path, String apk) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.fromFile(new File(path + "/" + apk)), "application/vnd.android.package-archive");
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void forceHideKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void enableButton(int buttonId, boolean state) {
        Button v = findViewById(buttonId);
        if (v != null)
            v.setEnabled(state);
    }

    public void updateButtonLabel(int buttonId, String label) {
        Button v = findViewById(buttonId);
        if (v != null)
            v.setText(label);
    }

    public void displayProgressLoading(int state) {
        ProgressBar v = findViewById(R.id.bar_loading);
        if (v != null)
            v.setVisibility(state);
    }

    public void removeProgressLoading() {
        ProgressBar v = findViewById(R.id.bar_loading);
        if (v != null)
            ((ViewManager) v.getParent()).removeView(v);
    }

    private void displaySFTPCredentials() {
        if (sftpCredentials != null) {
            {
                TextView v = findViewById(R.id.txt_host_name);
                if (v != null)
                    v.setText(sftpCredentials.getHostName());
            }
            {
                TextView v = findViewById(R.id.txt_port);
                if (v != null)
                    v.setText(String.valueOf(sftpCredentials.getPort()));
            }
            {
                TextView v = findViewById(R.id.txt_username);
                if (v != null)
                    v.setText(sftpCredentials.getUsername());
            }
            {
                TextView v = findViewById(R.id.txt_password);
                if (v != null)
                    v.setText(sftpCredentials.getPassword());
            }
        }
    }

    private void enableSFTPCredentials(boolean state) {
        {
            TextView v = findViewById(R.id.txt_host_name);
            if (v != null)
                v.setEnabled(state);
        }
        {
            TextView v = findViewById(R.id.txt_port);
            if (v != null)
                v.setEnabled(state);
        }
        {
            TextView v = findViewById(R.id.txt_username);
            if (v != null)
                v.setEnabled(state);
        }
        {
            TextView v = findViewById(R.id.txt_password);
            if (v != null)
                v.setEnabled(state);
        }
    }

    private void initializeSFTPCredentials() {
        sftpCredentials = SFTPCredentials.instance();
        sftpCredentials.setHostName(BuildConfig.HOST_NAME);
        sftpCredentials.setPort(BuildConfig.PORT);
        sftpCredentials.setUsername(BuildConfig.USERNAME);
        sftpCredentials.setPassword(BuildConfig.PASSWORD);
    }

    private void initializeSFTPPath() {
        sftpPath = SFTPPath.instance();
        sftpPath.setSourcePath(BuildConfig.SRC_PATH);
        sftpPath.setDestinationPath(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + BuildConfig.DST_PATH);
    }

    private void showContainerFields(int containerId, int state) {
        LinearLayout v = findViewById(containerId);
        if (v != null)
            v.setVisibility(state);
    }

    private void displaySFTPPath() {
        if (sftpPath != null) {
            {
                TextView v = findViewById(R.id.txt_src_path);
                if (v != null)
                    v.setText(sftpPath.getSourcePath());
            }
            {
                TextView v = findViewById(R.id.txt_dst_path);
                if (v != null)
                    v.setText(sftpPath.getDestinationPath());
            }
        }
        setSourceAPKFile(sourceAPKFile);
        setDestinationAPKFile("File name will be updated after start download is pressed.");
    }

    private void displaySavedFileDetails() {
        if (sftpSavedFile != null) {
            {
                TextView v = findViewById(R.id.txt_file);
                if (v != null)
                    v.setText(sftpSavedFile.getFile());
            }
            {
                TextView v = findViewById(R.id.txt_file_size);
                if (v != null)
                    v.setText(sftpSavedFile.getFileSize());
            }
        }
    }

    private void setSourceAPKFile(String name) {
        TextView v = findViewById(R.id.txt_src_file);
        if (v != null)
            v.setText(name);
    }

    private void setDestinationAPKFile(String name) {
        TextView v = findViewById(R.id.txt_dst_file);
        if (v != null)
            v.setText(name);
    }

    public static void showMessage(String title, String message) {
        AlertDialog.Builder v = new AlertDialog.Builder(context);
        v.setTitle(title);
        v.setMessage(message);
        v.setCancelable(true);
        v.setPositiveButton("Close", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        v.show();
    }
}
