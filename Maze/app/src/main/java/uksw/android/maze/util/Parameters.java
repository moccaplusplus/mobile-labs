package uksw.android.maze.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import uksw.android.maze.model.Maze;

public class Parameters {
    public static final String KEY_MAZE_SIZE = "MazeSize";
    public static final String KEY_MAZE_NAMES = "MazeNames";
    public static final String KEY_SELECTED_MAZE = "SelectedMaze";
    public static final String PREFIX_MAZE = "Maze";
    public static final int DEFAULT_SIZE = 20;

    public class Editor {
        private final SharedPreferences.Editor editor;

        Editor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        public void setMazeSize(int mazeSize) {
            editor.putInt(KEY_MAZE_SIZE, mazeSize);
        }

        public void select(String mazeName) {
            if (getMazeNames().contains(mazeName)) {
                editor.putString(KEY_SELECTED_MAZE, mazeName);
            }
        }

        public void setMazeData(String mazeName, String mazeData) {
            Set<String> names = getMazeNames();
            names.add(mazeName);
            editor.putStringSet(KEY_MAZE_NAMES, names);
            editor.putString(PREFIX_MAZE + mazeName, mazeData);
            editor.putString(KEY_SELECTED_MAZE, mazeName);
        }

        public void setMaze(String mazeName, Maze maze) {
            setMazeData(mazeName, maze.serialize());
        }

        public void removeMaze(String mazeName) {
            Set<String> names = getMazeNames();
            names.remove(mazeName);
            boolean isSelected = Objects.equals(getSelectedName(), mazeName);
            editor.putStringSet(KEY_MAZE_NAMES, names);
            editor.remove(PREFIX_MAZE + mazeName);
            if (isSelected) {
                String selection = names.stream().findFirst().orElse(null);
                if (selection == null) editor.remove(KEY_SELECTED_MAZE);
                else editor.putString(KEY_SELECTED_MAZE, selection);
            }
        }
    }

    public final SharedPreferences prefs;

    @SuppressWarnings("deprecation")
    public static Parameters get(Context context) {
        return new Parameters(PreferenceManager.getDefaultSharedPreferences(context));
    }

    private Parameters(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public void apply(Consumer<Editor> edit) {
        SharedPreferences.Editor editor = prefs.edit();
        edit.accept(new Editor(editor));
        editor.apply();
    }

    public boolean commit(Consumer<Editor> edit) {
        SharedPreferences.Editor editor = prefs.edit();
        edit.accept(new Editor(editor));
        return editor.commit();
    }

    public int getMazeSize() {
        return prefs.getInt(KEY_MAZE_SIZE, DEFAULT_SIZE);
    }

    public String getSelectedName() {
        return getSelectedName(null);
    }

    public String getSelectedName(String defaultValue) {
        return prefs.getString(KEY_SELECTED_MAZE, defaultValue);
    }

    public String getMazeData(String mazeName) {
        return prefs.getString(PREFIX_MAZE + mazeName, null);
    }

    public Set<String> getMazeNames() {
        return new HashSet<>(prefs.getStringSet(KEY_MAZE_NAMES, Collections.emptySet()));
    }

    public Maze getMaze(String mazeName) {
        String data = getMazeData(mazeName);
        return data == null ? null : Maze.deserialize(data);
    }

    public Maze getSelectedMaze() {
        String name = getSelectedName();
        return name == null ? null : getMaze(name);
    }
}
