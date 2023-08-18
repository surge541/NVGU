import me.surge.animation.Animation;
import me.surge.animation.Easing;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.nvgu.NVGU;
import org.nvgu.util.Alignment;
import org.nvgu.util.Border;
import org.nvgu.util.LinearGradientDirection;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;

public class GUI {

    public static NVGU nvgu;

    private static final Animation animationA = new Animation(1000, false, Easing.LINEAR);
    private static final Animation animationB = new Animation(1000, false, Easing.LINEAR);

    public static void main(String[] args) {
        nvgu = new NVGU();

        Window window = new Window("NVGU - GUI", 1280, 720, true);

        Runnable init = () -> {
            nvgu.create()
                    .createFont("inter", GUI.class.getResourceAsStream("inter.ttf"))
                    .createTexture("mountains", GUI.class.getResourceAsStream("mountains.jpg"));
        };

        Button button = new Button("Example Button", new Rectangle(100, 100, 150, 40));

        Runnable render = () -> nvgu.frame(window.getWidth(), window.getHeight(), () -> {
            if (animationA.getAnimationFactor() == 0.0) {
                animationA.setState(true);
            } else if (animationA.getAnimationFactor() == 1.0) {
                animationA.setState(false);
            }

            if (animationB.getAnimationFactor() == 0.0) {
                animationB.setState(true);
            } else if (animationB.getAnimationFactor() == 1.0) {
                animationB.setState(false);
            }

            nvgu.roundedRectangle(0, 0, window.getWidth(), window.getHeight(), 20, nvgu.texture("mountains", 0, 0, window.getWidth(), window.getHeight()))
                    .roundedRectangle(0, 0, window.getWidth(), window.getHeight(), 20, new Color(0, 0, 0, 150))
                    .roundedRectangle(0, 0, window.getWidth(), window.getHeight(), 20, nvgu.radialGradient(0, 0, window.getWidth(), window.getHeight(), 1000, 0, 500, new Color(0, 0, 0, 0), Color.BLACK, Alignment.CENTER_MIDDLE))
                    .roundedRectangleBorder(0, 0, window.getWidth(), window.getHeight(), 20, 3, nvgu.linearGradient(0, 0, window.getWidth(), window.getHeight(), lerpColour(Color.CYAN, Color.MAGENTA, animationA.getAnimationFactor()), lerpColour(Color.MAGENTA, Color.CYAN, animationB.getAnimationFactor()), LinearGradientDirection.DIAGONAL_LEFT_TO_RIGHT_UP), Border.INSIDE)
                    .text("Example GUI Application", window.getWidth() / 2f, 100, Color.WHITE, "inter", 40, Alignment.CENTER_MIDDLE);

            button.render(nvgu, window.getMouseX(), window.getMouseY());
        });

        window.run(init, render);
    }

    private static Color lerpColour(Color from, Color to, double factor) {
        return new Color(
                (int) (from.getRed() + (to.getRed() - from.getRed()) * factor),
                (int) (from.getGreen() + (to.getGreen() - from.getGreen()) * factor),
                (int) (from.getBlue() + (to.getBlue() - from.getBlue()) * factor),
                (int) (from.getAlpha() + (to.getAlpha() - from.getAlpha()) * factor)
        );
    }

    private static class Button {

        private final String text;
        private final Rectangle bounds;

        private final Animation hover = new Animation(100, false, Easing.LINEAR);

        public Button(String text, Rectangle bounds) {
            this.text = text;
            this.bounds = bounds;
        }

        public void render(NVGU nvgu, float mouseX, float mouseY) {
            hover.setState(bounds.contains(mouseX, mouseY));

            nvgu.roundedRectangle((float) (bounds.x + (2 * hover.getAnimationFactor())), (float) (bounds.y + (2 * hover.getAnimationFactor())), (float) (bounds.width - ((2 * hover.getAnimationFactor()) * 2)), (float) (bounds.height - ((2 * hover.getAnimationFactor()) * 2)), 10, lerpColour(new Color(023047), new Color(0x219ebc), hover.getLinearFactor()))
                    .text(this.text, bounds.x + bounds.width / 2f, bounds.y + bounds.height / 2f, Color.WHITE, "inter", 15, Alignment.CENTER_MIDDLE);
        }

    }

}
