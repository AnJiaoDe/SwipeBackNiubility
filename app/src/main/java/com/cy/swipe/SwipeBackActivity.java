package com.cy.swipe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class SwipeBackActivity extends BaseSwipeBackActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_animate);
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(SwipeBackActivity.this, SwipeBackActivity.class));

                Intent intent = new Intent(SwipeBackActivity.this, SwipeBack2Activity.class);
                startSwipeActivity(intent);
            }
        });
    }
}