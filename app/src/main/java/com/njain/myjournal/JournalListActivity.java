package com.njain.myjournal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.StorageReference;
import com.njain.myjournal.adapter.JournalRecyclerAdapter;
import com.njain.myjournal.model.Journal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class JournalListActivity extends AppCompatActivity {
    private static final String TAG = "JournalList/Failure: ";
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;

    public RecyclerView recyclerView;
    private JournalRecyclerAdapter journalRecyclerAdapter;
    private List<Journal> journalList;

    private CollectionReference collectionReference = db.collection("Journal");

    private Context ctx=this;
    SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onStart() {
        super.onStart();
        currentUser = firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }


    public class JournalSorter implements Comparator<Journal>
    {
        @Override
        public int compare(Journal j1, Journal j2) {
            return j1.getTimeAdded().compareTo(j2.getTimeAdded());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journal_list);
        Objects.requireNonNull(getSupportActionBar()).setElevation(0);

        swipeRefreshLayout=findViewById(R.id.swipeRLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.my_light_gray));
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.my_dark_blue));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getJournals(ctx);
                        swipeRefreshLayout.setRefreshing(false);
                    }
                },1500);

                //startActivity(new Intent(JournalListActivity.this,JournalListActivity.class));
            }
        });

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        recyclerView=findViewById(R.id.recyclerView);

        journalList=new ArrayList<>();

        getJournals(this);

        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser = firebaseAuth.getCurrentUser();
                if (currentUser != null) {

                }
                else{

                }
            }
        };

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        MenuItem item = menu.findItem(R.id.action_signout);
        SpannableString s = new SpannableString("Log Out");
        s.setSpan(new ForegroundColorSpan(Color.WHITE), 0, s.length(), 0);
        item.setTitle(s);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_add:
                    startActivity(new Intent(JournalListActivity.this,
                            PostJournalActivity.class));
                    overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
                break;
            case R.id.action_signout:
                if (currentUser != null && firebaseAuth != null) {
                    firebaseAuth.signOut();
                    Intent a = new Intent(Intent.ACTION_MAIN);
                    a.addCategory(Intent.CATEGORY_HOME);
                    a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(a);
                    new Handler().postDelayed(new Runnable() {

                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    }, 100);

                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void getJournals(final Context ctx) {
        final List<Journal> journalList=new ArrayList<>();

        String currentUserId = currentUser.getUid();

            collectionReference.whereEqualTo("userId",currentUserId)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @RequiresApi(api = Build.VERSION_CODES.N)
                        @Override
                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                            @Nullable FirebaseFirestoreException error) {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {

                                    Journal journal = snapshot.toObject(Journal.class);
                                    journal.setJournalId(snapshot.getId());//For updating we need a document reference
                                    journalList.add(journal);
                                }
                                journalList.sort(new JournalSorter());
                                Collections.reverse(journalList);
                                journalRecyclerAdapter=new JournalRecyclerAdapter(ctx,journalList);
                                recyclerView.setAdapter(journalRecyclerAdapter);
                                journalRecyclerAdapter.notifyDataSetChanged();

                            }
                            else{
//                                Log.d(TAG,"User Data Empty");
                            }
                        }
                    });

    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 3000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (firebaseAuth != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }
}
