package uksw.android.magicsquare;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputFilter;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.TextView;

public class SquareView extends GridLayout {
    protected int ySize = 3;
    protected int xSize = 3;
    protected int fontSize;
    protected EditText[][] inputs;
    protected TextView[] xSums;
    protected TextView[] ySums;

    public SquareView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttrs(attrs, defStyleAttr);
        initView();
    }

    public SquareView(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(attrs, 0);
        initView();
    }

    public SquareView(Context context) {
        super(context);
        initView();
    }

    public int getInputValue(int y, int x) {
        String s = inputs[y][x].getText().toString();
        if (s.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setInputValue(int y, int x, int value) {
        if (value == 0) {
            inputs[y][x].getText().clear();
        } else {
            inputs[y][x].setText(String.valueOf(value));
        }
    }

    public void setXSum(int y, int sum) {
        xSums[y].setText(String.valueOf(sum));
    }

    public void setYSum(int x, int sum) {
        ySums[x].setText(String.valueOf(sum));
    }

    protected void parseAttrs(AttributeSet attrs, int defStyleAttr) {
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.SquareView, defStyleAttr, 0);
        fontSize = a.getDimensionPixelSize(R.styleable.SquareView_textSize, 0);
        ySize = a.getInt(R.styleable.SquareView_ySize, ySize);
        xSize = a.getInt(R.styleable.SquareView_xSize, xSize);
        a.recycle();
    }

    protected void initView() {
        inputs = new EditText[ySize][xSize];
        xSums = new TextView[ySize];
        ySums = new TextView[xSize];
        setColumnCount(2 * xSize + 1);
        setRowCount(2 * ySize + 1);

        InputFilter[] filters = {new InputFilter.LengthFilter(1)};
        for (int y = 0; y < ySize; y++) {
            for (int x = 0; x < xSize; x++) {
                EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setFilters(filters);
                addTextView(input, y * 2, x * 2);
                inputs[y][x] = input;

                TextView tv = new TextView(getContext());
                tv.setText(x == xSize - 1 ? "=" : "+");
                addTextView(tv, y * 2, x * 2 + 1);

                tv = new TextView(getContext());
                tv.setText(y == ySize - 1 ? "=" : "+");
                addTextView(tv, y * 2 + 1, x * 2);
            }
        }

        for (int y = 0; y < ySize; y++) {
            TextView xSum = new TextView(getContext());
            addTextView(xSum, y * 2, 2 * xSize);
            xSums[y] = xSum;
        }

        for (int x = 0; x < xSize; x++) {
            TextView ySum = new TextView(getContext());
            addTextView(ySum, 2 * ySize, x * 2);
            ySums[x] = ySum;
        }
    }

    protected void addTextView(TextView tv, int row, int col) {
        addTextView(tv, createGridLayoutParams(row, col));
    }

    protected void addTextView(TextView tv, LayoutParams lp) {
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER);
        if (fontSize != 0) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize);
        }
        addView(tv);
    }

    protected LayoutParams createGridLayoutParams(int row, int col) {
        LayoutParams lp = new GridLayout.LayoutParams();
        lp.height = LayoutParams.WRAP_CONTENT;
        lp.width = LayoutParams.WRAP_CONTENT;
        lp.columnSpec = GridLayout.spec(col, 1, FILL, 1);
        lp.rowSpec = GridLayout.spec(row);
        return lp;
    }
}
