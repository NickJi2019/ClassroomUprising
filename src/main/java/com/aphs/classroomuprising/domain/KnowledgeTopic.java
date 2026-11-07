package com.aphs.classroomuprising.domain;

import java.util.ArrayList;

public class KnowledgeTopic {
    private String id;
    private String subject;
    private int unlockDay;
    private ArrayList<String> prerequisiteIds;

    public boolean isEligible(GameState state) { return false; }
}
