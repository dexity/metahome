// Copyright 2011 Alex Dementsov

package com.surfingbits.metahome;

import com.surfingbits.metahome.R;

import android.app.Dialog;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;
import java.util.ArrayList;

import com.surfingbits.metahome.MapView.ICellListener;
import com.surfingbits.metahome.*;

public class MetaHome extends ListActivity 
{
	public static final int NUM_ITEMS = 4;
	private static final int DIALOG_ITEMS = 1;
	private static final int DIALOG_REPLACE = 2;
	private MapView mMapView;
	private TextView mMapText;	
	private int source = 0;		
	private int selectedCell;
	private int selectedItem;
	private Menu mMenu;
	private static final String Status[] = {"Memory", "Database", "WebService"}; 
	private static ArrayList<ImageText> items = new ArrayList<ImageText>();	// Dynamically populated list
	private ImageText[] imagetext	= new ImageText[NUM_ITEMS];
	MyAdapter adapter;
	
	
	private static class MyAdapter extends BaseAdapter 
	{
        private LayoutInflater mInflater;
        
        public MyAdapter(Context context) 
        {
            // Cache the LayoutInflate to avoid asking for a new one each time.
            mInflater = LayoutInflater.from(context);
        }        

        public int getCount() {
            return items.size();
        }
        
        public Object getItem(int position) {
            return position;
        }
        
        public long getItemId(int position) {
            return position;
        }
        
        public View getView(int position, View convertView, ViewGroup parent) 
        {
            // A ViewHolder keeps references to children views to avoid unneccessary calls
            // to findViewById() on each row.
            ViewHolder holder;

            // When convertView is not null, we can reuse it directly, there is no need
            // to reinflate it. We only inflate a new View when the convertView supplied
            // by ListView is null.
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.list_item_icon_text, null);

                // Creates a ViewHolder and store references to the two children views
                // we want to bind data to.
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.text);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);

                convertView.setTag(holder);
            } else {
                // Get the ViewHolder back to get fast access to the TextView
                // and the ImageView.
                holder = (ViewHolder) convertView.getTag();
            }
            
            if (items.get(position) != null)
            {
            	// Bind the data efficiently with the holder.
            	holder.text.setText(items.get(position).text);	
            	holder.icon.setImageBitmap(items.get(position).icon);
            }
            return convertView;
        }
        
        
        static class ViewHolder {
            TextView text;
            ImageView icon;
        }        
	}
	
	
    @Override
    protected Dialog onCreateDialog(int id) 
    {
        switch (id) {
        case DIALOG_ITEMS:
        	return new AlertDialog.Builder(MetaHome.this)
        		.setTitle(R.string.dialog_select_title)
                .setSingleChoiceItems(getText(), 0, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                    	// Set which button is clicked
                    	selectedItem	= whichButton;
                    }
                })        		
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                    	onSelectItem();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                        // Do nothing
                    }
                })
                .setIcon(R.drawable.menu_edit)
        		.create();
        case DIALOG_REPLACE:
            return new AlertDialog.Builder(MetaHome.this)
                .setTitle(R.string.dialog_replace_title)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {
                    	onRemoveItem();
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) 
                    {

                    }
                })
                .setIcon(R.drawable.menu_delete)
                .create();        	
        }
        
        return null;
    }
    	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        setImageText();
        
        mMapView = (MapView) findViewById(R.id.map_view);
        mMapView.setFocusable(true); 
        mMapView.setFocusableInTouchMode(true);
        mMapView.setCellListener(new MyCellListener());
        mMapView.setImageText(imagetext);
        
        mMapText	= (TextView) findViewById(R.id.status);
        
        adapter	= new MyAdapter(this);
        setListAdapter(adapter);
        setStatus(0, savedInstanceState);
    }
    
    public void setImageText()
    {
    	imagetext[0]	= new ImageText(BitmapFactory.decodeResource(getResources(), R.drawable.restroom), "Restroom");
    	imagetext[1]	= new ImageText(BitmapFactory.decodeResource(getResources(), R.drawable.phone), "Telephone");
    	imagetext[2]	= new ImageText(BitmapFactory.decodeResource(getResources(), R.drawable.elevator), "Elevator");
    	imagetext[3]	= new ImageText(BitmapFactory.decodeResource(getResources(), R.drawable.atm), "ATM");
    }   
    
    private class MyCellListener implements ICellListener {
        public void onCellSelected() {
        	
        	IMapData data	= mMapView.getData();
        	
        	selectedCell	= mMapView.getSelectedCell();
        	if (selectedCell == -1)
        		return;
        			
        	if (data.isEmpty(selectedCell)) // should be in range
        	{
        		showDialog(DIALOG_ITEMS);

        	} else {
        		showDialog(DIALOG_REPLACE);
        	}
        }
    }
    
    public String[] getText()
    {
    	String[] str = new String[NUM_ITEMS];
    	for (int i=0; i<imagetext.length; i++)
    		str[i]	= imagetext[i].text;
    		
    	return str;
    }
    
    public void onSelectItem()
    {
    	mMapView.setCell(selectedCell, selectedItem);
		updateList(null);         	
    }
    
    public void onRemoveItem()
    {
    	mMapView.removeCell(selectedCell);
		updateList(null);    	
    }
    
    public void updateList(Bundle state)
    {
    	items.clear();	// Remove all items
    	if (state != null)
    	{
    		// Restore list items from state
    		int[] data = state.getIntArray("list_cells");
	    	for (int i=0; i<data.length; i++)
	    	{
	    		if (data[i] != -1)
	    		{
	    			ImageText it	= new ImageText(imagetext[data[i]].icon, imagetext[data[i]].text);
	    			items.add(it);
	    		}
	    	}
	    	adapter.notifyDataSetChanged();	// populate the list view 
	    	return;
    	}
    	
		// Take data from MapView data
    	IMapData data	= mMapView.getData();
	
    	for (int i=0; i<data.size(); i++)
    	{
    		if (!data.isEmpty(i))
    		{
    			ImageText it	= new ImageText(imagetext[data.get(i)].icon, imagetext[data.get(i)].text);
    			items.add(it);
    		}
    	}

    	adapter.notifyDataSetChanged();	// populate the list view 
    }
    
 
    @Override
    protected void onSaveInstanceState(Bundle state) 
    {
    	super.onSaveInstanceState(state);
    	IMapData data	= mMapView.getData();
    	
        int[] cells = new int[data.size()];
        for (int i=0; i<cells.length; i++)
        	cells[i] = data.get(i);        
        state.putIntArray("list_cells", cells);
        state.putInt("source", source);
    }

    public void setStatus(int status, Bundle state)
    {
    	if (state != null)
    		source = state.getInt("source");
    	else
    		source = status;
    	mMapText.setText("Source: " + Status[source]);
    	updateList(state);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
   
        // Hold on to this
        mMenu = menu;
        
        // Inflate the currently selected menu XML resource.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.memory, menu);        
                
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	
        switch (item.getItemId()) {
            case R.id.memory:
            	mMapView.setMemorySource(this);
            	Toast.makeText(this, Status[0], Toast.LENGTH_SHORT).show();
            	setStatus(0, null);
            	return true;
            case R.id.database:
            	mMapView.setDatabaseSource(this);
            	Toast.makeText(this, Status[1], Toast.LENGTH_SHORT).show();
            	setStatus(1, null);
            	return true;
            case R.id.webservice:
            	mMapView.setWebserviceSource(this);
            	Toast.makeText(this, Status[2], Toast.LENGTH_SHORT).show();
            	setStatus(2, null);
            	return true;            	
        }
    	return false;
    }
   
}

// Can use ViewHolder instead?
class ImageText
{
	String text;
	Bitmap icon; 
	public ImageText(Bitmap icon, String text)
	{
		this.icon	= icon;
		this.text	= text;
	}
}	

