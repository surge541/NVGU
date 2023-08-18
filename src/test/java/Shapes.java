import org.nvgu.NVGU;
import org.nvgu.util.Alignment;
import org.nvgu.util.Border;
import org.nvgu.util.LinearGradientDirection;
import org.nvgu.util.RightAngledTriangleCorner;

import java.awt.*;

public class Shapes {

    public static void main(String[] args) {
        NVGU nvgu = new NVGU();

        new Window("NVGU - Shapes", 600, 300, false).run(() -> nvgu.create().createFont("arial", Shapes.class.getResourceAsStream("arial.ttf")), () -> nvgu.frame(600, 300, () -> {
            // rectangles etc
            nvgu.rectangle(0, 0, 600, 300, Color.BLACK)
                    .rectangle(5, 5, 30, 30, Color.BLUE)
                    .rectangleBorder(40, 5, 30, 30, 1, Color.RED, Border.INSIDE)
                    .circle(90, 20, 15, Color.WHITE)
                    .circleBorder(125, 20, 15, 1, Color.GREEN)
                    .scope(() -> nvgu
                        .scale(165, 20, 0.75f)
                        .rotateDegrees(165, 20, 45)
                        .rectangle(150, 5, 30, 30, Color.CYAN)
                    )
                    .rectangle(185, 5, 30, 30, nvgu.linearGradient(185, 5, 185 + 30, 5 + 30, Color.RED, Color.GREEN, LinearGradientDirection.LEFT_TO_RIGHT))
                    .rectangle(220, 5, 30, 30, nvgu.linearGradient(220, 5, 30, 30, 20, Color.RED, Color.GREEN, LinearGradientDirection.LEFT_TO_RIGHT))
                    .circle(270, 20, 15, nvgu.linearGradient(255, 5, 30, 30, 20, Color.CYAN, Color.MAGENTA, LinearGradientDirection.DIAGONAL_LEFT_TO_RIGHT_DOWN))
                    .roundedRectangle(290, 5, 30, 30, 10, nvgu.linearGradient(290, 5, 30, 30, 20, Color.RED, Color.BLUE, LinearGradientDirection.TOP_TO_BOTTOM))
                    .roundedRectangle(325, 5, 30, 30, 10, nvgu.radialGradient(325, 5, 30, 30, 1, 35, 10, Color.YELLOW, Color.MAGENTA, Alignment.LEFT_TOP))
                    .rightAngledTriangle(360, 5, 30, 30, Color.ORANGE, RightAngledTriangleCorner.BOTTOM_RIGHT)
                    .rightAngledTriangleBorder(395, 5, 30, 30, 1, nvgu.linearGradient(395, 5, 30, 30, Color.CYAN, Color.MAGENTA, LinearGradientDirection.DIAGONAL_LEFT_TO_RIGHT_UP), RightAngledTriangleCorner.TOP_RIGHT)
                    .polygon(new float[][]{
                            new float[] { 430, 5 },
                            new float[] { 430, 35 },
                            new float[] { 460, 35 },
                            new float[] { 470, 20 },
                            new float[] { 460, 5 }
                    }, Color.WHITE);

            nvgu.text("Hello, world!", 5, 55, Color.WHITE, "arial", 20, Alignment.LEFT_TOP)
                    .setFontData("arial", 10, Alignment.LEFT_TOP)
                    .text("Smaller text!", 10 + nvgu.textWidth("Hello, world!", "arial", 20), 55, Color.BLUE)
                    .create();
        }));

        nvgu.destroy();
    }

}