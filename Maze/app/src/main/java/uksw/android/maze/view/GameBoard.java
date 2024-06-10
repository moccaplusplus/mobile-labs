package uksw.android.maze.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;

import androidx.annotation.Nullable;

import uksw.android.maze.model.Graph;
import uksw.android.maze.model.Maze;

public class GameBoard extends MazeView {
    private static final float CIRCLE_RADIUS = 20;
    private static final float PATH_STROKE = MAZE_STROKE / 4;

    private Paint playerPaint;
    private Paint pathPaint;
    private int currentNode;
    private Graph playerGraph;

    public GameBoard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public GameBoard(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public GameBoard(Context context) {
        super(context);
    }

    @Override
    protected void initView() {
        super.initView();
        playerPaint = new Paint();
        playerPaint.setColor(Color.RED);
        playerPaint.setStyle(Paint.Style.FILL);

        pathPaint = new Paint();
        pathPaint.setColor(Color.LTGRAY);
        pathPaint.setStyle(Paint.Style.STROKE);
        pathPaint.setStrokeWidth(PATH_STROKE);
        pathPaint.setStrokeCap(Paint.Cap.ROUND);
        pathPaint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override
    protected void drawMaze(Canvas canvas) {
        super.drawMaze(canvas);
        drawGraph(canvas, pathPaint, playerGraph);
        canvas.drawCircle(x(currentNode), y(currentNode), CIRCLE_RADIUS, playerPaint);
    }

    @Override
    public void setMaze(Maze maze) {
        currentNode = 0;
        playerGraph = maze == null ? null : new Graph(maze.nodeCount);
        super.setMaze(maze);
    }

    public boolean move(int y, int x) {
        int candidate = maze.nodeAt(maze.y(currentNode) + y, maze.x(currentNode) + x);
        if (candidate != -1 && maze.links(currentNode).anyMatch(i -> i == candidate)) {
            playerGraph.addEdge(currentNode, candidate);
            currentNode = candidate;
            invalidate();
            return true;
        }
        return false;
    }

    public boolean hasReachedTarget() {
        return currentNode == maze.nodeCount - 1;
    }
}
