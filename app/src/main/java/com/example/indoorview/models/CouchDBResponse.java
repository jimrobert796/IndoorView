package com.example.indoorview.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CouchDBResponse {
    @SerializedName("total_rows")
    public int total_rows;

    @SerializedName("offset")
    public int offset;

    @SerializedName("rows")
    public List<CouchDBRow> rows;
}
