package com.example.caucse.newdrawingrecorder;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends AppCompatActivity  {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void onButton1Clicked(View v){
        Intent intent = new Intent(getApplicationContext(), DrawActivity.class);
        startActivity(intent);
    }

    public void onButton2Clicked(View v){
        Intent intent = new Intent(getApplicationContext(), StorageActivity.class);
        startActivity(intent);
    }
}
