package si.apopulis.map;

import com.badlogic.gdx.Game;

public class ApopulisMap extends Game {

    @Override
    public void create() {
        setScreen(new MapScreen());
    }
}
