package com.example.camareapplication;

import android.app.IntentService;
import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * Copyright (c) 2023 Raysharp.cn. All rights reserved
 * <p>
 * MyService
 *
 * @author longyanghe
 * @date 2023-09-20
 */
public class MyService extends IntentService {
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public MyService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
