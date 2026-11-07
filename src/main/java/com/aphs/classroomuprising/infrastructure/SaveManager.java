package com.aphs.classroomuprising.infrastructure;

import com.aphs.classroomuprising.domain.*;

public class SaveManager {
    private String savePath;

    public boolean save(GameState state) { return false; }
    public GameState load() { return null; }
    private boolean validate(GameState state) { return false; }
}
