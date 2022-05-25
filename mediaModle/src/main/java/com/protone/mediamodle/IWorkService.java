package com.protone.mediamodle;

import android.net.Uri;

import org.jetbrains.annotations.Nullable;

public interface IWorkService {
    void updateMusicBucket();
    void updateMusic(@Nullable Uri data);
    void updateGalley(@Nullable Uri data);
}
