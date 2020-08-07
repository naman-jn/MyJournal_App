package com.njain.myjournal;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.njain.myjournal.util.JournalApi;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CreateAccountActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText usernameEditText;
    private Button createAccount;
    private ImageView backButton;
    private ProgressBar progressBar;
    public static final String TAG_E="CreateAccount Error: ";

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser currentUser;

    private FirebaseFirestore db= FirebaseFirestore.getInstance();

    private CollectionReference collectionReference=db.collection("Users");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        firebaseAuth=FirebaseAuth.getInstance();

        createAccount=findViewById(R.id.createAccount_account);
        backButton=findViewById(R.id.button_back);
        progressBar=findViewById(R.id.progress_account);
        emailEditText=findViewById(R.id.email_account);
        passwordEditText=findViewById(R.id.password_account);
        usernameEditText=findViewById(R.id.username_account);

        authStateListener=new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                currentUser= firebaseAuth.getCurrentUser();

                if(currentUser!=null){

                }else{

                }

            }
        };

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CreateAccountActivity.this,LoginActivity.class));
                finish();
            }
        });

        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();
                String username = usernameEditText.getText().toString().trim();

                if (!TextUtils.isEmpty(email)
                        && !TextUtils.isEmpty(password)
                        && !TextUtils.isEmpty(username)) {

                    createUserEmailAccount(email, password, username);

                }else {
                    Toast.makeText(CreateAccountActivity.this,
                            "Empty Fields Not Allowed",
                            Toast.LENGTH_LONG)
                            .show();
                }
            }
        });


    }

    private void createUserEmailAccount(String email, String password, final String username) {
        if (!TextUtils.isEmpty(email)
                && !TextUtils.isEmpty(password)
                && !TextUtils.isEmpty(username)) {

            progressBar.setVisibility(View.VISIBLE);



            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {

                                //email verification
                                currentUser = firebaseAuth.getCurrentUser();
                                currentUser.sendEmailVerification()
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){

                                            Toast.makeText(CreateAccountActivity.this,
                                                    "Registered successfully.Please verify your email address.", Toast.LENGTH_LONG).show();
                                            progressBar.setVisibility(View.INVISIBLE);

                                            Date currtime=Calendar.getInstance().getTime();
                                            assert currentUser != null;
                                            final String currentUserId = currentUser.getUid();

                                            //Create a user Map so we can create a user in the User collection
                                            Map<String, String> userObj = new HashMap<>();
                                            userObj.put("userId", currentUserId);
                                            userObj.put("username", username);
                                            userObj.put("date created",currtime.toString());

                                            //save to our firestore database//
                                            collectionReference.add(userObj)
                                                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                        @Override
                                                        public void onSuccess(DocumentReference documentReference) {
                                                            documentReference.get()
                                                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                                                                            if (task.getResult().exists()) {
                                                                                //progressBar.setVisibility(View.INVISIBLE);

                                                                                String name = task.getResult()
                                                                                        .getString("username");
//
                                                                                startActivity(new Intent(CreateAccountActivity.this,LoginActivity.class));


                                                                            }else {
                                                                                //progressBar.setVisibility(View.INVISIBLE);
                                                                                Toast.makeText(CreateAccountActivity.this,
                                                                                        task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                                                                //Log.e(TAG_E,"From Database Inner "+task.getException().getMessage());
                                                                            }
                                                                        }
                                                                    });

                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
//                                                            Log.e(TAG_E,"From Database "+e.getMessage());
                                                        }
                                                    });
                                            //save to our firestore database//

                                        }
                                        else{
                                            Toast.makeText(CreateAccountActivity.this,
                                                    task.getException().getMessage(),Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(CreateAccountActivity.this, e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                                //^email verification^
                                //_______________________________________//



                            }else {
                                  //something went wrong
                                Toast.makeText(CreateAccountActivity.this, task.getException().getMessage(), Toast.LENGTH_LONG).show();
                            }


                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CreateAccountActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressBar.setVisibility(View.INVISIBLE);
//                            Log.e(TAG_E,"From Firebase Auth "+e.getMessage());
                        }
                    });

        }else {

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        currentUser=firebaseAuth.getCurrentUser();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);
    }
}