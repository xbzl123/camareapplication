package com.example.camareapplication;

/**
 * Copyright (c) 2023 Raysharp.cn. All rights reserved
 * <p>
 * TaskTag
 *
 * @author longyanghe
 * @date 2023-09-15
 */
public class TaskTag {
    private int pos;
    private boolean isRunning;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    private String filePath;

    public long getStartTimeSec() {
        return startTimeSec;
    }

    public void setStartTimeSec(long startTimeSec) {
        this.startTimeSec = startTimeSec;
    }

    private long startTimeSec;

    public TaskTag(int pos, boolean isRunning) {
        this.pos = pos;
        this.isRunning = isRunning;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setRunning(boolean running) {
        isRunning = running;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    @Override
    public String toString() {
        return "TaskTag{" +
                "pos=" + pos +
                ", isRunning=" + isRunning +
                '}';
    }
}
