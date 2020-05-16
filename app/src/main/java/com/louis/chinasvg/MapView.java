package com.louis.chinasvg;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.graphics.PathParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class MapView extends View {

    private int[] colorArray = new int[]{0xFF239BD7, 0xFF30A9e5, 0xFF80CBF1, 0xFFFFFF};
    private Context mContext;
    private List<ProviceItem> mItemList;
    private Paint mPaint;
    private ProviceItem mSelectedProvice;
    private RectF totalRect;
    private float scale = 1.0f;

    public MapView(Context context) {
        super(context);
    }

    public MapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mItemList = new ArrayList<>();
        totalRect = new RectF();
        loadThread.run();
    }

    private Thread loadThread = new Thread() {
        @Override
        public void run() {
            InputStream inputStream = mContext.getResources().openRawResource(R.raw.china);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = null;

            try {
                builder = factory.newDocumentBuilder();
                Document doc = builder.parse(inputStream);
                Element rootElement = doc.getDocumentElement();
                NodeList items = rootElement.getElementsByTagName("path");
                float left = -1;
                float right = -1;
                float top = -1;
                float bottom = -1;
                List<ProviceItem> list = new ArrayList<>();
                for (int i = 0; i < items.getLength(); i++) {
                    Element element = (Element) items.item(i);
                    String pathdata = element.getAttribute("android:pathData");
                    Path path = PathParser.createPathFromPathData(pathdata);
                    ProviceItem proviceItem = new ProviceItem(path);
                    proviceItem.setDrawColor(colorArray[i % 4]);
                    RectF rectF = new RectF();
                    path.computeBounds(rectF, true);
                    left = left == -1 ? rectF.left : Math.min(left, rectF.left);
                    right = right == -1 ? rectF.right : Math.max(right, rectF.right);
                    top = top == -1 ? rectF.top : Math.min(top, rectF.top);
                    bottom = bottom == -1 ? rectF.bottom : Math.min(bottom, rectF.bottom);
                    list.add(proviceItem);
                }
                mItemList = list;
                totalRect.set(left,top,right,bottom);
                postInvalidate();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (totalRect != null) {
            double mapWidth = totalRect.width();
            scale = (float) (width / mapWidth);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mItemList != null) {
            canvas.save();
            canvas.scale(scale,scale);
            for (ProviceItem proviceItem : mItemList) {
                if (proviceItem != mSelectedProvice) {
                    proviceItem.drawItem(canvas, mPaint, false);
                } else {
                    proviceItem.drawItem(canvas, mPaint, true);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        handleTouch(event.getX()/scale, event.getY()/scale);
        return super.onTouchEvent(event);

    }

    private void handleTouch(float x, float y) {
        if (mItemList == null) {
            return;
        }

        ProviceItem selectItem = null;
        for (ProviceItem proviceItem : mItemList) {
            if (proviceItem.isTouch(x, y)) {
                selectItem = proviceItem;
            }
        }
        if (selectItem != null) {
            mSelectedProvice = selectItem;
            postInvalidate();
        }
    }
}
