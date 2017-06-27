package com.badquery.exojni;

import android.app.Activity;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.extractor.ts.TsExtractor;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;

/**
 * Created by andy@badquery.com using Android Studio 2.3.2 6/26/17
 *
 * The objective of this demo is to show a simple means of sourcing exoplayer via C++ (a real
 * programming language ::wink::). It's not polished, has no error handling, etc.
 */
public class MainActivity extends Activity {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // Our JNI Interface for com.google.android.exoplayer2.upstream.DataSource
    public static native long openFile(AssetManager am, String filename);
    public static native int readFile(byte[] buffer, int offset, int readLength);
    public static native void closeFile();

    // Our demo TS file (dude playing the violin)
    private final static Uri mUri = Uri.parse("sample4.ts");

    /**
     * This is all the java code.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // [Step 1] Create the media source, sourcing the stream data via C++ over the JNI
        DataSource.Factory dataSourceFactory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return new DataSource() {
                    @Override
                    public long open(DataSpec dataSpec) throws IOException {
                        return openFile(getAssets(),mUri.toString()); // dataSpec.uri);
                    }
                    @Override
                    public int read(byte[] buffer, int offset, int readLength) throws IOException {
                        return (readLength > 0 ? readFile(buffer,offset,readLength) : 0);
                    }
                    @Override
                    public Uri getUri() {
                        return mUri;
                    }
                    @Override
                    public void close() throws IOException {
                        closeFile();
                    }
                };
            }
        };
        ExtractorsFactory extractorsFactory = new ExtractorsFactory() {
            @Override
            public Extractor[] createExtractors() { // demo hardcode to use a TS file for simplicity
                return new TsExtractor[] {new TsExtractor()};
            }
        };
        MediaSource mediaSource = new ExtractorMediaSource(mUri, dataSourceFactory, extractorsFactory, null, null);

        // [Step 2] Create, Init & Prepare the player w/ the media source we've just created
        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(getApplicationContext()),new DefaultTrackSelector(),new DefaultLoadControl());
        ((SimpleExoPlayerView)findViewById(R.id.video_view)).setPlayer(player);
        player.setPlayWhenReady(true);
        player.seekToDefaultPosition();
        player.prepare(mediaSource, true, false);
    }

}
