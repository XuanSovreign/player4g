package com.space.player4g;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * @Author Sovereign
 * @Date 2019/8/1
 */
public class HDVideoActivity extends BaseActivity implements TextureView.SurfaceTextureListener {

    private String[] permissions = {Manifest.permission.READ_EXTERNAL_STORAGE};
    private TextureView mVwHighDefinition;
    private SurfaceTexture mSurfaceTexture;
    private IjkMediaPlayer mPlayer;
    private File mFile;
    private AlertDialog mDialog;
    private boolean isNetOpen;
    private boolean isFluent;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mPlayer == null) {
                return;
            }
            switch (msg.what) {
                case 0:
                    mPlayer.pause();
                    if (mDialog == null) {
                        createDialog();
                    } else {
                        mDialog.show();
                    }
                    mHandler.sendEmptyMessageDelayed(1, 3000);
                    break;
                case 1:
                    if (mDialog != null) {
                        mDialog.hide();
                    }
                    long position = mPlayer.getCurrentPosition();
                    int seek = new Random().nextInt(10) * 5000 + 5000;
                    mPlayer.seekTo(position + seek);
                    mPlayer.start();
                    int i = new Random().nextInt(10);
                    Log.e("=======", "handleMessage: " + i);
                    mHandler.sendEmptyMessageDelayed(0, i * 3000 + 1000);
                    break;

            }
        }
    };

    @Override
    protected int getLayOutID() {
        return R.layout.activity_video;
    }

    @Override
    protected void initView() {
        mVwHighDefinition = findViewById(R.id.surface_hd_video);
        mVwHighDefinition.setSurfaceTextureListener(this);
    }

    @Override
    protected void initData() {
        checkNet();
        checkPermission();
    }

    private void checkFile() {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }
        File directory = Environment.getExternalStorageDirectory();
        File secondDic = new File(directory, "player4G");
        if (!secondDic.exists()) {
            secondDic.mkdirs();
        }
        mFile = new File(secondDic, "player.mp4");
    }


    @Override
    protected void initListener() {

    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permission = -1;
            for (int i = 0; i < permissions.length; i++) {
                permission = ActivityCompat.checkSelfPermission(this, permissions[i]);
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    break;
                }
            }

            if (permission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, 0x0010);
            } else {
                checkFile();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean check = true;
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                check = false;
                break;
            }
        }
        if (check) {
            checkFile();
            playVideo();
        } else {
            Toast.makeText(this, "请申请相应权限", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        playVideo();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVwHighDefinition.isAvailable() && mPlayer != null) {
            mPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        if (mPlayer != null) {
            mPlayer.pause();
        }
        super.onPause();
    }

    private void playVideo() {
        if (mFile == null) {
            return;
        }
        if (!isNetOpen) {
            return;
        }
        mSurfaceTexture = mVwHighDefinition.getSurfaceTexture();
        Surface sf = new Surface(mSurfaceTexture);
        if (mPlayer == null) {
            mPlayer = new IjkMediaPlayer();
        }
        try {
            // >6.0的情况 使用 避免变速变调
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 0);
            } else {
                mPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "soundtouch", 1);
            }
            mPlayer.setDataSource(mFile.getAbsolutePath());
            mPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mPlayer.setSurface(sf);
        mPlayer.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {
                mPlayer.start();
                mPlayer.setScreenOnWhilePlaying(true);
                mPlayer.setLooping(true);
                if (!isFluent) {
                    mHandler.sendEmptyMessageDelayed(0, new Random().nextInt(10) * 3000 + 3000);
                }
            }
        });
    }

    private void releaseVideo() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }

        if (mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }
    }

    @Override
    protected void onStop() {
        if (isFinishing()) {
            releaseVideo();
        }
        super.onStop();
    }

    private void createDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.LoadingDialog);
        View view = View.inflate(this, R.layout.dialog_waitting, null);
        builder.setView(view);
        builder.setCancelable(false);
        mDialog = builder.show();
    }

    /**
     * 判断网络
     */
    private void checkNet() {
        // 获取ConnectivityManager
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo ni = cm.getActiveNetworkInfo();// 获取当前网络状态
        if (ni != null && ni.isConnectedOrConnecting()) {
            isNetOpen=true;
            switch (ni.getType()) {
                case ConnectivityManager.TYPE_WIFI:// wifi的情况下
                    isFluent=true;
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    if (ni.getSubtype() == 20) {
                        isFluent = true;
                    } else {
                        isFluent=false;
                    }
            }
        } else {
            isNetOpen=false;
            isFluent=false;
            Toast.makeText(this,"网络没有连接",Toast.LENGTH_SHORT).show();
        }
    }
}
