package com.a202.ala.guitarstudio;

import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.test.UiThreadTest;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Afinador extends ActionBarActivity {

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    //short[] audioData;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    //Complex[] fftTempArray;
    //Complex[] fftArray;
    int[] bufferData;
    int bytesRecorded;

    int mPeakPos;
    double[] absNormalizedSignal;
    final int mNumberOfFFTPoints = 1024;

    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_afinador);

        setButtonHandlers();
        enableButtons(false);

        /*bufferSize = AudioRecord.getMinBufferSize
                (RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING)*3;*/

        if(AudioRecord.getMinBufferSize
                (RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING) < mNumberOfFFTPoints*2){
            bufferSize = mNumberOfFFTPoints*2;
        }

        i = new Intent(this, Minijuego.class);

        //audioData = new short [bufferSize]; //short array that pcm data is put into.

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_afinador, menu);
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

    private void setButtonHandlers() {
        ((Button)findViewById(R.id.btStart)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.btStop)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.saltarMinijuego)).setOnClickListener(btnClick);
    }


    private void enableButton(int id,boolean isEnable){
        ((Button)findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btStart,!isRecording);
        enableButton(R.id.btStop,isRecording);
    }



    //_______USANDO EL MICRO_______________________________________________
    public void startRecording(){               //recogerSonido
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            public void run() {
                writeAudioDataToFile();
            }
        });

        recordingThread.start();
    }

    void updateUI(){
        if(absNormalizedSignal != null){
            TextView texto = (TextView) findViewById(R.id.frecuencia);
            texto.setText("" + absNormalizedSignal[mPeakPos]);
            if (absNormalizedSignal[mPeakPos] > 100 && absNormalizedSignal[mPeakPos] < 200) {
                texto.setBackgroundColor(Color.GREEN);
            } else {
                texto.setBackgroundColor(Color.RED);
            }
        }
    }

    private void writeAudioDataToFile(){        //guardarSonido
        byte data[] = new byte[bufferSize];     //array tipo byte -> está creado para guardar el audio, el sonido
        String filename = getTempFilename();    //donde se va a guardar el sonido (ruta+nombre del archivo)
        FileOutputStream os = null;             //lo lleva a la ruta

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;

        if(null != os){
            while(isRecording){
                read = recorder.read(data, 0, bufferSize);
                if(read > 0){
                    absNormalizedSignal = calculateFFT(data); // --> HERE ^__^
                    runOnUiThread(new Runnable() {
                        public void run() {
                            updateUI();
                        }
                    });
                }



                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording(){ //dejarRecogerSonido
        if(null != recorder){
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            //recordingThread = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        //deleteTempFile();
    }
    //____DEJANDO DE USAR EL MICRO________________________________________

    private void deleteTempFile() {                 //borrarTemporal
        File file = new File(getTempFilename());
        file.delete();
    }

    private String getTempFilename(){               //cogerTemporal
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);//porque esta antes y no despues???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);//cambiar variable?
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private void copyWaveFile(String inFilename,String outFilename){ //copiarArchivoDeOnda
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            Log.i("AVISO", "File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {
            //another code

    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.btStart:{
                    Log.i("AVISO", "Start Recording");
                    enableButtons(true);
                    startRecording();
                    break;
                }
                case R.id.btStop:{
                    Log.i("AVISO", "Stop Recording");
                    enableButtons(false);
                    stopRecording();
                    //calculate();
                    break;
                }
                case R.id.saltarMinijuego:{
                    Log.i("AVISO", "Saltar minijuego");
                    startActivity(i);
                    break;
                }
            }
        }
    };

    /*public void calculate(){
        Complex[] fftTempArray = new Complex[bufferSize];
        for (int i=0; i<bufferSize; i++)
        {
            fftTempArray[i] = new Complex(audioData[i], 0);
        }
        Complex[] fftArray = FFT.fft(fftTempArray);

        double[] micBufferData = new double[bufferSize];
        final int bytesPerSample = 2;
        final double amplification = 100.0;
        for (int index = 0, floatIndex = 0; index < bytesRecorded - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
            double sample = 0;
            for (int b = 0; b < bytesPerSample; b++) {
                int v = bufferData[index + b];
                if (b < bytesPerSample - 1 || bytesPerSample == 1) {
                    v &= 0xFF;
                }
                sample += v << (b * 8);
            }
            double sample32 = amplification * (sample / 32768.0);
            micBufferData[floatIndex] = sample32;
        }
    }*/

    public double[] calculateFFT(byte[] signal)
    {
        //final int mNumberOfFFTPoints = 1024;
        double mMaxFFTSample;

        double temp;
        Complex[] y;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
        double[] absSignal = new double[mNumberOfFFTPoints/2];

        for(int i = 0; i < mNumberOfFFTPoints-1; i++){
            temp = (double)((signal[2*i] & 0xFF) | (signal[2*i+1] << 8)) / 32768.0F;
            complexSignal[i] = new Complex(temp,0.0);
        }

        y = FFT.fft(complexSignal); // --> Here I use FFT class

        mMaxFFTSample = 0.0;
        mPeakPos = 0;
        for(int i = 0; i < (mNumberOfFFTPoints/2); i++)
        {
            absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
            if(absSignal[i] > mMaxFFTSample)
            {
                mMaxFFTSample = absSignal[i];
                mPeakPos = i;
            }
        }
        Log.i("Frecuencia", ""+mMaxFFTSample);
        return absSignal;
    }


}
