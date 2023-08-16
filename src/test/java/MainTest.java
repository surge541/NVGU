import org.lwjgl.nanovg.NVGPaint;
import org.nvgu.NVGU;
import org.nvgu.util.NVGUColour;

import java.awt.*;

public class MainTest {

    public static void main(String[] args) {
        NVGU nvgu = new NVGU();

        new Window().run(() -> nvgu.frame(1280, 720, () -> {
            nvgu.rectangle(5, 5, 30, 30, Color.BLUE)
                .rectangleBorder(40, 5, 30, 30, 1, Color.RED)
                .circle(90, 20, 15, Color.WHITE)
                .circleBorder(125, 20, 15, 1, Color.GREEN)
                .scope(() -> nvgu
                        .scale(165, 20, 0.75f)
                        .rotateDegrees(165, 20, 45)
                        .rectangle(150, 5, 30, 30, Color.CYAN)
                )
                .rectangle(185, 5, 30, 30, new NVGUColour(nvgu.createAndStoreLinearGradient(185, 5, 185 + 30, 5 + 30, Color.RED, Color.GREEN)))
                .rectangle(220, 5, 30, 30, nvgu.linearGradient(220, 5, 30, 30, Color.RED, Color.GREEN, NVGU.GradientDirection.LEFT_TO_RIGHT))
                .circle(270, 20, 15, nvgu.linearGradient(255, 5, 30, 30, Color.CYAN, Color.MAGENTA, NVGU.GradientDirection.DIAGONAL_LEFT_TO_RIGHT_DOWN))
                .roundedRectangle(290, 5, 30, 30, 10, nvgu.linearGradient(290, 5, 30, 30, Color.RED, Color.BLUE, NVGU.GradientDirection.TOP_TO_BOTTOM));
        }));

        nvgu.destroy();
    }

}