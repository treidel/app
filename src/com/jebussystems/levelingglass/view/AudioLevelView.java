package com.jebussystems.levelingglass.view;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.StringTokenizer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;

import android.view.View;

import com.jebussystems.levelingglass.R;
import com.jebussystems.levelingglass.util.LogWrapper;

public class AudioLevelView extends View
{
	// /////////////////////////////////////////////////////////////////////////
	// constants
	// /////////////////////////////////////////////////////////////////////////

	private static final String TAG = "view.audiolevel";

	private static final int DEFAULT_CEILING = 0;
	private static final int DEFAULT_FLOOR = -24;
	private static final int DEFAULT_LEVEL = DEFAULT_FLOOR;
	private static final int DEFAULT_NORMAL_COLOR = Color.GREEN;
	private static final int DEFAULT_WARNING_COLOR = Color.YELLOW;
	private static final int DEFAULT_ERROR_COLOR = Color.RED;
	private static final int DEFAULT_HOLD_COLOR = Color.GRAY;
	private static final int DEFAULT_MARK_COLOR = Color.DKGRAY;

	// /////////////////////////////////////////////////////////////////////////
	// types
	// /////////////////////////////////////////////////////////////////////////

	private static class Label
	{
		public int x;
		public int y;
		public String text;

	}

	// /////////////////////////////////////////////////////////////////////////
	// variables
	// /////////////////////////////////////////////////////////////////////////

	// variables populated by attribute or at runtime
	private final int floor;
	private final int ceiling;
	private final int ok;
	private final int warning;
	private final Set<Integer> marks = new HashSet<Integer>();
	private float level;
	private Float hold;

	// internal paint variables
	private Paint okPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint warningPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint errorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint holdPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint markPaint = new Paint();
	private Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

	// internally calculated variables populated by onSizeChanged() whenever the
	// view size changes
	private Rect okRange;
	private Rect warningRange;
	private Rect errorRange;
	private Rect holdRange;

	// internally calculated variables used by the paint routine
	private final Rect okRect = new Rect();
	private final Rect warningRect = new Rect();
	private final Rect errorRect = new Rect();
	private final Rect holdRect = new Rect();
	private final Collection<Path> markPaths = new LinkedList<Path>();
	private final Collection<Label> labels = new LinkedList<Label>();

	// /////////////////////////////////////////////////////////////////////////
	// constructors
	// /////////////////////////////////////////////////////////////////////////

	public AudioLevelView(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		LogWrapper.v(TAG, "AudioLevelView::AudioLevelView enter", "this=",
		        this, "context=", context, "attrs=", attrs);

		// initialize our attributes using the info from the attribute set
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs,
		        R.styleable.AudioLevel, 0, 0);
		try
		{
			int okColor = a.getColor(R.styleable.AudioLevel_ok_color,
			        DEFAULT_NORMAL_COLOR);
			int warningColor = a.getColor(R.styleable.AudioLevel_warning_color,
			        DEFAULT_WARNING_COLOR);
			int errorColor = a.getColor(R.styleable.AudioLevel_error_color,
			        DEFAULT_ERROR_COLOR);
			int holdColor = a.getColor(R.styleable.AudioLevel_hold_color,
			        DEFAULT_HOLD_COLOR);
			int markColor = a.getColor(R.styleable.AudioLevel_mark_color,
			        DEFAULT_MARK_COLOR);
			this.floor = a.getInt(R.styleable.AudioLevel_floor, DEFAULT_FLOOR);
			this.ceiling = a.getInt(R.styleable.AudioLevel_ceiling,
			        DEFAULT_CEILING);
			this.ok = a.getInt(R.styleable.AudioLevel_ok, DEFAULT_CEILING);
			this.warning = a.getInt(R.styleable.AudioLevel_warning,
			        DEFAULT_CEILING);
			this.level = a
			        .getFloat(R.styleable.AudioLevel_level, DEFAULT_LEVEL);
			this.hold = a.getFloat(R.styleable.AudioLevel_hold, DEFAULT_LEVEL);
			String marks = a.getString(R.styleable.AudioLevel_marks);
			if (null != marks)
			{
				// marks are comma separated integers
				StringTokenizer tokenizer = new StringTokenizer(marks, ",");
				while (true == tokenizer.hasMoreTokens())
				{
					String token = tokenizer.nextToken();
					this.marks.add(Integer.valueOf(token));
				}
			}

			// set the paint to use
			this.okPaint.setColor(okColor);
			this.warningPaint.setColor(warningColor);
			this.errorPaint.setColor(errorColor);
			this.holdPaint.setColor(holdColor);
			this.markPaint.setColor(markColor);
			this.markPaint.setStrokeWidth(1f);
			this.markPaint.setStyle(Paint.Style.STROKE);
			this.markPaint.setStrokeJoin(Paint.Join.BEVEL);
			this.textPaint.setColor(markColor);
			this.textPaint.setStyle(Paint.Style.STROKE);
			this.textPaint.setStrokeWidth(1);
			this.textPaint.setTextSize(16);
			this.textPaint.setTextAlign(Paint.Align.CENTER);
		}
		finally
		{
			a.recycle();
		}

		LogWrapper.v(TAG, "AudioLevelView::AudioLevelView exit");
	}

	// /////////////////////////////////////////////////////////////////////////
	// public methods
	// //////////////////////////////////////////s///////////////////////////////

	public void setLevel(float level)
	{
		LogWrapper.v(TAG, "AudioLevelView::setLevel enter", "this=", this,
		        "level=", level);
		if (this.level != level)
		{
			// set the level
			this.level = level;
			// force a recalculation of the dynamic values
			updateDynamicValues();
			// redraw
			invalidate();
		}
		LogWrapper.v(TAG, "AudioLevelView::setLevel exit");
	}

	public float getLevel()
	{
		return level;
	}

	public void setHold(Float hold)
	{
		LogWrapper.v(TAG, "AudioLevelView::setHold enter", "this=", this,
		        "hold=", hold);
		if (this.hold != hold)
		{
			// set the level
			this.hold = hold;
			// force a recalculation of the dynamic values
			updateDynamicValues();
			// redraw
			invalidate();
		}
		LogWrapper.v(TAG, "AudioLevelView::setHold exit");
	}

	public Float getHold()
	{
		return hold;
	}

	public int getCeiling()
	{
		return ceiling;
	}

	public int getFloor()
	{
		return floor;
	}

	public int getOK()
	{
		return ok;
	}

	public int getWarning()
	{
		return warning;
	}

	// /////////////////////////////////////////////////////////////////////////
	// View overrides
	// /////////////////////////////////////////////////////////////////////////

	@Override
	public void onSizeChanged(int width, int height, int oldwidth, int oldheight)
	{
		LogWrapper.v(TAG, "AudioLevelView::onSizeChanged enter", "this=", this,
		        "width=", width, "height=", height, "oldwidth=", oldwidth,
		        "oldheight=" + oldheight);
		
		LogWrapper.d(TAG, "sizing view for width=", width, "height=", height);
		
		// calculate the offsets
		int xOffset = getPaddingLeft() + 5;
		int yOffset = getPaddingTop();
		int xRange = width - getPaddingLeft() - getPaddingRight() - 10;
		int yRange = height - getPaddingTop() - getPaddingBottom();

		// calculate how 'wide' the level space is
		int span = getCeiling() - getFloor();

		// calculate the maximum widths of each band
		int okRange = (int) (((float) (getOK() - getFloor()) * (float) xRange) / (float) span);
		int warningRange = (int) (((float) (getWarning() - getOK()) * (float) xRange) / (float) span);
		int errorRange = (int) (((float) (getCeiling() - getWarning()) * (float) xRange) / (float) span);

		// calculate space for the marks + labels
		int yMark = (int) ((float) yRange * 0.15);
		int yLabel = (int) ((float) yRange * 0.30);

		// calculate space reserved for the meter itself
		int yMeter = yRange - yMark - yLabel;

		// calculate the ok range
		this.okRange = new Rect(xOffset, yOffset, xOffset + okRange, yOffset
		        + yMeter);
		this.warningRange = new Rect(this.okRange.right, this.okRange.top,
		        this.okRange.right + warningRange, this.okRange.bottom);
		this.errorRange = new Rect(this.warningRange.right,
		        this.warningRange.top, this.warningRange.right + errorRange,
		        this.warningRange.bottom);
		this.holdRange = new Rect(this.okRange.left, this.okRange.top,
		        this.errorRange.right, this.okRange.bottom);

		// clear any existing marks + labels
		this.markPaths.clear();
		this.labels.clear();

		// iterate through all marks to draw
		for (int mark : this.marks)
		{
			// calculate the position of the mark
			int markPosition = xOffset
			        + (((mark - getFloor()) * xRange) / span);
			// create the path
			Path path = new Path();
			path.moveTo(markPosition, yOffset + yMeter);
			path.lineTo(markPosition, yOffset + yMeter + yMark);
			// add the path to the list to draw
			this.markPaths.add(path);
			// create the label
			Label label = new Label();
			label.x = markPosition;
			label.y = yOffset + yMeter + yMark + yLabel
			        + (int) (0.15f * this.textPaint.getTextSize());
			label.text = String.valueOf(mark);
			// add the label
			this.labels.add(label);
		}

		// calculate the drawing rects
		updateDynamicValues();

		LogWrapper.v(TAG, "AudioLevelView::onSizeChanged exit");
	}

	@Override
	protected void onDraw(Canvas canvas)
	{
		LogWrapper.v(TAG, "AudioLevelView::onDraw enter", "this=", this,
		        "canvas=", canvas);
		// call the super class
		super.onDraw(canvas);
		// draw the error rect first then the warning rect then the ok rect in
		// to avoid
		// overlap
		canvas.drawRect(this.errorRect, this.errorPaint);
		canvas.drawRect(this.warningRect, this.warningPaint);
		canvas.drawRect(this.okRect, this.okPaint);
		canvas.drawRect(this.holdRect, this.holdPaint);
		// draw each mark
		for (Path path : this.markPaths)
		{
			canvas.drawPath(path, this.markPaint);
		}
		// draw each label
		for (Label label : this.labels)
		{
			canvas.drawText(label.text, label.x, label.y, this.textPaint);
		}
		LogWrapper.v(TAG, "AudioLevelView::onDraw exit");
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

	private void updateDynamicValues()
	{
		// figure out the range of possible levels
		int span = getCeiling() - getFloor();

		// see if we're in the error range, warning range or ok range
		if (getLevel() > (float) getWarning())
		{
			// ok + warning are both full
			this.warningRect.set(this.warningRange);
			this.okRect.set(this.okRange);
			// calculate the percent of pixels to draw
			float percent = (getLevel() - (float) getWarning())
			        / (float) (getCeiling() - getWarning());
			// handle clipping
			percent = Math.min(percent, 1.0f);
			// calculate the number of pixels we need to draw
			int pixels = (int) ((float) this.errorRange.width() * percent);
			// size the drawing rectangle
			this.errorRect.set(this.errorRange.left, this.errorRange.top,
			        this.errorRange.left + pixels, this.errorRange.bottom);

		}
		else if (getLevel() > (float) getOK())
		{
			// ok is full, error is empty
			this.okRect.set(this.okRange);
			this.errorRect.setEmpty();
			// calculate the percent of pixels to draw
			float percent = (getLevel() - (float) getOK())
			        / (float) (getWarning() - getOK());
			// handle clipping (shouldn't happen)
			percent = Math.min(percent, 1.0f);
			// calculate the number of pixels we need to draw
			int pixels = (int) ((float) this.warningRange.width() * percent);
			// size the drawing rectangle
			this.warningRect.set(this.warningRange.left, this.warningRange.top,
			        this.warningRange.left + pixels, this.warningRange.bottom);
		}
		else
		{
			// warning + error empty
			this.warningRect.setEmpty();
			this.errorRect.setEmpty();
			// calculate the percent of pixels to draw
			float percent = (getLevel() - (float) getFloor())
			        / (float) (getOK() - getFloor());
			// handle clipping
			percent = Math.max(percent, 0.0f);
			// calculate the number of pixels we need to draw
			int pixels = (int) ((float) this.okRange.width() * percent);
			// size the drawing rectangle
			this.okRect.set(this.okRange.left, this.okRange.top,
			        this.okRange.left + pixels, this.okRange.bottom);
		}

		// if we have a hold then calculate where it should be placed
		if (null != getHold())
		{
			// calculate the percentage point of the hold
			float percent = (getHold() - getFloor()) / (float) span;
			// handle clipping
			percent = Math.min(percent, 1.0f);
			percent = Math.max(percent, 0.0f);
			// calculate the number of pixels in we need to draw
			int pixels = (int) ((float) this.holdRange.width() * percent);
			// size the drawing rectangle
			this.holdRect.set(this.holdRange.left + pixels, this.holdRange.top,
			        this.holdRange.left + pixels + 1, this.holdRange.bottom);
		}
		else
		{
			// clear the rect
			this.holdRect.setEmpty();
		}
	}
}
