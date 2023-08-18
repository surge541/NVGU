import org.nvgu.NVGU;
import org.nvgu.util.Alignment;
import org.nvgu.util.LinearGradientDirection;
import org.nvgu.util.NVGUColour;

import java.awt.*;

public class MainTest {

    public static void main(String[] args) {
        NVGU nvgu = new NVGU();

        new Window().run(() -> nvgu.create().createFont("arial", MainTest.class.getResourceAsStream("arial.ttf")), () -> nvgu.frame(600, 300, () -> {
            // rectangles etc
            nvgu.rectangle(5, 5, 30, 30, Color.BLUE)
                    .rectangleBorder(40, 5, 30, 30, 1, Color.RED)
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
                    .roundedRectangle(325, 5, 30, 30, 10, nvgu.radialGradient(325, 5, 30, 30, 1, 35, 10, Color.RED, Color.BLUE, Alignment.LEFT_TOP));

            nvgu.text("Hello, world!", 5, 55, Color.WHITE, "arial", 20, Alignment.LEFT_TOP)
                    .setFontData("arial", 10, Alignment.LEFT_TOP)
                    .text("Smaller text!", 10 + nvgu.textWidth("Hello, world!", "arial", 20), 55, Color.BLUE)
                    .create();
        }));

        nvgu.destroy();
    }

}