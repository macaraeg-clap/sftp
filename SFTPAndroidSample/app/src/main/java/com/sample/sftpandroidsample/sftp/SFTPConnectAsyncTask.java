package com.sample.sftpandroidsample.sftp;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sample.sftpandroidsample.BuildConfig;
import com.sample.sftpandroidsample.MainActivity;

import java.util.Properties;

import static com.sample.sftpandroidsample.MainActivity.TAG;

public class SFTPConnectAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private SFTPConnectAsyncTask() {
    }

    public static SFTPConnectAsyncTask create(MainActivity.MainActivityController mainActivityController, SFTPCredentials sftpCredentials) {
        SFTPConnectAsyncTask v = new SFTPConnectAsyncTask();
        v.mainActivityController = mainActivityController;
        v.sftpCredentials = sftpCredentials;
        return v;
    }

    private MainActivity.MainActivityController mainActivityController;
    private SFTPCredentials sftpCredentials;
    private String errorMessage = "";

    @Override
    protected void onPreExecute() {
        if (mainActivityController != null) {
            mainActivityController.enableConnectButton(false);
            mainActivityController.updateConnectButtonLabel("Connecting...");
            mainActivityController.showConnectionLoading(View.VISIBLE);
        }
        Log.d(TAG, "Connecting...");
    }

    @Override
    protected void onPostExecute(Boolean result) {
        if (mainActivityController != null)
            mainActivityController.showConnectionLoading(View.INVISIBLE);
        if (result) {
            if (mainActivityController != null) {
                mainActivityController.enableSFTPCredentialsFields(false);
                mainActivityController.updateConnectButtonLabel("Successfully Connected");
                mainActivityController.removeConnectionLoading();
                mainActivityController.showDownloadApkFields(View.VISIBLE);
            }
            Log.i(TAG, "Connection Established...");
            return;
        }
        if (mainActivityController != null) {
            mainActivityController.enableConnectButton(true);
            mainActivityController.updateConnectButtonLabel("Connect Again");
            mainActivityController.showDownloadApkFields(View.INVISIBLE);
        }
        Log.e(TAG, "Connection Failed...");
        if (!"".equals(errorMessage))
            MainActivity.showMessage("Error", errorMessage);
    }

    @Override
    protected void onProgressUpdate(Void... values) {
    }

    @Override
    protected Boolean doInBackground(Void... var1) {
        boolean val = false;
        if (sftpCredentials != null) {
            val = true;
            try {
                JSch ssh = new JSch();
                Session session = ssh.getSession(sftpCredentials.getUsername(), sftpCredentials.getHostName(), sftpCredentials.getPort());
                Properties config = new Properties();
                config.put(BuildConfig.STRICT_HOSTKEY_CHECKIN_KEY, BuildConfig.STRICT_HOSTKEY_CHECKIN_VALUE);
                session.setConfig(config);
                session.setPassword(sftpCredentials.getPassword());
                //session.setTimeout(BuildConfig.TIME_OUT);
                session.connect();
                if (session.isConnected())
                    Log.i(TAG, "Session Successfully Connected...");
                Channel channel = session.openChannel(BuildConfig.SFTP);
                channel.connect();
                if (channel.isConnected())
                    Log.i(TAG, "Channel Successfully Connected...");
                if (mainActivityController != null) {
                    mainActivityController.setSession(session);
                    mainActivityController.setSFTPChannel((ChannelSftp) channel);
                }
            }
            catch (JSchException e) {
                Log.e(TAG, e.getMessage() + "...", e);
                errorMessage = e.getMessage();
                val = false;
            }
        }
        return val;
    }
}
