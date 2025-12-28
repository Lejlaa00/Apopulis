package si.apopulis.map.lwjgl3;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;

public class AssetPacker {

    public static void main(String[] args) {

        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.maxWidth = 1024;
        settings.maxHeight = 1024;
        settings.combineSubdirectories = true;
        settings.paddingX = 2;
        settings.paddingY = 2;
        settings.duplicatePadding = true;
        settings.edgePadding = true;
        settings.useIndexes = false;

        // INPUT folder (raw images)
        String inputDir = "lwjgl3/assets-raw";

        // OUTPUT folder (atlas)
        String outputDir = "assets/atlas";

        // Atlas name (will create ui.atlas + ui.png)
        String atlasName = "ui";

        TexturePacker.process(settings, inputDir, outputDir, atlasName);

        System.out.println("UI atlas generated successfully.");
    }
}
