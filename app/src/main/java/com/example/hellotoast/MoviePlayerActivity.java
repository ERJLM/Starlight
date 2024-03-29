package com.example.hellotoast;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;


import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@UnstableApi
@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
public class MoviePlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    //AppServer appServer;
    String videoUrl ;
    Button button_download;
    private TextView mShowCount;
    private boolean confirm;
    private String download_link;
    private int num_of_chunks;
    private User user;
    private Movie movie;
    private static boolean[] check = new boolean[1];

    private static MyDialogFragment loadingDialog;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        button_download = (Button) findViewById(R.id.button_download);
        user = (User)getIntent().getSerializableExtra("user");
        Log.w("USEROLL", String.valueOf(user));
        movie = (Movie)getIntent().getSerializableExtra("movie");
        loadingDialog = new MyDialogFragment();
        if(movie.isSeeded() || user.getSeeder() > 0) button_download.setVisibility(View.GONE);


        videoUrl = movie.getStreamingLink();


        if (videoUrl.contains(":8080")){
            Log.w("MOVIESEED", "YES1122");
            assert AndroidWebServer.getInstance() != null;
            Log.i("MOVIESEED", String.valueOf(movie.getId()));
            AndroidWebServer.getInstance().setMovie_info(movie.getId(), movie.getStreamingLink());
            videoUrl = "http://localhost:8080/playlist.m3u8";
        }
        Log.w("MOVIELINK", videoUrl);
        button_download.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MoviePlayerActivity.this);
                builder.setTitle("Confirmation");
                builder.setMessage("Are you sure you want to be the seeder of this movie?");

                // Add buttons for confirmation
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        check[0] = false;
                        download_request();
                        //while(!check[0]){}
                    }
                });

                // Add button for cancellation
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // User clicked No, do nothing or handle accordingly
                    }
                });

                // Show the dialog
                AlertDialog dialog = builder.create();
                dialog.show();



            }
        });

        //Start loading
        loadingDialog.show(getSupportFragmentManager(), "dialog");

        initializePlayer();
        Log.d("MyApp", "Activity created");
    }

    private void download_request(){
        Call<Seed_Request> call = RetrofitClient.getUserApi().seed(user.getId(), movie.getId());
        call.enqueue(new Callback<Seed_Request>(){
            @Override
            public void onResponse(Call<Seed_Request> call, Response<Seed_Request> response) {
                if (response.isSuccessful()) {
                    Seed_Request  result = response.body();
                    // Do something with the list of users...
                    if (result != null) {
                        check[0] = true;
                        download_link = result.getLink_de_download();
                        num_of_chunks = result.getNum_of_chunks();
                        confirm = result.isConfirm();
                        Log.w("RequestSeed", "Response was a Success");
                        Log.w("RequestSeed INFO: ", "LINK:" + download_link + "__Num of chunks" + num_of_chunks + "__is valid? " + confirm);
                        getResponse(download_link, num_of_chunks, confirm);
                    }
                } else {
                    Log.w("RequestSeed", "Response is null");
                }
            }

            @Override
            public void onFailure(Call<Seed_Request> call, Throwable t) {
                t.toString();
                Log.e("RequestSeed Fail", t.toString());
            }
        });
    }

    private void getResponse(String downloadLink, int numOfChunks, boolean confirm) {

        if(confirm) {
            user.setSeeder(movie.getId());
            for (int i = 0; i < num_of_chunks; i++) {
                String title = "data" + String.format("%03d", i) + ".ts";
                String chunkUrl = download_link + "/" + title;
                String localFilePath = MovieDownloadManager.downloadVideo(MoviePlayerActivity.this, chunkUrl, title );
                Log.w("RequestSeed getResponse", "chunk " + i + "downloaded with url: " + chunkUrl);
            }
            String movieUrl = download_link + "/playlist.m3u8";
            MovieDownloadManager.downloadVideo(MoviePlayerActivity.this, movieUrl, "playlist.m3u8");
            Toast.makeText(this, "Movie was downloaded", Toast.LENGTH_SHORT).show();
            button_download.setVisibility(View.GONE);
        }
        else Toast.makeText(this, "Couldn't download movie", Toast.LENGTH_SHORT).show();

    }

    @Override
    protected void onStop(){
        super.onStop();
        //appServer.stop();
        player.setPlayWhenReady(false);
        player.release();
        player = null;
    }
    private void initializePlayer() {
        PlayerView playerView = findViewById(R.id.playerView);
        player = new ExoPlayer.Builder(MoviePlayerActivity.this).build();
        player.addListener(new ExoPlayer.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    if (loadingDialog != null && loadingDialog.isVisible()) {
                        loadingDialog.dismiss();
                    }
                }
            }
            @Override
            public void onPlayerError(PlaybackException error) {
                Log.d("ONPLAYERERROR", "Erro apanhado: " + error.getMessage());

                //Show loading screen
                if (loadingDialog != null && !loadingDialog.isVisible()) {
                    loadingDialog.show(getSupportFragmentManager(), "dialog");
                }

                getMovieFromServer();
            }
        });


        playerView.setPlayer(player);
        MediaItem mediaItem = MediaItem.fromUri(videoUrl);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

    }

    private void getMovieFromServer() {
        Call<Movie> call = RetrofitClient.getMovieApi().get_movie_from_server(movie.getId());
        call.enqueue(new Callback<Movie>() {
            @Override
            public void onResponse(Call<Movie> call, Response<Movie> response) {
                if (response.isSuccessful()) {
                    Movie res = response.body();
                    // Do something with the list of users...
                    if (res != null) {
                        // Update the adapter with the new list of users
                        //userAdapter.setLogin(login);
                        getResponseMovie(res);

                    }
                } else {
                    Intent intent = new Intent(MoviePlayerActivity.this, ErrorActivity.class);
                    startActivity(intent);
                    Log.e("RequestLogin", "else");
                }
            }

            @Override
            public void onFailure(Call<Movie> call, Throwable t) {
                t.toString();
                Intent intent = new Intent(MoviePlayerActivity.this, ErrorActivity.class);
                startActivity(intent);
                Log.e("RequestLogin", t.toString());
            }
        });
    }

    private void getResponseMovie(Movie res) {
        movie = res;
        videoUrl = movie.getStreamingLink();
        if (loadingDialog != null && loadingDialog.isVisible()) {
            loadingDialog.dismiss();
        }
        Log.d("REQUESTMOVIESERVER", "Video pedido ao servidor");
        initializePlayer();
    }


    public void showNetflix(View view) {
        Toast toast = Toast.makeText(this, "Welcome to Starlight",
                Toast.LENGTH_SHORT);
        toast.show();
    }

    public void back(View view) {
        Intent intent = new Intent(MoviePlayerActivity.this, MovieDetailActivity.class);
        intent.putExtra("movie",movie);
        intent.putExtra("user", user);
        startActivity(intent);
    }



}