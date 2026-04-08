package com.voxd31.editor.web;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public final class VoxcraftWebApp extends ApplicationAdapter {
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.1f);
        font.setColor(Color.valueOf("16202fff"));
        layout = new GlyphLayout();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.89f, 0.93f, 0.98f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        StringBuilder sb = new StringBuilder();
        sb.append("voxcraft Web Runtime\n\n");
        sb.append("This release bundle provides the browser shell and web component packaging targets.\n");
        sb.append("The editor itself is still desktop-first because the current core runtime uses java.io.File.\n\n");
        sb.append("Bundled repository docs:\n");
        sb.append("- readme.md\n");

        layout.setText(font, sb);
        float x = 28f;
        float y = Gdx.graphics.getHeight() - 36f;

        batch.begin();
        font.draw(batch, layout, x, y);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (batch != null) {
            batch.getProjectionMatrix().setToOrtho2D(0f, 0f, width, height);
        }
    }

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
        }
        if (font != null) {
            font.dispose();
        }
    }
}
