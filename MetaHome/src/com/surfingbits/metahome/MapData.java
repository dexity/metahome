// Copyright 2011 Alex Dementsov

package com.surfingbits.metahome;

import android.content.Context;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

import java.util.*;
import java.net.URL;
import java.util.ArrayList;
import java.net.URLConnection;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.json.JSONArray;
import org.json.JSONObject;

import com.surfingbits.metahome.MetaHome;



class MemoryMapData implements IMapData
{
	private int[] cells		= new int[MetaHome.NUM_ITEMS];
	

	public MemoryMapData(Context context) {
		// context is not used here
		
        for (int i=0; i<cells.length; i++)
        	cells[i]	= -1;
	}
	
	public void set(int index, int value) {
		cells[index]	= value;
	}
	
	public int get(int index) {
		return cells[index];
	}
	
	public int[] getAll() {
		return cells;
	}
	
	public void remove(int index) {
		cells[index]	= -1;
	}
	
	public boolean isEmpty(int index) {
		return cells[index] == -1;
	}
	
	public int size() {
		return cells.length;
	}
}


class DatabaseMapData implements IMapData
{
	private DatabaseHelper mOpenHelper;
	
	private static final String DBNAME = "cells.db";
	private static final int DBVERSION = 3;

    public static final class Cells implements BaseColumns {

        private Cells() {}	
        
    	private static final String TABLE_NAME 	= "cells";
    	private static final String COLUMN_ID 	= "cid";
    	private static final String COLUMN_CELL = "cell";
    }
	
    static class DatabaseHelper extends SQLiteOpenHelper {

    	DatabaseHelper(Context context) { 
			// calls the super constructor, requesting the default cursor factory.
			super(context, DBNAME, null, DBVERSION);		
    	}
    	
        @Override
        public void onCreate(SQLiteDatabase db) 
        {
        	// Create table
            db.execSQL("CREATE TABLE " + Cells.TABLE_NAME + " ("
                    + Cells._ID + " INTEGER PRIMARY KEY,"
                    + Cells.COLUMN_ID + " INTEGER,"
                    + Cells.COLUMN_CELL + " INTEGER"
                    + ");");
            Log.i("DB", "Table created!");
            
            // Populate table
            for (int i=0; i< MetaHome.NUM_ITEMS; i++)
            {
        		ContentValues values	= new ContentValues();
        		values.put(Cells.COLUMN_ID, i);
        		values.put(Cells.COLUMN_CELL, -1);             
                long row = db.insert(Cells.TABLE_NAME, null, values);
                Log.i("DB", "Row " + row); 
            }            
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            db.execSQL("DROP TABLE IF EXISTS " + Cells.TABLE_NAME);
            onCreate(db);
        }        
    }
	
	public DatabaseMapData(Context context) {
		mOpenHelper = new DatabaseHelper(context);
		
		// Creates database and table if they do not exist
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();		
	}
	
	public void set(int index, int value) {
		
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		ContentValues values	= new ContentValues();
		values.put(Cells.COLUMN_CELL, value);
		db.update(Cells.TABLE_NAME, values, Cells.COLUMN_ID+"=?", new String[] {Long.toString(index)});
	}
	
	public int get(int index) {
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor cur	= db.query(Cells.TABLE_NAME,
							   new String[] {Cells.COLUMN_CELL},
							   Cells.COLUMN_ID+"=?",
							   new String[] {Long.toString(index)},
							   null,
							   null,
							   null);
		int value	= -1;	// default
		if (cur.moveToFirst())
			value	= cur.getInt(0);
		
		cur.close();
		return value;
	}
	
	public int[] getAll() {
		int[] cells	= new int[MetaHome.NUM_ITEMS];
		for (int i=0; i<cells.length; i++)
			cells[i]	= -1;	// default values
		
		
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor cur	= db.query(Cells.TABLE_NAME,
							   new String[] {Cells.COLUMN_ID, Cells.COLUMN_CELL},
							   null,
							   null,
							   null,
							   null,
							   null,
							   Long.toString(MetaHome.NUM_ITEMS));
		
		while (cur.moveToNext() )	// cursor starts from the first item 
		{
			int idx		= cur.getInt(0);
			int value	= cur.getInt(1); 
			if (idx >= 0 && idx < MetaHome.NUM_ITEMS) {
				cells[idx]	= value;
				Log.i("DB", "cells["+idx+"] = "+value);
			}
		}
		cur.close();
		return cells;
	}
	
	public void remove(int index) {
		
		set(index, -1);
	}
	
	public boolean isEmpty(int index) {
		return get(index) == -1;
	}
	
	public int size() {
		return MetaHome.NUM_ITEMS;
	}
}


class WebMapData implements IMapData
{
	// Local endpoint: http://192.168.1.144/index.php
	private static final String ENDPOINT 	= "http://surfingbits.com/metahome/index.php";
	private static final String KEY 		= "d3rw56h8";

	public WebMapData(Context context) {
		// context is not used here
	}
	
	public void set(int index, int value) {
		HashMap<String, String> params	= new HashMap<String, String>();
		params.put("action", "set");
		params.put("cell", Long.toString(index));
		params.put("value", Long.toString(value));
		JSONObject js	= getContentObject(params);
		
		boolean status;
		try {
			status	= js.getBoolean("status");
			Log.i("Web", "Set value cell["+index+"] = "+value+": "+status);
		} catch (Exception e) {
			Log.e("WebError", e.toString());
		}
	}
	
	public int get(int index) {
		HashMap<String, String> params	= new HashMap<String, String>();
		params.put("action", "get");
		params.put("cell", Long.toString(index));
		JSONObject js	= getContentObject(params);
		int value	= 0;
		try {
			value	= js.getInt("value");
			Log.i("Web", "Get value cell["+index+"]: "+value);
		} catch (Exception e) {
			Log.e("WebError", e.toString());
		}
		return value;
	}
	
	public int[] getAll()
	{
		int[] cells		= new int[MetaHome.NUM_ITEMS];
		for (int i=0; i<cells.length; i++)
			cells[i]	= -1;	// default values
		
		HashMap<String, String> params	= new HashMap<String, String>();
		params.put("action", "getall");
		JSONObject js	= getContentObject(params);
		
		try {
			JSONArray arr	= js.getJSONArray("cells");
			for (int i=0; i< arr.length() && i<cells.length; i++)
				cells[i]	= arr.getInt(i);	//Integer.parseInt(arr.getString(i));
			Log.i("Web", "Get all: "+arr.toString());
		} catch (Exception e) {
			Log.e("WebError", e.toString());
		}		
		
		return cells;
	}
	
	public void remove(int index) {
		HashMap<String, String> params	= new HashMap<String, String>();
		params.put("action", "remove");
		params.put("cell", Long.toString(index));
		JSONObject js	= getContentObject(params);

		boolean status;
		try {
			status	= js.getBoolean("status");
			Log.i("Web", "Remove cell cell["+index+"]: "+status);
		} catch (Exception e) {
			Log.e("WebError", e.toString());
		}
	}
	
	public boolean isEmpty(int index) {
		HashMap<String, String> params	= new HashMap<String, String>();
		params.put("action", "empty");
		params.put("cell", Long.toString(index));
		JSONObject js	= getContentObject(params);
		
		boolean empty;
		try {
			empty 	= js.getBoolean("empty");
			Log.i("Web", "Is empty: " + empty);
		} catch (Exception e) {
			empty 	= true;
			Log.e("WebError", e.toString());
		}
		return empty;
	}
	
	public int size() {
		HashMap<String, String> params	= new HashMap<String, String>();
		params.put("action", "size");
		JSONObject js	= getContentObject(params);
		int size	= 0;
		try {
			size	= js.getInt("size");
		} catch (Exception e) {
			Log.e("Web", e.toString());
		}
		
		return size;
	}
	
	private JSONObject getContentObject(HashMap<String, String> params)
	{
		String js = "";
		params.put("key", KEY);
				
		try
		{ 
			// Create a URLConnection object for a URL
		    URL url = new URL(createUrl(params));
		    URLConnection conn 		= url.openConnection();
		    InputStream response 	= new BufferedInputStream(conn.getInputStream());
		    BufferedReader reader 	= new BufferedReader(new InputStreamReader(response));
		    js	= reader.readLine();
		    Log.i("Web", js);
		}catch (Exception e) {
			Log.e("WebError", e.toString());
		}
				
		try {
			return new JSONObject(js);
		} catch (Exception e) {
			Log.e("Json", e.toString());
			return null;
		}
		
	}
	
	public static String createUrl(HashMap<String, String> params)
	{
		Iterator keyIter = params.keySet().iterator();
		ArrayList<String> ps	= new ArrayList<String>();
		int i	= 0;
		while(keyIter.hasNext()) {
			String	key	= (String)keyIter.next();
		    ps.add(key + "=" + params.get(key));
		}
		return ENDPOINT + "?" + join(ps, "&");
	}
	
	public static String join(Collection s, String delimiter) {
	    StringBuffer buffer = new StringBuffer();
	    Iterator iter = s.iterator();
	    while (iter.hasNext()) {
	        buffer.append(iter.next());
	        if (iter.hasNext()) {
	            buffer.append(delimiter);
	        }
	    }
	    return buffer.toString();
	}	
	
}
