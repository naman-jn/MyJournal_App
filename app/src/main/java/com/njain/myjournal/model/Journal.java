package com.njain.myjournal.model;


import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;


public class Journal implements Parcelable {


    @Exclude private String journalId;
    private String title;
    private String thought;
    private String imageUrl;
    private String userId;
    private Timestamp timeAdded;
    private String userName;


    protected Journal(Parcel in) {
        journalId = in.readString();
        title = in.readString();
        thought = in.readString();
        imageUrl = in.readString();
        userId = in.readString();
        timeAdded = in.readParcelable(Timestamp.class.getClassLoader());
        userName = in.readString();
    }

    public static final Creator<Journal> CREATOR = new Creator<Journal>() {
        @Override
        public Journal createFromParcel(Parcel in) {
            return new Journal(in);
        }

        @Override
        public Journal[] newArray(int size) {
            return new Journal[size];
        }
    };

    public String getJournalId() {
        return journalId;
    }

    public void setJournalId(String journalId) {
        this.journalId = journalId;
    }

    public Journal() { //Must for firestore to work
    }



    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getThought() {
        return thought;
    }

    public void setThought(String thought) {
        this.thought = thought;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Timestamp getTimeAdded() {
        return timeAdded;
    }

    public void setTimeAdded(Timestamp timeAdded) {
        this.timeAdded = timeAdded;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(journalId);
        dest.writeString(title);
        dest.writeString(thought);
        dest.writeString(imageUrl);
        dest.writeString(userId);
        dest.writeParcelable(timeAdded, flags);
        dest.writeString(userName);
    }
}
