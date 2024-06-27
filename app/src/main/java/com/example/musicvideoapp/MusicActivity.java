package com.example.musicvideoapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MusicActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private MusicAdapter adapter;
    private static MediaPlayer mediaPlayer;
    private ImageButton playPauseButton;
    private ImageButton nextButton;
    private ImageButton previousButton;
    private SeekBar seekBar;
    private TextView tvCurrentTime, tvTotalTime;
    private static int currentTrackIndex = 0;
    private boolean isSeekBarTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_music);

        // 初始化视图
        initializeViews();
        // 设置 RecyclerView
        setupRecyclerView();
        // 设置 MediaPlayer
        setupMediaPlayer();
        // 设置 SeekBar
        setupSeekBar();
        // 请求存储权限
        requestStoragePermission();
    }

    // 初始化视图组件
    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerview_music_list);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);
        seekBar = findViewById(R.id.seekbar_audio);
        playPauseButton = findViewById(R.id.btn_play_pause);
        nextButton = findViewById(R.id.btn_next);
        previousButton = findViewById(R.id.btn_previous);

        // 设置按钮点击事件
        playPauseButton.setOnClickListener(view -> playPause());
        nextButton.setOnClickListener(view -> playNextTrack());
        previousButton.setOnClickListener(view -> playPreviousTrack());
    }

    // 设置 RecyclerView 及其 Adapter 和分隔线
    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        adapter = new MusicAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    // 设置 MediaPlayer 实例
    private void setupMediaPlayer() {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
    }

    // 设置 SeekBar 的监听事件和更新任务
    private void setupSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeekBarTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeekBarTracking = false;
            }
        });

        // 每秒更新一次进度条和当前时间
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && !isSeekBarTracking) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                    tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
                }
                seekBar.postDelayed(this, 1000);
            }
        });
    }

    // 请求存储权限
    private void requestStoragePermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            loadMusicFiles();
        }
    }

    // 当 Activity 恢复时更新播放按钮和进度条
    @Override
    protected void onResume() {
        super.onResume();
        updatePlayPauseButton();
        updateSeekBarAndTime();
    }

    // 更新播放/暂停按钮的图标
    private void updatePlayPauseButton() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            playPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    // 更新进度条和时间
    private void updateSeekBarAndTime() {
        if (mediaPlayer != null) {
            seekBar.setMax(mediaPlayer.getDuration());
            seekBar.setProgress(mediaPlayer.getCurrentPosition());
            tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
            tvCurrentTime.setText(formatTime(mediaPlayer.getCurrentPosition()));
        }
    }

    // 播放或暂停音乐
    private void playPause() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playPauseButton.setImageResource(R.drawable.ic_play);
            } else {
                mediaPlayer.start();
                playPauseButton.setImageResource(R.drawable.ic_pause);
            }
        }
    }

    // 播放下一首音乐
    private void playNextTrack() {
        if (currentTrackIndex < adapter.musicFiles.size() - 1) {
            currentTrackIndex++;
        } else {
            currentTrackIndex = 0;
        }
        adapter.playMusic(adapter.musicFiles.get(currentTrackIndex));
    }

    // 播放上一首音乐
    private void playPreviousTrack() {
        if (currentTrackIndex > 0) {
            currentTrackIndex--;
        } else {
            currentTrackIndex = adapter.musicFiles.size() - 1;
        }
        adapter.playMusic(adapter.musicFiles.get(currentTrackIndex));
    }

    // 格式化时间为 mm:ss 格式
    private String formatTime(int time) {
        int minutes = time / 1000 / 60;
        int seconds = time / 1000 % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 加载音乐文件列表
    private void loadMusicFiles() {
        ArrayList<String> musicFiles = new ArrayList<>();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATA};

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        try (Cursor cursor = getContentResolver().query(collection, projection, selection, null, null)) {
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            while (cursor.moveToNext()) {
                String filePath = cursor.getString(dataIndex);
                String fileName = cursor.getString(nameColumn);
                musicFiles.add(fileName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        adapter.updateMusicList(musicFiles);
    }

    // 处理存储权限请求结果
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMusicFiles();
        }
    }

    // Adapter 类
    public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

        private List<String> musicFiles;
        private Context context;

        MusicAdapter(Context context, List<String> musicFiles) {
            this.context = context;
            this.musicFiles = new ArrayList<>(musicFiles);
        }

        @NonNull
        @Override
        public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new MusicViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
            String fileName = musicFiles.get(position);
            holder.musicFileName.setText(fileName);
            holder.itemView.setOnClickListener(v -> {
                currentTrackIndex = holder.getAdapterPosition();  // 更新 currentTrackIndex 为点击的项
                playMusic(fileName);
            });
        }

        // 播放指定音乐文件
        private void playMusic(String fileName) {
            String selection = MediaStore.Audio.Media.DISPLAY_NAME + "=?";
            String[] selectionArgs = new String[]{fileName};

            try (Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, selection, selectionArgs, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    String musicPath = cursor.getString(dataIndex);

                    if (mediaPlayer != null) {
                        mediaPlayer.reset();
                    } else {
                        mediaPlayer = new MediaPlayer();
                    }

                    mediaPlayer.setDataSource(musicPath);
                    mediaPlayer.prepareAsync();

                    mediaPlayer.setOnPreparedListener(mp -> {
                        seekBar.setMax(mediaPlayer.getDuration());
                        tvTotalTime.setText(formatTime(mediaPlayer.getDuration()));
                        tvCurrentTime.setText(formatTime(0));
                        mediaPlayer.start();
                        playPauseButton.setImageResource(R.drawable.ic_pause);
                    });

                    mediaPlayer.setOnCompletionListener(mp -> playNextTrack());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return musicFiles.size();
        }

        // 更新音乐列表
        void updateMusicList(List<String> musicFiles) {
            this.musicFiles.clear();
            this.musicFiles.addAll(musicFiles);
            notifyDataSetChanged();
        }

        // ViewHolder 类
        class MusicViewHolder extends RecyclerView.ViewHolder {
            TextView musicFileName;

            MusicViewHolder(View itemView) {
                super(itemView);
                musicFileName = itemView.findViewById(android.R.id.text1);
            }
        }
    }
}
