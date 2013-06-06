package com.jebussystems.levelingglass.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.jebussystems.levelingglass.R;

public class AudioLevelView extends View
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final int SILENCE_LEVEL_IN_DB = -98;
	private static final int DEFAULT_COLOR = Color.BLUE;

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	// /////////////////////////////////////////////////////////////////////////
	// variables
	// /////////////////////////////////////////////////////////////////////////

	private int level = SILENCE_LEVEL_IN_DB;
	private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private RectF rect;
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
			this.level = a.getInteger(R.styleable.AudioLevel_level,
			        SILENCE_LEVEL_IN_DB);
			this.color = a
			        .getColor(R.styleable.AudioLevel_color, DEFAULT_COLOR);
		}
		finally
		{
			a.recycle();
		}
		// set the paint to use
		this.paint.setColor(this.color);
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
			// calculate the drawing size
			this.rect = calculateDrawingArea(getWidth(), getHeight());
			// redraw
			invalidate();
		}
	}

	public Integer getLevel()
	{
		return level;
	}

	public void setColor(int color)
	{
		if (this.color != color)
		{
			// store the color
			this.color = color;
			// redraw
			invalidate();
		}
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
		// calculate the drawing size
		this.rect = calculateDrawingArea(width, height);
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

	private RectF calculateDrawingArea(int width, int height)
	{
		// Account for padding
		float xpad = (float) (getPaddingLeft() + getPaddingRight());
		float ypad = (float) (getPaddingTop() + getPaddingBottom());

		float ww = (float) width - xpad;
		float hh = (float) height - ypad;

		// calculate the percentage of pixels we want to fill
		float percent = 100.0f - 100.0f * ((float) this.level / (float) SILENCE_LEVEL_IN_DB);
		// calculate the number of pixels we need to draw
		float pixels = ww * percent;
		
		// size the drawing rectangle
		return new RectF(getPaddingLeft(), getPaddingTop(), pixels, hh);
	}
}
