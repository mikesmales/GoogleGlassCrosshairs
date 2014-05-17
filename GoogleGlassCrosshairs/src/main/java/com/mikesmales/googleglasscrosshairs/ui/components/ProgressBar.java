package com.mikesmales.googleglasscrosshairs.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.util.AttributeSet;
import android.view.View;

import com.mikesmales.googleglasscrosshairs.R;

public class ProgressBar extends View {

	private Context context;
	private NinePatchDrawable backgroundDrawable;
	private NinePatchDrawable foregroundDrawable;
	private Paint paint = new Paint();
	
	private int width;
	private int height;
	private int percentage;

	public ProgressBar(Context context, AttributeSet attrs) {

        super(context, attrs);
		this.context = context;

		backgroundDrawable =  (NinePatchDrawable) this.context.getResources().getDrawable(R.drawable.progress_bar_empty);
		foregroundDrawable =  (NinePatchDrawable) this.context.getResources().getDrawable(R.drawable.progress_bar_full);
	}

	@Override
	protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {

        super.onSizeChanged(xNew, yNew, xOld, yOld);
		width = getWidth();
		height = getHeight();
	}

	
	@Override
	protected void onDraw(Canvas canvas) {

        if (backgroundDrawable != null) {

            backgroundDrawable.setBounds(0, 0, width, height);
			backgroundDrawable.draw(canvas);
		}
		
		if (foregroundDrawable != null) {

            drawStatic(canvas);
		}
	}
	
	private void drawStatic(Canvas canvas) {

        double onePercent = (float) width / 100;
		
		int startPos = 0;
		double newWidth = onePercent * percentage;
		
		foregroundDrawable.setBounds(startPos, 0, (int) newWidth, height);
		foregroundDrawable.draw(canvas);
	
		drawPercentageLabel(canvas);
	}

	private void drawPercentageLabel(Canvas canvas) {

        paint.setColor(Color.BLACK);
		paint.setStrokeWidth(0);
		paint.setTextSize(20);
    	
		drawText(canvas);
    }
	
	private void drawText(Canvas canvas) {

        String labelText = percentage + "%";
		
		Rect bounds = new Rect();
		paint.getTextBounds(labelText, 0, labelText.length(), bounds);
		float textWidth = paint.measureText(labelText);

		int x = (getWidth() / 2) - ((int)textWidth / 2);
		int y = (getHeight() + bounds.height() )/ 2;
		
		canvas.drawText(labelText, x, y, paint);
	}
	
	public void setPercentage(int percentage) {

        if (percentage > 100)
			percentage = 100;
		
		this.percentage = percentage;
	}

}
