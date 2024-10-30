package com.glisco.isometricrenders.render;

import com.glisco.isometricrenders.property.DefaultPropertyBundle;
import com.glisco.isometricrenders.property.GlobalProperties;
import com.glisco.isometricrenders.property.PropertyBundle;
import com.glisco.isometricrenders.screen.IsometricUI;
import com.glisco.isometricrenders.util.ExportPathSpec;
import com.glisco.isometricrenders.util.ImageIO;
import com.glisco.isometricrenders.util.ParticleRestriction;
import com.glisco.isometricrenders.util.Translate;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Component;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.List;

public class BatchRenderable<R extends Renderable<?>> implements Renderable<BatchRenderable.BatchPropertyBundle> {

    private final List<R> delegates;
    private final String contentType;

    private R currentDelegate;
    private int currentIndex;

    private long renderDelay;
    private long lastRenderTime;

    private boolean batchActive;

    private BatchRenderable(String source, List<R> delegates) {
        this.delegates = delegates;
        this.reset();

        this.contentType = ExportPathSpec.exportRoot().resolve("batches/")
                .relativize(ImageIO.next(ExportPathSpec.exportRoot().resolve("batches/" + source + "/"))).toString();
        this.renderDelay = Math.max((int) Math.pow(GlobalProperties.exportResolution / 1024f, 2) * 100L, 75);
    }

    public static <R extends Renderable<?>> BatchRenderable<?> of(String source, List<R> delegates) {
        if (delegates.isEmpty()) {
            return new BatchRenderable<>(source, List.of(Renderable.EMPTY));
        } else {
            return new BatchRenderable<>(source, delegates);
        }
    }

    @Override
    public void prepare() {
        this.currentDelegate.prepare();
    }

    @Override
    public void emitVertices(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float tickDelta) {
        this.currentDelegate.emitVertices(matrices, vertexConsumers, tickDelta);

        if (this.batchActive && this.currentIndex < this.delegates.size() && System.currentTimeMillis() - this.lastRenderTime > this.renderDelay && ImageIO.taskCount() <= 5) {
            final var image = RenderableDispatcher.drawIntoImage(this.currentDelegate, 0, GlobalProperties.exportResolution);
            ImageIO.save(image, this.exportPath());

            this.currentIndex++;
            this.currentDelegate = this.currentIndex < this.delegates.size() ? this.delegates.get(this.currentIndex) : this.currentDelegate;
            this.lastRenderTime = System.currentTimeMillis();
        }
    }

    @Override
    public void draw(Matrix4f modelViewMatrix) {
        this.currentDelegate.draw(modelViewMatrix);
    }

    @Override
    public void cleanUp() {
        this.currentDelegate.cleanUp();
    }

    @Override
    public void dispose() {
        this.delegates.forEach(Renderable::dispose);
    }

    @Override
    public ParticleRestriction<?> particleRestriction() {
        return this.currentDelegate.particleRestriction();
    }

    private void start() {
        this.batchActive = true;
        this.currentIndex = 0;
        this.lastRenderTime = System.currentTimeMillis();
        this.renderDelay = Math.max((int) Math.pow(GlobalProperties.exportResolution / 1024f, 2) * 100L, 75);
    }

    private void reset() {
        this.batchActive = false;
        this.lastRenderTime = -1;
        this.currentIndex = -1;
        this.currentDelegate = this.delegates.get(0);
    }

    @Override
    public BatchPropertyBundle properties() {
        return new BatchPropertyBundle(this.currentDelegate.properties());
    }

    @Override
    public ExportPathSpec exportPath() {
        return this.currentDelegate.exportPath().relocate("batches/" + this.contentType);
    }

    public static class BatchPropertyBundle extends DefaultPropertyBundle {

        private final PropertyBundle delegate;

        public BatchPropertyBundle(PropertyBundle delegate) {
            this.delegate = delegate;

            // A bit ugly, but we copy all property values from the delegate and hook
            // the delegate onto our properties - this makes sure we don't always reset
            // the properties and that the mouse and keyboard controls actually affect the delegate
            if (this.delegate instanceof DefaultPropertyBundle defaultPropertyBundle) {
                this.scale.copyFrom(defaultPropertyBundle.scale);
                this.rotation.copyFrom(defaultPropertyBundle.rotation);
                this.slant.copyFrom(defaultPropertyBundle.slant);
                this.lightAngle.copyFrom(defaultPropertyBundle.lightAngle);
                this.xOffset.copyFrom(defaultPropertyBundle.xOffset);
                this.yOffset.copyFrom(defaultPropertyBundle.yOffset);

                this.scale.listen(defaultPropertyBundle.scale);
                this.rotation.listen(defaultPropertyBundle.rotation);
                this.slant.listen(defaultPropertyBundle.slant);
                this.lightAngle.listen(defaultPropertyBundle.lightAngle);
                this.xOffset.listen(defaultPropertyBundle.xOffset);
                this.yOffset.listen(defaultPropertyBundle.yOffset);
            }
        }

        @Override
        public void buildGuiControls(Renderable<?> renderable, FlowLayout container) {
            final BatchRenderable<?> batchRenderable = (BatchRenderable<?>) renderable;

            this.delegate.buildGuiControls(batchRenderable.currentDelegate, container);

            IsometricUI.sectionHeader(container, "batch.controls", true);
            try (var builder = IsometricUI.row(container)) {
                final var startButton = Components.button(Translate.gui("batch.start"), (ButtonComponent button) -> {
                    batchRenderable.start();
                    button.active = false;
                });
                builder.row.child(((Component) startButton).horizontalSizing(Sizing.fixed(60)).margins(Insets.right(5)));
                builder.row.child((Component) Components.button(Translate.gui("batch.reset"), (ButtonComponent button) -> {
                    batchRenderable.reset();
                    startButton.active = true;
                }));
            }

            IsometricUI.dynamicLabel(container, () -> Translate.gui(
                    "batch.remaining",
                    Math.max(0, batchRenderable.delegates.size() - batchRenderable.currentIndex - 1),
                    batchRenderable.delegates.size()
            ));
        }

        @Override
        public void applyToViewMatrix(Matrix4fStack modelViewStack) {
            this.delegate.applyToViewMatrix(modelViewStack);
        }

    }
}
