package com.protone.seenn.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.protone.api.context.IntentKt;
import com.protone.database.room.dao.DataBaseDAOHelper;
import com.protone.database.room.entity.Music;
import com.protone.mediamodle.Galley;
import com.protone.mediamodle.IWorkService;
import com.protone.mediamodle.WorkReceiver;
import com.protone.mediamodle.WorkReceiverKt;
import com.protone.seenn.R;

public class WorkService extends Service {

    private final BroadcastReceiver workReceiver = new WorkReceiver() {
        @Override
        public void updateMusicBucket() {
            WorkService.this.updateMusicBucket();
        }

        @Override
        public void updateMusic() {
            WorkService.this.updateMusic();
        }

        @Override
        public void updatePhoto() {
            WorkService.this.updatePhoto();
        }

        @Override
        public void updateVideo() {
            WorkService.this.updateVideo();
        }
    };

    private final Executor executor = Executors.newCachedThreadPool();

    public WorkService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        WorkReceiverKt.getWorkLocalBroadCast().registerReceiver(workReceiver, IntentKt.getWorkIntentFilter());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new WorkBinder();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WorkReceiverKt.getWorkLocalBroadCast().unregisterReceiver(workReceiver);
    }

    private void updateMusicBucket() {
        executor.execute(() -> {
            List<Music> cacheList = new ArrayList<>(Galley.INSTANCE.getMusic());
            List<Music> allMusic = DataBaseDAOHelper.INSTANCE.getAllMusic();
            Map<String, List<Music>> musicBucket = Galley.INSTANCE.getMusicBucket();
            musicBucket.put(getString(R.string.all_music), Galley.INSTANCE.getMusic());
            if (allMusic != null) {
                String myBucket;
                for (Music music : allMusic) {
                    if (Galley.INSTANCE.getMusic().contains(music)) {
                        cacheList.remove(music);
                        myBucket = music.getMyBucket();
                        if (myBucket != null) {
                            String[] split = myBucket.split("/");
                            for (String s : split) {
                                if (!musicBucket.containsKey(s)) {
                                    musicBucket.put(s, new ArrayList<>());
                                }
                                Objects.requireNonNull(musicBucket.get(s)).add(music);
                            }
                        }
                    } else {
                        DataBaseDAOHelper.INSTANCE.deleteMusic(music);
                    }
                }
            }
            Log.d("TAG", "updateMusicBucket: " + musicBucket);
            Log.d("TAG", "updateMusicBucket: " + cacheList);
            DataBaseDAOHelper.INSTANCE.insertMusicMulti(cacheList);
        });
    }

    private void updateMusic() {

    }

    private void updatePhoto() {

    }

    private void updateVideo() {

    }

    private class WorkBinder extends Binder implements IWorkService {

        @Override
        public void UpdateMusicBucket() {
            WorkService.this.updateMusicBucket();
        }

        @Override
        public void UpdateMusic() {
            WorkService.this.updateMusic();
        }

        @Override
        public void UpdatePhoto() {
            WorkService.this.updatePhoto();
        }

        @Override
        public void UpdateVideo() {
            WorkService.this.updateVideo();
        }
    }
}