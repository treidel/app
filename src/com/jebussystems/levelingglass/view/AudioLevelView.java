package com.jebussystems.levelingglass.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class AudioLevelView extends View
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final int SILENCE_LEVEL_IN_DB = -90;
	private static final int INVALID_COLOR = Color.GRAY;
	private static final int DEFAULT_COLOR = Color.BLUE;
	
	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// variables
	// /////////////////////////////////////////////////////////////////////////

	private Integer level = null;
	private Paint paint = new Paint();
	private Rect rect = new Rect();
	private int color = DEFAULT_COLOR;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public AudioLevelView(Context context)
	{
		super(context);
		// set the paint to use the invalid color 
		this.paint.setColor(INVALID_COLOR);
		// fill the view
		this.rect.set(0,  0, getWidth(), getHeight());
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void setLevel(int level)
	{
		if (this.level != level)
		{
			// set the level
			this.level = Math.max(SILENCE_LEVEL_IN_DB, level);
			// calculate the percentage of pixels we want to fill
			float percent = 100.0f + ((float)this.level / (float)SILENCE_LEVEL_IN_DB);
			// calculate the number of pixels we need to draw
			int pixels = (int) ((float) getWidth() * percent);
			// setup the rectangle
			this.rect.set(0, 0, pixels, getHeight());
			// set the color
			this.paint.setColor(getColor());
			// redraw
			invalidate();
		}
	}
	
	public void clearLevel()
	{
		// clear the level
		this.level = null;
		// reset the color to invalid
		this.paint.setColor(INVALID_COLOR);
		// fill the view
		this.rect.set(0, 0, getWidth(), getHeight());
		// redraw
		invalidate();
	}

	public Integer getLevel()
	{
		return level;
	}

	public void setColor(int color)
	{
		// store the color
		this.color = color;
	}

	public int getColor()
	{
		return this.color;
	}

	// /////////////////////////////////////////////////////////////////////////
	// View overrides
	// /////////////////////////////////////////////////////////////////////////

	@Override
	protected void onDraw(Canvas canvas)
	{
		// call the super class
		super.onDraw(canvas);
		// giv'er
		canvas.drawRect(this.rect, this.paint);
	}

	// /////////////////////////////////////////////////////////////////////////
	// package protected methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// protected methods
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// private methods
	// /////////////////////////////////////////////////////////////////////////
}
