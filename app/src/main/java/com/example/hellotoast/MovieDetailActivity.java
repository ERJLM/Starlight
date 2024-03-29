package com.example.hellotoast;


import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity {

    private ImageView MovieThumbnailImg,MovieCoverImg;
    private TextView tv_title,tv_description;
    private FloatingActionButton play_fab;

    private FloatingActionButton back_fab;

    private Button button_delete;

    private static Movie movie;

    private User user;
    private AndroidWebServer server;
    private static boolean[] check = new boolean[1];





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);
        play_fab = findViewById(R.id.play_fab);
        back_fab = findViewById(R.id.back_fab);
        button_delete = findViewById(R.id.button_delete);
        user = (User)getIntent().getSerializableExtra("user");
        movie = (Movie)getIntent().getSerializableExtra("movie");


        // Set click listener for login button
        play_fab.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
            @Override
            public void onClick(View v) {
                @SuppressLint("UnsafeOptInUsageError") Intent intent = new Intent(MovieDetailActivity.this, MoviePlayerActivity.class);
                intent.putExtra("movie",movie);
                intent.putExtra("user", user);
                startActivity(intent);
            }
        });

        back_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MovieDetailActivity.this, MovieSelectorActivity.class);
                intent.putExtra("user", user);
                startActivity(intent);
            }
        });

        //Delete Movie
        button_delete.setOnClickListener(view -> {
            // Create a confirmation dialog
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Confirmation");
            builder.setMessage("Are you sure you want to delete this movie?");

            // Add buttons for confirmation
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // User clicked Yes, implement your logic to delete the user here
                    deleteMovieRequest();

                    // Once the user is deleted, you can finish the activity or navigate back to the previous screen
                    back_fab.performClick();
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
        });

        if(!user.isAdmin()) button_delete.setVisibility(View.GONE);

        iniViews(movie);
    }

    void iniViews(Movie movie) {
        String movieTitle = movie.getTitle();
        int imagecover = R.drawable.stars;
        MovieCoverImg = findViewById(R.id.detail_movie_cover);
        Glide.with(this).load(imagecover).into(MovieCoverImg);
        tv_title = findViewById(R.id.detail_movie_title);
        tv_title.setText(movieTitle);
        getSupportActionBar().setTitle(movieTitle);
        tv_description = findViewById(R.id.detail_movie_desc);
        tv_description.setText(movieTitle);
        MovieCoverImg.setAnimation(AnimationUtils.loadAnimation(this,R.anim.scale_animation));
        play_fab.setAnimation(AnimationUtils.loadAnimation(this,R.anim.scale_animation));

    }

    private void deleteMovieRequest() {
        Call<Boolean> call = RetrofitClient.getMovieApi().del_movie(movie.getId());
        call.enqueue(new Callback<Boolean>() {
            @Override
            public void onResponse(Call<Boolean> call, Response<Boolean> response) {
                if (response.isSuccessful()) {
                    Boolean result = response.body();
                    // Do something with the list of users...
                    if (result == true) {
                       getResponse(true);
                    }
                } else {
                    getResponse(false);
                    Log.w("RequestDeleteMovie", "Response is false");
                }
            }

            @Override
            public void onFailure(Call<Boolean> call, Throwable t) {
                t.toString();
                Log.e("RequestDeleteMovie", t.toString());
            }
        });
    }

    private void getResponse(boolean check) {
        if(check) Toast.makeText(this, "Movie with id = " + movie.getId()  + " was removed", Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "Movie with id = " + movie.getId() + " couldn't be removed", Toast.LENGTH_SHORT).show();
    }

    private void requestMovie() {
        Call<Movie> call = RetrofitClient.getMovieApi().get_movie(movie.getId());
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
                    Log.e("RequestMovie", "else");
                }
            }

            @Override
            public void onFailure(Call<Movie> call, Throwable t) {
                t.toString();
                Log.e("RequestMovie", t.toString());
            }
        });
    }

    private void getResponseMovie(Movie res) {
        movie = res;
        Log.i("MYMOVIE", String.valueOf(movie.isSeeded()));
    }



}