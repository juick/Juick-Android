package com.juick.remote.api;

import android.os.Handler;
import android.os.Looper;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by gerc on 01.05.2016.
 */
public class ProgressRequestBody extends RequestBody {

    private File mFile;
    private String mediaType;
    private String mPath;
    private UploadCallbacks mListener;
    Handler handler = new Handler(Looper.getMainLooper());

    private static final int DEFAULT_BUFFER_SIZE = 2048;

    public interface UploadCallbacks {
        void onProgressUpdate(int percentage);
        void onError();
        void onFinish();
    }

    public ProgressRequestBody(final File file, String mediaType, final UploadCallbacks listener) {
        mFile = file;
        this.mediaType = mediaType;
        mListener = listener;
    }

    @Override
    public MediaType contentType() {
        return MediaType.parse(mediaType);
    }

    @Override
    public void writeTo(BufferedSink sink) throws IOException {
        if (mFile == null) return;
        long fileLength = mFile.length();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        FileInputStream in = new FileInputStream(mFile);
        long uploaded = 0;

        try {
            int read;

            while ((read = in.read(buffer)) != -1) {

                // update progress on UI thread
                handler.post(new ProgressUpdater(uploaded, fileLength));

                uploaded += read;
                sink.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
    }

    private class ProgressUpdater implements Runnable {
        private long mUploaded;
        private long mTotal;
        public ProgressUpdater(long uploaded, long total) {
            mUploaded = uploaded;
            mTotal = total;
        }

        @Override
        public void run() {
            mListener.onProgressUpdate((int)(100 * mUploaded / mTotal));
        }
    }
}
