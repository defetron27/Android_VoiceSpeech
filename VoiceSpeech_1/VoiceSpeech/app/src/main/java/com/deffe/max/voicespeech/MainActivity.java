package com.deffe.max.voicespeech;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends AppCompatActivity implements RecognitionListener
{
    private static final String KEYPHRASE = "computer";

    private static final String KWS_SEARCH = "wakeup";

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private SpeechRecognizer recognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        ((TextView) findViewById(R.id.caption_text)).setText("Preparing the recognizer");

        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }

        new SetupTask(this).execute();
    }
/*

    //Recognition

    @Override
    public void onReadyForSpeech(Bundle bundle) {

    }

    @Override
    public void onRmsChanged(float v) {

    }

    @Override
    public void onBufferReceived(byte[] bytes) {

    }

    @Override
    public void onError(int i) {

    }

    @Override
    public void onResults(Bundle bundle)
    {
        ArrayList<String> res = bundle.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);

        if (res != null)
        {

        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {

    }

    @Override
    public void onEvent(int i, Bundle bundle) {

    }
*/


    //PocketRecognition

    @Override
    public void onBeginningOfSpeech()
    {

    }

    @Override
    public void onEndOfSpeech()
    {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();

        if (text.equals(KEYPHRASE))
        {
            startToRecognizeText();
        }
        else
        {
            Toast.makeText(this, "text : " + text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResult(Hypothesis hypothesis)
    {
        if (hypothesis != null)
        {
            String text = hypothesis.getHypstr();
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Exception e) {
        ((TextView) findViewById(R.id.caption_text)).setText(e.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    private static class SetupTask extends AsyncTask<Void, Void, Exception>
    {
        WeakReference<MainActivity> activityReference;

        SetupTask(MainActivity activity)
        {
            this.activityReference = new WeakReference<>(activity);
        }
        @Override
        protected Exception doInBackground(Void... params)
        {
            try
            {
                Assets assets = new Assets(activityReference.get());
                File assetDir = assets.syncAssets();
                activityReference.get().setupRecognizer(assetDir);
            } catch (IOException e) {
                return e;
            }
            return null;
        }
        @Override
        protected void onPostExecute(Exception result)
        {
            if (result != null)
            {
                ((TextView) activityReference.get().findViewById(R.id.caption_text))
                        .setText("Failed to init recognizer " + result);
            }
            else
            {
                activityReference.get().switchSearch(KWS_SEARCH);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK)
        {
            if (requestCode == 12)
            {
                ArrayList<String> res = null;

                if (data != null)
                {
                    res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    ((TextView) findViewById(R.id.result_text)).setText(res.get(0));

                    switchSearch(KWS_SEARCH);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull  int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                new SetupTask(this).execute();
            }
            else
            {
                finish();
            }
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        if (recognizer != null)
        {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    private void switchSearch(String searchName)
    {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).

        if (searchName.equals(KWS_SEARCH))
        {
            recognizer.startListening(searchName);

            Toast.makeText(this, "KWS ", Toast.LENGTH_SHORT).show();
        }

        ((TextView) findViewById(R.id.caption_text)).setText(searchName);
    }

    private void startToRecognizeText()
    {
        recognizer.stop();

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "What can i do for you?");
        try
        {
            startActivityForResult(intent, 12);
        }
        catch (ActivityNotFoundException a)
        {
            Toast.makeText(getApplicationContext(), "Sorry! Your device does not support speech input", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRecognizer(File assetsDir) throws IOException
    {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))
                .setRawLogDir(assetsDir)
                .getRecognizer();
        recognizer.addListener(this);

        /* In your application you might not need to add all those searches.
          They are added here for demonstration. You can leave just one.
         */

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
    }

}
