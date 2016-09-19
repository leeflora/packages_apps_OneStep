package com.smartisanos.sidebar.view;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.smartisanos.sidebar.R;
import com.smartisanos.sidebar.SidebarController;
import com.smartisanos.sidebar.util.ContactItem;
import com.smartisanos.sidebar.util.LOG;
import com.smartisanos.sidebar.util.ResolveInfoGroup;
import com.smartisanos.sidebar.util.SidebarItem;
import com.smartisanos.sidebar.util.Utils;
import com.smartisanos.sidebar.util.anim.Anim;
import com.smartisanos.sidebar.util.anim.AnimStatusManager;
import com.smartisanos.sidebar.util.anim.AnimTimeLine;
import com.smartisanos.sidebar.util.anim.Vector3f;

public class SidebarListView extends ListView {
    private static final LOG log = LOG.getInstance(SidebarListView.class);

    private boolean mNeedFootView = false;
    private View mFootView;
    private SideView mSideView;

    private SidebarItem mDraggedItem;
    private int mDragPosition = -1;

    public SidebarListView(Context context) {
        this(context, null);
    }

    public SidebarListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    public SidebarListView(Context context, AttributeSet attrs,
            int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public SidebarListView(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mFootView = LayoutInflater.from(context).inflate(R.layout.sidebar_view_divider, null);
        setOnItemLongClickListener(mOnLongClickListener);
    }

    public void setSideView(SideView view) {
        mSideView = view;
    }

    private DragEventAdapter mDragEventAdapter;

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);
        if (adapter != null && adapter instanceof DragEventAdapter) {
            mDragEventAdapter = (DragEventAdapter) adapter;
        }
    }

    public void onDragStart(DragEvent event) {
        if (mDragEventAdapter != null) {
            mDragEventAdapter.onDragStart(event);
        }
    }

    public void onDragEnd() {
        if (mDragEventAdapter != null) {
            mDragEventAdapter.onDragEnd();
        }
    }

    private AdapterView.OnItemLongClickListener mOnLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (position < getHeaderViewsCount()
                    || position >= getChildCount() - getFooterViewsCount()) {
                return false;
            }
            int[] viewLoc = new int[2];
            view.getLocationOnScreen(viewLoc);
            viewLoc[0] = viewLoc[0] + view.getWidth() / 2;
            viewLoc[1] = viewLoc[1] + view.getHeight() / 2;
            mDraggedItem = (SidebarItem) SidebarListView.this.getAdapter().getItem(position);
            if (mDraggedItem == null) {
                // see ticket 136616 http://mantis.smartisan.cn/view.php?id=136616
                log.error("mDraggedItem == null !  position -> " + position
                        + ", count -> " + SidebarListView.this.getAdapter().getCount());
            }
            mDragPosition = position;
            Drawable icon = new BitmapDrawable(getResources(), mDraggedItem.getAvatar());
            SidebarController.getInstance(mContext).getSidebarRootView().startDrag(icon, view, viewLoc);
            mSideView.setDraggedList(SidebarListView.this);
            view.setVisibility(View.INVISIBLE);
            AnimStatusManager.getInstance().setStatus(AnimStatusManager.SIDEBAR_ITEM_DRAGGING, true);
            return false;
        }
    };

    public SidebarItem getDraggedItem() {
        return mDraggedItem;
    }

    public void deleteDraggedSidebarItem() {
        if (mDraggedItem != null) {
            mDraggedItem.delete();
            dragEnd();
        }
    }

    public void dropBackSidebarItem() {
        if (mDraggedItem != null) {
            if (mDragEventAdapter != null) {
                mDragEventAdapter.moveItemPostion(mDraggedItem, mDragPosition - this.getHeaderViewsCount());
            }
            dragEnd();
        }
    }

    private void dragEnd() {
        mDragPosition = -1;
        mDraggedItem = null;
        AnimStatusManager.getInstance().setStatus(AnimStatusManager.SIDEBAR_ITEM_DRAGGING, false);
    }

    private int[] convertToLocalCoordinate(int x, int y, Rect drawingRect) {
        int[] viewLoc = new int[2];
        getLocationOnScreen(viewLoc);
        int[] loc = new int[2];
        loc[0] = x - viewLoc[0];
        loc[1] = y - viewLoc[1];
        loc[0] = loc[0] + drawingRect.left;
        loc[1] = loc[1] + drawingRect.top;
        return loc;
    }

    public void dragObjectMove(int rawX, int rawY) {
        if (Utils.inArea(rawX, rawY, this)) {
            int count = getAdapter().getCount() - getHeaderViewsCount() - getFooterViewsCount();
            if (count > 0) {
                // convert global coordinate to view local coordinate
                Rect drawingRect = new Rect();
                getDrawingRect(drawingRect);
                int[] localLoc = convertToLocalCoordinate(rawX, rawY, drawingRect);
                int subViewHeight = drawingRect.bottom / getChildCount();
                int position = localLoc[1] / subViewHeight;
                pointToNewPositionWithAnim(position);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2, MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }

    public void setNeedFootView(boolean needFootView) {
        if (needFootView != mNeedFootView) {
            mNeedFootView = needFootView;
            requestLayout();
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
        if(mFootView == null){
            // this means the construcor is going on !
            return;
        }

        boolean isEmpty = false;
        if (mSideView != null) {
            isEmpty = mSideView.someListIsEmpty();
        }
        if (!isEmpty && mNeedFootView && (getAdapter() != null && !getAdapter().isEmpty())) {
            if (getFooterViewsCount() == 0) {
                addFooterView(mFootView);
            }
        } else {
            if (getFooterViewsCount() > 0) {
                removeFooterView(mFootView);
            }
        }

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        getViewTreeObserver().addOnGlobalLayoutListener(mAddItemWithAnimListener);
    }

    private ViewTreeObserver.OnGlobalLayoutListener mAddItemWithAnimListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
            //show anim
            int count = getChildCount();
            if (count == 0) {
                return;
            }
            View view = getChildAt(0);
            if (view.getTag() == null) {
                return;
            }
            boolean isNewAdded = false;
            if (view.getTag() instanceof ResolveInfoListAdapter.ViewHolder) {
                ResolveInfoGroup data = ((ResolveInfoListAdapter.ViewHolder) view.getTag()).resolveInfoGroup;
                if (data != null) {
                    if (data.isNewAdd) {
                        data.isNewAdd = false;
                        isNewAdded = true;
                    }
                }
            } else if (view.getTag() instanceof ContactListAdapter.ViewHolder) {
                ContactItem data = ((ContactListAdapter.ViewHolder) view.getTag()).mItem;
                if (data != null) {
                    if (data.isNewAdd) {
                        data.isNewAdd = false;
                        isNewAdded = true;
                    }
                }
            }
            if (!isNewAdded) {
                return;
            }
            int time = 200;
            AnimTimeLine timeLine = new AnimTimeLine();
            Anim alphaAnim = new Anim(view, Anim.TRANSPARENT, time, Anim.CUBIC_OUT, new Vector3f(), new Vector3f(0, 0, 1));
            Anim scaleBigAnim = new Anim(view, Anim.SCALE, time, Anim.CUBIC_OUT, new Vector3f(0.3f, 0.3f), new Vector3f(1.2f, 1.2f));
            Anim scaleNormal = new Anim(view, Anim.SCALE, time, Anim.CUBIC_OUT, new Vector3f(1.2f, 1.2f), new Vector3f(1, 1));
            scaleNormal.setDelay(time);

            timeLine.addAnim(alphaAnim);
            timeLine.addAnim(scaleBigAnim);
            timeLine.addAnim(scaleNormal);
            timeLine.start();
        }
    };

    private void pointToNewPositionWithAnim(int position) {
        int headViewCount = getHeaderViewsCount();
        int count = getCount() - getFooterViewsCount() - headViewCount;
        if (position < this.getHeaderViewsCount()
                || position >= this.getChildCount() - this.getFooterViewsCount()
                || mDragPosition == position) {
            return ;
        }
        int begin = headViewCount;
        int end = begin + count;
        // check invisible count
        int invisibleViewCount = 0;
        for (int i = begin; i < end; i++) {
            View view = getChildAt(i);
            if (view.getVisibility() != View.VISIBLE) {
                invisibleViewCount++;
            }
        }
        if (invisibleViewCount != 1) {
            for (int i = begin; i < end; i++) {
                View view = getChildAt(i);
                if (view.getVisibility() == View.INVISIBLE) {
                    log.error("dump INVISIBLE VIEW " + view);
                }
            }
            if (invisibleViewCount != 1) {
                for (int i = begin; i < end; i++) {
                    View view = getChildAt(i);
                    if (view.getVisibility() == View.INVISIBLE) {
                        log.error("dump INVISIBLE VIEW " + view);
                    }
                }
            }
            throw new IllegalArgumentException("invisibleViewCount != 1");
        }

        mDragPosition = position;
        position -= this.getHeaderViewsCount();
        View[] viewArr = new View[count];
        int index = 0;
        for (int i = begin; i < end; i++) {
            View view = getChildAt(i);
            if (view.getVisibility() == View.INVISIBLE) {
                viewArr[position] = view;
            } else {
                if (index == position) {
                    index++;
                }
                viewArr[index++] = view;
            }
        }
        AnimTimeLine moveAnimTimeLine = new AnimTimeLine();
        int toY = 0;
        for (int i = 0; i < headViewCount; ++i) {
            toY += getChildAt(i).getHeight();
        }
        for (int i = 0; i < viewArr.length; i++) {
            View view = viewArr[i];
            int fromY = (int) view.getY();
            if (fromY != toY) {
                Vector3f from = new Vector3f(0, fromY);
                Vector3f to = new Vector3f(0, toY);
                Anim anim = new Anim(view, Anim.TRANSLATE, 200, Anim.CUBIC_OUT, from, to);
                moveAnimTimeLine.addAnim(anim);
            }
            toY += view.getHeight();
        }
        moveAnimTimeLine.start();
    }
}
