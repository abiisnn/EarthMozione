package com.escombros.earthmozione;

import android.content.Context;
import android.widget.Button;

public class TouchableButton extends android.support.v7.widget.AppCompatButton {
    public TouchableButton(Context context) {
        super(context);
    }

    @Override
    public boolean performClick() {
        // do what you want
        return true;
    }
}
