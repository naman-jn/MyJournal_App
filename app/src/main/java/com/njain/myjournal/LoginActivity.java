package com.njain.myjournal;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.njain.myjournal.adapter.JournalRecyclerAdapter;
import com.njain.myjournal.model.Journal;
import com.njain.myjournal.util.JournalApi;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class LoginActivity extends AppCompatActivity {

    private Button createAccount;
    private Button login;
    private CardView google;
    private AutoCompleteTextView emailEditText;
    private EditText passwordEditText;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Users");

    // Configure Google Sign In
    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;
    private GoogleSignInClient mGoogleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        createAccount=findViewById(R.id.createAccount);
        login=findViewById(R.id.login);
        google=findViewById(R.id.google);
        emailEditText=findViewById(R.id.email);
        passwordEditText=findViewById(R.id.password);
        progressBar=findViewById(R.id.progress);

        firebaseAuth=FirebaseAuth.getInstance();


//        if(isSignedIn){
//            authStateListener=new FirebaseAuth.AuthStateListener() {
//                @Override
//                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
//                    currentUser= firebaseAuth.getCurrentUser();
//
//                    if(currentUser!=null){
//
//                    }else{
//
//                    }
//
//                }
//            };
//        }

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                login.setEnabled(false);
                String email=emailEditText.getText().toString().trim();
                String password=passwordEditText.getText().toString().trim();

                if (!TextUtils.isEmpty(email)
                        && !TextUtils.isEmpty(password)) {
                    progressBar.setVisibility(View.VISIBLE);
                    loginEmailPassUser(email,password);

                }else {
                    Toast.makeText(LoginActivity.this,
                            "Empty Fields Not Allowed",
                            Toast.LENGTH_LONG)
                            .show();
                    login.setEnabled(true);
                }

            }


        });
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(LoginActivity.this,CreateAccountActivity.class));
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
            }

        });

        google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    private void loginEmailPassUser(String email, String password) {

        firebaseAuth.signInWithEmailAndPassword(email,password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        progressBar.setVisibility(View.INVISIBLE);
                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();



                            if(currentUser.isEmailVerified()){  //Email Verified

                                String currentUserId = currentUser.getUid();

                                if(currentUser!=null && firebaseAuth != null){
                                    collectionReference                                //Proceeding according to current user
                                            .whereEqualTo("userId", currentUserId)
                                            .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                                @Override
                                                public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                                                    @Nullable FirebaseFirestoreException e) {

                                                    if (e != null) {
                                                    }
                                                    assert queryDocumentSnapshots != null;
                                                    if (!queryDocumentSnapshots.isEmpty()) {

                                                        progressBar.setVisibility(View.INVISIBLE);
                                                        for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {
                                                            JournalApi journalApi = JournalApi.getInstance();
                                                            journalApi.setUsername(snapshot.getString("username"));
                                                            journalApi.setUserId(snapshot.getString("userId"));

                                                            startActivity(new Intent(LoginActivity.this,JournalListActivity.class));
                                                            login.setEnabled(true);
                                                            finish();
                                                            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                                                        }
                                                    }
                                                    else{
                                                        Toast.makeText(LoginActivity.this, "QueryEmpty", Toast.LENGTH_SHORT).show();
                                                    }

                                                }
                                            });
                                }


                            }
                            else{
                                Toast.makeText(LoginActivity.this, "Please verify email address", Toast.LENGTH_SHORT).show();
                            }


                    }

                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(LoginActivity.this,e.getMessage(), Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.INVISIBLE);
                login.setEnabled(true);
            }
        });

    }

    //for Google SignIn
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                //Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                //Log.w(TAG, "Google sign in failed", e);
                //Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();;
                // ...
            }
        }
    } //for Google
    private void firebaseAuthWithGoogle(String idToken) {
        // [START_EXCLUDE silent]
           progressBar.setVisibility(View.VISIBLE);
        // [END_EXCLUDE]
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            //Log.d(TAG, "signInWithCredential:success");
                            final FirebaseUser user = firebaseAuth.getCurrentUser();

                            JournalApi journalApi=JournalApi.getInstance();
                            journalApi.setUserId(user.getUid());
                            journalApi.setUsername(user.getDisplayName());


                            collectionReference.whereEqualTo("userId",user.getUid())
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                            @RequiresApi(api = Build.VERSION_CODES.N)
                                            @Override
                                            public void onEvent(@androidx.annotation.Nullable QuerySnapshot queryDocumentSnapshots,
                                                                @androidx.annotation.Nullable FirebaseFirestoreException error) {
                                                if (!queryDocumentSnapshots.isEmpty()) {
                                                    startActivity(new Intent(LoginActivity.this,JournalListActivity.class));
                                                    finish();
                                                    overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                                                }
                                                else{
                                                    Date currtime= Calendar.getInstance().getTime();

                                                    Map<String, String> userObj = new HashMap<>();
                                                    userObj.put("userId", user.getUid());
                                                    userObj.put("username", user.getDisplayName());
                                                    userObj.put("date created",currtime.toString());

                                                    collectionReference.document()
                                                            .set(userObj)
                                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                @Override
                                                                public void onSuccess(Void aVoid) {
                                                                    startActivity(new Intent(LoginActivity.this,JournalListActivity.class));
                                                                    finish();
                                                                    overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
                                                                }
                                                            });
                                                }
                                                if (error != null) {
                                                    Log.e("Firestore", "Error listening for data", error);
                                                }
                                            }
                                        });

                        } else {
                            // If sign in fails, display a message to the user.
                            //.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication Failed\n"+task.getException(), Toast.LENGTH_SHORT).show();
                        }

                        // [START_EXCLUDE]
                        progressBar.setVisibility(View.INVISIBLE);
                        // [END_EXCLUDE]
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