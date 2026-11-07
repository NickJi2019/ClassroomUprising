package com.aphs.classroomuprising.infrastructure;

import com.aphs.classroomuprising.domain.*;
import java.io.*;

public class SaveManager {
    private final String savePath;

    public SaveManager() {
        this.savePath = "savegame.txt";
    }

    public SaveManager(String savePath) {
        this.savePath = savePath;
    }

    public boolean save(GameState state) {
        if (state == null || state.getStudent() == null) return false;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(savePath))) {
            writer.write("NAME:" + state.getStudent().getName());
            writer.newLine();
            writer.write("MENTAL:" + state.getStudent().getMentalState());
            writer.newLine();
            writer.write("ICS_KNOWLEDGE:" + state.getStudent().getIcsKnowledge());
            writer.newLine();
            writer.write("OTHER_KNOWLEDGE:" + state.getStudent().getOtherKnowledge());
            writer.newLine();
            writer.write("GRADE_HP:" + state.getStudent().getGradeHp());
            writer.newLine();
            writer.write("DAY:" + state.getCalendar().getCurrentDay());
            writer.newLine();
            writer.write("PERIOD:" + state.getCalendar().getCurrentPeriod().name());
            writer.newLine();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public GameState load() {
        File file = new File(savePath);
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String name = "Student";
            int mental = 100, ics = 0, other = 0, hp = 100, day = 1;
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length < 2) continue;
                switch (parts[0]) {
                    case "NAME": name = parts[1]; break;
                    case "MENTAL": mental = Integer.parseInt(parts[1]); break;
                    case "ICS_KNOWLEDGE": ics = Integer.parseInt(parts[1]); break;
                    case "OTHER_KNOWLEDGE": other = Integer.parseInt(parts[1]); break;
                    case "GRADE_HP": hp = Integer.parseInt(parts[1]); break;
                    case "DAY": day = Integer.parseInt(parts[1]); break;
                }
            }
            StudentState student = new StudentState(name);
            student.apply(new StatChange(mental - 100, ics, other, hp - 100));
            GameState state = new GameState(student);
            for (int i = 1; i < day; i++) {
                state.getCalendar().advanceDay();
            }
            return validate(state) ? state : null;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean validate(GameState state) {
        return state != null && state.getStudent() != null && state.getStudent().getName() != null;
    }
}
