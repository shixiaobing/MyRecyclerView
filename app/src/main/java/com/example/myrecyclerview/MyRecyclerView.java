package com.example.myrecyclerview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * 自定义控件 RecyclerView
 */
public class MyRecyclerView extends ViewGroup {


    private static final String TAG = "MyRecyclerView";
    private boolean needReLayout;
    // 屏幕的View的集合
    private List<View> viewList;
    private Adapter adapter;

    private int rowCount;
    // 每一行的高度
    private int[] heights;

    private int width;
    private int height;

    // 回收池
    private Recycler recycler;
    private int touchSlop;
    // 当前滑动的Y值
    private int currentY;
    // 偏移距离
    // scrollY 定义：第一个可见元素的左上顶点 距离屏幕左上角的距离
    private int scrollY;
    // 滑到第几行
    // firstRow定义： 第一个可见元素 在数据中的第几个
    private int firstRow;

    public MyRecyclerView(Context context) {
        super(context);
    }

    /**
     * 从xml文件中加载需要调用这个构造函数
     *
     * @param context
     * @param attrs
     */
    public MyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MyRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init(Context context) {
        needReLayout = true;
        viewList = new ArrayList<>();
        ViewConfiguration configuration = ViewConfiguration.get(context);
        touchSlop = configuration.getScaledTouchSlop();
    }

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        if (this.adapter != null) {
            recycler = new Recycler(adapter.getViewTypeCount());
        }
        needReLayout = true;
        scrollY = 0;
        firstRow = 0;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (adapter != null) {
            rowCount = adapter.getItemCount();
            heights = new int[rowCount];
            for (int i = 0; i < heights.length; i++) {
                heights[i] = adapter.getHeight(i);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * // onLayout --> childView onLayout 重新摆放子控件
     *
     * @param changed 父容器发生变化，一定要重新测量
     * @param l
     * @param t
     * @param r
     * @param b
     */
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (needReLayout || changed) {
            // 测量
            needReLayout = false;

            // 摆放的时候，初始化
            viewList.clear();

            // 比较耗时
            removeAllViews();
            if (adapter != null) {
                width = r - l;
                height = b - t;
                int top = 0, bottom;

                top = -scrollY;

                for (int i = 0; i < rowCount && top < height; ++i) {
                    bottom = top + heights[i];
                    // 实例化布局
                    // 怎么摆放
                    // 摆放多少个
                    View view = makeAndSetup(i, 0, top, width, bottom);
                    viewList.add(view);
                    top = bottom;
                }
            }
        }
    }

    // 不需要重写draw(), 不会重绘

    private View makeAndSetup(int index, int left, int top, int right, int bottom) {
        View view = obtain(index, right - left, bottom - top);
        view.layout(left, top, right, bottom);
        return view;
    }

    private View obtain(int row, int width, int height) {
        int type = adapter.getItemViewType(row);
        // 根据类型从回收池取View
        View recyclerView = recycler.getRecyclerView(type);
        //  取不出来
        View view = adapter.getView(row, recyclerView, this);
        if (view == null) {
            throw new RuntimeException("convertView 不能为空");
        }
        // 设置tag
        view.setTag(R.id.tag_type_view, type);

        // 测量
        view.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        );
        // 每次加载首屏
        addView(view, 0);
        return view;
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        int type = (int) view.getTag(R.id.tag_type_view);
        recycler.addRecyclerView(view, type);
    }

    // 事件机制

    // 滑动拦截
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false; // 默认不拦截
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                currentY = (int) ev.getRawY();
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                int y2 = Math.abs(currentY - (int) ev.getRawY());
                if (y2 > touchSlop) {
                    // 如果有子组件，可以遍历判断，例如是ScrollView，就可以返回false，表示不拦截
                    intercept = true; // 拦截
                }
            }
        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int y2 = (int) event.getRawY();
            int diff = currentY - y2; // 永远是第一个点减去第二个点
            scrollBy(0, diff);
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void scrollBy(int x, int diff) {
        scrollY += diff;
        // 修正
        scrollY = scrollBounds(scrollY, firstRow, heights, height);
        Log.i("touch", "ScrollBy:" + scrollY);

        if (scrollY > 0) { // 正向滑动
            while (heights[firstRow] < scrollY) { // while 可以消除scrollBy的不确定性
                if (!viewList.isEmpty()) {
                    removeTop();
                }
                scrollY -= heights[firstRow];
                firstRow++;
            }
            while (getFilledHeight() < height) {
                addBottom();
            }
        } else {
            // 往下滑 下滑的距离超过一个item的高度
            while (!viewList.isEmpty() && getFilledHeight() - heights[firstRow + viewList.size() - 1] > height) {
                removeBottom();
            }
            while (0 > scrollY) {
                addTop();
                firstRow--;
                scrollY += heights[firstRow + 1];
            }
        }

        repositionViews();

        awakenScrollBars();
    }

    /**
     * 摆放Views
     */
    private void repositionViews() {
        int left, top, right, bottom, i;
        top = -scrollY;
        i = firstRow;
        for (View view : viewList) {
            bottom = top + heights[i++];
            view.layout(0, top, width, bottom);
            top = bottom;
        }
    }

    private void addTop() {
        addTopAndBottom(firstRow - 1, 0);
    }

    private void addBottom() {
        final int size = viewList.size();
        addTopAndBottom(firstRow + size, size);
    }

    private void removeTop() {
        removeView(viewList.remove(0));
    }

    private void removeBottom() {
        removeTopOrBottom(viewList.size() - 1);
    }

    private void addTopAndBottom(int addRow, int index) {
        View view = obtain(addRow, width, heights[addRow]);
        viewList.add(index, view);
    }

    private void removeTopOrBottom(int position) {
        removeView(viewList.remove(position));
    }

    private int scrollBounds(int scrollY, int firstRow, int sizes[], int viewSize) {
        if (scrollY == 0) {
            // no op
        } else if (scrollY < 0) {
            // 修整下滑的临界值
            scrollY = Math.max(scrollY, -sumArray(sizes, 0, firstRow));
        } else {
            scrollY = Math.min(
                    scrollY,
                    sumArray(sizes, firstRow, sizes.length - 1 - firstRow) - viewSize
            );
        }
        return scrollY;
    }

    private int getFilledHeight() {
        return sumArray(heights, firstRow, viewList.size()) - scrollY;
    }

    private int sumArray(int array[], int firstIndex, int count) {
        int sum = 0;
        count += firstIndex;
        for (int i = firstIndex; i < count; ++i) {
            sum += array[i];
        }
        return sum;
    }

    /**
     * Adapter接口
     */
    interface Adapter {
        View getView(int position, View convertView, ViewGroup parent);

        // Item的类型
        int getItemViewType(int row);

        // Item的类型数量
        int getViewTypeCount();

        int getItemCount();

        int getHeight(int index);
    }

    /**
     * 回收池
     */
    class Recycler {

        private Stack<View>[] views;

        public Recycler(int count) {

            views = new Stack[count];
            // 给每一个stack赋值一个对象
            for (int i = 0; i < count; ++i) {
                views[i] = new Stack<>();
            }
        }

        public View getRecyclerView(int type) {
            try {
                return views[type].pop();
            } catch (Exception e) {
                return null;
            }
        }

        public void addRecyclerView(View view, int type) {
            views[type].push(view);
        }

    }
}
