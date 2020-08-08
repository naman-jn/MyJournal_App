package com.njain.myjournal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.njain.myjournal.model.Journal;
import com.njain.myjournal.util.JournalApi;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;

import java.util.Date;


public class PostJournalActivity extends AppCompatActivity implements View.OnClickListener{
    private static final int GALLERY_CODE = 1;
    private static final String TAG = "PostJournalActivity";
    private Button saveButton;
    private ProgressBar progressBar;
    private ImageView addPhotoButton;
    private EditText titleEditText;
    private EditText thoughtsEditText;
    private ImageView imageView;
    private TextView currentUserTextView;
    private int postActionId;
    private Journal journal_from_list;

    private String currentUserId;
    private String currentUserName;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser user;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private StorageReference storageReference;

    private CollectionReference collectionReference = db.collection("Journal");

    private Uri imageUri;

    private Context ctx=this;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    protected void setStatusBarTranslucent(boolean makeTranslucent) {
        if (makeTranslucent) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }//Status Bar Translucent

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_post_journal);
        setStatusBarTranslucent(true);

        storageReference= FirebaseStorage.getInstance().getReference();
        firebaseAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.post_progressBar);
        titleEditText = findViewById(R.id.post_title_et);
        thoughtsEditText = findViewById(R.id.post_description_et);
        currentUserTextView = findViewById(R.id.post_username_textview);
        imageView = findViewById(R.id.post_imageView);
        addPhotoButton = findViewById(R.id.postCameraButton);
        saveButton = findViewById(R.id.post_save_journal_button);

        postActionId=0;

        journal_from_list= getIntent().getParcelableExtra("journal");
        postActionId =getIntent().getIntExtra("postActionId",0);
        if(postActionId==1){
            String imgUrl= journal_from_list.getImageUrl();
            String title=journal_from_list.getTitle();
            String thought=journal_from_list.getThought();
            titleEditText.setText(title);
            thoughtsEditText.setText(thought);

            Picasso.get()
                    .load(imgUrl)
                    .placeholder(R.drawable.journal)
                    .fit()
                    .into(imageView);
            saveButton.setText("Update");

        }

        addPhotoButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);

        if (JournalApi.getInstance() != null) {
            currentUserId = JournalApi.getInstance().getUserId();
            currentUserName = JournalApi.getInstance().getUsername();
            currentUserTextView.setText(currentUserName);
        }
        else{
            Toast.makeText(this, "Something Wrong with Journal API", Toast.LENGTH_SHORT).show();
        }

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                user = firebaseAuth.getCurrentUser();
                if (user != null) {

                } else {

                }
            }
        };
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.post_save_journal_button:
                //saveJournal
                if(isNetworkAvailable()){
                    if(postActionId==0){
                        saveJournal();
                    }
                    else if(postActionId==1){
                        updateJournal();
                    }
                }
                else{
                    Toast.makeText(this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.postCameraButton:
                //get image from gallery/phone
//                Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT);
//                galleryIntent.setType("image/*");
//                startActivityForResult(galleryIntent, GALLERY_CODE);
                CropImage.activity()
                        .setAspectRatio(5,3)
                        .start(PostJournalActivity.this);
                break;
        }
    }

    private void saveJournal() {
        final String title = titleEditText.getText().toString().trim();
        final String thoughts = thoughtsEditText.getText().toString().trim();

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        if (!TextUtils.isEmpty(title) &&
                !TextUtils.isEmpty(thoughts)
                && imageUri != null) {

            final StorageReference filepath = storageReference
                    .child("journal_images")
                    .child("image_" + Timestamp.now().getSeconds());

//            ByteArrayOutputStream journalImgBitmap=new ByteArrayOutputStream();
//            imageBitmap.compress(Bitmap.CompressFormat.JPEG,100,journalImgBitmap);

            filepath.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                            filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    String imageUrl = uri.toString();

                                    Journal journal = new Journal();
                                    journal.setTitle(title);
                                    journal.setThought(thoughts);
                                    journal.setImageUrl(imageUrl);
                                    journal.setTimeAdded(new Timestamp(new Date()));
                                    journal.setUserName(currentUserName);
                                    journal.setUserId(currentUserId);


                                    collectionReference.add(journal)
                                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                                @Override
                                                public void onSuccess(DocumentReference documentReference) {
                                                    progressBar.setVisibility(View.INVISIBLE);
                                                    Toast.makeText(PostJournalActivity.this, "Saved", Toast.LENGTH_SHORT).show();
                                                    startActivity(new Intent(PostJournalActivity.this,
                                                            JournalListActivity.class));
                                                    saveButton.setEnabled(true);
                                                    finish();
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
//                                                    Log.d(TAG, "onFailure: " + e.getMessage());

                                                }
                                            });
//

                                }
                            });


                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.INVISIBLE);
                            saveButton.setEnabled(true);
                          //  Log.e("Error: ","Image upload failure");

                        }
                    });


        } else {
            Toast.makeText(this, "Empty Fields or Image Not Selected", Toast.LENGTH_LONG).show();
            progressBar.setVisibility(View.INVISIBLE);
            saveButton.setEnabled(true);

        }
    }

    private void updateJournal() {

        final String title = titleEditText.getText().toString().trim();
        final String thoughts = thoughtsEditText.getText().toString().trim();
        final String imageUrl= journal_from_list.getImageUrl();
        //final String[] this_journal_id = new String[1];

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        if(imageUri != null){
            if (!TextUtils.isEmpty(title) &&
                    !TextUtils.isEmpty(thoughts)) {

                final StorageReference filepath = storageReference
                        .child("journal_images")
                        .child("image_" + Timestamp.now().getSeconds());

                filepath.putFile(imageUri)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {


                                filepath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {

                                        String imageUrl = uri.toString();
                                        updateEntry(title,thoughts,imageUrl);
                                    }
                                });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressBar.setVisibility(View.INVISIBLE);
                                saveButton.setEnabled(true);
//                                Log.e("Error: ","Image upload failure");
                            }
                        });


            } else emptyFields();
        }
        //if user doesn't change to a new image
        else{
            if (!TextUtils.isEmpty(title) &&
                    !TextUtils.isEmpty(thoughts)) {

                updateEntry(title,thoughts,imageUrl);

            } else emptyFields();
        }

    }


    void updateEntry(String title,String thoughts,String imageUrl){
        final Journal j = new Journal();
        j.setTitle(title);
        j.setThought(thoughts);
        j.setImageUrl(imageUrl);
        j.setTimeAdded(new Timestamp(new Date()));
        j.setUserName(currentUserName);
        j.setUserId(currentUserId);

        collectionReference.document(journal_from_list.getJournalId())
                .set(j).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(PostJournalActivity.this, "Updated!",
                        Toast.LENGTH_LONG)
                        .show();
                progressBar.setVisibility(View.INVISIBLE);
                startActivity(new Intent(PostJournalActivity.this,
                        JournalListActivity.class));
                saveButton.setEnabled(true);
                finish();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
    }

    void emptyFields(){
        Toast.makeText(this, "Empty Fields", Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.INVISIBLE);
        saveButton.setEnabled(true);
    }


//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == GALLERY_CODE && resultCode == RESULT_OK) {
//            if (data != null) {
//                imageUri = data.getData(); // we have the actual path to the image
//                imageView.setImageURI(imageUri);//show image
//            }
//        }
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
                CropImage.ActivityResult result=CropImage.getActivityResult(data);
                if(data!=null){
                    imageUri = result.getUri(); // we have the actual path to the image
                    imageView.setImageURI(imageUri);//show image
                }

        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(PostJournalActivity.this,JournalListActivity.class));
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left);
    }
}