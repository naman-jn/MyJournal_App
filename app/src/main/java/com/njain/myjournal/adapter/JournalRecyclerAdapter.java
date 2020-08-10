package com.njain.myjournal.adapter;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.text.format.DateUtils;
import android.transition.Transition;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.njain.myjournal.JournalListActivity;
import com.njain.myjournal.PostJournalActivity;
import com.njain.myjournal.R;
import com.njain.myjournal.model.Journal;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class JournalRecyclerAdapter extends RecyclerView.Adapter<JournalRecyclerAdapter.ViewHolder> {

    private static final String TAG = "Recycler Adapter";
    private Context context;
    private List<Journal> journalList;
    private String documentId;

    private static Dialog infoDialog;

    private CardView delete;
    private ProgressBar progressBar;

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference collectionReference = db.collection("Journal");


    public JournalRecyclerAdapter(Context context, List<Journal> journalList) {
        this.context = context;
        this.journalList = journalList;
    }

    @NonNull
    @Override
    public JournalRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.journal_row, parent, false);
        return new ViewHolder(view, context);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalRecyclerAdapter.ViewHolder holder, int position) {
        Journal journal = journalList.get(position);
        holder.title.setText(journal.getTitle());
        holder.thought.setText(journal.getThought());
        holder.username.setText(journal.getUserName());

        String imgUrl = journal.getImageUrl();
        Picasso.get()
                .load(imgUrl)
                .resize(675,405)
                .placeholder(R.drawable.journal)
                .into(holder.img);

        //Source: https://medium.com/@shaktisinh/time-a-go-in-android-8bad8b171f87
        String timeAgo = (String) DateUtils.getRelativeTimeSpanString(journal
                .getTimeAdded()
                .getSeconds() * 1000);
        holder.dateCreated.setText(timeAgo);
    }

    @Override
    public int getItemCount() {
        return journalList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView img;
        public TextView title;
        public TextView thought;
        public TextView dateCreated;
        public TextView username;
        public ImageView share;
        public CardView journalRow;

        public ViewHolder(@NonNull View itemView, final Context ctx) {
            super(itemView);
            context = ctx;

            img = itemView.findViewById(R.id.jourrnal_image_list);
            title = itemView.findViewById(R.id.journal_title_list);
            thought = itemView.findViewById(R.id.journal_thought_list);
            dateCreated = itemView.findViewById(R.id.journal_timestamp_list);
            username = itemView.findViewById(R.id.journal_row_username);
            share = itemView.findViewById(R.id.journal_row_share_button);
            journalRow = itemView.findViewById(R.id.journal_row);

            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    Journal journal = journalList.get(position);

                    String imageUrl = journal.getImageUrl();

                    shareImage(imageUrl, context, position, journalList);


                }
            });

            journalRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    Journal journal = journalList.get(position);

                    Activity activity=(Activity) context;
                    final Intent intent = new Intent(context, PostJournalActivity.class);
                    intent.putExtra("journal", journal);
                    intent.putExtra("postActionId", 1);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right);

                }
            });

            journalRow.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {

                    infoDialog = new Dialog(context);
                    infoDialog.setContentView(R.layout.popup_delete);
                    infoDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    infoDialog.show();

                    delete = infoDialog.findViewById(R.id.card_delete);
                    progressBar=infoDialog.findViewById(R.id.progress_delete);


                    delete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            progressBar.setVisibility(View.VISIBLE);
                            final int position = getAdapterPosition();
                            final Journal journal = journalList.get(position);
                            final String userId = journal.getUserId();
                            final String url=journal.getImageUrl();
                            final Timestamp timeAdded = journal.getTimeAdded();

                            collectionReference
                                    .whereEqualTo("userId", userId)
                                    .whereEqualTo("timeAdded", timeAdded)
                                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                                        @RequiresApi(api = Build.VERSION_CODES.N)
                                        @Override
                                        public void onEvent(@Nullable QuerySnapshot queryDocumentSnapshots,
                                                            @Nullable FirebaseFirestoreException error) {

                                            for (QueryDocumentSnapshot snapshot : queryDocumentSnapshots) {

//                                                Log.d(TAG, snapshot.getId());
                                                snapshot.getReference().delete()
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                progressBar.setVisibility(View.INVISIBLE);
                                                                Toast.makeText(context, "Journal Deleted", Toast.LENGTH_SHORT).show();
                                                                final Intent intent = new Intent(context, JournalListActivity.class);
                                                                context.startActivity(intent);
                                                            }
                                                        });
                                            }
                                        }
                                    });
                            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(url);
                                    photoRef.delete();
                        }
                    });

                    return true;
                }
            });

        }

    }


    public void shareImage(String url, final Context context, final int position, final List<Journal>journalList) {

        Picasso.get().load(url)
                .resize(675,405)
                .into(new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "From Journal App");
                shareIntent.putExtra(Intent.EXTRA_TEXT, journalList.get(position).getTitle()
                        + "\n\n" + journalList.get(position).getThought());

                shareIntent.setType("image/*");

                Uri imageUri = getLocalBitmapUri(bitmap, context);

                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
                Log.d("TAG", "onBitmapLoaded: " + imageUri.toString());
                context.startActivity(Intent.createChooser(shareIntent, "Send Image"));
            }

            @Override
            public void onBitmapFailed(Exception e, Drawable errorDrawable) {

            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
            }
        });
    }

        public Uri getLocalBitmapUri (Bitmap bmp,Context context){
            Uri bmpUri = null;
            try {
                File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.PNG, 10, out);
                out.close();
                bmpUri = Uri.fromFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return bmpUri;
        }
    }
