// Create a new file called HorizontalSpaceItemDecoration.java
package com.example.cineswipe;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;

public class HorizontalSpaceItemDecoration extends RecyclerView.ItemDecoration {
    private final int space;

    public HorizontalSpaceItemDecoration(int space) {
        this.space = space;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.right = space;
        // Add left spacing only for the first item
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = space;
        }
    }
}