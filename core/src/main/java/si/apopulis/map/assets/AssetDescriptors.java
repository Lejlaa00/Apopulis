package si.apopulis.map.assets;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public final class AssetDescriptors {

    private AssetDescriptors() {}

    public static final AssetDescriptor<TextureAtlas> UI_ATLAS =
        new AssetDescriptor<>(AssetPaths.UI_ATLAS, TextureAtlas.class);

    public static final AssetDescriptor<BitmapFont> UI_FONT =
        new AssetDescriptor<>(AssetPaths.UI_FONT, BitmapFont.class);
}
