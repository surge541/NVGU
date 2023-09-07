package org.nvgu;

import org.lwjgl.BufferUtils;
import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;
import org.nvgu.util.*;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.opengl.GL11.*;

public class NVGU {

    private long handle = -1;
    private final List<NativeResource> resources = new ArrayList<>();

    private String currentFont = null;
    private int currentFontSize = -1;
    private Alignment alignment = Alignment.LEFT_TOP;

    private final Map<String, Integer> textures = new HashMap<>();

    /**
     * Creates the instance of NanoVG
     */
    public NVGU create() {
        if (handle == -1) {
            this.handle = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        }

        return this;
    }

    /**
     * Creates a font with the given identifier from the given input stream.
     * @param identifier what identifier will be used to draw the font
     * @param fontStream the input stream of the font
     */
    public NVGU createFont(String identifier, InputStream fontStream) {
        nvgCreateFontMem(handle, identifier, getBytes(fontStream, 1024), false);
        return this;
    }

    /**
     * Creates a texture.
     * @param identifier what identifier will be used to draw the texture
     * @param texture the input stream of the texture
     */
    public NVGU createTexture(String identifier, InputStream texture) {
        return createTexture(identifier, texture, NVG_IMAGE_NEAREST);
    }

    /**
     * Creates a texture.
     * @param identifier what identifier will be used to draw the texture
     * @param texture the input stream of the texture
     * @param flags any additional flags you want
     */
    public NVGU createTexture(String identifier, InputStream texture, int flags) {
        if (!textures.containsKey(identifier)) {
            textures.put(identifier, nvgCreateImageMem(handle, flags, getBytes(texture, 512)));
        }

        return this;
    }

    /**
     * Destroys the instance of NanoVG
     */
    public void destroy() {
        textures.forEach((identifier, imageHandle) -> nvgDeleteImage(handle, imageHandle));

        nvgDelete(handle);
        handle = -1;
    }

    /**
     * Begins a new frame
     * @param width the horizontal size of the frame in pixels
     * @param height the vertical size of the frame in pixels
     */
    public NVGU beginFrame(int width, int height) {
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

        freeResources();
        endFrame();

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
     * Textured rectangle
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param texture the texture identifier to use
     */
    public NVGU texturedRectangle(float x, float y, float width, float height, String texture) {
        return rectangle(x, y, width, height, texture(texture, x, y, width, height));
    }

    /**
     * Basic coloured rectangle.
     * @param rectangle bounds of the rectangle
     * @param colour colour of the rectangle
     */
    public NVGU rectangle(Rectangle rectangle, Color colour) {
        return rectangle((float) rectangle.getX(), (float) rectangle.getY(), (float) rectangle.getWidth(), (float) rectangle.getHeight(), colour);
    }

    /**
     * Basic coloured rectangle.
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
     * Textured rectangle border
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param texture the texture identifier to use
     */
    public NVGU texturedRectangleBorder(float x, float y, float width, float height, float thickness, String texture, Border border) {
        return rectangleBorder(x, y, width, height, thickness, texture(texture, x, y, width, height), border);
    }

    /**
     * Basic coloured rectangle border.
     * @param rectangle bounds of the rectangle
     * @param colour colour of the rectangle
     */
    public NVGU rectangleBorder(Rectangle rectangle, float thickness, Color colour, Border border) {
        return rectangleBorder((float) rectangle.getX(), (float) rectangle.getY(), (float) rectangle.getWidth(), (float) rectangle.getHeight(), thickness, colour, border);
    }

    /**
     * Basic coloured rectangle border.
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param thickness thickness of the border
     * @param colour colour of the rectangle
     */
    public NVGU rectangleBorder(float x, float y, float width, float height, float thickness, Color colour, Border border) {
        nvgBeginPath(handle);

        switch (border) {
            case INSIDE: {
                x += thickness / 2f;
                y += thickness / 2f;
                width -= thickness;
                height -= thickness;
                break;
            }

            case OUTSIDE: {
                x -= thickness / 2f;
                y -= thickness / 2f;
                width += thickness;
                height += thickness;
                break;
            }
        }

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
     * Textured rounded rectangle
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param topLeft radius of the top left corner
     * @param topRight radius of the top right corner
     * @param bottomRight radius of the bottom right corner
     * @param bottomLeft radius of the bottom left corner
     * @param texture the texture identifier to use
     */
    public NVGU texturedRoundedRectangle(float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, String texture) {
        return roundedRectangle(x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, texture(texture, x, y, width, height));
    }

    /**
     * Textured rounded rectangle
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param radius radius of the rounded rectangle
     * @param texture the texture identifier to use
     */
    public NVGU texturedRoundedRectangle(float x, float y, float width, float height, float radius, String texture) {
        return roundedRectangle(x, y, width, height, radius, texture(texture, x, y, width, height));
    }

    /**
     * Basic coloured rounded rectangle.
     * @param bounds bounds of the rectangle
     * @param topLeft radius of the top left corner
     * @param topRight radius of the top right corner
     * @param bottomRight radius of the bottom right corner
     * @param bottomLeft radius of the bottom left corner
     * @param colour colour of the rounded rectangle
     */
    public NVGU roundedRectangle(Rectangle bounds, float topLeft, float topRight, float bottomRight, float bottomLeft, Color colour) {
        return roundedRectangle((float) bounds.getX(), (float) bounds.getY(), (float) bounds.getWidth(), (float) bounds.getHeight(), topLeft, topRight, bottomRight, bottomLeft, colour);
    }

    /**
     * Basic coloured rounded rectangle.
     * @param bounds bounds of the rectangle
     * @param radius radius of the rounded rectangle
     * @param colour colour of the rounded rectangle
     */
    public NVGU roundedRectangle(Rectangle bounds, float radius, Color colour) {
        return roundedRectangle((float) bounds.getX(), (float) bounds.getY(), (float) bounds.getWidth(), (float) bounds.getHeight(), radius, colour);
    }

    /**
     * Basic coloured rounded rectangle.
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param radius radius of the rounded rectangle
     * @param colour colour of the rounded rectangle
     */
    public NVGU roundedRectangle(float x, float y, float width, float height, float radius, Color colour) {
        return roundedRectangle(x, y, width, height, radius, radius, radius, radius, colour);
    }

    /**
     * Basic coloured rounded rectangle.
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param topLeft radius of the top left corner
     * @param topRight radius of the top right corner
     * @param bottomRight radius of the bottom right corner
     * @param bottomLeft radius of the bottom left corner
     * @param colour colour of the rounded rectangle
     */
    public NVGU roundedRectangle(float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, Color colour) {
        nvgBeginPath(handle);

        nvgRoundedRectVarying(handle, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft);

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
     * Textured rounded rectangle
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param topLeft radius of the top left corner
     * @param topRight radius of the top right corner
     * @param bottomRight radius of the bottom right corner
     * @param bottomLeft radius of the bottom left corner
     * @param thickness thickness of the border
     * @param texture the texture identifier to use
     * @param border the border type to use
     */
    public NVGU texturedRounddeRectangleBorder(float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, float thickness, String texture, Border border) {
        return roundedRectangleBorder(x, y, width, height, topLeft, topRight, bottomRight, bottomLeft, thickness, texture(texture, x, y, width, height), border);
    }

    /**
     * Textured rounded rectangle
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param radius radius of the rounded rectangle
     * @param texture the texture identifier to use
     */
    public NVGU texturedRounddeRectangleBorder(float x, float y, float width, float height, float radius, float thickness, String texture, Border border) {
        return roundedRectangleBorder(x, y, width, height, radius, thickness, texture(texture, x, y, width, height), border);
    }

    /**
     * Basic coloured rounded rectangle border.
     * @param bounds bounds of the rectangle
     * @param topLeft radius of the top left corner
     * @param topRight radius of the top right corner
     * @param bottomRight radius of the bottom right corner
     * @param bottomLeft radius of the bottom left corner
     * @param thickness the thickness of the border
     * @param colour colour of the rounded rectangle
     */
    public NVGU roundedRectangleBorder(Rectangle bounds, float topLeft, float topRight, float bottomRight, float bottomLeft, float thickness, Color colour, Border border) {
        return roundedRectangleBorder((float) bounds.getX(), (float) bounds.getY(), (float) bounds.getWidth(), (float) bounds.getHeight(), topLeft, topRight, bottomRight, bottomLeft, thickness, colour, border);
    }

    /**
     * Basic coloured rounded rectangle border.
     * @param bounds bounds of the rectangle
     * @param radius radius of the rounded rectangle
     * @param thickness the thickness of the border
     * @param colour colour of the rounded rectangle
     */
    public NVGU roundedRectangleBorder(Rectangle bounds, float radius, float thickness, Color colour, Border border) {
        return roundedRectangleBorder((float) bounds.getX(), (float) bounds.getY(), (float) bounds.getWidth(), (float) bounds.getHeight(), radius, thickness, colour, border);
    }

    /**
     * Basic coloured rounded rectangle border.
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param radius radius of the rounded rectangle
     * @param thickness the thickness of the border
     * @param colour colour of the rounded rectangle
     */
    public NVGU roundedRectangleBorder(float x, float y, float width, float height, float radius, float thickness, Color colour, Border border) {
        return roundedRectangleBorder(x, y, width, height, radius, radius, radius, radius, thickness, colour, border);
    }

    /**
     * Basic coloured rounded rectangle border.
     * @param x left coordinate
     * @param y top coordinate
     * @param width width of the rectangle
     * @param height height of the rectangle
     * @param topLeft radius of the top left corner
     * @param topRight radius of the top right corner
     * @param bottomRight radius of the bottom right corner
     * @param bottomLeft radius of the bottom left corner
     * @param thickness the thickness of the border
     * @param colour colour of the rounded rectangle
     */
    public NVGU roundedRectangleBorder(float x, float y, float width, float height, float topLeft, float topRight, float bottomRight, float bottomLeft, float thickness, Color colour, Border border) {
        nvgBeginPath(handle);

        switch (border) {
            case INSIDE: {
                x += thickness / 2f;
                y += thickness / 2f;
                width -= thickness;
                height -= thickness;
                break;
            }

            case OUTSIDE: {
                x -= thickness / 2f;
                y -= thickness / 2f;
                width += thickness;
                height += thickness;
                break;
            }
        }

        nvgRoundedRectVarying(handle, x, y, width, height, topLeft, topRight, bottomRight, bottomLeft);

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
     * Basic coloured circle.
     * @param x centre x coordinate of the circle
     * @param y centre y coordinate of the circle
     * @param radius radius of the circle
     * @param colour colour of the circle
     */
    public NVGU circle(float x, float y, float radius, Color colour) {
        nvgBeginPath(handle);

        nvgCircle(handle, x, y, radius);

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
     * Basic coloured circle border.
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
     * Basic coloured right-angled triangle.
     * @param x centre x coordinate of the circle
     * @param y centre y coordinate of the circle
     * @param width width of the triangle
     * @param height height of the triangle
     * @param colour colour of the circle
     * @param corner where the corner is located
     */
    public NVGU rightAngledTriangle(float x, float y, float width, float height, Color colour, RightAngledTriangleCorner corner) {
        nvgBeginPath(handle);

        switch (corner) {
            case TOP_LEFT: {
                nvgMoveTo(handle, x, y);
                nvgLineTo(handle, x + width, y);
                nvgLineTo(handle, x, y + height);
                nvgLineTo(handle, x, y);
                break;
            }

            case TOP_RIGHT: {
                nvgMoveTo(handle, x + width, y);
                nvgLineTo(handle, x, y);
                nvgLineTo(handle, x + width, y + height);
                nvgLineTo(handle, x + width, y);
                break;
            }

            case BOTTOM_LEFT: {
                nvgMoveTo(handle, x, y + height);
                nvgLineTo(handle, x + width, y + height);
                nvgLineTo(handle, x, y);
                nvgLineTo(handle, x, y + height);
                break;
            }

            case BOTTOM_RIGHT: {
                nvgMoveTo(handle, x + width, y + height);
                nvgLineTo(handle, x, y + height);
                nvgLineTo(handle, x + width, y);
                nvgLineTo(handle, x + width, y + height);
                break;
            }
        }

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
     * Basic coloured right-angled triangle border.
     * @param x centre x coordinate of the circle
     * @param y centre y coordinate of the circle
     * @param width width of the triangle
     * @param height height of the triangle
     * @param colour colour of the circle
     * @param corner where the corner is located
     */
    public NVGU rightAngledTriangleBorder(float x, float y, float width, float height, float thickness, Color colour, RightAngledTriangleCorner corner) {
        nvgBeginPath(handle);

        switch (corner) {
            case TOP_LEFT: {
                nvgMoveTo(handle, x, y);
                nvgLineTo(handle, x + width, y);
                nvgLineTo(handle, x, y + height);
                nvgLineTo(handle, x, y);
                break;
            }

            case TOP_RIGHT: {
                nvgMoveTo(handle, x + width, y);
                nvgLineTo(handle, x, y);
                nvgLineTo(handle, x + width, y + height);
                nvgLineTo(handle, x + width, y);
                break;
            }

            case BOTTOM_LEFT: {
                nvgMoveTo(handle, x, y + height);
                nvgLineTo(handle, x + width, y + height);
                nvgLineTo(handle, x, y);
                nvgLineTo(handle, x, y + height);
                break;
            }

            case BOTTOM_RIGHT: {
                nvgMoveTo(handle, x + width, y + height);
                nvgLineTo(handle, x, y + height);
                nvgLineTo(handle, x + width, y);
                nvgLineTo(handle, x + width, y + height);
                break;
            }
        }

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
     * Basic filled polygon.
     * @param points array of points, a point being a float array of length 2
     * @param colour the colour of the polygon
     */
    public NVGU polygon(float[][] points, Color colour) {
        nvgBeginPath(handle);

        nvgMoveTo(handle, points[0][0], points[0][1]);

        for (int i = 1; i < points.length; i++) {
            nvgLineTo(handle, points[i][0], points[i][1]);
        }

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
     * Basic filled polygon.
     * @param points array of points, a point being a float array of length 2
     * @param colour the colour of the polygon
     */
    public NVGU polygonBorder(float[][] points, float thickness, Color colour) {
        nvgBeginPath(handle);

        nvgMoveTo(handle, points[0][0], points[0][1]);

        for (int i = 1; i < points.length; i++) {
            nvgLineTo(handle, points[i][0], points[i][1]);
        }

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
     * Renders the given text at given coordinates.
     * Uses {@link NVGU#currentFont}, {@link NVGU#currentFontSize} and {@link NVGU#alignment}
     * for the additional data.
     * If these aren't set, a {@link NullPointerException} with be thrown.
     * @param text the text to draw
     * @param x the x position
     * @param y the y position
     * @param colour the colour of the text - will not accept gradients
     */
    public NVGU text(String text, float x, float y, Color colour) {
        return text(text, x, y, colour, this.currentFont, this.currentFontSize, this.alignment);
    }

    /**
     * Renders the given text at given coordinates, with alignment {@link Alignment#LEFT_TOP}
     * @param text the text to draw
     * @param x the x position
     * @param y the y position
     * @param colour the colour of the text - will not accept gradients
     * @param font what font to use - must have been created using {@link NVGU#createFont(String, InputStream)}
     * @param size the font size
     */
    public NVGU text(String text, float x, float y, Color colour, String font, int size) {
        return text(text, x, y, colour, font, size, Alignment.LEFT_TOP);
    }

    /**
     * Renders the given text at given coordinates
     * @param text the text to draw
     * @param x the x position
     * @param y the y position
     * @param colour the colour of the text - will not accept gradients
     * @param font what font to use - must have been created using {@link NVGU#createFont(String, InputStream)}
     * @param size the font size
     * @param alignment how the text should be aligned in accordance with the coordinates
     */
    public NVGU text(String text, float x, float y, Color colour, String font, int size, Alignment alignment) {
        nvgBeginPath(handle);

        nvgFillColor(handle, createAndStoreColour(colour));
        nvgFontFace(handle, font);
        nvgFontSize(handle, size);
        nvgTextAlign(handle, alignment.getTextAlignment());
        nvgText(handle, x, y + 1, text);

        nvgClosePath(handle);

        return this;
    }

    public String getCurrentFont() {
        return currentFont;
    }

    public int getCurrentFontSize() {
        return currentFontSize;
    }

    public Alignment getAlignment() {
        return alignment;
    }

    /**
     * Sets the font data for {@link NVGU#text(String, float, float, Color)}
     * @param font what font to use - must have been created using {@link NVGU#createFont(String, InputStream)}
     * @param size the size of the font
     * @param alignment the alignment of the font
     */
    public NVGU setFontData(String font, int size, Alignment alignment) {
        this.currentFont = font;
        this.currentFontSize = size;
        this.alignment = alignment;

        return this;
    }

    /**
     * Gets the width of the given text.
     * Uses {@link NVGU#currentFont} and {@link NVGU#currentFontSize}
     * for the additional data.
     * If these aren't set, a {@link NullPointerException} with be thrown.
     * @param text the given text to calculate the width of
     * @return the width of the text
     */
    public float textWidth(String text) {
        return textWidth(text, currentFont, currentFontSize);
    }

    /**
     * Gets the width of the given text.
     * @param text the given text to calculate the width of
     * @param font the font to use
     * @param size the size of the font
     * @return the width of the text
     */
    public float textWidth(String text, String font, int size) {
        float[] bounds = new float[4];

        save();

        nvgFontFace(handle, font);
        nvgFontSize(handle, size);
        nvgTextBounds(handle, 0f, 0f, text, bounds);

        restore();

        return bounds[2];
    }

    /**
     * Gets the height of a font.
     * Uses {@link NVGU#currentFont} and {@link NVGU#currentFontSize}
     * for the additional data.
     * If these aren't set, a {@link NullPointerException} with be thrown.
     * @return the height of the font
     */
    public float textHeight() {
        return textHeight(currentFont, currentFontSize);
    }

    /**
     * Gets the height of a font.
     * @param font the font to use
     * @param size the font size
     * @return the height of the font
     */
    public float textHeight(String font, int size) {
        float[] ascender = new float[1];
        float[] descender = new float[1];
        float[] height = new float[1];

        nvgFontFace(handle, font);
        nvgFontSize(handle, size);
        nvgTextMetrics(handle, ascender, descender, height);

        return height[0];
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
        NVGColor nvgColour = NVGColor.calloc()
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
        NVGPaint paint = NVGPaint.calloc();

        resources.add(paint);

        return paint;
    }

    /**
     * Creates a linear gradient in an instance of an {@link NVGUColour}.
     * The position parameters will most likely be the same as the coordinates of whatever shape you are
     * drawing, e.g. a rectangle.
     * The feather will be the greatest of either width or height.
     * @param x start x coordinate of the gradient
     * @param y start y coordinate of the gradient
     * @param width width of the gradient
     * @param height height of the gradient
     * @param start start colour of the gradient
     * @param end end colour of the gradient
     * @param direction direction of the gradient
     * @return instance of the gradient inside an {@link NVGUColour}
     */
    public NVGUColour linearGradient(float x, float y, float width, float height, Color start, Color end, LinearGradientDirection direction) {
        return linearGradient(x, y, width, height, Math.max(width, height), start, end, direction);
    }

    /**
     * Creates a linear gradient in an instance of an {@link NVGUColour}. The position
     * parameters will most likely be the same as the coordinates of whatever shape you are
     * drawing, e.g. a rectangle.
     * @param x start x coordinate of the gradient
     * @param y start y coordinate of the gradient
     * @param width width of the gradient
     * @param height height of the gradient
     * @param feather the distance for the gradient to apply between the two colours
     * @param start start colour of the gradient
     * @param end end colour of the gradient
     * @param direction direction of the gradient
     * @return instance of the gradient inside an {@link NVGUColour}
     */
    public NVGUColour linearGradient(float x, float y, float width, float height, float feather, Color start, Color end, LinearGradientDirection direction) {
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

        colour.setPaint(nvgLinearGradient(handle, startX, startY, endX, endY, createAndStoreColour(start), createAndStoreColour(end), colour.getPaint()).feather(feather));

        return colour;
    }

    /**
     * Creates a radial gradient in an instance of an {@link NVGUColour}.
     * The position parameters will most likely be the same as the coordinates of whatever shape you are
     * drawing, e.g. a rectangle.
     * The feather will be the greatest of either width or height.
     * @param x start x coordinate of the gradient
     * @param y start y coordinate of the gradient
     * @param width width of the gradient
     * @param height height of the gradient
     * @param innerRadius the inner radius of the gradient
     * @param outerRadius the outer radius of the gradient
     * @param start start colour of the gradient
     * @param end end colour of the gradient
     * @param alignment alignment of the gradient
     * @return instance of the gradient inside an {@link NVGUColour}
     */
    public NVGUColour radialGradient(float x, float y, float width, float height, float innerRadius, float outerRadius, Color start, Color end, Alignment alignment) {
        return radialGradient(x, y, width, height, innerRadius, outerRadius, Math.max(width, height), start, end, alignment);
    }

    /**
     * Creates a radial gradient in an instance of an {@link NVGUColour}.
     * The position parameters will most likely be the same as the coordinates of whatever shape you are
     * drawing, e.g. a rectangle.
     * @param x start x coordinate of the gradient
     * @param y start y coordinate of the gradient
     * @param width width of the gradient
     * @param height height of the gradient
     * @param innerRadius the inner radius of the gradient
     * @param outerRadius the outer radius of the gradient
     * @param feather the distance for the gradient to apply between the two colours
     * @param start start colour of the gradient
     * @param end end colour of the gradient
     * @param alignment alignment of the gradient
     * @return instance of the gradient inside an {@link NVGUColour}
     */
    public NVGUColour radialGradient(float x, float y, float width, float height, float innerRadius, float outerRadius, float feather, Color start, Color end, Alignment alignment) {
        NVGUColour colour = new NVGUColour(createAndStorePaint());

        float startX = x;
        float startY = y;

        switch (alignment) {
            case CENTER_TOP:
                startX = x + width / 2f;
                break;

            case RIGHT_TOP:
                startX = x + width;
                break;

            case LEFT_MIDDLE:
                startY = y + height / 2f;
                break;

            case CENTER_MIDDLE:
                startX = x + width / 2f;
                startY = y + height / 2f;
                break;

            case RIGHT_MIDDLE:
                startX = x + width;
                startY = y + height / 2f;
                break;

            case LEFT_BOTTOM:
                startY = y + height;
                break;

            case CENTER_BOTTOM:
                startX = x + width / 2f;
                startY = y + height;
                break;

            case RIGHT_BOTTOM:
                startX = x + width;
                startY = y + height;
                break;
        }

        colour.setPaint(nvgRadialGradient(handle, startX, startY, innerRadius, outerRadius, createAndStoreColour(start), createAndStoreColour(end), colour.getPaint()).feather(feather));

        return colour;
    }

    public NVGUColour texture(String identifier, float x, float y, float width, float height) {
        NVGUColour colour = new NVGUColour(createAndStorePaint());

        nvgImageSize(handle, textures.get(identifier), new int[]{ (int) width }, new int[]{ (int) height });

        nvgImagePattern(handle, x, y, width, height, 0, textures.get(identifier), 1f, colour.getPaint());

        return colour;
    }

    /**
     * Frees all resources allocated by this object
     */
    public NVGU freeResources() {
        resources.forEach(NativeResource::free);
        resources.clear();

        return this;
    }

    /**
     * Gets the handle of the NanoVG instance.
     * @return the handle of the NanoVG instance
     */
    public long getHandle() {
        return handle;
    }

    /**
     * Allows you to execute code without breaking out of a chain of method calls
     * @param runnable the code to be executed
     */
    public NVGU also(Runnable runnable) {
        runnable.run();

        return this;
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