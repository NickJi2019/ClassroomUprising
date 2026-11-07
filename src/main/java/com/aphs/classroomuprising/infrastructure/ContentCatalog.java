package com.aphs.classroomuprising.infrastructure;

import com.aphs.classroomuprising.domain.*;
import java.util.ArrayList;

public class ContentCatalog {
    private ArrayList<StoryEvent> stories;
    private ArrayList<KnowledgeTopic> topics;
    private ArrayList<Question> questions;
    private ArrayList<Activity> activities;

    public Activity findActivity(String id) { return null; }
    public Question selectQuestion(String topicId) { return null; }
}
