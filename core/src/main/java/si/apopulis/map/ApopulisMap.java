package si.apopulis.map;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.assets.AssetManager;
import si.apopulis.map.assets.AssetDescriptors;

public class ApopulisMap extends Game {

    private AssetManager assetManager;

    @Override
    public void create() {
        assetManager = new AssetManager();
        
        // Load UI assets
        assetManager.load(AssetDescriptors.UI_ATLAS);
        assetManager.load(AssetDescriptors.UI_FONT);
        
        // Finish loading synchronously (in production, you might want async loading)
        assetManager.finishLoading();
        
        setScreen(new MapScreen(assetManager));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (assetManager != null) {
            assetManager.dispose();
        }
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }
}
