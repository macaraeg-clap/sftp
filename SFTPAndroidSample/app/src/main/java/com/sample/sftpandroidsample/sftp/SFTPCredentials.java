package com.sample.sftpandroidsample.sftp;

public class SFTPCredentials {

    private SFTPCredentials() {
    }

    public static SFTPCredentials instance() {
        return new SFTPCredentials();
    }

    private String hostName, username, password;
    private int port;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
