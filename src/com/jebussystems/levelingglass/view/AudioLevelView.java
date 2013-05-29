package com.jebussystems.levelingglass.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.jebussystems.levelingglass.R;

public class AudioLevelView extends View
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final int SILENCE_LEVEL_IN_DB = -90;
	private static final int INVALID_LEVEL = -1;
	private static final int INVALID_COLOR = Color.GRAY;
	private static final int DEFAULT_COLOR = Color.BLUE;

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// variables
	// /////////////////////////////////////////////////////////////////////////

	private int level = INVALID_LEVEL;
	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Rect rect;
	private int color = DEFAULT_COLOR;

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public AudioLevelView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		// initialize our attributes using the info from the attribute set
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
		        R.styleable.AudioLevel, 0, 0);
		try
		{
			this.level = a.getInteger(R.styleable.AudioLevel_level, INVALID_LEVEL);
			this.color = a.getColor(R.styleable.AudioLevel_color, DEFAULT_COLOR);
		}
		finally
		{
			a.recycle();
		}
		// set the paint to use the invalid color until we have a real value
		this.paint.setColor(INVALID_COLOR);
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
			float percent = 100.0f + ((float) this.level / (float) SILENCE_LEVEL_IN_DB);
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
		this.level = INVALID_LEVEL;
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
	public void onSizeChanged(int width, int height, int oldwidth, int oldheight)
	{
		// size the drawing rectangle
		this.rect = new Rect(0, 0, width, height);
	}

	
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
