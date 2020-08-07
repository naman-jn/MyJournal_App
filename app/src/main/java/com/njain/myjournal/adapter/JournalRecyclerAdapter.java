package com.njain.myjournal.adapter;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.format.DateUtils;
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
                .placeholder(R.drawable.journal)
                .fit()
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

            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    Journal journal = journalList.get(position);

                    String imageUrl = journal.getImageUrl();
                    String title = journal.getTitle();
                    String thought = journal.getThought();

                    String msgToSend = "Title: " + title +
                            "\n\n Thought: " + thought +
                            "\n\n Image URL: " + imageUrl;

                    // send the message through implicit action_send
                    Intent shareMsg = new Intent(Intent.ACTION_SEND);
                    shareMsg.setType("text/plain");
                    shareMsg.putExtra(Intent.EXTRA_SUBJECT, "My journal");
                    shareMsg.putExtra(Intent.EXTRA_TEXT, msgToSend);
                    context.startActivity(Intent.createChooser(shareMsg, "Share Via"));


                }
            });

            journalRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();

                    Journal journal = journalList.get(position);

                    final Intent intent = new Intent(context, PostJournalActivity.class);
                    intent.putExtra("journal", journal);
                    intent.putExtra("postActionId", 1);
                    context.startActivity(intent);
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
                            deleteJournal(getAdapterPosition());
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


        private void deleteJournal(int getAdapterPosition) {

        }

    }
}
