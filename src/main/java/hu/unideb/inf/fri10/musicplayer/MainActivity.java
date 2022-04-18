package hu.unideb.inf.fri10.musicplayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ktx.Firebase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import hu.unideb.inf.fri10.musicplayer.Model.UploadSong;

public class MainActivity extends AppCompatActivity implements SongChangeListener, AdapterView.OnItemSelectedListener {

    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener proximitySensorListener;
    private final List<MusicList> musicLists = new ArrayList<>();

    private RecyclerView musicRecyclerView;
    private MediaPlayer mediaPlayer;
    private TextView endTime, startTime, textViewImage;
    private boolean isPlaying = false;
    private SeekBar playerSeekBar;
    private ImageView playPauseImg;
    private Timer timer;
    private int currentSongListPosition = 0;
    private MusicAdapter musicAdapter;
    ProgressBar progressBar;
    Uri audioUri;
    StorageReference mStorageRef;
    StorageTask mUploadTask;
    DatabaseReference referenceSongs;
    String songsCategory;
    MediaMetadataRetriever mediaMetadataRetriever;
    byte[] art;
    String title1,artist1,album_art1 = "",duration1;
    TextView title2,artist2,album2,duration2,dataa;
    ImageView album_art2;





    ArrayAdapter<String> arrayAdapter;
    String[]  nme= {
            String.valueOf(R.id.musicTitle)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View decodeView = getWindow().getDecorView();
        int options = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        decodeView.setSystemUiVisibility(options);

        setContentView(R.layout.activity_main);

        musicRecyclerView = findViewById(R.id.musicRecyclerView);
        final CardView playPauseCard = findViewById(R.id.playPauseCard);
        playPauseImg = findViewById(R.id.playPauseImg);

        final ImageView nextBtn = findViewById(R.id.nextBtn);
        final ImageView prevBtn = findViewById(R.id.prevBtn);

        startTime = findViewById(R.id.startTime);
        endTime = findViewById(R.id.endTime);
        playerSeekBar = findViewById(R.id.playerSeekBar);
        musicRecyclerView.setHasFixedSize(true);
        musicRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //online player code

        mediaMetadataRetriever = new MediaMetadataRetriever();
        referenceSongs = FirebaseDatabase.getInstance("https://musicplayer-7065b-default-rtdb.firebaseio.com/").getReference().child("songs");
        mStorageRef= FirebaseStorage.getInstance().getReference().child("songs");






        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        proximitySensor= sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        //check if proximity sensor is available
        if (proximitySensor==null){
            Toast.makeText(this, "proximity sensor is not available", Toast.LENGTH_LONG).show();
            finish();
        }
        else{
            sensorManager.registerListener(proximitySensorListener,proximitySensor,sensorManager.SENSOR_DELAY_NORMAL);
        }


        mediaPlayer = new MediaPlayer();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles();

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 11);

            } else {
                getMusicFiles();
            }
        }
        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int nextSongListPosition = currentSongListPosition + 1;
                if (nextSongListPosition >= musicLists.size()) {
                    nextSongListPosition = 0;

                }
                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(nextSongListPosition).setPlaying(true);
                musicAdapter.updateList(musicLists);
                musicRecyclerView.scrollToPosition(nextSongListPosition);
                onChanged(nextSongListPosition);

            }
        });

        prevBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int prevSongListPosition = currentSongListPosition - 1;
                if (prevSongListPosition < 0) {
                    prevSongListPosition = musicLists.size() - 1;//play last song

                }
                musicLists.get(currentSongListPosition).setPlaying(false);
                musicLists.get(prevSongListPosition).setPlaying(true);
                musicAdapter.updateList(musicLists);
                musicRecyclerView.scrollToPosition(prevSongListPosition);
                onChanged(prevSongListPosition);
            }
        });

        playPauseCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPlaying) {
                    isPlaying = false;
                    mediaPlayer.pause();
                    playPauseImg.setImageResource(R.drawable.play_icon);
                } else {
                    isPlaying = true;
                    mediaPlayer.start();
                    playPauseImg.setImageResource(R.drawable.pause_icon);
                }
            }
        });
        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (b) {
                    if (isPlaying) {
                        mediaPlayer.seekTo(i);
                    } else {
                        mediaPlayer.seekTo(0);
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }


   @SuppressLint("Range")
    private void getMusicFiles() {

        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;

        Cursor cursor = contentResolver.query(uri,
                null, MediaStore.Audio.Media.DATA+" LIKE?", new String[]{"%.mp3%"}, null);

        if (cursor == null) {
            Toast.makeText(this, "Something went wrong!!", Toast.LENGTH_SHORT).show();
        } else if (!cursor.moveToNext()) {
            Toast.makeText(this, "No Music Found!", Toast.LENGTH_SHORT).show();
        }
            else {
            while (cursor.moveToNext()) {
               final String getMusicFileName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                long cursorId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                final String getArtistName = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                Uri musicFileUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursorId);
                String getDuration = "00:00";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    getDuration = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.AudioColumns.DURATION));

                }
                final MusicList musicList = new MusicList(getMusicFileName, getArtistName, getDuration, false, musicFileUri);
                musicLists.add(musicList);
            }
            musicAdapter = new MusicAdapter(musicLists, MainActivity.this);
            musicRecyclerView.setAdapter(musicAdapter);
        }
        cursor.close();
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getMusicFiles();
        } else {
            Toast.makeText(this, "Permission Declined By User", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus){
            View decodeView = getWindow().getDecorView();

            int options= View.SYSTEM_UI_FLAG_FULLSCREEN| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            decodeView.setSystemUiVisibility(options);
        }
    }

    @Override
    public void onChanged(int position) {
        currentSongListPosition=position;
        if (mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            mediaPlayer.reset();
        }
        else{
            mediaPlayer.start();
            mediaPlayer.reset();
        }

        mediaPlayer.setAudioAttributes(
                new AudioAttributes
                        .Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
        );
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaPlayer.setDataSource(MainActivity.this,musicLists.get(position).getMusicFile());
                    mediaPlayer.prepare();


                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Unable to play track", Toast.LENGTH_SHORT).show();
                }
            }
        }).start();

        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                final int getTotalDuration = mp.getDuration();
                String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                        TimeUnit.MILLISECONDS.toMinutes(getTotalDuration),
                        TimeUnit.MILLISECONDS.toSeconds(getTotalDuration) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getTotalDuration))) ;
                endTime.setText(generateDuration);
                isPlaying=true;
                playerSeekBar.setMax(getTotalDuration);
                mp.start();
                playPauseImg.setImageResource(R.drawable.pause_icon);

                proximitySensorListener=new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent sensorEvent) {
                        if (sensorEvent.values[0]< proximitySensor.getMaximumRange()){
                            mp.pause();
                        }else{
                            mp.start();
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int i) {

                    }
                };

                sensorManager.registerListener(proximitySensorListener,proximitySensor,sensorManager.SENSOR_DELAY_FASTEST);
            }
        });

        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
             runOnUiThread(new Runnable() {
                 @Override
                 public void run() {
                     final int getCurrentDuration = mediaPlayer.getCurrentPosition();
                     String generateDuration = String.format(Locale.getDefault(),"%02d:%02d",
                             TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration),
                             TimeUnit.MILLISECONDS.toSeconds(getCurrentDuration) -
                                     TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(getCurrentDuration))) ;
                     playerSeekBar.setProgress(getCurrentDuration);
                     startTime.setText(generateDuration);

                 }
             });


            }
        },1000,1000);

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.reset();
                timer.purge();
                timer.cancel();
                isPlaying=false;
                playerSeekBar.setProgress(0);
                playPauseImg.setImageResource(R.drawable.play_icon);


            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        getMenuInflater().inflate(R.menu.menu,menu);
        MenuItem menuItem= menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Type here to search");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {

               setContentView(R.layout.online_layout);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {




                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sub_menu) {

            Intent i = new Intent(MainActivity.this,MainActivity.class);
            startActivity(i);
            finish();
            return true;

        }
        if (id == R.id.sub_menu2) {

        setContentView(R.layout.online_layout);
        return true;
    }
        if (id == R.id.sub_menu3) {
             Intent i = new Intent(MainActivity.this,musicClient.class);
             startActivity(i);
             finish();
             return true;


        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(proximitySensorListener);}

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        songsCategory =adapterView.getItemAtPosition(i).toString();
        Toast.makeText(this,"Selected: "+songsCategory,Toast.LENGTH_LONG).show();

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

        return;
    }

    public void openAudioFiles (View v){
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("audio/*");
        startActivityForResult(i,101);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        textViewImage = findViewById(R.id.textViewSongsFileSelected);
        progressBar= findViewById(R.id.progressbar);
        title2 = findViewById(R.id.title);
        artist2 = findViewById(R.id.artist);
        duration2 = findViewById(R.id.duration);
        album2 = findViewById(R.id.album);
        dataa = findViewById(R.id.dataa);
        album_art2 = findViewById(R.id.imageview);
        Spinner spinner =findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(this);


        List <String> categories = new ArrayList<>();
        categories.add("Love Songs");
        categories.add("Gospel Songs");
        categories.add("Happy Songs");
        categories.add("Sad Songs");
        categories.add("Party Songs");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
    if (requestCode==101 && resultCode==RESULT_OK && data.getData() !=null)   {
        audioUri= data.getData();
        String fileNames = getFileName(audioUri);
        textViewImage.setText(fileNames);
        mediaMetadataRetriever.setDataSource(this,audioUri);
        art =mediaMetadataRetriever.getEmbeddedPicture();
        Bitmap bitmap = BitmapFactory.decodeByteArray(art,0,art.length);
        album_art2.setImageBitmap(bitmap);
        album2.setText(mediaMetadataRetriever.extractMetadata(mediaMetadataRetriever.METADATA_KEY_ALBUM));
        artist2.setText(mediaMetadataRetriever.extractMetadata(mediaMetadataRetriever.METADATA_KEY_ARTIST));
        title2.setText(mediaMetadataRetriever.extractMetadata(mediaMetadataRetriever.METADATA_KEY_TITLE));
        duration2.setText(mediaMetadataRetriever.extractMetadata(mediaMetadataRetriever.METADATA_KEY_DURATION));
        dataa.setText(mediaMetadataRetriever.extractMetadata(mediaMetadataRetriever.METADATA_KEY_GENRE));

        artist1 =mediaMetadataRetriever.extractMetadata(mediaMetadataRetriever.METADATA_KEY_ARTIST);
        title1 =mediaMetadataRetriever.extractMetadata(mediaMetadataRetriever.METADATA_KEY_TITLE);
        duration1 =mediaMetadataRetriever.extractMetadata(mediaMetadataRetriever.METADATA_KEY_DURATION);




    }
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri){
        String result = null;
        if(uri.getScheme().equals("content")){
            Cursor cursor = getContentResolver().query(uri,null,null,null,null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();

            }
        }
        if (result == null){
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1){
                result = result.substring(cut +1);
            }
        }
        return result;
    }

    public void uploadFileToFirebase (View v){
        if (textViewImage.equals("No file Selected")){
            Toast.makeText(this,"please selected an image!", Toast.LENGTH_SHORT).show();
        }
        else{
            if (mUploadTask !=null && mUploadTask.isInProgress()){
                Toast.makeText(this, "songs uploads in already progress!",Toast.LENGTH_SHORT).show();
            }
            else{
                uploadFileToFirebase();
            }
        }
    }

    private void uploadFileToFirebase() {

        if (audioUri!= null){
            Toast.makeText(this,"uploads please wait!",Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.VISIBLE);
            final StorageReference storageReference = mStorageRef.child(System.currentTimeMillis()+" . "+getfileextension(audioUri));
            mUploadTask = storageReference.putFile(audioUri).addOnSuccessListener((OnSuccessListener) (taskSnapshot) -> {
                storageReference.getDownloadUrl().addOnSuccessListener((OnSuccessListener) (uri) -> {
                    UploadSong uploadSong = new UploadSong(songsCategory,title1,artist1,album_art1,duration1,uri.toString());
                    String uploadId = referenceSongs.push().getKey();
                    referenceSongs.child(uploadId).setValue(uploadSong);





            });
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    double progress = (100.0* taskSnapshot.getBytesTransferred()/taskSnapshot.getTotalByteCount());
                    progressBar.setProgress((int) progress);

                }
            });
        }else{
            Toast.makeText(this,"No file to upload ",Toast.LENGTH_SHORT).show();
        }

    }
    private  String getfileextension(Uri audioUri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap= MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(audioUri));


    }
    public void openAlbumUploadActivity(View v){
        Intent in = new Intent(MainActivity.this,UploadAlbumActivity2.class);
        startActivity(in);
    }
}