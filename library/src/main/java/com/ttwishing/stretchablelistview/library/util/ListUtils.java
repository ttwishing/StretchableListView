package com.ttwishing.stretchablelistview.library.util;

/**
 * Created by kurt on 8/6/15.
 */
public class ListUtils {

    public enum ScrollDirection {
        UP, DOWN;

        public static ScrollDirection fromVelocity(ScrollDirection direction, float velocityX, float velocityY, boolean isVertical) {
            if (velocityY == 0.0F) {
                return direction;
            }
            if (velocityY > 0.0F)
                return UP;

            return DOWN;
        }
    }
}
