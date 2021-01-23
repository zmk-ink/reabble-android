package com.zmk.ink.reabble;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class LoadingDialog extends Dialog{
    private TextView tv_text;

    public LoadingDialog(@NonNull Context context) {
        super(context);
        //设置对话框背景透明
        if (getWindow()!= null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            setContentView(R.layout.dialog_loadding);
            tv_text = findViewById(R.id.tv_text);
            setCanceledOnTouchOutside(false);
        }
    }


    /**
     * 为加载进度个对话框设置不同的提示消息
     * @param message 给用户展示的提示信息
     * @return build模式设计，可以链式调用
     */
    public LoadingDialog setMessage(String message) {
        tv_text.setText(message);
        return this;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        return true;
    }

    @Override
    public void onBackPressed() {

    }
}
