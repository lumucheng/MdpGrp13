package com.example.lu.mdpgrp13;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

/**
 * Created by Mucheng on 28/9/15.
 */
public class PixelGridView extends View
{
    private static final int MAP_HEIGHT = 600;
    private static final int MAP_WIDTH = 450;
    private static final int CELL_WIDTH = 30;
    private static final int CELL_HEIGHT = 30;
    private static final int NUM_COLS = 15;
    private static final int NUM_ROWS = 20;

    private int robotCenterX = -1;
    private int robotCenterY = -1;
    private int robotAngle = 0;

    private int numColumns, numRows;
    private Paint blackPaint = new Paint();
    private boolean[][] cellChecked;

    public PixelGridView(Context context)
    {
        this(context, null);

        cellChecked = new boolean[NUM_ROWS][NUM_COLS];

        for (int i = 0; i < NUM_ROWS; i++) {
            for (int j = 0; j < NUM_COLS; j++) {
                cellChecked[i][j] = false;
            }
        }
    }

    public PixelGridView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    public void setNumColumns(int numColumns)
    {
        this.numColumns = numColumns;
        calculateDimensions();
    }

    public int getNumColumns()
    {
        return numColumns;
    }

    public void setNumRows(int numRows)
    {
        this.numRows = numRows;
        calculateDimensions();
    }

    public int getNumRows()
    {
        return numRows;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh)
    {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateDimensions();
    }

    private void calculateDimensions()
    {
        if (numColumns == 0 || numRows == 0)
            return;

        cellChecked = new boolean[numRows][numColumns];

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        Paint paint = new Paint();

        super.onDraw(canvas);
        canvas.drawColor(Color.WHITE);

        // Draw Start and Goal Zone
        Bitmap startImg = BitmapFactory.decodeResource(getResources(), R.drawable.start);
        canvas.drawBitmap(startImg, 0, 0, paint);
        Bitmap goalImg = BitmapFactory.decodeResource(getResources(), R.drawable.goal);
        canvas.drawBitmap(goalImg, 12 * CELL_WIDTH, 17 * CELL_HEIGHT, paint);

        // Draw obstacles here
        for (int i = 0; i < NUM_ROWS; i++)
        {
            for (int j = 0; j < NUM_COLS; j++)
            {
                if (cellChecked[i][j])
                {
                    canvas.drawRect(j * CELL_WIDTH, i * CELL_HEIGHT,
                            (j + 1) * CELL_WIDTH, (i + 1) * CELL_HEIGHT, blackPaint);
                }
            }
        }

        // Draw grid lines
        for (int i = 1; i < NUM_COLS; i++)
        {
            canvas.drawLine(i * CELL_WIDTH, 0, i * CELL_WIDTH, MAP_HEIGHT, blackPaint);
        }
        for (int i = 1; i < NUM_ROWS; i++)
        {
            canvas.drawLine(0, i * CELL_HEIGHT, MAP_WIDTH, i * CELL_HEIGHT, blackPaint);
        }

        // Draw robot
        if (robotCenterX != -1 && robotCenterY != -1) {

            Matrix matrix = new Matrix();
            matrix.postRotate(robotAngle);
            Bitmap robotImg = BitmapFactory.decodeResource(getResources(), R.drawable.robot);
            Bitmap rotatedBitmap = Bitmap.createBitmap(robotImg , 0, 0,
                    robotImg.getWidth(), robotImg.getHeight(), matrix, true);
            canvas.drawBitmap(rotatedBitmap, robotCenterX * CELL_WIDTH, robotCenterY * CELL_HEIGHT, paint);
        }

        postInvalidateDelayed(300);
    }

    public void setRobotStartPos(int startX, int startY, double angle) {
        robotCenterX = startX - 1;
        robotCenterY = startY - 1;
        invalidate();
    }

    public void rotateRobot(String rotateCommand) {
        if (rotateCommand == "l") {
            if (robotAngle == 270) {
                robotAngle = 0;
            }
            else {
                robotAngle += 90;
            }
        }
        else {
            if (robotAngle == 0) {
                robotAngle = 270;
            }
            else {
                robotAngle -= 90;
            }
        }

        invalidate();
    }

    public void moveRobot(String cmd) {
        if (cmd == "f") {
            switch (robotAngle) {
                case 0:
                    robotCenterY += 1;
                    break;
                case 90:
                    robotCenterX += 1;
                    break;
                case 180:
                    robotCenterY -= 1;
                    break;
                case 270:
                    robotCenterX -= 1;
                    break;
            }
        }
        else {
            switch (robotAngle) {
                case 0:
                    robotCenterY += 1;
                    break;
                case 90:
                    robotCenterX -= 1;
                    break;
                case 180:
                    robotCenterY -= 1;
                    break;
                case 270:
                    robotCenterX += 1;
                    break;
            }
        }
        invalidate();
    }

    // used to update robot position from algorithm commands
    public void drawRobot(int centerX, int centerY, double angle) {

        robotCenterX = centerX;
        robotCenterY = centerY;
        angle = robotAngle;

        invalidate();
    }

    public void drawObstacles(String grid) {

        int index = 0;

        for (int i = 0; i < NUM_ROWS; i++)
        {
            for (int j = 0; j < NUM_COLS; j++)
            {
                if (index < grid.length()) {
                    char cell = grid.charAt(index);

                    if (cell == '0') {
                        cellChecked[i][j] = false;
                    }
                    else {
                        cellChecked[i][j] = true;
                    }
                    index++;
                }
            }
        }
        invalidate();
    }

    public void addObstacle(String obstacle) {

        //"o:0101"

        String xCoordStr = obstacle.substring(0,1);
        String yCoordStr = obstacle.substring(2,3);

        int xCoord = Integer.parseInt(xCoordStr);
        int yCoord = Integer.parseInt(yCoordStr);

        cellChecked[xCoord][yCoord] = true;

        invalidate();
    }
}
