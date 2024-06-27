package com.example.musicvideoapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class VideoActivity extends AppCompatActivity {
    private VideoView videoView; // VideoView 控件
    private RecyclerView recyclerView; // RecyclerView 控件
    private VideoAdapter adapter; // 视频列表适配器
    private MediaController mediaController; // 媒体控制器
    private FrameLayout videoContainer; // 视频容器布局

    private GestureDetector gestureDetector; // 手势检测器
    private AudioManager audioManager; // 音频管理器
    private float brightness = -1.0f; // 屏幕亮度
    private int maxVolume; // 最大音量
    private int currentVolume; // 当前音量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_video);

        // 初始化 RecyclerView
        recyclerView = findViewById(R.id.recyclerview_video_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 初始化适配器
        adapter = new VideoAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // 初始化 VideoView 和视频容器
        videoView = findViewById(R.id.video_view);
        videoContainer = findViewById(R.id.video_container);

        // 初始化媒体控制器
        mediaController = new MediaController(this);
        mediaController.setAnchorView(videoContainer);
        videoView.setMediaController(mediaController);

        // 初始化音频管理器
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        // 初始化手势检测器
        gestureDetector = new GestureDetector(this, new GestureListener());

        // 请求存储权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            loadVideoFiles();
        }

        // 处理系统栏边距
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 处理手势事件
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    // 加载视频文件
    private void loadVideoFiles() {
        ArrayList<String> videoPaths = new ArrayList<>();
        Uri collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA
        };

        String selection = MediaStore.Video.Media.DATA + " LIKE ? AND " + MediaStore.Video.Media.MIME_TYPE + " = ?";
        String[] selectionArgs = new String[]{"%/Movies/%", "video/mp4"};

        try (Cursor cursor = getContentResolver().query(collection, projection, selection, selectionArgs, null)) {
            int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            while (cursor.moveToNext()) {
                String videoPath = cursor.getString(dataIndex);
                videoPaths.add(videoPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter.updateVideoList(videoPaths);
    }

    // 手势监听器类，用于调节亮度和音量
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            float x = e1.getX();
            float y = e1.getY();
            int width = videoView.getWidth();

            if (x < width / 2) {
                // 左半屏调节亮度
                changeBrightness(distanceY);
            } else {
                // 右半屏调节音量
                changeVolume(distanceY);
            }

            return true;
        }
    }

    // 改变屏幕亮度
    private void changeBrightness(float distanceY) {
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        if (brightness == -1.0f) {
            brightness = layoutParams.screenBrightness;
            if (brightness <= 0.00f) brightness = 0.50f;
            if (brightness < 0.01f) brightness = 0.01f;
        }

        layoutParams.screenBrightness = brightness + (distanceY / videoView.getHeight());
        if (layoutParams.screenBrightness > 1.0f) layoutParams.screenBrightness = 1.0f;
        else if (layoutParams.screenBrightness < 0.01f) layoutParams.screenBrightness = 0.01f;
        getWindow().setAttributes(layoutParams);
    }

    // 改变音量
    private void changeVolume(float distanceY) {
        int newVolume = currentVolume + (int) (distanceY / videoView.getHeight() * maxVolume);
        if (newVolume > maxVolume) newVolume = maxVolume;
        else if (newVolume < 0) newVolume = 0;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        currentVolume = newVolume; // 更新当前音量
    }

    // 视频适配器类
    public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {
        private ArrayList<String> videoPaths;
        private Context context;

        VideoAdapter(Context context, ArrayList<String> videoPaths) {
            this.context = context;
            this.videoPaths = new ArrayList<>(videoPaths);
        }

        @NonNull
        @Override
        public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            TextView textView = itemView.findViewById(android.R.id.text1);
            return new VideoViewHolder(itemView, textView);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
            String videoPath = videoPaths.get(position);
            holder.textView.setText(new File(videoPath).getName());
            holder.itemView.setOnClickListener(v -> playVideo(videoPath));
        }

        @Override
        public int getItemCount() {
            return videoPaths.size();
        }

        // 播放视频
        private void playVideo(String path) {
            Uri uri = Uri.parse(path);
            videoView.setVideoURI(uri);
            videoView.start();
        }

        // 视频 ViewHolder
        class VideoViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            VideoViewHolder(View itemView, TextView textView) {
                super(itemView);
                this.textView = textView;
            }
        }

        // 更新视频列表
        void updateVideoList(ArrayList<String> newVideoPaths) {
            videoPaths.clear();
            videoPaths.addAll(newVideoPaths);
            notifyDataSetChanged();
        }
    }
}
