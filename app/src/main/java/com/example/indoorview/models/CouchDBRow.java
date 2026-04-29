package com.example.indoorview.models;

import com.google.gson.annotations.SerializedName;

public class CouchDBRow {
    @SerializedName("id")
    public String id;

    @SerializedName("key")
    public Object key; // Puede ser null

    @SerializedName("value")
    public LugarCouchDB value;
}