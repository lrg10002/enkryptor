package com.github.lrg10002.enkryptor;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EKProperties {

    public String passhash;
    public byte[] salt;
    public Map<String, String> nameKeys = new HashMap<>();
    public Map<String, List<String>> tags = new HashMap<>(); //Key is file id, not name
    public Map<String, List<String>> revTags = new HashMap<>();

    public EKProperties(String ph, byte[] s, Map<String, String> nk, Map<String, List<String>> t) {
        passhash = ph;
        nameKeys = nk;
        tags = FXCollections.observableMap(t);
        salt = s;

        genRevTags();

        ((ObservableMap<String, List<String>>)tags).addListener((MapChangeListener<String, List<String>>) change -> genRevTags());
    }

    private void genRevTags() {
        revTags.clear();
        tags.keySet().stream()
                .forEach(fid -> {
                    tags.get(fid).forEach(tag -> {
                        if (revTags.containsKey(tag)) {
                            revTags.get(tag).add(fid);
                        } else {
                            revTags.put(tag, Arrays.asList(fid));
                        }
                    });
                });
    }

}
