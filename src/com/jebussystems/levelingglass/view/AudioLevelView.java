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
	private static final int DEFAULT_NORMAL_COLOR = Color.GREEN;
	private static final int DEFAULT_PEAK_COLOR = Color.RED;

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// variables
	// /////////////////////////////////////////////////////////////////////////

	private int floor;
	private int ceiling;
	private float level;
	private Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint peakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private RectF normalRect;
	private RectF peakRect;
	private final int normalColor;
	private final int peakColor;

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
			this.normalColor = a.getColor(R.styleable.AudioLevel_normal_color,
			        DEFAULT_NORMAL_COLOR);
			this.peakColor = a.getColor(R.styleable.AudioLevel_peak_color,
			        DEFAULT_PEAK_COLOR);
			this.floor = a.getInt(R.styleable.AudioLevel_floor,
			        DEFAULT_FLOOR_IN_DB);
			this.ceiling = a.getInt(R.styleable.AudioLevel_ceiling,
			        DEFAULT_CEILING_IN_DB);
			this.level = a.getInt(R.styleable.AudioLevel_level,
			        DEFAULT_LEVEL_IN_DB);
		}
		finally
		{
			a.recycle();
		}
		// set the paint to use
		this.normalPaint.setColor(this.normalColor);
		this.peakPaint.setColor(this.peakColor);

		Log.v(TAG, "AudioLevelView::AudioLevelView exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// /////////////////////////////////////////////////////////////////////////

	public void setLevel(float level)
	{
		Log.v(TAG, "AudioLevelView::setLevel enter level=" + level);
		if (this.level != level)
		{
			// set the level
			this.level = level;
			// calculate the drawing size
			calculateDrawingArea(getWidth(), getHeight());
			// redraw
			invalidate();
		}
		Log.v(TAG, "AudioLevelView::setLevel exit");
	}

	public float getLevel()
	{
		return level;
	}

	public void setCeiling(int ceiling)
	{
		Log.v(TAG, "AudioLevelView::setCeiling enter ceiling=" + ceiling);
		// store the ceiling
		this.ceiling = ceiling;
		// calculate the drawing size
		calculateDrawingArea(getWidth(), getHeight());
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
		calculateDrawingArea(getWidth(), getHeight());
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
		calculateDrawingArea(width, height);
		Log.v(TAG, "AudioLevelView::onSizeChanged exit");
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		Log.v(TAG, "AudioLevelView::onDraw enter canvas=" + canvas);
		// call the super class
		super.onDraw(canvas);
		// giv'er
		canvas.drawRect(this.normalRect, this.normalPaint);
		if (null != this.peakRect)
		{
			canvas.drawRect(this.peakRect, this.peakPaint);
		}
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

	private void calculateDrawingArea(int width, int height)
	{
		// Account for padding
		float xpad = (float) (getPaddingLeft() + getPaddingRight());
		float ypad = (float) (getPaddingTop() + getPaddingBottom());

		// account for padding
		float ww = (float) width - xpad;
		float hh = (float) height - ypad;

		// figure out the number of pixels for each rectangle
		float ww_normal = Math.round(ww * (-getFloor()) / (getCeiling() - getFloor()));
		float ww_peak = Math.round(ww * getCeiling() / (getCeiling() - getFloor()));

		// see if we're above or below the zero level
		if (level < 0f)
		{
			// no peak rectangle needed
			this.peakRect = null;
			// // calculate the percent of pixels to draw
			float percent = 1.0f - ((float) getLevel() / ((float) getFloor()));
			// handle clipping
			if (level < getFloor())
			{
				percent = 0.0f;
			}
			// calculate the number of pixels we need to draw
			float pixels = ww_normal * percent;
			// size the drawing rectangle
			this.normalRect = new RectF(getPaddingLeft(), getPaddingTop(),
			        pixels, hh);
		}
		else
		{
			// normal rectangle is full
			this.normalRect = new RectF(getPaddingLeft(), getPaddingTop(),
			        ww_normal, hh);
			// calculate the percent of pixels to draw
			float percent = ((float) getLevel() / ((float) getCeiling()));
			// handle clipping
			if (level > getCeiling())
			{
				percent = 1.0f;
			}
			// calculate the number of pixels we need to draw
			float pixels = ww_peak * percent;

			// size the drawing rectangle
			this.peakRect = new RectF(ww_normal,
			        getPaddingTop(), ww_normal + pixels, hh);
		}
	}
}
