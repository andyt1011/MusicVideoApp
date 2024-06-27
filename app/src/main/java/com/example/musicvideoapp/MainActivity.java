package com.example.musicvideoapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Button buttonAudio = findViewById(R.id.button_audio);
        Button buttonVideo = findViewById(R.id.button_video);

        buttonAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建Intent来启动PlayerActivity
                Intent intent = new Intent(MainActivity.this, MusicActivity.class);
                // 添加额外数据以指示播放的是音频
                intent.putExtra("mediaType", "audio");
                // 启动Activity
                startActivity(intent);
            }
        });

        buttonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 创建Intent来启动PlayerActivity
                Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                // 添加额外数据以指示播放的是视频
                intent.putExtra("mediaType", "video");
                // 启动Activity
                startActivity(intent);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}