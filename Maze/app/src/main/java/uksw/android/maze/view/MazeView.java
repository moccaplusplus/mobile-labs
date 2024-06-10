package uksw.android.maze.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import uksw.android.maze.model.Graph;
import uksw.android.maze.model.Maze;

public class MazeView extends View {
    protected static final float MAZE_STROKE = 64;
    protected static final float CELL_SIZE = 80;
    protected static final float WALL_STROKE = CELL_SIZE - MAZE_STROKE;

    protected Paint backgroundPaint;
    protected Paint mazePaint;
    protected Paint hidePaint;
    protected float boxSize;
    protected Maze maze;

    public MazeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView();
    }

    public MazeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public MazeView(Context context) {
        super(context);
        initView();
    }

    public void setMaze(Maze maze) {
        this.maze = maze;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int size;
        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            size = Math.min(widthSize, heightSize);
        } else if (widthMode == MeasureSpec.EXACTLY) {
            size = heightMode == MeasureSpec.UNSPECIFIED ? widthSize : Math.min(widthSize, heightSize);
        } else if (heightMode == MeasureSpec.EXACTLY) {
            size = widthMode == MeasureSpec.UNSPECIFIED ? heightSize : Math.min(widthSize, heightSize);
        } else {
            size = Math.min(getSuggestedMinimumWidth(), getSuggestedMinimumHeight());
        }
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (maze != null) {
            drawMaze(canvas);
        }
    }

    protected void initView() {
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.GRAY);
        backgroundPaint.setStyle(Paint.Style.FILL);

        mazePaint = new Paint();
        mazePaint.setColor(Color.WHITE);
        mazePaint.setStyle(Paint.Style.STROKE);
        mazePaint.setStrokeWidth(MAZE_STROKE);
        mazePaint.setStrokeCap(Paint.Cap.SQUARE);
        mazePaint.setStrokeJoin(Paint.Join.ROUND);

        hidePaint = new Paint();
        hidePaint.setColor(Color.WHITE);
        hidePaint.setStyle(Paint.Style.FILL);
    }

    protected void drawMaze(Canvas canvas) {
        boxSize = maze.size * CELL_SIZE;
        float outerBoxSize = boxSize + WALL_STROKE;

        float scale = (getWidth() - getPaddingLeft() - getPaddingRight()) / outerBoxSize;
        canvas.translate(getPaddingLeft(), getPaddingTop());
        canvas.scale(scale, scale);

        canvas.drawRect(0, 0, outerBoxSize, outerBoxSize, backgroundPaint);
        canvas.drawRect(WALL_STROKE, 0, CELL_SIZE, 2 * WALL_STROKE, hidePaint);
        canvas.drawRect(outerBoxSize - CELL_SIZE, outerBoxSize - 2 * WALL_STROKE,
                outerBoxSize - WALL_STROKE, outerBoxSize + WALL_STROKE, hidePaint);

        float translation = (WALL_STROKE + CELL_SIZE) / 2;
        canvas.translate(translation, translation);
        drawGraph(canvas, mazePaint, maze);
    }

    protected void drawGraph(Canvas canvas, Paint paint, Graph graph) {
        graph.edges().forEach(path -> canvas.drawLine(
                x(path.from), y(path.from), x(path.to), y(path.to),
                paint));
    }

    protected float x(int node) {
        return (boxSize * maze.x(node)) / maze.size;
    }

    protected float y(int node) {
        return (boxSize * maze.y(node)) / maze.size;
    }
}
