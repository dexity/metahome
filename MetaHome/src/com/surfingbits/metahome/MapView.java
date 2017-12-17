
// Copyright 2011 Alex Dementsov

package com.surfingbits.metahome;

import com.surfingbits.metahome.ImageText;

import android.content.Context;
import android.graphics.RectF;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;
import android.graphics.DashPathEffect;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.surfingbits.metahome.*;

/*
 * Three different memory sources:
 * 		- MemoryMapData
 * 		- DatabaseMapData
 * 		- WebMapData
 * 
 */


public class MapView extends View {
	
    private static final int MARGIN = 10;
    private static final int MAPSIZE = 230;
    private static final int bwidth = 100;
	
    private Rect[] squares	= new Rect[MetaHome.NUM_ITEMS];
    private IMapData data;			// Data handler. Can be both local and remote: "memory", "database", "webservice"
    private IMapData memory;		// Storage for memory source
    private IMapData ldata;			// Local storage for the data (cache)
    private ImageText[] imagetext	= new ImageText[MetaHome.NUM_ITEMS];
    
    private int mSelectedCell = -1; 
    private ShapeDrawable mShapeDrawable;
    private ICellListener mCellListener;
	
    public interface ICellListener {
        abstract void onCellSelected();
    }    
    
    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        requestFocus();

        if (memory != null)
        	data	= memory;
        else {
        	data 	= new MemoryMapData(context);	// Start with memory
        	memory	= data;
        }        
        
        //data 	= new MemoryMapData(context);	// Start with memory
        
        ldata = new MemoryMapData(context);
        refreshCache(data);
        
        squares[0]	= new Rect(MARGIN, MARGIN, MARGIN+bwidth, MARGIN+bwidth);
        squares[1]	= new Rect(2*MARGIN+bwidth, MARGIN, 2*(MARGIN+bwidth), MARGIN+bwidth);
        squares[2]	= new Rect(MARGIN, 2*MARGIN+bwidth, MARGIN+bwidth, 2*(MARGIN+bwidth));
        squares[3]	= new Rect(2*MARGIN+bwidth, 2*MARGIN+bwidth, 2*(MARGIN+bwidth), 2*(MARGIN+bwidth));
    }
    
    public void setImageText(ImageText[] imagetext)
    {
    	this.imagetext = imagetext;
    }
    
    public void setCell(int cellIndex, int itemIndex) {
    	data.set(cellIndex, itemIndex);
    	ldata.set(cellIndex, itemIndex);
        invalidate();
    }    
    
    public void removeCell(int cellIndex) {
    	data.remove(cellIndex);
    	ldata.remove(cellIndex);
        invalidate();
    }        
    
    @Override
    protected void onDraw(Canvas canvas) 
    {
        super.onDraw(canvas);
        
        float[] outerR = new float[] { 10, 10, 10, 10, 10, 10, 10, 10 };
        
        mShapeDrawable = new ShapeDrawable(new RoundRectShape(outerR, null, null));
        mShapeDrawable.getPaint().setColor(0xcccccccc);
        mShapeDrawable.setBounds(0, 0, MAPSIZE, MAPSIZE); 
        mShapeDrawable.draw(canvas);

        for (int i = 0; i < ldata.size(); i++)
        {
            if (ldata.isEmpty(i))
            {
            	drawPlace(canvas, i);
            }
            else {
            	drawIcon(canvas, i);
            }
        }
    }

    public void setMemorySource(Context context)
    {
    	if (data instanceof MemoryMapData)
    	{
    		refreshCache(data);
    		invalidate();
    		return;
    	}
    	
    	if (memory != null)
    		data	= memory;
    	else
    		data	= new MemoryMapData(context);
    	refreshCache(data);
    	invalidate();
    	Log.i("MapView", "MemoryMapData is set");
    }
    
    public void setDatabaseSource(Context context)
    {
    	if (data instanceof DatabaseMapData)
    		return;
    	
    	if (data instanceof MemoryMapData)
    		memory	= data;		// save
    	data	= new DatabaseMapData(context);
    	refreshCache(data);
    	invalidate();
    	Log.i("MapView", "DatabaseMapData is set");    	
    }
    
    public void setWebserviceSource(Context context)
    {
    	if (data instanceof WebMapData)
    		return;
    	
    	if (data instanceof MemoryMapData)
    		memory	= data;   	// save
    	data	= new WebMapData(context);
    	refreshCache(data);
    	invalidate();
    	Log.i("MapView", "WebMapData is set");    	    	
    }    
    
    private void refreshCache(IMapData d)
    {
    	// Gets all elements and refreshes the local storage
    	int[] cells	= d.getAll();
    	for (int i=0; i<cells.length; i++)
    		ldata.set(i, cells[i]);
    }
    
    public void setCellListener(ICellListener cellListener) {
        mCellListener = cellListener;
    }    
    
    public void drawPlace(Canvas canvas, int idx)
    {
    	float[] outR = new float[] { 8, 8, 8, 8, 8, 8, 8, 8};
    	DashPathEffect dashPath = new DashPathEffect(new float[]{5, 5}, 0);
    	Rect r	= squares[idx];
    	
    	Paint paint;
    	ShapeDrawable sdraw;
        sdraw	= new ShapeDrawable(new RoundRectShape(outR, null, null));        
        paint	= sdraw.getPaint();
        paint.setColor(0x33333333);
        paint.setStyle(Style.STROKE);
        paint.setPathEffect(dashPath);
        paint.setStrokeWidth(3);
        sdraw.setBounds(r.left+2, r.top+2, r.right-2, r.bottom-2); 
        sdraw.draw(canvas);
    }
     
    public void drawIcon(Canvas canvas, int idx)
    {
    	canvas.drawBitmap(imagetext[ldata.get(idx)].icon, null, squares[idx], null);
    }    
    
    public int getCellIndex(int x, int y)
    {
    	Rect r;
    	for (int i=0; i < squares.length; i++)
    	{
    		r	= squares[i];
    		if (x >= r.left && x < r.right && y >= r.top && y < r.bottom)
    			return i;
    	}
    	return -1;
    }
    
    public int getSelectedCell()
    {
    	return mSelectedCell;
    }
    
    public IMapData getData()
    {
    	return ldata;	// Returns local storage
    }    
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            int cell	= getCellIndex(x, y);
            mSelectedCell	= cell;

            if (isEnabled() && cell != -1) 
            {                
                if (mCellListener != null) {
                    mCellListener.onCellSelected();
                }
            }

            return true;
        }

        return false;
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

    }
    
    
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();        
        
        Parcelable s = super.onSaveInstanceState();
        b.putParcelable("map_super_state", s);        
        b.putIntArray("map_cells", dumpCells(ldata));
        b.putIntArray("memory_cells", dumpCells(memory));
        return b;
    }
    
    @Override
    protected void onRestoreInstanceState(Parcelable state) 
    {
        Bundle b = (Bundle) state;
        Parcelable superState = b.getParcelable("map_super_state");
        loadCells(ldata, b, "map_cells");
        loadCells(memory, b, "memory_cells");
        
        super.onRestoreInstanceState(superState);
    }
    
    private int[] dumpCells(IMapData d)
    {
    	if (d == null)
    		return null;
    	
        int[] cells = new int[d.size()];
        for (int i=0; i<cells.length; i++)
        	cells[i] = d.get(i);
        
        return cells;
    }
    
    private void loadCells(IMapData d, Bundle b, String name)
    {
        int[] cells = b.getIntArray(name);
        if (cells == null || d == null)
        	return;
        for (int i=0; i<cells.length; i++)
        	d.set(i, cells[i]);    	
    }
    
}
