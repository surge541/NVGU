package org.nvgu;

import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.nvgu.util.NVGUColour;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.opengl.GL11.*;

public class NVGU {

    private long handle = -1;
    private final List<NativeResource> resources = new ArrayList<>();

    /**
     * Destroys the instance of NanoVG
     */
    public void destroy() {
        nvgDelete(handle);
        handle = -1;
    }

    /**
     * Begins a new frame
     * @param width the horizontal size of the frame in pixels
     * @param height the vertical size of the frame in pixels
     */
    public NVGU beginFrame(int width, int height) {
        checkInitialisedState();

        nvgBeginFrame(handle, width, height, 1);

        return this;
    }

    /**
     * Ends the current frame
     */
    public NVGU endFrame() {
        nvgEndFrame(handle);
        return this;
    }

    /**
     * Begins, renders and ends a frame, and frees resources at the end.
     * @param width the horizontal size of the frame in pixels
     * @param height the vertical size of the frame in pixels
     * @param render what will be rendered in the frame
     */
    public NVGU frame(int width, int height, Runnable render) {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE);

        beginFrame(width, height);

        render.run();

        endFrame();
        freeResources();

        return this;
    }

    /**
     * Provides a scope where any transformations that have taken place will be reverted
     * immediately after rendering, such as rotations or scaling.
     * @param render what will be rendered in the scope
     */
    public NVGU scope(Runnable render) {
        save();
        render.run();
        restore();

        return this;
    }

    /**
     * Saves current transformations
     */
    public NVGU save() {
        nvgSave(handle);
        return this;
    }

    /**
     * Restores previous transformations
     */
    public NVGU restore() {
        nvgRestore(handle);
        return this;
    }

    /**
     * Basic solid-coloured rectangle.
     * @param rectangle bounds of the rectangle
     * @param colour colour of the rectangle
     */
    public NVGU rectangle(Rectangle rectangle, Color colour) {
        return rectangle((float) rectangle.getX(), (float) rectangle.getY(), (float) rectangle.getWidth(), (float) rectangle.getHeight(), colour);
    }

    /**
     * Basic solid-coloured rectangle.
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param colour colour of the rectangle
     */
    public NVGU rectangle(float x, float y, float width, float height, Color colour) {
        nvgBeginPath(handle);

        nvgRect(handle, x, y, width, height);

        if (colour instanceof NVGUColour) {
            ((NVGUColour) colour).apply(this, NVGUColour.RenderType.FILL);
        } else {
            nvgFillColor(handle, createAndStoreColour(colour));
            nvgFill(handle);
        }

        nvgClosePath(handle);

        return this;
    }

    /**
     * Basic solid-coloured rectangle border.
     * @param rectangle bounds of the rectangle
     * @param colour colour of the rectangle
     */
    public NVGU rectangleBorder(Rectangle rectangle, float thickness, Color colour) {
        return rectangleBorder((float) rectangle.getX(), (float) rectangle.getY(), (float) rectangle.getWidth(), (float) rectangle.getHeight(), thickness, colour);
    }

    /**
     * Basic solid-coloured rectangle border.
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param thickness thickness of the border
     * @param colour colour of the rectangle
     */
    public NVGU rectangleBorder(float x, float y, float width, float height, float thickness, Color colour) {
        nvgBeginPath(handle);

        nvgRect(handle, x, y, width, height);
        nvgStrokeWidth(handle, thickness);

        if (colour instanceof NVGUColour) {
            ((NVGUColour) colour).apply(this, NVGUColour.RenderType.STROKE);
        } else {
            nvgStrokeColor(handle, createAndStoreColour(colour));
            nvgStroke(handle);
        }

        nvgClosePath(handle);

        return this;
    }

    /**
     * Basic solid-coloured circle.
     * @param x centre x coordinate of the circle
     * @param y centre y coordinate of the circle
     * @param radius radius of the circle
     * @param colour colour of the circle
     */
    public NVGU circle(float x, float y, float radius, Color colour) {
        nvgBeginPath(handle);

        nvgCircle(handle, x, y, radius);
        nvgFillColor(handle, createAndStoreColour(colour));
        nvgFill(handle);

        nvgClosePath(handle);

        return this;
    }

    /**
     * Basic solid-coloured circle border.
     * @param x centre x coordinate of the circle
     * @param y centre y coordinate of the circle
     * @param radius radius of the circle
     * @param thickness thickness of the circle border
     * @param colour colour of the circle
     */
    public NVGU circleBorder(float x, float y, float radius, float thickness, Color colour) {
        nvgBeginPath(handle);

        nvgCircle(handle, x, y, radius);
        nvgStrokeWidth(handle, thickness);
        nvgStrokeColor(handle, createAndStoreColour(colour));
        nvgStroke(handle);

        nvgClosePath(handle);

        return this;
    }

    /**
     * Translates subsequent rendering to the given coordinates
     * @param x horizontal coordinate
     * @param y vertical coordinate
     */
    public NVGU translate(float x, float y) {
        nvgTranslate(handle, x, y);

        return this;
    }

    /**
     * Rotates subsequent rendering by the given angle
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param angle angle of rotation (degrees)
     */
    public NVGU rotateDegrees(float x, float y, float angle) {
        translate(x, y);
        nvgRotate(handle, (float) Math.toRadians(angle));
        translate(-x, -y);

        return this;
    }

    /**
     * Rotates the rendering in the render block
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param angle angle of rotation (degrees)
     * @param render what will be rotated and rendered
     */
    public NVGU rotateDegrees(float x, float y, float angle, Runnable render) {
        scope(() -> {
            rotateDegrees(x, y, angle);
            render.run();
        });

        return this;
    }

    /**
     * Rotates the rendering in the render block
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param angle angle of rotation (radians)
     * @param render what will be rotated and rendered
     */
    public NVGU rotateRadians(float x, float y, float angle, Runnable render) {
        scope(() -> {
            rotateRadians(x, y, angle);
            render.run();
        });

        return this;
    }

    /**
     * Rotates subsequent rendering by the given angle
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param angle angle of rotation (radians)
     */
    public NVGU rotateRadians(float x, float y, float angle) {
        translate(x, y);
        nvgRotate(handle, angle);
        translate(-x, -y);

        return this;
    }

    /**
     * Scales subsequent rendering by the given factors
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param factorX horizontal scale factor
     * @param factorY vertical scale factor
     */
    public NVGU scale(float x, float y, float factorX, float factorY) {
        translate(x, y);
        nvgScale(handle, factorX, factorY);
        translate(-x, -y);

        return this;
    }

    /**
     * Scales subsequent rendering by the given factor
     * @param x horizontal coordinate
     * @param y vertical coordinate
     * @param factor scale factor
     */
    public NVGU scale(float x, float y, float factor) {
        return scale(x, y, factor, factor);
    }

    // utility methods

    /**
     * Creates an instance of {@link NVGColor} from the given {@link Color}
     * @param colour the colour to transform into {@link NVGColor}
     */
    public NVGColor createAndStoreColour(Color colour) {
        NVGColor nvgColour = NVGColor.malloc()
                .r(colour.getRed() / 255f)
                .g(colour.getGreen() / 255f)
                .b(colour.getBlue() / 255f)
                .a(colour.getAlpha() / 255f);

        resources.add(nvgColour);

        return nvgColour;
    }

    /**
     * Creates an instance of {@link NVGPaint}
     */
    public NVGPaint createAndStorePaint() {
        NVGPaint paint = NVGPaint.malloc();

        resources.add(paint);

        return paint;
    }

    /**
     * Creates a linear gradient in an instance of an {@link NVGUColour}. The position
     * parameters will most likely be the same as the coordinates of whatever shape you are
     * drawing, e.g. a rectangle. However, if, for example, you are drawing a string, you
     * need to put the width of the text etc as parameters.
     * @param x start x coordinate of the gradient
     * @param y start y coordinate of the gradient
     * @param width width of the gradient
     * @param height height of the gradient
     * @param start start colour of the gradient
     * @param end end colour of the gradient
     * @param direction direction of the gradient
     * @return instance of the gradient inside an {@link NVGUColour}
     */
    public NVGUColour linearGradient(float x, float y, float width, float height, Color start, Color end, GradientDirection direction) {
        NVGUColour colour = new NVGUColour(createAndStorePaint());

        float startX = x;
        float startY = y;
        float endX = x + width;
        float endY = y;

        switch (direction) {
            case RIGHT_TO_LEFT:
                startX = x + width;
                endX = x;
                break;

            case TOP_TO_BOTTOM:
                startX = x;
                startY = y;
                endX = x;
                endY = y + height;
                break;

            case BOTTOM_TO_TOP:
                startX = x;
                startY = y + height;
                endX = x;
                endY = y;
                break;

            case DIAGONAL_LEFT_TO_RIGHT_UP:
                startY = y + height;
                endY = y;
                break;

            case DIAGONAL_LEFT_TO_RIGHT_DOWN:
                startY = y;
                endY = y + height;
                break;

            case DIAGONAL_RIGHT_TO_LEFT_UP:
                startX = x + width;
                startY = y + height;
                endX = x;
                endY = y;
                break;

            case DIAGONAL_RIGHT_TO_LEFT_DOWN:
                startX = x + width;
                startY = y;
                endX = x;
                endY = y + height;
                break;
        }

        nvgLinearGradient(handle, startX, startY, endX, endY, createAndStoreColour(start), createAndStoreColour(end), colour.getPaint());

        return colour;
    }

    /**
     * Creates a linear gradient and stores it in a {@link NVGPaint}
     * @param startX starting x coordinate of the gradient
     * @param startY starting y coordinate of the gradient
     * @param endX ending x coordinate of the gradient
     * @param endY ending y coordinate of the gradient
     * @param innerColour colour of the inner gradient
     * @param outerColour colour of the outer gradient
     */
    public NVGPaint createAndStoreLinearGradient(float startX, float startY, float endX, float endY, Color innerColour, Color outerColour) {
        NVGPaint paint = nvgLinearGradient(handle, startX, startY, endX, endY, createAndStoreColour(innerColour), createAndStoreColour(outerColour), NVGPaint.malloc());

        resources.add(paint);

        return paint;
    }

    /**
     * Frees all resources allocated by this object
     */
    public NVGU freeResources() {
        resources.forEach(NativeResource::free);
        resources.clear();

        return this;
    }

    public long getHandle() {
        return handle;
    }

    public enum GradientDirection {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        TOP_TO_BOTTOM,
        BOTTOM_TO_TOP,
        DIAGONAL_LEFT_TO_RIGHT_UP,
        DIAGONAL_LEFT_TO_RIGHT_DOWN,
        DIAGONAL_RIGHT_TO_LEFT_UP,
        DIAGONAL_RIGHT_TO_LEFT_DOWN
    }

    private void checkInitialisedState() {
        if (handle == -1) {
            this.handle = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        }
    }

    private ByteBuffer getBytes(InputStream stream, int size) {
        ByteBuffer buffer = BufferUtils.createByteBuffer(size);
        ReadableByteChannel channel = Channels.newChannel(stream);

        while (true) {
            try {
                int bytes = channel.read(buffer);

                if (bytes == -1) {
                    break;
                }

                if (buffer.remaining() == 0) {
                    ByteBuffer newBuffer = BufferUtils.createByteBuffer(buffer.capacity() * 3 / 2);
                    buffer.flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        buffer.flip();

        return MemoryUtil.memSlice(buffer);
    }

}