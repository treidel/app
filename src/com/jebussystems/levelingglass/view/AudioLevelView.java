package com.jebussystems.levelingglass.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.jebussystems.levelingglass.R;

public class AudioLevelView extends View
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "view.audiolevel";

	private static final int DEFAULT_CEILING_IN_DB = 0;
	private static final int DEFAULT_FLOOR_IN_DB = -100;
	private static final int DEFAULT_LEVEL_IN_DB = DEFAULT_FLOOR_IN_DB;
	private static final int DEFAULT_COLOR = Color.BLUE;

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// variables
	// /////////////////////////////////////////////////////////////////////////

	private int floor;
	private int ceiling;
	private int level;
	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private RectF rect;
	private int color = DEFAULT_COLOR;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public AudioLevelView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		Log.v(TAG, "AudioLevelView::AudioLevelView enter context=" + context
		        + " attrs=" + attrs);

		// initialize our attributes using the info from the attribute set
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
		        R.styleable.AudioLevel, 0, 0);
		try
		{
			this.color = a
			        .getColor(R.styleable.AudioLevel_color, DEFAULT_COLOR);
			this.floor = a.getInt(R.styleable.AudioLevel_floor_in_db,
			        DEFAULT_FLOOR_IN_DB);
			this.ceiling = a.getInt(R.styleable.AudioLevel_ceiling_in_db,
			        DEFAULT_CEILING_IN_DB);
			this.level = a.getInt(R.styleable.AudioLevel_level_in_db,
			        DEFAULT_LEVEL_IN_DB);
		}
		finally
		{
			a.recycle();
		}
		// set the paint to use
		this.paint.setColor(this.color);

		Log.v(TAG, "AudioLevelView::AudioLevelView exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void setColor(int color)
	{
		Log.v(TAG, "AudioLevelView::setColor enter color=" + color);
		if (this.color != color)
		{
			// store the colord sr
			this.color = color;

			// redraw
			invalidate();
		}
		Log.v(TAG, "AudioLevelView::setColor exit");
	}

	public int getColor()
	{
		return this.color;
	}

	public void setLevel(int level)
	{
		Log.v(TAG, "AudioLevelView::setLevel enter level=" + level);
		if (this.level != level)
		{
			// set the level
			this.level = level;
			// calculate the drawing size
			this.rect = calculateDrawingArea(getWidth(), getHeight());
			// redraw
			invalidate();
		}
		Log.v(TAG, "AudioLevelView::setLevel exit");
	}

	public int getLevel()
	{
		return level;
	}

	public void setCeiling(int ceiling)
	{
		Log.v(TAG, "AudioLevelView::setCeiling enter ceiling=" + ceiling);
		// store the ceiling
		this.ceiling = ceiling;
		// calculate the drawing size
		this.rect = calculateDrawingArea(getWidth(), getHeight());
		// redraw
		invalidate();
		Log.v(TAG, "AudioLevelView::setCeiling exit");
	}

	public int getCeiling()
	{
		return ceiling;
	}

	public void setFloor(int floor)
	{
		Log.v(TAG, "AudioLevelView::setFloor enter floor=" + floor);
		// store the floor
		this.floor = floor;
		// calculate the drawing size
		this.rect = calculateDrawingArea(getWidth(), getHeight());
		// redraw
		invalidate();
		Log.v(TAG, "AudioLevelView::setFloor exit");
	}

	public int getFloor()
	{
		return floor;
	}

	// /////////////////////////////////////////////////////////////////////////
	// View overrides
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void onSizeChanged(int width, int height, int oldwidth, int oldheight)
	{
		Log.v(TAG, "AudioLevelView::onSizeChanged enter width=" + width
		        + " height=" + height + " oldwidth=" + oldwidth + " oldheight="
		        + oldheight);
		// calculate the drawing size
		this.rect = calculateDrawingArea(width, height);
		Log.v(TAG, "AudioLevelView::onSizeChanged exit");
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		Log.v(TAG, "AudioLevelView::onDraw enter canvas=" + canvas);
		// call the super class
		super.onDraw(canvas);
		// giv'er
		canvas.drawRect(this.rect, this.paint);
		Log.v(TAG, "AudioLevelView::onDraw exit");
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

	private RectF calculateDrawingArea(int width, int height)
	{
		// Account for padding
		float xpad = (float) (getPaddingLeft() + getPaddingRight());
		float ypad = (float) (getPaddingTop() + getPaddingBottom());

		float ww = (float) width - xpad;
		float hh = (float) height - ypad;

		// calculate the percent of pixels to draw
		float percent = 1.0f - ((float) getLevel() / ((float) getCeiling() + (float) getFloor()));
		// handle corners
		if (level > getCeiling())
		{
			percent = 1.0f;
		}
		else if (level < getFloor())
		{
			percent = 0.0f;
		}

		// calculate the number of pixels we need to draw
		float pixels = ww * percent;

		// size the drawing rectangle
		return new RectF(getPaddingLeft(), getPaddingTop(), pixels, hh);
	}
}
