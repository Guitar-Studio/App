package com.a202.ala.guitarstudio;

import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


public class Minijuego extends ActionBarActivity {

    List<String> notes = new LinkedList(Arrays.asList("Do", "Re", "Mi", "Fa", "Sol", "La"));
    Integer[] timeStamp = {1, 2, 3, 4, 5, 6};
    //AudioTrack mediaPlayer = MediaPlayer.create(this, R.raw.notes);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_minijuego);
        for(int i=0;i<3;i++){
            createButton();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_minijuego, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void createButton(){
        Random rand = new Random();
        int randomNum = rand.nextInt(notes.size());
        Button button = new Button(this);
        button.setText(notes.get(randomNum));
        button.setId(randomNum);
        notes.remove(randomNum);

        LinearLayout selectNote = (LinearLayout)findViewById(R.id.selectNote);
        selectNote.addView(button);
    }
}
